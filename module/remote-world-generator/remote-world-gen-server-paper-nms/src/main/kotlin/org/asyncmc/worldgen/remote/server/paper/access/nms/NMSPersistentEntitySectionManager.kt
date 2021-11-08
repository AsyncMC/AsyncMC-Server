package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.ChunkCoordIntPair
import net.minecraft.world.level.entity.EntityAccess
import net.minecraft.world.level.entity.EntityPersistentStorage
import net.minecraft.world.level.entity.PersistentEntitySectionManager
import org.bukkit.entity.Entity
import java.util.concurrent.CompletableFuture

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSPersistentEntitySectionManager<T : EntityAccess?>(
    override val nms: PersistentEntitySectionManager<T>
) :NMSWrapper<PersistentEntitySectionManager<T>> {

    @Suppress("UNCHECKED_CAST")
    private val persistentStorage get() = persistentStorageField.get(nms) as EntityPersistentStorage<T>

    fun forceLoading(cx: Int, cz: Int): CompletableFuture<List<Entity>> {
        val pos = ChunkCoordIntPair(cx, cz)
        return persistentStorage.a(pos).thenApply { list -> list.b().map { it as Entity }.toList() }
    }

    inline fun scheduleEntityLoading(cx: Int, cz: Int) {
        scheduleEntityLoading(ChunkCoordIntPair.pair(cx, cz))
    }

    inline fun scheduleEntityLoading(chunkCoordinate: Long) {
        nms.b(chunkCoordinate)
    }

    inline fun tick() {
        nms.tick()
    }

    companion object {
        private val persistentStorageField by lazy {
            PersistentEntitySectionManager::class.java.fields.first { it.declaringClass == EntityPersistentStorage::class.java }
                .also { it.isAccessible = true }
        }
    }
}
