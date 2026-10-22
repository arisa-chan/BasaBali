#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
enrich_sentences.py — practice sentences built from frames.

Each generated sentence comes from one of ten frames.  Its words are picked from
curated lists (subject, verb, object, place, time, adjective) and the Balinese
and the English are written from the same slots, so the two always agree:

  time        Semeng, icang madaar nasi.           In the morning, I eat rice.
  place       Icang madaar nasi di warung.         I eat rice at the warung.
  companion   Icang majalan ka pasar ajak timpal icang.  I walk to the market with my friend.
  purpose     Icang majalan ka pasar meli jukut.   I walk to the market to buy vegetables.
  sequence    Sesampun madaar, icang mulih.        After eating, I go home.
  reason      Icang luwe krana durung madaar.      I am hungry because I have not eaten yet.
  condition   Yen peteng, icang mulih.              If it gets dark, I go home.
  contrast    Icang gede, sakewanten cai cenik.    I am big, but you are small.
  existence   Ada patang sampi ring carik.          There are four cows in the rice field.
  possession  Buku niki gelah tiang.                This book is mine.

A sentence is filed under the lowest lesson whose vocabulary covers all of its
words, so it only uses words from Lessons 1..N.  Generated sentences are appended
after the authored sentences of that lesson.  Authored sentences that use words
outside the vocabulary scope, or repeat an earlier sentence of the same lesson
(same Balinese and English), are removed.

Run: python3 BasaBaliApp/tools/enrich_sentences.py
(revise_lessons.py rebuilds the authored baseline and vocabulary first.)
"""
import collections
import os
import random
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)
import revise_lessons as rv

VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
SENT_FILES = ['balinese_course_sentences_section1.md',
              'balinese_course_sentences_section2.md',
              'balinese_course_sentences_section3.md']
SENT_RE = re.compile(r'(\d+)\.\s+(.+?)\s+->\s+(.+)')
LESSON_RE = re.compile(r'### Lesson (\d+):')
SEED = 2026
MAX_LESSON = 70
TARGET = {'time': 110, 'place': 170, 'companion': 90, 'purpose': 90, 'sequence': 90,
          'reason': 50, 'condition': 45, 'contrast': 40, 'existence': 60, 'possession': 35}
PER_LESSON = 14
PER_TYPE_LESSON = 4
USE_CAP = {'v': 45, 'o': 36, 'p': 30, 'c': 25, 'pd': 9}

SUBJECTS = {
    # bal: english, copula, third person, register (A = Andap, H = Alus), owner, companion suffix
    'icang': dict(en='I', be='am', third=False, reg='A', owner='tiang', poss=' icang', poss_en='my', mine='mine'),
    'tiang': dict(en='I', be='am', third=False, reg='H', owner='tiang', poss=' tiang', poss_en='my', mine='mine'),
    'cai': dict(en='you', be='are', third=False, reg='A', owner='cai', poss=' caine', poss_en='your', mine='yours'),
    'ragane': dict(en='you', be='are', third=False, reg='H', owner='ragane', poss=' ragane', poss_en='your', mine='yours'),
    'ia': dict(en='he / she', be='is', third=True, reg='A', owner='ia', poss='ne', poss_en='his / her', mine='his / hers'),
    'ipun': dict(en='he / she', be='is', third=True, reg='H', owner='ipun', poss='ne', poss_en='his / her', mine='his / hers'),
    'ida': dict(en='he / she (honored)', be='is', third=True, reg='H', owner='ida', poss='ne', poss_en='his / her',
                mine='his / hers (honored)'),
}

# bal: (english base, -ing, -s, register, category)
VERBS = {
    'majalan': ('walk', 'walking', 'walks', 'A', 'move'),
    'memargi': ('walk', 'walking', 'walks', 'H', 'move'),
    'melali': ('travel', 'traveling', 'travels', 'AH', 'travel'),
    'mulih': ('go home', 'going home', 'goes home', 'A', 'home'),
    'mantuk': ('go home', 'going home', 'goes home', 'H', 'home'),
    'madaar': ('eat', 'eating', 'eats', 'A', 'eat'),
    'ngajeng': ('eat', 'eating', 'eats', 'H', 'eat'),
    'nginum': ('drink', 'drinking', 'drinks', 'A', 'drink'),
    'ngunjuk': ('drink', 'drinking', 'drinks', 'H', 'drink'),
    'mandi': ('bathe', 'bathing', 'bathes', 'A', 'bathe'),
    'manjus': ('bathe', 'bathing', 'bathes', 'A', 'bathe'),
    'masiram': ('bathe', 'bathing', 'bathes', 'H', 'bathe'),
    'pules': ('sleep', 'sleeping', 'sleeps', 'A', 'sleep'),
    'medem': ('sleep', 'sleeping', 'sleeps', 'AH', 'sleep'),
    'sirep': ('sleep', 'sleeping', 'sleeps', 'H', 'sleep'),
    'bangun': ('wake up', 'waking up', 'wakes up', 'A', 'wake'),
    'matangi': ('wake up', 'waking up', 'wakes up', 'H', 'wake'),
    'meli': ('buy', 'buying', 'buys', 'A', 'buy'),
    'numbas': ('buy', 'buying', 'buys', 'H', 'buy'),
    'ngadep': ('sell', 'selling', 'sells', 'A', 'sell'),
    'ngadol': ('sell', 'selling', 'sells', 'H', 'sell'),
    'malajah': ('learn', 'learning', 'learns', 'AH', 'study'),
    'megawe': ('work', 'working', 'works', 'A', 'work'),
    'megae': ('work', 'working', 'works', 'A', 'work'),
    'makarya': ('work', 'working', 'works', 'H', 'work'),
    'negak': ('sit', 'sitting', 'sits', 'A', 'sit'),
    'malinggih': ('sit', 'sitting', 'sits', 'H', 'sit'),
    'maca': ('read', 'reading', 'reads', 'AH', 'read'),
    'nulis': ('write', 'writing', 'writes', 'AH', 'write'),
    'nari': ('dance', 'dancing', 'dances', 'AH', 'dance'),
    'ngigel': ('dance', 'dancing', 'dances', 'AH', 'dance'),
    'mabalih': ('watch', 'watching', 'watches', 'AH', 'watch'),
    'nonton': ('watch', 'watching', 'watches', 'AH', 'watch'),
    'mebakti': ('pray', 'praying', 'prays', 'A', 'pray'),
    'muspa': ('pray', 'praying', 'prays', 'H', 'pray'),
    'ngaba': ('bring', 'bringing', 'brings', 'A', 'carry'),
    'makta': ('bring', 'bringing', 'brings', 'H', 'carry'),
    'numpak': ('ride', 'riding', 'rides', 'AH', 'ride'),
    'teka': ('come', 'coming', 'comes', 'A', 'come'),
    'rauh': ('come', 'coming', 'comes', 'H', 'come'),
}
# Register partners: an Andap verb used with an Alus subject, and the reverse.
TO_H = {'madaar': 'ngajeng', 'nginum': 'ngunjuk', 'mandi': 'masiram', 'manjus': 'masiram',
        'pules': 'sirep', 'bangun': 'matangi', 'meli': 'numbas', 'ngadep': 'ngadol',
        'megawe': 'makarya', 'megae': 'makarya', 'mulih': 'mantuk', 'majalan': 'memargi',
        'negak': 'malinggih', 'mebakti': 'muspa', 'ngaba': 'makta', 'teka': 'rauh'}
TO_A = {'ngajeng': 'madaar', 'ngunjuk': 'nginum', 'masiram': 'mandi', 'sirep': 'pules',
        'matangi': 'bangun', 'numbas': 'meli', 'ngadol': 'ngadep', 'makarya': 'megawe',
        'mantuk': 'mulih', 'memargi': 'majalan', 'malinggih': 'negak', 'muspa': 'mebakti',
        'makta': 'ngaba', 'rauh': 'teka'}

# bal: (English with article, English bare, English plural, uncountable, verb categories)
OBJECTS = {
    'nasi': ('rice', 'rice', 'rice', True, {'eat', 'buy', 'carry'}),
    'be': ('fish', 'fish', 'fish', True, {'eat', 'buy', 'sell'}),
    'iwak': ('fish', 'fish', 'fish', True, {'eat', 'buy', 'sell'}),
    'jukut': ('vegetables', 'vegetables', 'vegetables', True, {'eat', 'buy', 'sell'}),
    'buah': ('fruit', 'fruit', 'fruit', True, {'eat', 'buy', 'sell', 'carry'}),
    'taluh': ('eggs', 'eggs', 'eggs', True, {'eat', 'buy', 'sell'}),
    'tahu': ('tofu', 'tofu', 'tofu', True, {'eat', 'buy', 'sell'}),
    'tempe': ('tempeh', 'tempeh', 'tempeh', True, {'eat', 'buy', 'sell'}),
    'bawang': ('garlic', 'garlic', 'garlic', True, {'buy', 'sell'}),
    'lombok': ('chili', 'chili', 'chili', True, {'buy', 'sell'}),
    'gula': ('sugar', 'sugar', 'sugar', True, {'buy', 'sell'}),
    'uyah': ('salt', 'salt', 'salt', True, {'buy', 'sell'}),
    'toya': ('water', 'water', 'water', True, {'drink'}),
    'yeh': ('water', 'water', 'water', True, {'drink'}),
    'kopi': ('coffee', 'coffee', 'coffee', True, {'drink', 'buy'}),
    'teh': ('tea', 'tea', 'tea', True, {'drink', 'buy'}),
    'basa Bali': ('Balinese', 'Balinese', 'Balinese', True, {'study'}),
    'buku': ('a book', 'book', 'books', False, {'buy', 'carry', 'read', 'write', 'sell'}),
    'baju': ('a shirt', 'shirt', 'shirts', False, {'buy', 'carry', 'sell'}),
    'kamen': ('a sarong', 'sarong', 'sarongs', False, {'buy', 'carry', 'sell'}),
    'sepatu': ('a pair of shoes', 'shoes', 'pairs of shoes', False, {'buy', 'carry'}),
    'piring': ('a plate', 'plate', 'plates', False, {'buy', 'carry'}),
    'gelas': ('a glass', 'glass', 'glasses', False, {'buy', 'carry'}),
    'sendok': ('a spoon', 'spoon', 'spoons', False, {'buy', 'carry'}),
    'montor': ('a motorbike', 'motorbike', 'motorbikes', False, {'buy', 'sell', 'ride'}),
    'sepeda': ('a bicycle', 'bicycle', 'bicycles', False, {'buy', 'sell', 'ride'}),
    'bemo': ('a minibus', 'minibus', 'minibuses', False, {'ride'}),
}
OBJ_CATS = {'eat', 'drink', 'buy', 'sell', 'carry', 'read', 'write', 'ride', 'study'}
REQUIRE_OBJ = {'ride', 'carry'}
NUMS = [('duang', 'two'), ('telung', 'three'), ('patang', 'four'), ('limang', 'five'), ('pitung', 'seven')]

# bal: (at / in / on phrase, to phrase)
PLACES = {
    'pasar': ('at the market', 'to the market'),
    'peken': ('at the market', 'to the market'),
    'toko': ('at the shop', 'to the shop'),
    'warung': ('at the warung', 'to the warung'),
    'sekolah': ('at school', 'to school'),
    'kantor': ('at the office', 'to the office'),
    'pura': ('at the temple', 'to the temple'),
    'merajan': ('at the family temple', 'to the family temple'),
    'tukad': ('at the river', 'to the river'),
    'pasih': ('at the beach', 'to the beach'),
    'carik': ('in the rice field', 'to the rice field'),
    'alas': ('in the forest', 'to the forest'),
    'banjar': ('at the banjar hall', 'to the banjar hall'),
    'desa': ('in the village', 'to the village'),
    'kota': ('in the city', 'to the city'),
    'umah': ('at home', 'home'),
    'jaba': ('outside', 'outside'),
    'kasur': ('on the bed', 'to the bed'),
    'kursi': ('on the chair', 'to the chair'),
    'meja': ('on the table', 'to the table'),
}
MOVE_CATS = {'move', 'carry', 'travel'}
PLACE_BY_CAT = {
    'move': ['pasar', 'peken', 'toko', 'warung', 'sekolah', 'kantor', 'pura', 'merajan',
             'tukad', 'pasih', 'carik', 'alas', 'banjar', 'desa', 'kota'],
    'travel': ['alas', 'pasih', 'desa', 'kota', 'tukad'],
    'carry': ['pasar', 'sekolah', 'kantor', 'umah'],
    'eat': ['warung', 'umah', 'kantor', 'jaba'],
    'drink': ['warung', 'umah', 'jaba'],
    'bathe': ['tukad', 'pasih', 'umah'],
    'sleep': ['umah', 'kasur'],
    'buy': ['pasar', 'peken', 'toko', 'warung'],
    'sell': ['pasar', 'peken', 'warung'],
    'study': ['sekolah', 'umah'],
    'work': ['kantor', 'carik', 'toko', 'warung'],
    'sit': ['umah', 'jaba', 'banjar', 'kursi'],
    'read': ['umah', 'sekolah'],
    'dance': ['banjar', 'pura'],
    'watch': ['banjar', 'umah', 'pura'],
    'pray': ['pura', 'merajan'],
}

COMPS = [('timpal', 'friend'), ('meme', 'mother'), ('bapa', 'father'), ('beli', 'older brother'),
         ('adi', 'younger sibling'), ('mbok', 'older sister')]
COMP_VERBS = ['majalan', 'madaar', 'nginum', 'meli', 'ngadep', 'malajah', 'megawe', 'nari',
              'ngigel', 'nonton', 'numpak', 'melali']
PURPOSE = [('pasar', 'meli'), ('pasar', 'ngadep'), ('toko', 'meli'), ('warung', 'madaar'),
           ('warung', 'nginum'), ('sekolah', 'malajah'), ('sekolah', 'maca'), ('kantor', 'megawe'),
           ('carik', 'megae'), ('pura', 'mebakti'), ('merajan', 'mebakti'), ('tukad', 'manjus'),
           ('tukad', 'mandi'), ('pasih', 'manjus'), ('banjar', 'nari'), ('banjar', 'ngigel'),
           ('banjar', 'nonton')]
AFTER = [('madaar', 'pules'), ('madaar', 'mulih'), ('madaar', 'mandi'), ('madaar', 'malajah'),
         ('madaar', 'megawe'), ('nginum', 'mulih'), ('mandi', 'madaar'), ('mandi', 'nginum'),
         ('megawe', 'mulih'), ('megawe', 'mandi'), ('malajah', 'mulih'), ('meli', 'mulih'),
         ('bangun', 'mandi'), ('bangun', 'madaar'), ('pules', 'bangun'), ('muspa', 'mulih'),
         ('nari', 'mandi'), ('maca', 'pules'), ('ngadep', 'mulih')]
BEFORE = [('pules', 'mandi'), ('pules', 'madaar'), ('madaar', 'mandi'), ('madaar', 'nginum'),
          ('mulih', 'meli'), ('mulih', 'madaar'), ('malajah', 'madaar'), ('megawe', 'madaar'),
          ('nari', 'madaar'), ('muspa', 'madaar'), ('negak', 'madaar')]
# Short authored-style sentences that bring two entries up to three per lesson.
EXTRA = [('Cuaca niki becik.', 'This weather is good.'),
         ('Angin nika dingin.', 'That wind is cold.'),
         ('Suksma mewali, Wayan.', "You're welcome, Wayan.")]
TIMES = [('semeng', 'In the morning'), ('tengai', 'In the afternoon'),
         ('sanja', 'In the evening'), ('wengi', 'At night')]

ADJ_EN = {'gede': 'big', 'cenik': 'small', 'tegeh': 'tall', 'endep': 'short', 'sugih': 'rich',
          'tiwas': 'poor', 'liang': 'happy', 'sebet': 'sad', 'seneng': 'glad', 'sedih': 'sad',
          'anyar': 'new', 'kuna': 'old'}
PERSON_ADJ = [('gede', 'cenik'), ('tegeh', 'endep'), ('sugih', 'tiwas'), ('liang', 'sebet'),
              ('seneng', 'sedih')]
OBJ_ADJ = [('gede', 'cenik'), ('anyar', 'kuna')]
CONTRAST_OBJ = {'umah': 'house', 'montor': 'motorbike', 'sepeda': 'bicycle', 'buku': 'book',
                'baju': 'shirt'}
COND = [  # condition, English condition, consequence verb, negated?, place?
    ('peteng', 'it gets dark', 'mulih', False, False),
    ('peteng', 'it gets dark', 'pules', False, False),
    ('ujan', 'it rains', 'majalan', True, False),
    ('ujan', 'it rains', 'negak', False, True),
    ('dingin', 'it is cold', 'mandi', True, False),
    ('wengi', 'night falls', 'pules', False, False),
    ('semeng', 'morning comes', 'mandi', False, False),
]
EXIST_COUNT = {  # bal: (English plural, places)
    'sampi': ('cows', ['carik', 'alas', 'desa']),
    'celeng': ('pigs', ['carik', 'alas']),
    'ayam': ('chickens', ['carik', 'umah', 'pasar']),
    'bebek': ('ducks', ['carik', 'tukad']),
    'buku': ('books', ['meja', 'kasur', 'umah', 'sekolah']),
    'piring': ('plates', ['meja', 'warung', 'umah']),
    'gelas': ('glasses', ['meja', 'warung', 'umah']),
    'sendok': ('spoons', ['meja', 'warung']),
    'kursi': ('chairs', ['warung', 'banjar', 'umah']),
    'bantal': ('pillows', ['kasur', 'umah']),
    'lampu': ('lamps', ['umah', 'banjar', 'kantor']),
    'montor': ('motorbikes', ['pasar', 'toko', 'kantor', 'warung']),
    'sepeda': ('bicycles', ['pasar', 'sekolah', 'kantor']),
    'baju': ('shirts', ['toko', 'pasar', 'umah']),
}
EXIST_MASS = {  # bal: (English, copula, places)
    'nasi': ('rice', 'is', ['meja', 'warung', 'umah']),
    'toya': ('water', 'is', ['umah', 'warung', 'banjar']),
    'kopi': ('coffee', 'is', ['warung', 'umah', 'toko']),
    'jukut': ('vegetables', 'are', ['pasar', 'warung', 'umah']),
    'taluh': ('eggs', 'are', ['pasar', 'toko', 'warung']),
    'buah': ('fruit', 'is', ['pasar', 'toko', 'umah']),
}
POSS_OBJ = {'buku': 'book', 'baju': 'shirt', 'montor': 'motorbike', 'sepeda': 'bicycle',
            'kamen': 'sarong', 'piring': 'plate', 'gelas': 'glass', 'sendok': 'spoon',
            'bantal': 'pillow', 'kursi': 'chair'}
REASON_STATES = {  # (Andap, Alus) state adjective and English
    'hungry': (('luwe', 'hungry'), ('seduk', 'hungry')),
    'tired': (('kenyel', 'tired'), ('lesu', 'tired')),
    'scared': (('jejeh', 'scared'), ('ajrih', 'scared')),
    'happy': (('liang', 'happy'), ('seneng', 'glad')),
}

FIRST = {}
_INDEX = {}


def vocab_index():
    _, sections = rv.parse_vocab(VOCAB)
    first = {}
    for sec in sections:
        for les in sec['lessons']:
            lid = int(re.match(r'#### Lesson (\d+):', les['header']).group(1))
            for e in les['entries']:
                first.setdefault(e['id'], lid)
    return first


def scope_index(L):
    if L not in _INDEX:
        idx = collections.defaultdict(list)
        for e, fl in FIRST.items():
            if fl <= L:
                words = e.split()
                idx[words[0]].append(words)
        for lst in idx.values():
            lst.sort(key=len, reverse=True)
        _INDEX[L] = idx
    return _INDEX[L]


def suffix_ok(t, idx):
    for suf in ('ne', 'e', 'n'):
        if t.endswith(suf) and len(t) > len(suf):
            base = t[:-len(suf)]
            if [base] in idx.get(base, ()):
                return True
    return False


def scope_bad(toks, L):
    """Tokens not covered by an entry taught in Lessons 1..L (same rule as revise_lessons)."""
    idx = scope_index(L)
    bad, i = [], 0
    while i < len(toks):
        t = toks[i]
        step = 0
        for words in idx.get(t, ()):
            if toks[i:i + len(words)] == words:
                step = len(words)
                break
        if step:
            i += step
            continue
        if t in rv.NAMES or suffix_ok(t, idx):
            i += 1
            continue
        if t in rv.EXC and FIRST.get(rv.EXC[t], 99) <= L:
            i += 1
            continue
        bad.append(t)
        i += 1
    return bad


def lesson_of(bal):
    toks = rv.norm(bal).split()
    for L in range(1, MAX_LESSON + 1):
        if not scope_bad(toks, L):
            return L
    return None


def cap(s):
    return s[:1].upper() + s[1:]


def reg_verb(key, subj):
    reg = SUBJECTS[subj]['reg']
    vreg = VERBS[key][3]
    if vreg == 'AH' or vreg == reg:
        return key
    return TO_H.get(key) if reg == 'H' else TO_A.get(key)


VEHICLES = {'montor', 'sepeda', 'bemo'}
LANGUAGE = {'basa Bali'}


def pick_object(rng, cat, avoid=()):
    options = [k for k, v in OBJECTS.items() if cat in v[4] and k not in avoid]
    return rng.choice(options) if options else None


def predicate(rng, subj, key, want_place=False, avoid=LANGUAGE):
    """Verb with optional object and place, in the register of the subject."""
    vk = reg_verb(key, subj)
    if vk is None:
        return None
    s = SUBJECTS[subj]
    base, ving, s3, _, cat = VERBS[vk]
    pr = dict(vb=vk, base=base, ving=ving, ve=s3 if s['third'] else base,
              ob='', oe='', pb='', pe='', fr=False, keys=[('v', vk)])
    okey = pick_object(rng, cat, avoid) if cat in OBJ_CATS and (cat in REQUIRE_OBJ or rng.random() < 0.6) else None
    if okey:
        art, bare, plural, mass, _ = OBJECTS[okey]
        num = rng.choice(NUMS) if not mass and rng.random() < 0.3 else None
        pr['ob'] = f"{num[0]} {okey}" if num else okey
        pr['oe'] = f"{num[1]} {plural}" if num else (bare if mass else art)
        pr['keys'].append(('o', okey))
    if want_place:
        opts = PLACE_BY_CAT.get(cat, [])
        if not opts:
            return None
        p = rng.choice(opts)
        at_en, to_en = PLACES[p]
        if cat in MOVE_CATS:
            pr['pb'], pr['pe'] = f"ka {p}", to_en
        else:
            pr['pb'] = f"{'di' if s['reg'] == 'A' else 'ring'} {p}"
            pr['pe'], pr['fr'] = at_en, True
        pr['keys'].append(('p', p))
    return pr


def clause_bal(subj, pr):
    return ' '.join(x for x in (subj, pr['vb'], pr['ob'], pr['pb']) if x)


def clause_en(subj, pr):
    return ' '.join(x for x in (SUBJECTS[subj]['en'], pr['ve'], pr['oe'], pr['pe']) if x)


TIME_VERBS = [k for k, v in VERBS.items() if v[4] != 'watch']


def f_time(rng):
    subj = rng.choice(list(SUBJECTS))
    pr = predicate(rng, subj, rng.choice(TIME_VERBS), want_place=rng.random() < 0.3)
    if pr is None:
        return None
    t_bal, t_en = rng.choice(TIMES)
    return dict(bal=cap(f"{t_bal}, {clause_bal(subj, pr)}"), en=f"{t_en}, {clause_en(subj, pr)}",
                keys=pr['keys'])


def f_place(rng):
    subj = rng.choice(list(SUBJECTS))
    pr = predicate(rng, subj, rng.choice(list(VERBS)), want_place=True)
    if pr is None:
        return None
    if pr['fr'] and rng.random() < 0.35:
        rest_bal = ' '.join(x for x in (subj, pr['vb'], pr['ob']) if x)
        rest_en = ' '.join(x for x in (SUBJECTS[subj]['en'], pr['ve'], pr['oe']) if x)
        bal = cap(f"{pr['pb']}, {rest_bal}")
        en = f"{cap(pr['pe'])}, {rest_en}"
    else:
        bal, en = clause_bal(subj, pr), clause_en(subj, pr)
    return dict(bal=cap(bal), en=en, keys=pr['keys'])


def f_companion(rng):
    subj = rng.choice(list(SUBJECTS))
    s = SUBJECTS[subj]
    pr = predicate(rng, subj, rng.choice(COMP_VERBS), want_place=rng.random() < 0.4)
    if pr is None:
        return None
    noun, noun_en = rng.choice(COMPS)
    bal = f"{clause_bal(subj, pr)} ajak {noun}{s['poss']}"
    en = f"{clause_en(subj, pr)} with {s['poss_en']} {noun_en}"
    return dict(bal=cap(bal), en=en, keys=pr['keys'] + [('c', noun)])


def f_purpose(rng):
    subj = rng.choice(list(SUBJECTS))
    dest, v2 = rng.choice(PURPOSE)
    mv = predicate(rng, subj, 'majalan')
    p2 = predicate(rng, subj, v2, avoid=VEHICLES) if v2 != 'malajah' else predicate(rng, subj, v2, avoid=())
    if mv is None or p2 is None:
        return None
    bal = ' '.join(x for x in (subj, mv['vb'], 'ka', dest, p2['vb'], p2['ob']) if x)
    en = ' '.join(x for x in (SUBJECTS[subj]['en'], mv['ve'], PLACES[dest][1], 'to', p2['base'], p2['oe']) if x)
    return dict(bal=cap(bal), en=en, keys=p2['keys'] + [('p', dest), ('pd', dest)])


def f_sequence(rng):
    subj = rng.choice(list(SUBJECTS))
    after = rng.random() < 0.6
    v1, v2 = rng.choice(AFTER if after else BEFORE)
    p1 = predicate(rng, subj, v1)
    p2 = predicate(rng, subj, v2)
    if p1 is None or p2 is None:
        return None
    conn_bal, conn_en = ('sesampun', 'After') if after else ('sedurung', 'Before')
    b1 = ' '.join(x for x in (p1['vb'], p1['ob']) if x)
    e1 = ' '.join(x for x in (p1['ving'], p1['oe']) if x)
    b2 = ' '.join(x for x in (p2['vb'], p2['ob']) if x)
    e2 = ' '.join(x for x in (p2['ve'], p2['oe']) if x)
    bal = f"{conn_bal} {b1}, {subj} {b2}"
    en = f"{conn_en} {e1}, {SUBJECTS[subj]['en']} {e2}"
    return dict(bal=cap(bal), en=en, keys=p1['keys'] + p2['keys'])


def f_reason(rng):
    subj = rng.choice(list(SUBJECTS))
    s = SUBJECTS[subj]
    h = s['reg'] == 'H'
    conj = 'mawinan' if h else rng.choice(['krana', 'sawireh'])
    have = 'has' if s['third'] else 'have'
    kind = rng.randrange(6)
    keys = []
    if kind == 5:
        return dict(bal="Dingin sawireh ujan", en="It is cold because it is raining", keys=[])
    if kind in (0, 1):
        st, st_en = REASON_STATES['hungry'][1 if h else 0]
        eat = reg_verb('madaar', subj)
        if eat is None:
            return None
        if kind == 0:
            clause, clause_en_ = f"durung {eat}", f"{s['en']} {have} not eaten yet"
        else:
            clause, clause_en_ = f"{'nenten' if h else 'ten'} {eat}", f"{s['en']} did not eat"
        keys = [('v', eat)]
    elif kind == 2:
        st, st_en = REASON_STATES['tired'][1 if h else 0]
        work = reg_verb('megawe', subj)
        if work is None:
            return None
        clause, clause_en_ = work, f"{s['en']} worked"
        keys = [('v', work)]
    elif kind == 3:
        st, st_en = REASON_STATES['scared'][1 if h else 0]
        clause, clause_en_ = 'peteng', 'it is dark'
    else:
        other = rng.choice([k for k in SUBJECTS if k != subj])
        st, st_en = REASON_STATES['happy'][1 if h else 0]
        came = reg_verb('teka', other)
        clause, clause_en_ = f"{other} {came}", f"{SUBJECTS[other]['en']} came"
    bal = f"{subj} {st} {conj} {clause}"
    en = f"{cap(s['en'])} {s['be']} {st_en} because {clause_en_}"
    return dict(bal=cap(bal), en=en, keys=keys)


def f_condition(rng):
    subj = rng.choice(list(SUBJECTS))
    s = SUBJECTS[subj]
    conj = 'yening' if s['reg'] == 'H' else 'yen'
    if rng.random() < 0.2:
        st, st_en = REASON_STATES['hungry'][1 if s['reg'] == 'H' else 0]
        pr = predicate(rng, subj, 'madaar')
        if pr is None:
            return None
        bal = f"{conj} {st}, {clause_bal(subj, pr)}"
        en = f"If {s['en']} {s['be']} {st_en}, {clause_en(subj, pr)}"
        return dict(bal=cap(bal), en=en, keys=pr['keys'])
    cond, cond_en, vk, neg, want_place = rng.choice(COND)
    pr = predicate(rng, subj, vk, want_place=want_place)
    if pr is None:
        return None
    if neg:
        neg_bal = 'nenten' if s['reg'] == 'H' else 'ten'
        body_bal = ' '.join(x for x in (subj, neg_bal, pr['vb'], pr['ob'], pr['pb']) if x)
        aux = 'does not' if s['third'] else 'do not'
        body_en = ' '.join(x for x in (s['en'], aux, pr['base'], pr['oe'], pr['pe']) if x)
    else:
        body_bal, body_en = clause_bal(subj, pr), clause_en(subj, pr)
    bal = f"{cap(conj)} {cond}, {body_bal}"
    en = f"If {cond_en}, {body_en}"
    return dict(bal=bal, en=en, keys=pr['keys'])


def f_contrast(rng):
    if rng.random() < 0.6:
        s1, s2 = rng.sample(list(SUBJECTS), 2)
        while SUBJECTS[s1]['en'] == SUBJECTS[s2]['en']:
            s1, s2 = rng.sample(list(SUBJECTS), 2)
        a1, a2 = rng.choice(PERSON_ADJ)
        bal = f"{s1} {a1}, sakewanten {s2} {a2}"
        en = (f"{cap(SUBJECTS[s1]['en'])} {SUBJECTS[s1]['be']} {ADJ_EN[a1]}, but "
              f"{SUBJECTS[s2]['en']} {SUBJECTS[s2]['be']} {ADJ_EN[a2]}")
        return dict(bal=cap(bal), en=en, keys=[])
    obj = rng.choice(list(CONTRAST_OBJ))
    a1, a2 = rng.choice(OBJ_ADJ)
    noun = CONTRAST_OBJ[obj]
    bal = f"{obj} niki {a1}, sakewanten {obj} nika {a2}"
    en = f"This {noun} is {ADJ_EN[a1]}, but that {noun} is {ADJ_EN[a2]}"
    return dict(bal=cap(bal), en=en, keys=[('o', obj)])


def f_existence(rng):
    subj = rng.choice(list(SUBJECTS))
    h = SUBJECTS[subj]['reg'] == 'H'
    prep, exist = ('ring', 'Wenten') if h else ('di', 'Ada')
    if rng.random() < 0.75:
        okey = rng.choice(list(EXIST_COUNT))
        plural, places = EXIST_COUNT[okey]
        num, p = rng.choice(NUMS), rng.choice(places)
        bal = f"{exist} {num[0]} {okey} {prep} {p}"
        en = f"There are {num[1]} {plural} {PLACES[p][0]}"
    else:
        okey = rng.choice(list(EXIST_MASS))
        bare, copula, places = EXIST_MASS[okey]
        p = rng.choice(places)
        bal = f"{exist} {okey} {prep} {p}"
        en = f"There {copula} {bare} {PLACES[p][0]}"
    return dict(bal=cap(bal), en=en, keys=[('o', okey), ('p', p)])


def f_possession(rng):
    subj = rng.choice(list(SUBJECTS))
    s = SUBJECTS[subj]
    okey = rng.choice(list(POSS_OBJ))
    dem, dem_en = rng.choice([('niki', 'This'), ('nika', 'That')])
    bal = f"{okey} {dem} gelah {s['owner']}"
    en = f"{dem_en} {POSS_OBJ[okey]} is {s['mine']}"
    return dict(bal=cap(bal), en=en, keys=[('o', okey)])


FRAMES = {'time': f_time, 'place': f_place, 'companion': f_companion, 'purpose': f_purpose,
          'sequence': f_sequence, 'reason': f_reason, 'condition': f_condition,
          'contrast': f_contrast, 'existence': f_existence, 'possession': f_possession}


def build_candidates(rng, banned):
    seen = set(banned)
    out = {}
    for name, fn in FRAMES.items():
        out[name] = []
        for _ in range(4000):
            c = fn(rng)
            if not c:
                continue
            bal = cap(c['bal']) + '.'
            key = rv.norm(bal)
            if key in seen:
                continue
            L = lesson_of(bal)
            if L is None:
                continue
            seen.add(key)
            out[name].append(dict(c, bal=bal, en=cap(c['en']) + '.', Lmin=L, type=name))
    return out


def select(rng, cands):
    """Spread sentences over the lessons where their words are all taught."""
    use = collections.Counter()
    per_lesson = collections.Counter()
    picked = []
    for name, cs in cands.items():
        rng.shuffle(cs)
        per_type = collections.Counter()
        n = 0
        for c in cs:
            if n >= TARGET[name]:
                break
            if any(use[k] >= USE_CAP[k[0]] for k in c['keys']):
                continue
            options = [L for L in range(c['Lmin'], MAX_LESSON + 1)
                       if per_type[L] < PER_TYPE_LESSON and per_lesson[L] < PER_LESSON]
            if not options:
                continue
            L = min(options, key=lambda x: (per_lesson[x], rng.random()))
            use.update(c['keys'])
            per_type[L] += 1
            per_lesson[L] += 1
            n += 1
            picked.append(dict(c, L=L))
    return picked


def merge_block(block, L, gen):
    seen_pairs, seen_bal, kept, out, marker = set(), set(), [], [], None
    for line in block:
        m = SENT_RE.match(line.strip())
        if not m:
            out.append(line)
            continue
        if marker is None:
            marker = len(out)
            out.append(None)
        bal, en = m.group(2).strip(), m.group(3).strip()
        key = rv.norm(bal)
        if (key, en) not in seen_pairs and not scope_bad(key.split(), L):
            seen_pairs.add((key, en))
            seen_bal.add(key)
            kept.append((bal, en))
    for bal, en in gen:
        key = rv.norm(bal)
        if key not in seen_bal:
            seen_bal.add(key)
            kept.append((bal, en))
    numbered = [f"{n}. {bal} -> {en}" for n, (bal, en) in enumerate(kept, 1)]
    if marker is not None:
        out[marker:marker + 1] = numbered
    else:
        j = max((k for k, line in enumerate(out) if line.strip()), default=len(out) - 1)
        out[j + 1:j + 1] = numbered
    return out


def rewrite_file(path, gen_map):
    lines = open(path, encoding='utf-8').read().splitlines()
    heads = [i for i, line in enumerate(lines) if LESSON_RE.match(line)]
    out = lines[:heads[0]]
    for n, h in enumerate(heads):
        end = heads[n + 1] if n + 1 < len(heads) else len(lines)
        L = int(LESSON_RE.match(lines[h]).group(1))
        out += merge_block(lines[h:end], L, gen_map.get(L, []))
    with open(path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(out).rstrip() + '\n')


def main():
    global FIRST
    rv.main()
    FIRST = vocab_index()
    rng = random.Random(SEED)
    banned = set()
    for fn in SENT_FILES:
        for line in open(os.path.join(ROOT, fn), encoding='utf-8'):
            m = SENT_RE.match(line.strip())
            if m:
                banned.add(rv.norm(m.group(2)))
    cands = build_candidates(rng, banned)
    picked = select(rng, cands)
    rng.shuffle(picked)
    gen_map = collections.defaultdict(list)
    for c in picked:
        gen_map[c['L']].append((c['bal'], c['en']))
    for bal, en in EXTRA:
        gen_map[lesson_of(bal)].append((bal, en))
    for fn in SENT_FILES:
        rewrite_file(os.path.join(ROOT, fn), gen_map)
    counts = collections.Counter(c['type'] for c in picked)
    print(f"generated {len(picked)} sentences: {dict(counts)}")
    print(f"lessons with generated sentences: {len(gen_map)}")


if __name__ == '__main__':
    main()
