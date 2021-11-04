package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class CreateChunkRequest (
    val x: Int,
    val z: Int,
    val worldId: String,
)
