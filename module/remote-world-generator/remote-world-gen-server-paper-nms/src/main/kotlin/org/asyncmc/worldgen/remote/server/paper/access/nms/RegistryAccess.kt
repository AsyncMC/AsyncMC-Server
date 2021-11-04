package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.core.IRegistry
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.level.biome.BiomeBase
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.TileEntityTypes

object RegistryAccess {
    val BLOCK_REGISTRY: NMSResourceKey<IRegistry<Block>, NMSIRegistry<Block, NMSBlock>>
        get() = NMSResourceKey(IRegistry.j) {
            NMSIRegistry(it, ::NMSBlock)
        }

    val BLOCK_ENTITY_TYPE_REGISTRY: NMSResourceKey<IRegistry<TileEntityTypes<*>>, NMSIRegistry<TileEntityTypes<*>, NMSTileEntityTypes<*>>>
        get() = NMSResourceKey(IRegistry.p) { reg ->
            NMSIRegistry(reg) {
                NMSTileEntityTypes(it)
            }
        }

    inline val ENTITY_TYPE_REGISTRY: NMSResourceKey<IRegistry<EntityTypes<*>>, NMSIRegistry<EntityTypes<*>, NMSEntityTypes<*>>>
        get() = NMSResourceKey(IRegistry.l) { reg ->
            NMSIRegistry(reg) {
                NMSEntityTypes(it)
            }
        }

    inline val BIOME_REGISTRY: NMSResourceKey<IRegistry<BiomeBase>, NMSIRegistry<BiomeBase, org.asyncmc.worldgen.remote.server.paper.access.nms.NMSBiomeBase>>
        get() = NMSResourceKey(IRegistry.aO) {
            NMSIRegistry(it, ::NMSBiomeBase)
        }
}
