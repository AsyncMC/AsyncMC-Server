package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.block.Block
import cn.nukkit.block.BlockID
import cn.nukkit.block.BlockUnknown
import cn.nukkit.blockentity.BlockEntity
import cn.nukkit.blockproperty.BooleanBlockProperty
import cn.nukkit.blockstate.BlockState
import cn.nukkit.item.Item
import cn.nukkit.level.biome.Biome
import cn.nukkit.level.biome.EnumBiome
import cn.nukkit.level.generator.Generator
import cn.nukkit.scheduler.Task
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.http.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonPrimitive
import org.asyncmc.worldgen.remote.client.powernukkit.RemoteToPowerNukkitConverter.LayeredBlockState
import org.asyncmc.worldgen.remote.client.powernukkit.biomes.*
import org.asyncmc.worldgen.remote.client.powernukkit.entities.*
import org.asyncmc.worldgen.remote.client.powernukkit.listeners.EntityFixer
import org.asyncmc.worldgen.remote.data.RemoteBlockState
import org.powernukkit.plugins.kotlin.KotlinPluginBase
import org.powernukkit.plugins.kotlin.fatal
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Paths


@PublishedApi
internal class AsyncMcWorldGenPowerNukkitClientPlugin: KotlinPluginBase() {
    lateinit var backend: Url
    lateinit var httpClient: HttpClient
    private var embeddedBackend: EmbeddedBackend? = null

    override fun onEnable() {
        try {
            throwingEnable()
        } catch (e: Throwable) {
            try {
                log.fatal(e) {
                    "An error has occurred while enabling the async world generator plugin and the server loading cannot proceed safely!"
                }
            } finally {
                try {
                    server.pluginManager.disablePlugin(this)
                } finally {
                    server.scheduler.scheduleTask(object : Task() {
                        override fun onRun(currentTick: Int) {
                            server.shutdown()
                        }
                    })
                }
            }
        }
    }

    override fun onDisable() {
        embeddedBackend?.stop()
        embeddedBackend?.join()
    }

    private fun throwingEnable() {
        saveDefaultConfig()
        reloadConfig()
        registerBiomes()

        server.pluginManager.registerEvents(EntityFixer(), this)
        if (server.pluginManager.getPlugin("MobPlugin")?.isEnabled != true) {
            BlockEntity.registerBlockEntity(BlockEntity.MOB_SPAWNER, BlockEntityMobSpawner::class.java)
        }

        if (config.getBoolean("backend.embedded.use-embedded", true)) {
            val backend = EmbeddedBackend(this)
            embeddedBackend = backend
            runBlocking {
                with(backend) {
                    configureBackendFiles().joinAll()
                    val backendData = startBackend()
                    config["backend.url"] = "http://127.0.0.1:${backendData.port}"
                    config["backend.secret-security-appid"] = backendData.secretAppId
                    config["backend.secret-security-token"] = backendData.secretToken
                }
            }
        } else {
            config["backend.url"] = config.getString("backend.external.url", "url")
            config["backend.secret-security-appid"] = config.getString("backend.external.secret-security-appid", "paste-it-here")
            config["backend.secret-security-token"] = config.getString("backend.external.secret-security-token", "paste-it-here")
        }

        backend = try {
            Url(config.getString("backend.url", "").also {
                require(it.isNotBlank()) {
                    "No default world backend was defined, the AsyncMc Remote World Generation plugin cannot continue. " +
                            "Please provide the backend information in the AsyncMcRemoteWorldGenClient/config.yml file"
                }
            })
        } catch (e: Exception) {
            log.error(e) {
                "Invalid URL providded at AsyncMcRemoteWorldGenClient/config.yml -> backend.url"
            }
            throw e
        }

        httpClient = HttpClient(CIO) {
            install(Auth) {
                basic {
                    sendWithoutRequest { request ->
                        request.url.host == backend.host && request.url.port == backend.port
                    }
                    credentials {
                        realm = "Remote World Generator"
                        BasicAuthCredentials(
                            username = config.getString("backend.secret-security-appid", "paste-it-here").also {
                                require(it != "paste-it-here") {
                                    "Please provide the backend information in the AsyncMcRemoteWorldGenClient/config.yml file."
                                }
                            },
                            password = config.getString("backend.secret-security-token").also {
                                require(it != "paste-it-here") {
                                    "Please provide the backend information in the AsyncMcRemoteWorldGenClient/config.yml file."
                                }
                            },
                        )
                    }
                }
            }
        }

        RemoteToPowerNukkitConverter.detectBlockStatesWithEntity()
        loadEnchantmentMappings()
        loadItemMappings()
        loadBiomeMappings()
        loadBlockMappings()
        loadEntityMappings()

        Generator.addGenerator(RemoteOverworldGenerator::class.java, RemoteOverworldGenerator.NAME, 0)
        Generator.addGenerator(RemoteNetherGenerator::class.java, RemoteNetherGenerator.NAME, 0)
        Generator.addGenerator(RemoteTheEndGenerator::class.java, RemoteTheEndGenerator.NAME, 0)
    }

    private fun loadEntityMappings() {
        val factories = useResource("$MAPPINGS/../entities.txt") { input ->
            input.bufferedReader().lineSequence()
                .filter { it.isNotBlank() }
                .map { it.split(':', limit = 2) }
                .associate { (remoteId, nukkitId) -> "minecraft:$remoteId" to nukkitId.takeIf { it.isNotBlank() } }
                .mapValues { (_, nukkitId) -> GenericEntityFactory(nukkitId) }
        }
        RemoteToPowerNukkitConverter.addEntityFactories(factories)
        sequenceOf(
            "giant" to GiantEntityFactory(),
            "item_frame" to ItemFrameEntityConverter(),
            "end_crystal" to EndCrystalEntityFactory(),
            "shulker" to ShulkerEntityFactory(),
            "villager" to VillagerEntityFactory(),
            "sheep" to SheepEntityFactory(),
            "cat" to CatEntityFactory(),
            "panda" to PandaEntityFactory(),
            "parrot" to ParrotEntityFactory(),
        ).associate { (id, factory) -> "minecraft:$id" to factory }
            .also { RemoteToPowerNukkitConverter.addEntityFactories(it) }

        /*val bedrockEntityIds = useResource("$MAPPINGS/../bedrock-entity-ids.txt") { input ->
            input.bufferedReader().lineSequence()
                .filter { it.isNotBlank() }
                .map { it.split('=', limit = 2) }
                .associateTo(Int2ObjectOpenHashMap()) { (numeric, string) -> numeric.toInt() to string }
        }

        RemoteToPowerNukkitConverter.addEntityIds(bedrockEntityIds)*/
    }

    private fun registerBiomes() {
        sequenceOf(
            9 to TheEndBiome(),
            13 to SnowyMountainsBiome(),
            40 to WarmOceanBiome(),
            41 to LukewarmOceanBiome(),
            42 to ColdOceanBiome(),
            43 to DeepWarmOceanBiome(),
            44 to DeepLukewarmOceanBiome(),
            45 to DeepColdOceanBiome(),
            46 to DeepFrozenOceanBiome(),
            47 to LegacyFrozenOceanBiome(),
            161 to GiantSpruceTaigaHillsBiome(),
            168 to BambooJungleBiome(),
            169 to BambooJungleHillsBiome(),
            178 to SoulSandForestBiome(),
            179 to CrimsonForestBiome(),
            180 to WarpedForestBiome(),
            181 to BasaltDeltasBiome(),
        ).forEach { (id, biome) ->
            registerBiomeIfAbsent(id, biome)
        }
    }

    private fun registerBiomeIfAbsent(id: Int, biome: Biome) {
        val registered = Biome.getBiome(id)
        if (registered !== EnumBiome.OCEAN.biome || id == registered.id) {
            return
        }
        BiomeRegisterer.registerBiome(id, biome)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadBiomeMappings() {
        val mappings = useResource("$MAPPINGS/biomes.json") { input ->
            Json.Default.decodeFromStream<Map<String, Map<String, Int>>>(input)
        }.mapValues { (_, map) ->
            map["bedrock_id"]!!.toUByte()
        }
        RemoteToPowerNukkitConverter.addBiomeMappings(mappings)
    }

    private fun loadEnchantmentMappings() {
        val mappings = useResource("$MAPPINGS/../enchantments.txt") { input ->
            input.bufferedReader().lineSequence()
                .filter { it.isNotBlank() }
                .map { it.split(':', limit = 2) }
                .map { (java, nukkit) -> "minecraft:$java" to nukkit.toInt() }
                .filter { (_, nukkit) -> nukkit >= 0 }
                .associate { (java, nukkit) -> java to nukkit }
        }
        RemoteToPowerNukkitConverter.addEnchantmentMappings(mappings)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadItemMappings() {
        val rawMapping = useResource("$MAPPINGS/items.json") { input ->
            @Suppress("JSON_FORMAT_REDUNDANT")
            Json {
                ignoreUnknownKeys = true
            }.decodeFromStream<Map<String, ItemMapping>>(input)
        }

        val mappings = rawMapping.mapValues { (_, mapping) ->
            RemoteToPowerNukkitConverter.ItemIdData(Item.fromString(mapping.bedrockIdentifier).id, mapping.bedrockData)
        }

        RemoteToPowerNukkitConverter.addItemMappings(mappings)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadBlockMappings() {
        val implicitlyWaterlogged = useResource("$MAPPINGS/../implicitly-waterlogged.txt") { input ->
            input.bufferedReader().readLines().toSet()
        }

        val replacements = useResource("$MAPPINGS/../id-replacements.txt") { input ->
            input.bufferedReader().lineSequence()
                .filter { it.isNotBlank() }
                .map { it.split(':', limit = 2) }
                .associate { (key, replacement) ->
                    "minecraft:$key" to if (';' !in replacement) {
                        IdReplacement("minecraft:$replacement")
                    } else {
                        val propsKV = replacement.split(';')
                        val props = propsKV.asSequence().drop(1)
                            .map { it.split('=', limit = 2) }
                            .associate { (k, v) -> k to v }
                        IdReplacement("minecraft:" + propsKV.first(), props)
                    }
                }
        }

        val rawMappings = useResource("$MAPPINGS/blocks.json") { input ->
            @Suppress("JSON_FORMAT_REDUNDANT")
            Json {
                ignoreUnknownKeys = true
            }.decodeFromStream<Map<String, BlockMapping>>(input)
        }
        val mappings = rawMappings.mapKeys { (key) ->
            val openIndex = key.indexOf('[')
            if (openIndex < 0) {
                RemoteBlockState(key, emptyMap())
            } else {
                require(key.last() == ']')
                val id = key.substring(0, openIndex)
                val properties = key.substring(openIndex + 1, key.lastIndex).split(',').associate {
                    val kv = it.split('=', limit = 2)
                    kv[0] to kv[1]
                }
                RemoteBlockState(id, properties)
            }
        }.mapValues { (remote, mapping) ->
            val fluid = if (remote.properties["waterlogged"] == "true" || remote.id in implicitlyWaterlogged) {
                BlockState.of(BlockID.WATER)
            } else {
                BlockState.AIR
            }

            val replaced: Boolean

            val baseState = try {
                if (remote.id == "minecraft:raw_iron_block") {
                    println("tracking")
                }
                val replacement = replacements[remote.id]
                replaced = replacement != null
                val bedrockId = (replacement?.id ?: mapping.bedrockIdentifier).lowercase()
                val initialProps = replacement?.properties ?: emptyMap()
                val initialState = BlockState.of(bedrockId).also {
                    @Suppress("DEPRECATION")
                    require(it.blockId < Block.MAX_BLOCK_ID && it.block !is BlockUnknown) {
                        "The block $mapping ($replacement) is not implemented by this version of PowerNukkit"
                    }
                }
                initialProps.entries.fold(initialState) { current, (name, value) ->
                    try {
                        val property = current.getProperty(name)
                        if (property is BooleanBlockProperty) {
                            current.withProperty(property, value.toBoolean())
                        } else {
                            current.withProperty(name, value)
                        }
                    } catch (e: Exception) {
                        log.error(e) {
                            "Could not apply the property $name with value $value to $current for the mapping: $remote -> $mapping ($replacement)"
                        }
                        current
                    }
                }
            } catch (_: Exception) {
                log.error { "Unsupported block mapping for $remote -> $mapping" }
                return@mapValues LayeredBlockState(BlockState.of(BlockID.STONE))
            }

            val main = mapping.bedrockStates?.entries?.fold(baseState) { current, (name, value) ->
                try {
                    val property = current.getProperty(name)
                    if (property is BooleanBlockProperty) {
                        current.withProperty(property, value.jsonPrimitive.content.toBoolean())
                    } else if (name == "coral_hang_type_bit") {
                        val int = if (value.jsonPrimitive.content == "true") 1 else 0
                        val persistence = property.getPersistenceValueForMeta(int)
                        current.withProperty(name, persistence)
                    } else {
                        current.withProperty(name, value.jsonPrimitive.content)
                    }
                } catch (e: Exception) {
                    val msg = {
                        "Could not apply the property $name with value $value to $current for the mapping: $remote -> $mapping"
                    }
                    if (replaced) {
                        log.debug(e, msg)
                    } else {
                        log.error(msg)
                    }
                    current
                }
            } ?: baseState

            if (main == BlockState.AIR) {
                LayeredBlockState(fluid, main)
            } else {
                LayeredBlockState(main, fluid)
            }
        }
        RemoteToPowerNukkitConverter.addToBlockCache(mappings)
    }

    internal inline fun <R> useResource(filename: String, block: (InputStream) -> R): R {
        val normalized = Paths.get(filename).normalize().joinToString("/")
        return requireNotNull(getResource(normalized)) {
            throw FileNotFoundException(normalized)
        }.use(block)
    }

    private data class IdReplacement(val id: String, val properties: Map<String, String> = emptyMap())

    @Serializable
    internal data class BlockMapping(
        @SerialName("bedrock_identifier")
        val bedrockIdentifier: String,
        @SerialName("bedrock_states")
        val bedrockStates: JsonObject? = null
    )

    @Serializable
    internal data class ItemMapping(
        @SerialName("bedrock_identifier")
        val bedrockIdentifier: String,
        @SerialName("bedrock_data")
        val bedrockData: Int,
    )

    companion object {
        private const val MAPPINGS = "org/asyncmc/worldgen/remote/client/powernukkit/mappings"
    }
}
