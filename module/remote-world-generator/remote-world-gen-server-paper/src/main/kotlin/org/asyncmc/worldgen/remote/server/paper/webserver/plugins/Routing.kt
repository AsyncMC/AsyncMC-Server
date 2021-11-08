package org.asyncmc.worldgen.remote.server.paper.webserver.plugins

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.future.await
import org.asyncmc.worldgen.remote.data.CreateChunkRequest
import org.asyncmc.worldgen.remote.data.PrepareWorldRequest
import org.asyncmc.worldgen.remote.data.RequestedChunkData
import org.asyncmc.worldgen.remote.server.paper.AsyncMcPaperWorldGenServer
import org.asyncmc.worldgen.remote.server.paper.ChunkConverter
import org.asyncmc.worldgen.remote.server.paper.EntityCaptureListener
import org.asyncmc.worldgen.remote.server.paper.callSync
import org.bukkit.Difficulty
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldInitEvent
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.name

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalCoroutinesApi::class)
fun Application.configureRouting(plugin: AsyncMcPaperWorldGenServer) {
    val worlds = ConcurrentHashMap<String, World>()

    val server = plugin.server
    val worldContainer = server.worldContainer.toPath()
    val worldsFolder = worldContainer.resolve("served-worlds")

    worldsFolder.toFile().deleteRecursively()
    Files.createDirectories(worldsFolder)

    routing {
        post<PrepareWorldRequest>("/world/prepare") { request ->
            val env = when (val id = request.dimension.id) {
                "minecraft:overworld" -> World.Environment.NORMAL
                "minecraft:the_nether" -> World.Environment.NETHER
                "minecraft:the_end" -> World.Environment.THE_END
                else -> throw UnsupportedOperationException("Dimension type $id is not supported")
            }

            val folder = worldsFolder.resolve("${env.name.lowercase()}_${request.seed}_structures-${request.generateStructures}")
            worlds[folder.name]?.let {
                call.respond(folder.name)
                return@post
            }

            val worldName = worldContainer.relativize(folder).toString().replace('\\', '/')
            val worldCreator = WorldCreator.ofNameAndKey(
                worldName,
                NamespacedKey(plugin, folder.name)
            )

            worldCreator.environment(env)

            worldCreator.seed(request.seed)
            worldCreator.generateStructures(request.generateStructures)

            val captureWorldCreation = object : Listener {
                @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
                fun onInitWorld(ev: WorldInitEvent) {
                    val world = ev.world
                    if (world.name == worldName) {
                        world.keepSpawnInMemory = false
                        world.difficulty = Difficulty.NORMAL
                        HandlerList.unregisterAll(this)
                    }
                }
            }

            server.pluginManager.registerEvents(captureWorldCreation, plugin)

            val world = callSync(plugin) {
                checkNotNull(worldCreator.createWorld())
            }

            worlds[folder.name] = world

            call.respond(folder.name)
        }

        get("/world/release/{worldId}") {
            val worldId = call.parameters["worldId"]
            val current = worlds[worldId] ?: return@get
            callSync(plugin) {
                server.unloadWorld(current, true)
            }
        }

        post<RequestedChunkData>("/chunk/create/{worldId}/{x}/{z}") { requestedData ->
            val request = CreateChunkRequest(
                x = requireNotNull(call.parameters["x"]).toInt(),
                z = requireNotNull(call.parameters["z"]).toInt(),
                worldId = requireNotNull(call.parameters["worldId"]),
            )
            val world = worlds[request.worldId]
            if (world == null) {
                call.response.status(HttpStatusCode.NotFound)
                return@post
            }

            var capturedEntities: Set<Entity>? = null
            val ignoreEntities = requestedData.ignoreEntities
            var remoteChunk = world.getChunkAtAsync(request.x, request.z, true, true).thenApply { chunk ->
                capturedEntities = if (ignoreEntities) emptySet() else EntityCaptureListener[request.x, request.z]
                if (!ignoreEntities && !chunk.isEntitiesLoaded && capturedEntities == null) {
                    plugin.logger.fine { "Entities not captured at x:${request.x} z:${request.z}" }
                    return@thenApply null
                }

                ChunkConverter.convert(
                    plugin = plugin,
                    chunk = chunk,
                    requestedData,
                )?.let { remoteChunk ->
                    if (chunk.isEntitiesLoaded) {
                        remoteChunk.copy(entities = chunk.entities.map {
                            ChunkConverter.convertEntity(it)
                        })
                    } else {
                        remoteChunk
                    }
                }
            }.await()

            if (remoteChunk == null) {
                call.response.status(HttpStatusCode.Locked)
                return@post
            }

            if (!ignoreEntities && remoteChunk.entities.isEmpty()) {
                val captured = capturedEntities
                if (captured != null) {
                    remoteChunk = remoteChunk.copy(entities = captured.map {
                        ChunkConverter.convertEntity(it)
                    })
                }
            }

            server.scheduler.runTaskLater(plugin, Runnable {
                world.unloadChunk(request.x, request.z)
            }, 5)

            call.respond(remoteChunk)
        }
    }
}
