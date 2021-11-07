package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.level.Level
import cn.nukkit.level.biome.EnumBiome

@PublishedApi
internal class RemoteNetherGenerator(options: Map<String, Any>) : RemoteGenerator(options) {
    @Suppress("unused")
    constructor(): this(emptyMap())

    override val fallbackBiome = EnumBiome.HELL.id.toUByte()

    override fun getName(): String = NAME

    override fun getId(): Int {
        return TYPE_NETHER
    }

    override fun getDimension(): Int {
        return Level.DIMENSION_NETHER
    }

    companion object {
        const val NAME = "asyncmc_remote_the_nether"
    }
}
