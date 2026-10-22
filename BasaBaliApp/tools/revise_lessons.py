#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
revise_lessons.py — expand the Lessons database from 25 to 30 entries per lesson.

The source snapshots in tools/source/ are the original 30-39-entry course files
(commit 62f3d79) that were later trimmed to 25.  This script:

  * keeps the current 25 entries of every lesson,
  * restores the most useful previously retired entries in original order,
  * tops up short lessons with review entries that already occur in that
    lesson's original sentence set (so every listed entry stays attested),
  * rewrites the vocabulary lists and the section sentence files,
  * filters out only sentences that use a vocabulary entry not selected anywhere,
  * updates the teaching-order line and renumbers sentences.

Usage: python3 tools/revise_lessons.py
"""
import json, os, re, sys, collections

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
PROJ = os.path.dirname(HERE)
FULL_VOCAB = os.path.join(HERE, 'source', 'vocabulary_full.md')
CURRENT_VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
RETIRED_JSON = os.path.join(HERE, 'retired_vocab.json')
SENT_FILES = [
    (1, 'balinese_course_sentences_section1.md'),
    (2, 'balinese_course_sentences_section2.md'),
    (3, 'balinese_course_sentences_section3.md'),
]
FULL_SENT = {
    1: os.path.join(HERE, 'source', 'sentences_section1_full.md'),
    2: os.path.join(HERE, 'source', 'sentences_section2_full.md'),
    3: os.path.join(HERE, 'source', 'sentences_section3_full.md'),
}
TARGET = 30
NAMES = {'wayan', 'made', 'nyoman', 'ketut', 'putu', 'kadek', 'gede', 'komang', 'agung', 'budi'}
EXC = {'cang': 'icang', 'druwen': 'druwe', 'jeron': 'jero', 'mawasta': 'mawasta',
       'rage': 'rage', 'malajah': 'malajah', 'pula': 'pula'}

# Entries that are not useful restored filler; they are either transparent
# prefixes or long derivations whose parts are already taught.
SKIP = {
    4: {'wastan tiang...', 'wastan tiang'},
    6: {'jero ketut'},
    8: {'a'},
    17: {'madaging celeng'},
}


def norm(s):
    s = s.lower().replace('é', 'e').replace('è', 'e').replace('…', ' ')
    s = re.sub(r"[^\w\s']", ' ', s)
    return ' '.join(s.split())


def parse_vocab(path):
    """Return (prefix_lines, sections); each section has lessons with raw entries."""
    lines = open(path, encoding='utf-8').read().splitlines()
    prefix, sections, section, lesson = [], [], None, None
    for line in lines:
        if line.startswith('### Section'):
            section = {'header': line, 'lessons': []}
            sections.append(section)
            lesson = None
            continue
        m = re.match(r'#### Lesson (\d+):', line)
        if m:
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
            lesson['entries'].append({'line': line, 'id': norm(raw), 'gloss': gloss, 'reg': reg,
                                      'display': parts[0].strip()})
        elif not sections:
            prefix.append(line)
    return prefix, sections


def build_matcher(ids):
    entries = sorted(ids, key=lambda e: (-len(e.split()), e))
    class M:
        def match(self, toks, i):
            for e in entries:
                w = e.split()
                if toks[i:i + len(w)] == w:
                    return e, len(w)
            return None, 0
    return M()


def tags_in(toks, matcher):
    out = []
    i = 0
    while i < len(toks):
        e, ln = matcher.match(toks, i)
        if e:
            out.append(e)
            i += ln
            continue
        t = toks[i]
        if t in NAMES or t in EXC:
            i += 1
            continue
        for suf in ('ne', 'e', 'n'):
            if t.endswith(suf) and len(t) > len(suf):
                e2, _ = matcher.match([t[:-len(suf)]], 0)
                if e2:
                    out.append(e2)
                    break
        i += 1
    return out


def write_vocab(path, prefix, sections, selected_lines):
    out = list(prefix)
    if out and out[-1] != '':
        out.append('')
    for sec in sections:
        if sec['header']:
            if out and out[-1] != '':
                out.append('')
            out.append(sec['header'])
            out.append('')
        for les in sec['lessons']:
            lid = int(re.match(r'#### Lesson (\d+):', les['header']).group(1))
            out.append(les['header'])
            out += selected_lines[lid]
            out.append('')
    with open(path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(out).rstrip() + '\n')


def main():
    prefix, sections = parse_vocab(FULL_VOCAB)
    _, current_sections = parse_vocab(CURRENT_VOCAB)
    full_by = {}
    all_full = []
    for sec in sections:
        for les in sec['lessons']:
            lid = int(re.match(r'#### Lesson (\d+):', les['header']).group(1))
            full_by[lid] = les['entries']
            all_full += [e['id'] for e in les['entries']]
    cur_by = {}
    for sec in current_sections:
        for les in sec['lessons']:
            lid = int(re.match(r'#### Lesson (\d+):', les['header']).group(1))
            cur_by[lid] = {e['id'] for e in les['entries']}
    entry_by_id = {}
    full_index = {}
    for L2, es in full_by.items():
        for i, e in enumerate(es):
            entry_by_id.setdefault(e['id'], e)
            full_index.setdefault(e['id'], (L2, i))

    # Entries occurring in each lesson's original sentences (for review filler).
    full_matcher = build_matcher(set(all_full))
    sent_entries = collections.defaultdict(collections.Counter)
    for sec, path in FULL_SENT.items():
        lesson = None
        for line in open(path, encoding='utf-8'):
            m = re.match(r'### Lesson (\d+):', line)
            if m:
                lesson = int(m.group(1))
                continue
            m = re.match(r'\d+\.\s+(.+?)\s+->\s+', line.strip())
            if m and lesson is not None:
                for e in tags_in(norm(m.group(1)).split(), full_matcher):
                    sent_entries[lesson][e] += 1

    first_lesson = {}
    for L2, es in full_by.items():
        for e in es:
            first_lesson.setdefault(e['id'], L2)

    selected = {}
    selected_lines = {}
    introduced = set()          # entries already introduced in their first lesson
    for lid in sorted(full_by):
        full = full_by[lid]

        def allowed(eid):
            fl = first_lesson.get(eid, lid)
            return fl == lid or (fl < lid and eid in introduced)

        keep = {e for e in cur_by.get(lid, set()) & {x['id'] for x in full} if allowed(e)}
        chosen = [e for e in full if e['id'] in keep]
        chosen_ids = {e['id'] for e in chosen}
        # Restore missing original entries in their original teaching order.
        for e in full:
            if len(chosen) >= TARGET:
                break
            if e['id'] in chosen_ids or e['id'] in SKIP.get(lid, set()) or not allowed(e['id']):
                continue
            chosen.append(e)
            chosen_ids.add(e['id'])
        # Short lessons: top up with attested review words from earlier lessons.
        if len(chosen) < TARGET:
            candidates = []
            for eid, cnt in sent_entries[lid].items():
                if eid in chosen_ids or first_lesson.get(eid, lid) >= lid or eid not in introduced:
                    continue
                candidates.append((cnt, first_lesson.get(eid, 0), eid))
            candidates.sort(key=lambda x: (-x[0], -x[1], x[2]))
            for cnt, fl, eid in candidates:
                if len(chosen) >= TARGET:
                    break
                e = entry_by_id.get(eid)
                if e is None:
                    continue
                chosen.append(e)
                chosen_ids.add(eid)
        # Preserve original course order in the final list.
        chosen.sort(key=lambda e: full_index.get(e['id'], (lid, 999)))
        if len(chosen) < TARGET:
            print(f"WARNING L{lid}: only {len(chosen)} entries (no attested fillers left)")
        selected[lid] = chosen
        selected_lines[lid] = [e['line'] for e in chosen]
        introduced.update(e['id'] for e in chosen if first_lesson.get(e['id']) == lid)
        print(f"L{lid}: {len(chosen)} entries")

    selected_ids = {e['id'] for es in selected.values() for e in es}
    write_vocab(CURRENT_VOCAB, prefix, sections, selected_lines)
    print(f"wrote {CURRENT_VOCAB}; {len(selected_ids)} selected unique entries")

    # Rewrite sentence files from the original full bank, keeping only sentences
    # whose every vocabulary entry is selected somewhere in the new course.
    for sec, fname in SENT_FILES:
        lines = open(FULL_SENT[sec], encoding='utf-8').read().splitlines()
        out, lesson, seq = [], None, 0
        removed = 0
        for line in lines:
            m = re.match(r'### Lesson (\d+):', line)
            if m:
                lesson = int(m.group(1)); seq = 0; out.append(line); continue
            m = re.match(r'\*\*New in this lesson \(teaching order\):\*\*\s*(.*)', line.strip())
            if m and lesson in selected:
                names = [e['display'] for e in selected[lesson]]
                out.append('**New in this lesson (teaching order):** ' + ', '.join(names))
                continue
            m = re.match(r'(\d+)\.\s+(.+?)\s+->\s+(.+)', line.strip())
            if m and lesson is not None:
                bal, en = m.group(2), m.group(3)
                toks = norm(bal).split()
                tags = tags_in(toks, full_matcher)
                # Also reject suffix/name matches whose base entry is unselected.
                bad = False
                i = 0
                while i < len(toks):
                    e, ln = full_matcher.match(toks, i)
                    if e:
                        if e not in selected_ids:
                            bad = True; break
                        i += ln; continue
                    t = toks[i]
                    if t in NAMES or t in EXC:
                        i += 1; continue
                    for suf in ('ne', 'e', 'n'):
                        if t.endswith(suf) and len(t) > len(suf):
                            e2, _ = full_matcher.match([t[:-len(suf)]], 0)
                            if e2 and e2 not in selected_ids:
                                bad = True; break
                    if bad: break
                    i += 1
                if bad:
                    removed += 1
                    continue
                seq += 1
                out.append(f"{seq}. {bal} -> {en}")
                continue
            out.append(line)
        with open(os.path.join(ROOT, fname), 'w', encoding='utf-8') as f:
            f.write('\n'.join(out).rstrip() + '\n')
        print(f"wrote {fname}; removed {removed} unselected-vocabulary sentences")

    # Keep a record of unique original entries that are still not selected.
    old = {}
    if os.path.exists(RETIRED_JSON):
        try:
            for r in json.load(open(RETIRED_JSON, encoding='utf-8')).get('retired', []):
                old[r['id']] = r
        except Exception:
            pass
    first_lesson = {}
    for L, es in full_by.items():
        for e in es:
            first_lesson.setdefault(e['id'], L)
    remaining = []
    for eid, L in sorted(first_lesson.items(), key=lambda x: (x[1], x[0])):
        if eid in selected_ids:
            continue
        r = old.get(eid, {})
        remaining.append({
            'id': eid,
            'lesson': L,
            'gloss': r.get('gloss', ''),
            'register': r.get('register', ''),
            'reason': r.get('reason', f'not selected in the {TARGET}-entry course cap'),
        })
    json.dump({'retired': remaining}, open(RETIRED_JSON, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    print(f"retired record updated: {len(remaining)} unique entries still outside the course")


if __name__ == '__main__':
    main()
