package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RequestedChunkData(
    val heightMaps: Boolean = false,
    val lightMaps: Boolean = false,
    val structures: Boolean = false,
    val openedTreasures: Boolean = false,
    val blockStates: Boolean = true,
    val blockEntities: Boolean = true,
)
