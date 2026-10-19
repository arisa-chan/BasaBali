# -*- coding: utf-8 -*-
"""
build_treebank.py — builds a rule-based dependency treebank of the whole course
sentence bank (8,577 sentences) and writes tools/treebank.tsv.

Method (documented, source-grounded — this is a MACHINE-ANNOTATED treebank, not
a hand-validated gold standard):

* Part of speech comes from the BASAbali dictionary's own `part of speech=`
  field, cached in `_dictcache.json` during the vocabulary cross-check, plus a
  curated function-word list (pronouns, numerals, prepositions, negators,
  conjunctions, particles, question words) and morphological heuristics.
* Dependencies follow the word-order facts for Plains/Sembiran Balinese in the
  published literature: the clause is head-initial with the verb (or the
  predicate) as head; A/S precedes V/O (SVO); modifiers follow their head
  (adjective, numeral, possessor, determiner, intensifier all follow the noun/
  adjective they modify); prepositions precede their complement; negators
  precede the predicate. Information-structure fronting is allowed and is
  flagged separately by the audit rather than treated as an error.

Output columns:  sent_id  index  token  pos  head  relation  confidence
  head = 0 means the token is the root; confidence 'lex' = relation forced by
  the lexicon (preposition, negator, particle, conjunction, determiner),
  'rule' = inferred by the clausal/NP rules.
"""
import os, re, io, json, collections

ROOT = r"C:\Users\Jaydee Lucero\Downloads\workspace"
SEC = {1: os.path.join(ROOT, 'balinese_course_sentences_section1.md'),
       2: os.path.join(ROOT, 'balinese_course_sentences_section2.md'),
       3: os.path.join(ROOT, 'balinese_course_sentences_section3.md')}
CACHE = os.path.join(ROOT, 'BasaBaliApp', 'tools', '_dictcache.json')
OUT = os.path.join(ROOT, 'BasaBaliApp', 'tools', 'treebank.tsv')

PRON = {'tiang', 'icang', 'titiang', 'cai', 'nyai', 'ia', 'ipun', 'ida', 'ragane',
        'cang', 'iraga', 'sira', 'nyen', 'apa', 'napi', 'encen'}
NUM = {'besik', 'siki', 'dua', 'duang', 'kalih', 'telu', 'telung', 'tiga', 'tigang',
       'papat', 'patang', 'lima', 'limang', 'nenem', 'enam', 'pitu', 'pitung',
       'kutus', 'sia', 'sangang', 'dasa', 'solas', 'roras', 'telulas', 'patbelas',
       'limolas', 'enembelas', 'pitulas', 'pelekutus', 'siangolas', 'seket', 'satus',
       'siu', 'tali', 'atus', 'likur', 'benang', 'selae', 'pasasur', 'setiman', 'samas',
       'domas', 'laksa', 'yuta', 'miliar'}
ADP = {'di', 'ring', 'ka', 'ke', 'uling', 'saking', 'ajak', 'sareng', 'teken',
       'buat', 'pabuat', 'baang', 'antara', 'samping', 'sanding', 'sor', 'luhur',
       'beten', 'harep', 'ajeng', 'pungkur', 'duri', 'tengah', 'jero', 'sisi', 'jaba'}
NEG = {'sing', 'tusing', 'ten', 'nenten', 'da', 'eda', 'sampunang', 'durung', 'tonden',
       'dereng', 'tan'}
CC = {'lan', 'miwah', 'tur', 'muah', 'nanging', 'sakewanten', 'kewanten', 'tapi',
      'utawi', 'wiadin', 'sareng', 'ajak'}
SC = {'krana', 'sangkan', 'mawinan', 'sawireh', 'yening', 'yen', 'apang', 'mangda',
      'kanti', 'ngantos', 'sedurung', 'setonden', 'sesampun', 'suud', 'mara', 'wau',
      'sambilanga'}
DET = {'ene', 'ento', 'niki', 'nika', 'ne', 'niké', 'punika'}
PCLE = {'je', 'ja', 'to', 'ni', 'dong', 'nah', 'dogen', 'kemanten', 'tuah', 'wantah',
        'saja', 'yakti', 'suba', 'sampun', 'nui', 'kai'}
Q = {'dija', 'pidan', 'kenken', 'sapunapi', 'kuda', 'pinten', 'ngudiang', 'kija',
     'pundi', 'kude', 'encen', 'kengken', 'engken', 'ngapain', 'ngawinan'}
ADV = {'jani', 'mangkin', 'dibi', 'dibeng', 'mani', 'benjang', 'puan', 'tuni', 'iwau',
       'nyanan', 'malih', 'buin', 'kanti', 'ngantos', 'sampun', 'suba', 'durung',
       'tonden', 'mara', 'wau', 'malu', 'apisan', 'pesan', 'pisan', 'gati', 'sajan',
       'bes', 'liu', 'akeh', 'kidik', 'bedik', 'dogen', 'kemanten'}

def load_pos():
    pos = {}
    cache = json.load(io.open(CACHE, encoding='utf-8')) if os.path.exists(CACHE) else {}
    for word, wt in cache.items():
        if not wt or not isinstance(wt, str):
            continue
        m = re.findall(r'part of speech\s*=\s*([a-z ]+)', wt)
        if m:
            p = m[0].strip()
            pos[word.lower()] = {
                'verb': 'verb', 'noun': 'noun', 'adjective': 'adj', 'adverb': 'adv',
                'adjective verb': 'verb', 'numeral': 'num', 'pronoun': 'pron',
                'preposition': 'adp', 'conjunction': 'cconj', 'classifier': 'clf',
                'interjection': 'intj', 'proper noun': 'propn', 'demonstrative': 'det',
                'particle': 'pcle', 'negation': 'neg',
            }.get(p, p)
    return pos

def pos_of(tok, pos):
    """returns (tag, source) — source 'dict' when the BASAbali dictionary supplied it"""
    t = tok.lower()
    if t in pos:
        return pos[t], 'dict'
    if t in PRON or t in Q:
        return 'pron', 'list'
    if t in NUM or re.fullmatch(r'[a-z]*(belas|likur|olas|atus|tali)', t):
        return 'num', 'list'
    if t in ADP:
        return 'adp', 'list'
    if t in NEG:
        return 'neg', 'list'
    if t in SC:
        return 'sconj', 'list'
    if t in CC:
        return 'cconj', 'list'
    if t in DET:
        return 'det', 'list'
    if t in PCLE:
        return 'pcle', 'list'
    if t in ADV:
        return 'adv', 'list'
    if re.match(r'^(ma|ng|n|m|ny|me)[a-z]', t) and len(t) > 5:
        return 'verb', 'heuristic'
    if re.search(r'(ang|in)$', t) and len(t) > 5:
        return 'verb', 'heuristic'
    return 'noun', 'heuristic'

def tokens_of(bal):
    return [w for w in re.findall(r"[A-Za-zÀ-ÿ']+", bal)]

def parse(toks, pos):
    n = len(toks)
    tagged = [pos_of(t, pos) for t in toks]
    tags = [x[0] for x in tagged]
    srcs = [x[1] for x in tagged]
    head = [0] * n
    rel = ['dep'] * n
    conf = ['rule'] * n
    # lexical relations (a preposition/negator/particle/conjunction attaches to the next content word)
    for i, t in enumerate(tags):
        if t in ('adp', 'preposition'): t = tags[i] = 'adp'
        if t in ('cconj', 'conjunction'): t = tags[i] = 'cconj'
        if t in ('adp', 'neg', 'pcle', 'det', 'cconj', 'sconj'):
            j = i + 1
            while j < n and tags[j] in ('adp', 'neg', 'pcle', 'det', 'adv') and tags[j] != 'adv':
                j += 1
            if j < n:
                head[i] = j + 1
                rel[i] = {'adp': 'case', 'neg': 'advmod:neg', 'pcle': 'discourse',
                          'det': 'det', 'cconj': 'cc', 'sconj': 'mark'}[t]
                conf[i] = 'lex'
    # verb / predicate head
    vi = next((i for i, t in enumerate(tags) if t == 'verb'), None)
    if vi is not None:
        head[vi] = 0; rel[vi] = 'root'
    else:
        pi = next((i for i, t in enumerate(tags) if t == 'adj'), None)
        if pi is not None:
            head[pi] = 0; rel[pi] = 'root'
        else:
            pi = next((i for i, t in enumerate(tags) if t in ('noun', 'pron', 'num')), 0)
            head[pi] = 0; rel[pi] = 'root'
    root = next((i for i in range(n) if head[i] == 0 and rel[i] == 'root'), None)
    # first pre-verb nominal = subject, later nominals = object/oblique
    seen_obj = False
    for i, t in enumerate(tags):
        if i == root or rel[i] != 'dep':
            continue
        if t in ('noun', 'pron'):
            if not seen_obj and root is not None and i < root:
                head[i] = root + 1; rel[i] = 'nsubj'
            elif not seen_obj:
                head[i] = root + 1; rel[i] = 'obj'; seen_obj = True
            else:
                head[i] = root + 1; rel[i] = ('nmod:poss' if tags[i - 1] == 'noun' else 'obl')
        elif t == 'num':
            prev = i - 1
            if prev >= 0 and tags[prev] in ('noun', 'adj'):
                head[i] = prev + 1; rel[i] = 'nummod'
            elif i + 1 < n and tags[i + 1] == 'noun':
                head[i] = i + 2; rel[i] = 'nummod'
            else:
                head[i] = (root + 1) if root is not None else 0
                rel[i] = 'nummod'
        elif t == 'adj':
            prev = i - 1
            if prev >= 0 and tags[prev] in ('noun', 'pron'):
                head[i] = prev + 1; rel[i] = 'amod'          # modifier follows its noun
            else:
                head[i] = (root + 1) if root is not None else 0
                rel[i] = 'amod'
        elif t == 'verb':
            head[i] = (root + 1) if root is not None else 0
            rel[i] = 'xcomp'
        elif t == 'adv':
            head[i] = (root + 1) if root is not None else 0
            rel[i] = 'advmod'
    return tags, head, rel, conf, srcs

def main():
    pos = load_pos()
    rows, stats = [], collections.Counter()
    sentences = []
    for sec, path in SEC.items():
        lesson = None
        for line in io.open(path, encoding='utf-8'):
            m = re.match(r'### Lesson (\d+):', line)
            if m:
                lesson = int(m.group(1)); continue
            m = re.match(r'(\d+)\.\s+(.+?)\s+->\s+(.+)', line.strip())
            if m and lesson:
                sentences.append((sec, lesson, int(m.group(1)), m.group(2).strip()))
    for sec, L, num, bal in sentences:
        toks = tokens_of(bal)
        if not toks:
            continue
        tags, head, rel, conf, srcs = parse(toks, pos)
        sid = f"S{sec}-L{L:02d}-{num:03d}"
        for i, t in enumerate(toks):
            rows.append((sid, i + 1, t, tags[i], head[i], rel[i], conf[i], srcs[i]))
            stats[rel[i]] += 1
        stats['sentences'] += 1
    with io.open(OUT, 'w', encoding='utf-8') as f:
        f.write('sent_id\tindex\ttoken\tpos\thead\trelation\tconfidence\tpos_src\n')
        for r in rows:
            f.write('\t'.join(str(x) for x in r) + '\n')
    print(f"treebank: {stats['sentences']} sentences, {len(rows)} tokens -> {OUT}")
    print("pos tags:", collections.Counter(r[3] for r in rows).most_common())
    print("relations:", [(k, v) for k, v in stats.most_common() if k != 'sentences'])

if __name__ == '__main__':
    main()
