# -*- coding: utf-8 -*-
"""
check_dictionary.py — cross-check the course vocabulary against the BASAbali /
BASAibu Wiki dictionary (dictionary.basabali.org, verified by Balai Bahasa).

  * every current vocabulary entry  (batched 50 pages/request via the MediaWiki API)
  * every entry that was removed or renamed during the review

Writes tools/dictionary_check.tsv (course gloss vs dictionary gloss + level of
speech) and prints the entries that look wrong or missing.
"""
import os, re, io, json, time, urllib.request, urllib.parse, collections

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
OUT = os.path.join(ROOT, 'BasaBaliApp', 'tools', 'dictionary_check.tsv')
CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '_dictcache.json')
API = 'https://dictionary.basabali.org/w/api.php'
UA = {'User-Agent': 'BasaBali-course-check/1.0 (educational content review)'}

# entries removed or renamed during the review — re-check these explicitly
REMOVED = ['ngampura', 'saking napi?', 'suksema', 'suksema mewali', 'rage sing engken',
           'prabu', 'mis', 'kakiyang', 'kakek', 'mabelas', 'telung benang', 'tangi',
           'kueh', 'selem denges', 'putih sentak', 'barak biing', 'dhuwur', 'padha',
           'minum', 'lelah', 'rong tali', 'kulkul', 'buka cicinge', 'batis beling',
           'baong daki', 'nyilih mata', 'basang bungkuk', 'lima lantang', 'bungut tuak',
           'paningalan badang', 'ngutang mis', 'be', 'paling', 'nyampah', 'kude',
           'engken', 'selae', 'pasasur', 'setiman', 'pelekutus', 'siangolas',
           'limolas', 'pitung dasa lima', 'ungu', 'sing kenken', 'nunas ampura',
           'pundi', 'saking pundi?', 'krebek', 'ngunjuk', 'dereng', 'ngrayunang',
           'makta', 'sirep', 'mesare', 'makolem', 'ngajeng', 'nginem', 'ngrayunang']

def load_vocab():
    rows, lesson = [], None
    for raw in io.open(VOCAB, encoding='utf-8'):
        line = raw.rstrip('\n')
        m = re.match(r'#### Lesson (\d+):', line)
        if m:
            lesson = int(m.group(1)); continue
        if lesson and line.startswith('* '):
            parts = [x.strip() for x in line[2:].split('->')]
            entry = parts[0].rstrip('.').replace('...', '').strip()
            gloss = parts[1] if len(parts) > 1 else ''
            reg = parts[2] if len(parts) > 2 else ''
            rows.append((lesson, entry, gloss, reg))
    return rows

def api(params):
    url = API + '?' + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers=UA)
    for attempt in range(3):
        try:
            with urllib.request.urlopen(req, timeout=40) as r:
                return json.load(r)
        except Exception as e:
            print(f"    retry {attempt+1} ({e})")
            time.sleep(2)
    return {}

def fetch_pages(titles):
    """Return {normalized_title: wikitext} for up to 50 titles."""
    d = api({'action': 'query', 'format': 'json', 'prop': 'revisions',
             'rvprop': 'content', 'rvslots': 'main', 'redirects': '1',
             'titles': '|'.join(titles)})
    pages = d.get('query', {}).get('pages', {})
    out = {}
    for p in pages.values():
        t = p.get('title', '')
        if 'missing' in p:
            out[t] = None
            continue
        try:
            out[t] = p['revisions'][0]['slots']['main']['*']
        except Exception:
            out[t] = ''
    return out

def parse_wikitext(t):
    """english translations + level-of-speech from the Balinese Word template."""
    if not t:
        return ('', '')
    m = re.search(r'english translations\s*=\s*([^|}]*)', t)
    en = m.group(1).strip() if m else ''
    levels = []
    for key, label in (('kasar', 'Kasar'), ('andap', 'Andap'), ('mider', 'Mider'),
                       ('alus sor', 'Alus sor'), ('alus madya', 'Alus madya'),
                       ('alus mider', 'Alus mider'), ('alus singgih', 'Alus singgih')):
        if re.search(r'\|\s*' + re.escape(key) + r'\s*=\s*\S', t):
            levels.append(label)
    return (en, ' / '.join(levels))

STOP = {'a', 'an', 'the', 'to', 'of', 'or', 'and', 'is', 'are', 'be', 'in', 'on', 'at',
        'it', 'for', 'with', 'that', 'this', 'has', 'have', 'not', 'no'}

def toks(s):
    return {w for w in re.findall(r"[a-z]+", s.lower()) if w not in STOP and len(w) > 2}

def main():
    rows = load_vocab()
    seen, entries = set(), []
    for L, e, g, r in rows:
        k = re.sub(r'[^a-z ]', '', e.lower()).strip()
        if k in seen:
            continue
        seen.add(k)
        entries.append((L, e, g, r))
    print(f"vocabulary rows: {len(rows)}  unique entries: {len(entries)}")

    to_check = [e for _, e, _, _ in entries] + [w for w in REMOVED if w not in {e for _, e, _, _ in entries}]
    to_check = list(dict.fromkeys(to_check))
    print(f"pages to check: {len(to_check)}")

    cache = {}
    if os.path.exists(CACHE):
        cache = json.load(io.open(CACHE, encoding='utf-8'))
    todo = [t for t in to_check if t not in cache]
    print(f"to fetch: {len(todo)}")
    for i in range(0, len(todo), 50):
        batch = todo[i:i + 50]
        res = fetch_pages(batch)
        for t in batch:
            cache[t] = res.get(t, res.get(t[:1].upper() + t[1:], None))
        print(f"  fetched {min(i+50, len(todo))}/{len(todo)}")
        json.dump(cache, io.open(CACHE, 'w', encoding='utf-8'), ensure_ascii=False)
        time.sleep(0.5)

    problems, ok, missing = [], 0, []
    with io.open(OUT, 'w', encoding='utf-8') as f:
        f.write("entry\tcourse_gloss\tcourse_reg\tdict_gloss\tdict_level\tverdict\n")
        for L, e, g, r in entries:
            t = cache.get(e)
            den, lvl = parse_wikitext(t)
            verdict = 'ok'
            if t is None:
                verdict = 'NOT-IN-DICTIONARY'
                missing.append((L, e, g))
            elif den:
                ct, dt = toks(g), toks(den)
                if ct and dt and not (ct & dt):
                    verdict = 'GLOSS-MISMATCH'
                    problems.append((L, e, g, den, lvl))
            f.write(f"{e}\t{g}\t{r}\t{den}\t{lvl}\t{verdict}\n")
            if verdict == 'ok':
                ok += 1
        for w in REMOVED:
            t = cache.get(w)
            den, lvl = parse_wikitext(t)
            f.write(f"[removed] {w}\t-\t-\t{den}\t{lvl}\t{'EXISTS' if t is not None else 'NOT-FOUND'}\n")

    print(f"\nentries matching the dictionary: {ok}/{len(entries)}")
    print(f"entries not in the dictionary: {len(missing)}")
    print(f"gloss mismatches to review: {len(problems)}")
    print(f"\nfull table: {OUT}")
    print("\n== first 40 gloss mismatches (lesson, entry, course, dictionary) ==")
    for L, e, g, den, lvl in problems[:40]:
        print(f"  L{L}: {e}  course='{g}'  dict='{den[:70]}' [{lvl}]")
    print("\n== removed entries still present in the dictionary ==")
    for w in REMOVED:
        t = cache.get(w)
        if t is not None:
            den, lvl = parse_wikitext(t)
            print(f"  {w}: dict='{den[:70]}' [{lvl}]")

if __name__ == '__main__':
    main()
