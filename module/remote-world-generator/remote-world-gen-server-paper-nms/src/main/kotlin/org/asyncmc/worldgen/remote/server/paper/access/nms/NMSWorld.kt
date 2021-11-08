package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.server.level.WorldServer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ChunkCoordIntPair
import net.minecraft.world.level.World
import org.asyncmc.worldgen.remote.server.paper.access.obc.asWrappedOBC
import org.bukkit.Chunk
import org.bukkit.Server
import org.bukkit.World as BukkitWorld

@JvmInline
value class NMSWorld(val bukkit: BukkitWorld): NMSWrapper<World> {
    inline val obc get() = bukkit.asWrappedOBC().obc
    override val nms: WorldServer get() = obc.handle as WorldServer
    inline val server: Server get() = nms.craftServer
    inline val entityPersistenceManager: NMSPersistentEntitySectionManager<Entity> get() = NMSPersistentEntitySectionManager(nms.G)

    fun forceLoadEntities(chunk: Chunk) {
        val chunkPos = ChunkCoordIntPair.pair(chunk.x, chunk.z)
        entityPersistenceManager.scheduleEntityLoading(chunkPos)
        entityPersistenceManager.tick()
    }
}
