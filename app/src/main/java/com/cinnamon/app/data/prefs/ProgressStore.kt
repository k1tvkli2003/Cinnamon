package com.cinnamon.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cinnamon.app.ui.theme.CinnamonThemes
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TimeZone

data class ProgressSnapshot(
    val xp: Int = 0,
    val streak: Int = 0,
    val streakAliveToday: Boolean = false,
    val dailyGoalXp: Int = 80,
    val xpToday: Int = 0,
    val reviewedToday: Int = 0,
    val weeklyXp: List<Int> = List(7) { 0 },   // oldest → today
    val theme: String = CinnamonThemes.TOASTED,
    val learningFocus: String = "None",
    val soundEffectsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val reduceMotion: Boolean = false
)

private val Context.progressDataStore by preferencesDataStore(name = "cinnamon_progress")

/**
 * Legacy-progress import bridge plus presentation preferences. XP, review
 * counts, and streaks are now derived from the immutable Room reward ledger.
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
        val LEARNING_FOCUS = stringPreferencesKey("learning_focus")
        val SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val SEEDED_VERSION = intPreferencesKey("seeded_version")
    }

    val snapshot: Flow<ProgressSnapshot> = appContext.progressDataStore.data.map { prefs ->
        val today = localEpochDay()
        val history = parseHistory(prefs[Keys.HISTORY] ?: "")
        ProgressSnapshot(
            xp = prefs[Keys.XP] ?: 0,
            streak = prefs[Keys.STREAK] ?: 0,
            streakAliveToday = (prefs[Keys.LAST_STUDY_DAY] ?: 0L) == today,
            dailyGoalXp = (prefs[Keys.DAILY_GOAL] ?: 80).coerceIn(20, 160),
            xpToday = history[today] ?: 0,
            reviewedToday = if ((prefs[Keys.REVIEWED_DAY] ?: 0L) == today) prefs[Keys.REVIEWED_TODAY] ?: 0 else 0,
            weeklyXp = (6 downTo 0).map { offset -> history[today - offset] ?: 0 },
            theme = prefs[Keys.THEME] ?: CinnamonThemes.TOASTED,
            learningFocus = prefs[Keys.LEARNING_FOCUS] ?: "None",
            soundEffectsEnabled = prefs[Keys.SOUND_EFFECTS_ENABLED] ?: true,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK_ENABLED] ?: true,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false
        )
    }

    suspend fun setTheme(name: String) {
        appContext.progressDataStore.edit { it[Keys.THEME] = name }
    }

    suspend fun setDailyGoal(xp: Int) {
        // Keep the aspirational target attainable under the reward engine's daily XP cap.
        appContext.progressDataStore.edit { it[Keys.DAILY_GOAL] = xp.coerceIn(20, 160) }
    }

    suspend fun setLearningFocus(focus: String) {
        require(focus in setOf("None", "Cardiology", "Neurology")) { "Unsupported learning focus" }
        appContext.progressDataStore.edit { it[Keys.LEARNING_FOCUS] = focus }
    }

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        appContext.progressDataStore.edit { it[Keys.SOUND_EFFECTS_ENABLED] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        appContext.progressDataStore.edit { it[Keys.HAPTIC_FEEDBACK_ENABLED] = enabled }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        appContext.progressDataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
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

        /**
         * Emits immediately and then at each local midnight while collected.
         * Ledger facts remain immutable; this only keeps date-derived read models
         * honest when the app stays open across a day boundary.
         */
        fun observeLocalEpochDay(): Flow<Long> = flow {
            while (currentCoroutineContext().isActive) {
                val now = System.currentTimeMillis()
                emit(localEpochDay(now))
                delay(millisUntilNextLocalDay(now))
            }
        }.distinctUntilChanged()

        /** Calendar day in the device's timezone (no java.time below minSdk 26). */
        fun localEpochDay(
            atEpochMillis: Long = System.currentTimeMillis(),
            timeZone: TimeZone = TimeZone.getDefault()
        ): Long {
            return (atEpochMillis + timeZone.getOffset(atEpochMillis)) / 86_400_000L
        }

        /**
         * Uses Calendar rather than a fixed day length so a daylight-saving
         * transition still wakes the observer at the following local midnight.
         */
        internal fun millisUntilNextLocalDay(
            atEpochMillis: Long = System.currentTimeMillis(),
            timeZone: TimeZone = TimeZone.getDefault()
        ): Long {
            val nextMidnightMillis = Calendar.getInstance(timeZone).run {
                timeInMillis = atEpochMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
                timeInMillis
            }
            return (nextMidnightMillis - atEpochMillis).coerceAtLeast(1L)
        }
    }
}
