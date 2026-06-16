package com.ratger.arrowtracker.tracker.display

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Vector3f
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta
import me.tofaa.entitylib.meta.display.TextDisplayMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.kyori.adventure.text.Component
import java.util.UUID

internal class DisplayFactory {
    fun create(viewerId: UUID): WrapperEntity {
        val interpolation = InterpolationProfiles.snap()
        val display = WrapperEntity(EntityTypes.TEXT_DISPLAY)
        display.addViewer(viewerId)
        display.consumeMeta { meta ->
            meta.isInvisible = true
        }
        display.consumeEntityMeta(TextDisplayMeta::class.java) { meta ->
            meta.text = Component.empty()
            meta.lineWidth = RenderStyle.TEXT_LINE_WIDTH
            meta.textOpacity = RenderStyle.MIN_RENDERABLE_OPACITY
            meta.backgroundColor = RenderStyle.BACKGROUND_COLOR
            meta.isShadow = false
            meta.isSeeThrough = true
            meta.isUseDefaultBackground = false
            meta.billboardConstraints = AbstractDisplayMeta.BillboardConstraints.FIXED
            meta.shadowRadius = 0.0f
            meta.shadowStrength = 0.0f
            meta.viewRange = RenderStyle.VIEW_RANGE
            meta.width = RenderStyle.CULLING_SIZE
            meta.height = RenderStyle.CULLING_SIZE
            meta.positionRotationInterpolationDuration = interpolation.positionRotationTicks
            meta.transformationInterpolationDuration = interpolation.transformationTicks
            meta.translation = TransformResolver.resolveTranslation(0.0f)
            meta.scale = Vector3f(0.0f, 0.0f, 0.0f)
            meta.leftRotation = TransformResolver.FLAT_ROTATION
            meta.rightRotation = TransformResolver.IDENTITY_ROTATION
        }
        return display
    }
}
