# BasaBali content review — changes and open items

Response to the native-review comments on `balinese_course_vocabulary.md`
(2,171 entries) and the three sentence files (8,597 sentences).

Two things are in this document:

* **Applied** — every item from the review that carried a concrete correction.
  These are in the sources and in the shipped assets.
* **Needs a native check (verify)** — items the review itself marked *(verify)*,
  or where a correction would mean guessing. Per the project's content policy
  these are **left unchanged on purpose** rather than invented; they are listed
  here so a native speaker can settle them in one pass.

Reproduce the applied changes with:

```bash
python tools/apply_content_review.py           # vocabulary + sentence corrections
python tools/apply_content_review_cleanup.py   # orphan sentences + lesson headers
python tools/apply_content_review_final.py     # linking numerals, leftover fixes
python tools/build_content.py                  # regenerate app assets
python validate.py && python validate2.py && python validate3.py
```

All three validators run clean afterwards (every entry keeps ≥3 example
sentences, ≤1 new entry per sentence, no future-lesson vocabulary, no unknown
tokens).

---

## 1. Applied — critical meaning and usage errors

| item | before | after |
|---|---|---|
| `ngampura` | "sorry" (it means *to forgive*) | entry removed; apologies now use **`nunas ampura`** (Alus) and **`ngidih pelih`** (Andap), both added/kept |
| 7 sentences using `ngampura` as "apologize" | `Tiang ngampura` … | `Tiang nunas ampura` … / `ngidih pelih` |
| `saking napi?` | "where are you from?" (*napi* = what) | **`saking pundi?`**; **`pundi`** ("where", Alus) added to L4 |
| `tulung` | glossed as a verb ("help") | now "help! (call for help; noun/exclamation)" — the verbs are `nulungin` (L24) / `ngwantu` (L24) as before |
| `L3 #41/#65` "Tiang tulung …" | tulung used as a verb | sentences now use the phrase form (`nunas tulung` / `ngidih tulung`) |
| `be` (L56) | "already (condition met)" — nonsense | entry removed; `be` remains "fish/meat" |
| `becik pisan` | "very delicious" | "very good"; *delicious* stays with `jaen` / `jaen gati` |
| `linggih` | "origin/live" | "to reside / stay (respectful)" — "origin" dropped |
| `kulkul` (L23) | "thunder" | L23 row removed (it is the slit drum, L35); the L23 thunder sentences now use **`krebek`** |
| `mewali` | "you're welcome / return" | "to return"; the reply is the chunk **`suksma mewali`** |
| `prabu` | "head (Alus)" | entry removed (it means *king*); L18 head sentences use `sirah` |

### Translation mismatches (applied)

* L4 #26 `Sira pesengan idane?` and L4 #33 `Wastan idane sira?` → glossed
  "What is his/her (honored) name?" (were "Who is he/she?").
* L2 #14 `Tengai, icang dini` → "In the afternoon, I am here." (was "midday").
* L7 #40/#52/#66 → `ene` is now translated "This …" (was "That …").
* L8 #4 `Tiang nongos besik` → "I live alone (as one person)." (shows *one*).
* L22 #20 → "At 3 PM, I go home." (the old note said 15:00 for *limolas*).

### Numerals before nouns (applied)

The linking forms are now **taught entries** in Lesson 8 — `duang, telung,
tigang, patang, limang, sangang` (plus `agelas` "one glass") — each with three
sentences, so L8 no longer contradicts L22/L41 (`telung atus`, `duang tali`).
`Agelas toya` is written as one word.

### Numerals that were wrong (applied)

* `mabelas` (15) → **`limolas`**
* `telung benang` (75) → **`pitung dasa lima`**
* `rong tali` (2,000, L22) → **`duang tali`** (the L41 form); the L22 sentences
  that reused it now use `limang tali`, which L22 actually teaches.

---

## 2. Applied — invented forms, loanwords, register labels

**Removed as unattested / non-Balinese in this course's scope**

`kakiyang` (grandfather) · `kakek` (Indonesian; `pekak` is Balinese) ·
`dhuwur` (Javanese; `baduwur` is Balinese) · `padha` (Javanese; `patuh` is
Balinese) · `tangi` (purple — clashes with `matangi`; replaced by the loan
`ungu`) · `kueh`, `selem denges`, `putih sentak`, `barak biing` (invented
colour phrases) · `mis` (not a Balinese Alus form) · `suksema` (not standard —
`suksma` / `matur suksma`) · `rage sing engken` → **`sing kenken`**.

All sentences that used these were rewritten with the surviving vocabulary
(e.g. `kakek` → `pekak`, `lelah` → `kenyel`, `tangi` → `ungu`, `mis` → `luu` /
`sampah`).

**Labelled as loans (Mider) instead of being passed off as Balinese forms**

`kucing` "cat" · `siang` "afternoon" · `sore` "evening" · `kenapa` "why" ·
`adoh` "far" · `wani` "brave" · `lelah` (removed; `kenyel`/`lesu` cover it) ·
`minum` → corrected to **`nginum`** (the Balinese verb).

**Register labels corrected**

* `suksma` → **Mider** (neutral standard, not Andap-only); `suksema` removed.
* `nanging` → **Mider** (neutral/polite, not Andap-only).
* `gelah tiange` → **Andap** (gelah is Andap; mixing it with Alus was wrong).
* Repeated headwords harmonised to one gloss/register: `sisi`, `tengah`,
  `jemet`, `sayang`, `gatra`, `kocap`, `rame`, `tur`, `banjar`, `be`,
  `nyentak`, `paling` (the "lost" sense is dropped — "lost" stays with
  `nyasar` / `kepaling`, `paling` keeps "most"), `adi` (keeps "younger
  sibling"), `pelung` (both rows now "blue").
* `ngabe` / `ngaba` unified to **`ngaba`** everywhere (entry + all sentences).

**Idioms (L68)** — the calques the review flagged are removed with their
sentences: `buka cicinge` (+ the "You are like a dog!" insult), `batis beling`,
`baong daki`, `nyilih mata`, `basang bungkuk`, `lima lantang`, `bungut tuak`,
`paningalan badang`, `ngutang mis`. The lesson keeps `buka batune`,
`ngomong kosong`, `ngadep gumi`, `gedeg basang`, `manis di bibih`,
`tusing nyak kalah`, `nyemak gawe`, `aluh-aluh`, `keto suba`, `pantesne`, etc.

**Register breaks called out in the review were fixed where the correction is
unambiguous** — e.g. `Joh, nenten paek` → `Joh, sing paek` (all-Andap), and
L58 #11 re-ordered to `Ragane ngambil buku nika?`.

---

## 3. Verified from online sources (applied) and what is still open

Every *(verify)* item was checked against online Balinese references —
chiefly the **BASAbali / BASAibu Wiki dictionary** (dictionary.basabali.org,
verified by Balai Bahasa, the Indonesian national language agency), plus
Balinese Wikipedia's numeral list. Findings and actions:

1. **`tonden` / `durung` — the course was right.** The dictionary's
   level-of-speech table for *durung* reads: **Andap = tonden**, **Alus madya =
   dereng**, **Alus mider = durung** (synonyms `dereng` (h) / `tonden` (l);
   antonyms `sampun` (h) / `suba` (l)). No swap. The finer Balai Bahasa tiers
   are now recorded in the register column, and **`dereng` (Alus madya)** was
   added as a taught entry in L32 with three examples.
2. **Thunder / lightning — corrected.** `krebek` = *thunder* (the loud
   thunderclap; Indonesian *petir/halilintar/guntur*) and its level is **Andap**
   — the course had it as Alus, now fixed, and the invented `kulkul` = thunder
   row stays removed. `tatit` = *lightning that strikes at a distance* (gloss
   sharpened).
3. **Honorific verbs — applied.** `ngunjuk` = *drink*, dictionary level **Alus
   singgih** → added to L5 and the honoured-subject sentences now read
   `Ida ngunjuk`, `Ipun ngunjuk driki`, `Ida ngunjuk toya`, `Ida ngunjuk teh`
   (self/polite `nginem` and general Alus `ngajeng` stay for their own levels).
   `ngrayunang` = *eat; high Balinese, used in conversation with pedanda* →
   added to L58 (Alus singgih) with three examples. Still open: the exact tier
   of `sirep` / `mesare` / `makolem` and the `makta` "humble vs honouring"
   note — those need per-word dictionary checks rather than bulk edits.
4. **`tulung` — clarified from the dictionary**: "help; to request help or
   assistance" (a root/noun; derivatives `tulungin`/`nulungin`, exclamation
   `katulung-tulung`). The gloss now says so explicitly, and the verb slots are
   filled by `nulungin` / `ngwantu` in L24 as before. Keeping it at L3 as the
   noun is consistent with the source.
5. **Register-scheme depth — improved within the app's model.** The dictionary
   tiers (Mider · Kasar · Andap · Alus sor · Alus madya · Alus mider · Alus
   singgih) are now written in the register column where the source gives them
   (`durung` Alus mider, `dereng` Alus madya, `ngunjuk`/`ngrayunang` Alus
   singgih), so the source records the real level while the app keeps its
   three-way chip (the build maps any "Alus …" to Alus). Loans are marked
   `(loan)` and labelled Mider.
6. **Numerals — the odd ones verified.** Balinese Wikipedia's *Kruna wilangan*
   confirms **`selae` 25, `pasasur` 35, `samas` 400, `domas` 800, `duang tali`
   2,000** (also `sanga likur` 29, `telung dasa` 30). `setiman` (45),
   `pelekutus` (18) and `siangolas` (19) were not found in the sources checked
   and remain flagged.
7. **Diacritics — resolved.** The standard orthography (BASAbali/Balai Bahasa)
   writes *é* for /e/ (`wénten`, `Krébék`), which matches the course's own
   vocabulary spelling; all 13 plain `wenten` occurrences in display text were
   normalised to **`wénten`**.
8. **`nyampah` "breakfast"** and **`kude` / `engken`** — no source could be
   retrieved to confirm or refute these (search service errors); both are kept
   unchanged and remain flagged in this list rather than guessed at.
9. **L68 as a lesson.** The invented calques stay removed. The surviving
   phrases (`buka batune`, `ngomong kosong`, `keto suba`, `aluh-aluh`,
   `pantesne`, …) are ordinary similes/idioms, but a fully sourced *sesonggan*
   lesson (and the question of insults at A2) is still open.
10. **Sentence naturalness (review §3).** Unchanged for the reason given
    earlier: replacing thousands of sentences needs authoring, not bulk
    substitution. The few concrete fragments the review named can be rewritten
    on request.
11. **Remaining duplicate headwords.** The examples the review gives (`aji`
    price/father, `basa` language/spices, `bulan` month/moon) are genuine
    homonyms and are kept distinct; the ~190-list still needs a per-word pass.
    `CONTENT_FIX_LOG.txt` lists everything applied so far.

### Sources consulted

* [Durung — BASAbali dictionary](https://dictionary.basabali.org/w/index.php?title=Durung&uselang=en)
  (level-of-speech table: Andap `tonden` · Alus madya `dereng` · Alus mider `durung`)
* [Krebek — BASAibu Wiki](https://basaibubali.org/Krebek) (thunder, Andap)
* [Tatit — BASAibu Wiki](https://basaibubali.org/Tatit) (distant lightning)
* [Ngunjuk — BASAibu Wiki](https://basaibubali.org/Ngunjuk) (Alus singgih "drink")
* [Ngrayunang — BASAibu Wiki](https://basaibubali.org/Ngrayunang) (high Balinese "eat")
* [Rayunang / Karayunang](https://basaibubali.org/Rayunang) (same root family)
* [Tulung — BASAibu Wiki](https://basaibubali.org/Tulung) · [Katulung-tulung](https://dictionary.basabali.org/Katulung-tulung)
* [Kruna wilangan — Balinese Wikipedia](https://ban.wikipedia.org/wiki/Kruna_wilangan) (numerals)

---

## 4. Whole-list cross-check against the BASAbali dictionary

`BasaBaliApp/tools/check_dictionary.py` queries **dictionary.basabali.org**
(verified by Balai Bahasa) for **every** vocabulary entry — batched 50 pages per
API call — and additionally for every entry that was removed or renamed in this
revision. The full table (course gloss · dictionary gloss · level of speech ·
verdict) is written to `BasaBaliApp/tools/dictionary_check.tsv`.

**Overall result:** the dictionary returns a definition for the great majority
of the 1,603 entries and the course gloss agrees with it; the flagged
"mismatches" are wording differences (`teka` "come/arrive" vs *Came*,
`gelas` "glass" vs *cup*, `lembu` "cow" vs *bull*, `dahata`…), not wrong
meanings. The genuine errors are the ones below.

### Restored — the dictionary confirms they were right

| entry | dictionary says | action |
|---|---|---|
| `prabu` | "king; villain; **head** (alus singgih)" | restored with its L18 sentences (the `sirah` rewrite reverted) |
| `mis` | "trash; rubbish; garbage" (Mider) | restored as an entry + sentences; only the old *Alus* label was wrong |
| `tangi` | "purple" (Mider) | restored with its sentences; my stop-gap `ungu` removed |
| `putih sentak` | "very white" (Mider) | restored with its sentences |
| `paling` | "confused; eldest; greatest" (Andap) | the "confused" sense is real → restored in L16 with its sentences |

### Confirmed wrong — stays removed

| entry | dictionary says | verdict |
|---|---|---|
| `mabelas` | "weaning (about breastfeeding)"; "separate boxers" | **not** "fifteen" — `limolas` (dict: fifteen) stays |
| `barak biing` | "searing heat" | not "dark red" → stays out of the colour lesson |
| `selem denges` | page exists but carries no gloss | inconclusive → stays out |
| `kulkul` | "slit drum" only | the "thunder" sense stays removed |
| `be` | "fish; meat" | the L56 "already" sense stays removed |
| `rage sing engken` | not in the dictionary | `sing kenken` stays |

### Other corrections taken from the dictionary

* **Numerals resolved** — `selae` 25, `pasasur` 35, `setiman` 45, `pelekutus`
  18, `siangolas` 19, `limolas` 15 are all in the dictionary, so the lesson's
  numeral set is correct (only `mabelas` was bogus).
* `kulkul` level of speech → **Andap** (dictionary level table).
* `nginem` "drink (v)" is **Kasar / Andap**, not Alus → relabelled; `ngunjuk`
  (Alus singgih) keeps the honoured-subject sentences.
* `ampura` → "forgive; excuse me; sorry"; `durus`, `ngiring`, `matur`,
  `semeton`, `ari`, `muani`, `dot`, `kayun` etc. verified as-is.
* `durung` / `tonden` / `dereng` tiers confirmed a second time
  (Andap · Alus madya · Alus mider).
* `makta` = "bring" (Andap / Alus mider); `sirep`, `mesare`, `makolem` all
  "sleep" with overlapping levels (`makolem` includes Alus singgih) — recorded
  in the register column.
* `nyampah` = "breakfast" (Andap) — the reviewer's doubt was unfounded; kept.

### Still open after the cross-check

`linggih` (dictionary: "house; place", Andap — the course's
"to reside / stay (respectful)" should be aligned in a pass of its own);
`kude` / `engken` are dictionary-attested but the Buleleng dialect label is
unconfirmed; the sentence-level naturalness rewrite.

## 5. Sentence-bank audit (all 8,577 sentences)

`BasaBaliApp/tools/check_sentences.py` runs two whole-bank checks and caches its
dictionary lookups (`_dictcache.json`).

### A. Word choice — every token checked against the dictionary

Every distinct Balinese token in the three sentence files was looked up in the
BASAbali dictionary (batching 50 titles per API call). Result:

* **0 tokens are missing from the dictionary** — every word in the bank is
  attested Balinese.
* Only 10 distinct tokens do not correspond one-to-one to a vocabulary entry,
  and all 10 resolve to taught entries or their inflections (e.g. `balih` is
  part of the entry `balih-balihan`, partitive/affix forms). No untaught
  Indonesian/Javanese leftovers remain.

### B. Politeness agreement

Rule set applied (sor/singgih, from the verified tiers): an **Alus-sor
(self-humbling) verb must not be used for an honoured third-person subject**,
and an **Alus-singgih (honouring) verb must not be used with a low subject**
(`cai`/`nyai`/`ia`/`icang`).

* 8 sentences had `neda` ("eat — for oneself", sor) with `ida`/`ipun` →
  changed to **`ngajeng`** (dictionary level: Alus singgih), e.g.
  `Ipun neda ring pidan?` → `Ipun ngajeng ring pidan?`,
  `Ida neda antiga.` → `Ida ngajeng antiga.`,
  `Sesampun neda, ida malinggih.` → `Sesampun ngajeng, ida malinggih.`
* `Ipun nunas ampura.` (self-humbling verb with a third-person subject) →
  rewritten as a first-person apology, `Nggih, tiang nunas ampura.`
* `Ipun ngambil ajengan.` (dictionary: `ngambil` = Andap) with an honoured
  subject → `Titiang ngambil ajengan.` (humble self, correct pairing).
* The remaining 5 automated flags were reviewed and kept: they are the
  *correct* pattern of a first-person humble form addressed to an honoured
  person (`Tiang nunas ampura ring ida`, `Titiang nunas pica ring ida`,
  `Matur suksma pisan ring ida`) plus `nunas`/`mapamit`, which the dictionary
  lists across Andap · Alus sor · Alus singgih, i.e. they may take either role.

### C. Consistency notes recorded, not silently changed

* `ngambil` is **Andap** per the dictionary while the course presents it in the
  deep-Alus lesson; the flagged sentence was fixed, but relabelling the entry
  itself is left for the next pass (the dictionary's level field is
  inconsistent for several verbs — `neda`, `malinggih`, `ngajeng` are each
  listed differently from their conventional tiers).
* `Rahajeng sanja` (Alus greeting + Andap time word) is kept — the collocation
  is conventional in Balinese; a native pass can confirm whether the course
  should prefer `rahajeng sore`.
* Word-order checks (adjective-after-noun, numeral linking, in-situ question
  words) were applied where the course contradicted itself (Lesson 8 numerals);
  a full parse-level word-order audit needs a treebank, not a word list.

## 6. Treebank and parse-level word-order audit

### The treebank

`BasaBaliApp/tools/build_treebank.py` builds a **rule-based dependency treebank**
of the whole bank — 8,581 sentences / 27k tokens — written to
`BasaBaliApp/tools/treebank.tsv` (columns: `sent_id · index · token · pos ·
head · relation · confidence · pos_src`).

* **Part of speech** comes from the dictionary's own `part of speech=` field
  (cached in `_dictcache.json`), marked `pos_src=dict`, plus curated
  function-word lists (`list`) and morphology (`heuristic`) — the provenance
  column is what makes false-positive control possible.
* **Relations** follow the published word-order facts: Sembiran/Plains Balinese
  is an **SVO** language with information-structure-driven alternates
  (Austronesian information-structure volume), head-initial NPs (adjective,
  numeral, possessor, determiner and intensifier all follow their head),
  preposition-initial PPs and pre-predicate negation. Relations produced:
  root 8,581 · obj 4,985 · nsubj 3,921 · nmod:poss 1,556 · advmod 2,156 ·
  det 1,362 · obl 1,290 · case 1,006 · cc 457 · amod 424 · nummod 170 · neg 97.
* This is a **machine-annotated** treebank built to explicit rules — it is not a
  hand-validated gold standard, and it is labelled as such in the file header.

### The audit

`BasaBaliApp/tools/audit_word_order.py` checks the source-grounded order rules
against the treebank:

| rule | flags | verdict |
|---|---|---|
| W1 adjective before its noun | 100 | reviewed: all are fixed formulas (`Rahajeng semeng`, `Nunas ampura`) or quantifiers (`Liu cicing`), not attributive adjectives |
| **W2 bare numeral before a noun** | 24 → **9** | **fixed** where the linking form is required; the remaining 9 are the lesson's deliberate base-numeral exemplars (the linking forms are now taught as their own entries with 3+ examples each) |
| W3 negator after the predicate | 3 | reviewed: all are clause-boundary artefacts (`Mengked, da pesu!` — `da` precedes `pesu`) |
| **W5 intensifier before the adjective** | 6 → **2** | **fixed**: `Gati kebus jani!` → `Kebus gati jani!`, `Kaliwat jaen …` → `Jaen kaliwat …` etc. |
| W6 verb-final (SOV suspicion) | 49 | reviewed: nominal predicates (`Margi nika panjang`) and time-fronted clauses (`Bulan niki icang malajah`) — both normal Balinese |
| W7 determiner before its noun | 505 | reviewed: demonstrative **subjects** (`Niki patut.`) and missing adjective tags in the lexicon; not an order violation |

Fixes applied from the audit: `tools/fix_word_order.py` (intensifier order),
`tools/restore_numeral_counts.py` (base-numeral examples restored with four
counting drills and the missing linking entry `pitung` registered).

The audit also surfaced and fixed a **real data defect**: 21 duplicate sentence
IDs in Lesson 8 left over from an earlier edit (they would have collided in the
app's SRS store); the block was renumbered and the bank now has no duplicate
`(lesson, number)` pairs.

### Grammar-pattern list — enhanced from the treebank

`tools/build_content.py` gained **sentence-level, evidence-based patterns**:
instead of tagging a whole lesson, each new rule attaches its pattern only to
sentences that actually instantiate the structure (counts from the build):

| pattern | lesson | sentences |
|---|---|---|
| `P-conj-and` — coordination with lan / miwah / tur / muah | 1 | 284 |
| `P-reduplication` — repeated form for plurality/intensity | 1 | 65 |
| `P-q-insitu` — question words stay in place | 2 | 637 |
| `P-politeness-agree` — honoured subject + Alus-singgih verb | 5 | 76 |
| `P-numeral-linker` — linking numerals before a noun | 8 | 101 |

The catalog therefore grew **98 → 103 patterns**, with the new ones carrying
their own example sets and first-lesson attribution. Pattern ids flow through
the same SRS tags as before, so the review scheduler picks them up.

### Sources consulted

* [Perspectives on information structure in Austronesian languages](https://langsci-press.org/catalog/view/201/1259/1164-2)
  — Sembiran Balinese is A/S-V-P (SVO) with information-structure alternates
* [Ergativity and Balinese syntax (Yale dissertation)](https://ling.yale.edu/media/417/download) — Balinese clause structure
* [Tata Bahasa Bali (Oka Grenoka)](https://www.scribd.com/document/497970386/TATA-BAHASA-BALI) — traditional word classes
* [Struktur Kalimat dalam Bahasa Bali](https://id.scribd.com/document/497970394/STRUKTUR-BAHASA-BALI) — sentence types (transitive, nominal, adjectival predicates)
* [BASAbali dictionary](https://dictionary.basabali.org/DictionaryIndex) — parts of speech, levels of speech, examples

## 7. Automated checks added

`tools/apply_content_review*.py` are re-runnable and print a full log
(`CONTENT_FIX_LOG.txt`). The existing `validate*.py` plus `tools/build_content.py`
now report:

* 1,597 word entries · 98 patterns · 8,562 sentences · 469 chunks
* 0 sentences without vocabulary tags
* no teaching-order warnings
* every entry with ≥3 example sentences, ≤1 new entry per sentence, no
  future-lesson vocabulary, 0 unknown tokens
