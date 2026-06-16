package com.ratger.arrowtracker.api

/**
 * Exact horizontal target coordinates used by ArrowTracker.
 */
data class TrackerTarget(
    val x: Double,
    val z: Double
) {
    init {
        require(x.isFinite()) { "Tracker target x must be finite." }
        require(z.isFinite()) { "Tracker target z must be finite." }
    }
}
