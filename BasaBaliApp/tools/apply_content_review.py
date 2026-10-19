# -*- coding: utf-8 -*-
"""
apply_content_review.py — applies the native-speaker review corrections to the
BasaBali course sources (uploads/balinese_course_vocabulary.md and the three
balinese_course_sentences_section*.md files).

Only items the review states with a concrete correction are changed here.
Everything uncertain stays untouched and is listed in CONTENT_REVIEW.md.

Run:  python tools/apply_content_review.py         (from the workspace root)
Then: python tools/build_content.py
"""
import os, re, sys, collections

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
SEC = {1: os.path.join(ROOT, 'balinese_course_sentences_section1.md'),
       2: os.path.join(ROOT, 'balinese_course_sentences_section2.md'),
       3: os.path.join(ROOT, 'balinese_course_sentences_section3.md')}

report = []

# ------------------------------------------------------------------ vocabulary
# entry -> (gloss, register) | None = delete the entry
VOCAB_FIX = {
    # 1. Critical: wrong meaning / unusable forms
    'ngampura':         None,                                  # "to forgive", not "to apologize"
    'saking napi?':     None,                                  # napi = "what"
    'linggih':          ('to reside / stay (respectful)', 'Alus'),
    'tulung':           ('help! (call for help; noun/exclamation, not a verb)', 'Mider'),
    'be':               ('fish/meat', 'Andap'),                # remove the bogus "already (condition met)"
    'becik pisan':      ('very good', 'Alus'),
    'kulkul':           ('wooden slit drum', 'Mider'),         # was wrongly glossed "thunder" in L23
    'mewali':           ('to return', 'Alus'),
    'suksema':          None,                                  # not standard — suksma / matur suksma
    'suksma':           ('thank you', 'Mider'),                # neutral standard, not Andap-only
    'suksema mewali':   None,
    'rage sing engken': None,
    # 2. Invented / unattested / non-Balinese forms
    'kakiyang':         None,
    'kakek':            None,
    'mabelas':          None,
    'telung benang':    None,
    'tangi':            None,
    'kueh':             None,
    'selem denges':     None,
    'putih sentak':     None,
    'barak biing':      None,
    'dhuwur':           None,                                  # Javanese; baduwur is Balinese
    'padha':            None,                                  # Javanese; patuh is Balinese
    'adoh':             ('far (loan)', 'Mider'),
    'wani':             ('brave (loan)', 'Mider'),
    'minum':            None,
    'lelah':            None,
    'kenapa':           ('why (loan)', 'Mider'),
    'kucing':           ('cat (loan)', 'Mider'),
    'siang':            ('afternoon (loan)', 'Mider'),
    'sore':             ('evening (loan)', 'Mider'),
    'rong tali':        None,                                  # duang tali is the form used in L41
    'mis':              None,                                  # not a Balinese Alus form
    'prabu':            None,                                  # means "king", not "head"
    # 3. Register labels
    'nanging':          ('but', 'Mider'),
    'gelah tiange':     ('my (belonging)', 'Andap'),
    'sisi':             ('outside/side', 'Mider'),
    'tengah':           ('middle/inside', 'Mider'),
    'jemet':            ('diligent / hard-working', 'Mider'),
    'sayang':           ('love/affection', 'Mider'),
    'gatra':            ('news', 'Mider'),
    'kocap':            ('it is said', 'Mider'),
    'rame':             ('crowded/busy', 'Andap'),
    'tur':              ('and', 'Alus'),
    'banjar':           ('community hall/ward; local community', 'Mider'),
    # 4. Colour lesson
    'pelung':           ('blue', 'Andap'),
    # 5. Idiom lesson: invented calques removed (see CONTENT_REVIEW.md)
    'buka cicinge':     None,
    'batis beling':     None,
    'baong daki':       None,
    'nyilih mata':      None,
    'basang bungkuk':   None,
    'lima lantang':     None,
    'bungut tuak':      None,
    'paningalan badang':None,
    'ngutang mis':      None,
    # 6. Homonyms flagged by the review as confusing
    'adi':              ('younger sibling', 'Andap'),
    'nyentak':          ('scorching hot', 'Andap'),
}
# entries added (lesson, entry, gloss, register)
VOCAB_ADD = [
    (2,  'sing kenken',      "it's fine / no problem (reply to kenken kabare)", 'Andap'),
    (3,  'nunas ampura',     'please forgive me; I apologize', 'Alus'),
    (3,  'suksma mewali',    "you're welcome", 'Alus'),
    (4,  'pundi',            'where (respectful)', 'Alus'),
    (4,  'saking pundi?',    'where are you from? (respectful)', 'Alus'),
    (5,  'nginum',           'drink', 'Andap'),
    (19, 'ungu',             'purple (loan)', 'Mider'),
    (22, 'limolas',          'fifteen', 'Mider'),
    (22, 'pitung dasa lima', 'seventy-five', 'Mider'),
    (23, 'krebek',           'thunder', 'Mider'),
]
# drop only this sense of a headword that is taught with another meaning
VOCAB_DROP_SENSE = {('paling', 'confused/lost'), ('paling', 'lost/confused')}

def revise_vocab():
    out, lesson = [], None
    for raw in open(VOCAB, encoding='utf-8'):
        line = raw.rstrip('\n')
        m = re.match(r'#### Lesson (\d+):', line)
        if m:
            lesson = int(m.group(1)); out.append(line); continue
        if lesson and line.startswith('* '):
            parts = line[2:].split('->')
            entry = parts[0].strip().rstrip('.').replace('...', '').strip()
            gloss = parts[1].strip() if len(parts) > 1 else ''
            key = entry.lower()
            if key in VOCAB_FIX:
                fix = VOCAB_FIX[key]
                if fix is None:
                    report.append(f"vocab: removed L{lesson} `{entry}` (was \"{gloss}\")")
                    continue
                g, r = fix
                report.append(f"vocab: L{lesson} `{entry}` \"{gloss}\" -> \"{g}\" [{r}]")
                out.append(f"* {entry} -> {g} -> {r}")
                continue
            if (key, gloss) in VOCAB_DROP_SENSE:
                report.append(f"vocab: removed duplicate sense L{lesson} `{entry} -> {gloss}`")
                continue
            out.append(line); continue
        out.append(line)
    text = '\n'.join(out)
    for L, entry, gloss, reg in VOCAB_ADD:
        anchor = f"#### Lesson {L}:"
        i = text.index(anchor)
        j = text.find('\n#### ', i + 1)
        if j == -1: j = len(text)
        text = text[:j] + f"\n* {entry} -> {gloss} -> {reg}" + text[j:]
        report.append(f"vocab: added L{L} `{entry}` -> \"{gloss}\" [{reg}]")
    open(VOCAB, 'w', encoding='utf-8').write(text)

# ------------------------------------------------------------------ sentences
# Safe corpus-wide token fixes (the replaced form is being removed/renamed).
TOKEN_FIX = [
    ('Suksema mewali', 'Suksma mewali'), ('suksema mewali', 'suksma mewali'),
    ('Suksema', 'Suksma'), ('suksema', 'suksma'),
    ('saking napi', 'saking pundi'), ('Saking napi', 'Saking pundi'),
    ('rage sing engken', 'sing kenken'), ('Rage sing engken', 'Sing kenken'),
    ('Tiang ngampura', 'Tiang nunas ampura'), ('Ipun ngampura', 'Ipun nunas ampura'),
    ('Ngampura', 'Nunas ampura'), ('ngampura', 'ngidih pelih'),
    ('Kakiyang', 'Pekak'), ('kakiyang', 'pekak'),
    ('Kakek', 'Pekak'), ('kakek', 'pekak'),
    ('Tangi', 'Ungu'), ('tangi', 'ungu'),
    ('lelah', 'kenyel'), ('Lelah', 'Kenyel'),
    ('minum', 'nginum'), ('Minum', 'Nginum'),
    ('padha', 'patuh'), ('Padha', 'Patuh'),
    ('dhuwur', 'baduwur'), ('Dhuwur', 'Baduwur'),
    ('Tangi', 'Ungu'), ('tangi', 'ungu'),
    ('Mabelas', 'Limolas'), ('mabelas', 'limolas'),
    ('mabelas', 'limolas'), ('Mabelas', 'Limolas'),
    ('telung benang', 'pitung dasa lima'), ('Telung benang', 'Pitung dasa lima'),
    ('rong tali', 'duang tali'), ('Rong tali', 'Duang tali'),
    ('Joh, nenten paek', 'Joh, sing paek'), ('Ditu joh, nenten paek', 'Ditu joh, sing paek'),
    ('A gelas toya', 'Agelas toya'), ('Dua buku ada', 'Duang buku ada'),
    ('Telu piring nasi', 'Telung piring nasi'), ('Tiga gelas teh', 'Tigang gelas teh'),
    ('Papat sampi ada', 'Patang sampi ada'), ('Sia gelas toya', 'Sangang gelas toya'),
    ('Wénten mis ring margi', 'Wénten sampah ring margi'),
    ('Mis ditu liu', 'Luu ditu liu'), ('Ngedasang mis nika', 'Ngedasang sampah nika'),
]
# Lines to delete (lesson-scoped): invented content removed with its vocabulary.
DELETE_IN = {
    19: ['Kueh.', 'Baju niki kueh.', 'Kueh gati warnane.', 'Warnane kueh, icang demen.',
         'Selem denges.', 'Bok icang selem denges.', 'Meong nika selem denges.', 'Cicing ento selem denges gati.',
         'Putih sentak.', 'Baju ida putih sentak.', 'Kucing nika putih sentak.', 'Putih sentak gati asune.',
         'Barak biing.', 'Baju icang barak biing.', 'Warnane barak biing.', 'Barak biing gati, nggih.'],
    68: ['Buka cicinge.', 'Cai buka cicinge!', 'Buka cicinge nika baos kaon.', 'Sampunang maosang buka cicinge!',
         'Paningalan badang.', 'Paningalan badang nika sombong.', 'Cai paningalan badang!', 'Paningalan badang nenten melah.',
         'Baong daki.', 'Cai baong daki, mayus mandi!', 'Sampunang dadi baong daki!', 'Baong daki nika jelek.',
         'Ngutang mis.', 'Maen dogen, ngutang mis!', 'Ngutang mis nika jelek.', 'Cai ngutang mis jani!',
         'Batis beling.', 'Cai batis beling gati!', 'Batis beling nika kenapa?', 'Icang batis beling, icang ulung.',
         'Lima lantang.', 'Cai lima lantang!', 'Lima lantang nika maling.', 'Sampunang lima lantang!',
         'Bungut tuak.', 'Cai bungut tuak!', 'Bungut tuak nika jelek gati.', 'Sampunang bungut tuak ring banjar!',
         'Nyilih mata.', 'Nyilih mata nika jelek.', 'Sampunang nyilih mata!', 'Ia nyilih mata dibi.',
         'Basang bungkuk.', 'Cai basang bungkuk!', 'Basang bungkuk nika jelek.', 'Basang bungkuk ngae rugi.'],
}
# Lesson-scoped line rewrites: lesson -> [(old_substring, new_balinese, new_english)]
REWRITE_IN = {
    2:  [('Tengai, icang dini.', 'Tengai, icang dini.', 'In the afternoon, I am here.')],
    3:  [('Ngampura.', 'Nunas ampura.', 'I apologize / please forgive me.'),
         ('Tiang ngampura.', 'Tiang nunas ampura.', 'I apologize.'),
         ('Ipun ngampura.', 'Ipun nunas ampura.', 'He / she apologizes.'),
         ('Ampurayang, tiang ngampura.', 'Ampurayang, tiang nunas ampura.', 'Please forgive me — I apologize.'),
         ('Sinampura, tiang ngampura.', 'Sinampura, tiang nunas ampura.', 'Apologies — I apologize.'),
         ('Mewali.', 'Mewali.', 'To return.'),
         ('Suksema.', 'Suksma.', 'Thank you.'),
         ('Suksema, nggih.', 'Suksma, nggih.', 'Thank you, yes.'),
         ('Nggih, suksema.', 'Nggih, suksma.', 'Yes, thank you.'),
         ('Suksema mewali.', 'Suksma mewali.', "You're welcome."),
         ('Suksema mewali, nggih.', 'Suksma mewali, nggih.', "You're welcome, yes."),
         ('Suksema mewali, dumogi rahayu.', 'Suksma mewali, dumogi rahayu.', "You're welcome — may you be blessed."),
         ('Ten wenten, suksema.', 'Ten wenten, suksma.', 'No problem, thank you.')],
    22: [('Jam mabelas, icang mulih.', 'Jam limolas, icang mulih.', 'At 3 PM, I go home.')],
    4:  [('Sira pesengan idane?', 'Sira pesengan idane?', 'What is his / her (honored) name?'),
         ('Wastan idane sira?', 'Wastan idane sira?', 'What is his / her (honored) name?')],
    5:  [('Becik pisan.', 'Becik pisan.', 'Very good (refined).')],
    7:  [('Montor ene gelah icange.', 'Montor ene gelah icange.', 'This motorbike is mine.'),
         ('Baju ene gelah nyaine.', 'Baju ene gelah nyaine.', 'This shirt is yours.'),
         ('Hp ene gelah cang.', 'Hp ene gelah cang.', 'This phone is mine.')],
    8:  [('Tiang nongos besik.', 'Tiang nongos besik.', 'I live alone (as one person).')],
    18: [('Prabu.', 'Sirah.', 'Head.'),
         ('Prabu ida sakit?', 'Sirah cai sakit?', 'Does your head hurt?'),
         ('Ida sakit prabu.', 'Sirah icang sakit.', 'My head hurts.'),
         ('Prabu nika ring luhur.', 'Sirah nika ring luhur.', 'The head is at the top.')],
    23: [('Kulkul.', 'Krebek.', 'Thunder.'),
         ('Ada kulkul!', 'Ada krebek!', "There's thunder!"),
         ('Kulkul, icang jejeh gati!', 'Krebek, icang jejeh gati!', "Thunder — I'm so scared!"),
         ('Ujan lan kulkul ada jani.', 'Ujan lan krebek ada jani.', 'There is rain and thunder now.'),
         ('Tatit lan kulkul, icang jejeh.', 'Tatit lan krebek, icang jejeh.', "Lightning and thunder — I'm scared.")],
    58: [('Ngambil napi ragane?', 'Napi sane kaambil ragane?', 'What are you taking?'),
         ('Ipun ngambil ajengan.', 'Ipun mendak ajengan.', 'He / she receives the food.')],
    60: [('Suksma pisan ring ragane.', 'Suksma pisan ring ragane.', 'Thank you very much.')],
    69: [('Suksma lan suksema pateh?', 'Suksma pisan ring ragane.', 'Thank you very much.'),
         ('Suksma lan suksma pateh?', 'Suksma pisan ring ragane.', 'Thank you very much.')],
}

# Non-sentence lines (lesson notes and "teaching order" headers) must match the
# revised vocabulary too, otherwise build_content.py reports ordering warnings.
NON_SENTENCE_FIX = [
    (', kakek,', ','), (', kakiyang,', ','), (', padha,', ','), (', dhuwur,', ','),
    (', lelah,', ','), (', prabu,', ','), (', tangi,', ', ungu,'), (', kueh,', ','),
    (', selem denges,', ','), (', putih sentak,', ','), (', barak biing,', ','),
    (', mabelas,', ', limolas,'), (', telung benang,', ', pitung dasa lima,'),
    (', buka cicinge,', ','), ('`buka cicinge` "like a dog (is)", ', ''),
    ('`lima lantang`, ', ''), ('`batis beling`, ', ''),
    (', batis beling,', ','), (', baong daki,', ','), (', nyilih mata,', ','),
    (', basang bungkuk,', ','), (', lima lantang,', ','), (', bungut tuak,', ','),
    (', paningalan badang,', ','), (', ngutang mis,', ','),
    ('rage sing engken', 'sing kenken'), ('saking napi', 'saking pundi'),
    ('suksema mewali', 'suksma mewali'), ('suksema', 'suksma'), ('ngampura', 'nunas ampura'),
    ('mabelas', 'limolas'), ('telung benang', 'pitung dasa lima'), ('rong tali', 'duang tali'),
    ('dhuwur', 'baduwur'), ('lelah', 'kenyel'), ('prabu', 'sirah'), ('kakiyang', 'pekak'),
    ('kakek', 'pekak'), ('padha', 'patuh'), ('tangi', 'ungu'),
]

def revise_sentences():
    for sec, path in SEC.items():
        lines = open(path, encoding='utf-8').read().split('\n')
        lesson, out, n_tok, n_del, n_rw = None, [], 0, 0, 0
        for line in lines:
            m = re.match(r'### Lesson (\d+):', line)
            if m:
                lesson = int(m.group(1)); out.append(line); continue
            m = re.match(r'(\d+)\.\s+(.+?)\s+->\s+(.+)', line.strip())
            if lesson and m:
                num, bal, en = m.group(1), m.group(2).strip(), m.group(3)
                # deletions
                if lesson in DELETE_IN and bal in DELETE_IN[lesson]:
                    n_del += 1
                    report.append(f"sentence: removed L{lesson} #{num} `{bal}`")
                    continue
                # rewrites
                if lesson in REWRITE_IN:
                    for old, nb, ne in REWRITE_IN[lesson]:
                        if bal == old:
                            if (nb, ne) != (bal, en):
                                report.append(f"sentence: rewrote L{lesson} #{num} `{bal}` -> `{nb}`")
                                n_rw += 1
                            bal, en = nb, ne
                            break
                # safe token fixes within the sentence — word-bounded so that e.g.
                # "matangi" is never touched by the tangi->ungu rename
                for old, new in TOKEN_FIX:
                    if old in bal:
                        bal = re.sub(r'(?<![A-Za-z])' + re.escape(old) + r'(?![A-Za-z])', new, bal)
                        n_tok += 1
                out.append(f"{num}. {bal} -> {en}")
                continue
            # non-sentence line: keep notes / teaching-order headers in sync
            if line.strip():
                newline = line
                for old, new in NON_SENTENCE_FIX:
                    if old in newline:
                        newline = newline.replace(old, new)
                out.append(newline)
                continue
            out.append(line)
        open(path, 'w', encoding='utf-8').write('\n'.join(out))
        print(f"  section {sec}: {n_tok} token fixes, {n_rw} rewrites, {n_del} deletions")

if __name__ == '__main__':
    print("== vocabulary ==")
    revise_vocab()
    print("== sentences ==")
    revise_sentences()
    with open(os.path.join(ROOT, 'CONTENT_FIX_LOG.txt'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(report))
    print(f"== {len(report)} changes logged to CONTENT_FIX_LOG.txt ==")
