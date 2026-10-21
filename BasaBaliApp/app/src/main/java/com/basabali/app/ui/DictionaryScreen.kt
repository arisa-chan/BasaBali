package com.basabali.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basabali.app.audio.TtsManager
import com.basabali.app.data.ContentRepository
import com.basabali.app.data.DictionaryEntry
import com.basabali.app.data.Sentence
import com.basabali.app.data.UserStore
import com.basabali.app.engine.SessionBuilder
import com.basabali.app.engine.Srs
import com.basabali.app.ui.lesson.RegisterChip
import com.basabali.app.ui.lesson.SpeakerButton
import com.basabali.app.ui.lesson.TappableBalinese
import com.basabali.app.ui.theme.CreamCard
import com.basabali.app.ui.theme.Gold
import com.basabali.app.ui.theme.GrayLock
import com.basabali.app.ui.theme.GreenOk
import com.basabali.app.ui.theme.Ink
import com.basabali.app.ui.theme.InkSoft
import com.basabali.app.ui.theme.RedBad
import com.basabali.app.ui.theme.Teal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val MAX_RESULTS = 300

/**
 * Dictionary module (replaces the old Review tab).
 *
 * Search is local and covers the whole bundled BASAbali / BASAibu dictionary.
 * Course entries additionally show their lesson number, whether the learner
 * has met them, their current SRS mastery, and the tagged exercise sentences
 * from the Lessons database.  The old review actions stay here, but their
 * sessions still draw exclusively from the Lesson sentence bank.
 */
@Composable
fun DictionaryScreen(
    content: ContentRepository,
    store: UserStore,
    builder: SessionBuilder,
    tts: TtsManager?,
    snack: SnackbarHostState,
    scope: CoroutineScope,
    onStartSession: (SessionKind) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val now = remember { System.currentTimeMillis() }
    val dueCount = remember(store.doneChunks.size, store.xp) { builder.dueCount() }
    val mistakeCount = store.mistakes.size
    val taught = store.taughtWordCount()

    val results = remember(query, content.dictionary.size) {
        filterDictionary(content, query.lowercase().trim(), MAX_RESULTS)
    }
    val courseEntries = remember(content.dictionary.size) {
        content.dictionary.filter { it.lesson > 0 }.take(MAX_RESULTS)
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Dictionary", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(2.dp))
            Text(
                "Search Balinese or English across the BasaBali course and the BASAbali Wiki dictionary. " +
                    "Course words also show their lesson, SRS mastery and exercise sentences.",
                fontSize = 12.sp, color = InkSoft
            )
        }

        item {
            DictionaryActionCard(
                emoji = "🧠", accent = Teal,
                title = "Review encountered words",
                sub = if (dueCount > 0) "$dueCount word/pattern reviews due — the SRS picks the sentences"
                      else "Nothing formally due — rehearse your weakest lesson words",
                active = taught > 0
            ) {
                if (taught > 0) onStartSession(SessionKind.Review)
                else scope.launch { snack.showSnackbar("Finish a lesson step first — then your words show up here.") }
            }
        }
        item {
            DictionaryActionCard(
                emoji = "❗", accent = RedBad,
                title = "Review past mistakes",
                sub = if (mistakeCount > 0) "$mistakeCount lesson sentence(s) you missed"
                      else "No open mistakes — anything you miss lands here",
                active = mistakeCount > 0
            ) {
                if (mistakeCount > 0) onStartSession(SessionKind.Mistakes)
                else scope.launch { snack.showSnackbar("No mistakes logged. Nice work!") }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Search Balinese or English…", color = InkSoft) },
                leadingIcon = { Text("🔎", fontSize = 18.sp) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(onClick = { query = "" }) { Text("Clear", fontSize = 12.sp) }
                    }
                }
            )
        }

        if (query.isBlank()) {
            item {
                Text(
                    "Lesson vocabulary (${content.words.size} entries)",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkSoft,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(courseEntries, key = { "course-${it.id}" }) { entry ->
                DictionaryCard(entry, content, store, tts, now)
            }
            if (content.dictionary.count { it.lesson > 0 } > MAX_RESULTS) {
                item {
                    Text("Showing the first $MAX_RESULTS course entries — type a search to narrow the list.",
                        fontSize = 11.sp, color = InkSoft)
                }
            }
        } else {
            item {
                Text(
                    if (results.isEmpty()) "No entry found for “$query”."
                    else "${results.size} result(s) — tap a card for examples and exercise sentences.",
                    fontSize = 12.sp, color = InkSoft
                )
            }
            items(results, key = { "hit-${it.id}" }) { entry ->
                DictionaryCard(entry, content, store, tts, now)
            }
        }

        item {
            Text(
                "Dictionary source: BASAbali / BASAibu Wiki (dictionary.basabali.org), " +
                    "cross-checked against Balai Bahasa Bali. Exercise sentences come from the BasaBali lessons.",
                fontSize = 10.sp, color = GrayLock,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun foldSearch(s: String): String =
    s.lowercase().replace('é', 'e').replace('è', 'e')

private fun filterDictionary(
    content: ContentRepository,
    q: String,
    limit: Int
): List<DictionaryEntry> {
    if (q.isBlank()) return emptyList()
    val needle = foldSearch(q)
    val scored = ArrayList<Pair<DictionaryEntry, Int>>(64)
    for (e in content.dictionary) {
        val head = foldSearch(e.headword)
        val en = foldSearch(e.english)
        val definitions = foldSearch(e.definitions.joinToString(" "))
        val enWordHit = en.split(Regex("[^a-z0-9]+")).any { it == needle }
        val rank = when {
            head == needle -> 0
            en == needle -> 1
            en.startsWith("$needle;") || en.startsWith("$needle,") || enWordHit -> 2
            head.startsWith(needle) -> 3
            head.contains(needle) -> 4
            en.contains(needle) || definitions.contains(needle) -> 5
            else -> continue
        }
        scored += e to rank
    }
    return scored
        .sortedWith(compareBy({ it.second }, { if (it.first.lesson > 0) 0 else 1 }, { it.first.headword.lowercase() }))
        .take(limit)
        .map { it.first }
}

@Composable
private fun DictionaryActionCard(
    emoji: String,
    accent: Color,
    title: String,
    sub: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) accent.copy(alpha = 0.10f) else CreamCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, if (active) accent.copy(alpha = 0.45f) else Color(0x14000000)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(sub, fontSize = 11.5.sp, color = InkSoft)
            }
            if (active) Text("▶", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DictionaryCard(
    entry: DictionaryEntry,
    content: ContentRepository,
    store: UserStore,
    tts: TtsManager?,
    now: Long
) {
    var expanded by remember(entry.id) { mutableStateOf(false) }
    val inCourse = entry.lesson > 0
    val srs = store.words[entry.id]
    val encountered = (srs?.lastReview ?: 0L) > 0L
    val mastery = Srs.masteryLabel(srs, now)
    val masteryDetail = Srs.masteryDetail(srs, now)
    val fraction = Srs.masteryFraction(srs, now)
    val exerciseSentences = remember(entry.id) {
        (content.sentencesByWord[entry.id] ?: emptyList())
            .filter { !it.headword && it.tokens.size in 2..14 }
            .take(4)
    }

    Surface(
        color = CreamCard, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (inCourse) Gold.copy(alpha = 0.35f) else Color(0x14000000)),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.headword, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RegisterChip(registerKey(entry.register))
                        Surface(
                            color = Teal.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Teal.copy(alpha = 0.4f))
                        ) {
                            Text(if (entry.pos.isBlank()) "pos not listed" else entry.pos,
                                Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                fontSize = 10.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                SpeakerButton(entry.headword, tts, size = 38)
            }
            Spacer(Modifier.height(8.dp))
            Text(entry.english, fontSize = 14.sp, color = Ink)
            Text(if (entry.register.isBlank()) "Register not listed" else entry.register,
                fontSize = 10.sp, color = InkSoft)
            Spacer(Modifier.height(7.dp))
            if (inCourse) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lesson ${entry.lesson}", fontSize = 11.sp, color = Gold,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (encountered) "Encountered · $mastery"
                        else "Not encountered yet",
                        fontSize = 11.sp,
                        color = if (encountered) GreenOk else InkSoft
                    )
                }
                if (encountered) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                        color = if (fraction >= 0.6f) GreenOk else Gold,
                        trackColor = Gold.copy(alpha = 0.14f)
                    )
                    Text(masteryDetail, fontSize = 9.sp, color = InkSoft)
                }
            } else {
                Text("Not part of the Lessons database", fontSize = 11.sp, color = RedBad)
            }
            if (!expanded) {
                Spacer(Modifier.height(5.dp))
                Text("Tap for dictionary examples${if (inCourse) " and exercise sentences" else ""} ▾",
                    fontSize = 10.sp, color = Teal)
            } else {
                if (entry.definitions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Meaning", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSoft)
                    entry.definitions.forEach { d ->
                        Text("• $d", fontSize = 12.sp, color = Ink)
                    }
                }
                if (entry.examples.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Dictionary examples", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSoft)
                    entry.examples.forEach { ex ->
                        Surface(
                            color = Color(0x0F0F766E), shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                        ) {
                            Column(Modifier.padding(9.dp)) {
                                Text("Dictionary example", fontSize = 9.sp, color = Teal, fontWeight = FontWeight.Bold)
                                Text(ex.bal, fontSize = 13.sp, color = Ink, fontWeight = FontWeight.Medium)
                                if (ex.en.isNotBlank()) {
                                    Text(ex.en, fontSize = 11.sp, color = InkSoft, fontStyle = FontStyle.Italic)
                                }
                            }
                        }
                    }
                }
                if (inCourse && exerciseSentences.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Exercise sentences from the Lessons database",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = InkSoft)
                    exerciseSentences.forEach { s ->
                        ExerciseSentenceRow(s, content, store, tts, now)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSentenceRow(
    s: Sentence,
    content: ContentRepository,
    store: UserStore,
    tts: TtsManager?,
    now: Long
) {
    val seen = store.seenSentences.containsKey(s.id)
    val labels = s.words.map { Srs.masteryLabel(store.words[it], now) }
    val rank = mapOf("Mastered" to 5, "Strong" to 4, "Familiar" to 3, "Learning" to 2, "Not encountered" to 1)
    val weakest = labels.minByOrNull { rank[it] ?: 0 } ?: "Not encountered"
    Surface(
        color = Color(0x0FD99A2B), shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
    ) {
        Column(Modifier.padding(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Exercise sentence · Lesson ${s.lesson}", fontSize = 9.sp,
                    color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (seen) Text("Encountered · $weakest", fontSize = 9.sp, color = GreenOk)
                else Text("Not encountered", fontSize = 9.sp, color = InkSoft)
            }
            Spacer(Modifier.height(3.dp))
            TappableBalinese(s.bal, content, null, 13.sp, FontWeight.Normal)
            Text(s.en, fontSize = 10.5.sp, color = InkSoft, fontStyle = FontStyle.Italic)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpeakerButton(s.bal, tts, size = 28)
                Spacer(Modifier.width(6.dp))
                Text("${s.words.size} word tags · ${s.register}", fontSize = 9.sp, color = GrayLock)
            }
        }
    }
}

private fun registerKey(label: String): String {
    val low = label.lowercase()
    return when {
        "singgih" in low -> "alus"
        "sor" in low -> "alus"
        "madya" in low -> "alus"
        "alus" in low -> "alus"
        "andap" in low -> "andap"
        "kasar" in low -> "andap"
        "mider" in low -> "mider"
        else -> "mider"
    }
}
