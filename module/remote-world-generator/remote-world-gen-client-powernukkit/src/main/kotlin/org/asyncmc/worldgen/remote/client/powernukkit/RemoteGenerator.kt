package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.blockstate.BlockState
import cn.nukkit.level.ChunkManager
import cn.nukkit.level.Level
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.level.generator.Generator
import cn.nukkit.math.NukkitRandom
import cn.nukkit.math.Vector3
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.asyncmc.worldgen.remote.data.MinecraftDimension
import org.asyncmc.worldgen.remote.data.PrepareWorldRequest
import org.asyncmc.worldgen.remote.data.RemoteChunk

internal abstract class RemoteGenerator(val options: Map<String, Any>): Generator() {
    protected lateinit var level: ChunkManager
    protected lateinit var random: NukkitRandom
    private var remoteNameAsync: Deferred<String>? = null
    private var remoteName: String? = null
    val backend = options["backend"]?.let { Url(it.toString()) } ?: plugin.backend

    protected abstract val fallbackBiome: UByte

    abstract override fun getDimension(): Int

    protected open fun createWorldPrepareRequest(): PrepareWorldRequest {
        return PrepareWorldRequest(
            when (dimension) {
                Level.DIMENSION_OVERWORLD -> MinecraftDimension.DIMENSION_OVERWORLD
                Level.DIMENSION_NETHER -> MinecraftDimension.DIMENSION_THE_NETHER
                Level.DIMENSION_THE_END -> MinecraftDimension.DIMENSION_THE_END
                else -> error("Unsupported dimension $dimension")
            },
            level.seed,
            options.getOrDefault("generate-structures", true).toString().toBoolean()
        )
    }

    private object SilentRetry: Throwable("Just retrying", null, false, false)

    private suspend inline fun <R> keepTrying(
        errorMessage: (Exception)->String = { "An error has occurred, retrying.." },
        retry: Long = 2_000,
        action: ()-> R
    ): R {
        var result: R
        while (true) {
            try {
                result = action()
                break
            } catch (retry: SilentRetry) {
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
        this.level = level
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
            keepTrying({"Could not receive the remote chunk x=$chunkX, z=$chunkX for $remoteWorldName - $level"}) {
                val response = try {
                    plugin.httpClient.get<ByteArray>("$backend/chunk/create/$remoteWorldName/$chunkX/$chunkZ?openTreasures=true") {
                        accept(ContentType.Application.ProtoBuf)
                    }
                } catch (e: NotFoundException) {
                    plugin.log.error(e) { "The remote world handler $remoteWorldName is no longer valid, trying to refresh it" }
                    sendPrepareWorldRequestAsync().await()
                    plugin.log.info { "The remote world handler was refreshed" }
                    throw SilentRetry
                }
                ProtoBuf.Default.decodeFromByteArray<RemoteChunk>(response)
            }
        }

        val chunk = level.getChunk(chunkX, chunkZ)
        val remoteChunk = runBlocking { remoteChunkAsync.await() }

        setBiomes(remoteChunk, chunk)
        setBlockStates(remoteChunk, chunk)
        if (remoteChunk.blockEntities.isNotEmpty()) {
            createBlockEntities(remoteChunk, chunk)
        }
        if (remoteChunk.entities.isNotEmpty()) {
            createEntities(remoteChunk, chunk)
        }
    }

    private fun createBlockEntities(remoteChunk: RemoteChunk, chunk: BaseFullChunk) {
        remoteChunk.blockEntities.forEach { remoteBlockEntity ->
            try {
                RemoteToPowerNukkitConverter.convertBlockEntity(remoteChunk, chunk, remoteBlockEntity)
            } catch (e: Exception) {
                plugin.log.error(e) {
                    "Failed to create the block entity $remoteBlockEntity"
                }
            }
        }
    }

    private fun createEntities(remoteChunk: RemoteChunk, chunk: BaseFullChunk) {
        remoteChunk.entities.forEach { remoteEntity ->
            try {
                RemoteToPowerNukkitConverter.createNukkitEntity(remoteChunk, chunk, remoteEntity)
            } catch (e: Exception) {
                plugin.log.error(e) {
                    "Failed to create the entity $remoteEntity"
                }
            }
        }
    }

    private fun setBlockStates(remoteChunk: RemoteChunk, chunk: BaseFullChunk) {
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
                    RemoteToPowerNukkitConverter.createDefaultBlockEntity(blockState.main, chunk, ix, cy, iz)
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

    override fun populateChunk(chunkX: Int, chunkZ: Int) {
        // Does nothing now
    }

    override fun getSettings(): Map<String, Any> {
        return options
    }

    override fun getSpawn(): Vector3 {
        return Vector3(0.0, 64.0, 0.0)
    }

    override fun getChunkManager(): ChunkManager {
        return level
    }
}
