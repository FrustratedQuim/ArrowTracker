package com.ratger.arrowtracker.tracker.model

import me.tofaa.entitylib.wrapper.WrapperEntity

internal class Session(
    var target: Target,
    var display: WrapperEntity? = null,
    var activeTicks: Int = 0,
    var spawnFadeTicks: Int = -1,
    var teleportFadeTicks: Int = -1,
    var warmupPending: Boolean = false,
    var clearTicks: Int = 0,
    var clearBaseScale: Float = 1.0f,
    var clearBaseOpacity: Float = 1.0f,
    var clearing: Boolean = false,
    var lastPlayerX: Double = Double.NaN,
    var lastPlayerZ: Double = Double.NaN,
    var lastYaw: Float? = null,
    var lastPassengerSignature: Int = 0,
    var lastMountTick: Long = Long.MIN_VALUE,
    var lastActiveSyncTick: Long = Long.MIN_VALUE
)
