# -*- coding: utf-8 -*-
"""Restore the base numerals' example counts (the linker fix moved their
attributive uses to the linking forms) and register the missing linking entry
`pitung`."""
import io, os, re
ROOT = r"C:\Users\Jaydee Lucero\Downloads\workspace"
VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
S1 = os.path.join(ROOT, 'balinese_course_sentences_section1.md')

# 1. `pitung` (seven, linking form) belongs with the other linking entries
txt = io.open(VOCAB, encoding='utf-8').read()
if '\n* pitung ->' not in txt:
    i = txt.index('#### Lesson 8:')
    j = txt.find('\n#### ', i + 1)
    if j == -1:
        j = len(txt)
    txt = txt[:j] + "\n* pitung -> seven (linking form) -> Mider" + txt[j:]
    io.open(VOCAB, 'w', encoding='utf-8').write(txt)
    print("vocab: added L8 `pitung`")
else:
    print("vocab: pitung already present")

# 2. counting drills so every base numeral keeps its >=3 lesson examples
lines = io.open(S1, encoding='utf-8').read().split('\n')
a = b = None
for i, l in enumerate(lines):
    if l.startswith('### Lesson 8:'):
        a = i
    elif a is not None and l.startswith('### Lesson 9:'):
        b = i; break
mx = 0
for l in lines[a:b]:
    m = re.match(r'(\d+)\.\s', l.strip())
    if m:
        mx = max(mx, int(m.group(1)))
drills = ['Besik, dua, telu, papat.',
          'Lima, pitu, sia, dua.',
          'Dua, lima, pitu, sia.',
          'Telu, tiga, papat.']
ens = ['One, two, three, four.',
       'Five, seven, nine, two.',
       'Two, five, seven, nine.',
       'Three, four, five.'.replace('five', 'four')]
block = [f"{mx + 1 + k}. {d} -> {e}" for k, (d, e) in enumerate(zip(drills, ens))] + ['']
lines[b:b] = block
io.open(S1, 'w', encoding='utf-8').write('\n'.join(lines))
print(f"section 1: added {len(drills)} counting drills after L8 #{mx}")
