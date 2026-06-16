package com.ratger.arrowtracker.tracker.arrow

import com.ratger.arrowtracker.tracker.model.ArrowVisualState
import com.ratger.arrowtracker.tracker.model.Target
import org.bukkit.entity.Player
import kotlin.math.sqrt

internal class VisualStateResolver {
    fun active(
        player: Player,
        target: Target,
        activeTicks: Int,
        spawnFadeTicks: Int,
        teleportFadeTicks: Int
    ): ArrowVisualState {
        val distanceMultiplier = resolveDistanceMultiplier(player, target)
        val appearanceMultiplier = resolveAppearanceMultiplier(activeTicks)
        val opacityMultiplier = resolveSpawnFadeOpacity(spawnFadeTicks) * resolveTeleportFadeOpacity(teleportFadeTicks)
        return ArrowVisualState(
            scaleMultiplier = appearanceMultiplier * distanceMultiplier,
            opacityMultiplier = opacityMultiplier,
            completed = false
        )
    }

    fun clearing(clearTicks: Int, baseScale: Float, baseOpacity: Float): ArrowVisualState {
        val progress = (clearTicks.toFloat() / CLEAR_DURATION_TICKS).coerceIn(0.0f, 1.0f)
        return ArrowVisualState(
            scaleMultiplier = baseScale * resolveClearMultiplier(progress),
            opacityMultiplier = baseOpacity,
            completed = progress >= 1.0f
        )
    }

    fun isAppearing(activeTicks: Int): Boolean {
        return activeTicks < APPEAR_DURATION_TICKS
    }

    fun isAnimating(activeTicks: Int, spawnFadeTicks: Int, teleportFadeTicks: Int): Boolean {
        return isAppearing(activeTicks) || isSpawnFading(spawnFadeTicks) || isTeleportFading(teleportFadeTicks)
    }

    fun advanceSpawnFade(spawnFadeTicks: Int): Int {
        return advanceFade(spawnFadeTicks, SPAWN_FADE_DURATION_TICKS)
    }

    fun advanceTeleportFade(teleportFadeTicks: Int): Int {
        return advanceFade(teleportFadeTicks, TELEPORT_FADE_DURATION_TICKS)
    }

    private fun resolveAppearanceMultiplier(activeTicks: Int): Float {
        val progress = (activeTicks.toFloat() / APPEAR_DURATION_TICKS).coerceIn(0.0f, 1.0f)
        return easeOutBack(progress)
    }

    private fun isSpawnFading(spawnFadeTicks: Int): Boolean {
        return spawnFadeTicks in 0..SPAWN_FADE_DURATION_TICKS
    }

    private fun isTeleportFading(teleportFadeTicks: Int): Boolean {
        return teleportFadeTicks in 0..TELEPORT_FADE_DURATION_TICKS
    }

    private fun resolveSpawnFadeOpacity(spawnFadeTicks: Int): Float {
        if (!isSpawnFading(spawnFadeTicks)) {
            return 1.0f
        }

        return (spawnFadeTicks.toFloat() / SPAWN_FADE_DURATION_TICKS)
            .coerceIn(0.0f, 1.0f)
    }

    private fun resolveTeleportFadeOpacity(teleportFadeTicks: Int): Float {
        if (!isTeleportFading(teleportFadeTicks)) {
            return 1.0f
        }

        return (teleportFadeTicks.toFloat() / TELEPORT_FADE_DURATION_TICKS)
            .coerceIn(0.0f, 1.0f)
    }

    private fun resolveDistanceMultiplier(player: Player, target: Target): Float {
        val deltaX = target.x - player.x
        val deltaZ = target.z - player.z
        val distance = sqrt(deltaX * deltaX + deltaZ * deltaZ)
        return resolveFadeMultiplier(distance)
    }

    private fun resolveFadeMultiplier(distance: Double): Float {
        return ((distance - TEXT_DISTANCE_FADE_END) / (TEXT_DISTANCE_FADE_START - TEXT_DISTANCE_FADE_END))
            .toFloat()
            .coerceIn(0.0f, 1.0f)
    }

    private fun easeOutBack(progress: Float): Float {
        val adjustedProgress = progress - 1.0f
        val overshoot = BACK_EASING_OVERSHOOT + 1.0f
        return 1.0f +
            overshoot * adjustedProgress * adjustedProgress * adjustedProgress +
            BACK_EASING_OVERSHOOT * adjustedProgress * adjustedProgress
    }

    private fun resolveClearMultiplier(progress: Float): Float {
        if (progress <= CLEAR_EXPAND_PORTION) {
            val expandProgress = progress / CLEAR_EXPAND_PORTION
            return lerp(easeOutQuad(expandProgress), 1.0f, CLEAR_PEAK_MULTIPLIER)
        }

        val shrinkProgress = (progress - CLEAR_EXPAND_PORTION) / (1.0f - CLEAR_EXPAND_PORTION)
        return lerp(easeInCubic(shrinkProgress), CLEAR_PEAK_MULTIPLIER, 0.0f)
    }

    private fun easeOutQuad(progress: Float): Float {
        return 1.0f - (1.0f - progress) * (1.0f - progress)
    }

    private fun easeInCubic(progress: Float): Float {
        return progress * progress * progress
    }

    private fun lerp(progress: Float, start: Float, end: Float): Float {
        return start + progress * (end - start)
    }

    private fun advanceFade(fadeTicks: Int, durationTicks: Int): Int {
        if (fadeTicks !in 0..durationTicks) {
            return FADE_INACTIVE
        }

        val nextTick = fadeTicks + 1
        return if (nextTick <= durationTicks) {
            nextTick
        } else {
            FADE_INACTIVE
        }
    }

    private companion object {
        const val FADE_INACTIVE = -1
        const val APPEAR_DURATION_TICKS = 8
        const val SPAWN_FADE_DURATION_TICKS = 20
        const val TELEPORT_FADE_DURATION_TICKS = 20
        const val TEXT_DISTANCE_FADE_START = 15.0
        const val TEXT_DISTANCE_FADE_END = 8.0
        const val BACK_EASING_OVERSHOOT = 2.5f
        const val CLEAR_DURATION_TICKS = 8
        const val CLEAR_EXPAND_PORTION = 0.25f
        const val CLEAR_PEAK_MULTIPLIER = 1.12f
    }
}
