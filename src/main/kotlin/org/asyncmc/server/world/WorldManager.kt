package org.asyncmc.server.world

import org.asyncmc.server.AsyncMc
import org.asyncmc.server.id.WorldId

public class WorldManager(public val server: AsyncMc) {
    internal fun acquireChunkProducer(world: World, id: WorldId): ChunkProducer {
        TODO("Not yet implemented")
    }
}
