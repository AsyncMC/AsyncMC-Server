package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.blockstate.BlockState
import cn.nukkit.level.ChunkManager
import cn.nukkit.level.Level
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.level.generator.Generator
import cn.nukkit.math.BlockVector3
import cn.nukkit.math.NukkitRandom
import cn.nukkit.math.Vector3
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.asyncmc.worldgen.remote.data.*
import org.powernukkit.plugins.kotlin.ExperimentalPowerNukkitKotlinApi

internal abstract class RemoteGenerator(val options: Map<String, Any>): Generator() {
    protected lateinit var chunkProvider: ChunkManager
    protected lateinit var random: NukkitRandom
    private var remoteNameAsync: Deferred<String>? = null
    private var remoteName: String? = null
    val backend = options["backend"]?.let { Url(it.toString()) } ?: plugin.backend

    @OptIn(ExperimentalSerializationApi::class)
    private val cachedSettings = ProtoBuf.Default.encodeToByteArray(RequestedChunkData(
        openedTreasures = plugin.config.getBoolean("worldgen.open-treasures", true),
    ))

    @OptIn(ExperimentalSerializationApi::class)
    private val requestEntities = ProtoBuf.Default.encodeToByteArray(RequestEntities(
        monsters = plugin.config.getSection("disable").let { !it.getBoolean("all-entities", false) && !it.getBoolean("monsters", false) },
        animals = plugin.config.getSection("disable").let { !it.getBoolean("all-entities", false) && !it.getBoolean("animals", false) },
        structureEntities = plugin.config.getSection("disable").let { !it.getBoolean("all-entities", false) && !it.getBoolean("structure-entities", false) },
        otherEntities = plugin.config.getSection("disable").let { !it.getBoolean("all-entities", false) && !it.getBoolean("other-entities", false) },
    ))

    protected abstract val fallbackBiome: UByte

    abstract override fun getDimension(): Int

    protected open fun createWorldPrepareRequest(): PrepareWorldRequest {
        return PrepareWorldRequest(
            dimension = when (dimension) {
                Level.DIMENSION_OVERWORLD -> MinecraftDimension.DIMENSION_OVERWORLD
                Level.DIMENSION_NETHER -> MinecraftDimension.DIMENSION_THE_NETHER
                Level.DIMENSION_THE_END -> MinecraftDimension.DIMENSION_THE_END
                else -> error("Unsupported dimension $dimension")
            },
            seed = chunkProvider.seed,
            generateStructures = options.getOrDefault("generate-structures",
                plugin.config.getBoolean("worldgen.generate-structures", true)
            ).toString().toBoolean()
        )
    }

    private object SilentRetry: Throwable("Just retrying", null, false, false)

    private suspend inline fun <R> keepTrying(
        errorMessage: (Exception)->String = { "An error has occurred, retrying.." },
        retry: Long = 2_000,
        action: ()-> R
    ): R {
        var result: R
        var silentRetryDelay = 50L
        while (true) {
            try {
                result = action()
                break
            } catch (_: SilentRetry) {
                delay(silentRetryDelay)
                if (silentRetryDelay < retry) {
                    silentRetryDelay = minOf((silentRetryDelay + silentRetryDelay * 0.25).toLong(), retry)
                }
                continue
            } catch (e: Exception) {
                plugin.log.error(e) {
                    errorMessage(e)
                }
                delay(retry)
            }
        }
        return result
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun init(level: ChunkManager, random: NukkitRandom) {
        this.chunkProvider = level
        this.random = random
        if (remoteNameAsync != null) {
            return
        }
        remoteNameAsync = CoroutineScope(Dispatchers.IO).sendPrepareWorldRequestAsync()
    }

    @OptIn(ExperimentalSerializationApi::class)
    protected fun CoroutineScope.sendPrepareWorldRequestAsync(): Deferred<String> {
        val prepareWorldRequest = createWorldPrepareRequest()
        return async {
            keepTrying({ "Could not connect to the world backend: $backend" }) {
                plugin.httpClient.post<String>("$backend/world/prepare") {
                    contentType(ContentType.Application.ProtoBuf)
                    accept(ContentType.Text.Plain)
                    body = ProtoBuf.Default.encodeToByteArray(prepareWorldRequest)
                }.also {
                    remoteName = it
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun generateChunk(chunkX: Int, chunkZ: Int) {
        val remoteChunkAsync = CoroutineScope(Dispatchers.IO).async {
            val remoteWorldName = remoteName ?: remoteNameAsync!!.await()
            keepTrying({"Could not receive the remote chunk x=$chunkX, z=$chunkX for $remoteWorldName - $chunkProvider"}) {
                requestData<RemoteChunk>("chunk/create", chunkX, chunkZ, cachedSettings)
            }
        }

        val chunk = chunkProvider.getChunk(chunkX, chunkZ)
        val remoteChunk = runBlocking { remoteChunkAsync.await() }

        setBiomes(remoteChunk, chunk)
        val blockEntities = if (remoteChunk.blockEntities.isEmpty()) {
            Int2ObjectMaps.emptyMap()
        } else {
            Int2ObjectOpenHashMap<RemoteBlockEntity>(remoteChunk.blockEntities.size).also { map ->
                remoteChunk.blockEntities.forEach { remoteBlockEntity ->
                    val cx = remoteBlockEntity.x and 0xF
                    val cz = remoteBlockEntity.z and 0xF
                    val cy = remoteBlockEntity.y + remoteChunk.minY
                    map[cz or (cx shl 4) or (cy shl 8)] = remoteBlockEntity
                }
            }
        }
        remoteChunk.blockEntities.associateBy { BlockVector3(it.x, it.y, it.z) }
        setBlockStates(remoteChunk, chunk, blockEntities)
        scheduleUpdates(remoteChunk, chunk)
    }

    private fun scheduleUpdates(remoteChunk: RemoteChunk, chunk: BaseFullChunk) {
        val level = chunk.provider.level ?: return // Can't schedule update if we don't have a level
        val pendingTickLists = remoteChunk.pendingTicks?.takeIf { it.blocks.isNotEmpty() || it.liquid.isNotEmpty() } ?: return
        plugin.mainThreadActionHandler.scheduleBlocking(level, chunk, pendingTickLists)
    }

    @OptIn(ExperimentalPowerNukkitKotlinApi::class)
    override fun populateChunk(chunkX: Int, chunkZ: Int) {
        val cachedChunk = chunkProvider.getChunk(chunkX, chunkZ)
        val level: Level = cachedChunk.provider?.level ?: run {
            plugin.log.warn { "The chunk population of cx:${chunkX} cz:${chunkZ} was aborted!" }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val remoteWorldName = remoteName ?: remoteNameAsync!!.await()
            var retries = 0
            val remoteEntities = keepTrying({"Could not receive the entities from remote chunk x=$chunkX, z=$chunkX for $remoteWorldName - $level"}) {
                if (retries++ >= 10) {
                    plugin.log.error { "Giving up on the entities at chunk x=$chunkX, z=$chunkX - $level" }
                    return@keepTrying null
                }
                requestData<List<RemoteEntity>>("entities/list", chunkX, chunkZ, requestEntities)
            }
            if (!remoteEntities.isNullOrEmpty()) {
                plugin.mainThreadActionHandler.scheduleEntitySpawningBlocking(level, cachedChunk, remoteEntities)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified R: Any> requestData(endpoint: String, chunkX: Int, chunkZ: Int, settings: ByteArray): R {
        return ProtoBuf.Default.decodeFromByteArray(requestByteArray(endpoint, chunkX, chunkZ, settings))
    }

    private suspend fun requestByteArray(endpoint: String, chunkX: Int, chunkZ: Int, settings: ByteArray): ByteArray {
        val remoteWorldName = remoteName ?: remoteNameAsync!!.await()
        return try {
            plugin.httpClient.post("$backend/$endpoint/$remoteWorldName/$chunkX/$chunkZ") {
                accept(ContentType.Application.ProtoBuf)
                contentType(ContentType.Application.ProtoBuf)
                body = settings
            }
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.Locked -> {
                    throw SilentRetry
                }
                HttpStatusCode.NotFound -> {
                    plugin.log.error { "The remote world handler $remoteWorldName is no longer valid, trying to refresh it" }
                    coroutineScope {
                        sendPrepareWorldRequestAsync().await()
                    }
                    plugin.log.info { "The remote world handler was refreshed" }
                    throw SilentRetry
                }
                else -> throw e
            }
        }
    }

    private fun setBlockStates(remoteChunk: RemoteChunk,
        chunk: BaseFullChunk,
        blockEntities: Int2ObjectMap<RemoteBlockEntity>
    ) {
        val minY = remoteChunk.minY
        require(remoteChunk.blockLayers.size <= 1) {
            "Unsupported response with multiple (size = ${remoteChunk.blockLayers.size})"
        }
        remoteChunk.blockLayers.firstOrNull()?.forEachIndexed { index, remoteBlockState ->
            val iy = index ushr 8
            val cy = iy - minY
            if (cy in 0..255) { // TODO Replace this hardcoded values with a PowerNukkit API for min..max Y
                val iz = index and 0xF
                val ix = index shr 4 and 0xF
                val blockState = RemoteToPowerNukkitConverter.convert(remoteBlockState)
                chunk.setBlockStateAtLayer(ix, cy, iz, 0, blockState.main)
                if (blockState.fluid != BlockState.AIR) {
                    chunk.setBlockStateAtLayer(ix, cy, iz, 1, blockState.fluid)
                }
                try {
                    val blockEntity = blockEntities[index]
                    val remoteBlock = blockEntity?.let { remoteBlockEntity ->
                        val x = remoteBlockEntity.x
                        val y = remoteBlockEntity.y
                        val z = remoteBlockEntity.z
                        remoteChunk.blockLayers[0][(z and 0xF) or (x and 0xF shl 4) or ((y + remoteChunk.minY) shl 8)]
                    }
                    RemoteToPowerNukkitConverter.createDefaultBlockEntity(
                        blockState = blockState.main,
                        chunk = chunk,
                        cx = ix,
                        cy = cy,
                        cz = iz,
                        remoteBlockEntity = blockEntity,
                        remoteBlockState = remoteBlock
                    )
                } catch (e: Exception) {
                    plugin.log.error(e) {
                        "Failed to create block entity at chunk (${chunk.x},${chunk.z}) block ($ix,$cy,$iz)"
                    }
                }
            }
        }
    }

    private fun setBiomes(remoteChunk: RemoteChunk, chunk: BaseFullChunk) {
        val firstIndex = ((64 - remoteChunk.minY)/4) shl 4 // Only mapping biomes at height 64
        remoteChunk.biomeMap.subList(firstIndex, firstIndex + (4*4)).forEachIndexed { index, biome ->
            val nukkitId = RemoteToPowerNukkitConverter.convertBiomeId(biome, fallbackBiome).toByte()
            val ix = (index and 0x3) * 4
            val iz = (index ushr 2 and 0x3) * 4
            for (x in ix..ix+3) {
                for (z in iz..iz+3) {
                    chunk.setBiomeId(x, z, nukkitId)
                }
            }
        }
    }

    override fun getSettings(): Map<String, Any> {
        return options
    }

    override fun getSpawn(): Vector3 {
        return Vector3(0.0, 64.0, 0.0)
    }

    override fun getChunkManager(): ChunkManager {
        return chunkProvider
    }
}
