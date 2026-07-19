package com.cinnamon.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastContractTest {

    @Test
    fun `warm cream critical body-text pairs meet the AA contrast floor`() {
        assertAtLeastAa(WarmInk, Oatmeal)
        assertAtLeastAa(WarmInk, ClayErrorContainer)
        assertAtLeastAa(OnCinnamonBark, ClayError)
    }

    private fun assertAtLeastAa(foreground: Color, background: Color) {
        val lighter = maxOf(foreground.relativeLuminance(), background.relativeLuminance())
        val darker = minOf(foreground.relativeLuminance(), background.relativeLuminance())
        assertTrue("Expected AA contrast, was ${(lighter + 0.05) / (darker + 0.05)}", (lighter + 0.05) / (darker + 0.05) >= 4.5)
    }

    private fun Color.relativeLuminance(): Double =
        (0.2126 * red.linearized()) + (0.7152 * green.linearized()) + (0.0722 * blue.linearized())

    private fun Float.linearized(): Double {
        val value = toDouble()
        return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
    }
}
