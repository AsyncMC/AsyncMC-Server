package org.asyncmc.server.world

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import org.asyncmc.server.id.WorldId
import org.asyncmc.server.math.ChunkPos
import kotlin.coroutines.CoroutineContext

public class World(
    public val worldManager: WorldManager,
    public val id: WorldId,
) {
    private val chunkProducer: ChunkProducer = worldManager.acquireChunkProducer(this, id)
    private var chunkManager: ChunkManager? = null

    internal fun CoroutineScope.startTicking() {
        check(chunkManager == null) {
            "The world $id is already ticking!"
        }
        chunkManager = ChunkManager(coroutineContext)
    }

    public suspend fun getOrCreateChunk(pos: ChunkPos): Chunk {
        return checkNotNull(getChunk(pos, load = true, create = true))
    }

    public suspend fun getChunk(pos: ChunkPos, load: Boolean = false, create: Boolean = false): Chunk? {
        return checkNotNull(chunkManager) {
            "The world $id is not ready to get chunks"
        }.requestChunk(pos, load, create)
    }

    private inner class ChunkManager(coroutineContext: CoroutineContext) : CoroutineScope {
        private val job: Job = SupervisorJob(coroutineContext.job)
        override val coroutineContext = coroutineContext + job

        private val loadedChunks = mutableMapOf<ChunkPos, Chunk>()
        private val loadChunkRequest = Channel<ChunkRequest>().apply {
            job.invokeOnCompletion {
                close(it)
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private val chunkLoadingChannel: ReceiveChannel<ChunkRequestResponse> = produce(coroutineContext) {
            loadChunkRequest.consumeEach { request ->
                launch {
                    try {
                        val chunk = loadedChunks[request.pos]?.takeIf { it.isLoaded }
                            ?: chunkProducer.loadExistingChunk(request.pos)
                            ?: chunkProducer.takeIf { request.create }?.createChunk(request.pos)
                        send(ChunkRequestResponse(request, chunk))
                    } catch (e: Exception) {
                        send(ChunkRequestResponse(request, null, e))
                    }
                }
            }
        }

        suspend fun requestChunk(pos: ChunkPos, load: Boolean, create: Boolean): Chunk? {
            // Fast path
            loadedChunks[pos]?.let { return it }

            if (!load) {
                return null
            }

            return coroutineScope {
                val response = async {
                    for (response in chunkLoadingChannel) {
                        if (response.request.pos == pos) {
                            response.exception?.let { throw it }
                            return@async response.chunk
                        }
                    }
                    check(!create) {
                        "Could not create the chunk at $pos, the loading channel was closed before the request was fulfilled."
                    }
                    return@async null
                }
                loadChunkRequest.send(ChunkRequest(pos, create))
                response.await()
            }
        }
    }

    private data class ChunkRequest(val pos: ChunkPos, val create: Boolean)
    private data class ChunkRequestResponse(val request: ChunkRequest, val chunk: Chunk?, val exception: Exception? = null)
}
