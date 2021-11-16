package org.asyncmc.worldgen.remote.client.powernukkit.listeners

import cn.nukkit.block.BlockID
import cn.nukkit.blockstate.BlockState
import cn.nukkit.event.EventHandler
import cn.nukkit.event.EventPriority
import cn.nukkit.event.Listener
import cn.nukkit.event.level.LevelLoadEvent
import cn.nukkit.level.Level
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.plugin.PowerNukkitPlugin
import cn.nukkit.scheduler.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.asyncmc.worldgen.remote.client.powernukkit.AsyncMcWorldGenPowerNukkitClientPlugin
import org.asyncmc.worldgen.remote.client.powernukkit.RemoteToPowerNukkitConverter
import org.asyncmc.worldgen.remote.data.RemoteEntity
import org.asyncmc.worldgen.remote.data.RemotePendingTick
import org.asyncmc.worldgen.remote.data.RemotePendingTickLists

internal class PendingTicksHandler(private val plugin: AsyncMcWorldGenPowerNukkitClientPlugin): Listener {
    val pendingTicks = Channel<LevelPendingTicks>(Channel.BUFFERED)
    val pendingEntities = Channel<LevelPendingEntities>(Channel.BUFFERED)
    private val tickSchedulerTask = plugin.server.scheduler.scheduleRepeatingTask(TickSchedulerTask(), 1, false)!!
    private val entitySpawnerTask = plugin.server.scheduler.scheduleRepeatingTask(PluginSpawnerTask(), 1, false)!!
    val savedPendingTicks = mutableSetOf<LevelPendingTicks>()
    val savedPendingEntities = mutableSetOf<LevelPendingEntities>()

    init {
        plugin.server.pluginManager.registerEvents(this, PowerNukkitPlugin.getInstance())
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                runBlocking { closeAndSave() }
            }
        })
    }

    private suspend fun closeAndSave() {
        plugin.dataFolder.apply {
            mkdirs()
            pendingTicks.close()
            pendingEntities.close()
            val pendingTicks = pendingTicks.receiveAsFlow().toList()
            val pendingEntities = pendingEntities.receiveAsFlow().toList()
            resolve("pendingTicks.json").apply {
                if (pendingTicks.isNotEmpty()) {
                    writeText(Json.encodeToString(pendingTicks))
                } else {
                    delete()
                }
            }
            resolve("pendingEntities.json").apply {
                if (pendingEntities.isNotEmpty()) {
                    writeText(Json.encodeToString(pendingEntities))
                } else {
                    delete()
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onLevelLoad(ev: LevelLoadEvent) {
        val levelName = ev.level.folderName
        val pendingTicksToLoad = savedPendingTicks.filter { it.levelName == levelName }
        val pendingEntitiesToLoad = savedPendingEntities.filter { it.levelName == levelName }
        savedPendingTicks -= pendingTicksToLoad
        savedPendingEntities -= pendingEntitiesToLoad
        with(CoroutineScope(Dispatchers.Default)) {
            launch {
                pendingTicksToLoad.forEach {
                    pendingTicks.send(it)
                }
            }
            launch {
                pendingEntitiesToLoad.forEach {
                    pendingEntities.send(it)
                }
            }
        }
    }

    private inner class TickSchedulerTask: Task() {
        override fun onRun(currentTick: Int) {
            repeat(40) {
                pendingTicks.tryReceive().apply {
                    when {
                        isClosed -> {
                            plugin.log.info { "The pending ticks channel is closed, no more ticks will be scheduled" }
                            cancel()
                        }
                        isFailure -> return
                        isSuccess -> getOrThrow().apply {
                            try {
                                if (!processTick()) {
                                    savedPendingTicks += this
                                }
                            } catch (e: Exception) {
                                plugin.log.error(e) {
                                    "Could not schedule ticks of the chunk cx:$chunkX cz:$chunkZ of the level $levelName"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private inner class PluginSpawnerTask: Task() {
        override fun onRun(currentTick: Int) {
            repeat(40) {
                pendingEntities.tryReceive().apply {
                    when {
                        isClosed -> {
                            plugin.log.info { "The plugin spawning channel is closed, no more entities will spawn" }
                            cancel()
                        }
                        isFailure -> return
                        isSuccess -> getOrThrow().apply {
                            try {
                                if (!spawnEntities()) {
                                    savedPendingEntities += this
                                }
                            } catch (e: Exception) {
                                plugin.log.error(e) {
                                    "Could not spawn entities of the chunk cx:$chunkX cz:$chunkZ of the level $levelName"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun LevelPendingEntities.spawnEntities(): Boolean {
        val level = cachedLevel ?: plugin.server.getLevelByName(levelName) ?: return false
        val chunk = cachedChunk?.takeIf { it.provider != null } ?: level.getChunk(chunkX, chunkZ, false) ?: return false
        createEntities(pending, chunk)
        return true
    }

    private fun createEntities(remoteEntities: List<RemoteEntity>, chunk: BaseFullChunk) {
        remoteEntities.forEach { remoteEntity ->
            try {
                RemoteToPowerNukkitConverter.createNukkitEntity(chunk, remoteEntity)
            } catch (e: Exception) {
                org.asyncmc.worldgen.remote.client.powernukkit.plugin.log.error(e) {
                    "Failed to create the entity $remoteEntity"
                }
            }
        }
    }

    fun cancelTasks() {
        if (!tickSchedulerTask.isCancelled) {
            tickSchedulerTask.cancel()
            plugin.log.info { "The pending ticks task is now cancelled, no more ticks will be scheduled" }
        }
        if (!entitySpawnerTask.isCancelled) {
            entitySpawnerTask.cancel()
            plugin.log.info { "The entity spawning task is now cancelled, no more entities will spawn" }
        }
    }

    private fun createRequest(level: Level, chunk: BaseFullChunk, pending: RemotePendingTickLists): LevelPendingTicks {
        return LevelPendingTicks(level.folderName, pending, chunk.x, chunk.z, level, chunk)
    }

    internal fun scheduleBlocking(level: Level, chunk: BaseFullChunk, pending: RemotePendingTickLists) {
        pendingTicks.trySendBlocking(
            createRequest(level, chunk, pending)
        ).getOrThrow()
    }

    internal fun scheduleEntitySpawningBlocking(level: Level, chunk: BaseFullChunk, entities: List<RemoteEntity>) {
        pendingEntities.trySendBlocking(
            LevelPendingEntities(level.name, entities, chunk.x, chunk.z, level, chunk)
        ).getOrThrow()
    }

    private fun processTick(level: Level, chunk: BaseFullChunk, remotePendingTick: RemotePendingTick) {
        var blockState = chunk.getBlockStateAt(remotePendingTick.x, remotePendingTick.y, remotePendingTick.z)
        var layer = 0
        remotePendingTick.blockId?.also { idCheck ->
            if (!(checkIdForPendingChunk(idCheck, blockState) ?: return@also)) {
                blockState = chunk.getBlockStateAt(remotePendingTick.x, remotePendingTick.y, remotePendingTick.z, 1)
                layer = 1
                if (!(checkIdForPendingChunk(idCheck, blockState) ?: return@also)) {
                    return
                }
            }
        }

        if (blockState.blockId == BlockID.STILL_LAVA) {
            blockState = blockState.withBlockId(BlockID.LAVA)
            chunk.setBlockStateAt(remotePendingTick.x, remotePendingTick.y, remotePendingTick.z, layer, blockState)
        } else if (blockState.blockId == BlockID.STILL_WATER) {
            blockState = blockState.withBlockId(BlockID.WATER)
            chunk.setBlockStateAt(remotePendingTick.x, remotePendingTick.y, remotePendingTick.z, layer, blockState)
        }

        val block = blockState.getBlock(level,
            (chunk.x shl 4) + remotePendingTick.x,
            remotePendingTick.y,
            (chunk.z shl 4) + remotePendingTick.z,
            layer,
            true
        )

        level.scheduleUpdate(block, block, remotePendingTick.ticks, remotePendingTick.priority, false)
    }

    internal fun LevelPendingTicks.processTick(): Boolean {
        val level = cachedLevel ?: plugin.server.getLevelByName(levelName) ?: return false
        val chunk = cachedChunk?.takeIf { it.provider != null } ?: level.getChunk(chunkX, chunkZ, false) ?: return false
        sequenceOf(pending.blocks, pending.liquid).flatten().forEach { remotePendingTick ->
            processTick(level, chunk, remotePendingTick)
        }
        return true
    }

    private fun checkIdForPendingChunk(remoteId: String, blockState: BlockState): Boolean? {
        val validIds = when (remoteId) {
            "minecraft:lava" -> listOf(BlockID.LAVA, BlockID.STILL_LAVA)
            "minecraft:water" -> listOf(BlockID.WATER, BlockID.STILL_WATER)
            else -> return null
        }
        if (blockState.blockId !in validIds) {
            return false
        }

        return true
    }

    @Serializable
    internal data class LevelPendingTicks(
        val levelName: String,
        val pending: RemotePendingTickLists,
        val chunkX: Int,
        val chunkZ: Int,

        @Transient
        val cachedLevel: Level? = null,

        @Transient
        val cachedChunk: BaseFullChunk? = null,
    )

    @Serializable
    internal data class LevelPendingEntities(
        val levelName: String,
        val pending: List<RemoteEntity>,
        val chunkX: Int,
        val chunkZ: Int,

        @Transient
        val cachedLevel: Level? = null,

        @Transient
        val cachedChunk: BaseFullChunk? = null,
    )
}
