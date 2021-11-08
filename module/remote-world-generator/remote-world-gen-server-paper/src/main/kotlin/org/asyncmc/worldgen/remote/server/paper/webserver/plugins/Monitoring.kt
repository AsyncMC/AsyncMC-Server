package org.asyncmc.worldgen.remote.server.paper.webserver.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            //call.request.path().startsWith("/")
            call.response.status()?.isSuccess() == false
        }
    }

}
