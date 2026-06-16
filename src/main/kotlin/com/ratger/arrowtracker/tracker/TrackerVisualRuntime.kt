package com.ratger.arrowtracker.tracker

import com.ratger.arrowtracker.tracker.arrow.PoseResolver
import com.ratger.arrowtracker.tracker.arrow.VisualStateResolver
import com.ratger.arrowtracker.tracker.display.DisplayController
import com.ratger.arrowtracker.tracker.display.DisplayUpdater
import com.ratger.arrowtracker.tracker.display.InterpolationProfiles
import com.ratger.arrowtracker.tracker.display.SyncPolicy
import com.ratger.arrowtracker.tracker.model.ArrowPose
import com.ratger.arrowtracker.tracker.model.ArrowVisualState
import com.ratger.arrowtracker.tracker.model.Session
import com.ratger.arrowtracker.tracker.model.Target
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.logging.Level

internal class TrackerVisualRuntime(
    private val plugin: JavaPlugin,
    private val sessions: SessionStore,
    private val poseResolver: PoseResolver,
    private val visualStateResolver: VisualStateResolver,
    private val displayController: DisplayController,
    private val displayUpdater: DisplayUpdater
) {
    private var tickTask: BukkitTask? = null
    private var serviceTick: Long = 0L

    fun start() {
        if (tickTask != null) {
            return
        }

        displayController.start()
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { tick() }, 1L, 1L)
    }

    fun shutdown() {
        tickTask?.cancel()
        tickTask = null

        sessions.forEach { playerId, session ->
            disposeSession(Bukkit.getPlayer(playerId), session)
        }
        sessions.clear()
        displayController.shutdown()
    }

    fun activate(player: Player, session: Session, target: Target) {
        session.target = target
        session.activeTicks = INITIAL_VISIBLE_ACTIVE_TICK
        session.spawnFadeTicks = FADE_INACTIVE
        session.teleportFadeTicks = FADE_INACTIVE
        session.warmupPending = true
        session.clearTicks = 0
        session.clearBaseScale = 1.0f
        session.clearBaseOpacity = 1.0f
        session.clearing = false
        resetSyncState(session)
        renderNow(player, session)
    }

    fun beginClearing(player: Player, session: Session) {
        val activeState = visualStateResolver.active(
            player,
            session.target,
            session.activeTicks,
            session.spawnFadeTicks,
            session.teleportFadeTicks
        )
        session.clearBaseScale = activeState.scaleMultiplier
        session.clearBaseOpacity = activeState.opacityMultiplier
        session.clearing = true
        session.spawnFadeTicks = FADE_INACTIVE
        session.teleportFadeTicks = FADE_INACTIVE
        session.warmupPending = false
        session.clearTicks = 0
    }

    fun recreateDisplay(player: Player, session: Session) {
        if (!session.clearing) {
            session.activeTicks = INITIAL_VISIBLE_ACTIVE_TICK
            session.spawnFadeTicks = 0
            session.teleportFadeTicks = FADE_INACTIVE
            session.warmupPending = true
        }
        displayController.dispose(player, session)
    }

    fun fadeAfterTeleport(session: Session) {
        if (session.clearing) {
            return
        }

        session.spawnFadeTicks = FADE_INACTIVE
        session.teleportFadeTicks = 0
        session.warmupPending = true
        session.display?.takeIf { it.isSpawned }?.let(displayUpdater::hide)
        resetSyncState(session)
    }

    fun remove(playerId: UUID, session: Session) {
        forgetSession(playerId, Bukkit.getPlayer(playerId), session)
    }

    private fun tick() {
        serviceTick++

        sessions.removeIf { playerId, session ->
            val player = Bukkit.getPlayer(playerId)
            if (player == null || !player.isOnline) {
                forgetSession(playerId, null, session)
                return@removeIf true
            }

            try {
                if (tickSession(player, session)) {
                    forgetSession(playerId, player, session)
                    return@removeIf true
                }
            } catch (exception: Exception) {
                plugin.logger.log(
                    Level.WARNING,
                    "Tracker session for player ${player.name} failed and will be removed.",
                    exception
                )
                forgetSession(playerId, player, session)
                return@removeIf true
            }
            false
        }
    }

    private fun renderNow(player: Player, session: Session) {
        syncActive(player, session, forceUpdate = true)
    }

    private fun tickSession(player: Player, session: Session): Boolean {
        if (session.clearing) {
            return tickClearing(player, session)
        }

        syncActive(player, session, forceUpdate = false)
        if (session.activeTicks < Int.MAX_VALUE) {
            session.activeTicks++
        }
        session.spawnFadeTicks = visualStateResolver.advanceSpawnFade(session.spawnFadeTicks)
        session.teleportFadeTicks = visualStateResolver.advanceTeleportFade(session.teleportFadeTicks)
        return false
    }

    private fun tickClearing(player: Player, session: Session): Boolean {
        val state = syncClearing(player, session)
        if (state.completed) {
            return true
        }
        if (session.clearTicks < Int.MAX_VALUE) {
            session.clearTicks++
        }
        return false
    }

    private fun syncActive(player: Player, session: Session, forceUpdate: Boolean) {
        var cachedPose: ArrowPose? = null
        fun pose(): ArrowPose {
            return cachedPose ?: poseFor(player, session).also { cachedPose = it }
        }

        val prepared = displayController.prepare(
            player,
            session,
            serviceTick,
            spawnYaw = { pose().yaw }
        )
        val display = prepared.display
        if (session.warmupPending) {
            val warmupPose = pose()
            val warmupState = warmupState(player, session)
            displayUpdater.update(display, warmupPose, warmupState, InterpolationProfiles.snap())
            rememberSync(player, session, warmupPose)
            session.lastActiveSyncTick = serviceTick
            session.warmupPending = false
            return
        }

        val animating = visualStateResolver.isAnimating(
            session.activeTicks,
            session.spawnFadeTicks,
            session.teleportFadeTicks
        )
        val moved = SyncPolicy.movedEnough(player, session)
        val shouldUpdate = forceUpdate ||
            prepared.spawnedNow ||
            animating ||
            (moved && SyncPolicy.canUpdateActive(serviceTick, session))
        if (!shouldUpdate) {
            return
        }

        val pose = pose()
        val state = visualStateResolver.active(
            player,
            session.target,
            session.activeTicks,
            session.spawnFadeTicks,
            session.teleportFadeTicks
        )
        val interpolation = InterpolationProfiles.active(
            forceUpdate = forceUpdate || prepared.spawnedNow,
            animating = animating
        )
        displayUpdater.update(display, pose, state, interpolation)
        rememberSync(player, session, pose)
        session.lastActiveSyncTick = serviceTick
    }

    private fun syncClearing(player: Player, session: Session): ArrowVisualState {
        var cachedPose: ArrowPose? = null
        fun pose(): ArrowPose {
            return cachedPose ?: poseFor(player, session).also { cachedPose = it }
        }

        val display = displayController.prepare(
            player,
            session,
            serviceTick,
            spawnYaw = { pose().yaw }
        ).display
        val pose = pose()
        val state = visualStateResolver.clearing(
            session.clearTicks,
            session.clearBaseScale,
            session.clearBaseOpacity
        )
        displayUpdater.update(display, pose, state, InterpolationProfiles.clearing())
        rememberSync(player, session, pose)
        return state
    }

    private fun poseFor(player: Player, session: Session): ArrowPose {
        val fallbackYaw = session.lastYaw ?: DEFAULT_FALLBACK_YAW
        return poseResolver.resolve(player, session.target, fallbackYaw)
    }

    private fun rememberSync(player: Player, session: Session, pose: ArrowPose) {
        session.lastPlayerX = player.x
        session.lastPlayerZ = player.z
        session.lastYaw = pose.yaw
    }

    private fun warmupState(player: Player, session: Session): ArrowVisualState {
        val state = visualStateResolver.active(
            player,
            session.target,
            session.activeTicks,
            session.spawnFadeTicks,
            session.teleportFadeTicks
        )
        return state.copy(opacityMultiplier = 0.0f)
    }

    private fun disposeSession(player: Player?, session: Session) {
        session.clearing = false
        session.spawnFadeTicks = FADE_INACTIVE
        session.teleportFadeTicks = FADE_INACTIVE
        session.warmupPending = false
        displayController.dispose(player, session)
    }

    private fun forgetSession(playerId: UUID, player: Player?, session: Session) {
        disposeSession(player, session)
        displayController.forget(playerId)
    }

    private fun resetSyncState(session: Session) {
        displayController.resetState(session)
    }

    private companion object {
        const val DEFAULT_FALLBACK_YAW = 180.0f
        const val INITIAL_VISIBLE_ACTIVE_TICK = 1
        const val FADE_INACTIVE = -1
    }
}
