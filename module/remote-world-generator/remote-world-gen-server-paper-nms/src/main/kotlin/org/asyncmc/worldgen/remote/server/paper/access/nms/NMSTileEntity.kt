package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.block.entity.TileEntity
import net.minecraft.world.level.block.entity.TileEntityLootable

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSTileEntity(override val nms: TileEntity): NMSWrapper<TileEntity> {
    inline val pos: NMSBlockPos get() = NMSBlockPos(nms.position)

    inline val isLootableInventoryHolder: Boolean
        get() = nms is TileEntityLootable

    inline val tileType: NMSTileEntityTypes<*>
        get() = NMSTileEntityTypes(nms.tileType)

    inline fun save(nbt: NMSNBTTagCompound): NMSNBTTagCompound {
        return NMSNBTTagCompound(nms.save(nbt.nms))
    }

    inline fun load(nbt: NMSNBTTagCompound) {
        nms.load(nbt.nms)
    }
}
