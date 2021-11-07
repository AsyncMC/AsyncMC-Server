package org.asyncmc.worldgen.remote.client.powernukkit.biomes

import cn.nukkit.level.biome.impl.ocean.OceanBiome

open class ColdOceanBiome: OceanBiome() {
    override fun getName(): String {
        return "Cold Ocean"
    }
}
