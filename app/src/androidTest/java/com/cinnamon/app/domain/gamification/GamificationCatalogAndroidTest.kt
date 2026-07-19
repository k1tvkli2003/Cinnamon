package com.cinnamon.app.domain.gamification

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Guards against JVM-only regex assumptions in the bundled on-device catalog. */
@RunWith(AndroidJUnit4::class)
class GamificationCatalogAndroidTest {
    @Test
    fun bundledCatalog_validatesWithAndroidRegexEngine() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bundle = GamificationCatalogLoader().loadValidated(
            catalogJson = context.assets.open("gamification/catalog-v1.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() },
            copyJson = context.assets.open("gamification/copy-en-v1.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        )

        val report = GamificationCatalogValidator.validate(bundle)
        assertTrue(report.errors.joinToString { it.code + ":" + it.path }, report.isValid)
    }
}
