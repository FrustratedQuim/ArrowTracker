package com.ratger.arrowtracker.tracker.display

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import kotlin.math.round

internal object RenderStyle {
    // Reserved technical glyph block: U+F700-U+F70F.
    private const val TECHNICAL_GLYPH_TRACKER_ARROW = "\uF700"

    const val CULLING_SIZE = 4.0f
    const val VIEW_RANGE = 32.0f
    const val TEXT_LINE_WIDTH = 200
    const val BACKGROUND_COLOR = 0
    const val MIN_VISIBLE_SCALE_MULTIPLIER = 0.01f
    const val MIN_VISIBLE_OPACITY_MULTIPLIER = 0.0001f
    const val MIN_RENDERABLE_OPACITY_INT = 26
    const val MIN_RENDERABLE_OPACITY: Byte = MIN_RENDERABLE_OPACITY_INT.toByte()

    val TRACKER_COMPONENT: Component = Component.text(TECHNICAL_GLYPH_TRACKER_ARROW, TextColor.color(0xFFFFFF))

    fun resolveTextOpacity(opacityMultiplier: Float): Byte {
        val opacity = MIN_RENDERABLE_OPACITY_INT +
            round((255 - MIN_RENDERABLE_OPACITY_INT) * opacityMultiplier).toInt()
        return opacity.coerceIn(MIN_RENDERABLE_OPACITY_INT, 255).toByte()
    }
}
