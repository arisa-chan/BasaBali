package com.basabali.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basabali.app.audio.TtsManager
import com.basabali.app.data.ContentRepository
import com.basabali.app.data.Story
import com.basabali.app.data.UserStore
import com.basabali.app.ui.lesson.SpeakerButton
import com.basabali.app.ui.lesson.TappableBalinese
import com.basabali.app.ui.theme.CreamCard
import com.basabali.app.ui.theme.Gold
import com.basabali.app.ui.theme.GreenOk
import com.basabali.app.ui.theme.Ink
import com.basabali.app.ui.theme.InkSoft
import com.basabali.app.ui.theme.RedBad

/**
 * E11 — interactive story: read/listen line by line, then three English
 * comprehension taps. Finishing grants XP and registers light SRS exposure
 * for every word in the story (reading = exposure; the quiz = retrieval).
 */
@Composable
fun StoryScreen(
    story: Story,
    content: ContentRepository,
    store: UserStore,
    tts: TtsManager?,
    onDone: (xp: Int, correct: Int) -> Unit,
    onExit: () -> Unit
) {
    var lineIdx by remember { mutableStateOf(0) }
    var phase by remember { mutableStateOf("reading") }   // reading | quiz | done
    var qIdx by remember { mutableStateOf(0) }
    var qPicked by remember { mutableStateOf<Int?>(null) }
    var qCorrect by remember { mutableStateOf(0) }
    var bonusXp by remember { mutableStateOf(0) }

    val totalSteps = story.lines.size + story.questions.size

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, contentDescription = "Exit story", tint = InkSoft)
            }
            LinearProgressIndicator(
                progress = {
                    val done = if (phase == "reading") lineIdx else story.lines.size + qIdx
                    done.toFloat() / totalSteps.toFloat()
                },
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = Gold,
                trackColor = Gold.copy(alpha = 0.15f)
            )
            Text("📖", fontSize = 18.sp)
        }
        Spacer(Modifier.height(14.dp))

        when (phase) {
            "reading" -> {
                val line = story.lines[lineIdx]
                LaunchedEffect(lineIdx) { tts?.speak(line.bal) }
                Text(story.title, fontSize = 15.sp, color = InkSoft)
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = CreamCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0x1A000000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TappableBalinese(line.bal, content, null, fontSize = 20.sp,
                            arrangement = Arrangement.Center)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            SpeakerButton(line.bal, tts, size = 48)
                            SpeakerButton(line.bal, tts, slow = true, size = 48)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(line.en, fontSize = 14.sp, fontStyle = FontStyle.Italic, color = InkSoft,
                            textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (lineIdx + 1 < story.lines.size) lineIdx++
                        else phase = "quiz"
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(if (lineIdx + 1 < story.lines.size) "Next" else "Answer the questions →",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            "quiz" -> {
                val q = story.questions[qIdx]
                val qLine = story.lines[q.line.coerceIn(0, story.lines.lastIndex)]
                // re-read the line the question is about (Balinese appears → it speaks)
                LaunchedEffect(qIdx) { tts?.speak(qLine.bal) }
                Text(story.title, fontSize = 15.sp, color = InkSoft)
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = CreamCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x1A000000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        TappableBalinese(qLine.bal,
                            content, null, fontSize = 15.sp, fontWeight = FontWeight.Normal)
                        Spacer(Modifier.height(8.dp))
                        Text(q.prompt, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    }
                }
                Spacer(Modifier.height(18.dp))
                q.options.forEachIndexed { i, opt ->
                    val state = when {
                        qPicked == null -> "idle"
                        i == q.answer -> "right"
                        i == qPicked -> "wrong"
                        else -> "dim"
                    }
                    val bg = when (state) {
                        "right" -> GreenOk.copy(alpha = 0.16f)
                        "wrong" -> RedBad.copy(alpha = 0.16f)
                        "dim" -> CreamCard.copy(alpha = 0.55f)
                        else -> CreamCard
                    }
                    val border = when (state) {
                        "right" -> GreenOk
                        "wrong" -> RedBad
                        else -> Color(0x24000000)
                    }
                    Surface(
                        color = bg, shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, border),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            .then(Modifier.clickableIf(qPicked == null) {
                                qPicked = i
                                if (i == q.answer) qCorrect++
                            })
                    ) {
                        Text(opt, Modifier.padding(16.dp), fontSize = 16.sp, color = Ink)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (qPicked != null) {
                    Button(
                        onClick = {
                            qPicked = null
                            if (qIdx + 1 < story.questions.size) qIdx++
                            else {
                                phase = "done"
                                // exposure credit for every word/pattern in the story
                                val now = System.currentTimeMillis()
                                for (l in story.lines) {
                                    for (w in l.words) {
                                        val st = store.wordState(w)
                                        if (st.lastReview <= 0L) st.lastReview = now
                                    }
                                    for (p in l.patterns) {
                                        val st = store.patternState(p)
                                        if (st.lastReview <= 0L) st.lastReview = now
                                    }
                                }
                                store.completedStories += story.id
                                val xp = 15 + 5 * qCorrect
                                store.xp += xp
                                // Route story XP through recordSession so the league table,
                                // the weekly XP quest and the practice calendar all see it.
                                bonusXp = store.recordSession(xp, lessonsCompleted = 0, perfectAnswers = 0)
                                store.save()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) { Text("Continue", fontSize = 16.sp) }
                }
            }

            "done" -> {
                Spacer(Modifier.height(40.dp))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 56.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Story finished!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenOk)
                    Spacer(Modifier.height(6.dp))
                    Text("$qCorrect/${story.questions.size} correct · +${15 + 5 * qCorrect + bonusXp} XP",
                        fontSize = 15.sp, color = InkSoft)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onDone(15 + 5 * qCorrect + bonusXp, qCorrect) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) { Text("Back to path", fontSize = 16.sp) }
            }
        }
    }
}

private fun Modifier.clickableIf(enabled: Boolean, onClick: () -> Unit): Modifier =
    this.clickable(enabled = enabled, onClickLabel = null, onClick = onClick)
