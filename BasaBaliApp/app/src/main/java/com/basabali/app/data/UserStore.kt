package com.basabali.app.data

import android.content.Context
import com.basabali.app.engine.Quests
import com.basabali.app.engine.SrsState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * All persisted learner state: the two SRS stores (words + grammar patterns),
 * path progress, XP, streak, sentence-exposure log (rotation), quests, league,
 * stories, placement and scheduler preference. One JSON file in filesDir.
 */
class UserStore(private val context: Context) {

    val words = HashMap<String, SrsState>()
    val patterns = HashMap<String, SrsState>()
    val doneChunks = HashSet<String>()
    val seenSentences = HashMap<String, Long>()   // sentenceId -> last shown (ms)

    var xp: Int = 0
    var streak: Int = 0
    var lastDayKey: String = ""                   // yyyy-MM-dd

    // ---- v2 ----
    var schedulerMode: Int = 0                    // 0 = Leitner, 1 = FSRS
    var placementDone: Boolean = false
    val completedStories = HashSet<String>()
    val league = LeagueData()
    var leagueLastRank: Int = 0                   // 0 = not settled yet
    var leaguePromoted: Int = -1                  // 1 promoted, 0 demoted, -1 stayed
    var questDay: String = ""
    var questProg: MutableList<Int> = mutableListOf(0, 0, 0)
    var questBonusClaimed: Boolean = false

    // ---- v4 ----
    /** sentenceId -> times missed and not yet answered correctly again. */
    val mistakes = HashMap<String, Int>()
    /** yyyy-MM-dd keys of days with at least one finished session/story. */
    val practiceDays = HashSet<String>()

    fun wordState(id: String): SrsState = words.getOrPut(id) { SrsState() }

    /** Patterns start one box up: they decay slower than single words. */
    fun patternState(id: String): SrsState = patterns.getOrPut(id) { SrsState(box = 1) }

    fun isTaught(wordId: String): Boolean = (words[wordId]?.lastReview ?: 0L) > 0L

    fun taughtWordCount(): Int = words.values.count { it.lastReview > 0L }

    /** Completed LEARN subunits (Unit 1/2 nodes) — gates audio/typing unlocks. */
    fun learnedCount(): Int = doneChunks.count { it.contains("U1S") || it.contains("U2S") }

    /** Highest unit number the learner has ever completed a subunit in (>= 1). */
    fun maxUnitReached(): Int {
        var m = 1
        val re = Regex("U(\\d)S")
        for (id in doneChunks) {
            val g = re.find(id)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            if (g > m) m = g
        }
        return m
    }

    /** Record a miss; persisted immediately so the Review tab sees it right away. */
    fun noteMistake(sentenceId: String) {
        mistakes[sentenceId] = (mistakes[sentenceId] ?: 0) + 1
        save()
    }

    fun clearMistake(sentenceId: String) {
        if (mistakes.remove(sentenceId) != null) save()
    }

    fun touchSeen(sentenceId: String, now: Long) {
        seenSentences[sentenceId] = now
    }

    fun questDefs(): List<com.basabali.app.engine.QuestDef> =
        if (questDay.isEmpty()) emptyList() else Quests.forDay(questDay)

    /**
     * Called once per finished (non-failed) session or story. Feeds league XP and quest
     * progress, marks the practice calendar and advances the streak; auto-awards the
     * quest bonus once per day. Returns bonus XP.
     */
    fun recordSession(xpGain: Int, lessonsCompleted: Int, perfectAnswers: Int): Int {
        league.userXp += xpGain

        val today = Quests.dayKey()
        practiceDays += today
        // the streak counts every practice day (lesson sessions and stories alike),
        // so it always agrees with the calendar in the Quests tab
        if (lastDayKey != today) {
            val yesterday = LocalDate.now().minusDays(1).toString()
            streak = if (lastDayKey == yesterday) streak + 1 else 1
            lastDayKey = today
        }
        if (questDay != today) {
            questDay = today
            questProg = mutableListOf(0, 0, 0)
            questBonusClaimed = false
        }
        val defs = Quests.forDay(today)
        defs.forEachIndexed { i, d ->
            val add = when (d.key) {
                "xp" -> xpGain
                "lessons" -> lessonsCompleted
                "perfect" -> perfectAnswers
                else -> 0
            }
            while (questProg.size <= i) questProg += 0
            questProg[i] = questProg[i] + add
        }
        var bonus = 0
        if (!questBonusClaimed && defs.isNotEmpty() &&
            defs.indices.all { questProg.getOrElse(it) { 0 } >= defs[it].goal }
        ) {
            questBonusClaimed = true
            bonus = defs.sumOf { it.reward }
            xp += bonus
        }
        return bonus
    }

    // ------------------------------------------------------------------ io

    private fun file(): File = File(context.filesDir, "user_state.json")

    fun save() {
        try {
            val root = JSONObject()
            root.put("v", 2)
            root.put("xp", xp)
            root.put("streak", streak)
            root.put("lastDayKey", lastDayKey)
            root.put("sched", schedulerMode)
            root.put("placement", placementDone)
            root.put("doneChunks", JSONArray(doneChunks.toList()))
            root.put("completedStories", JSONArray(completedStories.toList()))
            root.put("words", statesJson(words))
            root.put("patterns", statesJson(patterns))
            val seen = JSONObject()
            for ((k, v) in seenSentences) seen.put(k, v)
            root.put("seen", seen)
            root.put("questDay", questDay)
            root.put("questProg", JSONArray(questProg.toList()))
            root.put("questClaimed", questBonusClaimed)
            val mk = JSONObject()
            for ((k, v) in mistakes) mk.put(k, v)
            root.put("mistakes", mk)
            root.put("practiceDays", JSONArray(practiceDays.toList()))

            val lg = JSONObject()
            lg.put("tier", league.tier)
            lg.put("weekStart", league.weekStart)
            lg.put("userXp", league.userXp)
            lg.put("lastRank", leagueLastRank)
            lg.put("promoted", leaguePromoted)
            val rivals = JSONArray()
            for (r in league.rivals) {
                val ro = JSONObject()
                ro.put("n", r.name)
                ro.put("p", r.profile)
                rivals.put(ro)
            }
            lg.put("rivals", rivals)
            root.put("league", lg)

            file().writeText(root.toString())
        } catch (_: Exception) {
            // never crash the session on persistence problems
        }
    }

    fun load() {
        try {
            val f = file()
            if (!f.exists()) return
            val root = JSONObject(f.readText())
            xp = root.optInt("xp", 0)
            streak = root.optInt("streak", 0)
            lastDayKey = root.optString("lastDayKey", "")
            schedulerMode = root.optInt("sched", 0)
            placementDone = root.optBoolean("placement", false)
            doneChunks.clear()
            val dc = root.optJSONArray("doneChunks")
            if (dc != null) for (i in 0 until dc.length()) {
                val id = dc.getString(i)
                // migrate pre-unit ids "L12C3" -> learn-unit node "L12U1S3"
                val m = Regex("^L(\\d+)C(\\d+)$").matchEntire(id)
                doneChunks += if (m != null) "L${m.groupValues[1]}U1S${m.groupValues[2]}" else id
            }
            completedStories.clear()
            val cs = root.optJSONArray("completedStories")
            if (cs != null) for (i in 0 until cs.length()) completedStories += cs.getString(i)
            loadStates(root.optJSONObject("words"), words, 0)
            loadStates(root.optJSONObject("patterns"), patterns, 1)
            seenSentences.clear()
            val seen = root.optJSONObject("seen")
            if (seen != null) {
                val it = seen.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    seenSentences[k] = seen.optLong(k, 0L)
                }
            }
            mistakes.clear()
            val mk = root.optJSONObject("mistakes")
            if (mk != null) {
                val mit = mk.keys()
                while (mit.hasNext()) {
                    val k = mit.next()
                    mistakes[k] = mk.optInt(k, 1)
                }
            }
            practiceDays.clear()
            val pd = root.optJSONArray("practiceDays")
            if (pd != null) for (i in 0 until pd.length()) practiceDays += pd.getString(i)

            questDay = root.optString("questDay", "")
            questBonusClaimed = root.optBoolean("questClaimed", false)
            questProg = mutableListOf(0, 0, 0)
            val qp = root.optJSONArray("questProg")
            if (qp != null) for (i in 0 until minOf(3, qp.length())) questProg[i] = qp.optInt(i, 0)

            val lg = root.optJSONObject("league")
            if (lg != null) {
                league.tier = lg.optInt("tier", 0)
                league.weekStart = lg.optLong("weekStart", 0L)
                league.userXp = lg.optInt("userXp", 0)
                leagueLastRank = lg.optInt("lastRank", 0)
                leaguePromoted = lg.optInt("promoted", -1)
                league.rivals.clear()
                val rv = lg.optJSONArray("rivals")
                if (rv != null) for (i in 0 until rv.length()) {
                    val ro = rv.optJSONObject(i) ?: continue
                    league.rivals += RivalState(ro.optString("n", "?"), ro.optInt("p", 0))
                }
            }
        } catch (_: Exception) {
            // corrupted file -> start fresh
        }
    }

    private fun statesJson(map: Map<String, SrsState>): JSONObject {
        val o = JSONObject()
        for ((k, v) in map) {
            val so = JSONObject()
            so.put("s", v.successes)
            so.put("l", v.lapses)
            so.put("b", v.box)
            so.put("t", v.lastReview)
            so.put("S", v.stability)
            so.put("D", v.difficulty)
            o.put(k, so)
        }
        return o
    }

    private fun loadStates(o: JSONObject?, into: HashMap<String, SrsState>, defaultBox: Int) {
        into.clear()
        if (o == null) return
        val it = o.keys()
        while (it.hasNext()) {
            val k = it.next()
            val so = o.optJSONObject(k) ?: continue
            into[k] = SrsState(
                successes = so.optInt("s", 0),
                lapses = so.optInt("l", 0),
                box = so.optInt("b", defaultBox),
                lastReview = so.optLong("t", 0L),
                stability = so.optDouble("S", 0.0),
                difficulty = so.optDouble("D", 0.0)
            )
        }
    }

    fun reset() {
        words.clear()
        patterns.clear()
        doneChunks.clear()
        seenSentences.clear()
        completedStories.clear()
        xp = 0
        streak = 0
        lastDayKey = ""
        placementDone = false
        questDay = ""
        questProg = mutableListOf(0, 0, 0)
        questBonusClaimed = false
        mistakes.clear()
        practiceDays.clear()
        league.tier = 0
        league.weekStart = 0L
        league.userXp = 0
        league.rivals.clear()
        leagueLastRank = 0
        leaguePromoted = -1
        save()
    }
}
