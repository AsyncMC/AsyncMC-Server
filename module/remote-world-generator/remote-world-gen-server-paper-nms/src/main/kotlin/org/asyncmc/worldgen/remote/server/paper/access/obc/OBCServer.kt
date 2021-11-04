package org.asyncmc.worldgen.remote.server.paper.access.obc

import org.asyncmc.worldgen.remote.server.paper.access.nms.asWrappedNMS
import org.bukkit.Server
import org.bukkit.craftbukkit.v1_17_R1.CraftServer

@JvmInline
value class OBCServer(val bukkit: Server) {
    inline val obc get() = bukkit as CraftServer
    inline val nms get() = bukkit.asWrappedNMS().nms
}
