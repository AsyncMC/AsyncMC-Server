package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.block.BlockID
import cn.nukkit.blockstate.BlockState
import cn.nukkit.blockstate.BlockStateRegistry
import org.asyncmc.worldgen.remote.data.RemoteBlockState
import java.util.concurrent.ConcurrentHashMap

internal object RemoteToPowerNukkitConverter {
    private val blockStateCache = ConcurrentHashMap<RemoteBlockState, LayeredBlockState>()
    private val fallback = LayeredBlockState(BlockState.of(BlockID.STONE))

    fun convert(blockState: RemoteBlockState): LayeredBlockState {
        return blockStateCache.computeIfAbsent(blockState) { state ->
            val originalId = state.id
            val baseState = LayeredBlockState(BlockState.of(blockIdConverter(originalId)))
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

    internal fun addToCache(mappings: Map<RemoteBlockState, LayeredBlockState>) {
        blockStateCache += mappings
    }

    private fun blockIdConverter(id: String): String {
        return id
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
            current.copy(main = current.main.withProperty(name, value))
        } catch (e: Exception) {
            plugin.log.error(e) {
                "Attempted to apply an unsupported property. Original id: $id, Property: $name, Value: $value, current state: $current"
            }
            current
        }
    }

    internal data class LayeredBlockState(
        val main: BlockState,
        val fluid: BlockState = BlockState.AIR
    )
}
