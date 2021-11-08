package org.asyncmc.worldgen.remote.server.paper

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.asyncmc.worldgen.remote.server.paper.webserver.plugins.*
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class AsyncMcPaperWorldGenServer: JavaPlugin() {
    private var webServer: CIOApplicationEngine? = null

    override fun onEnable() {
        tryOrFail {
            saveDefaultConfig()
            reloadConfig()
            val port = config.getInt("webserver.port", -1).takeIf { it != -1 } ?: (server.port + 1)
            EntityCaptureListener.startCleanupTask(this)
            server.pluginManager.registerEvents(EntityCaptureListener, this)
            startServer(port)
        }
    }

    private inline fun tryOrFail(action: ()->Unit) {
        try {
            action()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "The startup has failed. Shutting down the server", e)
            server.shutdown()
            throw e
        }
    }

    override fun onDisable() {
        webServer?.stop(10_000, 20_000)
    }

    private fun startServer(port: Int) {
        val plugin = this
        val server = embeddedServer(CIO, host = server.ip.takeIf { it.isNotBlank() } ?: "0.0.0.0", port = port) {
            configureRouting(plugin)
            configureSerialization()
            configureMonitoring()
            configureHTTP()
            configureSecurity()
            configureMicrometerMetrics()
        }
        webServer = server
        server.start(wait = false)
    }
}
