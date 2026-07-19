package com.cinnamon.app.data.prefs

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressStoreDayBoundaryTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun localEpochDay_usesTheProvidedTimezone() {
        val thirtyMinutesBeforeUtcMidnight = 86_400_000L - 1_800_000L
        val utcPlusOne = TimeZone.getTimeZone("GMT+01:00")

        assertEquals(0L, ProgressStore.localEpochDay(thirtyMinutesBeforeUtcMidnight, utc))
        assertEquals(1L, ProgressStore.localEpochDay(thirtyMinutesBeforeUtcMidnight, utcPlusOne))
    }

    @Test
    fun delayUntilNextLocalDay_targetsTheNextMidnight() {
        val almostMidnight = utcMillis(year = 2026, month = Calendar.JULY, day = 18, hour = 23, minute = 59, second = 58)

        assertEquals(2_000L, ProgressStore.millisUntilNextLocalDay(almostMidnight, utc))
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int
    ): Long = Calendar.getInstance(utc).run {
        clear()
        set(year, month, day, hour, minute, second)
        timeInMillis
    }
}
