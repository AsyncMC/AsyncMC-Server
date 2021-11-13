package org.asyncmc.worldgen.remote.server.paper.webserver.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import org.bukkit.configuration.file.FileConfiguration

fun Application.configureHTTP(config: FileConfiguration) {
    if (config.getBoolean("webserver.enable.CORS", true)) {
        install(CORS) {
            method(HttpMethod.Options)
            //method(HttpMethod.Put)
            //method(HttpMethod.Delete)
            //method(HttpMethod.Patch)
            header(HttpHeaders.Authorization)
            allowCredentials = true
            val svs = config.getStringList("webserver.CORS-hosts")
            if (svs.isEmpty() || svs.any { it.equals("all", ignoreCase = true) }) {
                anyHost()
            } else {
                hosts += svs
            }
        }
    }
    if (config.getBoolean("webserver.enable.compression", true)) {
        install(Compression) {
            gzip {
                priority = 1.0
            }
            deflate {
                priority = 10.0
                minimumSize(1024) // condition
            }
        }
    }
}
