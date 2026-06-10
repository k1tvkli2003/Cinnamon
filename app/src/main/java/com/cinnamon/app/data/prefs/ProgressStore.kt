package com.cinnamon.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cinnamon.app.ui.theme.CinnamonThemes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.TimeZone

data class ProgressSnapshot(
    val xp: Int = 0,
    val streak: Int = 0,
    val streakAliveToday: Boolean = false,
    val dailyGoalXp: Int = 80,
    val xpToday: Int = 0,
    val reviewedToday: Int = 0,
    val weeklyXp: List<Int> = List(7) { 0 },   // oldest → today
    val theme: String = CinnamonThemes.TOASTED
)

private val Context.progressDataStore by preferencesDataStore(name = "cinnamon_progress")

/**
 * Single source of truth for learner progress: XP, real date-based streak,
 * daily goal, per-day history, and the selected theme.
 */
class ProgressStore private constructor(private val appContext: Context) {

    private object Keys {
        val XP = intPreferencesKey("xp")
        val STREAK = intPreferencesKey("streak")
        val LAST_STUDY_DAY = longPreferencesKey("last_study_day")
        val DAILY_GOAL = intPreferencesKey("daily_goal_xp")
        val REVIEWED_TODAY = intPreferencesKey("reviewed_today")
        val REVIEWED_DAY = longPreferencesKey("reviewed_day")
        val HISTORY = stringPreferencesKey("xp_history")     // "epochDay:xp,epochDay:xp"
        val THEME = stringPreferencesKey("theme")
        val SEEDED_VERSION = intPreferencesKey("seeded_version")
    }

    val snapshot: Flow<ProgressSnapshot> = appContext.progressDataStore.data.map { prefs ->
        val today = localEpochDay()
        val history = parseHistory(prefs[Keys.HISTORY] ?: "")
        ProgressSnapshot(
            xp = prefs[Keys.XP] ?: 0,
            streak = prefs[Keys.STREAK] ?: 0,
            streakAliveToday = (prefs[Keys.LAST_STUDY_DAY] ?: 0L) == today,
            dailyGoalXp = prefs[Keys.DAILY_GOAL] ?: 80,
            xpToday = history[today] ?: 0,
            reviewedToday = if ((prefs[Keys.REVIEWED_DAY] ?: 0L) == today) prefs[Keys.REVIEWED_TODAY] ?: 0 else 0,
            weeklyXp = (6 downTo 0).map { offset -> history[today - offset] ?: 0 },
            theme = prefs[Keys.THEME] ?: CinnamonThemes.TOASTED
        )
    }

    suspend fun addXp(amount: Int) {
        if (amount <= 0) return
        appContext.progressDataStore.edit { prefs ->
            val today = localEpochDay()
            prefs[Keys.XP] = (prefs[Keys.XP] ?: 0) + amount

            val history = parseHistory(prefs[Keys.HISTORY] ?: "").toMutableMap()
            history[today] = (history[today] ?: 0) + amount
            prefs[Keys.HISTORY] = history.entries
                .sortedBy { it.key }
                .takeLast(14)
                .joinToString(",") { "${it.key}:${it.value}" }

            val lastDay = prefs[Keys.LAST_STUDY_DAY] ?: 0L
            when {
                lastDay == today -> Unit
                lastDay == today - 1 -> prefs[Keys.STREAK] = (prefs[Keys.STREAK] ?: 0) + 1
                else -> prefs[Keys.STREAK] = 1
            }
            prefs[Keys.LAST_STUDY_DAY] = today
        }
    }

    suspend fun incrementReviewed() {
        appContext.progressDataStore.edit { prefs ->
            val today = localEpochDay()
            val sameDay = (prefs[Keys.REVIEWED_DAY] ?: 0L) == today
            prefs[Keys.REVIEWED_TODAY] = (if (sameDay) prefs[Keys.REVIEWED_TODAY] ?: 0 else 0) + 1
            prefs[Keys.REVIEWED_DAY] = today
        }
    }

    suspend fun setTheme(name: String) {
        appContext.progressDataStore.edit { it[Keys.THEME] = name }
    }

    suspend fun setDailyGoal(xp: Int) {
        appContext.progressDataStore.edit { it[Keys.DAILY_GOAL] = xp.coerceIn(20, 400) }
    }

    suspend fun seededVersionOnce(): Int =
        appContext.progressDataStore.data.first()[Keys.SEEDED_VERSION] ?: 0

    suspend fun setSeededVersion(version: Int) {
        appContext.progressDataStore.edit { it[Keys.SEEDED_VERSION] = version }
    }

    private fun parseHistory(raw: String): Map<Long, Int> =
        raw.split(',')
            .mapNotNull { token ->
                val parts = token.split(':')
                if (parts.size == 2) parts[0].toLongOrNull()?.let { day ->
                    parts[1].toIntOrNull()?.let { xp -> day to xp }
                } else null
            }
            .toMap()

    companion object {
        @Volatile
        private var INSTANCE: ProgressStore? = null

        fun getInstance(context: Context): ProgressStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProgressStore(context.applicationContext).also { INSTANCE = it }
            }

        /** Calendar day in the device's timezone (no java.time below minSdk 26). */
        fun localEpochDay(): Long {
            val now = System.currentTimeMillis()
            return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
        }
    }
}
