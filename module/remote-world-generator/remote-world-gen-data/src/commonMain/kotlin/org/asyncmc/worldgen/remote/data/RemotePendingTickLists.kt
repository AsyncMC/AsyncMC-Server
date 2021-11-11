package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RemotePendingTickLists (
    val blocks: List<RemotePendingTick>,
    val liquid: List<RemotePendingTick>,
)
