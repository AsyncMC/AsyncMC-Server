package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.Server
import cn.nukkit.blockstate.BlockState
import cn.nukkit.level.ChunkManager
import cn.nukkit.level.Level
import cn.nukkit.level.generator.Generator
import cn.nukkit.math.NukkitRandom
import cn.nukkit.math.Vector3
import io.ktor.client.request.*
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
    protected lateinit var bestSpawn: Vector3
    private var remoteNameAsync: Deferred<String>? = null
    private var remoteName: String? = null
    val backend = options["backend"]?.let { Url(it.toString()) } ?: plugin.backend

    override fun getId(): Int {
        return 0
    }

    abstract override fun getDimension(): Int

    protected open fun createWorldPrepareRequest(): PrepareWorldRequest {
        return PrepareWorldRequest(
            when (dimension) {
                Level.DIMENSION_OVERWORLD -> MinecraftDimension.DIMENSION_OVERWORLD
                Level.DIMENSION_NETHER -> MinecraftDimension.DIMENSION_THE_NETHER
                Level.DIMENSION_THE_END -> MinecraftDimension.DIMENSION_THE_END
                else -> error("Unsupported dimension $dimension")
            },
            name+"_"+level.hashCode().toString(),
            Server.getInstance().difficulty,
            level.seed,
            options.getOrDefault("generate-structures", true).toString().toBoolean()
        )
    }

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
        val prepareWorldRequest = createWorldPrepareRequest()
        remoteNameAsync = CoroutineScope(Dispatchers.IO).async {
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
                val response = plugin.httpClient.get<ByteArray>("$backend/chunk/create/$remoteWorldName/$chunkX/$chunkZ") {
                    accept(ContentType.Application.ProtoBuf)
                }
                ProtoBuf.Default.decodeFromByteArray<RemoteChunk>(response)
            }
        }

        val chunk = level.getChunk(chunkX, chunkZ)
        val remoteChunk = runBlocking { remoteChunkAsync.await() }

        val minY = remoteChunk.minY
        require(remoteChunk.blockLayers.size <= 1) {
            "Unsupported response with multiple (size = ${remoteChunk.blockLayers.size})"
        }
        remoteChunk.blockLayers.firstOrNull()?.forEachIndexed { index, remoteBlockState ->
            val iz = index and 0xF
            val ix = index shr 4 and 0xF
            val iy = index ushr 8
            val cy = iy - minY
            val blockState = RemoteToPowerNukkitConverter.convert(remoteBlockState)
            chunk.setBlockStateAtLayer(ix, cy, iz, 0, blockState.main)
            if (blockState.fluid != BlockState.AIR) {
                chunk.setBlockStateAtLayer(ix, cy, iz, 1, blockState.fluid)
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
        return bestSpawn.clone()
    }

    override fun getChunkManager(): ChunkManager {
        return level
    }
}
