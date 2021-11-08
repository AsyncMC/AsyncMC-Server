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
    val blockLight: RemoteLightMap?,
    val skyLight: RemoteLightMap?,
    val heightMapMotionBlocking: RemoteHeightMap?,
    val heightMapMotionBlockingNoLeaves: RemoteHeightMap?,
    val heightMapMotionOceanFloor: RemoteHeightMap?,
    val heightMapMotionOceanFloorForWorldGen: RemoteHeightMap?,
    val heightMapMotionSurface: RemoteHeightMap?,
    val heightMapMotionSurfaceForWorldGen: RemoteHeightMap?,
    val blocksPendingTick: SerializedNbtFile,
    val liquidPendingTick: SerializedNbtFile,
    val blocksPendingUpdate: List<BlockPosInsideChunk>,
    val liquidPendingUpdate: List<BlockPosInsideChunk>,
    val structures: SerializedNbtFile?,
    val entities: List<RemoteEntity>,
)
