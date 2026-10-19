# -*- coding: utf-8 -*-
"""Fix the two real findings of the parse-level audit:
   W2  bare numeral before a noun  -> linking form (duang, telung, tigang, …)
   W5  intensifier before the adjective -> post-positive order
Only the sentences the treebank flagged are touched."""
import io, os, re, collections
ROOT = r"C:\Users\Jaydee Lucero\Downloads\workspace"
TB = os.path.join(ROOT, 'BasaBaliApp', 'tools', 'treebank.tsv')
S1 = os.path.join(ROOT, 'balinese_course_sentences_section1.md')
S3 = os.path.join(ROOT, 'balinese_course_sentences_section3.md')

LINK = {'dua': 'duang', 'telu': 'telung', 'tiga': 'tigang', 'papat': 'patang',
        'lima': 'limang', 'pitu': 'pitung', 'sia': 'sangang'}   # kutus needs no linker
INTENS = {'gati', 'kaliwat', 'liwat', 'luyu', 'pisan', 'sajan', 'pesan'}

rows = collections.defaultdict(list)
for ln, line in enumerate(io.open(TB, encoding='utf-8')):
    if ln == 0:
        continue
    sid, idx, tok, pos, head, rel, conf, src = line.rstrip('\n').split('\t')
    rows[sid].append((tok.lower(), pos))

w2, w5 = [], []
for sid, toks in rows.items():
    for i in range(len(toks) - 1):
        if toks[i][0] in LINK and toks[i + 1][1] in ('noun', 'clf'):
            w2.append((sid, toks[i][0], toks[i + 1][0]))
        if toks[i][0] in INTENS and toks[i + 1][1] == 'adj':
            w5.append((sid, toks[i][0], toks[i + 1][0]))
print(f"W2 to fix: {len(w2)}   W5 to fix: {len(w5)}")

def fix(path, pairs, kind):
    txt = io.open(path, encoding='utf-8').read()
    n = 0
    for sid, a, b in pairs:
        if kind == 'w2':
            old, new = f"{a} {b}", f"{LINK[a]} {b}"
        else:
            old, new = f"{a} {b}", f"{b} {a}"
        # keep the original capitalisation of the first word of the sentence
        for o, nw in ((old, new), (old.capitalize(), new.capitalize())):
            if o in txt:
                txt = txt.replace(o, nw, 1)
                n += 1
                break
        else:
            print(f"   !! not found: {sid}: {old}")
    io.open(path, 'w', encoding='utf-8').write(txt)
    print(f"  {os.path.basename(path)}: {n} fixes applied")

fix(S1, [x for x in w2 if x[0].startswith('S1')], 'w2')
fix(S3, [x for x in w2 if x[0].startswith('S3')], 'w2')
fix(S3, w5, 'w5')
