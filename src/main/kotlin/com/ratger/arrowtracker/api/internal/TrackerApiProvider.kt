package com.ratger.arrowtracker.api.internal

import com.ratger.arrowtracker.api.ArrowTrackerApi
import com.ratger.arrowtracker.api.TrackerState
import com.ratger.arrowtracker.api.TrackerTarget
import com.ratger.arrowtracker.tracker.TrackerService
import com.ratger.arrowtracker.tracker.model.Target
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.Collections
import java.util.LinkedHashMap
import java.util.UUID

internal class TrackerApiProvider(
    private val trackerService: TrackerService
) : ArrowTrackerApi {

    override fun showTracker(player: Player, target: TrackerTarget): Boolean {
        requireMainThread()
        return trackerService.showTracking(player, target.toInternal())
    }

    override fun hideTracker(player: Player): Boolean {
        requireMainThread()
        return trackerService.hideTracking(player)
    }

    override fun toggleTracker(player: Player, target: TrackerTarget): TrackerState {
        requireMainThread()
        return if (trackerService.toggleTracking(player, target.toInternal())) {
            TrackerState.ACTIVE
        } else {
            TrackerState.INACTIVE
        }
    }

    override fun isTracking(playerId: UUID): Boolean {
        requireMainThread()
        return trackerService.isTracking(playerId)
    }

    override fun getTarget(playerId: UUID): TrackerTarget? {
        requireMainThread()
        return trackerService.trackedTarget(playerId)?.toApi()
    }

    override fun getActiveTrackers(): Map<UUID, TrackerTarget> {
        requireMainThread()
        val snapshot = LinkedHashMap<UUID, TrackerTarget>()
        trackerService.activeTargets().forEach { (playerId, target) ->
            snapshot[playerId] = target.toApi()
        }
        return Collections.unmodifiableMap(snapshot)
    }

    private fun requireMainThread() {
        check(Bukkit.isPrimaryThread()) {
            "ArrowTracker API must be used from the main server thread."
        }
    }

    private fun TrackerTarget.toInternal(): Target {
        return Target(x, z)
    }

    private fun Target.toApi(): TrackerTarget {
        return TrackerTarget(x, z)
    }
}
