package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RequestedChunkData(
    val heightMaps: Boolean = false,
    val lightMaps: Boolean = false,
    val structures: Boolean = false,
    val openedTreasures: Boolean = false,
    val monsters: Boolean = true,
    val animals: Boolean = true,
    val structureEntities: Boolean = true,
    val otherEntities: Boolean = true,
) {
    public val ignoreEntities: Boolean get() = !monsters && !animals && !structureEntities && !otherEntities
}