package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.block.state.IBlockData

/**
 * Block State
 */
@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSIBlockData(override val nms: IBlockData): NMSWrapper<IBlockData> {
    inline val block: NMSBlock get() = NMSBlock(nms.block)
    inline fun properties() = nms.stateMap.entries.associate { (property, value) ->
        NMSIBlockState(property) to value
    }
}
