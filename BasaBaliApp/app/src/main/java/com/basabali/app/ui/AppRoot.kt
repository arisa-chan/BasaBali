package com.basabali.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basabali.app.AppGraph
import com.basabali.app.data.NodeKind
import com.basabali.app.data.PathNode
import com.basabali.app.engine.Exercise
import com.basabali.app.ui.lesson.LessonScreen
import com.basabali.app.ui.lesson.SessionRunner
import com.basabali.app.ui.theme.Gold
import com.basabali.app.ui.theme.InkSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Screen {
    object Main : Screen()
    object Placement : Screen()
    data class Session(val kind: SessionKind) : Screen()
    data class Complete(val result: SessionRunner.SessionResult, val kind: SessionKind) : Screen()
    data class StoryRoute(val storyId: String) : Screen()
}

sealed class SessionKind {
    /** A node on the 5-unit lesson path (learn / reinforce / review). */
    data class Node(val node: PathNode) : SessionKind()
    /** Due-based SRS practice from the Review tab. */
    object Review : SessionKind()
    /** Re-drill of the mistake log from the Review tab. */
    object Mistakes : SessionKind()
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { AppGraph.init(ctx) }
        ready = true
    }

    if (!ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Gold)
                Spacer(Modifier.height(14.dp))
                Text("Preparing the course…", color = InkSoft, fontSize = 13.sp)
            }
        }
        return
    }

    var screen by remember {
        mutableStateOf<Screen>(if (AppGraph.store.placementDone) Screen.Main else Screen.Placement)
    }

    when (val sc = screen) {
        Screen.Main -> MainScreen(
            content = AppGraph.content,
            store = AppGraph.store,
            builder = AppGraph.builder,
            tts = AppGraph.tts,
            asr = AppGraph.asr,
            onStartSession = { kind -> screen = Screen.Session(kind) },
            onStory = { story -> screen = Screen.StoryRoute(story.id) },
            onPlacement = { screen = Screen.Placement },
            onReset = { AppGraph.store.reset() }
        )

        Screen.Placement -> PlacementScreen(
            builder = AppGraph.builder,
            tts = AppGraph.tts,
            onFinished = {
                AppGraph.store.placementDone = true
                AppGraph.store.save()
                screen = Screen.Main
            },
            onSkip = {
                AppGraph.store.placementDone = true
                AppGraph.store.save()
                screen = Screen.Main
            }
        )

        is Screen.StoryRoute -> {
            val story = AppGraph.content.storyById(sc.storyId)
            if (story == null) {
                LaunchedEffect(Unit) { screen = Screen.Main }
            } else {
                StoryScreen(
                    story = story,
                    content = AppGraph.content,
                    store = AppGraph.store,
                    tts = AppGraph.tts,
                    onDone = { _, _ -> screen = Screen.Main },
                    onExit = { screen = Screen.Main }
                )
            }
        }

        is Screen.Session -> SessionHost(
            kind = sc.kind,
            onDone = { result -> screen = Screen.Complete(result, sc.kind) },
            onExit = { screen = Screen.Main }
        )

        is Screen.Complete -> CompleteScreen(
            result = sc.result,
            canRetry = true,
            onContinue = { screen = Screen.Main },
            onRetry = { screen = Screen.Session(sc.kind) }
        )
    }
}

@Composable
private fun SessionHost(
    kind: SessionKind,
    onDone: (SessionRunner.SessionResult) -> Unit,
    onExit: () -> Unit
) {
    // Format unlocks follow the unit being played: listening & speaking from
    // U1, fill-blank & register choice from U2, dictation & free typing from
    // U3. Review-hub sessions use the highest unit the learner has reached.
    val caps = remember(kind) {
        val unit = when (kind) {
            is SessionKind.Node -> kind.node.unit
            else -> AppGraph.store.maxUnitReached()
        }
        AppGraph.builder.caps(
            tts = AppGraph.tts?.available == true,
            asr = AppGraph.asr?.available() == true,
            unit = unit
        )
    }
    // Building a session walks the sentence bank and ranks distractors.  Do it
    // off the UI thread and show progress: tapping a path bubble used to freeze
    // the screen for seconds with no feedback.
    var exercises by remember(kind) { mutableStateOf<List<Exercise>?>(null) }
    LaunchedEffect(kind) {
        exercises = withContext(Dispatchers.Default) {
            when (kind) {
                is SessionKind.Node -> when (val k = kind.node.kind) {
                    is NodeKind.Learn -> AppGraph.builder.newChunkSession(k.chunk, caps)
                    is NodeKind.Reinforce -> AppGraph.builder.reinforceChunkSession(k.chunk, caps)
                    is NodeKind.Cumulative -> AppGraph.builder.cumulativeReviewSession(k.upToLesson, k.slots, caps)
                    is NodeKind.LessonReview -> AppGraph.builder.lessonReviewSession(k.lessonId, k.slots, caps)
                }
                SessionKind.Review -> AppGraph.builder.reviewSession(caps)
                SessionKind.Mistakes -> AppGraph.builder.mistakeSession(caps)
            }
        }
    }

    val built = exercises
    if (built == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Gold)
                Spacer(Modifier.height(14.dp))
                Text("Building this exercise set…", color = InkSoft, fontSize = 13.sp)
            }
        }
        return
    }

    if (built.isEmpty()) {
        LaunchedEffect(Unit) {
            // never soft-lock a path node or review action that cannot generate exercises
            if (kind is SessionKind.Node) {
                AppGraph.store.doneChunks.add(kind.node.id)
                AppGraph.store.save()
            }
            onExit()
        }
        return
    }

    val runner = remember(built) {
        SessionRunner(
            initialExercises = built,
            builder = AppGraph.builder,
            caps = caps,
            content = AppGraph.content,
            store = AppGraph.store,
            nodeId = (kind as? SessionKind.Node)?.node?.id,
            onFinish = onDone
        )
    }

    LessonScreen(
        runner = runner,
        content = AppGraph.content,
        tts = AppGraph.tts,
        asr = AppGraph.asr,
        onExit = onExit
    )
}
