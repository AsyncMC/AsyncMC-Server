package org.asyncmc.worldgen.remote.server.paper.webserver.plugins

import io.ktor.application.*
import io.ktor.auth.*

fun Application.configureSecurity(appId: String, token: String) {
    authentication {
        basic(name = "clientMinecraftServer") {
            realm = "Remote World Generator"
            validate { credentials ->
                if (credentials.name == appId && credentials.password == token) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }
}
