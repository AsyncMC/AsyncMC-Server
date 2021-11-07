package org.asyncmc.worldgen.remote.client.powernukkit.biomes

import cn.nukkit.level.biome.impl.HellBiome

open class WarpedForestBiome: HellBiome() {
    override fun getName(): String {
        return "Warped Forest"
    }
}
