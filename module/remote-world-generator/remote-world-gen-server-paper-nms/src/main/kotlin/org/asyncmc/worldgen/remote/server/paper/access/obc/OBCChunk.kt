package org.asyncmc.worldgen.remote.server.paper.access.obc

import org.asyncmc.worldgen.remote.server.paper.access.nms.asWrappedNMS
import org.bukkit.Chunk
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk

@JvmInline
value class OBCChunk(val bukkit: Chunk) {
    inline val obc get() = bukkit as CraftChunk
    inline val nms get() = bukkit.asWrappedNMS().nms
}
