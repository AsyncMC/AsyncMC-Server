package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RemoteChunk(
    val x: Int,
    val z: Int,
    val minY: Int,
    val maxY: Int,
    val blockLayers: List<RemotePaletteBlockStates>,
    val blockEntities: List<RemoteBlockEntity>,
    val biomeMap: RemotePalettedBiomeMap,
    val lightMaps: RemoteLightMaps?,
    val heightMaps: RemoteHeightMaps?,
    val pendingTicks: RemotePendingTickLists?,
    val structures: SerializedNbtFile?,
)
