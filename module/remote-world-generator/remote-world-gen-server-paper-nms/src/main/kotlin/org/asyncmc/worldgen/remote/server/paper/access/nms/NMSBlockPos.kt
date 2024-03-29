package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.core.BlockPosition

@JvmInline
value class NMSBlockPos(override val nms: BlockPosition): NMSWrapper<BlockPosition> {
    constructor(x: Int, y: Int, z: Int): this(BlockPosition(x, y, z))
    inline val x: Int get() = nms.x
    inline val y: Int get() = nms.y
    inline val z: Int get() = nms.z
}
