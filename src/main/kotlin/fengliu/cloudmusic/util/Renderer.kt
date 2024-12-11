/*
 * Copyright © 2024 RTAkland
 * Author: RTAkland
 * Date: 2024/12/11
 */

@file:JvmName("Renderer")

package fengliu.cloudmusic.util

import fengliu.cloudmusic.command.MusicCommand
import fengliu.cloudmusic.render.MusicIconTexture.MUSIC_ICON_ID
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.render.RenderLayer
import org.joml.Quaternionf

object Renderer {
    private val renderLayer = RenderLayer::getGuiTexturedOverlay

    init {
        var rotationAngle = 0f
        HudRenderCallback.EVENT.register { context, _ ->
            val player = MusicCommand.getPlayer()
            player.playingMusic ?: return@register
            rotationAngle += 1f
            if (rotationAngle >= 360f) rotationAngle = 0f
            val matrix = context.matrices
            matrix.push()
            matrix.translate(5f + 24f, 20f + 24f, 0f)
            val quaternion = Quaternionf().apply {
                rotateZ(Math.toRadians(rotationAngle.toDouble()).toFloat())
            }
            matrix.multiply(quaternion)
            matrix.translate(-(5f + 24f), -(20f + 24f), 0f)
            context.drawTexture(
                renderLayer, MUSIC_ICON_ID, 5,
                20, 0f, 0f, 48,
                48, 48, 48
            )
            matrix.pop()
        }
    }
}