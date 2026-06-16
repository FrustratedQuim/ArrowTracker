package com.ratger.arrowtracker.tracker.display

internal data class Interpolation(
    val positionRotationTicks: Int,
    val transformationTicks: Int
)

internal object InterpolationProfiles {
    private val animation = Interpolation(
        positionRotationTicks = 1,
        transformationTicks = 1
    )
    private val activeMovement = Interpolation(
        positionRotationTicks = SyncPolicy.ACTIVE_UPDATE_INTERVAL_TICKS.toInt(),
        transformationTicks = SyncPolicy.ACTIVE_UPDATE_INTERVAL_TICKS.toInt()
    )
    private val snap = Interpolation(
        positionRotationTicks = 0,
        transformationTicks = 0
    )

    fun active(forceUpdate: Boolean, animating: Boolean): Interpolation {
        return if (forceUpdate || animating) {
            animation
        } else {
            activeMovement
        }
    }

    fun clearing(): Interpolation {
        return animation
    }

    fun snap(): Interpolation {
        return snap
    }
}
