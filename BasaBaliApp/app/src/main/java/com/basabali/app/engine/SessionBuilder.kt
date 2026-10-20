package com.basabali.app.engine

import com.basabali.app.data.ChunkMeta
import com.basabali.app.data.ContentRepository
import com.basabali.app.data.Sentence
import com.basabali.app.data.UserStore
import kotlin.math.min
import kotlin.random.Random

/**
 * Device/progress capabilities consulted when choosing exercise formats.
 * [unit] is the unit of the node being played (review hubs pass the highest
 * unit the learner has reached). Format unlocks:
 *   U1  listening + speaking (ear & mouth from the very first lesson)
 *   U2  fill-in-the-blank + register choice
 *   U3  dictation + free typing
 */
data class Caps(
    val tts: Boolean,             // Indonesian-proxy TTS ready
    val asr: Boolean,             // speech recognition available
    val unit: Int                 // 1..5
) {
    fun listeningUnlocked(): Boolean = tts
    fun fillUnlocked(): Boolean = unit >= 2
    fun typingUnlocked(): Boolean = unit >= 3
    fun dictationUnlocked(): Boolean = tts && unit >= 3
    fun speakUnlocked(): Boolean = asr
}

/** One placement-test question with the milestone lesson it probes. */
data class PlacementQ(val lesson: Int, val ex: MultipleChoiceEx)

/**
 * Builds exercise sessions, implementing app_design_learning_system.md:
 *
 *  - learn sessions (Units 1–2 of a lesson): ≤5 new items per subunit,
 *    card → vocab/sentence recognition → production (+listening once unlocked),
 *    review interleaved
 *  - review sessions (practice hub, Unit 3 cumulative, Units 4–5 lesson review):
 *    candidates = sentences whose words are ALL taught; due-based scoring for the
 *    hub, coverage-based scoring for forced lesson/cumulative reviews
 *  - per-session balance: review sessions play a rotating [Fmt] plan so no single
 *    exercise type dominates (fill-blank is capped in U2, free typing in U3+)
 *  - error attribution (ErrorAttribution): content tile → word blame;
 *    order/function-word → pattern blame
 *  - new-item highlighting: sentences carry `highlight` while their newItem is
 *    still untaught, so first encounters render the word in purple
 */
class SessionBuilder(
    private val content: ContentRepository,
    private val store: UserStore,
    private val random: Random = Random(System.currentTimeMillis())
) {
    companion object {
        const val MAX_SESSION = 18
        const val REVIEW_SLOTS_NEW = 3
        const val REVIEW_SLOTS_PURE = 10
        const val MIN_TOKENS = 2
        const val MAX_TOKENS = 12
        const val MAX_BANK_TOKENS = 10

        val FUNCTION_TOKENS = setOf(
            "di", "ring", "ka", "ke", "lan", "miwah", "tur", "muah",
            "ne", "ba", "ajak", "sareng", "teken", "uli", "saking", "baang"
        )

        val PLACEMENT_MILESTONES = listOf(2, 5, 8, 12, 16, 21, 26, 31, 36, 42, 47, 52, 58, 64)
    }

    /** One engine slot of the per-session balance plan. */
    enum class Fmt { CHOICE_EN, CHOICE_BAL, LISTEN, BANK, FILL, DICT, TYPE, SPEAK }

    /**
     * Review mix, 18 slots (~18 exercises per subunit): 2 recognition-choice,
     * 1 Balinese-choice, 2 listen, 4 word-bank, 3 fill-blank, 3 free-typing,
     * 1 dictation, 2 speaking — no format dominates, and each slot degrades
     * gracefully when the sentence or device capabilities can't render it
     * (see [makeForSlot]).
     */
    private val REVIEW_PLAN = listOf(
        Fmt.CHOICE_EN, Fmt.BANK, Fmt.FILL, Fmt.LISTEN, Fmt.TYPE, Fmt.BANK,
        Fmt.CHOICE_BAL, Fmt.FILL, Fmt.DICT, Fmt.SPEAK, Fmt.BANK, Fmt.TYPE,
        Fmt.CHOICE_EN, Fmt.FILL, Fmt.LISTEN, Fmt.BANK, Fmt.TYPE, Fmt.SPEAK
    )

    /** Mistake re-drill stays gentle: recognition / bank / listen / fill only. */
    private val MISTAKE_PLAN = listOf(
        Fmt.CHOICE_EN, Fmt.BANK, Fmt.LISTEN, Fmt.BANK, Fmt.FILL,
        Fmt.CHOICE_BAL, Fmt.BANK, Fmt.LISTEN, Fmt.FILL, Fmt.BANK
    )

    fun now(): Long = System.currentTimeMillis()

    fun caps(tts: Boolean, asr: Boolean, unit: Int): Caps = Caps(tts, asr, unit)

    // ------------------------------------------------------------------ due queries

    fun dueWords(now: Long = now()): Map<String, Double> {
        val out = HashMap<String, Double>()
        for ((id, st) in store.words) {
            if (Srs.isDue(st, now, Srs.TAU_WORD)) out[id] = 1.0 - Srs.pRecall(st, now)
        }
        return out
    }

    fun duePatterns(now: Long = now()): Map<String, Double> {
        val out = HashMap<String, Double>()
        for ((id, st) in store.patterns) {
            if (Srs.isDue(st, now, Srs.TAU_PATTERN)) out[id] = 1.0 - Srs.pRecall(st, now)
        }
        return out
    }

    fun dueCount(): Int {
        val n = now()
        return dueWords(n).size + duePatterns(n).size
    }

    // ------------------------------------------------------------------ learn session (Units 1 & 2)

    fun newChunkSession(chunk: ChunkMeta, caps: Caps): List<Exercise> {
        val out = ArrayList<Exercise>()
        val items = chunk.items
        val lesson = chunk.lessonId

        // E2 warm-up: match pairs over the chunk's words (older taught words top
        // the board up when glosses collide); skipped when an unambiguous board
        // of ≥3 pairs can't be built.
        buildMatchPairs(items, lesson)?.let { out += it }

        // Per item: card → recognition (vocab MC for every third entry, else
        // sentence choice/listen) → production (E4 word bank) → ear/mouth pass
        // (E5 listening or E9 speaking — speaking is read-aloud, not recall).
        for ((i, item) in items.withIndex()) {
            val usages = content.usageSentences(item, lesson, 4)
            val example = usages.firstOrNull() ?: content.anyUsageSentence(item, lesson)
            out += WordCardEx(
                wordId = item,
                gloss = content.gloss(item),
                register = content.registerOf(item),
                exampleBal = example?.bal ?: content.display(item),
                exampleEn = example?.en ?: content.gloss(item)
            )
            val s1 = usages.getOrNull(0)
            if (s1 != null) {
                out += if (i % 3 == 0) {
                    makeVocabChoice(item, toEnglish = (i / 3) % 2 == 0, lesson)
                } else if (caps.listeningUnlocked() && random.nextInt(100) < 35) {
                    makeListenTap(s1, toEnglish = true, focus = listOf("w:$item"))
                } else {
                    makeChoice(s1, toEnglish = true, focus = listOf("w:$item"))
                }
            }

            val s2 = usages.getOrNull(1) ?: usages.getOrNull(0)
            if (s2 != null) {
                if (s2.tokens.size in MIN_TOKENS..MAX_BANK_TOKENS) out += makeBank(s2, listOf("w:$item"))
                else out += makeChoice(s2, toEnglish = false, focus = listOf("w:$item"))
            }
            val s3 = usages.getOrNull(2)
            if (s3 != null && random.nextInt(100) < 60) {
                val speakOk = caps.speakUnlocked() && s3.tokens.size in MIN_TOKENS..8
                out += when {
                    speakOk && random.nextInt(100) < 50 -> makeSpeak(s3, listOf("w:$item"))
                    caps.listeningUnlocked() -> makeListenTap(s3, toEnglish = true, focus = listOf("w:$item"))
                    else -> makeChoice(s3, toEnglish = true, focus = listOf("w:$item"))
                }
            }
        }

        val exclude = out.mapNotNull { it.sentenceId }.toSet()
        val review = buildReview(REVIEW_SLOTS_NEW, exclude, caps)
        val merged = interleave(out, review)
        return if (merged.size > MAX_SESSION + 2) merged.take(MAX_SESSION + 2) else merged
    }

    /**
     * Word↔gloss pairs for the E2 board, preference order: current chunk items,
     * then already-taught entries of the same lesson, then other taught entries.
     * A displayed gloss that appears twice is dropped from the board (it would
     * be indistinguishable for the learner); null when fewer than 3 survive.
     */
    private fun buildMatchPairs(items: List<String>, lessonId: Int): MatchPairsEx? {
        val ordered = ArrayList<String>()
        ordered += items.shuffled(random)
        val lessonItems = (content.lessonsById[lessonId]?.items ?: emptyList())
            .filter { it !in items && store.isTaught(it) }
        ordered += lessonItems.shuffled(random)
        if (ordered.size < 8) {
            ordered += store.words.keys
                .filter { it !in ordered && store.isTaught(it) }
                .shuffled(random)
        }
        val kept = LinkedHashMap<String, Pair<String, String>>()   // label -> (id, label)
        val ambiguous = HashSet<String>()
        for (id in ordered) {
            if (kept.size >= 5) break
            val label = content.shortGloss(content.gloss(id), 26)
            if (label in ambiguous) continue
            val prev = kept[label]
            if (prev != null) {
                kept.remove(label)
                ambiguous += label
            } else {
                kept[label] = id to label
            }
        }
        if (kept.size < 3) return null
        return MatchPairsEx(kept.values.toList())
    }

    /** E3-style vocabulary choice: word → meaning, or meaning → word. */
    fun makeVocabChoice(item: String, toEnglish: Boolean, lesson: Int): MultipleChoiceEx {
        val gloss = content.shortGloss(content.gloss(item), 30)
        return if (toEnglish) {
            val distr = content.vocabGlossDistractors(item, lesson, 3)
            val options = (listOf(gloss) + distr).distinct().shuffled(random)
            MultipleChoiceEx(
                prompt = content.display(item),
                correct = gloss,
                options = options,
                toEnglish = true,
                register = content.registerOf(item),
                sentenceId = null,
                tags = listOf("w:$item"),
                focusTags = listOf("w:$item")
            )
        } else {
            val distr = content.vocabWordDistractors(item, lesson, 3)
            val correct = content.display(item)
            val options = (listOf(correct) + distr).distinct().shuffled(random)
            MultipleChoiceEx(
                prompt = gloss,
                correct = correct,
                options = options,
                toEnglish = false,
                register = content.registerOf(item),
                sentenceId = null,
                tags = listOf("w:$item"),
                focusTags = listOf("w:$item")
            )
        }
    }

    // ------------------------------------------------- reinforce session (Unit 2)

    /**
     * U2 subunit: the chunk's words re-encountered a second time — a match-pairs
     * warm-up and two vocabulary choice checks over the chunk, then balanced
     * sentence exercises (coverage-driven like the forced reviews, but scoped
     * to the chunk). Topped up with due-based review so the session is never short.
     */
    fun reinforceChunkSession(chunk: ChunkMeta, caps: Caps): List<Exercise> {
        val chunkItems = chunk.items.toSet()
        val remaining = chunkItems.toMutableSet()
        val out = ArrayList<Exercise>()
        val used = HashSet<String>()
        val now = now()

        buildMatchPairs(chunk.items, chunk.lessonId)?.let { out += it }
        val order = chunk.items.shuffled(random)
        if (order.isNotEmpty()) out += makeVocabChoice(order[0], toEnglish = true, chunk.lessonId)
        if (order.size > 1) out += makeVocabChoice(order[1], toEnglish = false, chunk.lessonId)

        var slotIdx = random.nextInt(REVIEW_PLAN.size)
        var guard = 0
        while (out.size < 18 && remaining.isNotEmpty() && guard < 90) {
            guard++
            val scored = ArrayList<Triple<Sentence, Double, List<String>>>(32)
            for (s in content.sentences) {
                if (s.headword || s.id in used || s.lesson > chunk.lessonId) continue
                if (s.tokens.size !in MIN_TOKENS..MAX_TOKENS || s.words.isEmpty()) continue
                var taughtAll = true
                for (w in s.words) {
                    if (!store.isTaught(w)) { taughtAll = false; break }
                }
                if (!taughtAll) continue
                val covered = s.words.filter { it in remaining }
                if (covered.isEmpty()) continue
                var score = covered.size * 2.0
                for (w in covered) score += 1.0 / (1.0 + tagMaturity("w:$w"))
                if (s.lesson == chunk.lessonId) score += 0.5
                val last = store.seenSentences[s.id] ?: 0L
                score += min(0.25, (now - last).toDouble() / (10.0 * Srs.DAY_MS))
                score -= 0.02 * s.tokens.size
                scored += Triple(s, score, covered.map { "w:$it" })
            }
            if (scored.isEmpty()) break
            scored.sortByDescending { it.second }
            val pick = scored[random.nextInt(min(3, scored.size))]
            val s = pick.first
            used += s.id
            remaining.removeAll(pick.third.map { it.substring(2) }.toSet())
            val ex = makeForSlot(s, pick.third, caps, REVIEW_PLAN[slotIdx % REVIEW_PLAN.size]) ?: continue
            slotIdx++
            out += ex
        }
        // Second pass: the chunk words were just drilled in U1 so they are rarely
        // SRS-due — keep rotating fresh sentences over the same words (freshest
        // first) so U2 stays a full sentence-practice session.
        var guard2 = 0
        while (out.size < 18 && guard2 < 60) {
            guard2++
            val scored = ArrayList<Triple<Sentence, Double, List<String>>>(32)
            for (s in content.sentences) {
                if (s.headword || s.id in used || s.lesson > chunk.lessonId) continue
                if (s.tokens.size !in MIN_TOKENS..MAX_TOKENS || s.words.isEmpty()) continue
                var taughtAll = true
                for (w in s.words) {
                    if (!store.isTaught(w)) { taughtAll = false; break }
                }
                if (!taughtAll) continue
                val covered = s.words.filter { it in chunkItems }
                if (covered.isEmpty()) continue
                var score = covered.size.toDouble()
                if (s.lesson == chunk.lessonId) score += 0.3
                val last = store.seenSentences[s.id] ?: 0L
                score += min(1.0, (now - last).toDouble() / (10.0 * Srs.DAY_MS))
                score -= 0.02 * s.tokens.size
                scored += Triple(s, score, covered.map { "w:$it" })
            }
            if (scored.isEmpty()) break
            scored.sortByDescending { it.second }
            val pick = scored[random.nextInt(min(3, scored.size))]
            used += pick.first.id
            val ex = makeForSlot(pick.first, pick.third, caps, REVIEW_PLAN[slotIdx % REVIEW_PLAN.size]) ?: continue
            slotIdx++
            out += ex
        }
        if (out.size < 18) out += buildReview(18 - out.size, used, caps)
        return out
    }

    // ------------------------------------------------- mistake review (Review tab)

    /**
     * Re-drills sentences the learner got wrong and has not answered correctly
     * since (store.mistakes), most-missed first, on the gentle [MISTAKE_PLAN].
     * The taught filter is intentionally absent: a sentence can only be in the
     * mistake log because the app already showed it as an exercise.
     */
    fun mistakeSession(caps: Caps, maxSlots: Int = 10): List<Exercise> {
        if (store.mistakes.isEmpty()) return emptyList()
        val ranked = store.mistakes.keys
            .mapNotNull { content.sentencesById[it] }
            .filter { !it.headword && it.tokens.size in MIN_TOKENS..MAX_TOKENS }
            .sortedByDescending { store.mistakes[it.id] ?: 0 }
        val out = ArrayList<Exercise>()
        var slotIdx = 0
        for (s in ranked) {
            if (out.size >= maxSlots) break
            val focus = s.words.map { "w:$it" }
            val ex = makeForSlot(s, focus, caps, MISTAKE_PLAN[slotIdx % MISTAKE_PLAN.size]) ?: continue
            slotIdx++
            out += ex
        }
        return out
    }

    // ------------------------------------------------------------------ due-based review (practice hub)

    /**
     * Practice-hub session: due-based SRS picks first; when nothing is formally
     * due, the session still builds by rehearsing the weakest taught words, so
     * "Review" is never a dead end.
     */
    fun reviewSession(caps: Caps, maxSlots: Int = REVIEW_SLOTS_PURE): List<Exercise> {
        val out = buildReview(maxSlots, emptySet(), caps).toMutableList()
        if (out.size >= maxSlots) return out
        val used = out.mapNotNull { it.sentenceId }.toSet()
        val weakest = store.words.entries
            .filter { it.value.lastReview > 0L }
            .sortedWith(compareBy({ it.value.box }, { it.value.lastReview }))
            .take(40)
            .map { it.key }
            .toSet()
        if (weakest.isNotEmpty()) {
            out += forcedReview({ true }, weakest, maxSlots - out.size, caps, used)
        }
        return out
    }

    private fun buildReview(maxSlots: Int, exclude: Set<String>, caps: Caps): List<Exercise> {
        val now = now()
        val dw = dueWords(now).toMutableMap()
        val dp = duePatterns(now).toMutableMap()
        val out = ArrayList<Exercise>()
        val used = exclude.toMutableSet()
        var slotIdx = random.nextInt(REVIEW_PLAN.size)
        var guard = 0

        while (out.size < maxSlots && (dw.isNotEmpty() || dp.isNotEmpty()) && guard < 80) {
            guard++
            val scored = ArrayList<Triple<Sentence, Double, List<String>>>(24)
            for (s in content.sentences) {
                if (s.headword) continue
                if (s.tokens.size !in MIN_TOKENS..MAX_TOKENS) continue
                if (s.id in used) continue
                if (s.words.isEmpty()) continue
                var taughtAll = true
                for (w in s.words) {
                    if (!store.isTaught(w)) { taughtAll = false; break }
                }
                if (!taughtAll) continue
                var score = 0.0
                val focus = ArrayList<String>(4)
                for (w in s.words) {
                    val d = dw[w]
                    if (d != null) { score += d; focus += "w:$w" }
                }
                for (p in s.patterns) {
                    val d = dp[p]
                    if (d != null) { score += 1.5 * d; focus += "p:$p" }
                }
                if (focus.isEmpty()) continue
                val last = store.seenSentences[s.id] ?: 0L
                score += min(0.3, (now - last).toDouble() / (10.0 * Srs.DAY_MS))
                score -= 0.02 * s.tokens.size
                scored += Triple(s, score, focus)
            }
            if (scored.isEmpty()) break
            scored.sortByDescending { it.second }
            val pick = scored[random.nextInt(min(3, scored.size))]
            val s = pick.first
            val focus = pick.third
            used += s.id
            val ex = makeForSlot(s, focus, caps, REVIEW_PLAN[slotIdx % REVIEW_PLAN.size]) ?: continue
            slotIdx++
            out += ex
            for (t in focus) {
                if (t.startsWith("w:")) dw.remove(t.substring(2)) else dp.remove(t.substring(2))
            }
        }
        return out
    }

    // ------------------------------------------------------------------ forced reviews (Units 3–5)

    /** Unit 3 — cumulative review of everything taught from Lesson 1 up to [upToLesson]. */
    fun cumulativeReviewSession(upToLesson: Int, maxSlots: Int, caps: Caps): List<Exercise> {
        val focus = store.words.keys
            .filter { (content.words[it]?.firstLesson ?: Int.MAX_VALUE) <= upToLesson }
            .toSet()
        return forcedReview({ l -> l <= upToLesson }, focus, maxSlots, caps)
    }

    /** Units 4–5 — review covering every entry of one lesson. */
    fun lessonReviewSession(lessonId: Int, maxSlots: Int, caps: Caps): List<Exercise> {
        val focus = (content.lessonsById[lessonId]?.items ?: emptyList())
            .filter { store.isTaught(it) }
            .toSet()
        return forcedReview({ l -> l == lessonId }, focus, maxSlots, caps)
    }

    /**
     * Coverage-driven review: repeatedly pick the sentence covering the most
     * not-yet-covered focus words (weakest first), rendered on the balanced
     * [REVIEW_PLAN], then top up with due-based review so the session is never short.
     */
    private fun forcedReview(
        inScope: (Int) -> Boolean,
        focusWords: Set<String>,
        maxSlots: Int,
        caps: Caps,
        exclude: Set<String> = emptySet()
    ): List<Exercise> {
        val now = now()
        val remaining = focusWords.toMutableSet()
        val out = ArrayList<Exercise>()
        val used = exclude.toMutableSet()
        var slotIdx = random.nextInt(REVIEW_PLAN.size)
        var guard = 0

        while (out.size < maxSlots && remaining.isNotEmpty() && guard < 150) {
            guard++
            val scored = ArrayList<Triple<Sentence, Double, List<String>>>(32)
            for (s in content.sentences) {
                if (s.headword || !inScope(s.lesson) || s.id in used) continue
                if (s.tokens.size !in MIN_TOKENS..MAX_TOKENS || s.words.isEmpty()) continue
                var taughtAll = true
                for (w in s.words) {
                    if (!store.isTaught(w)) { taughtAll = false; break }
                }
                if (!taughtAll) continue
                val covered = s.words.filter { it in remaining }
                if (covered.isEmpty()) continue
                var score = covered.size * 2.0
                for (w in covered) score += 1.0 / (1.0 + tagMaturity("w:$w"))
                val last = store.seenSentences[s.id] ?: 0L
                score += min(0.3, (now - last).toDouble() / (10.0 * Srs.DAY_MS))
                score -= 0.02 * s.tokens.size
                scored += Triple(s, score, covered.map { "w:$it" })
            }
            if (scored.isEmpty()) break
            scored.sortByDescending { it.second }
            val pick = scored[random.nextInt(min(3, scored.size))]
            val s = pick.first
            used += s.id
            val coveredIds = pick.third.map { it.substring(2) }
            remaining.removeAll(coveredIds.toSet())
            val ex = makeForSlot(s, pick.third, caps, REVIEW_PLAN[slotIdx % REVIEW_PLAN.size]) ?: continue
            slotIdx++
            out += ex
        }
        // Top-up to the full subunit length: once every focus word is covered, keep
        // rotating fresh sentences over the same scope (freshly taught words are
        // rarely SRS-due, so the due-based fallback alone would leave the session short).
        var fillGuard = 0
        while (out.size < maxSlots && fillGuard < 150) {
            fillGuard++
            val scored = ArrayList<Triple<Sentence, Double, List<String>>>(32)
            for (s in content.sentences) {
                if (s.headword || !inScope(s.lesson) || s.id in used) continue
                if (s.tokens.size !in MIN_TOKENS..MAX_TOKENS || s.words.isEmpty()) continue
                var taughtAll = true
                for (w in s.words) {
                    if (!store.isTaught(w)) { taughtAll = false; break }
                }
                if (!taughtAll) continue
                var score = s.words.count { it in focusWords }.toDouble()
                val last = store.seenSentences[s.id] ?: 0L
                score += min(1.0, (now - last).toDouble() / (10.0 * Srs.DAY_MS))
                score -= 0.02 * s.tokens.size
                scored += Triple(s, score, s.words.map { "w:$it" })
            }
            if (scored.isEmpty()) break
            scored.sortByDescending { it.second }
            val pick = scored[random.nextInt(min(3, scored.size))]
            used += pick.first.id
            val ex = makeForSlot(pick.first, pick.third, caps, REVIEW_PLAN[slotIdx % REVIEW_PLAN.size]) ?: continue
            slotIdx++
            out += ex
        }
        if (out.size < maxSlots) {
            out += buildReview(maxSlots - out.size, used, caps)
        }
        return out
    }

    // ------------------------------------------------------------------ format ladder & factories

    /** Rotates the fallback format for capability-locked slots. */
    private var fallbackSerial = 0

    /**
     * Renders [slot]; when the sentence or device can't support that format the
     * slot degrades to the nearest lighter format instead of silently skewing
     * the session towards whichever type happens to be feasible.
     */
    private fun makeForSlot(s: Sentence, focus: List<String>, caps: Caps, slot: Fmt): Exercise? {
        val mat = minMaturity(focus)
        val short = s.tokens.size in MIN_TOKENS..8
        val bankable = s.tokens.size in MIN_TOKENS..MAX_BANK_TOKENS
        val direct = when (slot) {
            Fmt.CHOICE_EN -> makeChoice(s, toEnglish = true, focus)
            Fmt.CHOICE_BAL -> makeChoice(s, toEnglish = false, focus)
            Fmt.LISTEN -> if (caps.listeningUnlocked()) makeListenTap(s, toEnglish = true, focus) else null
            Fmt.BANK -> if (bankable) makeBank(s, focus) else null
            Fmt.FILL -> if (caps.fillUnlocked()) makeFillBlank(s, focus) else null
            Fmt.DICT -> if (caps.dictationUnlocked() && short && mat >= 3) makeDictation(s, focus) else null
            Fmt.TYPE -> if (caps.typingUnlocked() && bankable && mat >= 3) makeType(s, focus) else null
            Fmt.SPEAK -> if (caps.speakUnlocked() && short && mat >= 1) makeSpeak(s, focus) else null
        }
        if (direct != null) return direct
        // lock-aware fallbacks: unlocked formats fall to their nearest sibling, and
        // locked-typing slots rotate between grammar/recognition so no one format
        // absorbs every fallback (U2 used to end up all word-bank / all fill-blank)
        return when (slot) {
            Fmt.CHOICE_EN, Fmt.LISTEN, Fmt.SPEAK -> makeChoice(s, toEnglish = true, focus)
            Fmt.CHOICE_BAL, Fmt.BANK, Fmt.FILL ->
                if (bankable) makeBank(s, focus) else makeChoice(s, toEnglish = false, focus)
            Fmt.TYPE -> {
                if (!caps.typingUnlocked() && caps.fillUnlocked()) {
                    when (fallbackSerial++ % 3) {
                        0 -> makeFillBlank(s, focus) ?: makeBank(s, focus)
                        1 -> if (bankable) makeBank(s, focus) else makeChoice(s, toEnglish = false, focus)
                        else -> makeChoice(s, toEnglish = true, focus)
                    }
                } else if (bankable) makeBank(s, focus)
                else makeChoice(s, toEnglish = false, focus)
            }
            Fmt.DICT ->
                if (caps.listeningUnlocked()) makeListenTap(s, toEnglish = true, focus)
                else makeChoice(s, toEnglish = true, focus)
        }
    }

    private fun minMaturity(focus: List<String>): Int {
        var mat = Int.MAX_VALUE
        for (t in focus) {
            val m = tagMaturity(t)
            if (m < mat) mat = m
        }
        return if (mat == Int.MAX_VALUE) 0 else mat
    }

    fun tagMaturity(tag: String): Int {
        val st = if (tag.startsWith("w:")) store.words[tag.substring(2)]
                 else store.patterns[tag.substring(2)]
        return st?.successes ?: 0
    }

    private fun allTags(s: Sentence): List<String> =
        s.words.map { "w:$it" } + s.patterns.map { "p:$it" }

    /** Purple-highlight the sentence's new entry only while it is still untaught. */
    private fun highlightFor(s: Sentence): String? =
        s.newItem?.takeIf { !store.isTaught(it) }

    fun makeChoice(s: Sentence, toEnglish: Boolean, focus: List<String>): Exercise {
        return if (toEnglish) {
            val distr = content.distractorEnglish(s, 2)
            val options = (listOf(s.en) + distr).distinct().shuffled(random)
            MultipleChoiceEx(s.bal, s.en, options, true, s.register, s.id, allTags(s), focus, highlightFor(s))
        } else {
            val distr = content.distractorBalinese(s, 2)
            val options = (listOf(s.bal) + distr).distinct().shuffled(random)
            MultipleChoiceEx(s.en, s.bal, options, false, s.register, s.id, allTags(s), focus, null)
        }
    }

    fun makeBank(s: Sentence, focus: List<String>): Exercise {
        // Tiles never carry punctuation; the authored sentence is kept separately
        // for the feedback card (issue: "[driki,]" → "[driki],").
        val answer = bankTokens(s.bal)
        val answerLower = answer.map { it.lowercase() }
        val bank = answer.toMutableList()
        val altGroups = ArrayList<List<String>>(answer.size)
        val added = HashSet<String>()
        var variantCount = 0
        for ((i, tok) in answer.withIndex()) {
            val group = ArrayList<String>(2)
            group += answerLower[i]
            if (variantCount < 6) {
                val alternative = content.registerAlternatives(tok, s.lesson)
                    .firstOrNull {
                        it.lowercase() !in answerLower && it.lowercase() !in added
                    }
                if (alternative != null) {
                    group += alternative.lowercase()
                    added += alternative.lowercase()
                    bank += content.display(alternative)
                    variantCount++
                }
            }
            altGroups += group
        }
        content.distractorToken(s)?.let { bank += it }
        bank.shuffle(random)
        return WordBankEx(
            s.en,
            answerLower,
            bank,
            s.register,
            s.id,
            allTags(s),
            focus,
            highlightFor(s),
            answerDisplay = s.bal,
            altGroups = altGroups
        )
    }

    /** Surface tokens for the word bank: punctuation stripped, word kept intact. */
    private fun bankTokens(bal: String): List<String> =
        bal.split(" ")
            .map { it.trim { c -> !c.isLetterOrDigit() && c != '\'' && c != '-' } }
            .filter { it.isNotEmpty() }

    fun makeFillBlank(s: Sentence, focus: List<String>): Exercise? {
        val focusWords = focus.filter { it.startsWith("w:") }.map { it.substring(2) }
            .filter { !it.contains(' ') }
        val target = focusWords.firstOrNull { containsToken(s, it) }
            ?: s.words.firstOrNull { !it.contains(' ') && containsToken(s, it) }
            ?: return null
        val toks = s.bal.split(" ")
        val idx = toks.indexOfFirst { it.lowercase().trim('.', ',', '!', '?', ';', ':') == target }
        if (idx < 0) return null
        val before = toks.subList(0, idx).joinToString(" ")
        val after = toks.subList(idx + 1, toks.size).joinToString(" ")
        // Half the time the distractors are gloss-head peers of the target —
        // e.g. sirep vs pules for "sleep" — which turns the blank into a
        // register/word choice rather than a random-word choice.
        val peers = if (random.nextInt(100) < 50) content.glossPeers(target, s, 3) else emptyList()
        val distr = (if (peers.size >= 2) peers else content.wordDistractors(target, s, 3))
            .filter { it != target }
        val options = (listOf(target) + distr).distinct().shuffled(random)
        if (options.size < 2) return null
        return FillBlankEx(before, after, target, options, s.en, s.register, s.id, allTags(s), focus, highlightFor(s))
    }

    fun makeListenTap(s: Sentence, toEnglish: Boolean, focus: List<String>): Exercise {
        return if (toEnglish) {
            val distr = content.distractorEnglish(s, 2)
            val options = (listOf(s.en) + distr).distinct().shuffled(random)
            ListenTapEx(s.bal, s.en, options, true, s.register, s.id, allTags(s), focus)
        } else {
            val distr = content.distractorBalinese(s, 2)
            val options = (listOf(s.bal) + distr).distinct().shuffled(random)
            ListenTapEx(s.bal, s.bal, options, false, s.register, s.id, allTags(s), focus)
        }
    }

    fun makeDictation(s: Sentence, focus: List<String>): Exercise =
        DictationEx(
            audioText = s.bal,
            answerNorm = TextMatch.normalize(s.bal),
            answerDisplay = s.bal,
            en = s.en,
            register = s.register,
            sentenceId = s.id,
            tags = allTags(s),
            focusTags = focus
        )

    fun makeType(s: Sentence, focus: List<String>): Exercise {
        // Also accept register variants of this sentence (dini↔driki, nanging↔sakewanten,
        // …) in any alus/andap/mixed combination — see ContentRepository.variantSentences.
        val canonical = TextMatch.normalize(s.bal)
        val alts = content.variantSentences(s)
            .map { TextMatch.normalize(it) }
            .filter { it.isNotEmpty() && it != canonical }
            .distinct()
        return TypeTranslationEx(
            promptEn = s.en,
            answerNorm = canonical,
            answerDisplay = s.bal,
            register = s.register,
            sentenceId = s.id,
            tags = allTags(s),
            focusTags = focus,
            altNorm = alts
        )
    }

    fun makeSpeak(s: Sentence, focus: List<String>): Exercise =
        SpeakEx(
            target = s.bal,
            targetNorm = TextMatch.normalize(s.bal),
            en = s.en,
            register = s.register,
            sentenceId = s.id,
            tags = allTags(s),
            focusTags = focus
        )

    private fun containsToken(s: Sentence, wordId: String): Boolean =
        s.bal.split(" ").any { it.lowercase().trim('.', ',', '!', '?', ';', ':') == wordId }

    private fun interleave(main: List<Exercise>, review: List<Exercise>): List<Exercise> {
        if (review.isEmpty()) return main
        val out = ArrayList<Exercise>(main.size + review.size)
        var ri = 0
        var count = 0
        for (ex in main) {
            out += ex
            count++
            if (ri < review.size && count % 4 == 0) { out += review[ri]; ri++ }
        }
        while (ri < review.size) { out += review[ri]; ri++ }
        return out
    }

    // ------------------------------------------------------------------ placement test

    /**
     * 14 milestone probes across the course (Balinese → English recognition).
     * Prefers sentences that introduce a new entry, but any well-sized non-headword
     * sentence of the milestone lesson qualifies — `newItem` is usually set only on
     * the headword line, so requiring it starved the test down to ~4 questions.
     */
    fun placementQuestions(): List<PlacementQ> {
        val out = ArrayList<PlacementQ>()
        for (ml in PLACEMENT_MILESTONES) {
            val lessonSents = content.sentencesByLesson[ml] ?: emptyList()
            val pool = lessonSents.filter {
                !it.headword && it.tokens.size in 3..9 && it.en.length in 8..70
            }
            val s = pool.filter { it.newItem != null }.randomOrNull(random)
                ?: pool.randomOrNull(random)
                ?: lessonSents.filter { !it.headword && it.tokens.size in 2..12 }.randomOrNull(random)
                ?: continue
            out += PlacementQ(
                ml,
                makeChoice(s, toEnglish = true, focus = listOfNotNull(s.newItem?.let { "w:$it" })) as MultipleChoiceEx
            )
        }
        return out
    }

    /** Highest milestone lesson justified by the answers (≥60% cumulative & milestone correct). */
    fun placementFrontier(results: List<Pair<Int, Boolean>>): Int {
        var frontier = 0
        var correct = 0
        var seen = 0
        for ((lesson, ok) in results.sortedBy { it.first }) {
            seen++
            if (ok) correct++
            val acc = correct.toDouble() / seen
            if (ok && acc >= 0.6) frontier = lesson
        }
        return frontier
    }

    /** Mark every path node of lessons ≤ frontier done; seed mid-strength SRS states. */
    fun applyPlacement(frontierLesson: Int) {
        if (frontierLesson <= 0) return
        val now = now()
        for (node in content.path) {
            if (node.lessonId <= frontierLesson) store.doneChunks.add(node.id)
        }
        for (lesson in content.lessonsById.values) {
            if (lesson.id > frontierLesson) continue
            for (item in lesson.items) {
                val st = store.wordState(item)
                st.successes = maxOf(st.successes, 2)
                st.lastReview = now
                st.box = maxOf(st.box, 3)
                if (st.stability <= 0.0) st.stability = 4.0
                if (st.difficulty <= 0.0) st.difficulty = 5.0
            }
        }
        for (p in content.patterns.values) {
            if (p.firstLesson > frontierLesson) continue
            val st = store.patternState(p.id)
            st.successes = maxOf(st.successes, 2)
            st.lastReview = now
            st.box = maxOf(st.box, 3)
            if (st.stability <= 0.0) st.stability = 6.0
            if (st.difficulty <= 0.0) st.difficulty = 5.0
        }
        store.save()
    }
}

/**
 * Decides WHICH tag to penalize on a wrong word-bank build (doc §5.4):
 *  - right tiles, wrong order          → grammar patterns
 *  - wrong/missing function word       → grammar patterns
 *  - one wrong content word            → that word entry
 *  - anything else                     → the exercise's focus tags
 */
object ErrorAttribution {
    fun forBank(ex: WordBankEx, builtLower: List<String>, sentence: Sentence?): List<String> {
        val answer = ex.answerTokens
        if (builtLower == answer) return emptyList()

        val ac = answer.groupingBy { it }.eachCount()
        val bc = builtLower.groupingBy { it }.eachCount()
        val missing = ArrayList<String>()
        val extra = ArrayList<String>()
        for ((t, c) in ac) {
            val d = c - (bc[t] ?: 0)
            repeat(if (d > 0) d else 0) { missing += t }
        }
        for ((t, c) in bc) {
            val d = c - (ac[t] ?: 0)
            repeat(if (d > 0) d else 0) { extra += t }
        }

        val pats = sentence?.patterns?.map { "p:$it" } ?: emptyList()
        return when {
            missing.isEmpty() && extra.isEmpty() ->
                if (pats.isNotEmpty()) pats else ex.focusTags
            (missing + extra).any { it in SessionBuilder.FUNCTION_TOKENS } ->
                if (pats.isNotEmpty()) pats else ex.focusTags
            missing.size == 1 -> {
                val wid = sentence?.words?.firstOrNull { w -> w.split(" ").contains(missing[0]) }
                if (wid != null) listOf("w:$wid") else ex.focusTags
            }
            else -> ex.focusTags
        }
    }
}
