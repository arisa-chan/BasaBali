package com.basabali.app.engine

/**
 * Exercise specifications produced by [SessionBuilder] and rendered by the UI.
 *
 * Tag conventions:
 *  - `tags`      — every SRS tag the exercise touches; all get `onCorrect` when answered right
 *  - `focusTags` — the subset blamed on a wrong answer (refined by [ErrorAttribution])
 */
sealed class Exercise {
    open val sentenceId: String? = null
    open val tags: List<String> = emptyList()
    open val focusTags: List<String> = emptyList()
}

/** E1 — zero-recall vocabulary card introducing a new word. */
data class WordCardEx(
    val wordId: String,
    val gloss: String,
    val register: String,
    val exampleBal: String,
    val exampleEn: String
) : Exercise() {
    override val tags: List<String> = listOf("w:$wordId")
}

/** E2 — tap-the-pairs warm-up (word id ↔ unique gloss label). */
data class MatchPairsEx(
    val pairs: List<Pair<String, String>>
) : Exercise() {
    override val tags: List<String> = pairs.map { "w:${it.first}" }
}

/** E3 — multiple choice, either direction. */
data class MultipleChoiceEx(
    val prompt: String,
    val correct: String,
    val options: List<String>,
    val toEnglish: Boolean,
    val register: String,
    override val sentenceId: String?,
    override val tags: List<String>,
    override val focusTags: List<String>,
    val highlight: String? = null     // entry id to tint purple (first encounter)
) : Exercise()

/** E4 — build the Balinese sentence from tiles (+ one distractor tile). */
data class WordBankEx(
    val promptEn: String,
    val answerTokens: List<String>,   // lowercase, for comparison
    val bankTokens: List<String>,     // display casing
    val register: String,
    override val sentenceId: String?,
    override val tags: List<String>,
    override val focusTags: List<String>,
    val highlight: String? = null     // entry id whose tiles render purple
) : Exercise()

/** E7 — fill the blank; options are word ids, displayed via ContentRepository.display(). */
data class FillBlankEx(
    val before: String,
    val after: String,
    val correct: String,              // word id (lowercase)
    val options: List<String>,        // word ids
    val en: String,
    val register: String,
    override val sentenceId: String?,
    override val tags: List<String>,
    override val focusTags: List<String>,
    val highlight: String? = null     // entry id to tint purple (first encounter)
) : Exercise()

/** E5 — listen & tap: audio of the Balinese sentence; pick the English meaning
 *  (toEnglish = true) or the correct Balinese transcript (toEnglish = false). */
data class ListenTapEx(
    val audioText: String,            // Balinese text for TTS
    val correct: String,
    val options: List<String>,
    val toEnglish: Boolean,
    val register: String,
    override val sentenceId: String?,
    override val tags: List<String>,
    override val focusTags: List<String>
) : Exercise()

/** E6 — dictation: hear the Balinese, type it. Graded by TextMatch similarity. */
data class DictationEx(
    val audioText: String,
    val answerNorm: String,           // normalized answer for similarity
    val answerDisplay: String,        // shown in feedback
    val en: String,
    val register: String,
    override val sentenceId: String?,
    override val tags: List<String>,
    override val focusTags: List<String>
) : Exercise()

/** E8 — free typing translation: English prompt → type the Balinese sentence. */
data class TypeTranslationEx(
    val promptEn: String,
    val answerNorm: String,
    val answerDisplay: String,
    val register: String,
    override val sentenceId: String?,
    override val tags: List<String>,
    override val focusTags: List<String>,
    /** Normalized register/mix alternatives that grade as correct (dini↔driki …). */
    val altNorm: List<String> = emptyList()
) : Exercise()

/** E9 — speak: read the Balinese aloud; ASR transcript scored by similarity. */
data class SpeakEx(
    val target: String,               // Balinese sentence (display)
    val targetNorm: String,
    val en: String,
    val register: String,
    override val sentenceId: String?,
    override val tags: List<String>,
    override val focusTags: List<String>
) : Exercise()
