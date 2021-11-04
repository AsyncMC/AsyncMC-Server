package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.biome.BiomeBase

@JvmInline
value class NMSBiomeBase(override val nms: BiomeBase) : NMSWrapper<BiomeBase> {
    constructor(nms: Any): this(nms as BiomeBase)
}
