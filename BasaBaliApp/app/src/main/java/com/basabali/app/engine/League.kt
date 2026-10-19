package com.basabali.app.engine

import com.basabali.app.data.RivalState
import com.basabali.app.data.UserStore
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Offline weekly league. Rivals are independent simulated learners, each with a
 * study profile — a serious learner doing several hours a day, a casual player
 * checking in for fifteen minutes, a weekend-only player, a streak keeper —
 * and each studies in a few sessions per day at deterministic pseudo-random
 * times. XP lands in bursts as the phone clock passes each session, so the
 * board moves in real time, scores are *not* tied to the learner's XP, and the
 * learner can genuinely rank up (or slip down) depending on how much they play.
 */
object LeagueEngine {
    val TIERS = listOf("Bronze", "Silver", "Gold", "Sapphire", "Emerald", "Ruby")
    const val USER = "Tiang (You)"
    private const val WEEK_MS = 7L * Srs.DAY_MS

    /**
     * A simulated-learner archetype: sessions per study day, XP per session and
     * the chance of studying on an ordinary weekday (weekends always count).
     */
    class Profile(
        val label: String,
        val minSessions: Int,
        val maxSessions: Int,
        val minXp: Int,
        val maxXp: Int,
        val weekdayChance: Float
    )

    val PROFILES = listOf(
        Profile("serious · 3 h/day", 4, 6, 26, 44, 1.00f),
        Profile("serious · 2 h/day", 3, 5, 22, 38, 1.00f),
        Profile("regular · 1 h/day", 2, 4, 16, 30, 0.95f),
        Profile("regular · 45 min", 2, 3, 14, 26, 0.90f),
        Profile("casual · 15 min/day", 1, 2, 10, 20, 0.85f),
        Profile("casual · 10 min/day", 1, 1, 8, 16, 0.80f),
        Profile("weekend learner", 1, 3, 12, 24, 0.30f),
        Profile("streak keeper · 5 min", 1, 1, 6, 12, 1.00f)
    )

    private val RIVAL_NAMES = listOf(
        "Wayan Sudira", "Made Ari", "Nyoman Suardi", "Ketut Rai", "Putu Oka",
        "Kadek Yuni", "Gede Bagus", "Komang Ayu", "Ayu Saraswati", "Sri Wahyuni",
        "Dewa Agung", "Nyoman Weda", "Ketut Suri", "Made Lenyot", "Wayan Gobleg"
    )

    fun profileOf(r: RivalState): Profile = PROFILES[r.profile.coerceIn(0, PROFILES.lastIndex)]

    fun profileLabel(r: RivalState): String = profileOf(r).label

    fun ensureActive(store: UserStore, now: Long) {
        val lg = store.league
        if (lg.weekStart <= 0L || now - lg.weekStart >= WEEK_MS) {
            if (lg.weekStart > 0L) settle(store, now)
            startWeek(store, now)
        }
    }

    private fun settle(store: UserStore, now: Long) {
        val board = standings(store, now)
        val myRank = board.indexOfFirst { it.first == USER } + 1
        store.leagueLastRank = myRank
        val lg = store.league
        when {
            myRank in 1..3 -> {
                lg.tier = min(lg.tier + 1, TIERS.lastIndex)
                store.leaguePromoted = 1
            }
            myRank >= board.size - 1 -> {
                lg.tier = max(lg.tier - 1, 0)
                store.leaguePromoted = 0
            }
            else -> store.leaguePromoted = -1
        }
    }

    private fun startWeek(store: UserStore, now: Long) {
        val r = Random(now)
        val lg = store.league
        lg.weekStart = now
        lg.userXp = 0
        lg.rivals.clear()
        // one of nearly every archetype, so the table mixes grinders, regulars and casuals
        val profiles = (PROFILES.indices + PROFILES.indices).shuffled(r).take(9)
        for ((i, n) in RIVAL_NAMES.shuffled(r).take(9).withIndex()) {
            lg.rivals += RivalState(n, profiles[i])
        }
    }

    /**
     * XP accrued by [r] so far this week: for every elapsed day the rival studies
     * (unless a casual skips a weekday) in a few sessions at deterministic times
     * between 06:00 and 21:50; a session's XP lands once its time has passed.
     */
    fun rivalXp(r: RivalState, weekStart: Long, now: Long): Int {
        if (weekStart <= 0L || now <= weekStart) return 0
        val prof = profileOf(r)
        var total = 0
        val elapsedDays = (now - weekStart).toDouble() / Srs.DAY_MS
        val days = min(7, kotlin.math.ceil(elapsedDays).toInt())
        for (d in 0 until days) {
            val rnd = Random(hash(r.name, weekStart.toInt(), d))
            val epochDay = weekStart / Srs.DAY_MS + d
            val dow = ((epochDay + 3) % 7).toInt()          // 0 = Monday … 5,6 = weekend
            val weekend = dow >= 5
            if (!weekend && rnd.nextFloat() > prof.weekdayChance) continue
            val sessions = prof.minSessions + rnd.nextInt(prof.maxSessions - prof.minSessions + 1)
            for (k in 0 until sessions) {
                val frac = 0.25f + rnd.nextFloat() * 0.66f   // 06:00 … 21:50
                val sessionAt = weekStart + ((d + frac) * Srs.DAY_MS).toLong()
                if (sessionAt > now) continue
                total += prof.minXp + rnd.nextInt(prof.maxXp - prof.minXp + 1)
            }
        }
        return total
    }

    private fun hash(name: String, salt: Int, day: Int): Int {
        var h = salt * 1_000_003 + 17
        for (c in name) h = h * 31 + c.code
        h = h * 31 + day
        return h and 0x7fffffff
    }

    fun standings(store: UserStore, now: Long): List<Pair<String, Int>> {
        val lg = store.league
        val list = lg.rivals.map { it.name to rivalXp(it, lg.weekStart, now) }.toMutableList()
        list += USER to lg.userXp
        list.sortByDescending { it.second }
        return list
    }

    fun daysLeft(store: UserStore, now: Long): Int {
        val end = store.league.weekStart + WEEK_MS
        return max(0, ((end - now) / Srs.DAY_MS).toInt() + 1)
    }

    fun tierName(store: UserStore): String = TIERS[store.league.tier.coerceIn(0, TIERS.lastIndex)]
}
