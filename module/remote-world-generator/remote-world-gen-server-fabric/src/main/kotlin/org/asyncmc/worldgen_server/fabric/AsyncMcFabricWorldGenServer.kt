package org.asyncmc.worldgen_server.fabric

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import net.fabricmc.api.ModInitializer
import org.asyncmc.worldgen_server.fabric.webserver.plugins.*
import java.net.InetAddress

object AsyncMcFabricWorldGenServer: ModInitializer {
    var configured = false
    var serverAddress: InetAddress? = null
    var port: Int = -1
    var webServer: CIOApplicationEngine? = null
    override fun onInitialize() {
        // Does nothing now
    }

    fun startService(theServer: IMinecraftServer) {
        val server = embeddedServer(CIO, host = serverAddress?.hostAddress ?: "0.0.0.0", port = port) {
            configureRouting(theServer)
            configureSerialization()
            configureMonitoring()
            configureHTTP()
            configureSecurity()
        }
        webServer = server
        server.start(wait = false)
    }

    fun stopService() {
        webServer?.stop(10_000, 30_000)
    }
}
