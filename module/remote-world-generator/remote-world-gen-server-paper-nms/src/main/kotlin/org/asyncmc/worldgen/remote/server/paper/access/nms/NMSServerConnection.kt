package org.asyncmc.worldgen.remote.server.paper.access.nms

import io.netty.channel.AbstractChannel
import io.netty.channel.ChannelFuture
import net.minecraft.server.network.ServerConnection
import java.net.InetSocketAddress

@JvmInline
value class NMSServerConnection(override val nms: ServerConnection) : NMSWrapper<ServerConnection> {
    fun findBoundPort(): Int {
        return ServerConnection::class.java.declaredFields.first { it.type == List::class.java }
            .also { it.isAccessible = true }
            .let { it.get(nms) as List<*> }
            .let { it.first() as ChannelFuture }
            .let { it.channel() as AbstractChannel }
            .let { it.localAddress() as InetSocketAddress }
            .port
    }
}
