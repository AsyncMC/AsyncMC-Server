package org.asyncmc.server.math

import kotlinx.serialization.Serializable

@Serializable
public data class BlockPos(val x: Int, val y: Int, val z: Int)
