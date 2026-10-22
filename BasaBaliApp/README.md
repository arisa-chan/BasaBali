# BasaBali — a Duolingo-style Android app for learning Balinese from English

A complete, buildable Android Studio project implementing the learning system designed in
`app_design_learning_system.md`, powered by the 70-lesson curriculum
(`balinese_course_sentences_section1/2/3.md` + `balinese_course_vocabulary.md`).

**Content scale:** 1,613 vocabulary entries · 98 grammar patterns · 8,597 tagged sentences ·
**1,506 path steps** (5 units × 70 lessons) · **6 interactive stories** — all shipped offline in the APK assets.

---

## Build & run

1. Install **Android Studio** (Koala 2024.1.1 or newer).
2. `File → Open…` → select the `BasaBaliApp/` folder and let Gradle sync
   (AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.09, compileSdk 34, minSdk 26).
   - CLI alternative: run `gradle wrapper --gradle-version 8.7` once, then `./gradlew assembleDebug`.
3. Run the `app` configuration on a device/emulator with **API 26+**.

No network needed at runtime — course content, SRS, leagues and quests are all on-device.

## What's implemented

### Core loop (v1)
| Design-doc section | Implementation |
|---|---|
| §1 path & sessions | Sections → lessons → **5 units per lesson** (see v3 below), subunits of ~5 words (adaptive: 4 for idioms, 6 for number sets), linear unlock, replay |
| §2 exercise types | **E1** word card · **E2** match pairs · **E3** multiple choice (both directions) · **E4** word bank · **E7** fill-in-the-blank · register chips (`alus/andap/mider`) on every prompt |
| §2.2 format ladder | card → recognition → production per new word; review formats escalate with tag maturity |
| §4 word SRS | Leitner boxes → expanding half-lives, forgetting curve `p = 2^(−Δ/h)`, due at p<0.70 |
| §4.3 sentence selection | candidates = all-words-taught + ≥1 due tag; scored by summed forgetting pressure + sibling-rotation bonus − length penalty; top-3 randomized pick |
| §5 pattern SRS | second store over 98 grammar tags, slower decay, due at p<0.60, weighted 1.5× in scoring |
| §5.4 error attribution | word-bank diffs: content-tile miss → **word** blame; order/function-word error → **pattern** blame |
| Session mechanics | 5 hearts, downgraded re-queue after misses, XP, streaks, JSON persistence |

### v2 additions (this revision)
| Feature | Implementation |
|---|---|
| **Audio (E5/E6)** | **Indonesian TTS proxy (`id-ID`)** speaks the whole 8,597-sentence bank — Balinese shares most phonology and its Mider/loan layer with Indonesian. Listening exercises: *listen & tap* (E5, unlocks after 2 chunks) and *dictation* (E6, after 8 chunks). Normal + 🐢 slow playback; UI degrades gracefully when no TTS engine exists. |
| **Speaking (E9)** | *Read-aloud* with **Indonesian ASR (`id-ID`)**: 5 hypotheses scored by token-level similarity (pass ≥ 0.60), mic permission flow, "couldn't hear you → skip" path, unlocks after 16 chunks when a recognizer exists. |
| **Free typing (E8)** | Type-the-translation with é/e-tolerant normalization; exact = green, ≥0.85 similarity = "almost, watch spelling", else red + requeue. |
| **Stories (E11)** | 6 interactive stories gated by lesson progress (*Sanja di Pasih* L30 → *Tamiu saking Jawa* L62), each line TTS-narrated + translated, followed by 3 comprehension taps. Story text is **vocabulary-gated and machine-validated** by the pipeline (same matcher as the course validators). |
| **FSRS scheduler** | Full **FSRS-4.5** (default weights, power forgetting curve, stability/difficulty) selectable in Settings; hot-swappable with Leitner — states lazily bootstrap in both directions, both models' fields persist. Verified: intervals grow 1→6→33→143 d; a lapse collapses stability 8890 d→51 d. |
| **Leagues** | Offline weekly leagues: 9 generated rivals accruing XP at daily rates, Bronze→Ruby tiers, top-3 promotion / bottom-2 demotion at settlement, live leaderboard with promotion/demotion zones. |
| **Quests** | 3 deterministic daily quests (XP / lessons / perfect answers) with progress bars and a +30 XP completion bonus. |
| **Placement test** | 14 milestone probes across the course (L2→L64); frontier = highest milestone passed at ≥60% cumulative accuracy; earlier chunks are marked done and their words/patterns seeded mid-strength so skipped material resurfaces in SRS review. Retakeable from Settings. |
| **Share** | Native share-sheet for streak/XP. |

### v3 additions (this revision)

**Lesson = 5 units** (e.g. a 30-word lesson → U1: 6 subunits × 5 words · U2: 6 × 5 · U3: 3 · U4: 3 · U5: 1):

| Unit | Content | Sessions |
|---|---|---|
| **U1 · Learn** | one subunit per ~5-word chunk — first teaching pass | card → recognition → production (+listening once unlocked) |
| **U2 · Reinforce** | the same chunks a second time | identical builder; SRS fills deeper review |
| **U3 · Cumulative review** | everything from Lesson 1 → this lesson | 3 subunits × 12 coverage-driven exercises |
| **U4 · Lesson review** | every entry of the current lesson | `ceil(words/10)` subunits × 12 exercises |
| **U5 · Mastery** | full-coverage check of the lesson | a single 14-exercise subunit |

Coverage-driven review repeatedly picks the sentence covering the **most not-yet-covered,
weakest** focus words (then tops up with due-based SRS review) — verified to reach 30/30
entries for a 30-word lesson across U4+U5. Old `L12C3`-style progress ids auto-migrate to
`L12U1S3` on load.

**Audio buttons everywhere** — 🔊 (Indonesian-proxy TTS) on word cards (headword + example),
Balinese multiple-choice prompts, match-pair word tiles, fill-in-the-blank (reads the sentence
with a pause at the blank), the answer line in the feedback banner, and story lines; slow 🐢
playback on listening/dictation/story players.

**Tap-a-word dictionary** — every dictionary word in a displayed Balinese sentence is
**underlined**; tapping opens a **popup dialog** (not a toast) listing *all* English glosses
for that word from the course vocabulary — multi-sense words (`lima` = five/hand, `aji` =
price/father, `kulkul` = thunder/drum) show every sense with register and source lesson.
Lookups are position-aware: multi-word entries (`selawe tali`) resolve for each token they span,
and `-ne/-e/-n` suffixed forms resolve to their base.

**New-word highlight** — when a sentence introduces a vocabulary entry the learner has never
met, that word renders in **light purple** (`NewWordPurple`) in the sentence and on its
word-bank tiles; the highlight disappears automatically once the word is taught (second pass
and reviews render normally).

**Bug fixes in this revision**
1. *Compile error*: multi-char literals `'.,!?;:'` → vararg `trim('.', ',', '!', '?', ';', ':')` (4 sites).
2. *Placement test starved to ~4 questions*: the filter required `newItem != null`, which is
   almost always the headword line; now any well-sized non-headword sentence of the milestone
   lesson qualifies (newItem-bearing sentences preferred) → a full 14-probe test.
3. *Word-bank tile reflow*: the ghost placeholder rendered `···` (narrower than the word), so
   tiles shifted under the learner's finger and wrong tiles got tapped; ghosts now render the
   real token in transparent ink — bounds are pixel-stable.
4. *Ambiguous match-pairs*: dedupe now keys on the gloss **head** (text before `/` or `(`), so
   `patut`/`aa`/`saja` ("correct/yes", "yes", "yes (true)") no longer share a board — ambiguous
   sets fall through to flashcards instead.

### v4 additions (this revision)

**Balanced exercise mixes.** Every review-style session (U2 reinforce, U3 cumulative, U4
lesson review, U5 mastery, practice hub, mistake drill) now plays a rotating 12-slot
engine plan — choice·2, word-bank·3, fill-blank·2, free-typing·2, dictation·1,
listen·1, speak·1 — instead of the old maturity ladder that over-produced fill-blank in
U2 and free-typing in U3+. Each slot degrades gracefully when a sentence or device
capability can't render it (typing/dictation need ≥3 successes; fill needs U2+; longer
sentences fall back to choice), so no format dominates. Verified per unit:
U2 12/12, U3 12/12, U4 12/12, U5 14/14 slots with typing ≤3 and fill ≤3.

**Vocabulary drills restored in U1+.** Match-pairs boards are rebuilt to survive
ambiguous gloss clusters (colliding labels are dropped, previously taught words top the
board up to 5 pairs) and a new vocabulary multiple-choice exercise (word→meaning and
meaning→word) is woven into U1 chunk sessions (every 3rd entry) and into U2 warm-ups.

**Register-tolerant free typing.** Typing exercises accept same-gloss register siblings
(`dini↔driki`, `ditu↔drika`, `nanging↔sakewanten/tapi`, `tiang↔titiang/icang`, …) in any
alus/andap/mixed combination — up to 128 generated variants, e.g. both
*"Tiang driki sakewanten ragane drika"* and *"Tiang driki nanging ragane drika"* pass for
*"Tiang dini nanging ragane ditu"*, with a "another register fits this sentence too" note.

**Speaking skip.** The read-aloud exercise shows an always-visible **Skip speaking →**
button; tapping it disables the remaining speaking steps of that subunit (TTS is stopped
and stale ASR results are ignored before the mic can pick up the model audio).

**TTS everywhere.** Balinese text announces itself when it appears: word cards,
Balinese multiple-choice prompts (sentence and vocabulary), fill-blank sentences (with a
gap at the blank), speak targets, pronunciation models in placement, story lines and
story-question lines, and Balinese answers in feedback. Balinese answer choices are
spoken when tapped in every multiple-choice/fill-blank exercise.

**Review fixes.** The mistake log now persists immediately, survives mid-session exits,
and is only cleared by a correct answer in a *later* session — the same-session requeue
no longer erases a mistake before the learner can revisit it. "Review encountered words"
always builds a session (due SRS first, then a weakest-word rehearsal when nothing is
formally due), and the Words/Sentences-encountered lists are collapsible.

**Live league rivals.** Rivals gain XP in stochastic study bursts through the week *and*
proportionally to the learner's weekly XP (per-rival 0.45×–1.49× factors), and the board
refreshes while it is open — scores move as you play instead of staying static.

### v5 additions (this revision)

**Bigger subunits (~18 exercises).** Every review-style subunit now targets 18
exercises: the U1 closing review, U2 reinforce and closing review, U3 cumulative
(×3), U4 lesson reviews and U5 mastery all build 18 slots, and the balanced plan
grew with them (2 recognition-choice, 1 Balinese-choice, 4 word-bank, 3 fill-blank,
3 free-typing, 2 listen, 1 dictation, 2 speaking). When every focus word is already
covered the builder keeps rotating fresh sentences over the same scope, so a
subunit is never cut short by coverage. Capability-locked slots fall back with a
rotation (fill → bank → choice) instead of all collapsing into word-bank.

**Skipping speaking keeps the practice.** Tapping **Skip speaking** no longer drops
the speaking steps: the current and every later speaking exercise of the subunit are
swapped for equivalent non-speaking exercises on the same sentence (word-bank,
fill-blank or recognition, rotating), so the subunit keeps its full length and the
sentence still gets practised. Stale ASR results are ignored and TTS is stopped
before the mic opens.

**Sentence punctuation.** The content pipeline was stripping the authored
punctuation from every displayed Balinese sentence; `build_content.py` now keeps it
("Nggih, niki patut.", "Aa, nyen adan caine?", "Matur suksma, dumogi rahayu.").
All 8,581 sentences and the story lines were regenerated: matching tokens stay
punctuation-free, only the display form changed. Sentence registers are now
deterministic too (majority of the sentence's entries, ties resolve Alus > Andap >
Mider), fixing 724 sentences that had drifted.

**Same-meaning tiles never share a word bank.** The extra drag-and-drop tile is now
a distinct-meaning word: `cai`/`nyai`, `sing`/`tusing`, `ene`/`niki`, `ento`/`nika`
and every other same-gloss sibling of a word already in the sentence is excluded
(verified: all 6,562 bank-eligible sentences still have a valid distractor).

**Live, archetype-based league.** Rivals are independent simulated learners — a
3 h/day grinder, a 15-minutes-a-day casual, a weekend learner, a streak keeper —
each studying in a few sessions per day at deterministic times between 06:00 and
21:50. XP lands in bursts as the phone clock passes each session, scores no longer
track the learner's XP, and ranks move in both directions (verified live: a rival
went 175 → 217 XP as the clock advanced; the learner sat at rank 10 with 0 XP and
rank 1 with 600 XP). Each rival row shows its study profile and the board animates
reorderings.

**Picture sharing.** Sentences and progress now share as proper formatted images
(1080×1350 PNG cards with branding, the register chip, the mascot and — for
progress — the streak/XP/words/steps blocks), handed to the system share sheet via
a FileProvider. Share buttons live on every sentence in the Review tab, in the
exercise feedback, and in Settings → Share progress.

**Ayu, the mascot.** One Balinese character, drawn with primitives (cream kebaya,
gold sash, frangipani behind the ear — the flower of everyday offerings), with
breathing, blinking and mood changes. She appears in every exercise with a speech
bubble (attested course lines like "Ngiring!", "Patut!" reactions on correct
answers, "Sampunang sebet!" on misses), on the completion screen, and inside the
share cards.

**Motion throughout.** Feedback banners slide in, options colour-animate on
selection, the progress bar glides, the current path node pulses, the streak flame
breathes, quest bars animate, low hearts pulse, the completion screen counts XP up
with the mascot bouncing, and frangipani petals drift slowly behind the main shell.

## Architecture

```
app/src/main/
├── assets/content/            # words / patterns / sentences / curriculum / stories JSON
└── java/com/basabali/app/
    ├── BasaBaliApp.kt           # AppGraph: content, store, builder, TTS, ASR
    ├── MainActivity.kt
    ├── audio/
    │   ├── TtsManager.kt        # id-ID TTS proxy, Compose-observable availability
    │   └── AsrManager.kt        # id-ID SpeechRecognizer, 5 hypotheses
    ├── data/
    │   ├── Models.kt            # Sentence, ChunkMeta, Story, LeagueData, …
    │   ├── ContentRepository.kt # asset loading, indices, distractors, stories
    │   └── UserStore.kt         # SRS stores, progress, quests, league, settings
    ├── engine/
    │   ├── Srs.kt               # scheduler facade (Leitner ⇄ FSRS, hot-swappable)
    │   ├── Fsrs.kt              # FSRS-4.5 math (default weights)
    │   ├── TextMatch.kt         # normalization, Levenshtein, similarity thresholds
    │   ├── Exercise.kt          # 9 sealed exercise specs with tags/focusTags
    │   ├── SessionBuilder.kt    # chunk/review/placement session assembly + ErrorAttribution
    │   ├── Quests.kt            # deterministic daily quests
    │   └── League.kt            # weekly offline league engine
    └── ui/
        ├── AppRoot.kt           # Path · Placement · Session · Story · League · Complete
        ├── PathScreen.kt        # path + quests + league + stories + settings + share
        ├── PlacementScreen.kt   · StoryScreen.kt · LeagueScreen.kt · CompleteScreen.kt
        ├── lesson/LessonScreen.kt    # host: progress, hearts, check/feedback flow
        ├── lesson/SessionRunner.kt   # grading, requeue, XP/streak/quest/league updates
        ├── lesson/ExerciseViews.kt   # all 9 exercise composables incl. audio & typing
        ├── path/… · theme/Theme.kt
```

## Regenerating content

```bash
python3 tools/build_content.py     # + tools/stories_src.py
```
Reads the four curriculum files one directory up, re-verifies the ≤1-new-entry guarantee
while tagging, **aborts if any story line uses vocabulary beyond its gate lesson**, and
writes the five JSON assets.

## Known limitations / next steps

- **Audio fidelity:** the id-ID TTS pronounces core Austronesian vocabulary well but will
  mispronounce Balinese-specific phonology/lexicon; the permanent fix is recording the
  8,600-sentence bank with native speakers (the format ladder already treats audio as
  swappable — no engine changes needed).
- **ASR scoring** is similarity-based; a Balinese acoustic model would raise the bar.
- Real social features (friends, shared streaks) need a backend; leagues are single-device
  simulations by design.
- HLR: once you have telemetry, train θ on (successes, lapses, lag) — the store schema and
  the `Srs` facade are the designated swap-in point.

## Content provenance

All sentences were authored under machine-validated constraints (`validate*.py`): ≥3 example
sentences per vocabulary entry per lesson, and **at most one new entry per sentence** with
strictly cumulative progression — which is exactly what makes the `words`/`newItem` tags
reliable for SRS scheduling, distractor generation and story gating.
