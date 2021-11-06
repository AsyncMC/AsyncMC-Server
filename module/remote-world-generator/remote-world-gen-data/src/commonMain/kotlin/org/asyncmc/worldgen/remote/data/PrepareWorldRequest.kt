package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class PrepareWorldRequest(
    val dimension: MinecraftDimension,
    val seed: Long,
    val generateStructures: Boolean,
)
