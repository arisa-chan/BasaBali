# -*- coding: utf-8 -*-
import re, sys

def norm(s):
    s = s.lower().replace('é','e').replace('è','e').replace('…',' ')
    s = re.sub(r"[^\w\s']", ' ', s)
    return ' '.join(s.split())

# ---- parse FULL vocabulary file (all sections) ----
vocab, lesson, current_section = {}, None, None
for line in open('uploads/balinese_course_vocabulary.md', encoding='utf-8'):
    line = line.rstrip()
    m = re.match(r'### Section (\d+)', line)
    if m: current_section = int(m.group(1)); continue
    m = re.match(r'#### Lesson (\d+):', line)
    if m:
        lesson = int(m.group(1))
        vocab[lesson] = []
        continue
    if lesson and line.startswith('* '):
        entry = line[2:].split('->')[0].strip()
        entry = entry.rstrip('.').replace('...','').strip()
        vocab[lesson].append(norm(entry))

entry_lesson = {}
for L in sorted(vocab):
    for e in vocab[L]:
        if e not in entry_lesson: entry_lesson[e] = L

# ---- parse a sentence file ----
def parse_sents(fname):
    sents, lesson = {}, None
    for line in open(fname, encoding='utf-8'):
        m = re.match(r'### Lesson (\d+):', line)
        if m:
            lesson = int(m.group(1)); sents.setdefault(lesson, []); continue
        if line.startswith('## Section') and 'Completion' in line: lesson = None; continue
        if lesson:
            m = re.match(r'(\d+)\.\s+(.+?)\s+->\s+(.+)', line.strip())
            if m: sents[lesson].append((int(m.group(1)), norm(m.group(2)).split(), m.group(2)))
    return sents

NAMES = {'wayan','made','nyoman','ketut','putu','kadek','gede','komang','agung'}
EXC = {'cang':'icang','druwen':'druwe','jeron':'jero','mawasta':'mawasta','rage':'rage',
       'malajah':'malajah','pula':'pula'}

all_entries = sorted(entry_lesson.keys(), key=lambda e: (-len(e.split()), e))

def match_entry(tokens, i, pool):
    for e in all_entries:
        w = e.split()
        if e in pool and tokens[i:i+len(w)] == w: return e, len(w)
    return None, 0

def validate(fname, lessons):
    known = set(e for e, ll in entry_lesson.items() if ll < lessons[0])
    sents = parse_sents(fname)
    viol_new, viol_future, viol_unknown, counts = [], [], [], {}
    for L in lessons:
        if L not in sents: continue
        pool = set(e for e, ll in entry_lesson.items() if ll <= L)
        counts[L] = {e: 0 for e in vocab[L]}
        for num, toks, raw in sents[L]:
            for e in vocab[L]:
                w = e.split(); n = len(w)
                for i in range(len(toks)-n+1):
                    if toks[i:i+n-1] == w[:-1] and toks[i+n-1] in (w[-1], w[-1]+'ne', w[-1]+'e', w[-1]+'n'):
                        counts[L][e] += 1; break
            new_here, i = [], 0
            while i < len(toks):
                e, ln = match_entry(toks, i, pool)
                if e:
                    if e not in known: known.add(e); new_here.append(e)
                    i += ln; continue
                t = toks[i]
                if t in NAMES or t in EXC: i += 1; continue
                ok = False
                for suf in ('ne','e','n'):
                    if t.endswith(suf) and len(t) > len(suf):
                        base = t[:-len(suf)]
                        e2, _ = match_entry([base], 0, pool)
                        if e2:
                            if e2 not in known: known.add(e2); new_here.append(e2+'(+'+suf+')')
                            ok = True; break
                if ok: i += 1; continue
                ef, lnf = match_entry(toks, i, set(entry_lesson.keys()))
                if ef:
                    viol_future.append((L, num, raw, ef, entry_lesson[ef])); i += lnf; continue
                viol_unknown.append((L, num, raw, t)); i += 1
    print(f"=== {fname}: sentences per lesson ===")
    for L in lessons:
        if L in sents: print(f"  L{L}: {len(sents[L])}")
    bad = [(L,e,c) for L in sorted(counts) for e,c in counts[L].items() if c < 3]
    print(f"=== entries with <3 occurrences: {len(bad)} ===")
    for L,e,c in bad: print(f"  L{L}: '{e}' -> {c}")
    print(f"=== >1 new entry in one sentence: {len(viol_new)} ===")
    for L,n,r,ne in viol_new[:40]: print(f"  L{L} #{n}: {r}  NEW={ne}")
    print(f"=== future vocab used: {len(viol_future)} ===")
    for L,n,r,e,el in viol_future[:60]: print(f"  L{L} #{n}: {r}  -> '{e}' (Lesson {el})")
    print(f"=== unknown tokens: {len(viol_unknown)} ===")
    seen=set()
    for L,n,r,t in viol_unknown:
        k=(L,t)
        if k not in seen: seen.add(k); print(f"  L{L} #{n}: '{t}'  in: {r}")

validate('balinese_course_sentences_section2.md', list(range(11,31)))
