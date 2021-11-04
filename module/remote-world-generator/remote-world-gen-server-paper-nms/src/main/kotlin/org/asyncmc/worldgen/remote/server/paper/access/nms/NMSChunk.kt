package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.chunk.Chunk
import net.minecraft.world.level.material.FluidType
import org.asyncmc.worldgen.remote.server.paper.access.obc.asWrappedOBC
import org.bukkit.Chunk as BukkitChunk

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSChunk(val bukkit: BukkitChunk): NMSWrapper<Chunk> {
    inline val obc get() = bukkit.asWrappedOBC().obc
    override val nms: Chunk get() = obc.handle
    inline val tileEntities: Map<NMSBlockPos, NMSTileEntity> get() = nms.tileEntities.entries.associate { (pos, tile) ->
        NMSBlockPos(pos) to NMSTileEntity(tile)
    }
    inline val sections: List<NMSChunkSection> get() = nms.sections.map { NMSChunkSection(it) }

    inline val height: Int get() = nms.height

    inline val biomeArray: NMSBiomeStorage? get() = nms.biomeIndex?.let { NMSBiomeStorage(it) }

    inline val pos: NMSChunkPos get() = NMSChunkPos(nms.pos)

    inline val blockTickScheduler: NMSTickList<Block> get() = NMSTickList(nms.o())
    inline val fluidTickScheduler: NMSTickList<FluidType> get() = NMSTickList(nms.p())

    inline val structureStarts get() = nms.g().entries.associate { (gen, start) ->
        NMSStructureGenerator(gen) to NMSStructureStart(start)
    }

    inline val structureReferences get() = nms.w().entries.associate { (gen, refs) ->
        NMSStructureGenerator(gen) to refs
    }

    inline fun getHeightmap(type: NMSHeightMapType): NMSHeightMap {
        return NMSHeightMap(nms.a(type.nms))
    }
}
