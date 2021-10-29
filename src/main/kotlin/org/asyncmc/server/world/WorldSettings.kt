package org.asyncmc.server.world

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.asyncmc.server.id.WorldGeneratorId

@Serializable
public data class WorldSettings(
    val worldSeed: Long,
    val generator: WorldGeneratorId,
    val generatorSettings: JsonObject,
)
