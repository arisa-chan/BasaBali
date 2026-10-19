package com.basabali.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basabali.app.audio.TtsManager
import com.basabali.app.engine.SessionBuilder
import com.basabali.app.ui.lesson.OptionCard
import com.basabali.app.ui.theme.CreamCard
import com.basabali.app.ui.theme.Gold
import com.basabali.app.ui.theme.GreenOk
import com.basabali.app.ui.theme.Ink
import com.basabali.app.ui.theme.InkSoft
import com.basabali.app.ui.theme.RedBad

/**
 * Onboarding placement test: 14 milestone probes across the course.
 * The frontier is the highest milestone passed with ≥60% cumulative accuracy;
 * all chunks up to it are marked done and their words seeded mid-strength.
 */
@Composable
fun PlacementScreen(
    builder: SessionBuilder,
    tts: TtsManager?,
    onFinished: (frontierLesson: Int) -> Unit,
    onSkip: () -> Unit
) {
    val questions = remember { builder.placementQuestions() }
    var phase by remember { mutableStateOf(if (questions.isEmpty()) "result" else "intro") }
    var idx by remember { mutableStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    val results = remember { ArrayList<Pair<Int, Boolean>>() }
    var frontier by remember { mutableStateOf(0) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(20.dp).verticalScroll(rememberScrollState())
    ) {
        when (phase) {
            "intro" -> {
                Spacer(Modifier.height(40.dp))
                Text("🌺", fontSize = 52.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Suksma ring Bali!", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Already know some Balinese? Take a short ${questions.size}-question placement test " +
                        "to skip ahead — or start from the very beginning.",
                    fontSize = 14.sp, color = InkSoft, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { phase = "quiz" },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start placement test", fontSize = 16.sp) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSkip,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start from Lesson 1") }
            }

            "quiz" -> {
                val q = questions[idx]
                // Balinese prompt appears → speak it
                LaunchedEffect(idx) { tts?.speak(q.ex.prompt) }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { (idx.toFloat()) / questions.size.toFloat() },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Gold, trackColor = Gold.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.padding(6.dp))
                    Text("${idx + 1}/${questions.size}", fontSize = 13.sp, color = InkSoft)
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = CreamCard, shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x1A000000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Milestone · Lesson ${q.lesson}", fontSize = 12.sp, color = InkSoft)
                        Spacer(Modifier.height(6.dp))
                        Text(q.ex.prompt, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    }
                }
                Spacer(Modifier.height(18.dp))
                for (opt in q.ex.options) {
                    val ex = q.ex
                    val state = when {
                        picked == null -> "idle"
                        opt == ex.correct -> "right"
                        opt == picked -> "wrong"
                        else -> "dim"
                    }
                    OptionCard(text = opt, state = state, enabled = picked == null) { picked = opt }
                    Spacer(Modifier.height(10.dp))
                }
                if (picked != null) {
                    val ok = picked == q.ex.correct
                    Surface(
                        color = (if (ok) GreenOk else RedBad).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(if (ok) "Correct ✨" else "Answer: ${q.ex.correct}",
                                color = if (ok) GreenOk else RedBad, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            results += q.lesson to ok
                            picked = null
                            if (idx + 1 < questions.size) idx++
                            else {
                                frontier = builder.placementFrontier(results.toList())
                                builder.applyPlacement(frontier)
                                phase = "result"
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Continue", fontSize = 16.sp) }
                }
            }

            "result" -> {
                Spacer(Modifier.height(50.dp))
                Text(if (frontier > 0) "🎓" else "🌱", fontSize = 56.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Text(
                    if (frontier > 0) "Great! You'll start after Lesson $frontier."
                    else "Let's begin at the very beginning!",
                    fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (frontier > 0) "Earlier chunks are marked complete and their words will resurface in your spaced review."
                    else "Lesson 1: greetings, yes/no, and your first Balinese sentences.",
                    fontSize = 13.sp, color = InkSoft, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(34.dp))
                Button(
                    onClick = { onFinished(frontier) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start learning", fontSize = 16.sp) }
            }
        }
    }
}
