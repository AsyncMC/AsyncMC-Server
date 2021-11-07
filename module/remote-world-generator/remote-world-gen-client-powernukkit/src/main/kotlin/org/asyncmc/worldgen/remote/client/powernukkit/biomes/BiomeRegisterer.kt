package org.asyncmc.worldgen.remote.client.powernukkit.biomes

import cn.nukkit.level.biome.Biome

internal object BiomeRegisterer: Biome() {
    fun registerBiome(id: Int, biome: Biome) {
        register(id, biome)
    }

    override fun getName(): String {
        return "Fake"
    }
}
