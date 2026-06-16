package com.ratger.arrowtracker.api

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Public ArrowTracker integration contract.
 *
 * All methods must be called from the main server thread.
 */
interface ArrowTrackerApi {
    /**
     * Enables a tracker for [player] or updates its target.
     *
     * Returns `false` only when the same target is already active.
     */
    fun showTracker(player: Player, target: TrackerTarget): Boolean

    /**
     * Starts the tracker removal animation for [player].
     *
     * Returns `false` when there is no active tracker.
     */
    fun hideTracker(player: Player): Boolean

    /**
     * Toggles a tracker for [player] and returns the final state after the call.
     */
    fun toggleTracker(player: Player, target: TrackerTarget): TrackerState

    /**
     * Returns whether [playerId] currently has an active tracker.
     */
    fun isTracking(playerId: UUID): Boolean

    /**
     * Returns the tracked target for [playerId] or `null` if no tracker is active.
     */
    fun getTarget(playerId: UUID): TrackerTarget?

    /**
     * Returns an immutable snapshot of all active trackers.
     */
    fun getActiveTrackers(): Map<UUID, TrackerTarget>
}
