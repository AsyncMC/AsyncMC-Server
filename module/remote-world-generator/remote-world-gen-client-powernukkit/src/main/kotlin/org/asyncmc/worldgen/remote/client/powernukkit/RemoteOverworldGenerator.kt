package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.level.Level

@PublishedApi
internal class RemoteOverworldGenerator(options: Map<String, Any>) : RemoteGenerator(options) {
    @Suppress("unused")
    constructor(): this(emptyMap())

    override fun getName(): String = NAME

    override fun getDimension(): Int {
        return Level.DIMENSION_OVERWORLD
    }

    companion object {
        const val NAME = "asyncmc_remote_overworld"
    }
}
