package com.basabali.app.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Per-tag spaced-repetition state. Tags are words ("w:id") or grammar
 * patterns ("p:id"). Carries both Leitner fields (box) and FSRS fields
 * (stability/difficulty) so the scheduler can be switched at runtime
 * without migration (see app_design_learning_system.md §4, §7).
 */
class SrsState(
    var successes: Int = 0,
    var lapses: Int = 0,
    var box: Int = 0,
    var lastReview: Long = 0L,
    var stability: Double = 0.0,     // FSRS S (days); 0 = uninitialized
    var difficulty: Double = 0.0     // FSRS D (1..10); 0 = uninitialized
)

/**
 * Scheduler facade. Mode is set once at app start from user settings:
 *  - LEITNER: box-based expanding half-lives (1..128 days), p = 2^(-Δ/h)
 *  - FSRS:    FSRS-4.5 stability/difficulty with power forgetting curve
 */
object Srs {
    enum class Mode { LEITNER, FSRS }

    var mode: Mode = Mode.LEITNER

    const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    const val TAU_WORD: Double = 0.70
    const val TAU_PATTERN: Double = 0.60
    private const val MAX_BOX = 7

    fun halfLifeMs(st: SrsState): Long = DAY_MS * (1L shl min(max(st.box, 0), MAX_BOX))

    private fun elapsedDays(st: SrsState, now: Long): Double =
        max(0L, now - st.lastReview).toDouble() / DAY_MS

    /** Lazily bootstrap FSRS fields for states created under Leitner (mode switching). */
    private fun ensureFsrs(st: SrsState) {
        if (mode == Mode.FSRS && st.lastReview > 0L && st.stability <= 0.0) {
            st.stability = (1 shl min(max(st.box, 0), MAX_BOX)).toDouble()
            if (st.difficulty <= 0.0) st.difficulty = 5.0
        }
    }

    fun pRecall(st: SrsState, now: Long): Double {
        if (st.lastReview <= 0L) return 0.0
        ensureFsrs(st)
        return when (mode) {
            Mode.LEITNER -> {
                val d = max(0L, now - st.lastReview)
                2.0.pow(-d.toDouble() / halfLifeMs(st).toDouble())
            }
            Mode.FSRS -> {
                if (st.stability <= 0.0) 0.0 else Fsrs.retrievability(elapsedDays(st, now), st.stability)
            }
        }
    }

    fun isDue(st: SrsState, now: Long, tau: Double): Boolean {
        if (st.lastReview <= 0L) return false
        ensureFsrs(st)
        return when (mode) {
            Mode.LEITNER -> pRecall(st, now) < tau
            Mode.FSRS -> st.stability > 0.0 && elapsedDays(st, now) >= Fsrs.intervalDays(st.stability)
        }
    }

    fun onCorrect(st: SrsState, now: Long) {
        when (mode) {
            Mode.LEITNER -> {
                st.box = min(st.box + 1, MAX_BOX)
            }
            Mode.FSRS -> {
                st.box = min(st.box + 1, MAX_BOX)             // keep Leitner field in sync
                val grade = if (st.successes == 0) 2 else 3   // first success = Hard, then Good
                ensureFsrs(st)
                if (st.stability <= 0.0 || st.difficulty <= 0.0) {
                    st.stability = Fsrs.initStability(grade)
                    st.difficulty = Fsrs.initDifficulty(grade)
                } else {
                    val r = Fsrs.retrievability(elapsedDays(st, now), st.stability)
                    st.difficulty = Fsrs.nextDifficulty(st.difficulty, grade)
                    st.stability = Fsrs.nextRecallStability(st.difficulty, st.stability, r, grade)
                }
            }
        }
        st.successes += 1
        st.lastReview = now
    }

    fun onWrong(st: SrsState, now: Long) {
        when (mode) {
            Mode.LEITNER -> {
                st.box = max(0, st.box - 2)
            }
            Mode.FSRS -> {
                st.box = max(0, st.box - 2)                   // keep Leitner field in sync
                ensureFsrs(st)
                if (st.stability <= 0.0 || st.difficulty <= 0.0) {
                    st.stability = Fsrs.initStability(1)
                    st.difficulty = Fsrs.initDifficulty(1)
                } else {
                    val r = Fsrs.retrievability(elapsedDays(st, now), st.stability)
                    st.difficulty = Fsrs.nextDifficulty(st.difficulty, 1)
                    st.stability = Fsrs.nextForgetStability(st.difficulty, st.stability, r)
                }
            }
        }
        st.lapses += 1
        st.lastReview = now
    }
}
