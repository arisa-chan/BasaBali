# -*- coding: utf-8 -*-
"""
check_sentences.py — sentence-level audit of the course bank.

A. word choice / evidence: every unique Balinese token in the sentence files is
   looked up in the BASAbali dictionary (dictionary.basabali.org, verified by
   Balai Bahasa). Tokens that are neither vocabulary entries, name/exception
   forms, nor suffix derivations of an entry and are NOT in the dictionary are
   reported (with their sentences) as unverified word choices.
B. politeness agreement: humble (Alus sor, self-directed) verbs used with an
   honoured third-person subject, and honouring (Alus singgih) verbs used with a
   low/informal subject.
"""
import os, re, io, json, time, urllib.request, urllib.parse, collections

ROOT = r"C:\Users\Jaydee Lucero\Downloads\workspace"
VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
SEC = {1: os.path.join(ROOT, 'balinese_course_sentences_section1.md'),
       2: os.path.join(ROOT, 'balinese_course_sentences_section2.md'),
       3: os.path.join(ROOT, 'balinese_course_sentences_section3.md')}
CACHE = os.path.join(ROOT, 'BasaBaliApp', 'tools', '_dictcache.json')
API = 'https://dictionary.basabali.org/w/api.php'
UA = {'User-Agent': 'BasaBali-course-check/1.0 (educational review)'}
NAMES = {'wayan', 'made', 'nyoman', 'ketut', 'putu', 'kadek', 'gede', 'komang', 'agung', 'budi', 'sri'}
EXC = {'cang', 'druwen', 'jeron', 'mawasta', 'rage'}

HUMBLE = {'neda', 'ngambil', 'nunas', 'matur', 'mapamit', 'nunas lugra', 'nunas pica'}
HONOUR = {'ngunjuk', 'ngrayunang', 'ngajeng', 'malinggih', 'mantuk', 'seda', 'makolem',
          'ngandika', 'ledang', 'mawasta', 'mapesengan', 'ngaksi', 'nyingak'}
HON_SUBJ = {'ida', 'ipun'}
LOW_SUBJ = {'cai', 'nyai', 'ia', 'icang'}

def norm_tok(t):
    return re.sub(r'[^a-z0-9]', '', t.lower())

def load_vocab_entries():
    rows, lesson = [], None
    for raw in io.open(VOCAB, encoding='utf-8'):
        line = raw.rstrip('\n')
        m = re.match(r'#### Lesson (\d+):', line)
        if m:
            lesson = int(m.group(1)); continue
        if lesson and line.startswith('* '):
            e = line[2:].split('->')[0].rstrip('.').replace('...', '').strip()
            rows.append(e)
    return rows

def api(params):
    req = urllib.request.Request(API + '?' + urllib.parse.urlencode(params), headers=UA)
    for a in range(3):
        try:
            with urllib.request.urlopen(req, timeout=40) as r:
                return json.load(r)
        except Exception as e:
            time.sleep(2)
    return {}

def exists_batch(titles):
    d = api({'action': 'query', 'format': 'json', 'redirects': '1',
             'titles': '|'.join(titles)})
    out = {}
    for p in d.get('query', {}).get('pages', {}).values():
        out[p.get('title', '')] = 'missing' not in p
    return out

def main():
    entries = load_vocab_entries()
    ent_norm = {re.sub(r'[^a-z ]', '', e.lower()).strip() for e in entries}
    ent_tokens = set()
    for e in entries:
        for w in e.lower().split():
            ent_tokens.add(norm_tok(w))

    sentences = []
    for sec, path in SEC.items():
        lesson = None
        for line in io.open(path, encoding='utf-8'):
            m = re.match(r'### Lesson (\d+):', line)
            if m:
                lesson = int(m.group(1)); continue
            m = re.match(r'(\d+)\.\s+(.+?)\s+->\s+(.+)', line.strip())
            if m and lesson:
                sentences.append((sec, lesson, int(m.group(1)), m.group(2).strip(), m.group(3).strip()))
    print(f"sentences: {len(sentences)}")

    # ---- A. token inventory
    tok_count = collections.Counter()
    tok_examples = collections.defaultdict(list)
    for sec, L, n, bal, en in sentences:
        for w in re.findall(r"[A-Za-zÀ-ÿ']+", bal):
            t = norm_tok(w)
            if not t or t in NAMES or t in EXC:
                continue
            if t in ent_tokens or t in {x.replace(' ', '') for x in ent_norm}:
                continue
            tok_count[t] += 1
            if len(tok_examples[t]) < 3:
                tok_examples[t].append(f"L{L}#{n}: {bal}")

    # suffix derivation of a known entry (-ne/-e/-n) counts as covered
    def covered(t):
        for suf in ('ne', 'e', 'n'):
            if t.endswith(suf) and len(t) > len(suf) and t[:-len(suf)] in ent_tokens:
                return True
        return False

    unknown = [t for t in tok_count if not covered(t)]
    print(f"distinct tokens not matching a vocabulary entry: {len(unknown)}")

    cache = json.load(io.open(CACHE, encoding='utf-8')) if os.path.exists(CACHE) else {}
    todo = [t for t in unknown if t not in cache]
    for i in range(0, len(todo), 50):
        b = todo[i:i + 50]
        res = exists_batch(b)
        for t in b:
            cache[t] = res.get(t, res.get(t.capitalize(), None))
        json.dump(cache, io.open(CACHE, 'w', encoding='utf-8'), ensure_ascii=False)
        print(f"  looked up {min(i+50, len(todo))}/{len(todo)}")
        time.sleep(0.4)

    absent = [(t, tok_count[t], tok_examples[t]) for t in unknown if cache.get(t) is None]
    present = [(t, tok_count[t], tok_examples[t]) for t in unknown if cache.get(t)]
    absent.sort(key=lambda x: -x[1])
    present.sort(key=lambda x: -x[1])
    print(f"\n== tokens NOT in the dictionary ({len(absent)}) ==")
    for t, c, ex in absent:
        print(f"  {t} x{c}: {ex[0][:70]}")
    print(f"\n== tokens in the dictionary but not taught as vocabulary ({len(present)}) ==")
    for t, c, ex in present[:25]:
        print(f"  {t} x{c}: {ex[0][:70]}")

    # ---- B. politeness agreement
    print("\n== politeness-agreement flags ==")
    flags = 0
    for sec, L, n, bal, en in sentences:
        toks = [norm_tok(w) for w in re.findall(r"[A-Za-zÀ-ÿ']+", bal)]
        s = set(toks)
        if s & HON_SUBJ and (s & HUMBLE):
            print(f"  L{L}#{n}: humble verb with honoured subject -> {bal} | {en}"); flags += 1
        if s & LOW_SUBJ and (s & HONOUR):
            print(f"  L{L}#{n}: honouring verb with low subject -> {bal} | {en}"); flags += 1
    print(f"  total: {flags}")

if __name__ == '__main__':
    main()
