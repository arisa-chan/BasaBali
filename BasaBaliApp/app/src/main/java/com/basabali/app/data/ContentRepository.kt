package com.basabali.app.data

import android.content.res.AssetManager
import com.basabali.app.engine.TextMatch
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Loads the compiled course assets (see tools/build_content.py) and provides
 * the indices the session builder needs:
 *  - sentences by lesson / by word
 *  - headword + usage sentences per word per lesson
 *  - distractor generators for multiple choice, word bank and fill-blank
 */
class ContentRepository(private val assets: AssetManager) {

    val words = LinkedHashMap<String, WordEntry>()
    val patterns = LinkedHashMap<String, PatternEntry>()
    val sentences = ArrayList<Sentence>(9000)
    val sentencesById = HashMap<String, Sentence>(9000)
    val sentencesByLesson = HashMap<Int, List<Sentence>>()
    val sentencesByWord = HashMap<String, MutableList<Sentence>>()
    val sections = ArrayList<SectionMeta>()
    val stories = ArrayList<Story>()
    val chunks = ArrayList<ChunkMeta>()
    val chunksById = HashMap<String, ChunkMeta>()
    val lessonsById = HashMap<Int, LessonMeta>()

    /** Full unit/subunit path (U1 learn, U2 reinforce, U3 cumulative, U4 lesson review, U5 mastery). */
    val path = ArrayList<PathNode>()
    var nodesByLesson: Map<Int, List<PathNode>> = emptyMap()
        private set

    // dictionary lookup indices (tap-a-word popups)
    private var byToken: Map<String, List<String>> = emptyMap()
    private var byFirstToken: Map<String, List<String>> = emptyMap()

    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        loadWords()
        loadPatterns()
        loadSentences()
        loadCurriculum()
        loadStories()
        buildIndices()
        buildPath()
        loaded = true
    }

    // ---------------------------------------------------------------- loading

    private fun readJson(name: String): String =
        assets.open("content/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun loadWords() {
        val arr = JSONObject(readJson("words.json")).getJSONArray("words")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.getString("id")
            words[id] = WordEntry(
                id = id,
                firstLesson = o.getInt("lesson"),
                gloss = o.optString("gloss", id),
                register = o.optString("register", "mider")
            )
        }
    }

    private fun loadPatterns() {
        val arr = JSONObject(readJson("patterns.json")).getJSONArray("patterns")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.getString("id")
            patterns[id] = PatternEntry(id, o.optString("name", id), o.getInt("lesson"))
        }
    }

    private fun loadSentences() {
        val arr = JSONObject(readJson("sentences.json")).getJSONArray("sentences")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val s = Sentence(
                id = o.getString("id"),
                lesson = o.getInt("lesson"),
                num = o.getInt("num"),
                bal = o.getString("bal"),
                en = o.getString("en"),
                tokens = o.getJSONArray("tokens").toStringList(),
                words = o.getJSONArray("words").toStringList(),
                patterns = o.getJSONArray("patterns").toStringList(),
                newItem = o.optString("newItem", "").ifEmpty { null },
                register = o.optString("register", "mider"),
                headword = o.optBoolean("headword", false)
            )
            sentences += s
            sentencesById[s.id] = s
        }
    }

    private fun loadCurriculum() {
        val root = JSONObject(readJson("curriculum.json"))
        val secs = root.getJSONArray("sections")
        for (i in 0 until secs.length()) {
            val so = secs.getJSONObject(i)
            val lessonsJ = so.getJSONArray("lessons")
            val lessons = ArrayList<LessonMeta>()
            for (j in 0 until lessonsJ.length()) {
                val lo = lessonsJ.getJSONObject(j)
                val lid = lo.getInt("id")
                val chunksJ = lo.getJSONArray("chunks")
                val chunkLists = ArrayList<List<String>>()
                for (c in 0 until chunksJ.length()) chunkLists += chunksJ.getJSONArray(c).toStringList()
                val meta = LessonMeta(
                    id = lid,
                    title = lo.getString("title"),
                    section = so.getInt("id"),
                    items = lo.getJSONArray("items").toStringList(),
                    chunks = chunkLists
                )
                lessons += meta
                lessonsById[lid] = meta
                chunkLists.forEachIndexed { idx, items ->
                    val ch = ChunkMeta("L${lid}C$idx", so.getInt("id"), lid, idx, items)
                    chunks += ch
                    chunksById[ch.id] = ch
                }
            }
            sections += SectionMeta(so.getInt("id"), so.getString("title"), lessons)
        }
    }

    private fun loadStories() {
        try {
            val arr = JSONObject(readJson("stories.json")).getJSONArray("stories")
            for (i in 0 until arr.length()) {
                val so = arr.getJSONObject(i)
                val lines = ArrayList<StoryLine>()
                val lj = so.getJSONArray("lines")
                for (j in 0 until lj.length()) {
                    val lo = lj.getJSONObject(j)
                    lines += StoryLine(
                        bal = lo.getString("bal"),
                        en = lo.getString("en"),
                        tokens = lo.getJSONArray("tokens").toStringList(),
                        words = lo.getJSONArray("words").toStringList(),
                        patterns = lo.getJSONArray("patterns").toStringList()
                    )
                }
                val qs = ArrayList<StoryQuestion>()
                val qj = so.getJSONArray("questions")
                for (j in 0 until qj.length()) {
                    val qo = qj.getJSONObject(j)
                    qs += StoryQuestion(
                        prompt = qo.getString("prompt"),
                        line = qo.getInt("line"),
                        options = qo.getJSONArray("options").toStringList(),
                        answer = qo.getInt("answer")
                    )
                }
                stories += Story(
                    id = so.getString("id"),
                    title = so.getString("title"),
                    titleEn = so.getString("titleEn"),
                    gate = so.getInt("gate"),
                    lines = lines,
                    questions = qs
                )
            }
        } catch (_: Exception) {
            // stories are optional content
        }
    }

    fun storyById(id: String): Story? = stories.firstOrNull { it.id == id }

    private fun org.json.JSONArray.toStringList(): List<String> =
        (0 until length()).map { getString(it) }

    private fun buildIndices() {
        buildLookupIndex()
        sentencesByLesson.putAll(sentences.groupBy { it.lesson })
        for (s in sentences) {
            for (w in s.words) {
                sentencesByWord.getOrPut(w) { ArrayList() } += s
            }
        }
    }

    private fun buildPath() {
        path.clear()
        for (sec in sections) {
            for (lesson in sec.lessons) {
                val lessonChunks = lesson.chunks.indices.mapNotNull { chunksById["L${lesson.id}C$it"] }
                val k = lessonChunks.size
                // U1 Learn: introduce the chunks one at a time, then close with a
                // whole-lesson review subunit.
                for (i in 0 until k) {
                    path += PathNode("L${lesson.id}U1S$i", lesson.id, 1, i, NodeKind.Learn(lessonChunks[i]))
                }
                path += PathNode("L${lesson.id}U1S$k", lesson.id, 1, k, NodeKind.LessonReview(lesson.id, 18))
                // U2 Reinforce: sentence exercises over the same chunks (no new-word
                // introduction), then another whole-lesson review subunit.
                for (i in 0 until k) {
                    path += PathNode("L${lesson.id}U2S$i", lesson.id, 2, i, NodeKind.Reinforce(lessonChunks[i]))
                }
                path += PathNode("L${lesson.id}U2S$k", lesson.id, 2, k, NodeKind.LessonReview(lesson.id, 18))
                // U3 Cumulative review: everything from Lesson 1 up to this lesson.
                for (i in 0 until 3) {
                    path += PathNode("L${lesson.id}U3S$i", lesson.id, 3, i, NodeKind.Cumulative(lesson.id, 18))
                }
                // U4 Lesson review: ceil(words / 10) subunits.
                val n4 = (lesson.items.size + 9) / 10
                for (i in 0 until n4) {
                    path += PathNode("L${lesson.id}U4S$i", lesson.id, 4, i, NodeKind.LessonReview(lesson.id, 18))
                }
                // U5 Mastery check.
                path += PathNode("L${lesson.id}U5S0", lesson.id, 5, 0, NodeKind.LessonReview(lesson.id, 18))
            }
        }
        nodesByLesson = path.groupBy { it.lessonId }
    }

    /** Head of a gloss: text before '/' or '(' — groups synonyms and register pairs. */
    fun glossHead(gloss: String): String {
        val cut = gloss.indexOfFirst { it == '/' || it == '(' }
        return (if (cut >= 0) gloss.substring(0, cut) else gloss).trim().lowercase()
    }

    /**
     * Distractor ids that share a gloss head with [target] (e.g. sirep/pules both
     * "sleep") — powers register-choice fill-blanks. Falls back to other lesson
     * words when a gloss has no peers.
     */
    fun glossPeers(target: String, s: Sentence, n: Int): List<String> {
        val tw = words[target] ?: return emptyList()
        val head = glossHead(tw.gloss)
        val pool = ArrayList(
            words.values
                .filter { it.id != target && !it.id.contains(' ') && it.firstLesson <= s.lesson && glossHead(it.gloss) == head }
                .map { it.id }
        )
        if (pool.size < n) {
            val extra = (sentencesByLesson[s.lesson] ?: emptyList())
                .flatMap { it.words }.distinct()
                .filter { it != target && !it.contains(' ') && it !in pool }
            pool += extra
        }
        return pool.shuffled().take(n)
    }

    private fun buildLookupIndex() {
        val single = HashMap<String, MutableList<String>>()
        val multi = HashMap<String, MutableList<String>>()
        for (id in words.keys) {
            val toks = id.split(" ")
            if (toks.size == 1) single.getOrPut(toks[0]) { ArrayList() } += id
            else multi.getOrPut(toks[0]) { ArrayList() } += id
        }
        byToken = single
        byFirstToken = multi
    }

    /**
     * Dictionary lookup for the token at [index] in a normalized sentence:
     * every vocabulary entry that covers that position (single-word entries —
     * including bases of -ne/-e/-n suffixed forms — plus multi-word entries
     * whose token span includes the position). Powers the tap-a-word popup.
     */
    fun lookupsAt(tokensNorm: List<String>, index: Int): List<WordEntry> {
        if (index !in tokensNorm.indices) return emptyList()
        val out = LinkedHashMap<String, WordEntry>()
        val t = tokensNorm[index]
        val bases = LinkedHashSet<String>()
        bases += t
        for (suf in listOf("ne", "e", "n")) {
            if (t.length > suf.length && t.endsWith(suf)) bases += t.substring(0, t.length - suf.length)
        }
        for (b in bases) {
            byToken[b]?.forEach { id -> words[id]?.let { out.putIfAbsent(id, it) } }
        }
        for (start in 0..index) {
            val ids = byFirstToken[tokensNorm[start]] ?: continue
            for (id in ids) {
                val w = id.split(" ")
                if (start + w.size - 1 < index) continue
                if (start + w.size > tokensNorm.size) continue
                var match = true
                for (k in w.indices) {
                    if (tokensNorm[start + k] != w[k]) { match = false; break }
                }
                if (match) words[id]?.let { out.putIfAbsent(id, it) }
            }
        }
        return out.values.toList()
    }

    // ---------------------------------------------------------------- queries

    fun gloss(id: String): String = words[id]?.gloss ?: id

    fun registerOf(id: String): String = words[id]?.register ?: "mider"

    /** The vocabulary-presentation line for [wordId] inside [lesson] (if any). */
    fun headwordSentence(wordId: String, lesson: Int): Sentence? =
        (sentencesByLesson[lesson] ?: emptyList()).firstOrNull { it.headword && it.tokens.joinToString(" ") == wordId }

    /** Real usage sentences containing [wordId] in [lesson], suitable for exercises. */
    fun usageSentences(wordId: String, lesson: Int, limit: Int = 4): List<Sentence> =
        (sentencesByLesson[lesson] ?: emptyList())
            .filter { !it.headword && it.words.contains(wordId) && it.tokens.size in 2..12 }
            .take(limit)

    /** Any usable sentence containing [wordId] (fallback across lessons). */
    fun anyUsageSentence(wordId: String, limitLesson: Int? = null): Sentence? {
        val list = sentencesByWord[wordId] ?: return null
        return list.firstOrNull { !it.headword && it.tokens.size in 2..12 && (limitLesson == null || it.lesson <= limitLesson) }
    }

    fun lessonNeighbours(lesson: Int): List<Sentence> =
        (sentencesByLesson[max(1, lesson - 1)] ?: emptyList()) +
            (sentencesByLesson[lesson] ?: emptyList()) +
            (sentencesByLesson[lesson + 1] ?: emptyList())

    // ------------------------------------------------------------ distractors

    /**
     * Near-miss choices: rather than a random sentence from the lesson, rank
     * candidates by how similar their Balinese form AND English wording are to
     * the target.  The result is a set like "You and I are here." /
     * "You and I are there." / "He and I are here." — same shape, one word
     * different — instead of obviously unrelated translations.
     */
    fun distractorEnglish(s: Sentence, n: Int): List<String> {
        val variants = variantKeys(s)
        val pool = sentences.filter {
            it.id != s.id && it.en != s.en && !it.headword &&
                it.en.length in 4..70 && it.tokens.size in 2..12 &&
                TextMatch.normalize(it.bal) !in variants
        }
        return rankDistractors(s, pool, toEnglish = true, n = n).map { it.en }
    }

    fun distractorBalinese(s: Sentence, n: Int): List<String> {
        val variants = variantKeys(s)
        val pool = sentences.filter {
            it.id != s.id && it.bal != s.bal && it.en != s.en && !it.headword &&
                it.tokens.size in 2..9 && TextMatch.normalize(it.bal) !in variants
        }
        return rankDistractors(s, pool, toEnglish = false, n = n).map { it.bal }
    }

    /** Normalized canonical sentence plus every register/mix variant of it. */
    private fun variantKeys(s: Sentence): Set<String> {
        val out = HashSet<String>()
        out += TextMatch.normalize(s.bal)
        for (variant in variantSentences(s)) out += TextMatch.normalize(variant)
        return out
    }

    private fun rankDistractors(s: Sentence, pool: List<Sentence>, toEnglish: Boolean, n: Int): List<Sentence> {
        val scored = pool.map { c ->
            val balSim = TextMatch.sentenceSimilarity(s.bal, c.bal)
            val enSim = TextMatch.sentenceSimilarity(s.en, c.en)
            val lenSim = 1.0 - abs(c.tokens.size - s.tokens.size).coerceAtMost(6) / 6.0
            val sameLesson = if (c.lesson == s.lesson) 0.08 else 0.0
            val score = if (toEnglish) {
                enSim * 0.50 + balSim * 0.34 + lenSim * 0.08 + sameLesson
            } else {
                balSim * 0.60 + enSim * 0.24 + lenSim * 0.08 + sameLesson
            }
            c to score
        }.sortedByDescending { it.second }
        val out = ArrayList<Sentence>(n)
        val seen = HashSet<String>()
        for ((c, _) in scored) {
            val key = if (toEnglish) c.en else c.bal
            if (!seen.add(key)) continue
            out += c
            if (out.size >= n) break
        }
        return out
    }

    /**
     * A single extra tile for the word bank, never a same-meaning sibling of a
     * word already in the sentence: the bank must not offer `cai` next to `nyai`
     * ("you"), `sing` next to `tusing` ("no"), `niki` next to `ene` ("this") …
     * Picks a single-word entry of the lesson with a different meaning, falling
     * back to earlier lessons when the lesson pool has none.
     */
    fun distractorToken(s: Sentence): String? {
        val used = s.bal.lowercase().split(" ")
            .map { it.trim('.', ',', '!', '?', ';', ':') }
            .filter { it.isNotEmpty() }
            .toSet()
        val sentenceGlosses = s.words.mapNotNull { words[it]?.gloss }
        fun clashes(id: String): Boolean {
            val e = words[id] ?: return true
            if (e.id.contains(' ')) return true
            if (id.lowercase() in used) return true
            return sentenceGlosses.any { glossOverlap(e.gloss, it) }
        }
        val sameLesson = (sentencesByLesson[s.lesson] ?: emptyList())
            .flatMap { it.words }
            .distinct()
            .filter { !clashes(it) }
        if (sameLesson.isNotEmpty()) return sameLesson.randomOrNull()
        return words.values
            .filter { it.firstLesson <= s.lesson && !clashes(it.id) }
            .map { it.id }
            .randomOrNull()
    }

    /** Wrong options for a fill-blank on [target] (single-token word ids). */
    fun wordDistractors(target: String, s: Sentence, n: Int): List<String> {
        val sameLesson = (sentencesByLesson[s.lesson] ?: emptyList()).flatMap { it.words }.distinct()
        var pool = sameLesson.filter { it != target && !it.contains(' ') }
        if (pool.size < n) {
            pool = (pool + words.keys.filter { !it.contains(' ') && it != target }).distinct()
        }
        return pool.shuffled().take(n)
    }

    // ------------------------------------------------- vocabulary multiple choice

    /** Candidate distractor entries: same lesson first, then earlier lessons. */
    private fun vocabPool(target: String, lesson: Int): Pair<List<WordEntry>, List<WordEntry>> {
        val pool = words.values.filter { it.id != target && it.firstLesson <= lesson }
        return pool.filter { it.firstLesson == lesson } to pool.filter { it.firstLesson < lesson }
    }

    /** True when two glosses share a meaning-bearing word (yes / "yes (true)"). */
    private fun glossOverlap(a: String, b: String): Boolean {
        val ta = a.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
        val tb = b.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
        return ta.any { it in tb }
    }

    /** Wrong Balinese words for a word-level multiple choice on [target]. */
    fun vocabWordDistractors(target: String, lesson: Int, n: Int): List<String> {
        val (same, earlier) = vocabPool(target, lesson)
        val targetGloss = gloss(target)
        val correct = display(target)
        val out = ArrayList<String>(n)
        for (e in same.shuffled() + earlier.shuffled()) {
            if (glossOverlap(e.gloss, targetGloss)) continue
            val d = display(e.id)
            if (d != correct && d !in out) out += d
            if (out.size >= n) break
        }
        return out
    }

    /** Wrong English glosses for a word-level multiple choice on [target]. */
    fun vocabGlossDistractors(target: String, lesson: Int, n: Int): List<String> {
        val (same, earlier) = vocabPool(target, lesson)
        val targetGloss = gloss(target)
        val correct = shortGloss(targetGloss, 30)
        val out = ArrayList<String>(n)
        for (e in same.shuffled() + earlier.shuffled()) {
            if (glossOverlap(e.gloss, targetGloss)) continue
            val d = shortGloss(e.gloss, 30)
            if (!d.equals(correct, ignoreCase = true) && d !in out) out += d
            if (out.size >= n) break
        }
        return out
    }

    // ------------------------------------------------- register variants

    /**
     * Sentences that mean the same as [s] with one or more content words swapped
     * for a register sibling (dini↔driki "here", nanging↔sakewanten "but", …).
     * Combinations are emitted by increasing number of swaps, so full-Alus,
     * full-Andap and mixed answers are all accepted by the typing exercise.
     */
    fun variantSentences(s: Sentence, limit: Int = 128): List<String> {
        val toks = s.bal.split(" ").filter { it.isNotBlank() }
        if (toks.size < 2) return emptyList()
        val options = ArrayList<List<String>>(toks.size)
        for (tok in toks) {
            val e = words[tokenKey(tok)]
            if (e == null || e.id.contains(' ')) { options += listOf(tok); continue }
            val peers = words.values
                .filter { it.id != e.id && !it.id.contains(' ') && glossHead(it.gloss) == glossHead(e.gloss) }
                .map { it.id }
            options += if (peers.isEmpty()) listOf(tok) else listOf(tok) + peers
        }
        val swapPos = toks.indices.filter { options[it].size > 1 }
        if (swapPos.isEmpty()) return emptyList()
        val out = ArrayList<String>(limit)
        for (level in 1..swapPos.size) {
            for (chosen in choose(swapPos, level)) {
                emitVariants(toks, options, chosen, out, limit)
                if (out.size >= limit) return out
            }
        }
        return out
    }

    /**
     * Register siblings of a single surface [token] that the learner has already
     * been taught (firstLesson <= [maxLesson]), ordered by when they were taught.
     * Used by the word-bank exercise so a mixed-register build (dini … sakewanten
     * … driki, for example) is accepted just like it is in typing.
     */
    fun registerAlternatives(token: String, maxLesson: Int): List<String> {
        val key = tokenKey(token)
        if (key.isEmpty()) return emptyList()
        val entry = words[key] ?: return emptyList()
        if (entry.id.contains(' ')) return emptyList()
        val head = glossHead(entry.gloss)
        if (head.isEmpty()) return emptyList()
        return words.values
            .filter {
                it.id != entry.id && !it.id.contains(' ') &&
                    it.firstLesson <= maxLesson && glossHead(it.gloss) == head
            }
            .sortedWith(compareBy({ it.firstLesson }, { it.id }))
            .map { it.id }
            .distinct()
    }

    /** Normalized key of a surface token (lowercase, accents folded, punctuation dropped). */
    private fun tokenKey(tok: String): String {
        val sb = StringBuilder()
        for (c in tok.lowercase()) {
            val cc = when (c) {
                'é' -> 'e'
                'è' -> 'e'
                else -> c
            }
            if (cc.isLetterOrDigit()) sb.append(cc)
        }
        return sb.toString()
    }

    private fun emitVariants(
        toks: List<String>,
        options: List<List<String>>,
        chosen: List<Int>,
        out: MutableList<String>,
        limit: Int
    ) {
        val alts = chosen.map { options[it].drop(1) }   // drop the original token
        val idx = IntArray(chosen.size)
        val arr = toks.toMutableList()
        while (true) {
            chosen.forEachIndexed { j, pos -> arr[pos] = alts[j][idx[j]] }
            out += arr.joinToString(" ")
            if (out.size >= limit) return
            var j = chosen.size - 1
            while (j >= 0) {
                idx[j]++
                if (idx[j] < alts[j].size) break
                idx[j] = 0
                j--
            }
            if (j < 0) return
        }
    }

    private fun <T> choose(list: List<T>, k: Int): List<List<T>> {
        val out = ArrayList<List<T>>()
        fun rec(start: Int, acc: MutableList<T>) {
            if (acc.size == k) { out += acc.toList(); return }
            for (i in start until list.size) {
                acc += list[i]
                rec(i + 1, acc)
                acc.removeAt(acc.size - 1)
            }
        }
        rec(0, mutableListOf())
        return out
    }

    // ---------------------------------------------------------------- display

    private val properNouns = setOf(
        "bali", "jawa", "galungan", "kuningan", "nyepi", "saraswati", "siwaratri",
        "om swastiastu", "basa bali", "gunung agung", "idul"
    )

    /** Pretty display of a word id (capitalizes proper nouns, first letter of sentences). */
    fun display(wordId: String): String {
        val parts = wordId.split(" ")
        return if (wordId in properNouns) {
            parts.joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
        } else wordId
    }

    fun shortGloss(gloss: String, max: Int = 30): String {
        val g = gloss.trim()
        if (g.length <= max) return g
        val cut = g.take(max)
        val sp = cut.lastIndexOf(' ')
        return (if (sp > 12) cut.take(sp) else cut) + "…"
    }

    fun random(): Random = Random.Default
}
