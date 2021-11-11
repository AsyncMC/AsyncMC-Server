package org.asyncmc.worldgen.remote.server.paper.webserver.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import org.bukkit.configuration.file.FileConfiguration
import org.slf4j.event.Level

fun Application.configureMonitoring(config: FileConfiguration) {
    val logSuccess = config.getBoolean("webserver.log.success", false)
    val logLocked = config.getBoolean("webserver.log.locked", true)
    val logOthers = config.getBoolean("webserver.log.others", true)
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            val status = call.response.status()
            when {
                status?.isSuccess() == true -> logSuccess
                status == HttpStatusCode.Locked -> logLocked
                else -> logOthers
            }
        }
    }
}
