package com.basabali.app.ui.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basabali.app.AppGraph
import com.basabali.app.audio.AsrManager
import com.basabali.app.audio.SoundFx
import com.basabali.app.audio.TtsManager
import com.basabali.app.data.ContentRepository
import com.basabali.app.engine.DictationEx
import com.basabali.app.engine.FillBlankEx
import com.basabali.app.engine.ListenTapEx
import com.basabali.app.engine.MatchPairsEx
import com.basabali.app.engine.MultipleChoiceEx
import com.basabali.app.engine.SpeakEx
import com.basabali.app.engine.TypeTranslationEx
import com.basabali.app.engine.WordBankEx
import com.basabali.app.engine.WordCardEx
import com.basabali.app.ui.AvatarArt
import com.basabali.app.ui.BudayaAvatar
import com.basabali.app.ui.ShareSentenceButton
import com.basabali.app.ui.theme.CreamCard
import com.basabali.app.ui.theme.Gold
import com.basabali.app.ui.theme.GreenOk
import com.basabali.app.ui.theme.Ink
import com.basabali.app.ui.theme.InkSoft
import com.basabali.app.ui.theme.RedBad

/** Short Balinese lines the mascot says while the learner works (all attested in the course). */
private val IDLE_LINES = listOf("Ngiring!", "Rahajeng!", "Ngiring malajah Basa Bali!")

@Composable
fun LessonScreen(
    runner: SessionRunner,
    content: ContentRepository,
    tts: TtsManager?,
    asr: AsrManager?,
    onExit: () -> Unit
) {
    val ex = runner.current
    // per-exercise interaction state, reset whenever the queue index changes
    var pickedChoice by remember(runner.index) { mutableStateOf<String?>(null) }
    var bankSelected by remember(runner.index) { mutableStateOf<List<Int>>(emptyList()) }
    var typedText by remember(runner.index) { mutableStateOf("") }
    // once skipped, the speaking steps of this subunit become non-speaking exercises
    var skipSpeaking by remember(runner) { mutableStateOf(false) }

    // motion: the new exercise settles in, the bar glides, low hearts pulse
    val entryShift = remember { Animatable(0f) }
    LaunchedEffect(runner.index) {
        entryShift.snapTo(16f)
        entryShift.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
    }
    val animProgress by animateFloatAsState(runner.progress, tween(320), label = "progress")
    val heartTransition = rememberInfiniteTransition(label = "hearts")
    val lowHeartScale by heartTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heart-pulse"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // ------------------------------------------------ top bar
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { runner.persist(); onExit() }) {
                Icon(Icons.Default.Close, contentDescription = "Exit lesson", tint = InkSoft)
            }
            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = Gold,
                trackColor = Gold.copy(alpha = 0.15f)
            )
            val hearts = runner.hearts
            Text(
                "♥ $hearts",
                color = RedBad,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.graphicsLayer {
                    val s = if (hearts <= 2) lowHeartScale else 1f
                    scaleX = s; scaleY = s
                }
            )
        }

        // ------------------------------------------------ mascot line (moves with the mood)
        val fb = runner.feedback
        val mood = when {
            fb == null -> AvatarArt.IDLE
            fb.ok -> AvatarArt.HAPPY
            else -> AvatarArt.ENCOURAGING
        }
        val line = when {
            fb == null -> IDLE_LINES[runner.index % IDLE_LINES.size]
            fb.ok -> "Patut!"
            else -> "Sampunang sebet!"
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BudayaAvatar(mood, avatarSize = 52.dp)
            Spacer(Modifier.width(8.dp))
            Surface(
                color = CreamCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x1A000000))
            ) {
                Text(
                    line,
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    fontSize = 13.sp,
                    color = Ink,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ------------------------------------------------ exercise area
        Box(
            Modifier
                .weight(1f)
                .graphicsLayer {
                    translationY = entryShift.value
                    alpha = (1f - entryShift.value / 40f).coerceIn(0f, 1f)
                }
        ) {
            when (val e = ex) {
                null -> Unit

                is WordCardEx -> WordCardView(e, content, tts) {
                    runner.onCardSeen(e)
                    runner.continueNext()
                }

                is MatchPairsEx -> MatchPairsView(e, content, tts) { errs ->
                    runner.onMatchPairsDone(e, errs)
                    runner.continueNext()
                }

                is MultipleChoiceEx -> MultipleChoiceView(
                    ex = e,
                    content = content,
                    tts = tts,
                    enabled = runner.feedback == null && pickedChoice == null,
                    picked = pickedChoice
                ) { choice ->
                    pickedChoice = choice
                    runner.submitChoice(e, choice)
                }

                is ListenTapEx -> ListenTapView(
                    ex = e,
                    tts = tts,
                    enabled = runner.feedback == null && pickedChoice == null,
                    picked = pickedChoice
                ) { choice ->
                    pickedChoice = choice
                    runner.submitListen(e, choice)
                }

                is FillBlankEx -> FillBlankView(
                    ex = e,
                    content = content,
                    tts = tts,
                    enabled = runner.feedback == null && pickedChoice == null,
                    picked = pickedChoice
                ) { choice ->
                    pickedChoice = choice
                    runner.submitBlank(e, choice)
                }

                is WordBankEx -> WordBankView(
                    ex = e,
                    selected = bankSelected,
                    enabled = runner.feedback == null,
                    onPickBank = { i -> bankSelected = bankSelected + i },
                    onRemoveAnswer = { i -> bankSelected = bankSelected - i }
                )

                is DictationEx -> DictationView(
                    ex = e,
                    tts = tts,
                    text = typedText,
                    onText = { typedText = it },
                    enabled = runner.feedback == null
                )

                is TypeTranslationEx -> TypeTranslationView(
                    ex = e,
                    text = typedText,
                    onText = { typedText = it },
                    enabled = runner.feedback == null
                )

                is SpeakEx -> if (skipSpeaking) {
                    // safety net: a speaking step the replacement could not swap out
                    LaunchedEffect(e) { runner.continueNext() }
                } else SpeakView(
                    ex = e,
                    tts = tts,
                    asr = asr,
                    enabled = runner.feedback == null,
                    onResult = { res ->
                        // ignore stale results: the learner may have skipped this step
                        if (res.isNotEmpty() && !skipSpeaking && runner.current === e) runner.submitSpeak(e, res)
                    },
                    onSkip = {
                        skipSpeaking = true
                        // keep the exercise count: swap every speaking step of this
                        // subunit for an equivalent word-bank / fill / recognition item
                        runner.replaceSpeaking()
                    }
                )
            }
        }

        // ------------------------------------------------ bottom: check / feedback
        // unique answer sounds: rising chime on correct, low buzz on wrong
        LaunchedEffect(fb) {
            if (fb != null) AppGraph.fx?.play(if (fb.ok) SoundFx.Sound.CORRECT else SoundFx.Sound.WRONG)
        }
        val bank = ex as? WordBankEx
        val dict = ex as? DictationEx
        val typ = ex as? TypeTranslationEx

        if (fb == null && bank != null) {
            Button(
                onClick = {
                    val built = bankSelected.map { bank.bankTokens[it] }
                    runner.submitBank(bank, built)
                },
                enabled = bankSelected.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Check", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (fb == null && (dict != null || typ != null)) {
            Button(
                onClick = {
                    if (dict != null) runner.submitTyped(dict, typedText, dict.answerNorm, dict.answerDisplay)
                    else if (typ != null) runner.submitTyped(typ, typedText, typ.answerNorm, typ.answerDisplay, typ.altNorm)
                },
                enabled = typedText.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Check", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // feedback banner slides in (kept composed while it fades back out)
        val lastFb = remember { arrayOfNulls<SessionRunner.Feedback>(1) }
        if (fb != null) lastFb[0] = fb
        val shownFb = fb ?: lastFb[0]
        AnimatedVisibility(
            visible = fb != null,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 4 },
            exit = fadeOut(tween(120))
        ) {
            val f = shownFb
            if (f != null) {
                val c = if (f.ok) GreenOk else RedBad
                val sentence = ex?.sentenceId?.let { content.sentencesById[it] }
                // Balinese answer/sentence shown on every result (correct included)
                // so it can be tapped, replayed, and shared.
                val balAnswer: String? = when (val e0 = ex) {
                    is WordBankEx -> e0.answerDisplay
                    is DictationEx -> e0.answerDisplay
                    is TypeTranslationEx -> e0.answerDisplay
                    is SpeakEx -> e0.target
                    is MultipleChoiceEx -> if (!e0.toEnglish) e0.correct else sentence?.bal
                    is ListenTapEx -> if (!e0.toEnglish) e0.correct else sentence?.bal
                    is FillBlankEx -> sentence?.bal
                    else -> null
                }
                val showBal = balAnswer != null
                val shareRegister = sentence?.register ?: when (val e0 = ex) {
                    is WordBankEx -> e0.register
                    is DictationEx -> e0.register
                    is TypeTranslationEx -> e0.register
                    is SpeakEx -> e0.register
                    is MultipleChoiceEx -> e0.register
                    is ListenTapEx -> e0.register
                    is FillBlankEx -> e0.register
                    else -> "mider"
                }
                // a Balinese answer shown in the feedback announces itself too
                LaunchedEffect(fb, balAnswer) {
                    if (fb != null && showBal && balAnswer != null) tts?.speak(balAnswer)
                }
                Column {
                    Surface(
                        color = c.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, c.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                if (f.ok) "Sadu! Correct ✨" else "Not quite…",
                                color = c,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (f.note != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(f.note, color = InkSoft, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                            }
                            if (showBal && balAnswer != null) {
                                Spacer(Modifier.height(6.dp))
                                Text(if (f.ok) "Sentence:" else "Answer:", fontSize = 12.sp, color = InkSoft)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f)) {
                                        TappableBalinese(balAnswer, content, null,
                                            fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                    }
                                    if (tts != null) SpeakerButton(balAnswer, tts, size = 36)
                                    Spacer(Modifier.width(6.dp))
                                    ShareSentenceButton(
                                        bal = balAnswer,
                                        en = sentence?.en ?: f.correctText ?: "",
                                        register = shareRegister,
                                        tint = c,
                                        size = 34
                                    )
                                }
                                if (!f.ok && f.correctText != null && f.correctText != balAnswer) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Correct answer:  ${f.correctText}", color = Ink, fontSize = 14.sp)
                                }
                            } else if (!f.ok && f.correctText != null) {
                                Spacer(Modifier.height(4.dp))
                                Text("Answer:  ${f.correctText}", color = Ink, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { runner.continueNext() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}
