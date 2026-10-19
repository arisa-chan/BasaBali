package com.basabali.app.engine

import kotlin.math.exp
import kotlin.math.pow

/**
 * FSRS-4.5 scheduling math with the published default weights.
 *
 * Stability `S` (days) and difficulty `D` (1..10) live on SrsState;
 * retrievability follows the power forgetting curve R = (1 + t/(9S))^-1.
 * Our binary outcomes map to grades: first correct = Hard(2), later
 * correct = Good(3), wrong = Again(1).
 */
object Fsrs {

    private val W = doubleArrayOf(
        0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01,
        1.49, 0.14, 0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61
    )

    const val REQUEST_RETENTION = 0.85

    fun initStability(grade: Int): Double = W[(grade - 1).coerceIn(0, 3)].coerceAtLeast(0.1)

    fun initDifficulty(grade: Int): Double =
        clampD(W[4] - exp(W[5] * (grade - 1)) + 1.0)

    fun retrievability(elapsedDays: Double, stability: Double): Double {
        if (stability <= 0.0) return 0.0
        return (1.0 + elapsedDays / (9.0 * stability)).pow(-1.0)
    }

    fun intervalDays(stability: Double): Double =
        (9.0 * stability * (1.0 / REQUEST_RETENTION - 1.0)).coerceAtLeast(0.4)

    fun nextDifficulty(d: Double, grade: Int): Double {
        val d0 = initDifficulty(4)
        return clampD(W[7] * d0 + (1.0 - W[7]) * (d - W[6] * (grade - 3)))
    }

    fun nextRecallStability(d: Double, s: Double, r: Double, grade: Int): Double {
        val hardPenalty = if (grade == 2) W[15] else 1.0
        val easyBonus = if (grade == 4) W[16] else 1.0
        val ns = s * (1.0 + exp(W[8]) * (11.0 - d) * s.pow(-W[9]) *
                (exp(W[10] * (1.0 - r)) - 1.0) * hardPenalty * easyBonus)
        return ns.coerceAtLeast(0.1)
    }

    fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        val ns = W[11] * d.pow(-W[12]) * ((s + 1.0).pow(W[13]) - 1.0) * exp(W[14] * (1.0 - r))
        return ns.coerceAtLeast(0.1)
    }

    private fun clampD(x: Double): Double = x.coerceIn(1.0, 10.0)
}
