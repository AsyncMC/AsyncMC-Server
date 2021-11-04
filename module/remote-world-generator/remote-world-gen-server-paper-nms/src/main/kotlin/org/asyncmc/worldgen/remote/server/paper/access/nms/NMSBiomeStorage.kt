package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.chunk.BiomeStorage

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSBiomeStorage(override val nms: BiomeStorage): NMSWrapper<BiomeStorage> {
    inline fun toIntArray(): IntArray {
        return nms.a()
    }
}
