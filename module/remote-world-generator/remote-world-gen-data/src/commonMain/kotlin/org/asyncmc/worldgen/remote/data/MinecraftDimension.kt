package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
public value class MinecraftDimension(public val id: String) {
    public companion object {
        public val DIMENSION_OVERWORLD: MinecraftDimension = MinecraftDimension("minecraft:overworld")
        public val DIMENSION_THE_NETHER: MinecraftDimension = MinecraftDimension("minecraft:the_nether")
        public val DIMENSION_THE_END: MinecraftDimension = MinecraftDimension("minecraft:the_end")
    }
}
