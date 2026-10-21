#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
build_content.py — Content pipeline for the BasaBali app.

Compiles the course files into tagged JSON assets:
  uploads/balinese_course_vocabulary.md
  balinese_course_sentences_section1.md  (Lessons 1-10)
  balinese_course_sentences_section2.md  (Lessons 11-30)
  balinese_course_sentences_section3.md  (Lessons 31-70)

Outputs (to app/src/main/assets/content/):
  words.json      — every vocabulary entry: id, first lesson, gloss, register
  patterns.json   — grammar pattern catalog (one or more per lesson)
  sentences.json  — full sentence bank; each sentence tagged with the word-entry
                    ids it contains, its grammar patterns, the single new entry
                    it introduces (curriculum guarantee), register, headword flag
  curriculum.json — sections -> lessons -> chunks (adaptive size per design doc)

The tokenizer/matcher mirrors validate*.py: greedy longest-entry match over the
cumulative vocabulary pool, plus suffix rules (-ne / -e / -n) and a name list.
"""
import json, os, re, sys, unicodedata
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from stories_src import STORIES

PROJ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))   # .../BasaBaliApp
ROOT = os.path.dirname(PROJ)                                          # ..."$ARENA_WORKSPACE" (course files)
UPLOADS = os.path.join(ROOT, 'uploads')
OUT = os.path.join(PROJ, 'app', 'src', 'main', 'assets', 'content')

SENT_FILES = [
    (1, 'balinese_course_sentences_section1.md'),
    (2, 'balinese_course_sentences_section2.md'),
    (3, 'balinese_course_sentences_section3.md'),
]

NAMES = {'wayan', 'made', 'nyoman', 'ketut', 'putu', 'kadek', 'gede', 'komang', 'agung', 'budi'}
EXC = {'cang': 'icang', 'druwen': 'druwe', 'jeron': 'jero', 'mawasta': 'mawasta',
       'rage': 'rage', 'malajah': 'malajah', 'pula': 'pula'}

# ---------------------------------------------------------------- normalization
def norm(s):
    s = s.lower().replace('é', 'e').replace('è', 'e').replace('…', ' ')
    s = re.sub(r"[^\w\s']", ' ', s)
    return ' '.join(s.split())

# ---------------------------------------------------------------- vocabulary
def parse_vocab():
    vocab, lesson = {}, None
    path = os.path.join(UPLOADS, 'balinese_course_vocabulary.md')
    for line in open(path, encoding='utf-8'):
        line = line.rstrip()
        m = re.match(r'#### Lesson (\d+):', line)
        if m:
            lesson = int(m.group(1)); vocab[lesson] = []
            continue
        if lesson and line.startswith('* '):
            parts = [p.strip() for p in line[2:].split('->')]
            entry = parts[0].rstrip('.').replace('...', '').strip()
            gloss = parts[1] if len(parts) > 1 else ''
            reg = parts[2] if len(parts) > 2 else ''
            vocab[lesson].append((norm(entry), gloss, reg))
    return vocab

def reg_norm(r):
    r = r.lower()
    for k in ('alus', 'andap', 'mider'):
        if k in r:
            return k
    return 'mider'

# ---------------------------------------------------------------- patterns
PATTERN_NAMES = {
    'P-zerocopula': 'No copula: Noun + Adjective',
    'P-neg-register': 'Negation: sing/tusing (Andap) vs ten/nenten (Alus)',
    'P-exist-ada': 'Existence: ada (Andap) / wenten (Alus)',
    'P-greet-time': 'Greetings: rahajeng + time of day',
    'P-q-sapunapi': 'How-questions: sapunapi / kenken',
    'P-polite-formulas': 'Polite formulas: suksma, ampura, durus',
    'P-imperative-soft': 'Soft imperatives: durus / raris / silakan',
    'P-name-qa': 'Asking & giving names',
    'P-origin-saking': 'Origin: saking / uli + place',
    'P-eat-register': 'Speech levels for eating: neda / ngajeng / madaar',
    'P-poss-gelah': 'Possession: thing + gelah/druwe(n) + owner',
    'P-give-dative': 'Give: baang/wehin + person + thing',
    'P-modal-dadi': 'Permission: dadi / dados',
    'P-num-noun': 'Number + noun order',
    'P-q-kuda': 'Quantity questions: kuda / pinten',
    'P-family-poss': 'Family terms with possessives',
    'P-q-words': 'The question-word system',
    'P-svo-basic': 'Basic S-V(-O) sentences with routine verbs',
    'P-speed-adv': 'Speed adverbs after the verb',
    'P-prep-di-ring': 'Location: di (Andap) / ring (Alus)',
    'P-prep-ka': 'Direction: ka (to a place)',
    'P-directional-kema': 'kema/mrika (go there), mai/mriki (come here)',
    'P-orientation-kaja': 'Balinese orientation: kaja/kelod/kangin/kauh',
    'P-locative-adj': 'Spatial adjectives: luhur/beten, ajeng/pungkur…',
    'P-time-adverb': 'Time adverbials: jani, dibi, mani…',
    'P-tense-by-time': 'Time words carry past/future meaning',
    'P-adj-order': 'Adjectives follow the noun',
    'P-intensifier-gati': 'Intensifier gati after adjectives',
    'P-feeling-pred': 'Feelings as predicates',
    'P-buy-sell': 'Buying & selling: meli/numbas, ngadep/ngadol',
    'P-body-poss': 'Body parts with possessives',
    'P-ache-sakit': 'Aches: body part + sakit/sungkan',
    'P-color-warnane': 'Colors: warnane + color; noun + color',
    'P-wear-nganggo': 'Wearing: nganggo/nganggen + garment',
    'P-price-qa': 'Price questions: aji kuda / kude regane',
    'P-future-lakar': 'Future with lakar',
    'P-num-price-tali': 'Prices with tali (thousands)',
    'P-age-tiban': 'Ages: number + tiban/warsa',
    'P-weather-pred': 'Weather predicates',
    'P-transitive-svo': 'Transitive verbs: S + V + O',
    'P-ditransfer-give': 'Indirect objects: give + person + thing',
    'P-ride-numpak': 'Riding: numpak/munggah/naek + vehicle',
    'P-prep-full': 'Full preposition system incl. duwur/beten/samping/antara',
    'P-companion-ajak': 'Companions: ajak / sareng / teken',
    'P-purpose-baang': 'Purpose: baang / pabuat / buat (for)',
    'P-loc-phrase': 'Location phrases in the house compound',
    'P-family-extended': 'Extended family sentences',
    'P-nature-describe': 'Describing nature & landscape',
    'P-conj-because': 'Cause: krana / sangkan / sawireh / mawinan',
    'P-conj-if': 'Conditionals: yening / yen',
    'P-conj-before-after': 'Sequence: sedurung/setonden, sesampun/suud',
    'P-past-taen': 'Experience: taen / suba taen / sing taen',
    'P-past-time-words': 'Past time words: dibi, ibak, mara, tuni…',
    'P-future-lakar-jagi': 'Future: lakar / kel / jagi (phrase)',
    'P-modal-bisa': 'Ability: bisa / nyidaang / mrasidayang',
    'P-profession-zero-copula': 'Professions with zero copula',
    'P-opinion-mapineh': 'Opinions: tiang mapineh / titiang mamanah',
    'P-ceremony-frame': 'Ceremony vocabulary in sentences',
    'P-taste-pred': 'Taste predicates: manis, pait, lalah…',
    'P-daily-verbs': 'Daily-life verb routines',
    'P-directions-imperative': 'Giving directions: belok, terus, dauh/dangin…',
    'P-comparative-an': 'Comparatives: -an suffix, lebih … teken',
    'P-superlative-paling': 'Superlatives: paling + adjective',
    'P-experience-kene-keto': 'Referring: kene / keto / kenten',
    'P-num-big': 'Large numbers & rupiah',
    'P-emergency-imperative': 'Emergency calls: tulung!, awas!, enggalang!',
    'P-love-ajak': 'Affection: sayang/tresna + ajak/ring',
    'P-communication': 'Communication verbs with objects',
    'P-material-pred': 'Materials as predicates: tembok bata',
    'P-creation-transitive': 'Creation verbs with objects',
    'P-festival-frame': 'Festival & ceremony frames',
    'P-cause-effect': 'Result clauses: dadi ne, ento krana, lantas',
    'P-reflexive-padidi': 'Reflexives: padidi / ngraga',
    'P-grooming-ma': 'Grooming ma- verbs (no object needed)',
    'P-environment': 'Environment talk & slogans (mangda …)',
    'P-imperative-prohib': 'Commands & prohibitions: da/eda/sampunang',
    'P-story-frame': 'Story frame: ada kone … lantas … suba keto',
    'P-arts': 'Arts & performance sentences',
    'P-abstract-nouns': 'Abstract nouns as subjects',
    'P-reported-speech': 'Reported speech: kone / jare / orahanga',
    'P-conditional': 'Advanced conditionals: asal, dogen/kemanten',
    'P-only-dogen': 'Limiting: dogen / kemanten',
    'P-cognition': 'Cognition verbs: inget, ngerti, percaya…',
    'P-sor-singgih': 'Sor-singgih: humble self vs honored other',
    'P-tourism': 'Tourism situations',
    'P-degree-adverbs': 'Degree: pisan, sajan, bes, tuah, hampir',
    'P-motion-verbs': 'Directional motion: munggah/tedun, pesu/medal, masuk/manjing',
    'P-home-register': 'Going home: mulih / mantuk / budal',
    'P-texture-pred': 'Texture predicates',
    'P-interjections': 'Interjections & sentence-final particles',
    'P-particles': 'Particles: to, ni, je, ja, dong',
    'P-business': 'Business & work sentences',
    'P-emotion-advanced': 'Advanced emotion predicates',
    'P-civic': 'Civics & governance sentences',
    'P-myth-frame': 'Myth & legend frames',
    'P-idioms': 'Idioms learned as whole chunks',
    'P-review-grammar': 'Grammar review mix',
    'P-review-phrases': 'Set-phrase review mix',
    # added from the treebank-based word-order audit
    'P-numeral-linker': 'Linking numerals: -ng form before a noun (duang, telung, patang…)',
    'P-q-insitu': 'Question words stay in place (in situ) instead of moving to the front',
    'P-conj-and': 'Coordination with lan / miwah / tur / muah',
    'P-reduplication': 'Reduplication: repeated form for plurality or intensity',
    'P-politeness-agree': 'Politeness agreement: honoured subject + Alus-singgih verb',
}

LESSON_PATTERNS = {
    1: ['P-zerocopula', 'P-neg-register', 'P-exist-ada'],
    2: ['P-greet-time', 'P-q-sapunapi'],
    3: ['P-polite-formulas', 'P-imperative-soft'],
    4: ['P-name-qa', 'P-origin-saking'],
    5: ['P-eat-register'],
    6: ['P-exist-ada'],
    7: ['P-poss-gelah', 'P-give-dative', 'P-modal-dadi'],
    8: ['P-num-noun', 'P-q-kuda'],
    9: ['P-family-poss'],
    10: ['P-q-words'],
    11: ['P-svo-basic', 'P-speed-adv'],
    12: ['P-prep-di-ring', 'P-prep-ka', 'P-directional-kema'],
    13: ['P-orientation-kaja', 'P-locative-adj'],
    14: ['P-time-adverb', 'P-tense-by-time'],
    15: ['P-adj-order', 'P-intensifier-gati'],
    16: ['P-feeling-pred'],
    17: ['P-buy-sell'],
    18: ['P-body-poss', 'P-ache-sakit'],
    19: ['P-color-warnane'],
    20: ['P-wear-nganggo'],
    21: ['P-price-qa', 'P-future-lakar'],
    22: ['P-num-price-tali', 'P-age-tiban'],
    23: ['P-weather-pred'],
    24: ['P-transitive-svo', 'P-ditransfer-give'],
    25: ['P-ride-numpak'],
    26: ['P-prep-full', 'P-companion-ajak', 'P-purpose-baang'],
    27: ['P-loc-phrase'],
    28: ['P-family-extended'],
    29: ['P-nature-describe'],
    30: ['P-conj-because', 'P-conj-if', 'P-conj-before-after'],
    31: ['P-past-taen', 'P-past-time-words'],
    32: ['P-future-lakar-jagi', 'P-modal-bisa'],
    33: ['P-profession-zero-copula'],
    34: ['P-opinion-mapineh'],
    35: ['P-ceremony-frame'],
    36: ['P-taste-pred'],
    37: ['P-daily-verbs'],
    38: ['P-directions-imperative'],
    39: ['P-comparative-an', 'P-superlative-paling'],
    40: ['P-experience-kene-keto'],
    41: ['P-num-big'],
    42: ['P-emergency-imperative'],
    43: ['P-love-ajak'],
    44: ['P-communication'],
    45: ['P-material-pred'],
    46: ['P-creation-transitive'],
    47: ['P-festival-frame'],
    48: ['P-cause-effect'],
    49: ['P-reflexive-padidi', 'P-grooming-ma'],
    50: ['P-environment'],
    51: ['P-imperative-prohib'],
    52: ['P-story-frame'],
    53: ['P-arts'],
    54: ['P-abstract-nouns'],
    55: ['P-reported-speech'],
    56: ['P-conditional', 'P-only-dogen'],
    57: ['P-cognition'],
    58: ['P-sor-singgih'],
    59: ['P-tourism'],
    60: ['P-degree-adverbs'],
    61: ['P-motion-verbs', 'P-home-register'],
    62: ['P-texture-pred'],
    63: ['P-interjections', 'P-particles'],
    64: ['P-business'],
    65: ['P-emotion-advanced'],
    66: ['P-civic'],
    67: ['P-myth-frame'],
    68: ['P-idioms'],
    69: ['P-review-grammar'],
    70: ['P-review-phrases'],
}

KEYWORD_PATTERNS = [
    ({'sing', 'ten', 'tusing', 'nenten'}, 'P-neg-register'),
    ({'gelah', 'druwe', 'druwen'}, 'P-poss-gelah'),
]

# ------------------------------------------------------------------ evidence-based patterns
# These were added after the parse-level audit (tools/build_treebank.py +
# tools/audit_word_order.py): instead of tagging every sentence of a lesson,
# each rule attaches its pattern only to the sentences that actually show the
# structure (validated against the treebank).
NUM_LINKERS = {'duang', 'telung', 'tigang', 'patang', 'limang', 'pitung', 'sangang'}
Q_WORDS = {'dija', 'pidan', 'kenken', 'sapunapi', 'kuda', 'pinten', 'ngudiang',
           'kija', 'pundi', 'kude', 'encen', 'sira', 'nyen', 'napi', 'apa', 'kengken',
           'engken', 'ngapain', 'ngawinan'}
CONJ_AND = {'lan', 'miwah', 'tur', 'muah'}
ALUS_SINGGIH_VERBS = {'ngajeng', 'ngunjuk', 'ngrayunang', 'malinggih', 'mantuk',
                      'seda', 'makolem', 'ngandika', 'nyingak', 'ngaksi', 'ledang'}

def _has_linker(toks, words):
    return any(t in NUM_LINKERS for t in toks)

def _q_in_situ(toks, words):
    return any(t in Q_WORDS for t in toks[1:])

def _coord_and(toks, words):
    return any(t in CONJ_AND for t in toks)

def _reduplication(toks, words):
    # norm() turns "jalan-jalan" into two identical tokens
    return any(toks[i] == toks[i + 1] for i in range(len(toks) - 1))

def _politeness_agreement(toks, words):
    s = set(toks)
    return bool(s & {'ida', 'ipun'}) and bool(s & ALUS_SINGGIH_VERBS)

SENTENCE_PATTERNS = [
    ('P-numeral-linker', _has_linker),
    ('P-q-insitu', _q_in_situ),
    ('P-conj-and', _coord_and),
    ('P-reduplication', _reduplication),
    ('P-politeness-agree', _politeness_agreement),
]

CHUNK_OVERRIDES = {8: 6, 22: 6, 41: 6, 68: 4}   # numbers: bigger; idioms: smaller
DEFAULT_CHUNK = 5

# ---------------------------------------------------------------- matcher
class Matcher:
    def __init__(self, entry_lesson):
        self.entry_lesson = entry_lesson
        self.all_entries = sorted(entry_lesson.keys(), key=lambda e: (-len(e.split()), e))

    def match(self, tokens, i, pool):
        for e in self.all_entries:
            w = e.split()
            if e in pool and tokens[i:i + len(w)] == w:
                return e, len(w)
        return None, 0

    def tag(self, tokens, pool, lesson_items=frozenset()):
        """Return the word-entry ids present in the sentence.

        Greedy longest-entry match; additionally tags single-token components of a
        matched multi-word entry when that component is an item of the CURRENT
        lesson (mirrors how the curriculum validators count occurrences, e.g.
        'sor' inside 'ring sor' in Lesson 26)."""
        words = []
        i = 0
        while i < len(tokens):
            e, ln = self.match(tokens, i, pool)
            if e:
                if e not in words:
                    words.append(e)
                if ln > 1:
                    for part in e.split(' '):
                        if part in lesson_items and part in pool and part not in words:
                            words.append(part)
                i += ln
                continue
            t = tokens[i]
            if t in NAMES or t in EXC:
                if t in EXC and EXC[t] in pool and EXC[t] not in words:
                    words.append(EXC[t])
                i += 1
                continue
            for suf in ('ne', 'e', 'n'):
                if t.endswith(suf) and len(t) > len(suf):
                    base = t[:-len(suf)]
                    e2, _ = self.match([base], 0, pool)
                    if e2:
                        if e2 not in words:
                            words.append(e2)
                        break
            i += 1
        return words

# ---------------------------------------------------------------- sentences
def parse_sentences_file(path, section):
    lessons, order = {}, {}
    lesson, title = None, None
    for line in open(path, encoding='utf-8'):
        m = re.match(r'### Lesson (\d+):\s*(.+)', line)
        if m:
            lesson = int(m.group(1))
            title = m.group(2).strip()
            lessons[lesson] = {'section': section, 'title': title, 'sents': []}
            continue
        if lesson is None:
            continue
        m = re.match(r'\*\*New in this lesson \(teaching order\):\*\*\s*(.+)', line.strip())
        if m:
            order[lesson] = m.group(1)
            continue
        m = re.match(r'(\d+)\.\s+(.+?)\s+->\s+(.+)', line.strip())
        if m:
            num = int(m.group(1))
            bal, en = m.group(2).strip(), m.group(3).strip()
            lessons[lesson]['sents'].append((num, bal, en))
    return lessons, order

def clean_en(en):
    en = re.sub(r'\[(?:recycled|review)[^\]]*\]', '', en)
    en = re.sub(r'\s{2,}', ' ', en).strip()
    return en

def parse_order(line):
    line = line.strip()
    items = []
    for part in line.split(','):
        p = part.strip()
        p = re.sub(r'\s*\(.*?\)\s*$', '', p)          # trailing annotation
        p = p.rstrip('.…').strip()
        p = p.replace('…', '').strip()
        if p:
            items.append(norm(p))
    return items

def display_form(bal):
    """Display text for the bank: keep the authored punctuation (commas, ?!.)
    and normalize whitespace / strip wrapping quotes only. The punctuation-free
    `tokens` list is still produced by norm() for matching."""
    s = ' '.join(bal.split())
    s = s.strip('"“”\'')
    return s

def majority_register(regs):
    """Deterministic majority register for a sentence without a new item.
    Ties resolve Alus > Andap > Mider, so a mixed sentence keeps the polite
    reading the course already labels it with."""
    cnt = {'alus': 0, 'andap': 0, 'mider': 0}
    for r in regs:
        if r in cnt:
            cnt[r] += 1
    if sum(cnt.values()) == 0:
        return 'mider'
    return max(cnt, key=lambda r: (cnt[r], r == 'alus', r == 'andap'))


# ---------------------------------------------------------------- stories
def build_stories(entry_lesson, matcher):
    stories_out, errors = [], []
    for st in STORIES:
        gate = st['gate']
        pool = {e for e, ll in entry_lesson.items() if ll <= gate}
        pats = list(LESSON_PATTERNS.get(gate, []))
        lines_out = []
        for li, line in enumerate(st['lines']):
            toks = norm(line['bal']).split()
            words = matcher.tag(toks, pool)
            # verify every token is accounted for (entry / suffix / name)
            i = 0
            while i < len(toks):
                e, ln = matcher.match(toks, i, pool)
                if e:
                    i += ln; continue
                t = toks[i]
                if t in NAMES or t in EXC:
                    i += 1; continue
                ok = False
                for suf in ('ne', 'e', 'n'):
                    if t.endswith(suf) and len(t) > len(suf):
                        e2, _ = matcher.match([t[:-len(suf)]], 0, pool)
                        if e2:
                            ok = True; break
                if not ok:
                    errors.append(f"{st['id']} line {li+1}: token '{t}' not known by lesson {gate}: {line['bal']}")
                i += 1
            lines_out.append({
                'bal': display_form(line['bal']),
                'en': line['en'],
                'tokens': toks,
                'words': words,
                'patterns': pats,
            })
        stories_out.append({
            'id': st['id'], 'title': st['title'], 'titleEn': st['titleEn'],
            'gate': gate, 'lines': lines_out, 'questions': st['questions'],
        })
    return stories_out, errors

# ---------------------------------------------------------------- main
def main():
    vocab = parse_vocab()
    entry_lesson, entry_gloss, entry_reg = {}, {}, {}
    for L in sorted(vocab):
        for e, gloss, reg in vocab[L]:
            if e not in entry_lesson:
                entry_lesson[e] = L
                entry_gloss[e] = gloss
                entry_reg[e] = reg_norm(reg)
    matcher = Matcher(entry_lesson)

    all_lessons, all_order = {}, {}
    for sec, fname in SENT_FILES:
        ls, od = parse_sentences_file(os.path.join(ROOT, fname), sec)
        all_lessons.update(ls)
        all_order.update(od)

    # ---- walk lessons in order, tag sentences
    known = set()                     # entries introduced so far (global)
    sentences_out = []
    warnings = []
    for L in sorted(all_lessons):
        info = all_lessons[L]
        pool = {e for e, ll in entry_lesson.items() if ll <= L}
        order_items = parse_order(all_order[L]) if L in all_order else [e for e, _, _ in vocab[L]]
        order_set = set(order_items)
        vocab_set = {e for e, _, _ in vocab[L]}
        if order_set != vocab_set:
            miss = vocab_set - order_set
            extra = order_set - vocab_set
            if miss:
                warnings.append(f"L{L}: vocab entries missing from teaching order: {sorted(miss)[:6]}")
                # append missing at end so chunking still covers them
                order_items += sorted(miss)
            if extra:
                warnings.append(f"L{L}: teaching order has unknown entries: {sorted(extra)[:6]}")
                order_items = [x for x in order_items if x in vocab_set]
        sec = info['section']
        for (num, bal, en) in info['sents']:
            toks = norm(bal).split()
            # tags = entries matched within cumulative pool (+ current-lesson components)
            words = matcher.tag(toks, pool, order_set)
            # new item: first token-run matching an entry not yet known
            new_item, i = None, 0
            while i < len(toks):
                e, ln = matcher.match(toks, i, pool)
                if e:
                    if e not in known:
                        known.add(e)
                        if new_item is None:
                            new_item = e
                    i += ln
                    continue
                t = toks[i]
                if t in NAMES or t in EXC:
                    i += 1; continue
                hit = False
                for suf in ('ne', 'e', 'n'):
                    if t.endswith(suf) and len(t) > len(suf):
                        e2, _ = matcher.match([t[:-len(suf)]], 0, pool)
                        if e2:
                            if e2 not in known:
                                known.add(e2)
                                if new_item is None:
                                    new_item = e2
                            hit = True
                            break
                i += 1
            # patterns
            pats = list(LESSON_PATTERNS.get(L, []))
            tset = set(toks)
            for keys, pid in KEYWORD_PATTERNS:
                if tset & keys and pid not in pats:
                    pats.append(pid)
            # evidence-based patterns: attached only where the structure occurs
            for pid, pred in SENTENCE_PATTERNS:
                if pid not in pats and pred(toks, toks):
                    pats.append(pid)
            headword = bool(words) and ' '.join(words) == ' '.join(toks) and len(words) >= 1 \
                       and norm(bal).rstrip(' .!?') in entry_lesson
            register = entry_reg.get(new_item, '') if new_item else ''
            if not register:
                register = majority_register([entry_reg[w] for w in words if w in entry_reg])
            sentences_out.append({
                'id': f"S{sec}-L{L:02d}-{num:03d}",
                'lesson': L, 'num': num,
                'bal': display_form(bal),
                'en': clean_en(en),
                'tokens': toks,
                'words': words,
                'patterns': pats,
                'newItem': new_item or '',
                'register': register,
                'headword': headword,
            })

    # ---- curriculum: lessons -> chunks
    curriculum = {'sections': []}
    sec_titles = {1: 'The Basics (A1)', 2: 'Building Sentences (A1+)', 3: 'Expanding Horizons (A2)'}
    for sec in (1, 2, 3):
        lessons_json = []
        for L in sorted(all_lessons):
            if all_lessons[L]['section'] != sec:
                continue
            items = parse_order(all_order[L]) if L in all_order else [e for e, _, _ in vocab[L]]
            vocab_set = {e for e, _, _ in vocab[L]}
            items = [x for x in items if x in vocab_set]
            missing = vocab_set - set(items)
            if missing:
                items += sorted(missing)
            size = CHUNK_OVERRIDES.get(L, DEFAULT_CHUNK)
            n = len(items)
            k = max(1, (n + size - 1) // size)
            base, rem = n // k, n % k
            chunks, idx = [], 0
            for c in range(k):
                sz = base + (1 if c < rem else 0)
                chunks.append(items[idx:idx + sz]); idx += sz
            lessons_json.append({
                'id': L,
                'title': all_lessons[L]['title'],
                'items': items,
                'chunks': chunks,
            })
        curriculum['sections'].append({'id': sec, 'title': sec_titles[sec], 'lessons': lessons_json})

    # ---- words & patterns json
    words_out = [{'id': e, 'lesson': entry_lesson[e], 'gloss': entry_gloss[e],
                  'register': entry_reg[e]} for e in sorted(entry_lesson, key=lambda x: (entry_lesson[x], x))]
    pat_first = {}
    for L in sorted(LESSON_PATTERNS):
        for p in LESSON_PATTERNS[L]:
            pat_first.setdefault(p, L)
    # evidence-based patterns get their first lesson from the sentences that show them
    for s in sentences_out:
        for p in s['patterns']:
            pat_first.setdefault(p, s['lesson'])
    patterns_out = [{'id': p, 'name': PATTERN_NAMES.get(p, p), 'lesson': pat_first[p]}
                    for p in sorted(pat_first, key=lambda x: (pat_first[x], x))]

    os.makedirs(OUT, exist_ok=True)
    def dump(name, obj):
        with open(os.path.join(OUT, name), 'w', encoding='utf-8') as f:
            json.dump(obj, f, ensure_ascii=False, separators=(',', ':'))
        print(f"  {name}: {os.path.getsize(os.path.join(OUT, name))//1024} KB")

    print("Writing assets:")
    dump('words.json', {'words': words_out})
    dump('patterns.json', {'patterns': patterns_out})
    dump('sentences.json', {'sentences': sentences_out})
    dump('curriculum.json', curriculum)

    stories_out, story_errors = build_stories(entry_lesson, matcher)
    stories_out.sort(key=lambda st: (st['gate'], st['title'].lower()))
    if story_errors:
        print("\nSTORY VOCABULARY VIOLATIONS (fix stories_src.py):")
        for e in story_errors:
            print("  ", e)
        sys.exit(1)
    dump('stories.json', {'stories': stories_out})

    # ---- stats & sanity
    print(f"stories validated: {len(STORIES)}")
    print(f"\nStats: {len(words_out)} word entries, {len(patterns_out)} patterns, "
          f"{len(sentences_out)} sentences, "
          f"{sum(len(l['chunks']) for s in curriculum['sections'] for l in s['lessons'])} chunks")
    ev = {}
    for s in sentences_out:
        for p in s['patterns']:
            ev[p] = ev.get(p, 0) + 1
    print("evidence-based patterns:",
          {pid: ev.get(pid, 0) for pid, _ in SENTENCE_PATTERNS})
    no_tags = [s['id'] for s in sentences_out if not s['words']]
    print(f"Sentences with zero word tags: {len(no_tags)} {no_tags[:5]}")
    multi_new = 0
    headwords = sum(1 for s in sentences_out if s['headword'])
    print(f"Headword sentences: {headwords}")
    if warnings:
        print("\nWarnings:")
        for w in warnings[:20]:
            print(" ", w)
    else:
        print("No warnings.")

if __name__ == '__main__':
    main()
