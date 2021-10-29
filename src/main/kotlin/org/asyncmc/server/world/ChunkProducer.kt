package org.asyncmc.server.world

import org.asyncmc.server.math.ChunkPos

public abstract class ChunkProducer {
    public abstract suspend fun loadExistingChunk(pos: ChunkPos): Chunk?
    public abstract suspend fun createChunk(pos: ChunkPos): Chunk
}
