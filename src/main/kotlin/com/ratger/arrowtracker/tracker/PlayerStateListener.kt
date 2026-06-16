package com.ratger.arrowtracker.tracker

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent

internal class PlayerStateListener(
    private val trackerService: TrackerService
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        trackerService.removeTracking(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerChangedWorld(event: PlayerChangedWorldEvent) {
        trackerService.recreateDisplay(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        trackerService.recreateDisplay(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (!shouldFadeAfterTeleport(event)) {
            return
        }

        trackerService.fadeAfterTeleport(event.player)
    }

    private fun shouldFadeAfterTeleport(event: PlayerTeleportEvent): Boolean {
        val to = event.to
        if (event.from.world.uid != to.world.uid) {
            return false
        }

        val deltaX = to.x - event.from.x
        val deltaY = to.y - event.from.y
        val deltaZ = to.z - event.from.z
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ >= TELEPORT_VISUAL_RESTART_DISTANCE_SQUARED
    }

    private companion object {
        const val TELEPORT_VISUAL_RESTART_DISTANCE = 1.0
        const val TELEPORT_VISUAL_RESTART_DISTANCE_SQUARED =
            TELEPORT_VISUAL_RESTART_DISTANCE * TELEPORT_VISUAL_RESTART_DISTANCE
    }
}
