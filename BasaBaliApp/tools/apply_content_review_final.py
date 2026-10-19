# -*- coding: utf-8 -*-
"""Final pass: teach the linking numerals explicitly, fix sentences that reused
paling/duang-tali in earlier lessons, and drop the duplicate kulkul row in L23."""
import os, re, io
ROOT = r"C:\Users\Jaydee Lucero\Downloads\workspace"
VOCAB = os.path.join(ROOT, 'uploads', 'balinese_course_vocabulary.md')
S1 = os.path.join(ROOT, 'balinese_course_sentences_section1.md')
S2 = os.path.join(ROOT, 'balinese_course_sentences_section2.md')
S3 = os.path.join(ROOT, 'balinese_course_sentences_section3.md')

# ---- 1. vocabulary: remove the duplicate kulkul row in Lesson 23, add linking numerals to L8
out, lesson = [], None
for raw in io.open(VOCAB, encoding='utf-8'):
    line = raw.rstrip('\n')
    m = re.match(r'#### Lesson (\d+):', line)
    if m: lesson = int(m.group(1)); out.append(line); continue
    if lesson == 23 and line.startswith('* kulkul '):
        print("vocab: removed duplicate L23 `kulkul` (taught in L35)"); continue
    out.append(line)
text = '\n'.join(out)
adds = [('duang', 'two (linking form)'), ('telung', 'three (linking form)'),
        ('tigang', 'three (polite linking form)'), ('patang', 'four (linking form)'),
        ('limang', 'five (linking form)'), ('sangang', 'nine (linking form)'),
        ('agelas', 'one glass')]
i = text.index('#### Lesson 8:')
j = text.find('\n#### ', i + 1)
block = '\n* ' + '\n* '.join(f"{e} -> {g} -> Mider" for e, g in adds)
text = text[:j] + block + text[j:]
io.open(VOCAB, 'w', encoding='utf-8').write(text)
print("vocab: added L8 linking numerals:", ', '.join(e for e, _ in adds))

# ---- 2. sentences
L8_NEW = [
    '10. Agelas toya. -> One glass of water.',
    '11. Agelas teh driki. -> One glass of tea is here.',
    '12. Icang ngidih agelas toya. -> I ask for a glass of water.',
    '15. Duang buku ada dini. -> There are two books here.',
    '16. Duang cicing ditu. -> There are two dogs there.',
    '17. Icang ngaba duang gelas. -> I carry two glasses.',
    '24. Telung piring nasi dini. -> Three plates of rice are here.',
    '25. Telung gelas teh driki. -> Three glasses of tea are here.',
    '26. Ia ngaba telung buku. -> He carries three books.',
    '28. Tigang gelas teh driki. -> Three glasses of tea are here.',
    '29. Tigang buku ada dini. -> There are three books here.',
    '32. Tigang piring nasi ditu. -> Three plates of rice are there.',
    '31. Patang sampi ada ditu. -> There are four cows there.',
    '33. Patang cicing ada dini. -> There are four dogs here.',
    '34. Icang ngaba patang piring. -> I carry four plates.',
    '44. Limang gelas toya driki. -> Five glasses of water are here.',
    '45. Limang buku ada dini. -> There are five books here.',
    '46. Ia ngaba limang piring. -> He carries five plates.',
    '57. Sangang gelas toya driki. -> Nine glasses of water are here.',
    '58. Sangang buku ada dini. -> There are nine books here.',
    '59. Sangang piring nasi ditu. -> Nine plates of rice are there.',
]
REWRITE = {
    S2: [('82. Icang paling. -> Confused / lost (familiar).', '82. Icang bingung. -> I am confused.'),
         ('83. Cai paling? Dija umah caine? -> Are you lost? Where is your house?', '83. Cai bingung? Dija umah caine? -> Are you confused? Where is your house?'),
         ('84. Paling gati ia di alas. -> He is so lost in the forest.', '84. Bingung gati ia di alas. -> He is so confused in the forest.'),
         ('106. Aji kopi nika duang tali. -> The price of that coffee is 2,000.', '106. Aji kopi nika limang tali. -> The price of that coffee is 5,000.'),
         ('107. Duang tali, suksma. -> 2,000, thanks.', '107. Limang tali, suksma. -> 5,000, thanks.'),
         ('108. Icang mayar duang tali. -> I pay 2,000.', '108. Icang mayar limang tali. -> I pay 5,000.')],
    S3: [('113. Icang paling di alas. -> I am lost in the forest.', '113. Icang nyasar di alas. -> I am lost in the forest.'),
         ('114. Ia paling, ia jejeh. -> He is lost, he is scared.', '114. Ia nyasar, ia jejeh. -> He is lost, he is scared.'),
         ('115. Paling gati icang jani, dija jalan ka peken? -> I am so lost now — where is the road to the market?', '115. Nyasar gati icang jani, dija jalan ka peken? -> I am so lost now — where is the road to the market?'),
         ('119. Takon ka polisi, cai paling! -> Ask the police — you are lost!', '119. Takon ka polisi, cai nyasar! -> Ask the police — you are lost!'),
         ('11. Napi sane kaambil ragane? -> What are you taking?', '11. Ragane ngambil buku nika? -> Are you taking that book?'),
         ('12. Ipun mendak ajengan. -> He / she receives the food.', '12. Ipun ngambil ajengan. -> He / she takes the food.')],
}
for path, pairs in REWRITE.items():
    txt = io.open(path, encoding='utf-8').read()
    n = 0
    for old, new in pairs:
        if old in txt:
            txt = txt.replace(old, new); n += 1
        else:
            print(f"  !! not found in {os.path.basename(path)}: {old[:60]}")
    io.open(path, 'w', encoding='utf-8').write(txt)
    print(f"{os.path.basename(path)}: {n}/{len(pairs)} rewrites")

# insert the new L8 sentences just before the L9 heading, and extend the L8 order line
txt = io.open(S1, encoding='utf-8').read()
i = txt.index('### Lesson 9:')
txt = txt[:i] + '\n'.join(L8_NEW) + '\n\n' + txt[i:]
txt = txt.replace('dua, kalih, telu, tiga, papat, lima, nenem',
                  'dua, duang, kalih, telu, telung, tiga, tigang, papat, patang, lima, limang, nenem')
txt = txt.replace('kutus, sia, dasa,', 'kutus, sia, sangang, dasa, agelas,')
io.open(S1, 'w', encoding='utf-8').write(txt)
print("section 1: inserted L8 linking-numeral sentences and order-line entries")
