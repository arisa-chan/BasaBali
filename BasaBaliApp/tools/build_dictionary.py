#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
build_dictionary.py — compiles the offline Dictionary module asset.

Sources
  * uploads/balinese_course_vocabulary.md — authoritative lesson vocabulary
  * tools/_dictcache.json — checked BASAbali / BASAibu Wiki entries for the course
  * tools/_dictionary_download.json — optional full Category:Balinese Words crawl
    produced by download_dictionary.py (the same community dictionary)

Output: app/src/main/assets/content/dictionary.json
Each entry carries the BASAbali English gloss, part of speech, detailed speech
level (Alus singgih / Alus sor / Mider / Andap / Kasar), sourced examples, and
the course lesson when the word is part of the Lessons database.
"""
import json, os, re, sys, csv, collections

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))       # workspace root
PROJ = os.path.dirname(HERE)                       # BasaBaliApp
VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
CACHE = os.path.join(HERE, '_dictcache.json')
DOWNLOAD = os.path.join(HERE, '_dictionary_download.json')
TREEBANK = os.path.join(HERE, 'treebank.tsv')
OUT = os.path.join(PROJ, 'app', 'src', 'main', 'assets', 'content', 'dictionary.json')

sys.path.insert(0, HERE)
from download_dictionary import parse_entry, clean_wiki



def trim_text(s, limit):
    s = (s or '').strip()
    if len(s) <= limit:
        return s
    cut = s[:limit]
    sp = cut.rfind(' ')
    return (cut[:sp] if sp > 40 else cut).rstrip(' ,;:') + '…'


def clean_examples(examples):
    out = []
    for ex in examples[:2]:
        bal = trim_text(ex.get('bal', ''), 220)
        en = trim_text(ex.get('en', ''), 240)
        if bal:
            out.append({'bal': bal, 'en': en})
    return out

def norm(s):
    s = s.lower().replace('é', 'e').replace('è', 'e').replace('…', ' ')
    s = re.sub(r"[^\w\s']", ' ', s)
    return ' '.join(s.split())


def load_course():
    """id -> {lesson, gloss, register, headword} keeping the first occurrence."""
    out = {}
    lesson = None
    for line in open(VOCAB, encoding='utf-8'):
        m = re.match(r'#### Lesson (\d+):', line)
        if m:
            lesson = int(m.group(1))
            continue
        if lesson and line.startswith('* '):
            parts = [p.strip() for p in line[2:].split('->')]
            if len(parts) < 2:
                continue
            head = parts[0].rstrip('.').replace('...', '').strip()
            key = norm(head)
            if key and key not in out:
                out[key] = {
                    'lesson': lesson,
                    'gloss': parts[1].strip(),
                    'register': parts[2].strip() if len(parts) > 2 else '',
                    'headword': head,
                }
    return out


def simple_register(label):
    low = (label or '').lower()
    if 'singgih' in low: return 'Alus singgih'
    if 'sor' in low: return 'Alus sor'
    if 'madya' in low: return 'Alus madya'
    if 'alus' in low: return 'Alus mider'
    if 'andap' in low: return 'Andap'
    if 'kasar' in low: return 'Kasar'
    if 'mider' in low: return 'Mider'
    return label or ''



POS_NAMES = {
    'noun': 'noun', 'verb': 'verb', 'adj': 'adjective', 'adv': 'adverb',
    'pron': 'pronoun', 'num': 'numeral', 'adp': 'preposition',
    'cconj': 'conjunction', 'sconj': 'conjunction', 'det': 'determiner',
    'pcle': 'particle', 'neg': 'negation', 'intj': 'interjection',
    'clf': 'classifier', 'propn': 'proper noun',
}


def load_treebank_pos():
    counts = collections.defaultdict(collections.Counter)
    if not os.path.exists(TREEBANK):
        return {}
    try:
        with open(TREEBANK, encoding='utf-8') as f:
            for row in csv.DictReader(f, delimiter='\t'):
                tok = (row.get('token') or '').lower()
                pos = row.get('pos') or ''
                if tok and pos:
                    counts[tok][pos] += 1
    except Exception:
        return {}
    out = {}
    for tok, c in counts.items():
        tag = c.most_common(1)[0][0]
        out[tok] = POS_NAMES.get(tag, tag)
    return out

def main():
    course = load_course()
    tb_pos = load_treebank_pos()
    raw = {}
    # Start with the verified course cross-check cache, then overlay any extra
    # entries from the (resumable) Category:Balinese Words crawl.
    cache = json.load(open(CACHE, encoding='utf-8'))
    for k, text in cache.items():
        p = parse_entry(text)
        if p and p.get('english'):
            p['headword'] = k
            raw[norm(k)] = p
    print(f"course dictionary cache: {len(raw)} entries")
    if os.path.exists(DOWNLOAD):
        try:
            d = json.load(open(DOWNLOAD, encoding='utf-8'))
            added = 0
            for k, v in d.get('entries', {}).items():
                if isinstance(v, dict) and v.get('english'):
                    key = norm(k)
                    if key not in raw:
                        added += 1
                    raw[key] = v
            print(f"extra downloaded entries: {added} (total {len(raw)})")
        except Exception as e:
            print(f"could not read download checkpoint: {e}")

    entries = []
    used = set()
    for key, c in course.items():
        if not key:
            continue
        d = raw.get(key) or {}
        en = (d.get('english') or c['gloss']).strip()
        reg = (d.get('register') or '').strip() or simple_register(c['register'])
        pos = (d.get('pos') or '').strip()
        if not pos:
            parts = key.split()
            pos = tb_pos.get(key, '') if len(parts) == 1 else 'phrase'
        if not pos and key == 'wenten':
            pos = 'verb'
        entries.append({
            'id': key,
            'headword': c['headword'],
            'english': en,
            'definitions': d.get('definitions', [])[:2],
            'pos': pos,
            'register': reg,
            'examples': clean_examples(d.get('examples', [])),
            'lesson': c['lesson'],
        })
        used.add(key)

    extras = 0
    for key, d in raw.items():
        if not key or key in used or not d.get('english'):
            continue
        entries.append({
            'id': key,
            'headword': d.get('headword') or key,
            'english': d['english'],
            'definitions': d.get('definitions', [])[:2],
            'pos': d.get('pos', ''),
            'register': d.get('register', ''),
            'examples': clean_examples(d.get('examples', [])),
            'lesson': 0,
        })
        used.add(key)
        extras += 1
        if extras >= 30000:
            break

    entries.sort(key=lambda e: (e['headword'].lower(), e['id']))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump({'source': 'BASAbali / BASAibu Wiki dictionary (dictionary.basabali.org)',
                   'entries': entries}, f, ensure_ascii=False, separators=(',', ':'))
    print(f"wrote {len(entries)} dictionary entries ({len(course)} course, {extras} extra) "
          f"-> {OUT} ({os.path.getsize(OUT)//1024} KB)")


if __name__ == '__main__':
    main()
