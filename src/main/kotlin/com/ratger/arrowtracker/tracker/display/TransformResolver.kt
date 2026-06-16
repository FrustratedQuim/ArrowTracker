package com.ratger.arrowtracker.tracker.display

import com.github.retrooper.packetevents.util.Quaternion4f
import com.github.retrooper.packetevents.util.Vector3f
import kotlin.math.cos
import kotlin.math.sin

internal object TransformResolver {
    const val BASE_SCALE = 2.709375f

    private const val FLAT_X_ROTATION_RADIANS = (-Math.PI / 2.0).toFloat()
    private const val PASSENGER_TRANSLATION_X = -0.18f
    private const val PASSENGER_TRANSLATION_Y = -1.8f
    private const val PASSENGER_TRANSLATION_Z = -1.0f
    private const val SCALE_COMPENSATION_X = 0.18f
    private const val SCALE_COMPENSATION_Y = 0.08f
    private const val CENTERING_PIXEL_SHIFT_X = 0.09375f

    val IDENTITY_ROTATION = Quaternion4f(0.0f, 0.0f, 0.0f, 1.0f)
    val FLAT_ROTATION = flatRotation()

    fun resolveTranslation(scaleMultiplier: Float): Vector3f {
        val shrinkFactor = (1.0f - scaleMultiplier).coerceAtLeast(0.0f)
        return Vector3f(
            PASSENGER_TRANSLATION_X +
                SCALE_COMPENSATION_X * shrinkFactor +
                CENTERING_PIXEL_SHIFT_X * scaleMultiplier,
            PASSENGER_TRANSLATION_Y + SCALE_COMPENSATION_Y * shrinkFactor,
            PASSENGER_TRANSLATION_Z
        )
    }

    private fun flatRotation(): Quaternion4f {
        val halfRadians = FLAT_X_ROTATION_RADIANS / 2.0f
        return Quaternion4f(sin(halfRadians), 0.0f, 0.0f, cos(halfRadians))
    }
}
