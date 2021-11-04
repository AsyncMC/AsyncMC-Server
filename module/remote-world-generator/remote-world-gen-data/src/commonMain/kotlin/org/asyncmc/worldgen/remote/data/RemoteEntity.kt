package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RemoteEntity(
    val id: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val nbt: SerializedNbtFile,
)
