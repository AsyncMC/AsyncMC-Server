package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RemoteLightMaps (
    val blockLight: RemoteLightMap?,
    val skyLight: RemoteLightMap?,
)
