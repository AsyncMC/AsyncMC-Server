package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.block.Block
import cn.nukkit.block.BlockID
import cn.nukkit.block.BlockUnknown
import cn.nukkit.blockproperty.BooleanBlockProperty
import cn.nukkit.blockstate.BlockState
import cn.nukkit.level.generator.Generator
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.asyncmc.worldgen.remote.client.powernukkit.RemoteToPowerNukkitConverter.LayeredBlockState
import org.asyncmc.worldgen.remote.data.RemoteBlockState
import org.powernukkit.plugins.kotlin.KotlinPluginBase
import java.io.FileNotFoundException
import java.io.InputStream

@PublishedApi
internal class AsyncMcWorldGenPowerNukkitClientPlugin: KotlinPluginBase() {
    lateinit var backend: Url
    lateinit var httpClient: HttpClient

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()
        backend = Url(requireNotNull(config.getString("default-backend")) {
            "No default world backend was defined, the AsyncMc Remote World Generation plugin cannot continue."
        })
        httpClient = HttpClient(CIO)
        Generator.addGenerator(RemoteOverworldGenerator::class.java, RemoteOverworldGenerator.NAME, 0)
        Generator.addGenerator(RemoteNetherGenerator::class.java, RemoteNetherGenerator.NAME, 0)
        Generator.addGenerator(RemoteTheEndGenerator::class.java, RemoteTheEndGenerator.NAME, 0)

        loadBlockMappings()
    }

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
            }.decodeFromString<Map<String, BlockMapping>>(input.bufferedReader().readText())
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
        RemoteToPowerNukkitConverter.addToCache(mappings)
    }

    private inline fun <R> useResource(filename: String, block: (InputStream) -> R): R {
        return requireNotNull(getResource(filename)) {
            throw FileNotFoundException(filename)
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

    companion object {
        private const val MAPPINGS = "org/asyncmc/worldgen/remote/client/powernukkit/mappings"
    }
}
