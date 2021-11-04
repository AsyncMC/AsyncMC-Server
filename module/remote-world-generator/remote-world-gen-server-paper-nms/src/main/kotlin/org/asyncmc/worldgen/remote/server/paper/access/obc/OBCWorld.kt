package org.asyncmc.worldgen.remote.server.paper.access.obc

import org.asyncmc.worldgen.remote.server.paper.access.nms.asWrappedNMS
import org.bukkit.World
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld

@JvmInline
value class OBCWorld(val bukkit: World) {
    inline val obc get() = bukkit as CraftWorld
    inline val nms get() = bukkit.asWrappedNMS().nms
}
