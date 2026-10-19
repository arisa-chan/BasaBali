# -*- coding: utf-8 -*-
"""Clean-up pass: remove sentences whose only entry was removed, and sync the
lesson "teaching order" headers with the revised vocabulary."""
import os, re
ROOT = r"C:\Users\Jaydee Lucero\Downloads\workspace"
SEC = {1: os.path.join(ROOT, 'balinese_course_sentences_section1.md'),
       2: os.path.join(ROOT, 'balinese_course_sentences_section2.md'),
       3: os.path.join(ROOT, 'balinese_course_sentences_section3.md')}

# sentences to drop (normalised balinese text) because their vocabulary entry was removed
DROP = {2: {'paling', 'duang tali'},
        3: {'paling', 'mis'}}
def nkey(bal):
    return re.sub(r'[^a-z ]', '', bal.lower()).strip()
# lesson-scoped edits to the non-sentence header lines
HEADER = {
    4:  [('saking pundi?, uli dija?', 'saking pundi?, pundi, uli dija?')],
    5:  [('minum', 'nginum')],
    11: [('maungu', 'matangi')],
    49: [('maungu', 'matangi')],
    32: [('lakar (recycled), kel,', 'lakar (recycled), jagi, kel,')],
    38: [(', paling (recycled),', ',')],
    42: [(', paling (recycled),', ',')],
    16: [(', paling,', ','), ('paling (recycled), ', '')],
    22: [(', duang tali,', ','), ('duang tali (recycled), ', '')],
    50: [(', mis,', ','), ('mis (recycled), ', '')],
}

for sec, path in SEC.items():
    lines = open(path, encoding='utf-8').read().split('\n')
    lesson, out, dropped, hdr = None, [], 0, 0
    for line in lines:
        m = re.match(r'### Lesson (\d+):', line)
        if m:
            lesson = int(m.group(1)); out.append(line); continue
        if lesson and re.match(r'\d+\.\s', line.strip()):
            bal = line.strip().split('->')[0]
            bal = re.sub(r'^\d+\.\s*', '', bal).strip()
            if lesson in DROP and nkey(bal) in DROP[lesson]:
                dropped += 1
                print(f"  drop L{lesson}: {line.strip()[:60]}")
                continue
            out.append(line); continue
        if lesson in HEADER and line.strip():
            newline = line
            for old, new in HEADER[lesson]:
                if old in newline:
                    newline = newline.replace(old, new); hdr += 1
            out.append(newline); continue
        out.append(line)
    open(path, 'w', encoding='utf-8').write('\n'.join(out))
    print(f"section {sec}: dropped {dropped}, header edits {hdr}")
