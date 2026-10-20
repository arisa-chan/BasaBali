#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
trim_vocab.py - cap every lesson at 25 vocabulary items and remove the
sentences that use entries retired from the course.

The per-lesson drop lists below were chosen by hand: redundant register /
synonym variants and derived multi-word phrases go first, while the core
pronouns, question words, numerals, everyday verbs and one alus/andap pair for
the important concepts stay.  The script is idempotent: running it again keeps
the trimmed curriculum unchanged.

Usage:
  python3 tools/trim_vocab.py           # report only
  python3 tools/trim_vocab.py --write   # rewrite vocab + sentence sources
"""
import json, os, re, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from build_content import ROOT, SENT_FILES, norm

VOCAB_PATH = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
RETIRED_JSON = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'retired_vocab.json')
SENT_PATHS = [os.path.join(ROOT, name) for _, name in SENT_FILES]
MAX_ITEMS = 25

# Entries removed from the course (all occurrences).  Each lesson with more
# than 25 unique entries keeps its 25 most useful items; every id below loses
# its slot.  Rationale: drop the fourth synonym before a register pair, drop
# phrases that are transparently buildable from kept words, and drop rarer
# derived vocabulary before core everyday vocabulary.
DROP = {
    1: {'inggih', 'saja', 'tusing', 'nyai', 'miwah', 'nanging', 'beneh'},
    2: {'siang', 'sore', 'kabar', 'mepamit', 'lantang yusa', 'dumogi rahayu', 'tiang becik'},
    3: {'nunas ampura', 'sinampura', 'raris', 'ngidih tulung', 'sing ada'},
    4: {'parab', 'wastan tiang', 'sing nawang', 'ten uning', 'gumi', 'pundi', 'toris', 'basa bali'},
    5: {'neda', 'nginem', 'rayunan', 'jaen gati', 'becik pisan', 'ngajeng semeng', 'padi', 'susu'},
    6: {'lawa', 'lele', 'yuyu', 'jero ketut', 'lelipi', 'kucing', 'wanara'},
    7: {'gelah icange', 'gelah nyaine', 'napi nika', 'apa ento', 'niki druwen tiang', 'ene gelah cang'},
    8: {'a', 'tigang', 'agelas', 'akidik', 'abedik', 'telah', 'tiga', 'enam', 'akeh', 'pragat',
        'kalih', 'angka', 'kude regane'},
    9: {'ibu', 'nanang', 'pianak', 'gek'},
    11: {'mesare', 'maseh', 'majan'},
    12: {'rurung', 'sanggah', 'merajan', 'kota'},
    14: {'galah', 'buin mani', 'ih puan', 'makelo', 'malih jebos'},
    15: {'lawas', 'wayah', 'tua', 'banyol'},
    16: {'kendel', 'takut', 'paling'},
    17: {'be bawi', 'madaging celeng'},
    18: {'prabu', 'peningalan', 'cangkem', 'karna', 'cokor', 'untu'},
    19: {'gadang nguda', 'selem kotor'},
    20: {'destar', 'ali ali', 'mabaju', 'makamben', 'senteng'},
    22: {'enembelas', 'pitulas', 'pelekutus', 'siangolas', 'kalih dasa', 'pitung dasa lima'},
    23: {'guisi', 'lempuyeng', 'masan ujan', 'masan'},
    24: {'mapanggih'},
    25: {'nyewa', 'ban', 'penumpang'},
    27: {'garpu'},
    33: {'perawat', 'nganggur'},
    35: {'canang sari', 'sasajen', 'gebogan', 'sembahyang', 'toya tirta'},
    37: {'ngumbah', 'nari'},
    46: {'mait', 'makaput', 'nunjel', 'negul'},
}
RETIRED = set().union(*DROP.values())


# ---------------------------------------------------------------- vocabulary
def parse_vocab_lines(path):
    """Return (prefix_lines, sections) where sections are
    {'header': line, 'lessons': [{'header': line, 'entries': [(raw, id, gloss, reg)]}]}."""
    lines = open(path, encoding='utf-8').read().splitlines()
    prefix, sections, section, lesson = [], [], None, None
    for line in lines:
        msec = re.match(r'###\s+Section\b', line)
        mles = re.match(r'####\s+Lesson\s+(\d+):', line)
        if msec:
            section = {'header': line, 'lessons': []}
            sections.append(section); lesson = None
            continue
        if mles:
            if section is None:
                section = {'header': '', 'lessons': []}
                sections.append(section)
            lesson = {'header': line, 'entries': []}
            section['lessons'].append(lesson)
            continue
        if lesson is not None and line.startswith('* '):
            parts = [p.strip() for p in line[2:].split('->')]
            raw = parts[0].rstrip('.').replace('...', '').strip()
            gloss = parts[1] if len(parts) > 1 else ''
            reg = parts[2] if len(parts) > 2 else ''
            lesson['entries'].append((line, norm(raw), gloss, reg))
        elif line.startswith('# ') and not sections:
            prefix.append(line)
    return prefix, sections


def build_selection(sections):
    entries_by_lesson = {}
    first = {}
    for sec in sections:
        for les in sec['lessons']:
            lid = int(re.match(r'####\s+Lesson\s+(\d+):', les['header']).group(1))
            entries_by_lesson[lid] = les['entries']
            for _, e, _, _ in les['entries']:
                first.setdefault(e, lid)

    for lid, drop in DROP.items():
        present = {e for _, e, _, _ in entries_by_lesson.get(lid, [])}
        uniq = {e for _, e, _, _ in entries_by_lesson.get(lid, []) if first.get(e) == lid}
        bad = drop & present - uniq
        if bad:
            raise SystemExit(f"L{lid}: drop list has non-unique entries: {sorted(bad)}")
        if len(uniq - drop) > MAX_ITEMS:
            raise SystemExit(f"L{lid}: {len(uniq - drop)} unique entries > {MAX_ITEMS}; extend DROP")

    kept = {}
    for lid, ents in entries_by_lesson.items():
        # de-duplicate entries by id, preserving order
        seen, deduped = set(), []
        for x in ents:
            if x[1] in RETIRED or x[1] in seen:
                continue
            seen.add(x[1]); deduped.append(x)
        uniq = [x for x in deduped if first.get(x[1]) == lid]
        if len(uniq) > MAX_ITEMS:
            raise SystemExit(f"L{lid}: still {len(uniq)} unique entries; extend DROP")
        if len(uniq) == MAX_ITEMS:
            keep_ids = {x[1] for x in uniq}
        else:
            keep_ids = {x[1] for x in uniq}
            # fill the remaining slots with curated review items, in source order
            for x in deduped:
                if len(keep_ids) >= MAX_ITEMS:
                    break
                if first.get(x[1], lid) < lid:
                    keep_ids.add(x[1])
        kept[lid] = [x for x in deduped if x[1] in keep_ids]
    return entries_by_lesson, first, kept


def _gloss_head(g):
    for cut in ('/', '('):
        if cut in g:
            g = g.split(cut)[0]
    return norm(g).strip()


def retired_records(entries_by_lesson, kept):
    recs = []
    for lid in sorted(entries_by_lesson):
        lesson_kept = kept.get(lid, [])
        for _, e, gloss, reg in entries_by_lesson[lid]:
            if e not in RETIRED:
                continue
            if ' ' in e:
                reason = 'derived multi-word phrase; its parts stay in the course'
            else:
                peer = None
                for _, k, kg, _ in lesson_kept:
                    if k != e and ' ' not in k and _gloss_head(kg) == _gloss_head(gloss):
                        peer = k
                        break
                if peer:
                    reason = f"redundant with kept '{peer}' (same meaning / register)"
                else:
                    reason = 'lower-frequency item trimmed to keep the lesson at 25'
            recs.append({'id': e, 'lesson': lid, 'gloss': gloss, 'register': reg, 'reason': reason})
    # one record per id (first lesson)
    out, seen = [], set()
    for r in recs:
        if r['id'] in seen:
            continue
        seen.add(r['id']); out.append(r)
    return out


def write_vocab(path, prefix, sections, kept):
    out = []
    if prefix:
        out += prefix
    first_section = True
    for sec in sections:
        if sec['header']:
            if out and out[-1] != '':
                out.append('')
            out.append(sec['header'])
            out.append('')
        for les in sec['lessons']:
            lid = int(re.match(r'####\s+Lesson\s+(\d+):', les['header']).group(1))
            out.append(les['header'])
            out += [x[0] for x in kept.get(lid, [])]
            out.append('')
        first_section = False
    with open(path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(out).rstrip() + '\n')


# ---------------------------------------------------------------- sentences
def combined_matcher(first, retired_ids):
    class M:
        def __init__(self):
            self.entries = sorted(set(first) | retired_ids,
                                  key=lambda e: (-len(e.split()), e))
        def match(self, toks, i):
            for e in self.entries:
                w = e.split()
                if toks[i:i + len(w)] == w:
                    return e, len(w)
            return None, 0
    return M()


def contains_retired(toks, matcher, retired_ids):
    i = 0
    while i < len(toks):
        e, ln = matcher.match(toks, i)
        if e:
            if e in retired_ids:
                return True
            # A longer phrase must not hide a retired word or sub-phrase:
            # "saking pundi" contains pundi, "sing ada unduk" contains "sing ada".
            if ln > 1:
                parts = e.split()
                for a in range(len(parts)):
                    for b in range(a + 1, len(parts) + 1):
                        if ' '.join(parts[a:b]) in retired_ids:
                            return True
            i += ln
            continue
        t = toks[i]
        for suf in ('ne', 'e', 'n'):
            if t.endswith(suf) and len(t) > len(suf):
                e2, _ = matcher.match([t[:-len(suf)]], 0)
                if e2 and e2 in retired_ids:
                    return True
        i += 1
    return False


def update_sentences(paths, first, kept, retired_ids):
    matcher = combined_matcher(first, retired_ids)
    kept_names = {lid: ', '.join(raw[2:].split('->')[0].strip() for raw in [x[0] for x in ents])
                  for lid, ents in kept.items()}  # unused; kept below
    removed = 0
    for path in paths:
        lines = open(path, encoding='utf-8').read().splitlines()
        out, lesson, seq = [], None, 0
        for line in lines:
            m = re.match(r'### Lesson (\d+):', line)
            if m:
                lesson = int(m.group(1)); seq = 0; out.append(line); continue
            m = re.match(r'\*\*New in this lesson \(teaching order\):\*\*\s*(.*)', line.strip())
            if m and lesson in kept:
                names = []
                for raw, e, _, _ in kept[lesson]:
                    name = raw[2:].split('->')[0].strip()
                    names.append(name)
                out.append('**New in this lesson (teaching order):** ' + ', '.join(names))
                continue
            m = re.match(r'(\d+)\.\s+(.+?)\s+->\s+(.+)', line.strip())
            if m and lesson is not None:
                bal, en = m.group(2), m.group(3)
                toks = norm(bal).split()
                if contains_retired(toks, matcher, retired_ids):
                    removed += 1
                    continue
                # keep the list sequential after removals
                seq += 1
                out.append(f"{seq}. {bal} -> {en}")
                continue
            out.append(line)
        with open(path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(out).rstrip() + '\n')
    return removed


def main():
    write = '--write' in sys.argv
    prefix, sections = parse_vocab_lines(VOCAB_PATH)
    entries_by_lesson, first, kept = build_selection(sections)
    retired = retired_records(entries_by_lesson, kept)
    print(f"lessons: {len(kept)}; retired entries: {len(retired)}")
    over = [(lid, len([x for x in ents if first.get(x[1]) == lid])) for lid, ents in kept.items()
            if len(ents) > MAX_ITEMS]
    print(f"lessons over {MAX_ITEMS}: {over if over else 'none'}")
    print("kept per lesson:", {lid: len(ents) for lid, ents in sorted(kept.items())})
    if not write:
        print("dry run; pass --write to update the source files")
        return
    if retired:
        with open(RETIRED_JSON, 'w', encoding='utf-8') as f:
            json.dump({'retired': retired}, f, ensure_ascii=False, indent=2)
            f.write('\n')
    else:
        print("no retired entries in the current source; keeping existing retired_vocab.json")
    write_vocab(VOCAB_PATH, prefix, sections, kept)
    removed = update_sentences(SENT_PATHS, first, kept, RETIRED)
    print(f"wrote {VOCAB_PATH}")
    print(f"wrote {RETIRED_JSON}")
    print(f"removed {removed} sentences that used retired entries")


if __name__ == '__main__':
    main()
