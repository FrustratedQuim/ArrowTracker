package com.ratger.arrowtracker.tracker.display

import com.github.retrooper.packetevents.util.Vector3f
import com.ratger.arrowtracker.tracker.model.ArrowPose
import com.ratger.arrowtracker.tracker.model.ArrowVisualState
import me.tofaa.entitylib.meta.display.TextDisplayMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.kyori.adventure.text.Component

internal class DisplayUpdater {
    fun hide(display: WrapperEntity) {
        display.consumeMeta { meta ->
            meta.isInvisible = true
        }
        display.consumeEntityMeta(TextDisplayMeta::class.java) { meta ->
            meta.text = Component.empty()
            meta.textOpacity = RenderStyle.MIN_RENDERABLE_OPACITY
        }
        display.refresh()
    }

    fun update(
        display: WrapperEntity,
        pose: ArrowPose,
        state: ArrowVisualState,
        interpolation: Interpolation
    ) {
        val scaleInvisible = state.scaleMultiplier <= RenderStyle.MIN_VISIBLE_SCALE_MULTIPLIER
        val textInvisible = scaleInvisible ||
            state.opacityMultiplier <= RenderStyle.MIN_VISIBLE_OPACITY_MULTIPLIER
        val effectiveScale = if (scaleInvisible) {
            0.0f
        } else {
            TransformResolver.BASE_SCALE * state.scaleMultiplier
        }

        display.rotateHead(pose.yaw, 0.0f)
        display.consumeMeta { meta ->
            meta.isInvisible = textInvisible
        }
        display.consumeEntityMeta(TextDisplayMeta::class.java) { meta ->
            meta.positionRotationInterpolationDuration = interpolation.positionRotationTicks
            meta.transformationInterpolationDuration = interpolation.transformationTicks
            meta.translation = TransformResolver.resolveTranslation(state.scaleMultiplier)
            meta.scale = Vector3f(effectiveScale, effectiveScale, effectiveScale)
            meta.text = if (textInvisible) Component.empty() else RenderStyle.TRACKER_COMPONENT
            meta.textOpacity = if (textInvisible) {
                RenderStyle.MIN_RENDERABLE_OPACITY
            } else {
                RenderStyle.resolveTextOpacity(state.opacityMultiplier)
            }
        }
        display.refresh()
    }
}
