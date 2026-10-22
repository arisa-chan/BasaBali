package com.basabali.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basabali.app.AppGraph
import com.basabali.app.audio.AsrManager
import com.basabali.app.audio.SoundFx
import com.basabali.app.audio.TtsManager
import com.basabali.app.data.ContentRepository
import com.basabali.app.data.NodeKind
import com.basabali.app.data.PathNode
import com.basabali.app.data.Sentence
import com.basabali.app.data.Story
import com.basabali.app.data.UserStore
import com.basabali.app.engine.LeagueEngine
import com.basabali.app.engine.Quests
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
import com.basabali.app.ui.theme.NewWordPurple
import com.basabali.app.ui.theme.RedBad
import com.basabali.app.ui.theme.Teal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private fun unitLabel(unit: Int, lessonId: Int): String = when (unit) {
    1 -> "U1 · Learn"
    2 -> "U2 · Reinforce"
    3 -> "U3 · Review L1–$lessonId"
    4 -> "U4 · Lesson review"
    else -> "U5 · Mastery"
}

/**
 * App shell: sticky top bar (title · streak · XP), sticky bottom navigation
 * (Lessons · Stories · Quests · Leaderboard · Dictionary · Settings) and the six
 * tab contents. Lesson / story / completion screens route away from here.
 */
@Composable
fun MainScreen(
    content: ContentRepository,
    store: UserStore,
    builder: SessionBuilder,
    tts: TtsManager?,
    asr: AsrManager?,
    onStartSession: (SessionKind) -> Unit,
    onStory: (Story) -> Unit,
    onPlacement: () -> Unit,
    onReset: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val now = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) { LeagueEngine.ensureActive(store, now) }

    val tabs = listOf(
        "📚" to "Lessons",
        "📖" to "Stories",
        "🎯" to "Quests",
        "🏆" to "Leaderboard",
        "🔎" to "Dictionary",
        "⚙️" to "Settings"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = MaterialTheme.colorScheme.background,
        // ------------------------------------------------- sticky top bar
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("BasaBali", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Learn Balinese from English", fontSize = 11.sp, color = InkSoft)
                    }
                    HeaderStat("🔥", "${store.streak}", pulse = true)
                    HeaderStat("⭐", "${store.xp}")
                }
            }
        },
        // ------------------------------------------------- sticky bottom navigation
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                tabs.forEachIndexed { i, (emoji, label) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(emoji, fontSize = 17.sp) },
                        label = {
                            Text(label, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            // slow-drifting frangipani petals behind the tabs (subtle motion)
            FloatingPetals(Modifier.fillMaxSize())
            when (tab) {
                0 -> LessonsTab(content, store, snack, scope) { node ->
                    onStartSession(SessionKind.Node(node))
                }
                1 -> StoriesTab(content, store, snack, scope, onStory)
                2 -> QuestsTab(store)
                3 -> LeagueContent(store)
                4 -> DictionaryScreen(content, store, builder, tts, snack, scope, onStartSession)
                else -> SettingsTab(content, store, tts, asr, onPlacement, onReset)
            }
        }
    }
}

/** Gentle ambient motion for the main shell: petals drifting down the screen. */
@Composable
private fun FloatingPetals(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "petals")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(tween(120_000, easing = LinearEasing)),
        label = "petal-time"
    )
    Canvas(modifier) {
        val n = 10
        for (i in 0 until n) {
            val speedY = 24f + (i % 4) * 9f
            val speedX = 6f + (i % 3) * 6f
            val x = ((i * 149.7f + t * speedX) % (size.width + 160f)) - 80f
            val y = ((i * 211.3f + t * speedY) % (size.height + 160f)) - 80f
            val r = 5.5f + (i % 3) * 2.5f
            drawCircle(
                color = if (i % 2 == 0) Gold.copy(alpha = 0.10f) else Teal.copy(alpha = 0.07f),
                radius = r,
                center = Offset(x, y)
            )
        }
    }
}

// ================================================================ Lessons tab

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonsTab(
    content: ContentRepository,
    store: UserStore,
    snack: SnackbarHostState,
    scope: CoroutineScope,
    onStartNode: (PathNode) -> Unit
) {
    val done = store.doneChunks
    val currentNodeId = remember(done.toSet()) { content.path.firstOrNull { it.id !in done }?.id }
    val currentNode = remember(currentNodeId) { content.path.firstOrNull { it.id == currentNodeId } }
    val currentLessonId = currentNode?.lessonId
    val learnedWords = remember(done.toSet(), store.xp) { store.taughtWordCount() }
    // one shared pulse for the current path node (all other nodes stay still)
    val nodeTransition = rememberInfiniteTransition(label = "path-node")
    val nodePulse by nodeTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "node-pulse"
    )

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // continue banner
        if (currentNode != null) {
            item {
                Surface(
                    color = Gold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, Gold.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth().clickable { onStartNode(currentNode) }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("▶", fontSize = 20.sp, color = Gold, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(
                                "Lesson ${currentNode.lessonId} · ${unitLabel(currentNode.unit, currentNode.lessonId)} · step ${currentNode.sub + 1}",
                                fontSize = 12.sp, color = InkSoft
                            )
                        }
                    }
                }
            }
        }

        // stats strip
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("${done.size}/${content.path.size}", "steps done", Modifier.weight(1f))
                MiniStat("$learnedWords", "words seen", Modifier.weight(1f))
                MiniStat("${content.sentences.size}", "sentence bank", Modifier.weight(1f))
            }
        }

        // sections → lessons → units → subunits
        for (section in content.sections) {
            item {
                Text("Section ${section.id} · ${section.title}",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkSoft,
                    modifier = Modifier.padding(top = 10.dp))
            }
            for (lesson in section.lessons) {
                item {
                    val nodes = content.nodesByLesson[lesson.id] ?: emptyList()
                    var expanded by remember(lesson.id) { mutableStateOf(lesson.id == currentLessonId) }
                    val doneCount = nodes.count { it.id in done }
                    Surface(
                        color = CreamCard, shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0x14000000)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lesson ${lesson.id} · ${lesson.title}",
                                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink,
                                    modifier = Modifier.weight(1f))
                                Text("$doneCount/${nodes.size}", fontSize = 12.sp,
                                    color = if (doneCount == nodes.size && nodes.isNotEmpty()) GreenOk else InkSoft)
                                Spacer(Modifier.width(6.dp))
                                Text(if (expanded) "▲" else "▼", fontSize = 10.sp, color = InkSoft)
                            }
                            if (expanded) {
                                Spacer(Modifier.height(10.dp))
                                for (u in 1..5) {
                                    val unitNodes = nodes.filter { it.unit == u }
                                    if (unitNodes.isEmpty()) continue
                                    Row(
                                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(unitLabel(u, lesson.id), fontSize = 10.sp, color = InkSoft,
                                            fontWeight = FontWeight.SemiBold, modifier = Modifier.width(104.dp))
                                        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            unitNodes.forEach { node ->
                                                val state = when {
                                                    node.id in done -> "done"
                                                    node.id == currentNodeId -> "current"
                                                    else -> "locked"
                                                }
                                                ChipForNode(node, state, nodePulse) {
                                                    when (state) {
                                                        "locked" -> scope.launch {
                                                            snack.showSnackbar("Finish the previous step first — the path unlocks step by step.")
                                                        }
                                                        else -> onStartNode(node)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipForNode(node: PathNode, state: String, pulse: Float = 1f, onClick: () -> Unit) {
    val bg = when (state) {
        "done" -> GreenOk.copy(alpha = 0.18f)
        "current" -> Gold
        else -> GrayLock.copy(alpha = 0.12f)
    }
    val fg = when (state) {
        "done" -> GreenOk
        "current" -> Color.White
        else -> GrayLock
    }
    val border = when (state) {
        "done" -> GreenOk.copy(alpha = 0.6f)
        "current" -> Gold
        else -> GrayLock.copy(alpha = 0.35f)
    }
    // review-type subunits get a ♻ glyph so learners can tell them from new-teaching steps
    val isReview = node.kind is NodeKind.LessonReview || node.kind is NodeKind.Cumulative ||
        node.kind is NodeKind.Reinforce
    Surface(
        color = bg,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, border),
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer {
                if (state == "current") {
                    scaleX = pulse
                    scaleY = pulse
                }
            }
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                when {
                    state == "done" -> "✓"
                    state == "locked" -> "🔒"
                    isReview -> "♻"
                    else -> "${node.sub + 1}"
                },
                color = fg,
                fontSize = if (state == "locked") 12.sp else 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ================================================================ Stories tab

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoriesTab(
    content: ContentRepository,
    store: UserStore,
    snack: SnackbarHostState,
    scope: CoroutineScope,
    onStory: (Story) -> Unit
) {
    val done = store.doneChunks
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Stories", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Read, listen and answer — story XP counts for quests and the leaderboard. Finished stories stay open.",
            fontSize = 12.sp, color = InkSoft
        )
        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (story in content.stories) {
                val completed = story.id in store.completedStories
                // A finished story is always readable, even if the learner jumped ahead.
                val unlocked = completed || content.path.none { it.lessonId <= story.gate && it.id !in done }
                val bg = when {
                    completed -> GreenOk.copy(alpha = 0.14f)
                    unlocked -> CreamCard
                    else -> GrayLock.copy(alpha = 0.10f)
                }
                Surface(
                    color = bg, shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (unlocked && !completed) Gold.copy(alpha = 0.5f) else Color(0x14000000)),
                    modifier = Modifier.width(158.dp).clickable {
                        if (unlocked) onStory(story)
                        else scope.launch { snack.showSnackbar("Finish all lessons up to Lesson ${story.gate} to unlock this story.") }
                    }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (completed) "✅" else if (unlocked) "📖" else "🔒", fontSize = 20.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(story.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(story.titleEn, fontSize = 11.sp, color = InkSoft,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!unlocked && !completed) Text("Lesson ${story.gate}+", fontSize = 10.sp, color = GrayLock)
                    }
                }
            }
        }
    }
}

// ================================================================ Quests tab (+ calendar)

@Composable
private fun QuestsTab(store: UserStore) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // streak banner
        Surface(
            color = Gold.copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 30.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("${store.streak}-day streak", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Practice every day to keep it alive — the calendar below shows your history.",
                        fontSize = 12.sp, color = InkSoft)
                }
            }
        }

        // daily quests
        val today = Quests.dayKey()
        val defs = Quests.forDay(today)
        val prog = if (store.questDay == today) store.questProg else listOf(0, 0, 0)
        Surface(
            color = CreamCard, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0x14000000)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily quests", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = Ink, modifier = Modifier.weight(1f))
                    if (store.questDay == today && store.questBonusClaimed)
                        Text("+30 XP claimed ✓", fontSize = 11.sp, color = GreenOk, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                defs.forEachIndexed { i, d ->
                    val p = (prog.getOrElse(i) { 0 }).coerceAtMost(d.goal)
                    val frac by animateFloatAsState(p.toFloat() / d.goal.toFloat(), tween(500), label = "quest")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.label(), fontSize = 13.sp, color = Ink)
                            Spacer(Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { frac },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = if (p >= d.goal) GreenOk else Gold,
                                trackColor = Gold.copy(alpha = 0.12f)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("$p/${d.goal}", fontSize = 11.sp, color = InkSoft)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        CalendarCard(store)
    }
}

@Composable
private fun CalendarCard(store: UserStore) {
    var monthOffset by remember { mutableStateOf(0) }
    val today = remember { LocalDate.now() }
    val ym = remember(monthOffset, today) { today.withDayOfMonth(1).plusMonths(monthOffset.toLong()) }
    val monthName = remember(ym) { ym.month.name.lowercase().replaceFirstChar { it.uppercase() } }

    Surface(
        color = CreamCard, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0x14000000)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◀", fontSize = 14.sp, color = InkSoft,
                    modifier = Modifier.clip(CircleShape).clickable { monthOffset-- }.padding(horizontal = 10.dp, vertical = 4.dp))
                Text("$monthName ${ym.year}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f))
                Text("▶", fontSize = 14.sp, color = InkSoft,
                    modifier = Modifier.clip(CircleShape).clickable { monthOffset++ }.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach {
                    Text(it, fontSize = 10.sp, color = InkSoft, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))
            val firstDow = ym.dayOfWeek.value            // Monday = 1 … Sunday = 7
            val cells: List<Int?> = List(firstDow - 1) { null } + (1..ym.lengthOfMonth()).toList()
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    for (j in 0 until 7) {
                        val d = week.getOrNull(j)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (d != null) {
                                val date = ym.withDayOfMonth(d)
                                val practiced = date.toString() in store.practiceDays
                                val isToday = date == today
                                Box(
                                    Modifier.size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (practiced) Gold.copy(alpha = 0.85f) else Color.Transparent)
                                        .then(
                                            if (isToday && !practiced) Modifier.border(1.5.dp, Teal, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$d", fontSize = 11.sp,
                                        color = if (practiced) Color.White else Ink,
                                        fontWeight = if (practiced || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            val prefix = "${ym.year}-${"%02d".format(ym.monthValue)}"
            val monthCount = remember(prefix, store.practiceDays.size) {
                store.practiceDays.count { it.startsWith(prefix) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Gold.copy(alpha = 0.85f)))
                Spacer(Modifier.width(6.dp))
                Text("practiced · $monthCount day(s) in $monthName", fontSize = 11.sp, color = InkSoft)
            }
        }
    }
}

// ================================================================ Review tab

@Composable
private fun ReviewTab(
    content: ContentRepository,
    store: UserStore,
    builder: SessionBuilder,
    tts: TtsManager?,
    snack: SnackbarHostState,
    scope: CoroutineScope,
    onStartSession: (SessionKind) -> Unit
) {
    val dueCount = remember(store.doneChunks.size, store.xp) { builder.dueCount() }
    val mistakeCount = store.mistakes.size
    val taught = store.taughtWordCount()
    var wordsOpen by remember { mutableStateOf(false) }
    var sentsOpen by remember { mutableStateOf(false) }

    val words = remember(store.words.size, store.xp) {
        store.words.entries
            .filter { it.value.lastReview > 0L }
            .sortedWith(compareBy({ content.words[it.key]?.firstLesson ?: 99 }, { it.key }))
            .map { it.key }
    }
    val sents = remember(store.seenSentences.size) {
        store.seenSentences.entries
            .sortedByDescending { it.value }
            .take(200)
            .mapNotNull { content.sentencesById[it.key] }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ReviewActionCard(
                emoji = "🧠", accent = Teal,
                title = "Review encountered words",
                sub = if (dueCount > 0) "$dueCount word/pattern reviews due — the SRS picks the sentences"
                      else "Nothing formally due — rehearse your weakest words",
                active = taught > 0
            ) {
                if (taught > 0) onStartSession(SessionKind.Review)
                else scope.launch { snack.showSnackbar("Finish a lesson step first — then your words show up here.") }
            }
        }
        item {
            ReviewActionCard(
                emoji = "❗", accent = RedBad,
                title = "Review past mistakes",
                sub = if (mistakeCount > 0) "$mistakeCount sentence(s) you missed — they leave this list once you answer them correctly in a later session"
                      else "No open mistakes — anything you miss lands here 🎉",
                active = mistakeCount > 0
            ) {
                if (mistakeCount > 0) onStartSession(SessionKind.Mistakes)
                else scope.launch { snack.showSnackbar("No mistakes logged. Nice work!") }
            }
        }

        item {
            CollapsibleHeader(
                label = "Words you've met",
                count = words.size,
                open = wordsOpen
            ) { wordsOpen = !wordsOpen }
        }
        if (wordsOpen) {
            items(words.size) { i ->
                WordReviewRow(content, store, tts, words[i])
            }
        }

        item {
            CollapsibleHeader(
                label = if (store.seenSentences.size > 200)
                    "Sentences you've met (200 newest of ${store.seenSentences.size})"
                else "Sentences you've met",
                count = sents.size,
                open = sentsOpen
            ) { sentsOpen = !sentsOpen }
        }
        if (sentsOpen) {
            items(sents.size) { i ->
                SentenceReviewRow(content, tts, sents[i])
            }
        }
    }
}

@Composable
private fun CollapsibleHeader(label: String, count: Int, open: Boolean, onToggle: () -> Unit) {
    Surface(
        color = CreamCard, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0x14000000)),
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkSoft,
                modifier = Modifier.weight(1f))
            Text("$count", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.width(8.dp))
            Text(if (open) "▲" else "▼", fontSize = 11.sp, color = InkSoft)
        }
    }
}

@Composable
private fun ReviewActionCard(
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
private fun WordReviewRow(content: ContentRepository, store: UserStore, tts: TtsManager?, id: String) {
    val st = store.words[id]
    val box = (st?.box ?: 0).coerceIn(0, 7)
    val filled = ((box + 1) * 6 / 8).coerceIn(1, 6)
    Surface(
        color = CreamCard, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0x14000000)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            SpeakerButton(text = content.display(id), tts = tts, size = 34)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(content.display(id), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.width(6.dp))
                    RegisterChip(content.registerOf(id))
                }
                Text(content.gloss(id), fontSize = 12.sp, color = InkSoft)
            }
            Text(
                "●".repeat(filled) + "○".repeat(6 - filled),
                fontSize = 9.sp,
                color = if (box >= 4) GreenOk else Gold
            )
        }
    }
}

@Composable
private fun SentenceReviewRow(content: ContentRepository, tts: TtsManager?, s: Sentence) {
    Surface(
        color = CreamCard, shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0x14000000)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    TappableBalinese(s.bal, content, null, 15.sp, FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                SpeakerButton(text = s.bal, tts = tts, size = 30)
                Spacer(Modifier.width(6.dp))
                ShareSentenceButton(bal = s.bal, en = s.en, register = s.register, tint = Teal, size = 30)
            }
            Spacer(Modifier.height(3.dp))
            Text(s.en, fontSize = 12.sp, color = InkSoft, fontStyle = FontStyle.Italic)
            if (s.newItem != null) {
                Text("new here: ${content.display(s.newItem)}", fontSize = 10.sp, color = NewWordPurple)
            }
        }
    }
}

// ================================================================ Settings tab

@Composable
private fun SettingsTab(
    content: ContentRepository,
    store: UserStore,
    tts: TtsManager?,
    asr: AsrManager?,
    onPlacement: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // scheduler
        Surface(
            color = CreamCard, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0x14000000)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Spaced-repetition scheduler", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        store.schedulerMode = 0; Srs.mode = Srs.Mode.LEITNER; store.save()
                    }) {
                    RadioButton(selected = store.schedulerMode == 0, onClick = {
                        store.schedulerMode = 0; Srs.mode = Srs.Mode.LEITNER; store.save()
                    })
                    Text("Simple — expanding boxes (1→128 days)", fontSize = 13.sp, color = Ink)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        store.schedulerMode = 1; Srs.mode = Srs.Mode.FSRS; store.save()
                    }) {
                    RadioButton(selected = store.schedulerMode == 1, onClick = {
                        store.schedulerMode = 1; Srs.mode = Srs.Mode.FSRS; store.save()
                    })
                    Text("FSRS-4.5 — stability/difficulty model", fontSize = 13.sp, color = Ink)
                }
            }
        }

        // audio & sounds
        Surface(
            color = CreamCard, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0x14000000)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Audio & sounds", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("🔊 Voice: " +
                    (if (tts?.available == true) "ready (${tts.languageLabel})" else "starting / unavailable"),
                    fontSize = 12.sp, color = if (tts?.available == true) GreenOk else InkSoft)
                if (tts?.available == true) {
                    SpeakerButton("Om Swastiastu, rahajeng semeng!", tts, size = 34)
                }
                Text("🎤 Speech check: Indonesian ASR — " +
                    (if (asr?.available() == true) "ready" else "unavailable on this device"),
                    fontSize = 12.sp, color = InkSoft)
                Text("🎵 UI sounds — tap to test (all normalized to the same volume)",
                    fontSize = 12.sp, color = InkSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "Click" to SoundFx.Sound.CLICK,
                        "Correct" to SoundFx.Sound.CORRECT,
                        "Wrong" to SoundFx.Sound.WRONG,
                        "Complete" to SoundFx.Sound.COMPLETE,
                        "Fail" to SoundFx.Sound.FAIL
                    ).forEach { (label, sound) ->
                        TextButton(onClick = { AppGraph.fx?.play(sound) }) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // share — renders a proper formatted picture card, not just text
        Surface(
            color = Gold.copy(alpha = 0.10f), shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().clickable {
                scope.launch {
                    val bmp = withContext(Dispatchers.Default) { ShareCards.progressCard(store, content) }
                    ShareCards.share(
                        context, bmp,
                        "I have a ${store.streak}-day streak and ${store.xp} XP on BasaBali — learning Balinese! 🌺"
                    )
                }
            }
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📤", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Share progress", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("A picture card of your streak, XP and words learned", fontSize = 12.sp, color = InkSoft)
                }
            }
        }

        // danger zone
        Surface(
            color = CreamCard, shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0x14000000)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                TextButton(onClick = onPlacement) {
                    Text("Retake placement test", fontSize = 13.sp)
                }
                TextButton(onClick = { showResetDialog = true }) {
                    Text("Reset all progress", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }

        Text(
            "BasaBali — 70 lessons · 5-unit path · 7,079 exercise sentences · 20 stories",
            fontSize = 11.sp, color = InkSoft,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all progress?") },
            text = { Text("This clears XP, streak, quests, league, stories, mistakes, practice days, path progress and every word/pattern SRS state on this device.") },
            confirmButton = {
                TextButton(onClick = { showResetDialog = false; onReset() }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Keep") } }
        )
    }
}

// ================================================================ shared bits

@Composable
private fun HeaderStat(icon: String, value: String, pulse: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "stat")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) 1.16f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "stat-pulse"
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
        Text(
            icon, fontSize = 16.sp,
            modifier = if (pulse) Modifier.graphicsLayer { scaleX = scale; scaleY = scale } else Modifier
        )
        Spacer(Modifier.width(3.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun MiniStat(big: String, small: String, modifier: Modifier = Modifier) {
    Surface(
        color = CreamCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0x14000000)),
        modifier = modifier
    ) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(big, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(small, fontSize = 10.sp, color = InkSoft)
        }
    }
}
