package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RemoteBlockState (
    val id: String,
    val properties: Map<String, String>,
)
