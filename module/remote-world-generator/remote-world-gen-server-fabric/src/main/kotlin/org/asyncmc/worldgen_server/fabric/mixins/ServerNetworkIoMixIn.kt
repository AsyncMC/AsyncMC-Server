package org.asyncmc.worldgen_server.fabric.mixins

import net.minecraft.server.ServerNetworkIo
import org.asyncmc.worldgen_server.fabric.AsyncMcFabricWorldGenServer
import org.spongepowered.asm.mixin.Mixin
import java.io.IOException
import java.net.InetAddress
import java.net.SocketAddress

@Suppress("unused")
@Mixin(ServerNetworkIo::class)
class ServerNetworkIoMixIn {
    @Throws(IOException::class)
    fun bind(address: InetAddress?, port: Int) {
        AsyncMcFabricWorldGenServer.serverAddress = address
        AsyncMcFabricWorldGenServer.port = port
        AsyncMcFabricWorldGenServer.configured = true
    }

    @Suppress("UNUSED_PARAMETER")
    @Throws(IOException::class)
    fun bindLocal(address: InetAddress?, port: Int): SocketAddress? {
        throw UnsupportedOperationException()
    }
}
