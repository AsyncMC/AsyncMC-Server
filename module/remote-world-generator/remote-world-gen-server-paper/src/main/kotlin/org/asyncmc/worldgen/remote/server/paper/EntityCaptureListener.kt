package org.asyncmc.worldgen.remote.server.paper

import org.bukkit.Chunk
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.event.world.EntitiesUnloadEvent
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

internal object EntityCaptureListener: Listener {
    private val capturedEntities: MutableMap<Long, Pair<Long, Set<Entity>>> = ConcurrentHashMap()

    fun startCleanupTask(plugin: Plugin) {
        plugin.server.scheduler.scheduleSyncRepeatingTask(plugin, {
            val now = System.currentTimeMillis()
            capturedEntities.values.removeIf { (captureTime) ->
                (now - captureTime) > 30_000
            }
        }, 30 * 20L, 30 * 20L)
    }

    operator fun get(cx: Int, cz: Int): Set<Entity>? = capturedEntities[Chunk.getChunkKey(cx, cz)]?.second

    private fun handle(chunk: Chunk, entities: List<Entity>) {
        capturedEntities.compute(chunk.chunkKey) { _, current ->
            System.currentTimeMillis() to (current?.second ?: emptySet()) + entities
        }
    }

    @EventHandler
    fun unloadEvent(ev: EntitiesUnloadEvent) {
        handle(ev.chunk, ev.entities)
    }

    @EventHandler
    fun loadEvent(ev: EntitiesLoadEvent) {
        handle(ev.chunk, ev.entities)
    }
}
