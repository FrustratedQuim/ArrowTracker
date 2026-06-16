package com.ratger.arrowtracker.tracker.model

internal data class Target(
    val x: Double,
    val z: Double
) {
    init {
        require(x.isFinite()) { "Target x must be finite." }
        require(z.isFinite()) { "Target z must be finite." }
    }
}
