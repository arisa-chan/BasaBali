package com.basabali.app.ui.lesson

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.basabali.app.AppGraph
import com.basabali.app.audio.AsrManager
import com.basabali.app.audio.SoundFx
import com.basabali.app.audio.TtsManager
import com.basabali.app.engine.DictationEx
import com.basabali.app.engine.ListenTapEx
import com.basabali.app.engine.SpeakEx
import com.basabali.app.engine.TypeTranslationEx
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.basabali.app.data.WordEntry
import com.basabali.app.ui.theme.NewWordPurple
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basabali.app.data.ContentRepository
import com.basabali.app.engine.FillBlankEx
import com.basabali.app.engine.MatchPairsEx
import com.basabali.app.engine.MultipleChoiceEx
import com.basabali.app.engine.WordBankEx
import com.basabali.app.engine.WordCardEx
import com.basabali.app.ui.theme.CreamCard
import com.basabali.app.ui.theme.Gold
import com.basabali.app.ui.theme.GrayLock
import com.basabali.app.ui.theme.GreenOk
import com.basabali.app.ui.theme.Ink
import com.basabali.app.ui.theme.InkSoft
import com.basabali.app.ui.theme.RedBad
import com.basabali.app.ui.theme.Teal
import kotlinx.coroutines.delay

// ------------------------------------------------------------------ shared bits

@Composable
fun RegisterChip(register: String) {
    val label: String
    val color: Color
    when (register) {
        "alus" -> { label = "alus · refined"; color = Gold }
        "andap" -> { label = "andap · familiar"; color = Teal }
        else -> { label = "mider · neutral"; color = GrayLock }
    }
    Surface(
        color = color.copy(alpha = 0.13f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun OptionCard(
    text: String,
    state: String,          // idle | right | wrong | dim
    enabled: Boolean,
    fill: Boolean = true,
    onClick: () -> Unit
) {
    val bgTarget = when (state) {
        "right" -> GreenOk.copy(alpha = 0.16f)
        "wrong" -> RedBad.copy(alpha = 0.16f)
        "dim" -> CreamCard.copy(alpha = 0.55f)
        else -> CreamCard
    }
    val borderTarget = when (state) {
        "right" -> GreenOk
        "wrong" -> RedBad
        else -> Color(0x24000000)
    }
    val bg by animateColorAsState(bgTarget, tween(220), label = "option-bg")
    val border by animateColorAsState(borderTarget, tween(220), label = "option-border")
    val mod = if (fill) Modifier.fillMaxWidth() else Modifier
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, border),
        modifier = mod.clickable(enabled = enabled) { onClick() }
    ) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 14.dp), fontSize = 16.sp, color = Ink)
    }
}

// ------------------------------------------------------------------ E1 word card

@Composable
fun WordCardView(ex: WordCardEx, content: ContentRepository, tts: TtsManager?, onContinue: () -> Unit) {
    // the new vocabulary entry announces itself (TTS on appearance)
    LaunchedEffect(ex.wordId) { tts?.speak(content.display(ex.wordId)) }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        RegisterChip(ex.register)
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                content.display(ex.wordId),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            if (tts != null) {
                Spacer(Modifier.width(10.dp))
                SpeakerButton(content.display(ex.wordId), tts, size = 44)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            ex.gloss,
            fontSize = 18.sp,
            color = InkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(30.dp))
        Surface(
            color = CreamCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x1A000000)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                TappableBalinese(ex.exampleBal, content, ex.wordId, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tts != null) SpeakerButton(ex.exampleBal, tts, size = 38)
                    Spacer(Modifier.width(10.dp))
                    Text(ex.exampleEn, fontSize = 14.sp, fontStyle = FontStyle.Italic, color = InkSoft,
                        modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().clickable { onContinue() }
        ) {
            Box(Modifier.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text("Continue", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ------------------------------------------------------------------ E2 match pairs

@Composable
fun MatchPairsView(ex: MatchPairsEx, content: ContentRepository, tts: TtsManager?, onDone: (Int) -> Unit) {
    var errors by remember(ex) { mutableStateOf(0) }
    val left = remember(ex) { ex.pairs.map { it.first }.shuffled() }
    val right = remember(ex) { ex.pairs.shuffled() }
    var selLeft by remember(ex) { mutableStateOf<String?>(null) }
    var selRight by remember(ex) { mutableStateOf<Pair<String, String>?>(null) }
    val matched = remember(ex) { mutableStateListOf<String>() }
    var wrongFlash by remember(ex) { mutableStateOf(false) }

    LaunchedEffect(selLeft, selRight) {
        val l = selLeft
        val r = selRight
        if (l != null && r != null) {
            if (r.first == l) {
                matched += l
                selLeft = null
                selRight = null
                if (matched.size == ex.pairs.size) onDone(errors)
            } else {
                errors++
                wrongFlash = true
                delay(400)
                wrongFlash = false
                selLeft = null
                selRight = null
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text("Tap the pairs", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (w in left) {
                    val isMatched = matched.contains(w)
                    val isSel = selLeft == w
                    PairTile(
                        text = content.display(w),
                        selected = isSel,
                        wrong = isSel && wrongFlash,
                        done = isMatched,
                        modifier = Modifier.fillMaxWidth(),
                        onSpeak = if (tts != null) ({ tts.speak(content.display(w)) }) else null
                    ) {
                        selLeft = if (selLeft == w) null else w
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (p in right) {
                    val isMatched = matched.contains(p.first)
                    val isSel = selRight?.first == p.first
                    PairTile(
                        text = p.second,
                        selected = isSel,
                        wrong = isSel && wrongFlash,
                        done = isMatched,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selRight = if (selRight?.first == p.first) null else p
                    }
                }
            }
        }
    }
}

@Composable
private fun PairTile(
    text: String,
    selected: Boolean,
    wrong: Boolean,
    done: Boolean,
    modifier: Modifier = Modifier,
    onSpeak: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val bg = when {
        done -> GreenOk.copy(alpha = 0.18f)
        wrong -> RedBad.copy(alpha = 0.18f)
        selected -> Gold.copy(alpha = 0.20f)
        else -> CreamCard
    }
    val border = when {
        done -> GreenOk
        wrong -> RedBad
        selected -> Gold
        else -> Color(0x22000000)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, border),
        modifier = modifier.clickable(enabled = !done) { AppGraph.fx?.play(SoundFx.Sound.CLICK); onClick() }
    ) {
        if (onSpeak != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text,
                    Modifier.weight(1f).padding(vertical = 14.dp, horizontal = 6.dp),
                    fontSize = 14.sp,
                    color = if (done) GreenOk else Ink,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
                Text(
                    "🔊",
                    Modifier.clickable(enabled = !done) { onSpeak() }
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    fontSize = 13.sp
                )
            }
        } else {
            Text(
                text,
                Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                fontSize = 14.sp,
                color = if (done) GreenOk else Ink,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ------------------------------------------------------------------ E3 multiple choice

@Composable
fun MultipleChoiceView(
    ex: MultipleChoiceEx,
    content: ContentRepository,
    tts: TtsManager?,
    enabled: Boolean,
    picked: String?,
    onPick: (String) -> Unit
) {
    // Balinese prompts (sentence or vocabulary entry) speak themselves when shown
    if (ex.toEnglish) LaunchedEffect(ex.sentenceId, ex.prompt) { tts?.speak(ex.prompt) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            when {
                ex.toEnglish -> "What does this mean?"
                ex.sentenceId != null -> "Choose the Balinese sentence"
                else -> "Which Balinese word matches?"
            },
            fontSize = 15.sp,
            color = InkSoft
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            color = CreamCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x1A000000))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RegisterChip(ex.register)
                    if (ex.toEnglish && tts != null) {
                        Spacer(Modifier.weight(1f))
                        SpeakerButton(ex.prompt, tts, size = 38)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (ex.toEnglish) {
                    TappableBalinese(ex.prompt, content, ex.highlight, fontSize = 21.sp)
                } else {
                    Text(ex.prompt, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        for (opt in ex.options) {
            val state = when {
                picked == null -> "idle"
                opt == ex.correct -> "right"
                opt == picked -> "wrong"
                else -> "dim"
            }
            OptionCard(text = opt, state = state, enabled = enabled) {
                // Balinese choices are spoken when tapped (before grading)
                if (!ex.toEnglish) tts?.speak(opt)
                onPick(opt)
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ E4 word bank

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordBankView(
    ex: WordBankEx,
    selected: List<Int>,
    enabled: Boolean,
    onPickBank: (Int) -> Unit,
    onRemoveAnswer: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Translate into Balinese", fontSize = 15.sp, color = InkSoft)
            Spacer(Modifier.weight(1f))
            RegisterChip(ex.register)
        }
        Spacer(Modifier.height(10.dp))
        Text(ex.promptEn, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(20.dp))

        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x33000000)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
        ) {
            FlowRow(
                Modifier.padding(10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selected.isEmpty()) {
                    Text(
                        "tap the words below…",
                        color = InkSoft,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                selected.forEach { bankIdx ->
                    TokenTile(
                        text = ex.bankTokens[bankIdx],
                        tint = Gold,
                        onClick = { if (enabled) onRemoveAnswer(bankIdx) }
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val hlTokens = ex.highlight?.split(" ")?.toSet() ?: emptySet()
            ex.bankTokens.indices.forEach { i ->
                val isNew = ex.bankTokens[i].lowercase().trim('.', ',', '!', '?', ';', ':') in hlTokens
                if (selected.contains(i)) {
                    // render the REAL token invisibly so tile widths never change and
                    // remaining tiles cannot reflow under the learner's finger
                    TokenTile(text = ex.bankTokens[i], tint = Color.Transparent, ghost = true, onClick = {})
                } else {
                    TokenTile(
                        text = ex.bankTokens[i],
                        tint = Teal,
                        textColor = if (isNew) NewWordPurple else Ink,
                        onClick = { if (enabled) onPickBank(i) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenTile(
    text: String,
    tint: Color,
    textColor: Color = Ink,
    ghost: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = if (ghost) Color(0x10000000) else tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (ghost) Color.Transparent else tint.copy(alpha = 0.55f)),
        modifier = Modifier.clickable(enabled = !ghost) { AppGraph.fx?.play(SoundFx.Sound.CLICK); onClick() }
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            fontSize = 16.sp,
            color = if (ghost) Color.Transparent else textColor
        )
    }
}

// ------------------------------------------------------------------ E7 fill blank

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillBlankView(
    ex: FillBlankEx,
    content: ContentRepository,
    tts: TtsManager?,
    enabled: Boolean,
    picked: String?,
    onPick: (String) -> Unit
) {
    val audio = listOf(ex.before, ex.after).filter { it.isNotBlank() }.joinToString(", ")
    // the sentence announces itself when it appears (with a natural gap at the blank)
    LaunchedEffect(ex.sentenceId, ex.before, ex.after) { tts?.speak(audio) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Complete the sentence", fontSize = 15.sp, color = InkSoft)
        Spacer(Modifier.height(12.dp))
        Surface(
            color = CreamCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x1A000000))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RegisterChip(ex.register)
                    if (tts != null) {
                        Spacer(Modifier.weight(1f))
                        SpeakerButton(audio, tts, size = 38)
                    }
                }
                Spacer(Modifier.height(10.dp))
                FlowRow {
                    if (ex.before.isNotEmpty()) {
                        TappableBalinese(ex.before, content, ex.highlight, fontSize = 22.sp)
                    }
                    Text(" _____ ", fontSize = 22.sp, color = Gold, fontWeight = FontWeight.Bold)
                    if (ex.after.isNotEmpty()) {
                        TappableBalinese(ex.after, content, ex.highlight, fontSize = 22.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(ex.en, fontSize = 14.sp, fontStyle = FontStyle.Italic, color = InkSoft)
            }
        }
        Spacer(Modifier.height(22.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (opt in ex.options) {
                val state = when {
                    picked == null -> "idle"
                    opt == ex.correct -> "right"
                    opt == picked -> "wrong"
                    else -> "dim"
                }
                OptionCard(
                    text = content.display(opt),
                    state = state,
                    enabled = enabled,
                    fill = false
                ) {
                    tts?.speak(content.display(opt))
                    onPick(opt)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ v2: audio & typing

@Composable
fun SpeakerButton(text: String, tts: TtsManager?, slow: Boolean = false, size: Int = 64) {
    Surface(
        color = if (tts?.available == true) Gold else GrayLock.copy(alpha = 0.4f),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .size(size.dp)
            .clickable(enabled = tts?.available == true) { tts?.speak(text, slow) }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(if (slow) "🐢" else "🔊", fontSize = (size / 2.6).sp)
        }
    }
}

@Composable
fun ListenTapView(
    ex: ListenTapEx,
    tts: TtsManager?,
    enabled: Boolean,
    picked: String?,
    onPick: (String) -> Unit
) {
    LaunchedEffect(ex.sentenceId) { tts?.speak(ex.audioText) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (ex.toEnglish) "Listen — what does it mean?" else "Listen — which sentence did you hear?",
            fontSize = 15.sp, color = InkSoft, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        SpeakerButton(ex.audioText, tts, size = 84)
        Spacer(Modifier.height(10.dp))
        Text("play again · slowly →", fontSize = 11.sp, color = InkSoft)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeakerButton(ex.audioText, tts, size = 44)
            SpeakerButton(ex.audioText, tts, slow = true, size = 44)
        }
        Spacer(Modifier.height(26.dp))
        Column(Modifier.fillMaxWidth()) {
            for (opt in ex.options) {
                val state = when {
                    picked == null -> "idle"
                    opt == ex.correct -> "right"
                    opt == picked -> "wrong"
                    else -> "dim"
                }
                OptionCard(text = opt, state = state, enabled = enabled) {
                    if (!ex.toEnglish) tts?.speak(opt)
                    onPick(opt)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun DictationView(ex: DictationEx, tts: TtsManager?, text: String, onText: (String) -> Unit, enabled: Boolean) {
    LaunchedEffect(ex.sentenceId) { tts?.speak(ex.audioText) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Type what you hear", fontSize = 15.sp, color = InkSoft)
        Spacer(Modifier.height(22.dp))
        SpeakerButton(ex.audioText, tts, size = 84)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeakerButton(ex.audioText, tts, size = 44)
            SpeakerButton(ex.audioText, tts, slow = true, size = 44)
        }
        Spacer(Modifier.height(26.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { if (enabled) onText(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("…in Balinese", color = InkSoft) },
            singleLine = false,
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun TypeTranslationView(ex: TypeTranslationEx, text: String, onText: (String) -> Unit, enabled: Boolean) {
    Column(Modifier.fillMaxSize()) {
        Text("Translate into Balinese — type it yourself", fontSize = 15.sp, color = InkSoft)
        Spacer(Modifier.height(12.dp))
        Surface(
            color = CreamCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x1A000000))
        ) {
            Column(Modifier.padding(16.dp)) {
                RegisterChip(ex.register)
                Spacer(Modifier.height(8.dp))
                Text(ex.promptEn, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { if (enabled) onText(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("type the Balinese sentence…", color = InkSoft) },
            singleLine = false,
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun SpeakView(
    ex: SpeakEx,
    tts: TtsManager?,
    asr: AsrManager?,
    enabled: Boolean,
    onResult: (List<String>) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var status by remember(ex.sentenceId) { mutableStateOf("idle") } // idle|listening|done|denied|unavailable
    // the target sentence is the model pronunciation — play it as the exercise appears
    LaunchedEffect(ex.sentenceId) { tts?.speak(ex.target) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            status = "listening"
            asr?.listen { res -> status = "done"; onResult(res) }
        } else {
            status = "denied"
        }
    }

    fun startListening() {
        if (asr == null || !asr.available()) { status = "unavailable"; return }
        val granted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            tts?.stop()   // never let the mic pick up the model audio
            status = "listening"
            asr.listen { res -> status = "done"; onResult(res) }
        } else {
            permLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Read this sentence aloud", fontSize = 15.sp, color = InkSoft)
        Spacer(Modifier.height(18.dp))
        Surface(
            color = CreamCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x1A000000)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RegisterChip(ex.register)
                Spacer(Modifier.height(8.dp))
                Text(ex.target, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(ex.en, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = InkSoft, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            SpeakerButton(ex.target, tts, size = 52)
            Surface(
                color = if (status == "listening") RedBad else Teal,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .size(64.dp)
                    .clickable(enabled = enabled && status != "listening") { startListening() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (status == "listening") "👂" else "🎤", fontSize = 26.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when (status) {
                "listening" -> "Listening… speak now"
                "done" -> "Checking…"
                "denied" -> "Microphone permission denied"
                "unavailable" -> "No speech recognizer on this device"
                else -> "Tap the mic and read the sentence"
            },
            fontSize = 13.sp, color = InkSoft, textAlign = TextAlign.Center
        )
        // Always-visible skip: tapping it turns speaking off until the end of this subunit.
        Spacer(Modifier.height(14.dp))
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, Teal),
            modifier = Modifier.clickable(enabled = enabled) {
                tts?.stop()
                asr?.stop()
                onSkip()
            }
        ) {
            Box(Modifier.padding(horizontal = 22.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                Text("Skip speaking →", fontSize = 14.sp, color = Teal, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Turns off the remaining speaking steps of this subunit",
            fontSize = 11.sp, color = InkSoft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
    }
}

// ------------------------------------------------------------------ tappable sentences & dictionary popup

private fun normToken(tok: String): String {
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

private data class TokenParts(val prefix: String, val core: String, val suffix: String)

/**
 * Split a surface token into punctuation (prefix/suffix) and its word core, so
 * "drika." renders as the word "drika" plus a full stop that is never
 * underlined. Only the core is looked up / highlighted / tappable.
 */
private fun tokenParts(tok: String): TokenParts {
    fun isWordChar(c: Char) = c.isLetterOrDigit() || c == '\'' || c == '-'
    val start = tok.indexOfFirst { isWordChar(it) }
    if (start < 0) return TokenParts(tok, "", "")
    var end = tok.length
    while (end > start && !isWordChar(tok[end - 1])) end--
    return TokenParts(tok.substring(0, start), tok.substring(start, end), tok.substring(end))
}

private fun highlightRanges(norm: List<String>, entry: String?): List<IntRange> {
    if (entry.isNullOrBlank()) return emptyList()
    val w = entry.split(" ")
    val ranges = ArrayList<IntRange>()
    var i = 0
    while (i + w.size <= norm.size) {
        var m = true
        for (k in w.indices) {
            if (norm[i + k] != w[k]) { m = false; break }
        }
        if (m) { ranges += i until i + w.size; i += w.size } else i++
    }
    return ranges
}

/**
 * Renders a Balinese sentence as individually tappable tokens:
 *  - every dictionary word is UNDERLINED — tapping opens a popup (not a toast)
 *    listing all English glosses from the course vocabulary (multi-sense words
 *    like `lima` = five/hand show every sense with register + source lesson)
 *  - tokens belonging to [highlightEntry] (the sentence's brand-new vocabulary)
 *    render in light purple
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TappableBalinese(
    text: String,
    content: ContentRepository,
    highlightEntry: String?,
    fontSize: TextUnit = 21.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    arrangement: Arrangement.Horizontal = Arrangement.Start
) {
    val display = remember(text) { text.split(" ").filter { it.isNotBlank() } }
    val parts = remember(text) { display.map { tokenParts(it) } }
    val norm = remember(text) { display.map { normToken(it) } }
    val hl = remember(text, highlightEntry) { highlightRanges(norm, highlightEntry) }
    var popup by remember { mutableStateOf<Pair<String, List<WordEntry>>?>(null) }

    FlowRow(horizontalArrangement = arrangement) {
        display.forEachIndexed { i, _ ->
            val p = parts[i]
            val lookups = remember(text, i) {
                if (norm[i].isEmpty()) emptyList() else content.lookupsAt(norm, i)
            }
            val purple = hl.any { i in it }
            Row {
                if (p.prefix.isNotEmpty()) {
                    Text(p.prefix, fontSize = fontSize, fontWeight = fontWeight, color = Ink)
                }
                if (p.core.isNotEmpty()) {
                    Text(
                        text = p.core,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = if (purple) NewWordPurple else Ink,
                        textDecoration = if (lookups.isNotEmpty()) TextDecoration.Underline else null,
                        modifier = if (lookups.isNotEmpty()) {
                            Modifier.clickable { popup = p.core to lookups }
                        } else Modifier
                    )
                }
                if (p.suffix.isNotEmpty()) {
                    Text(p.suffix, fontSize = fontSize, fontWeight = fontWeight, color = Ink)
                }
                Text(" ", fontSize = fontSize)
            }
        }
    }

    popup?.let { entry ->
        WordPopupDialog(entry.first, entry.second, onDismiss = { popup = null })
    }
}

@Composable
fun WordPopupDialog(token: String, entries: List<WordEntry>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(token, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                entries.forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RegisterChip(e.register)
                        Spacer(Modifier.width(8.dp))
                        Text(e.gloss, fontSize = 14.sp, color = Ink, modifier = Modifier.weight(1f))
                    }
                    Text("vocabulary of Lesson ${e.firstLesson}", fontSize = 10.sp, color = InkSoft)
                    Spacer(Modifier.height(10.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
