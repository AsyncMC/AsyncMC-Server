package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration
import net.minecraft.world.level.levelgen.structure.StructureStart

@JvmInline
value class NMSStructureStart<C : WorldGenFeatureConfiguration>(
    override val nms: StructureStart<C>
) : NMSWrapper<StructureStart<C>> {
    fun toNbt(worldNms: NMSWorld, pos: NMSChunkPos): NMSNBTTagCompound {
        return NMSNBTTagCompound(nms.a(worldNms.nms, pos.nms))
    }
}
