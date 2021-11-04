package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.levelgen.feature.StructureGenerator
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration

@JvmInline
value class NMSStructureGenerator<C: WorldGenFeatureConfiguration>(
    override val nms: StructureGenerator<C>
) : NMSWrapper<StructureGenerator<C>> {
    val name: String get() = nms.g()
}
