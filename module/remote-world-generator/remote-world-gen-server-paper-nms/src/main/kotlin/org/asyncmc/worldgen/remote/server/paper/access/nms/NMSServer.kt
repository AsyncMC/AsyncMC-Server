package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.server.dedicated.DedicatedServer
import org.asyncmc.worldgen.remote.server.paper.access.obc.asWrappedOBC
import org.bukkit.Server
import java.net.SocketAddress

@JvmInline
value class NMSServer(val bukkit: Server): NMSWrapper<DedicatedServer> {
    inline val obc get() = bukkit.asWrappedOBC().obc
    override val nms: DedicatedServer get() = obc.server

    inline var port: Int
        get() = nms.port
        set(value) {
            nms.port = value
        }

    inline val registryManager: NMSRegistryManager
        get() = NMSRegistryManager(nms.l)

    inline val socketAddress: SocketAddress? get() = nms.serverConnection?.a()
}
