package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RemotePendingTick (
    val x: Int,
    val y: Int,
    val z: Int,
    val ticks: Int,
    val priority: Int,
    val blockId: String?
)
