package org.asyncmc.server.math

import kotlinx.serialization.Serializable

@Serializable
public data class ChunkPos(val x: Int, val z: Int) {
    public fun minBlockPos(minHeight: Int = 0): BlockPos = BlockPos(x shl 4, minHeight, z shl 4)
    public fun maxBlockPos(maxHeight: Int = 255): BlockPos = BlockPos(x shl 4 + 0xF, maxHeight, z shl 4 + 0xF)
}
