package org.asyncmc.worldgen.remote.server.paper.webserver.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.serialization.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalSerializationApi::class)
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
        serialization(ContentType.Application.ProtoBuf, ProtoBuf.Default)
    }
}
