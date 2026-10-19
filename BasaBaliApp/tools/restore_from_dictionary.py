# -*- coding: utf-8 -*-
"""
restore_from_dictionary.py — re-adds/repairs entries that the BASAbali dictionary
(dictionary.basabali.org, verified by Balai Bahasa) confirms are genuine Balinese,
using the pre-review backup in the session scratchpad for the original sentences.

Dictionary findings used here:
  prabu        king; villain; head (alus singgih)   -> the course gloss was right
  mis          trash; rubbish; garbage   [Mider]    -> genuine word, label was wrong
  tangi        purple                    [Mider]    -> genuine word
  putih sentak very white                [Mider]    -> genuine phrase
  paling       confused; eldest; greatest [Andap]   -> the "confused" sense is real
  kulkul       slit drum                 [Andap]
  nginem       drink (v)                 [Kasar / Andap]
  ampura       forgive                              -> refine gloss
  selem denges (no gloss in the dictionary)         -> stays removed
  barak biing  searing heat (not "dark red")        -> stays removed
"""
import io, os, re, glob
WS = r"C:\Users\Jaydee Lucero\Downloads\workspace"
VOCAB = os.path.join(WS, 'uploads', 'balinese_course_vocabulary.md')
S = {1: os.path.join(WS, 'balinese_course_sentences_section1.md'),
     2: os.path.join(WS, 'balinese_course_sentences_section2.md'),
     3: os.path.join(WS, 'balinese_course_sentences_section3.md')}
BK = glob.glob(os.path.join(os.environ.get('COMMANDCODE_SCRATCHPAD', ''), 'backup', '*.md'))
BK = {os.path.basename(p)[0]: p for p in BK}      # 'v','s' keys by first letter is ambiguous; map explicitly
BK = {}
for p in glob.glob(os.path.join(os.environ.get('COMMANDCODE_SCRATCHPAD', ''), 'backup', '*.md')):
    n = os.path.basename(p)
    if n.startswith('vocab'):
        BK['v'] = p
    else:
        BK[n[1]] = p

# ---------------- vocabulary
FIX = {
    'kulkul':  ('slit drum', 'Andap'),
    'ampura':  ('forgive; excuse me; sorry', 'Alus'),
    'nginem':  ('drink (familiar)', 'Andap'),
}
ADD = [(18, 'prabu', 'head (honouring form); also king', 'Alus (Singgih)'),
       (50, 'mis', 'trash; rubbish; garbage', 'Mider'),
       (19, 'putih sentak', 'very white', 'Mider'),
       (16, 'paling', 'confused; at a loss', 'Andap')]
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
            print(f"  vocab fixed L{lesson}: {key} -> [{g} / {r}]")
            continue
    out.append(line)
text = '\n'.join(out)
# tangi comes back as a Mider synonym next to the Andap 'ungu' in Lesson 19
for L, entry, gloss, reg in ADD:
    i = text.index(f"#### Lesson {L}:")
    j = text.find('\n#### ', i + 1)
    if j == -1:
        j = len(text)
    text = text[:j] + f"\n* {entry} -> {gloss} -> {reg}" + text[j:]
    print(f"  vocab added L{L}: {entry} [{reg}]")
i = text.index('#### Lesson 19:')
j = text.find('\n#### ', i + 1)
text = text[:j] + "\n* tangi -> purple -> Mider" + text[j:]
print("  vocab added L19: tangi [Mider]")
io.open(VOCAB, 'w', encoding='utf-8').write(text)

# ---------------- sentences (original lines recovered from the backup)
def from_backup(sec, lesson, nums):
    """original sentence lines of a lesson, by number"""
    t = io.open(BK[str(sec)], encoding='utf-8').read().split('\n')
    cur, out = None, {}
    for l in t:
        m = re.match(r'### Lesson (\d+):', l)
        if m:
            cur = int(m.group(1)); continue
        m = re.match(r'(\d+)\.\s', l.strip())
        if cur == lesson and m:
            out[int(m.group(1))] = l.strip()
    return [out[n] for n in nums if n in out]

def replace_block(path, lesson, old_pairs, add_lines=()):
    lines = io.open(path, encoding='utf-8').read().split('\n')
    hit = 0
    for i, l in enumerate(lines):
        for old, new in old_pairs:
            if l.strip() == old:
                lines[i] = new; hit += 1
    if add_lines:
        a = b = None
        for i, l in enumerate(lines):
            if l.startswith(f'### Lesson {lesson}:'):
                a = i
            elif a is not None and l.startswith('### Lesson '):
                b = i; break
        if b is None:
            b = len(lines)
        mx = 0
        for l in lines[a:b]:
            m = re.match(r'(\d+)\.\s', l.strip())
            if m:
                mx = max(mx, int(m.group(1)))
        lines[b:b] = [f'{mx + 1 + k}. {s.split(". ", 1)[1]}' for k, s in enumerate(add_lines)] + ['']
        hit += len(add_lines)
    io.open(path, 'w', encoding='utf-8').write('\n'.join(lines))
    print(f"  {os.path.basename(path)} L{lesson}: {hit} sentence line(s) restored")

# L18: back to the dictionary-confirmed 'prabu' head sentences
replace_block(S[2], 18,
              [('13. Sirah. -> Head.', from_backup(2, 18, [13])[0]),
               ('14. Sirah cai sakit? -> Does your head hurt?', from_backup(2, 18, [14])[0]),
               ('15. Sirah icang sakit. -> My head hurts.', from_backup(2, 18, [15])[0]),
               ('16. Sirah nika ring luhur. -> The head is at the top.', from_backup(2, 18, [16])[0])])
# L50: 'mis' is a real (Mider) word for trash
replace_block(S[3], 50,
              [('21. Luu. -> Trash (familiar).', from_backup(3, 50, [21])[0]),
               ('22. Wénten sampah ring margi. -> There is trash on the road.', from_backup(3, 50, [22])[0]),
               ('23. Luu ditu liu. -> There is much trash there.', from_backup(3, 50, [23])[0]),
               ('24. Ngedasang sampah nika pabuat jagat. -> Cleaning up trash is for the world.', from_backup(3, 50, [24])[0])])
# L19: 'tangi' purple (Mider) + 'putih sentak' very white (Mider) come back
replace_block(S[2], 19,
              [('49. Ungu. -> Purple (neutral loan).', from_backup(2, 19, [49])[0]),
               ('50. Baju nyai ungu. -> Your shirt is purple.', from_backup(2, 19, [50])[0]),
               ('51. Warna nika ungu. -> That color is purple.', from_backup(2, 19, [51])[0]),
               ('52. Ungu gati buku caine. -> Your book is so purple.', from_backup(2, 19, [52])[0])],
              add_lines=from_backup(2, 19, [97, 98, 99, 100]))
# L16: 'paling' confused is dictionary-real
replace_block(S[2], 16,
              [('82. Icang bingung. -> I am confused.', from_backup(2, 16, [82])[0]),
               ('83. Cai bingung? Dija umah caine? -> Are you confused? Where is your house?', from_backup(2, 16, [83])[0]),
               ('84. Bingung gati ia di alas. -> He is so confused in the forest.', from_backup(2, 16, [84])[0])],
              add_lines=from_backup(2, 16, [81]))
# teaching-order headers
for path, L, old, new in ((S[2], 18, 'sirah, prabu,', 'sirah, prabu,'),
                          (S[2], 19, 'pelung, biru, ungu,', 'pelung, biru, tangi, ungu,'),
                          (S[3], 50, 'luu, mis,', 'luu, mis,'),
                          (S[2], 16, 'bingung, paling,', 'bingung, paling,')):
    lines = io.open(path, encoding='utf-8').read().split('\n')
    a = b = None
    for i, l in enumerate(lines):
        if l.startswith(f'### Lesson {L}:'):
            a = i
        elif a is not None and l.startswith('### Lesson '):
            b = i; break
    for i in range(a, b or len(lines)):
        if old.split(', ')[1] not in lines[i] and 'New in this lesson' in lines[i]:
            lines[i] = lines[i].replace('New in this lesson (teaching order):** ',
                                        'New in this lesson (teaching order):** ' + old.split(', ')[1] + ', ', 1)
            print(f"  header L{L}: re-inserted {old.split(', ')[1]}")
    io.open(path, 'w', encoding='utf-8').write('\n'.join(lines))
print("done")
