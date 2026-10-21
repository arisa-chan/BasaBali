#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
download_dictionary.py — downloads the BASAbali / BASAibu Wiki dictionary
(https://dictionary.basabali.org, the dictionary verified by Balai Bahasa Bali)
and converts its wikitext into the compact JSON used by the app.

Source pages: Category:Balinese Words (27k+ entries).  Only the fields the app
needs are kept: headword, English meaning, part of speech, register (from the
definition's `form=` field) and up to three sourced example sentences.

Usage:
  python3 tools/download_dictionary.py            # resume from checkpoint
  python3 tools/download_dictionary.py --fresh    # ignore checkpoint

The checkpoint/output path is tools/_dictionary_download.json; build_dictionary.py
merges it with the course vocabulary and writes assets/content/dictionary.json.
"""
import json, os, re, sys, time, urllib.parse, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '_dictionary_download.json')
API = 'https://dictionary.basabali.org/w/api.php'
UA = {'User-Agent': 'BasaBali-app-dictionary/1.0 (educational offline dictionary)'}
CATEGORY = 'Category:Balinese Words'


def clean_wiki(s):
    if not s:
        return ''
    s = re.sub(r'\[\[(?:[^|\]]*\|)?([^\]]*)\]\]', r'\1', s)
    s = re.sub(r"'{2,}", '', s)
    s = re.sub(r'<[^>]+>', '', s)
    s = s.replace('&nbsp;', ' ').replace('&amp;', '&').replace('\xa0', ' ')
    return re.sub(r'\s+', ' ', s).strip()


def template_fields(body):
    out, cur, buf = {}, None, []
    for line in body.splitlines():
        m = re.match(r'\|\s*([A-Za-z ]+?)\s*=\s*(.*)$', line)
        if m:
            if cur:
                out[cur] = '\n'.join(buf).strip()
            cur, buf = m.group(1).strip().lower(), [m.group(2)]
        elif cur is not None:
            buf.append(line)
    if cur:
        out[cur] = '\n'.join(buf).strip()
    return out


def parse_entry(text):
    if not text or not isinstance(text, str) or 'Balinese Word' not in text:
        return None
    m = re.search(r'\{\{Balinese Word\s*(.*?)\n\}\}', text, re.S)
    body = m.group(1) if m else text
    f = template_fields(body)
    en = clean_wiki(f.get('english translations', ''))
    defs, forms, pos = [], [], ''
    for dm in re.finditer(r'\{\{Balinese Word/Definition Object\s*(.*?)\}\}', text, re.S):
        df = template_fields(dm.group(1))
        d = clean_wiki(df.get('definition', ''))
        form = clean_wiki(df.get('form', ''))
        if form and form not in forms:
            forms.append(form)
        if df.get('language') == 'en' and d:
            defs.append(d)
        p = clean_wiki(df.get('part of speech', ''))
        if p and not pos:
            pos = p
    if not en and defs:
        en = defs[0]
    if not en:
        return None
    examples = []
    for em in re.finditer(r'\{\{Balinese Word/Example\s*(.*?)\}\}', text, re.S):
        ef = template_fields(em.group(1))
        ban = clean_wiki(ef.get('ban', ''))
        den = clean_wiki(ef.get('en', ''))
        if ban and len(ban) <= 700:
            examples.append({'bal': ban, 'en': den[:600]})
        if len(examples) >= 2:
            break
    return {
        'english': en[:500],
        'definitions': defs[:2],
        'pos': pos,
        'register': ' / '.join(forms),
        'examples': examples,
    }


def api(params):
    url = API + '?' + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers=UA)
    last = None
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=70) as r:
                return json.load(r)
        except Exception as e:
            last = e
            print(f"  retry {attempt + 1}: {e}", flush=True)
            time.sleep(2 + attempt * 2)
    raise last


def main():
    fresh = '--fresh' in sys.argv
    data = {'source': 'https://dictionary.basabali.org (Category:Balinese Words)',
            'entries': {}, 'continue': ''}
    if os.path.exists(OUT) and not fresh:
        try:
            data = json.load(open(OUT, encoding='utf-8'))
            print(f"resuming: {len(data.get('entries', {}))} entries, "
                  f"continue={data.get('continue','')!r}", flush=True)
        except Exception:
            pass
    entries = data.setdefault('entries', {})
    cont = data.get('continue', '')
    batch = 0
    while True:
        params = {
            'action': 'query', 'format': 'json', 'generator': 'categorymembers',
            'gcmtitle': CATEGORY, 'gcmnamespace': '0', 'gcmlimit': '50',
            'prop': 'revisions', 'rvprop': 'content', 'rvslots': 'main',
        }
        if cont:
            params['gcmcontinue'] = cont
        d = api(params)
        pages = d.get('query', {}).get('pages', {})
        for p in pages.values():
            title = p.get('title', '')
            try:
                text = p['revisions'][0]['slots']['main']['*']
            except Exception:
                continue
            parsed = parse_entry(text)
            if parsed:
                key = re.sub(r'\s+', ' ', title.strip().lower())
                parsed['headword'] = title.strip()
                entries[key] = parsed
        cont = d.get('continue', {}).get('gcmcontinue', '')
        data['continue'] = cont
        batch += 1
        if batch % 10 == 0:
            json.dump(data, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
            print(f"  batch {batch}: {len(entries)} entries", flush=True)
        if not cont:
            break
        time.sleep(0.1)
    json.dump(data, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
    print(f"done: {len(entries)} dictionary entries -> {OUT}", flush=True)


if __name__ == '__main__':
    main()
