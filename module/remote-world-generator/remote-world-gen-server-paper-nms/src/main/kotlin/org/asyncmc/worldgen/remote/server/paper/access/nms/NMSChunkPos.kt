package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.ChunkCoordIntPair

@JvmInline
value class NMSChunkPos(override val nms: ChunkCoordIntPair) : NMSWrapper<ChunkCoordIntPair> {
    inline val x: Int get() = nms.b
    inline val z: Int get() = nms.c
    inline val firstBlockX: Int get() = x shl 4
    inline val firstBlockZ: Int get() = z shl 4
}
