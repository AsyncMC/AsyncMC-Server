package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RemotePalettedBiomeMap(
    val biomes: List<String>,
    val data: IntArray,
) {
    private constructor(palette: List<String>, data: List<String>): this(palette, data.map { palette.indexOf(it) }.toIntArray())
    public constructor(data: List<String>): this(data.distinct(), data)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RemotePalettedBiomeMap

        if (biomes != other.biomes) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = biomes.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
