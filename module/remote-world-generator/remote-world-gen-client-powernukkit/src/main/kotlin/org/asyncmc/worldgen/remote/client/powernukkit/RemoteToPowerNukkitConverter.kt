package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.block.Block
import cn.nukkit.block.BlockEntityHolder
import cn.nukkit.block.BlockID
import cn.nukkit.blockproperty.BooleanBlockProperty
import cn.nukkit.blockstate.BlockState
import cn.nukkit.blockstate.BlockStateRegistry
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.asyncmc.worldgen.remote.data.RemoteBlockState
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf

internal object RemoteToPowerNukkitConverter {
    private val blockStateCache = ConcurrentHashMap<RemoteBlockState, LayeredBlockState>()
    private val fallback = LayeredBlockState(BlockState.of(BlockID.STONE))

    private val biomeIds = ConcurrentHashMap<String, UByte>()

    private val blocksWithEntity = IntOpenHashSet()

    fun convert(blockState: RemoteBlockState): LayeredBlockState {
        return blockStateCache.computeIfAbsent(blockState) { state ->
            plugin.log.warn { "Got a state that was not cached: $state" }
            val originalId = state.id
            val baseState = LayeredBlockState(BlockState.of(originalId))
            if (baseState.main.runtimeId == BlockStateRegistry.getUpdateBlockRegistration()) {
                plugin.log.error {
                    "Found an unmapped block id: $originalId, Original State: $state"
                }
                return@computeIfAbsent fallback
            }
            state.properties.entries.fold(baseState) { current, (name, value) ->
                blockStatePropertyConverter(originalId, name, value, current)
            }
        }
    }


    internal fun detectBlockStatesWithEntity() {
        @Suppress("DEPRECATION")
        val blockClasses = Block.list
        blockClasses.asSequence()
            .filterNotNull()
            .map { it.kotlin }
            .filter { BlockEntityHolder::class.isSubclassOf(it) }
            .map { it.createInstance() }
            .forEach {
                blocksWithEntity.add(it.id)
            }
    }

    internal fun addToBlockCache(mappings: Map<RemoteBlockState, LayeredBlockState>) {
        blockStateCache += mappings
    }

    internal fun addBiomeMappings(mappings: Map<String, UByte>) {
        biomeIds += mappings
    }

    private fun blockStatePropertyConverter(id: String, name: String, value: String, current: LayeredBlockState): LayeredBlockState {
        if (name == "waterlogged") {
            return if (value == "true") {
                current.copy(fluid = BlockState.of(BlockID.WATER))
            } else {
                current
            }
        }
        return try {
            val main = current.main
            val property = main.getProperty(name)
            val updated = if (property is BooleanBlockProperty) {
                main.withProperty(property, value.toBoolean())
            } else {
                main.withProperty(name, value)
            }
            current.copy(main = updated)
        } catch (e: Exception) {
            plugin.log.error(e) {
                "Attempted to apply an unsupported property. Original id: $id, Property: $name, Value: $value, current state: $current"
            }
            current
        }
    }

    fun convertBiomeId(biome: String, fallback: UByte): UByte {
        return biomeIds.getOrDefault(biome, fallback)
    }

    fun hasBlockEntity(blockId: Int): Boolean {
        return blocksWithEntity.contains(blockId)
    }

    internal data class LayeredBlockState(
        val main: BlockState,
        val fluid: BlockState = BlockState.AIR
    )
}
