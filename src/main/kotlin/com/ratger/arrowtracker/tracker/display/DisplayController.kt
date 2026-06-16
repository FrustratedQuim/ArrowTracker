package com.ratger.arrowtracker.tracker.display

import com.github.retrooper.packetevents.protocol.world.Location
import com.github.retrooper.packetevents.util.Vector3d
import com.ratger.arrowtracker.tracker.model.Session
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.entity.Player
import java.util.UUID

internal class DisplayController(
    private val displayFactory: DisplayFactory,
    private val mountController: MountController
) {
    data class Prepared(
        val display: WrapperEntity,
        val spawnedNow: Boolean
    )

    fun start() {
        mountController.start()
    }

    fun shutdown() {
        mountController.shutdown()
    }

    fun forget(playerId: UUID) {
        mountController.forget(playerId)
    }

    fun prepare(
        player: Player,
        session: Session,
        serviceTick: Long,
        spawnYaw: () -> Float
    ): Prepared {
        val displayState = ensureDisplay(player, session, spawnYaw)
        val display = displayState.display
        ensureMounted(player, session, display, serviceTick)
        return Prepared(display, displayState.spawnedNow)
    }

    fun dispose(player: Player?, session: Session) {
        removeDisplay(player, session)
    }

    fun resetState(session: Session) {
        session.lastYaw = null
        invalidateState(session)
    }

    private fun ensureDisplay(player: Player, session: Session, spawnYaw: () -> Float): Prepared {
        val existingDisplay = session.display
        if (existingDisplay != null && existingDisplay.isSpawned) {
            return Prepared(existingDisplay, spawnedNow = false)
        }
        if (existingDisplay != null) {
            removeDisplay(player, session)
        }

        val display = displayFactory.create(player.uniqueId)
        display.spawn(spawnLocation(player, spawnYaw()))
        session.display = display
        invalidateState(session)
        return Prepared(display, spawnedNow = true)
    }

    private fun ensureMounted(player: Player, session: Session, display: WrapperEntity, serviceTick: Long) {
        val passengerSignature = mountController.passengerSignature(player)
        if (!SyncPolicy.shouldResyncMount(serviceTick, session, passengerSignature)) {
            return
        }

        mountController.mount(display, player)
        session.lastPassengerSignature = mountController.passengerSignature(player)
        session.lastMountTick = serviceTick
    }

    private fun removeDisplay(player: Player?, session: Session) {
        val display = session.display ?: return
        if (player != null && display.isSpawned) {
            mountController.dismount(display, player)
        }
        display.remove()
        session.display = null
        invalidateState(session)
    }

    private fun spawnLocation(player: Player, yaw: Float): Location {
        return Location(
            Vector3d(player.x, player.y + player.height, player.z),
            yaw,
            0.0f
        )
    }

    private fun invalidateState(session: Session) {
        session.lastPlayerX = Double.NaN
        session.lastPlayerZ = Double.NaN
        session.lastPassengerSignature = 0
        session.lastMountTick = Long.MIN_VALUE
        session.lastActiveSyncTick = Long.MIN_VALUE
    }
}
