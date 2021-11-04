package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.block.entity.TileEntity

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSTileEntity(override val nms: TileEntity): NMSWrapper<TileEntity> {
    inline val tileType: NMSTileEntityTypes<*>
        get() = NMSTileEntityTypes(nms.tileType)

    inline fun save(nbt: NMSNBTTagCompound): NMSNBTTagCompound {
        return NMSNBTTagCompound(nms.save(nbt.nms))
    }
}
