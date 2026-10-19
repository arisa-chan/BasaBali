package com.basabali.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basabali.app.data.UserStore
import com.basabali.app.engine.LeagueEngine
import com.basabali.app.ui.theme.CreamCard
import com.basabali.app.ui.theme.Gold
import com.basabali.app.ui.theme.GreenOk
import com.basabali.app.ui.theme.Ink
import com.basabali.app.ui.theme.InkSoft
import com.basabali.app.ui.theme.RedBad
import kotlinx.coroutines.delay

/**
 * Leaderboard tab content: weekly offline league with 9 generated rivals,
 * top-3 promotion, bottom-2 demotion. Story XP now feeds league.userXp too
 * (stories route through store.recordSession), so the promise below holds.
 */
@Composable
fun LeagueContent(store: UserStore) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        LeagueEngine.ensureActive(store, now)
        store.save()
        // live board: rivals gain XP in bursts, so keep the clock moving
        while (true) {
            delay(20_000)
            now = System.currentTimeMillis()
        }
    }
    val board = remember(store.league.userXp, store.league.weekStart, now) {
        LeagueEngine.standings(store, now)
    }
    val tierIdx = store.league.tier.coerceIn(0, LeagueEngine.TIERS.lastIndex)
    val tierEmoji = listOf("🥉", "🥈", "🥇", "💎", "🏅", "👑")[tierIdx]

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Surface(
            color = Gold.copy(alpha = 0.12f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Gold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tierEmoji, fontSize = 40.sp)
                Text("${LeagueEngine.TIERS[tierIdx]} League", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("${LeagueEngine.daysLeft(store, now)} day(s) left · top 3 promote, bottom 2 demote",
                    fontSize = 12.sp, color = InkSoft)
                if (store.leagueLastRank > 0) {
                    Spacer(Modifier.height(6.dp))
                    val msg = when (store.leaguePromoted) {
                        1 -> "Last week: rank #${store.leagueLastRank} — promoted! 🎉"
                        0 -> "Last week: rank #${store.leagueLastRank} — demoted, climb back!"
                        else -> "Last week: rank #${store.leagueLastRank} — stayed"
                    }
                    Text(msg, fontSize = 12.sp, color = InkSoft)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(board, key = { _, item -> item.first }) { i, (name, xp) ->
                val isUser = name == LeagueEngine.USER
                val profile = if (isUser) null
                    else store.league.rivals.firstOrNull { it.name == name }?.let { LeagueEngine.profileLabel(it) }
                val promo = i < 3
                val demo = i >= board.size - 2
                val bg = when {
                    isUser -> Gold.copy(alpha = 0.18f)
                    promo -> GreenOk.copy(alpha = 0.10f)
                    demo -> RedBad.copy(alpha = 0.08f)
                    else -> CreamCard
                }
                Surface(
                    color = bg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isUser) Gold.copy(alpha = 0.6f) else Color(0x14000000)),
                    modifier = Modifier.fillMaxWidth().animateItem()
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = if (promo) GreenOk else if (demo) RedBad else InkSoft,
                            modifier = Modifier.width(28.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontSize = 15.sp, color = Ink,
                                fontWeight = if (isUser) FontWeight.Bold else FontWeight.Normal)
                            Text(profile ?: "you", fontSize = 10.sp, color = InkSoft)
                        }
                        Text("$xp XP", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = if (isUser) Gold else InkSoft)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Earn XP by finishing lessons and stories this week — both count here.",
            fontSize = 12.sp, color = InkSoft)
    }
}
