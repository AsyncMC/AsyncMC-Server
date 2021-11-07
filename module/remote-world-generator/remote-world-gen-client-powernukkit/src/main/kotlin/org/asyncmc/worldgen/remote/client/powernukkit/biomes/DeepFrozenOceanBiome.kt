package org.asyncmc.worldgen.remote.client.powernukkit.biomes

import cn.nukkit.level.biome.impl.ocean.FrozenOceanBiome

open class DeepFrozenOceanBiome: FrozenOceanBiome() {
    override fun getName(): String {
        return "Deep Frozen Ocean"
    }
}
