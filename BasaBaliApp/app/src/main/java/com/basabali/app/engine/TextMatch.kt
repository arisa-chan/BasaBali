package com.basabali.app.engine

import kotlin.math.max

/**
 * Text comparison for typed answers (E6 dictation, E8 free typing) and
 * ASR transcripts (E9 speaking). Balinese-aware normalization: case,
 * é/è → e, punctuation stripped.
 */
object TextMatch {

    fun normalize(s: String): String {
        val lowered = s.lowercase().replace('é', 'e').replace('è', 'e')
        val sb = StringBuilder()
        for (c in lowered) {
            if (c.isLetterOrDigit() || c.isWhitespace()) sb.append(c) else sb.append(' ')
        }
        return sb.toString().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun tokens(s: String): List<String> = normalize(s).split(" ").filter { it.isNotEmpty() }

    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val tmp = prev; prev = cur; cur = tmp
        }
        return prev[b.length]
    }

    fun tokenSimilarity(a: String, b: String): Double {
        val m = max(a.length, b.length)
        if (m == 0) return 1.0
        return 1.0 - levenshtein(a, b).toDouble() / m
    }

    /** Position-aligned token similarity in 0..1 (order-sensitive, by design). */
    fun sentenceSimilarity(a: String, b: String): Double {
        val ta = tokens(a)
        val tb = tokens(b)
        if (ta.isEmpty() && tb.isEmpty()) return 1.0
        val n = max(ta.size, tb.size)
        var sum = 0.0
        for (i in 0 until n) {
            val x = ta.getOrNull(i)
            val y = tb.getOrNull(i)
            sum += if (x == null || y == null) 0.0 else tokenSimilarity(x, y)
        }
        return sum / n
    }

    const val TYPING_EXACT = 0.999
    const val TYPING_NEAR = 0.85   // accepted with an "almost" note
    const val SPEAK_PASS = 0.60    // ASR transcripts are noisy; be generous
}
