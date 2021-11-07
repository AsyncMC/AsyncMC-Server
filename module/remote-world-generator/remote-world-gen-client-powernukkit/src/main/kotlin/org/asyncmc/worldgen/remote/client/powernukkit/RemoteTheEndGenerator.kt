package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.level.Level

@PublishedApi
internal class RemoteTheEndGenerator(options: Map<String, Any>) : RemoteGenerator(options) {
    @Suppress("unused")
    constructor(): this(emptyMap())

    override val fallbackBiome = 9.toUByte()

    override fun getId(): Int {
        return TYPE_OLD
    }

    override fun getName(): String = NAME

    override fun getDimension(): Int {
        return Level.DIMENSION_THE_END
    }

    companion object {
        const val NAME = "asyncmc_remote_the_end"
    }
}
