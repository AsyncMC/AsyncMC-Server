package org.asyncmc.worldgen.remote.client.powernukkit.biomes

import cn.nukkit.level.biome.impl.ocean.OceanBiome

open class WarmOceanBiome: OceanBiome() {
    override fun getName(): String {
        return "Warm Ocean"
    }
}
