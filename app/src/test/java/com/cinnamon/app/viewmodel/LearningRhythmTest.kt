package com.cinnamon.app.viewmodel

import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningRhythmTest {

    @Test
    fun `local epoch days keep the same ISO week across host timezone and locale`() {
        val originalTimeZone = TimeZone.getDefault()
        val originalLocale = Locale.getDefault()
        val studyDays = listOf(
            LocalDate.of(2025, 12, 29).toEpochDay(),
            LocalDate.of(2025, 12, 30).toEpochDay(),
            LocalDate.of(2025, 12, 31).toEpochDay(),
            LocalDate.of(2025, 12, 31).toEpochDay(), // a duplicate event day must not inflate progress
            LocalDate.of(2026, 1, 5).toEpochDay(),
            LocalDate.of(2026, 1, 6).toEpochDay()
        )

        try {
            val results = listOf(
                TimeZone.getTimeZone("America/Los_Angeles") to Locale.FRANCE,
                TimeZone.getTimeZone("Pacific/Kiritimati") to Locale.US
            ).map { (timeZone, locale) ->
                TimeZone.setDefault(timeZone)
                Locale.setDefault(locale)
                weeksWithAtLeastThreeStudyDays(studyDays)
            }

            assertEquals(listOf(1, 1), results)
        } finally {
            TimeZone.setDefault(originalTimeZone)
            Locale.setDefault(originalLocale)
        }
    }
}
