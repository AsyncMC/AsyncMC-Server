package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class ChunkCoordinates (
    val x: Int,
    val z: Int,
    val worldId: String,
)
