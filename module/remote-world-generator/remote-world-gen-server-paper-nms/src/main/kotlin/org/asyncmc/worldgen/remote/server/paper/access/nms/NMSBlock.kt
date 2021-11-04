package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.block.Block

/**
 * Block State
 */
@JvmInline
value class NMSBlock(override val nms: Block): NMSWrapper<Block>
