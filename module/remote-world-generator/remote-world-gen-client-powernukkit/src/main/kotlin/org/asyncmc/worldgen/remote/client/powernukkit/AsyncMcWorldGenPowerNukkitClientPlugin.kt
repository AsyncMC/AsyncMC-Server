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

            val baseState = try {
                BlockState.of(mapping.bedrockIdentifier).also {
                    @Suppress("DEPRECATION")
                    require(it.blockId < Block.MAX_BLOCK_ID && it.block !is BlockUnknown) {
                        "The block $mapping is not implemented by this version of PowerNukkit"
                    }
                }
            } catch (e: Exception) {
                log.error(e) { "Unsupported block mapping for $remote -> $mapping" }
                return@mapValues LayeredBlockState(BlockState.of(BlockID.STONE))
            }

            val main = mapping.bedrockStates?.entries?.fold(baseState) { current, (name, value) ->
                try {
                    val property = current.getProperty(name)
                    if (property is BooleanBlockProperty) {
                        current.withProperty(property, value.jsonPrimitive.content.toBoolean())
                    } else {
                        current.withProperty(name, value.jsonPrimitive.content)
                    }
                } catch (e: Exception) {
                    log.error(e) {
                        "Could not apply the property $name with value $value to $current for the mapping: $remote -> $mapping"
                    }
                    current
                }
            } ?: baseState

            LayeredBlockState(main, fluid)
        }
        RemoteToPowerNukkitConverter.addToCache(mappings)
    }

    private inline fun <R> useResource(filename: String, block: (InputStream) -> R): R {
        return requireNotNull(getResource(filename)) {
            throw FileNotFoundException(filename)
        }.use(block)
    }

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
