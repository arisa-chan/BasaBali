package com.basabali.app.ui.lesson

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.basabali.app.data.ContentRepository
import com.basabali.app.data.Sentence
import com.basabali.app.data.UserStore
import com.basabali.app.engine.Caps
import com.basabali.app.engine.DictationEx
import com.basabali.app.engine.ErrorAttribution
import com.basabali.app.engine.Exercise
import com.basabali.app.engine.FillBlankEx
import com.basabali.app.engine.ListenTapEx
import com.basabali.app.engine.MatchPairsEx
import com.basabali.app.engine.MultipleChoiceEx
import com.basabali.app.engine.SessionBuilder
import com.basabali.app.engine.SpeakEx
import com.basabali.app.engine.Srs
import com.basabali.app.engine.TextMatch
import com.basabali.app.engine.TypeTranslationEx
import com.basabali.app.engine.WordBankEx
import com.basabali.app.engine.WordCardEx
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/**
 * Runtime state machine for one session.
 *
 * Handles: hearts, grading (tap / build / type / speak), SRS updates for every
 * tag on the exercise, error attribution (word vs pattern blame), in-session
 * re-queue of a downgraded variant after a miss, XP, streaks, quests, league
 * XP and chunk completion.
 */
class SessionRunner(
    initialExercises: List<Exercise>,
    private val builder: SessionBuilder,
    private val caps: Caps,
    private val content: ContentRepository,
    private val store: UserStore,
    private val nodeId: String?,
    private val onFinish: (SessionResult) -> Unit
) {
    data class Feedback(val ok: Boolean, val correctText: String?, val note: String? = null)

    data class SessionResult(
        val xp: Int,
        val correct: Int,
        val total: Int,
        val failed: Boolean,
        val streak: Int,
        val streakUp: Boolean,
        val questBonus: Int
    )

    private val queue = initialExercises.toMutableStateList()

    var index by mutableStateOf(0)
        private set
    var hearts by mutableStateOf(5)
        private set
    var correctCount by mutableStateOf(0)
        private set
    var answeredCount by mutableStateOf(0)
        private set
    var feedback by mutableStateOf<Feedback?>(null)
        private set
    var finished by mutableStateOf(false)
        private set

    private val requeued = HashSet<String>()
    private val missedThisSession = HashSet<String>()
    private var speakReplacements = 0
    private var resultDelivered = false

    val current: Exercise? get() = queue.getOrNull(index)
    val total: Int get() = queue.size
    val progress: Float get() = if (queue.isEmpty()) 0f else index.toFloat() / queue.size.toFloat()

    // ------------------------------------------------------------------ inputs

    /** Word cards are exposure only: register the word as taught, no grading. */
    fun onCardSeen(ex: WordCardEx) {
        val st = store.wordState(ex.wordId)
        if (st.lastReview <= 0L) st.lastReview = System.currentTimeMillis()
    }

    /** Match-pairs is a warm-up: exposure credit, no heart loss. */
    fun onMatchPairsDone(ex: MatchPairsEx, errors: Int) {
        val now = System.currentTimeMillis()
        for (t in ex.tags) updateTag(t, ok = true, now = now)
        answeredCount++
        if (errors == 0) correctCount++
    }

    fun submitChoice(ex: MultipleChoiceEx, choice: String): Boolean =
        grade(ex, choice == ex.correct, ex.correct)

    fun submitListen(ex: ListenTapEx, choice: String): Boolean =
        grade(ex, choice == ex.correct, ex.correct)

    fun submitBlank(ex: FillBlankEx, choice: String): Boolean =
        grade(ex, choice == ex.correct, content.display(ex.correct))

    fun submitBank(ex: WordBankEx, built: List<String>): Boolean {
        val builtLower = built.map { it.lowercase() }
        // Register variants are position-wise alternatives, so all-Alus, all-Andap
        // and mixed builds (dini + sakewanten + driki) are all accepted.
        val ok = when {
            ex.altGroups.isNotEmpty() && builtLower.size == ex.altGroups.size ->
                builtLower.indices.all { builtLower[it] in ex.altGroups[it] }
            else -> builtLower == ex.answerTokens
        }
        val blame = if (!ok) {
            ErrorAttribution.forBank(ex, builtLower, ex.sentenceId?.let { content.sentencesById[it] })
        } else emptyList()
        return grade(ex, ok, ex.answerDisplay, blame)
    }

    /**
     * E6 dictation / E8 typing: similarity-graded with an "almost" band.
     * [altNorm] carries register/mix variants (dini↔driki …): an exact match on
     * any variant is fully correct, so all-Alus, all-Andap and mixed answers pass.
     */
    fun submitTyped(
        ex: Exercise,
        typed: String,
        answerNorm: String,
        answerDisplay: String,
        altNorm: List<String> = emptyList()
    ): Boolean {
        val sim = TextMatch.sentenceSimilarity(typed, answerNorm)
        if (sim >= TextMatch.TYPING_EXACT) return grade(ex, true, answerDisplay)
        var bestAlt = 0.0
        for (alt in altNorm) {
            val s = TextMatch.sentenceSimilarity(typed, alt)
            if (s > bestAlt) bestAlt = s
            if (bestAlt >= TextMatch.TYPING_EXACT) break
        }
        return when {
            bestAlt >= TextMatch.TYPING_EXACT ->
                grade(ex, true, answerDisplay, note = "Accepted — another register fits this sentence too")
            sim >= TextMatch.TYPING_NEAR ->
                grade(ex, true, answerDisplay, note = "Almost — watch the spelling")
            bestAlt >= TextMatch.TYPING_NEAR ->
                grade(ex, true, answerDisplay, note = "Almost — accepted as a register variant")
            else -> grade(ex, false, answerDisplay)
        }
    }

    /** E9 speaking: best ASR hypothesis vs target; generous threshold. */
    fun submitSpeak(ex: SpeakEx, transcripts: List<String>): Boolean {
        if (transcripts.isEmpty()) return false
        var best = 0.0
        var bestText = ""
        for (t in transcripts) {
            val sim = TextMatch.sentenceSimilarity(t, ex.targetNorm)
            if (sim > best) { best = sim; bestText = t }
        }
        val ok = best >= TextMatch.SPEAK_PASS
        val note = if (ok && best < TextMatch.TYPING_EXACT) "Heard: “$bestText”"
                   else if (!ok) "Heard: “$bestText”"
                   else null
        return grade(ex, ok, ex.target, note = note)
    }

    fun continueNext() {
        if (finished) return
        feedback = null
        index++
        if (hearts <= 0 || index >= queue.size) finish()
    }

    /**
     * Persist learner state when the learner leaves mid-session (the review
     * mistake log and SRS updates should survive the exit).
     */
    fun persist() {
        store.save()
    }

    /**
     * The learner asked to stop speaking: replace the current speaking step and
     * every later one with an equivalent non-speaking exercise on the same
     * sentence (word-bank / fill-blank / recognition), so the subunit keeps its
     * full exercise count and the sentence still gets practised.
     */
    fun replaceSpeaking() {
        for (i in index until queue.size) {
            val speak = queue.getOrNull(i) as? SpeakEx ?: continue
            val alt = nonSpeakingAlternative(speak, speakReplacements) ?: continue
            queue[i] = alt
            speakReplacements++
        }
    }

    private fun nonSpeakingAlternative(ex: SpeakEx, n: Int): Exercise? {
        val s = ex.sentenceId?.let { content.sentencesById[it] } ?: return null
        val focus = ex.focusTags.ifEmpty { ex.tags }
        return when (n % 4) {
            0 -> if (s.tokens.size in SessionBuilder.MIN_TOKENS..SessionBuilder.MAX_BANK_TOKENS)
                builder.makeBank(s, focus) else builder.makeChoice(s, toEnglish = false, focus)
            1 -> if (caps.fillUnlocked()) builder.makeFillBlank(s, focus) ?: builder.makeBank(s, focus)
                 else builder.makeBank(s, focus)
            2 -> builder.makeChoice(s, toEnglish = false, focus)
            else -> builder.makeChoice(s, toEnglish = true, focus)
        }
    }

    // ------------------------------------------------------------------ internals

    private fun grade(
        ex: Exercise,
        ok: Boolean,
        correctText: String,
        blameOverride: List<String>? = null,
        note: String? = null
    ): Boolean {
        if (feedback != null || finished) return ok   // ignore double submits
        val now = System.currentTimeMillis()
        answeredCount++
        ex.sentenceId?.let { store.touchSeen(it, now) }
        if (ok) {
            correctCount++
            // a correct answer clears the sentence from the mistake log (Review tab) —
            // but not the same-session requeue, so a recovered miss stays reviewable
            ex.sentenceId?.let { sid ->
                if (sid !in missedThisSession) store.clearMistake(sid)
            }
            for (t in ex.tags) updateTag(t, ok = true, now = now)
            feedback = Feedback(true, null, note)
        } else {
            hearts = max(0, hearts - 1)
            ex.sentenceId?.let { sid ->
                missedThisSession += sid
                store.noteMistake(sid)
            }
            val blame = blameOverride ?: ex.focusTags.ifEmpty { ex.tags }
            for (t in blame) updateTag(t, ok = false, now = now)
            feedback = Feedback(false, correctText, note)
            val sid = ex.sentenceId
            if (sid != null && requeued.add(sid)) {
                downgrade(ex)?.let { easier ->
                    val pos = min(index + 2, queue.size)
                    queue.add(pos, easier)
                }
            }
        }
        return ok
    }

    private fun updateTag(tag: String, ok: Boolean, now: Long) {
        if (tag.startsWith("w:")) {
            val st = store.wordState(tag.substring(2))
            if (ok) Srs.onCorrect(st, now) else Srs.onWrong(st, now)
        } else if (tag.startsWith("p:")) {
            val st = store.patternState(tag.substring(2))
            if (ok) Srs.onCorrect(st, now) else Srs.onWrong(st, now)
        }
    }

    private fun downgrade(ex: Exercise): Exercise? {
        val sid = ex.sentenceId ?: return null
        val s = content.sentencesById[sid] ?: return null
        return when (ex) {
            is WordBankEx, is FillBlankEx, is DictationEx, is TypeTranslationEx, is SpeakEx -> {
                val distr = content.distractorBalinese(s, 2)
                val options = (listOf(s.bal) + distr).distinct().shuffled()
                MultipleChoiceEx(s.en, s.bal, options, false, s.register, s.id, ex.tags, ex.focusTags)
            }
            is ListenTapEx -> {
                // fall back to a readable choice exercise on the same sentence
                val distr = content.distractorEnglish(s, 2)
                val options = (listOf(s.en) + distr).distinct().shuffled()
                MultipleChoiceEx(s.bal, s.en, options, true, s.register, s.id, ex.tags, ex.focusTags)
            }
            is MultipleChoiceEx -> ex.copy(options = ex.options.shuffled())
            else -> null
        }
    }

    private fun finish() {
        if (resultDelivered) return
        resultDelivered = true
        finished = true
        val failed = hearts <= 0

        if (!failed && nodeId != null) store.doneChunks.add(nodeId)

        // the streak is advanced inside recordSession (shared with stories, so it always
        // matches the practice calendar); report whether this session was today's first
        val today = LocalDate.now().toString()
        val streakBefore = store.lastDayKey

        val xpGain = correctCount * 2 + (if (!failed) 10 else 0)
        store.xp += xpGain

        var questBonus = 0
        if (!failed) {
            questBonus = store.recordSession(xpGain, lessonsCompleted = 1, perfectAnswers = correctCount)
        }
        val streakUp = !failed && streakBefore != today && store.lastDayKey == today
        store.save()
        onFinish(SessionResult(xpGain + questBonus, correctCount, answeredCount, failed, store.streak, streakUp, questBonus))
    }
}
