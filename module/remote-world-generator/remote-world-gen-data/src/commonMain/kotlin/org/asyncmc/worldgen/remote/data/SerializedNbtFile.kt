package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class SerializedNbtFile(
    val littleEndian: Boolean,
    val compressed: Boolean,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SerializedNbtFile

        if (littleEndian != other.littleEndian) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = littleEndian.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
