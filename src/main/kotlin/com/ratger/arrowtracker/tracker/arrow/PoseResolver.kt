package com.ratger.arrowtracker.tracker.arrow

import com.ratger.arrowtracker.tracker.model.ArrowPose
import com.ratger.arrowtracker.tracker.model.Target
import org.bukkit.entity.Player
import kotlin.math.atan2

internal class PoseResolver {
    fun resolve(player: Player, target: Target, fallbackYaw: Float): ArrowPose {
        val deltaX = target.x - player.x
        val deltaZ = target.z - player.z
        if (deltaX * deltaX + deltaZ * deltaZ < TARGET_EPSILON) {
            return ArrowPose(yaw = wrapDegrees(fallbackYaw))
        }

        val trackedYaw = (Math.toDegrees(atan2(deltaZ, deltaX)) - 90.0).toFloat()
        return ArrowPose(
            yaw = wrapDegrees(trackedYaw + TEXT_DISPLAY_YAW_OFFSET_DEGREES)
        )
    }

    private fun wrapDegrees(value: Float): Float {
        var wrapped = value % 360.0f
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f
        }
        return wrapped
    }

    private companion object {
        const val TARGET_EPSILON = 1.0E-6
        const val TEXT_DISPLAY_YAW_OFFSET_DEGREES = 180.0f
    }
}
