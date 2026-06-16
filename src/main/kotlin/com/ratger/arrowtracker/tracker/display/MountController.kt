package com.ratger.arrowtracker.tracker.display

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.entity.Player
import java.util.UUID

internal class MountController(
    private val passengerSync: PassengerSync = PassengerSync()
) {
    fun start() {
        passengerSync.start()
    }

    fun shutdown() {
        passengerSync.shutdown()
    }

    fun forget(playerId: UUID) {
        passengerSync.forget(playerId)
    }

    fun mount(display: WrapperEntity, player: Player) {
        sendPacket(display, player, passengerSync.attach(player, display.entityId))
    }

    fun dismount(display: WrapperEntity, player: Player) {
        sendPacket(display, player, passengerSync.detach(player, display.entityId))
    }

    fun passengerSignature(player: Player): Int {
        return passengerSync.passengerSignature(player)
    }

    private fun sendPacket(display: WrapperEntity, player: Player, passengerIds: IntArray) {
        display.sendPacketToViewers(
            WrapperPlayServerSetPassengers(player.entityId, passengerIds)
        )
    }
}
