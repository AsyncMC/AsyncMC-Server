package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
public value class BlockPosInsideChunk(private val xyz: Int) {
    public constructor(x: Int, y: Int, z: Int): this((x and 0xF) or (z and 0xF shl 4) or (y shl 8))
    public val x: Int get() = xyz and 0xF
    public val z: Int get() = xyz ushr 4 and 0xF
    public val y: Int get() = xyz shr 8
}
