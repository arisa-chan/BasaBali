package com.basabali.app.engine

import java.time.LocalDate
import kotlin.random.Random

/** Daily quests, deterministic per calendar day (seeded by the date). */
data class QuestDef(val key: String, val goal: Int, val reward: Int) {
    fun label(): String = when (key) {
        "xp" -> "Earn $goal XP"
        "lessons" -> "Complete $goal lesson${if (goal == 1) "" else "s"}"
        "perfect" -> "Answer $goal exercises perfectly"
        else -> key
    }
}

object Quests {
    fun dayKey(): String = LocalDate.now().toString()

    fun forDay(dayKey: String): List<QuestDef> {
        val r = Random(dayKey.hashCode().toLong())
        return listOf(
            QuestDef("xp", listOf(20, 40, 60)[r.nextInt(3)], 10),
            QuestDef("lessons", listOf(1, 2, 3)[r.nextInt(3)], 10),
            QuestDef("perfect", listOf(5, 8, 12)[r.nextInt(3)], 10)
        ).shuffled(r)
    }
}
