# -*- coding: utf-8 -*-
"""Remove duplicate vocabulary rows introduced by repeated fix runs and make the
first occurrence of every entry authoritative (register included)."""
import os, re, io, collections
ROOT = r"C:\Users\Jaydee Lucero\Downloads\workspace"
VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')

def norm(e):
    e = e.lower().replace('é', 'e').replace('è', 'e')
    e = re.sub(r"[^\w\s']", ' ', e)
    return ' '.join(e.split())

seen = collections.Counter()
out, lesson = [], None
for raw in io.open(VOCAB, encoding='utf-8'):
    line = raw.rstrip('\n')
    m = re.match(r'#### Lesson (\d+):', line)
    if m:
        lesson = int(m.group(1)); out.append(line); continue
    if lesson and line.startswith('* '):
        entry = line[2:].split('->')[0].strip().rstrip('.').replace('...', '').strip()
        key = (lesson, norm(entry))
        if seen[key]:
            print(f"  dropped duplicate L{lesson}: {entry}")
            seen[key] += 1
            continue
        seen[key] = 1
        out.append(line); continue
    out.append(line)
io.open(VOCAB, 'w', encoding='utf-8').write('\n'.join(out))
print("dedupe complete")
