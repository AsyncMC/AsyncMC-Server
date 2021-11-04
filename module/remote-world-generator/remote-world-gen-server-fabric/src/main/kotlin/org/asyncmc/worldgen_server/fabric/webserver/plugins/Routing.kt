package org.asyncmc.worldgen_server.fabric.webserver.plugins

import com.mojang.serialization.Lifecycle
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import net.minecraft.resource.DataPackSettings
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.RegistryKey
import net.minecraft.world.Difficulty
import net.minecraft.world.GameMode
import net.minecraft.world.dimension.DimensionType
import net.minecraft.world.gen.GeneratorOptions
import net.minecraft.world.level.LevelInfo
import net.minecraft.world.level.LevelProperties
import org.apache.commons.io.FileUtils
import org.asyncmc.worldgen_server.data.CreateChunkRequest
import org.asyncmc.worldgen_server.data.PrepareWorldRequest
import org.asyncmc.worldgen_server.data.RemoteChunk
import org.asyncmc.worldgen_server.fabric.ChunkConverter
import org.asyncmc.worldgen_server.fabric.IMinecraftServer
import org.asyncmc.worldgen_server.fabric.NoOpWorldGenerationProgressListener
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.name

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalCoroutinesApi::class)
fun Application.configureRouting(minecraftServer: IMinecraftServer) {
    val theServer = minecraftServer.theServer
    val theWorkerExecutor = minecraftServer.theWorkerExecutor
    val theSession = minecraftServer.theSession

    val worlds = ConcurrentHashMap<String, ServerWorld>()
    val worldsFolder = theServer.runDirectory.toPath().resolve("served-worlds")
    FileUtils.deleteDirectory(worldsFolder.toFile())
    Files.createDirectories(worldsFolder)
    routing {
        post<PrepareWorldRequest>("/world/prepare") { request ->
            val folderAsync = async(Dispatchers.IO) {
                Files.createTempDirectory(
                    worldsFolder,
                    request.name.replace(Regex("[^a-zA-Z0-9_-]"), "X")
                )
            }
            val dimensionTypeRegistry = theServer.registryManager[Registry.DIMENSION_TYPE_KEY]
            val folder = folderAsync.await()
            val levelProperties = request.extractProperties(folder, theServer)
            val world = ServerWorld(
                theServer,
                theWorkerExecutor,
                theSession,
                levelProperties,
                RegistryKey.of(Registry.WORLD_KEY, Identifier("asyncmc", folder.name)),
                when (val id = request.dimension.id) {
                    "minecraft:overworld" -> dimensionTypeRegistry[DimensionType.OVERWORLD_ID]
                    "minecraft:the_nether" -> dimensionTypeRegistry[DimensionType.THE_NETHER_ID]
                    "minecraft:the_end" -> dimensionTypeRegistry[DimensionType.THE_END_ID]
                    else -> throw UnsupportedOperationException("Dimension type $id is not supported")
                },
                NoOpWorldGenerationProgressListener,
                if (request.dimension.id == "minecraft:overworld") {
                    levelProperties.generatorOptions.chunkGenerator
                } else {
                    TODO()
               },
                false,
                request.seed,
                emptyList(),
                false,
            )
            worlds[folder.name] = world
            call.respond(folder.name)
        }

        get("/chunk/create/{worldId}/{x}/{z}") {
            val request = CreateChunkRequest(
                x = requireNotNull(call.parameters["x"]).toInt(),
                z = requireNotNull(call.parameters["z"]).toInt(),
                worldId = requireNotNull(call.parameters["worldId"]),
            )
            val world = worlds[request.worldId]
            if (world == null) {
                call.response.status(HttpStatusCode.NotFound)
                return@get
            }
            val chunk = world.getChunk(request.x, request.z)
            val remoteChunk: RemoteChunk = ChunkConverter.convert(chunk, theServer)
            call.respond(remoteChunk)
        }
    }
}

fun PrepareWorldRequest.extractProperties(folder: Path, theServer: MinecraftServer): LevelProperties {
    val registryManager = theServer.registryManager
    val dimensionTypes = registryManager[Registry.DIMENSION_TYPE_KEY]
    val biomes = registryManager[Registry.BIOME_KEY]
    val generatorSettingsRegistry = registryManager[Registry.CHUNK_GENERATOR_SETTINGS_KEY]
    return LevelProperties (
        LevelInfo(
            folder.name,
            GameMode.SURVIVAL,
            false,
            Difficulty.values()[difficulty],
            true,
            theServer.gameRules,
            DataPackSettings.SAFE_MODE
        ),
        GeneratorOptions(seed, generateStructures, false,
            GeneratorOptions.getRegistryWithReplacedOverworldGenerator(
                dimensionTypes,
                DimensionType.createDefaultDimensionOptions(
                    dimensionTypes,
                    biomes,
                    generatorSettingsRegistry,
                    seed
                ),
                GeneratorOptions.createOverworldGenerator(
                    biomes,
                    generatorSettingsRegistry,
                    seed
                )
            )
        ),
        Lifecycle.stable(),
    )
}
