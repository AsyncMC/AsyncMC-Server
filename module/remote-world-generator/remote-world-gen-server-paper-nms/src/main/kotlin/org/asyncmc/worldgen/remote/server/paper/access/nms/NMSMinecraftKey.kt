package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.resources.MinecraftKey

/**
 * Block State
 */
@JvmInline
value class NMSMinecraftKey(override val nms: MinecraftKey): NMSWrapper<MinecraftKey> {
    override fun toString(): String = nms.toString()
}
