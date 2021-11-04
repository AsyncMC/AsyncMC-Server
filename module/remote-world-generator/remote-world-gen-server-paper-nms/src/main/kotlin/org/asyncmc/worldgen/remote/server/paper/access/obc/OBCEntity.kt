package org.asyncmc.worldgen.remote.server.paper.access.obc

import org.asyncmc.worldgen.remote.server.paper.access.nms.asWrappedNMS
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity
import org.bukkit.entity.Entity

@JvmInline
value class OBCEntity(val bukkit: Entity) {
    inline val obc get() = bukkit as CraftEntity
    inline val nms get() = bukkit.asWrappedNMS().nms
}
