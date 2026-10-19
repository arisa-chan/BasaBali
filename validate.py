# -*- coding: utf-8 -*-
import re, sys

def norm(s):
    s = s.lower().replace('é','e').replace('è','e')
    s = s.replace('…',' ')
    s = re.sub(r"[^\w\s']", ' ', s)
    return ' '.join(s.split())

# ---- parse vocabulary file: Section 1 lessons 1-10 ----
vocab = {}   # lesson_num -> list of entries (normalized)
lesson = None
in_s1 = False
for line in open('uploads/balinese_course_vocabulary.md', encoding='utf-8'):
    line = line.rstrip()
    if line.startswith('### Section 1'): in_s1 = True; continue
    if line.startswith('### Section 2'): in_s1 = False; continue
    m = re.match(r'#### Lesson (\d+):', line)
    if m:
        lesson = int(m.group(1)) if in_s1 else None
        if lesson: vocab[lesson] = []
        continue
    if lesson and line.startswith('* '):
        entry = line[2:].split('->')[0].strip()
        entry = entry.rstrip('.').replace('...','').strip()
        vocab[lesson].append(norm(entry))

# entry -> first lesson it appears in
entry_lesson = {}
for L in sorted(vocab):
    for e in vocab[L]:
        if e not in entry_lesson: entry_lesson[e] = L

# ---- parse sentences file ----
sents = {}  # lesson -> list of (num, tokens, raw)
lesson = None
for line in open('balinese_course_sentences_section1.md', encoding='utf-8'):
    m = re.match(r'### Lesson (\d+):', line)
    if m: lesson = int(m.group(1)); sents[lesson] = []; continue
    if line.startswith('## Section 1 Completion'): lesson = None; continue
    if lesson:
        m = re.match(r'(\d+)\.\s+(.+?)\s+->\s+(.+)', line.strip())
        if m:
            bal = m.group(2)
            sents[lesson].append((int(m.group(1)), norm(bal).split(), bal))

NAMES = {'wayan','made','nyoman','ketut','putu','kadek','gede','komang'}
EXC = {'cang':'icang','druwen':'druwe','jeron':'jero','mawasta':'mawasta','rage':'rage'}

all_entries = sorted(entry_lesson.keys(), key=lambda e: (-len(e.split()), e))

def match_entry(tokens, i, pool):
    """longest entry match at position i from pool (set). returns (entry, len)"""
    for e in all_entries:
        w = e.split()
        if e in pool and tokens[i:i+len(w)] == w:
            return e, len(w)
    return None, 0

known = set()   # entries known so far (cumulative)
viol_new, viol_future, viol_unknown, counts = [], [], [], {}

for L in sorted(sents):
    pool = set(e for e, ll in entry_lesson.items() if ll <= L)
    counts[L] = {e: 0 for e in vocab[L]}
    for num, toks, raw in sents[L]:
        # count occurrences for this lesson's entries
        for e in vocab[L]:
            w = e.split()
            for i in range(len(toks)-len(w)+1):
                if toks[i:i+len(w)] == w:
                    counts[L][e] += 1; break
        # walk tokens
        new_here, i = [], 0
        while i < len(toks):
            e, ln = match_entry(toks, i, pool)
            if e:
                if e not in known:
                    known.add(e); new_here.append(e)
                i += ln; continue
            t = toks[i]
            if t in NAMES or t in EXC:
                i += 1; continue
            ok = False
            for suf in ('ne','e','n'):
                if t.endswith(suf) and len(t) > len(suf):
                    base = t[:-len(suf)]
                    e2, _ = match_entry([base], 0, pool)
                    if e2:
                        if e2 not in known: known.add(e2); new_here.append(e2+'(+'+suf+')')
                        ok = True; break
            if ok: i += 1; continue
            # future-lesson entry?
            ef, lnf = match_entry(toks, i, set(entry_lesson.keys()))
            if ef:
                viol_future.append((L, num, raw, ef, entry_lesson[ef]))
                i += lnf; continue
            viol_unknown.append((L, num, raw, t))
            i += 1
        if len(new_here) > 1:
            viol_new.append((L, num, raw, new_here))

print("=== sentences per lesson ===")
for L in sorted(sents): print(f"L{L}: {len(sents[L])} sentences")
print(f"\n=== entries with <3 occurrences in their lesson ===")
bad = [(L,e,c) for L in sorted(counts) for e,c in counts[L].items() if c < 3]
for L,e,c in bad: print(f"  Lesson {L}: '{e}' -> {c}")
if not bad: print("  NONE - all entries have >=3 sentences")
print(f"\n=== sentences introducing >1 new entry: {len(viol_new)} ===")
for L,n,r,ne in viol_new: print(f"  L{L} #{n}: {r}  NEW={ne}")
print(f"\n=== future-lesson vocab used: {len(viol_future)} ===")
for L,n,r,e,el in viol_future: print(f"  L{L} #{n}: {r}  -> '{e}' (Lesson {el})")
print(f"\n=== unknown tokens: {len(viol_unknown)} ===")
for L,n,r,t in viol_unknown: print(f"  L{L} #{n}: {r}  -> token '{t}'")
