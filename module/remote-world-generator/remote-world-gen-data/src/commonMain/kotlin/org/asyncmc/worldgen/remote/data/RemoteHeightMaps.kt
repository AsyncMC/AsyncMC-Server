package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RemoteHeightMaps (
    val heightMapMotionBlocking: RemoteHeightMap?,
    val heightMapMotionBlockingNoLeaves: RemoteHeightMap?,
    val heightMapMotionOceanFloor: RemoteHeightMap?,
    val heightMapMotionOceanFloorForWorldGen: RemoteHeightMap?,
    val heightMapMotionSurface: RemoteHeightMap?,
    val heightMapMotionSurfaceForWorldGen: RemoteHeightMap?,
)
