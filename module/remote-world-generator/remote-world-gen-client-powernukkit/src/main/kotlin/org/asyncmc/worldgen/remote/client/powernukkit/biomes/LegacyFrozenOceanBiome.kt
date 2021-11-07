package org.asyncmc.worldgen.remote.client.powernukkit.biomes

import cn.nukkit.level.biome.impl.ocean.FrozenOceanBiome

open class LegacyFrozenOceanBiome: FrozenOceanBiome() {
    override fun getName(): String {
        return "Legacy Frozen Ocean"
    }
}
