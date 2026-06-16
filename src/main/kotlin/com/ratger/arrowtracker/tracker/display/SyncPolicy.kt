package com.ratger.arrowtracker.tracker.display

import com.ratger.arrowtracker.tracker.model.Session
import org.bukkit.entity.Player

internal object SyncPolicy {
    const val POSITION_SYNC_DISTANCE = 0.25
    const val ACTIVE_UPDATE_INTERVAL_TICKS = 2L
    const val MOUNT_RESYNC_INTERVAL_TICKS = 20L

    private const val POSITION_SYNC_DISTANCE_SQUARED = POSITION_SYNC_DISTANCE * POSITION_SYNC_DISTANCE

    fun movedEnough(player: Player, session: Session): Boolean {
        if (session.lastPlayerX.isNaN() || session.lastPlayerZ.isNaN()) {
            return true
        }

        val deltaX = player.x - session.lastPlayerX
        val deltaZ = player.z - session.lastPlayerZ
        return deltaX * deltaX + deltaZ * deltaZ >= POSITION_SYNC_DISTANCE_SQUARED
    }

    fun canUpdateActive(serviceTick: Long, session: Session): Boolean {
        return session.lastActiveSyncTick == Long.MIN_VALUE ||
            serviceTick - session.lastActiveSyncTick >= ACTIVE_UPDATE_INTERVAL_TICKS
    }

    fun shouldResyncMount(serviceTick: Long, session: Session, passengerSignature: Int): Boolean {
        return session.lastMountTick == Long.MIN_VALUE ||
            passengerSignature != session.lastPassengerSignature ||
            serviceTick - session.lastMountTick >= MOUNT_RESYNC_INTERVAL_TICKS
    }
}
