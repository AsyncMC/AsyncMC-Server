package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.ITileEntity

/**
 * Block State
 */
@JvmInline
value class NMSBlock(override val nms: Block): NMSWrapper<Block> {
    inline val hasTile: Boolean get() = nms is ITileEntity
}
