package com.basabali.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basabali.app.AppGraph
import com.basabali.app.audio.SoundFx
import com.basabali.app.ui.lesson.SessionRunner
import com.basabali.app.ui.theme.Gold
import com.basabali.app.ui.theme.GreenOk
import com.basabali.app.ui.theme.Ink
import com.basabali.app.ui.theme.InkSoft
import com.basabali.app.ui.theme.RedBad

@Composable
fun CompleteScreen(
    result: SessionRunner.SessionResult,
    canRetry: Boolean,
    onContinue: () -> Unit,
    onRetry: () -> Unit
) {
    val acc = if (result.total > 0) (result.correct * 100 / result.total) else 0

    // fanfare when the subunit is finished; sad cadence when hearts ran out
    LaunchedEffect(Unit) {
        AppGraph.fx?.play(if (result.failed) SoundFx.Sound.FAIL else SoundFx.Sound.COMPLETE)
    }

    // the mascot celebrates (or sympathises) and the XP number counts up
    val bob by rememberInfiniteTransition(label = "complete").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val xpShown by animateIntAsState(
        targetValue = result.xp,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "xp"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (result.failed) "💔" else "🎉",
            fontSize = 44.sp,
            modifier = Modifier.graphicsLayer { translationY = bob * -7f }
        )
        Spacer(Modifier.height(4.dp))
        BudayaAvatar(
            mood = if (result.failed) AvatarArt.ENCOURAGING else AvatarArt.HAPPY,
            avatarSize = 104.dp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (result.failed) "Kehabisan hati — out of hearts!" else "Puput! Lesson complete!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (result.failed) RedBad else GreenOk,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (result.failed) "Mistakes shorten the memory — try the session again."
            else if (result.streakUp) "Streak up: ${result.streak} day${if (result.streak == 1) "" else "s"} 🔥"
            else "Keep the streak alive tomorrow 🔥",
            fontSize = 14.sp,
            color = InkSoft,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatPill("⭐ +$xpShown", "XP earned")
            StatPill("${acc}%", "accuracy")
            StatPill("${result.correct}/${result.total}", "correct")
        }

        Spacer(Modifier.height(36.dp))

        if (result.failed && canRetry) {
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Try again", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(
            onClick = onContinue,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (result.failed) "Back to path" else "Continue", fontSize = 16.sp) }
    }
}

@Composable
private fun StatPill(big: String, small: String) {
    Surface(
        color = Gold.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.4f))
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(big, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(small, fontSize = 11.sp, color = InkSoft)
        }
    }
}
