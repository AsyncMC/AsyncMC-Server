package org.asyncmc.worldgen.remote.server.paper.access.obc

import org.asyncmc.worldgen.remote.server.paper.access.nms.NMSTileEntity
import org.bukkit.block.TileState
import org.bukkit.craftbukkit.v1_17_R1.block.CraftBlockEntityState

@JvmInline
value class OBCTileState(val bukkit: TileState) {
    inline val obs: CraftBlockEntityState<*> get() = bukkit as CraftBlockEntityState<*>
    inline val nms: NMSTileEntity get() = NMSTileEntity(obs.tileEntity)
}
