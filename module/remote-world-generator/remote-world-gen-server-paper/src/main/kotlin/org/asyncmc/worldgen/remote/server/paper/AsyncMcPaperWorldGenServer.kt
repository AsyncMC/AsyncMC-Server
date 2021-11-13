package org.asyncmc.worldgen.remote.server.paper

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.asyncmc.worldgen.remote.server.paper.webserver.plugins.*
import org.bukkit.plugin.java.JavaPlugin
import java.security.SecureRandom
import java.util.logging.Level

class AsyncMcPaperWorldGenServer: JavaPlugin() {
    private var webServer: CIOApplicationEngine? = null

    override fun onEnable() {
        tryOrFail {
            saveDefaultConfig()
            reloadConfig()
            var appid: String = config.getString("webserver.secret-security-appid", "generate")!!
            var token: String = config.getString("webserver.secret-security-token", "generate")!!
            var save = false
            if (appid == "generate") {
                appid = generateString(8)
                config.set("webserver.secret-security-appid", appid)
                save = true
            }
            if (token == "generate") {
                token = generateString(32)
                config.set("webserver.secret-security-token", token)
                save = true
            }
            if (save) {
                saveConfig()
                logger.warning {
                    "A new security token/appid was generated, please check the AsyncMcPaperWorldGenServer/config.yml file to get it."
                }
            }
            val port = config.getInt("webserver.port", -1).takeIf { it != -1 } ?: (server.port + 1)
            EntityCaptureListener.startCleanupTask(this)
            server.pluginManager.registerEvents(EntityCaptureListener, this)
            startServer(port, appid, token)
        }
    }

    private fun generateString(bytes: Int) = ByteArray(bytes).also { buffer ->
        SecureRandom.getInstanceStrong().nextBytes(buffer)
    }.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

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

    private fun startServer(port: Int, appId: String, token: String) {
        val plugin = this
        val server = embeddedServer(CIO, host = server.ip.takeIf { it.isNotBlank() } ?: "0.0.0.0", port = port) {
            configureSecurity(appId, token)
            configureRouting(plugin)
            configureSerialization()
            if (config.getBoolean("webserver.enable.call-logging", true)) {
                configureMonitoring(config)
            }
            configureHTTP(config)
            if (config.getBoolean("webserver.enable.metrics", true)) {
                configureMicrometerMetrics()
            }
        }
        webServer = server
        server.start(wait = false)
    }
}
