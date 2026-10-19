# -*- coding: utf-8 -*-
"""
apply_web_verified_review.py — revisions verified against online Balinese
references (BASAbali / BASAibu Wiki dictionary, verified by Balai Bahasa;
ban.wikipedia "Kruna wilangan" for numerals).

Sources (consulted 2026-10):
  dictionary.basabali.org/Durung    Andap = tonden · Alus madya = dereng ·
                                    Alus mider = durung (antonyms Sampun/Suba)
  basaibubali.org/Krebek            "Thunder", level Andap (Krébék),
                                    Indonesian petir/halilintar/guntur
  basaibubali.org/Tatit             lightning that strikes at a distance
  basaibubali.org/Ngunjuk           level Alus singgih ("drink")
  basaibubali.org/Ngrayunang        "eat; high Balinese, used with pedanda"
  basaibubali.org/Tulung            "help; to request help" (root; tulungin)
  ban.wikipedia.org/Kruna_wilangan  Selae 25 · Pasasur 35 · Samas 400 ·
                                    Domas 800 · Duang tali 2.000
"""
import io, os, re
ROOT = r"C:\Users\Jaydee Lucero\Downloads\workspace"
VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
S = {1: os.path.join(ROOT, 'balinese_course_sentences_section1.md'),
     2: os.path.join(ROOT, 'balinese_course_sentences_section2.md'),
     3: os.path.join(ROOT, 'balinese_course_sentences_section3.md')}

FIX = {
    'durung': ('not yet', 'Alus (Mider)'),
    'tonden': ('not yet', 'Andap'),
    'krebek': ('thunder (the loud thunderclap)', 'Andap'),
    'tatit':  ('lightning (strikes at a distance)', 'Mider'),
    'tulung': ('help; a call for help (noun/root — verbs: nulungin / ngwantu)', 'Mider'),
    'nginem': ('drink (polite)', 'Alus'),
}
ADD = [(5, 'ngunjuk', 'drink (honouring others)', 'Alus (Singgih)'),
       (32, 'dereng', 'not yet', 'Alus (Madya)'),
       (58, 'ngrayunang', 'eat (highest honour; used of priests)', 'Alus (Singgih)')]

# ---- vocabulary
out, lesson = [], None
for raw in io.open(VOCAB, encoding='utf-8'):
    line = raw.rstrip('\n')
    m = re.match(r'#### Lesson (\d+):', line)
    if m:
        lesson = int(m.group(1)); out.append(line); continue
    if lesson and line.startswith('* '):
        key = line[2:].split('->')[0].strip().rstrip('.').replace('...', '').strip().lower()
        if key in FIX:
            g, r = FIX[key]
            out.append(f"* {line[2:].split('->')[0].strip()} -> {g} -> {r}")
            print(f"  vocab L{lesson}: {key} -> [{g} / {r}]")
            continue
    out.append(line)
text = '\n'.join(out)
for L, entry, gloss, reg in ADD:
    i = text.index(f"#### Lesson {L}:")
    j = text.find('\n#### ', i + 1)
    if j == -1:
        j = len(text)
    text = text[:j] + f"\n* {entry} -> {gloss} -> {reg}" + text[j:]
    print(f"  vocab added L{L}: {entry} [{reg}]")
io.open(VOCAB, 'w', encoding='utf-8').write(text)

# ---- helpers
def lesson_span(lines, L):
    start = end = None
    for i, l in enumerate(lines):
        if l.startswith(f'### Lesson {L}:'):
            start = i
        elif start is not None and l.startswith('### Lesson '):
            end = i; break
    if end is None:
        end = len(lines)
    return start, end

def max_num(lines, a, b):
    mx = 0
    for l in lines[a:b]:
        m = re.match(r'(\d+)\.\s', l.strip())
        if m:
            mx = max(mx, int(m.group(1)))
    return mx

def append_sentences(path, L, sents):
    lines = io.open(path, encoding='utf-8').read().split('\n')
    a, b = lesson_span(lines, L)
    if a is None:
        print(f"  !! lesson {L} not found in {os.path.basename(path)}"); return
    n = max_num(lines, a, b)
    block = [f"{n + 1 + k}. {bal} -> {en}" for k, (bal, en) in enumerate(sents)]
    lines[b:b] = block + ['']
    io.open(path, 'w', encoding='utf-8').write('\n'.join(lines))
    print(f"  {os.path.basename(path)}: +{len(block)} sentences in L{L} (from #{n+1})")

def header_fix(path, L, old, new):
    lines = io.open(path, encoding='utf-8').read().split('\n')
    a, b = lesson_span(lines, L)
    hit = 0
    for i in range(a, b):
        if old in lines[i]:
            lines[i] = lines[i].replace(old, new); hit += 1
    io.open(path, 'w', encoding='utf-8').write('\n'.join(lines))
    print(f"  header L{L}: {hit} edit(s) for {old[:40]!r}")

# ---- honoured persons drink with ngunjuk (Alus singgih), not nginem
t = io.open(S[1], encoding='utf-8').read()
for o, n in [('Ida nginem.', 'Ida ngunjuk.'),
             ('Ipun nginem driki.', 'Ipun ngunjuk driki.'),
             ('Ida nginem toya.', 'Ida ngunjuk toya.'),
             ('Ida nginem teh.', 'Ida ngunjuk teh.')]:
    if o in t:
        t = t.replace(o, n); print(f"  L5: {o} -> {n}")
    else:
        print(f"  !! L5 not found: {o}")
io.open(S[1], 'w', encoding='utf-8').write(t)

append_sentences(S[1], 5, [('Ngunjuk.', 'To drink (refined).'),
                           ('Ida ngunjuk.', 'He / she (honored) drinks.'),
                           ('Ipun ngunjuk driki.', 'He / she drinks here.')])
append_sentences(S[3], 32, [('Dereng.', 'Not yet.'),
                            ('Ida dereng rauh.', 'He / she (honored) has not arrived yet.'),
                            ('Tiang dereng madaar.', 'I have not eaten yet.')])
append_sentences(S[3], 58, [('Ngrayunang.', 'To eat (highest honour).'),
                            ('Ida ngrayunang.', 'He / she (honored) eats.'),
                            ('Ida sampun ngrayunang.', 'He / she (honored) has eaten.')])

# teaching-order headers
header_fix(S[1], 5,  'nginem, minum,', 'nginem, ngunjuk, minum,')
header_fix(S[3], 32, 'durung (recycled)', 'durung (recycled), dereng')
header_fix(S[3], 58, 'makta,', 'makta, ngrayunang,')

# ---- standard orthography: wénten (é) — matches the course's own vocabulary entry
for p in S.values():
    t = io.open(p, encoding='utf-8').read()
    n = len(re.findall(r'(?<![A-Za-zÀ-ÿ])wenten(?![A-Za-zÀ-ÿ])', t)) + t.count('Wenten')
    t = re.sub(r'(?<![A-Za-zÀ-ÿ])wenten(?![A-Za-zÀ-ÿ])', 'wénten', t)
    t = t.replace('Wenten', 'Wénten')
    io.open(p, 'w', encoding='utf-8').write(t)
    if n:
        print(f"  {os.path.basename(p)}: wenten -> wénten x{n}")
print("done")
