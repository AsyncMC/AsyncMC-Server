package org.asyncmc.worldgen.remote.client.powernukkit.biomes

import cn.nukkit.level.biome.Biome

open class TheEndBiome: Biome() {
    override fun getName(): String {
        return "The End"
    }

    override fun canRain(): Boolean {
        return false
    }
}
