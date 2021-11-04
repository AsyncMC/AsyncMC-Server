package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.server.level.WorldServer
import net.minecraft.world.level.World
import org.asyncmc.worldgen.remote.server.paper.access.obc.asWrappedOBC
import org.bukkit.Server
import org.bukkit.World as BukkitWorld

@JvmInline
value class NMSWorld(val bukkit: BukkitWorld): NMSWrapper<World> {
    inline val obc get() = bukkit.asWrappedOBC().obc
    override val nms: WorldServer get() = obc.handle as WorldServer
    inline val server: Server get() = nms.craftServer
}
