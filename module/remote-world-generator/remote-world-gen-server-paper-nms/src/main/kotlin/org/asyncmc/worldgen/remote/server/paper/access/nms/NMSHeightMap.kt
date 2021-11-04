package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.levelgen.HeightMap

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSHeightMap(override val nms: HeightMap): NMSWrapper<HeightMap> {
    inline fun asLongArray(): LongArray {
        return nms.a()
    }
}
