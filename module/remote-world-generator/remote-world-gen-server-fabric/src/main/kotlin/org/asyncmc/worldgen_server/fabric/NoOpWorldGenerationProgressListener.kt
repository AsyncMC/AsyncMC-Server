package org.asyncmc.worldgen_server.fabric

import net.minecraft.server.WorldGenerationProgressListener
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.chunk.ChunkStatus

object NoOpWorldGenerationProgressListener: WorldGenerationProgressListener {
    override fun start(spawnPos: ChunkPos?) = Unit
    override fun start() = Unit
    override fun setChunkStatus(pos: ChunkPos?, status: ChunkStatus?) = Unit
    override fun stop() = Unit
}
