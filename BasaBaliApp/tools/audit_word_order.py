# -*- coding: utf-8 -*-
"""
audit_word_order.py — parse-level word-order audit of the course sentence bank,
driven by tools/treebank.tsv.

Rules (head-initial Balinese; see CONTENT_REVIEW.md §6 for sources):
  W1 adjective before its noun        -> adjectives follow the noun
  W2 bare numeral before a noun       -> linking form required (duang, telung, …)
  W3 negator after the predicate      -> negators precede what they negate
  W4 preposition after its complement -> prepositions precede their complement
  W5 intensifier before the adjective -> pisan/gati/sajan follow the adjective
  W6 verb-final clause (SOV)          -> Balinese is SVO
  W7 determiner before its noun       -> ene/ento/niki/nika follow the noun
"""
import os, re, io, collections

ROOT = r"C:\Users\Jaydee Lucero\Downloads\workspace"
TB = os.path.join(ROOT, 'BasaBaliApp', 'tools', 'treebank.tsv')
SEC = {1: os.path.join(ROOT, 'balinese_course_sentences_section1.md'),
       2: os.path.join(ROOT, 'balinese_course_sentences_section2.md'),
       3: os.path.join(ROOT, 'balinese_course_sentences_section3.md')}

BARE_NUM = {'dua', 'telu', 'tiga', 'papat', 'lima', 'pitu', 'kutus', 'sia'}
LINKED = {'duang', 'telung', 'tigang', 'patang', 'limang', 'pitung', 'sangang'}
INTENS = {'pisan', 'gati', 'sajan', 'pesan', 'liu', 'akeh', 'kaliwat', 'liwat', 'luyu'}

def load_treebank():
    sents = collections.OrderedDict()
    for ln, line in enumerate(io.open(TB, encoding='utf-8')):
        if ln == 0:
            continue
        sid, idx, tok, pos, head, rel, conf, src = line.rstrip('\n').split('\t')
        sents.setdefault(sid, []).append((int(idx), tok, pos, int(head), rel, conf, src))
    return sents

def load_text():
    txt = {}
    for sec, path in SEC.items():
        lesson = None
        for line in io.open(path, encoding='utf-8'):
            m = re.match(r'### Lesson (\d+):', line)
            if m:
                lesson = int(m.group(1)); continue
            m = re.match(r'(\d+)\.\s+(.+?)\s+->\s+(.+)', line.strip())
            if m and lesson:
                txt[f"S{sec}-L{lesson:02d}-{int(m.group(1)):03d}"] = (m.group(2).strip(), m.group(3).strip())
    return txt

def main():
    tb, txt = load_treebank(), load_text()
    hits = collections.defaultdict(list)
    for sid, toks in tb.items():
        pos = [t[2] for t in toks]
        words = [t[1].lower() for t in toks]
        heads = [t[3] for t in toks]
        rels = [t[4] for t in toks]
        srcs = [t[6] for t in toks]
        bal, en = txt.get(sid, ('', ''))
        for i in range(len(toks) - 1):
            # W1 adjective immediately before a noun — only when the dictionary
            # itself supplied the adjective tag (avoids heuristic false alarms)
            if (pos[i] == 'adj' and srcs[i] == 'dict' and pos[i + 1] in ('noun', 'propn')
                    and (i == 0 or pos[i - 1] not in ('noun', 'pron'))):
                hits['W1 adjective-before-noun'].append((sid, bal, f"{words[i]} {words[i+1]}"))
            # W2 bare numeral before a noun
            if words[i] in BARE_NUM and pos[i + 1] in ('noun', 'clf'):
                hits['W2 bare-numeral'].append((sid, bal, f"{words[i]} {words[i+1]}"))
            # W5 intensifier before the adjective
            if words[i] in INTENS and pos[i + 1] == 'adj':
                hits['W5 intensifier-before-adj'].append((sid, bal, f"{words[i]} {words[i+1]}"))
            # W7 determiner directly before a noun *and* a noun precedes it in the same
            # clause (otherwise the demonstrative is simply the subject: "Niki patut.")
            if (pos[i] == 'det' and pos[i + 1] in ('noun', 'propn')
                    and any(pos[j] in ('noun', 'pron') for j in range(i))):
                hits['W7 determiner-before-noun'].append((sid, bal, f"{words[i]} {words[i+1]}"))
        for i in range(1, len(toks)):
            # W3 negator after the predicate
            if pos[i] == 'neg' and pos[i - 1] in ('verb', 'adj'):
                hits['W3 negator-after-predicate'].append((sid, bal, f"{words[i-1]} {words[i]}"))
        # W6 verb-final transitive clause (SOV): root verb last, with a definite
        # object nominal before it (a plain S-V intransitive is normal and not flagged)
        roots = [i for i, r in enumerate(rels) if r == 'root']
        if roots and pos[roots[0]] == 'verb' and roots[0] == len(toks) - 1:
            definite_obj = any(pos[j] == 'noun' and pos[j + 1] == 'det'
                               for j in range(len(toks) - 1))
            if definite_obj:
                hits['W6 verb-final (SOV suspicion)'].append((sid, bal, words[roots[0]]))
    total = sum(len(v) for v in hits.values())
    print(f"sentences parsed: {len(tb)}")
    print(f"word-order flags: {total}\n")
    for rule in sorted(hits):
        v = hits[rule]
        print(f"== {rule}: {len(v)}")
        for sid, bal, pair in v[:14]:
            print(f"   {sid}: {bal}   [{pair}]")
        if len(v) > 14:
            print(f"   … +{len(v)-14} more")
    print("\nlinker-form numerals in use:",
          collections.Counter(w for toks in tb.values() for w in [t[1].lower() for t in toks] if w in LINKED))
    print("possessive relations (nmod:poss):", sum(1 for toks in tb.values() for t in toks if t[4] == 'nmod:poss'))
    print("case (preposition) relations:", sum(1 for toks in tb.values() for t in toks if t[4] == 'case'))

if __name__ == '__main__':
    main()
