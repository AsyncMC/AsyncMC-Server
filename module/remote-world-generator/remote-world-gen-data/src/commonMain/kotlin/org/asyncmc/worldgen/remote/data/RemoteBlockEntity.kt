package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RemoteBlockEntity(
    val id: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val nbt: SerializedNbtFile,
)
