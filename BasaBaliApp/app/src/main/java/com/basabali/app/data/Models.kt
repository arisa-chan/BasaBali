package com.basabali.app.data

/** A vocabulary entry from the curriculum (id = normalized Balinese form). */
data class WordEntry(
    val id: String,
    val firstLesson: Int,
    val gloss: String,
    val register: String          // "alus" | "andap" | "mider"
)

/** A grammar pattern (construct tag) from the pattern catalog. */
data class PatternEntry(
    val id: String,
    val name: String,
    val firstLesson: Int
)

/** One tagged sentence of the bank. */
data class Sentence(
    val id: String,
    val lesson: Int,
    val num: Int,
    val bal: String,              // display form (original casing, no sentence punctuation)
    val en: String,
    val tokens: List<String>,     // normalized lowercase tokens (for matching)
    val words: List<String>,      // word-entry ids contained in the sentence
    val patterns: List<String>,   // grammar pattern ids
    val newItem: String?,         // the single entry this sentence introduces (curriculum guarantee)
    val register: String,
    val headword: Boolean         // vocabulary-presentation line ("Buku." -> "Book.")
)

data class LessonMeta(
    val id: Int,
    val title: String,
    val section: Int,
    val items: List<String>,
    val chunks: List<List<String>>
)

data class ChunkMeta(
    val id: String,               // "L12C3"
    val sectionId: Int,
    val lessonId: Int,
    val index: Int,
    val items: List<String>
)

data class SectionMeta(
    val id: Int,
    val title: String,
    val lessons: List<LessonMeta>
)

// ------------------------------------------------------------------ v2 models

/** One display line of a story, tagged like a bank sentence. */
data class StoryLine(
    val bal: String,
    val en: String,
    val tokens: List<String>,
    val words: List<String>,
    val patterns: List<String>
)

/** English comprehension tap after the story; `line` indexes the tagged line it exercises. */
data class StoryQuestion(
    val prompt: String,
    val line: Int,
    val options: List<String>,
    val answer: Int
)

data class Story(
    val id: String,
    val title: String,
    val titleEn: String,
    val gate: Int,                    // unlocked when every chunk of lessons <= gate is done
    val lines: List<StoryLine>,
    val questions: List<StoryQuestion>
)

/** One simulated league rival: display name + study-profile index (see LeagueEngine.PROFILES). */
class RivalState(val name: String, val profile: Int = 0)

class LeagueData(
    var tier: Int = 0,
    var weekStart: Long = 0L,
    var userXp: Int = 0,
    val rivals: MutableList<RivalState> = mutableListOf()
)

// ------------------------------------------------------------------ path structure (v3)

/**
 * Per-lesson unit structure (5 units):
 *  U1 Learn      — one subunit per ~5-word chunk (first teaching pass)
 *  U2 Reinforce  — the same chunks again (second pass; SRS fills review)
 *  U3 Cumulative — 3 subunits reviewing Lesson 1..this lesson
 *  U4 Lesson rev — ceil(items/10) subunits covering every entry of this lesson
 *  U5 Mastery    — a single full-coverage subunit for this lesson
 */
sealed class NodeKind {
    data class Learn(val chunk: ChunkMeta) : NodeKind()
    /** U2 subunit: sentence exercises over an already-introduced chunk (no word cards). */
    data class Reinforce(val chunk: ChunkMeta) : NodeKind()
    data class Cumulative(val upToLesson: Int, val slots: Int) : NodeKind()
    data class LessonReview(val lessonId: Int, val slots: Int) : NodeKind()
}

data class PathNode(
    val id: String,          // "L12U3S1"
    val lessonId: Int,
    val unit: Int,           // 1..5
    val sub: Int,            // 0-based subunit index
    val kind: NodeKind
)


// ------------------------------------------------------------------ dictionary

/** One sourced example sentence from the BASAbali / BASAibu Wiki dictionary. */
data class DictionaryExample(val bal: String, val en: String)

/** One offline dictionary record; lesson == 0 means it is outside the course. */
data class DictionaryEntry(
    val id: String,
    val headword: String,
    val english: String,
    val definitions: List<String>,
    val pos: String,
    val register: String,
    val examples: List<DictionaryExample>,
    val lesson: Int
)
