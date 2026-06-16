package com.ratger.arrowtracker.tracker

import com.ratger.arrowtracker.tracker.model.ActionResult
import com.ratger.arrowtracker.tracker.model.Session
import com.ratger.arrowtracker.tracker.model.Target
import org.bukkit.entity.Player
import java.util.UUID

internal class TrackerService(
    private val sessions: SessionStore,
    private val visualRuntime: TrackerVisualRuntime
) {
    fun start() {
        visualRuntime.start()
    }

    fun shutdown() {
        visualRuntime.shutdown()
    }

    fun toggleTrackingResult(player: Player, target: Target): ActionResult {
        return if (toggleTracking(player, target)) {
            ActionResult.STARTED
        } else {
            ActionResult.CLEARED
        }
    }

    fun toggleTracking(player: Player, target: Target): Boolean {
        val playerId = player.uniqueId
        val existingSession = sessions[playerId]
        if (existingSession != null && !existingSession.clearing && existingSession.target == target) {
            hideTracking(player)
            return false
        }

        showTracking(player, target)
        return true
    }

    fun showTracking(player: Player, target: Target): Boolean {
        val playerId = player.uniqueId
        val existingSession = sessions[playerId]
        if (existingSession != null && !existingSession.clearing && existingSession.target == target) {
            return false
        }

        val session = existingSession ?: Session(target)
        sessions[playerId] = session
        visualRuntime.activate(player, session, target)
        return true
    }

    fun clearTracking(player: Player): ActionResult {
        return if (hideTracking(player)) {
            ActionResult.CLEARED
        } else {
            ActionResult.MISSING
        }
    }

    fun hideTracking(player: Player): Boolean {
        val session = sessions[player.uniqueId] ?: return false
        if (session.clearing) {
            return false
        }

        visualRuntime.beginClearing(player, session)
        return true
    }

    fun isTracking(playerId: UUID): Boolean {
        return sessions.isTracking(playerId)
    }

    fun trackedTarget(playerId: UUID): Target? {
        return sessions.trackedTarget(playerId)
    }

    fun activeTargets(): Map<UUID, Target> {
        return sessions.activeTargets()
    }

    fun removeTracking(playerId: UUID) {
        val session = sessions.remove(playerId) ?: return
        visualRuntime.remove(playerId, session)
    }

    fun recreateDisplay(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        visualRuntime.recreateDisplay(player, session)
    }

    fun fadeAfterTeleport(player: Player) {
        val session = sessions[player.uniqueId] ?: return
        visualRuntime.fadeAfterTeleport(session)
    }
}
