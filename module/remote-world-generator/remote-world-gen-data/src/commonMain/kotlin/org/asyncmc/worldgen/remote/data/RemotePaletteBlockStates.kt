package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public class RemotePaletteBlockStates(
    private val palette: List<RemoteBlockState>,
    private val data: IntArray,
): AbstractList<RemoteBlockState>() {
    private constructor(palette: List<RemoteBlockState>, data: List<RemoteBlockState>): this(palette, data.map { palette.indexOf(it) }.toIntArray())
    public constructor(data: List<RemoteBlockState>): this(data.distinct(), data)

    override val size: Int get() = data.size

    override fun get(index: Int): RemoteBlockState {
        return palette[data[index]]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false

        other as RemotePaletteBlockStates

        if (palette != other.palette) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + palette.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}
