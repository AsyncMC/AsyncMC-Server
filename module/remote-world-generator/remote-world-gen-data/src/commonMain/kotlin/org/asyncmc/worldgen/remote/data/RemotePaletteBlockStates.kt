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
}
