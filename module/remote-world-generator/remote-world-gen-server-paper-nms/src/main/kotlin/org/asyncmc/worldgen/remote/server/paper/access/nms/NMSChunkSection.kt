package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.chunk.ChunkSection

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSChunkSection(override val nms: ChunkSection): NMSWrapper<ChunkSection> {
    inline val yPos: Int get() = nms.yPosition

    inline fun getBlockState(x: Int, y: Int, z: Int): NMSIBlockData {
        return NMSIBlockData(nms.getType(x, y, z))
    }

    companion object {
        inline fun isEmpty(section: NMSChunkSection?): Boolean = isEmpty(section?.nms)
        inline fun isEmpty(section: ChunkSection?): Boolean = ChunkSection.a(section)
    }
}
