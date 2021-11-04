package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.block.entity.TileEntity
import net.minecraft.world.level.block.entity.TileEntityTypes

@JvmInline
value class NMSTileEntityTypes<T: TileEntity>(override val nms: TileEntityTypes<T>): NMSWrapper<TileEntityTypes<T>>
