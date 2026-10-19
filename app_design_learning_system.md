# Duolingo-Style App — Feature List & Learning-System Design
### Companion design doc for the Bahasa Bali course (Lessons 1–70, ~8,600 sentence bank)

---

## 1. Feature inventory (what a Duolingo-like app has)

**Curriculum & content**
1. **Linear learning path** — Units → Lessons → subsections ("chunks"). Our Section 1/2/3 map naturally to 3 milestone tiers (A1 intro, A1 builder, A2); each of the 70 lessons becomes ~5–6 chunk-sessions.
2. **Exercise engine** — a catalog of exercise types (Section 2 below) rendered from tagged sentence data.
3. **Lesson notes/tips** — short grammar cards before/inside a lesson. Our MD headers ("Grammar note", "Cultural note") are ready-made content.
4. **Stories / reading passages** — longer narrative reading with inline comprehension checks (unlockable from ~Lesson 30; our L52 storytelling vocabulary + `ada kone…` frame is designed for this).
5. **Placement test** — optional skip-ahead onboarding (check out of Lessons 1–10 quickly).

**Review & personalization**
6. **Spaced-repetition practice hub** — personalized daily review built from two SRS stores: *words* and *grammar patterns* (Sections 4–5).
7. **Difficulty model** — predicts P(correct) per exercise per learner (Duolingo calls this "Birdbrain"); sessions target ~80% success ("desirable difficulty").
8. **Error analytics** — per-word and per-pattern weakness tracking ("you keep missing `di` vs `ring`").

**Session mechanics**
9. **Hearts/energy** (mistake budget) or unlimited-with-streak variant.
10. **Immediate feedback + in-session re-teaching** — a missed item reappears 1–3 exercises later in the same session, with slightly more scaffolding.
11. **XP, session-complete screens, daily goals.**

**Gamification & retention**
12. **Streaks** (+ streak freeze/repair), **leagues/leaderboards**, **quests** (daily/weekly), **badges/achievements**, **friend quests**.

**Skills & media**
13. **Audio** — TTS or recorded audio per sentence, normal + slow speed; listening exercises.
14. **Speaking** — speech recognition (read-aloud, then free-answer). *For Balinese: no off-the-shelf ASR — plan human recording + shadowing/self-check first (Section 7).*
15. **Word strength visualization** — per-word bars (weak → gold), unit mastery rings.

**Progress & meta**
16. **Unit tests / legendary levels** — harder re-runs of completed material for mastery.
17. **CEFR-style level readout** (A1 → A2), calendar heatmap, words-learned counters.
18. **Offline mode**, accessibility (dyslexia font, reduced motion), settings for reminder timing.

*(Duolingo Max-style AI features — roleplay chat, "Explain my answer", video call — are optional v2+ additions, not core.)*

---

## 2. Exercise types & when each first appears

### 2.1 The catalog

| # | Exercise type | What the learner does | Cognitive skill | First appears |
|---|---|---|---|---|
| E1 | **Word card / image reveal** | See new word + translation (+picture/audio), tap to continue | Encoding (zero recall) | Course Lesson 1, first exercise for each new word |
| E2 | **Match pairs** | Tap L2 word ↔ English translation pairs (timed board of 5–10) | Rapid recognition | Course Lesson 1 |
| E3 | **Translate L2→L1 (multiple choice)** | "What does *Nggih* mean?" pick 1 of 3–4 | Recognition | Course Lesson 1 |
| E4 | **Translate L1→L2 (word bank)** | Build the Balinese sentence by tapping tiles | Assisted production | Course Lesson 1 (late in session) |
| E5 | **Listen & tap** | Hear audio, pick the matching sentence/word | Listening recognition | Unit 1, after ~2–3 lessons |
| E6 | **Listen & type (dictation)** | Hear audio, type the sentence | Listening + spelling | End of Unit 1 / Unit 2 |
| E7 | **Fill-in-the-blank (tap or type)** | Complete "Ida ___ ring pura." (malinggih / negak / …) | Grammar-slot production | End of Unit 1 / early Unit 2 — the workhorse for grammar |
| E8 | **Translate L1→L2 (free typing)** | Type the whole sentence, no tiles | Unassisted production | Unit 2 (after ~1–2 weeks of study) |
| E9 | **Speak: read aloud** | Say the displayed sentence; ASR scores | Pronunciation | Mid-course, permission-gated (defer for Balinese → "shadow & self-rate") |
| E10 | **Speak: answer a question** | Respond freely to a prompt | Fluency | Late course / v2 |
| E11 | **Story interaction** | Read/listen a dialogue, answer comprehension taps | Extended reading | From ~Lesson 30 (after enough vocab) |
| E12 | **Timed challenge** | Mixed review under clock | Fluency under pressure | After first unit completion |
| E13 | **Register choice (course-specific!)** | "Who is being spoken about — pick the right verb: *Ida ___* (ngajeng / madaar)" | Sociolinguistic competence | Lesson 5 onward (first Alus/Andap verb pairs) |
| E14 | **Sort the sentence (scramble)** | Reorder word cards into S-V-O order | Syntax | Unit 1–2 (variant of E4 without translation) |

### 2.2 First appearance *within* a single lesson session

A new word climbs a fixed **format ladder** across its encounters — production formats never appear at first exposure:

```
Encounter 1 (in the introducing session):
  E1 word card  →  E3 L2→L1 choice  →  E4 word-bank build
Encounter 2 (same session or next-day review):
  E3/E5 (recognition, no translation shown)  →  E4 or E7
Encounter 3+ (SRS reviews, days later):
  E6 dictation / E7 fill-blank / E8 free typing, as maturity grows
Mature word (≥5 successes): any format, incl. timed.
```

### 2.3 Anatomy of one ~14-exercise session (new-content lesson)

```
 1  E1/E2  present the chunk's new words (≤5) — answers visible
 2  E3     recognize new word #1 (L2→L1)
 3  E4     produce new word #1 in sentence (word bank)
 4  E3/E14 review word from previous chunk (interleave!)
 5  E4     produce new word #2
 6  E5     listen & tap — new word #3 (if audio ready)
 7  E7     fill-blank on the lesson's grammar pattern
 8  E4     produce new word #3
 9  E13    register choice (from Lesson 5 on)
10  E3     recognize new word #4
11  E6/E4  dictation or build — review item now due
12  E4     produce new word #5
13  E7     grammar pattern again, harder slot
14  E8/E12 one "stretch" item (typing/timed) if session error-free
```
- New items get **2–3 exposures in-session** (more come later via SRS).
- **~40% of the session is review** of prior chunks/lessons even in "new content" lessons; in pure practice-hub sessions it's 100% review.
- Any miss → the exact item (or an easier-format sibling) reappears before session end.

### 2.4 Course-level rollout map (for our Bali curriculum)

| Course position | Unlocked types |
|---|---|
| Lesson 1 (L1 Basics) | E1, E2, E3, E4 |
| Lessons 2–4 | + E5 (listening tap), E14 |
| Lessons 5–10 (end Section 1) | + E13 register choice (L5 eat-verbs!), E7 fill-blank |
| Lessons 11–20 | + E6 dictation, E8 free typing |
| Lessons 21–30 | + E12 timed, unit tests |
| Lessons 31+ (Section 3) | + E11 stories (L52 storytelling is the natural first story unit) |
| Later / v2 | E9, E10 speaking |

---

## 3. How many new words per lesson/chunk? (the 24-word question)

**Recommendation: 24 words → 5 chunks of 5-5-5-5-4. Default chunk size = 5 (range 4–6).**

Why not 6 chunks of 4: 4/chunk is safe but over-fragments — more sessions, more overhead, thinner thematic coherence, and a slower sense of progress. Why not bigger: working memory holds **4±1 novel chunks** (Cowan 2001; the older 7±2 does not apply to brand-new material), and unfamiliar phonology/orthography consumes extra load. Duolingo's own lessons typically introduce **~4–6 new words** in ~12–18 exercises, with each new word seen 2–4 times per session.

**Adaptive sizing rules (important):**
- **4 per chunk** for: abstract/opaque items (L68 idioms like `paningalan badang`), emotionally-loaded sets, items with tricky audio.
- **5 per chunk** default.
- **6–8 per chunk** for: highly structured, mutually-reinforcing sets — numbers (L22: `solas, roras, telulas…` are one schema), or color terms with the same frame (`Warnane ___`).
- **Keep Alus/Andap pairs in the SAME chunk** (`mulih/mantuk`, `jaen/becik pisan`): they must be learned contrastively; separating them causes retroactive interference. But **cap confusable near-identical forms** at 2 per chunk (e.g., don't put `mis`, `mas`, `mesa`-like minimal pairs together).
- Chunk boundaries should follow **semantic/functional coherence** — our curriculum files already define teaching order in blocks; chunk = contiguous blocks.

**Applied to our course (30–32 entries/lesson):** each lesson → **6 chunks × 5**. Example, Lesson 12 (Places):
`[di, ring, ka, tongos, genah] · [peken, pasar, warung, toko, kota] · [desa, banjar, pura, merajan, sanggah] · [sekolah, kantor, rumah sakit, margi, jalan] · [rurung, tukad, pasih, segara, alas, carik] · [kema, mrika, mai, mriki]`
→ 70 lessons × ~6 = **~420 sessions** total, each 10–15 min — right in Duolingo's course-size ballpark.

**Per-word exposure budget (spaced over time, not crammed in one session):**
- Session 0 (intro): 3 exposures (E1 → E3 → E4). Our sentence bank already provides **3 distinct sentences per word**, one per exposure.
- Then SRS: ~day 1, 2, 4, 7, 15, 30, 60… → a word reaches "gold" after ~7–10 successful retrievals over ~2 months (expanding-retrieval schedule; consistent with Cepeda et al. 2006 meta-analysis on expanding intervals).

---

## 4. Word-level SRS with a sentence bank

### 4.1 The model (Duolingo's published approach: Half-Life Regression)

Duolingo's practice scheduling is driven by **HLR** (Settles & Meeder, ACL 2016 — trained on hundreds of millions of exercise attempts; [paper](https://research.duolingo.com/papers/settles.acl16.pdf), [code](https://github.com/duolingo/halflife-regression)). For each **word (lexeme)** *w* the app stores a state and predicts forgetting:

```
p_recall(w) = 2^(−Δ / h_w)          Δ = time since last exposure
h_w         = h0 · 2^(θ · x_w)      half-life grows with success, shrinks with errors
x_w         = [correct_count, wrong_count, recent-trial outcomes, word difficulty, …]
```

- **Due condition:** `p_recall < τ` (τ ≈ 0.6–0.8). Reviewing near — but not after — the forgetting point is the "desirable difficulty" sweet spot (Bjork); Duolingo's difficulty model [Birdbrain](https://blog.duolingo.com/learning-how-to-help-you-learn-introducing-birdbrain/) similarly targets ~80% expected success per exercise.
- **Correct answer:** `correct_count += 1` for every word tagged on the sentence; Δ reset → half-life roughly doubles (expanding schedule ≈ 1d → 2d → 4d → 7d → 15d → 30d → 60d…).
- **Wrong answer:** `wrong_count += 1`, half-life cut (≈halved), the word is **re-queued inside the session** (1–2 more exposures, easier format), and scheduled again ~10 min–1 day out.
- **Pragmatic v1 alternative:** Leitner boxes / SM-2 (Anki-class) — ship simple, swap in trained HLR (or **FSRS**, the modern open-source SRS) once you have telemetry. Keep the same data schema so the upgrade is drop-in.

### 4.2 Sentence ↔ word linkage (the part that makes a sentence bank work)

Every sentence in the bank carries **tags**:

```json
{
  "id": "L12-S22",
  "bal": "Icang ka peken.",
  "en": "I go to the market.",
  "words": ["icang", "ka", "peken"],
  "patterns": ["preposition-ka-direction"],
  "new_item": "ka",            // ≤1 by curriculum construction!
  "lesson": 12, "register": "andap", "formats_eligible": ["E3","E4","E6","E7","E8"]
}
```

Our curriculum makes tagging nearly free: sentences were authored under the *≤1 new entry* rule with a declared teaching order, so `words` = entries introduced so far that appear in the sentence, and `new_item` is explicit. **Each word maps to ≥3 distinct sentences** → the scheduler can always rotate to an unseen sentence for a due word (novelty preserves the retrieval effort that makes SRS work; reusing the identical sentence turns recall into pattern-matching).

### 4.3 How sentences are chosen from SRS state

```python
def build_review_slots(user, n_slots):
    due_words    = {w for w in user.words    if p_recall(w) < TAU_W}
    due_patterns = {p for p in user.patterns if p_recall(p) < TAU_P}
    chosen = []
    while len(chosen) < n_slots and (due_words or due_patterns):
        cands = [s for s in BANK
                 if s.lesson <= user.path_frontier        # nothing untaught
                 and not s.on_cooldown(user)              # rotation: no repeat until
                                                          #   all ≥3 siblings shown
                 and (s.words & due_words or s.patterns & due_patterns)]
        s = argmax(cands, key=lambda s:
              LAM_W * sum(1 - p_recall(w) for w in s.words & due_words)
            + LAM_P * sum(1 - p_recall(p) for p in s.patterns & due_patterns)
            - LAM_D * abs(estimate_difficulty(s, user) - 0.2)   # Birdbrain: aim ~80% success
            + bonus_if_sentence_reviews_multiple_due_items(s))  # "two-birds" sentences
        chosen.append(with_format(s, user))       # format ladder by min tag maturity
        project_recall_update(s, chosen)          # don't pick 3 sentences sharing one word
    return chosen

def with_format(s, user):
    m = min(maturity(t, user) for t in s.words | s.patterns)   # successes count
    return E3_or_E5 if m<=1 else E4_or_E7 if m<=3 else E6_or_E8
```

Key properties:
- A sentence is eligible iff **all** its words/patterns are already taught (our progression guarantee makes this trivially safe).
- Score = **summed forgetting pressure** of the due tags it covers → naturally prefers sentences that review several weak items at once, while the cooldown + sibling-rotation constraints keep surface variety.
- **Format escalates with word maturity** (recognition → bank → typing), so the same sentence can serve a beginner and an advanced review.

---

## 5. Grammar-pattern SRS (yes — Duolingo really does this)

### 5.1 What Duolingo does

The HLR paper's feature vector *x* includes not only per-lexeme history but also **construct tags** — grammar labels attached to every exercise (Duolingo's course data tags items like *ser-vs-estar*, *past-imperfect*, *negation*). Consequences:
- The trained model estimates forgetting for **grammar constructs** the same way it does for words: a construct you haven't exercised in 3 weeks has a decayed half-life even if all the words inside it are fresh.
- Practice selection therefore surfaces exercises whose **constructs are due**, and the difficulty model uses construct features to predict P(correct).
- Duolingo also researches **error attribution** — deciding *which* tag (word vs grammar) caused a miss — so updates are assigned to the right store rather than smeared across all tags.

### 5.2 Our pattern store for the Bali course

Patterns are extracted from each lesson's **Focus/Grammar note** headers — we already wrote them. Initial inventory (~40 patterns):

| ID | Pattern | First taught | Example sentences (bank) |
|---|---|---|---|
| P-neg-register | `sing/tusing` vs `ten/nenten` choice | L1 | "Ene sing beneh." / "Nika nenten patut." |
| P-exist | `ada/wénten` existential | L1 | "Ada cicing dini." |
| P-zerocopula | N + Adj with no copula | L1 | "Niki patut." |
| P-greet-time | `rahajeng` + time-of-day | L2 | "Rahajeng semeng." |
| P-poss-gelah | thing + `gelah/druwe(n)` + owner | L7 | "Niki gelah tiang." |
| P-give-dative | `baang/wehin` + person + thing | L7 | "Baang icang buku." |
| P-num-noun | numeral + noun order | L8 | "Telu buku ada dini." |
| P-eat-register | `neda` (self) vs `ajeng/ngajeng` (other) vs `madaar` | L5/L11 | "Tiang neda." / "Ida ngajeng." |
| P-prep-di-ring | `di` vs `ring` vs `ka` choice | L12 | "Icang di jumah." / "Tiang ring jumah." |
| P-time-future | time adverbial carries future (pre-`lakar`) | L14 | "Mani icang ka peken." |
| P-adj-order | Noun + Adjective | L15 | "Umah icang gede." |
| P-feel-pred | subject + feeling adjective | L16 | "Tiang liang." |
| P-compar-an | `-an` comparative + `teken` | L39 | "Montor enggalan teken sepeda." |
| P-super-paling | `paling` + adj | L39 | "Gunung Agung paling tegeh." |
| P-prohib | `da/eda/sampunang` + V | L51 | "Da kema!" / "Sampunang mrika." |
| P-story-frame | `ada kone … lantas … suba keto …` | L52 | story exercises |
| P-sor-singgih | humble verb for self, honorific verb for respected other | L11→L58 | "Tiang makta." / "Ida ngandika." |
| …(plus per-lesson micro-patterns) | | | |

Each pattern gets the **same HLR-style state** as a word: `(correct, wrong, last_exposure) → h_p, p_recall(p)`. Because a pattern is exercised by *every* sentence tagged with it, patterns accumulate exposures fast; use a **longer base half-life and a lower due-threshold** than words (patterns are coarser skills — they decay slower but need periodic re-proof: e.g., `h0_pattern = 3·h0_word`, `TAU_P = 0.5–0.7`).

### 5.3 Sentence selection with both stores

Eligibility and scoring extend naturally (see `build_review_slots` above):
- A sentence enters the candidate pool if **any** tag — word *or* pattern — is due.
- Score = `λ_w·Σ(word forgetting) + λ_p·Σ(pattern forgetting)`; tune λ so a due pattern with fresh words still wins a slot (this is how "you haven't practiced negation in 9 days" resurfaces even though `sing`/`ten` are individually strong).
- **Format by pattern maturity too:** immature pattern → E7 fill-the-slot (isolates the grammar) or E3 recognition; mature pattern → full-sentence E8 typing or E13 register choice under time.
- **Pattern-first teaching slot:** in the introducing lesson, the pattern gets E7/E13 with the *newest, most contrastive* pair (e.g., `ngajeng` vs `madaar` with subject `Ida` vs `Icang`).

### 5.4 Error attribution (which store to blame)

| Exercise | Signal | Attribution |
|---|---|---|
| Word-bank build | wrong *content* tile chosen | blame that **word** |
| Word-bank build | right tiles, wrong **order** (e.g., `gelah tiang niki` vs `niki gelah tiang`) | blame the **pattern** |
| Word-bank build | missing/extra function word (`di`, `ring`, `ka`, `-ne`) | blame the **pattern** (P-prep-*) |
| Fill-blank | wrong slot choice among grammar variants (`sing` vs `ten`) | blame **pattern** + that word |
| Multiple choice (meaning) | wrong pick | blame the target **word** |
| Register choice | wrong pick | blame **P-sor-singgih** + the verb pair |
| Free typing | align via edit distance → per-token + order analysis | distribute blame token-vs-order as above |
| Untargetable (audio, total miss) | — | smear across all tags at low weight |

Wrong → that tag's `wrong += 1`, half-life cut, in-session re-queue (easier format of a *different* sentence with the same tag, so the learner re-derives the rule rather than remembering the tile layout).

---

## 6. Worked example — learner's Day 1 and Day 3

**Day 1, Session "L1-C1" (chunk: nggih, inggih, saja, patut, beneh):**
```
E1 cards: nggih, inggih, saja, patut, beneh        (encoding)
E2 match pairs (5 words ↔ English)                 (recognition, fun)
E3 "Nggih." → pick meaning                         (nggih #2)
E4 build "Nggih, patut." from tiles                (patut production)
E3 "Beneh." meaning                                (beneh)
E4 build "Nggih, beneh."                           (combo)
E7 "___, niki patut." (nggih / ten / nenten)       (pattern P-neg-register preview)
…session length 12; misses reappear at slots 10-12.
```
State after: each word `(correct=2–3, wrong=0, h≈1 day)`.

**Day 3 review hub (10 slots):** `nggih` p=0.55 due; `patut` p=0.72 due; pattern `P-zerocopula` p=0.6 due → scheduler picks:
- "Nika beneh." (E6 dictation — beneh also near-due, two-birds bonus)
- "Nggih, niki patut." (E7 blank on `patut`)
- "Ento beneh, aa." (E4 — rotates in a *third* sibling sentence for beneh)
- …plus items from L1-C2/L2 chunks that are due.

---

## 7. Bali-course implementation notes

1. **Audio is the bottleneck.** No production Balinese TTS/ASR exists. Plan: record all ~8,600 bank sentences (native speakers, both registers where paired) — this is a realistic community-scale project (~15–20 h of audio) and unlocks E5/E6 and later E9. Until then, run text-only with listening slots disabled.
2. **`é` handling:** accept both `e`/`é` in typed answers; dictation displays the accent-aware spelling.
3. **Register exercises (E13) are the course's signature item type** — no mainstream course teaches sor-singgih; make it a visible feature ("Politeness level" skill meter).
4. **Tagging pipeline:** parse the three curriculum MD files → sentence bank JSON with `words`, `patterns`, `new_item`, `lesson`, `register` (the validators already tokenize everything; reuse that code).
5. **SRS choice:** v1 Leitner/SM-2 (2 days of work) → v2 FSRS → v3 HLR trained on your own logs. Schema stays: per-tag `(successes, lapses, last_seen, state)`.
6. **Session targets:** 12–16 exercises, ~80% expected success, ≤5 new words, ≥1 pattern drill per session once L5 is reached.

---

## Sources
- Settles & Meeder (2016), *A Trainable Spaced Repetition Model for Language Learning* (ACL) — HLR: https://research.duolingo.com/papers/settles.acl16.pdf · code: https://github.com/duolingo/halflife-regression
- Duolingo Blog (2020), *Introducing Birdbrain* — difficulty model / ~80% target: https://blog.duolingo.com/learning-how-to-help-you-learn-introducing-birdbrain/
- IEEE Spectrum, *How Duolingo's AI Learns What You Need to Learn*: https://spectrum.ieee.org/duolingo
- Learning science: Cowan (2001) 4±1 working memory; Cepeda et al. (2006) spacing meta-analysis; Roediger & Karpicke (2006) testing effect; Bjork — desirable difficulties; Nation (2013) vocabulary batching.
