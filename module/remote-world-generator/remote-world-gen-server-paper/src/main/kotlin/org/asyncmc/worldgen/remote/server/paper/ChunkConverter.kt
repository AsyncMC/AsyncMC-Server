package org.asyncmc.worldgen.remote.server.paper

import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtLongArray
import org.asyncmc.worldgen.remote.data.*
import org.asyncmc.worldgen.remote.server.paper.access.nms.*
import org.bukkit.Chunk
import org.bukkit.entity.Entity
import br.com.gamemods.nbtmanipulator.NbtCompound as OurNbtCompound

@Suppress("TYPE_MISMATCH_WARNING")
object ChunkConverter {
    private val air = RemoteBlockState("minecraft:air", emptyMap())

    private val emptyChunkSection = sequence {
        for (y in 0..0xF) {
            for (x in 0..0xF) {
                for (z in 0..0xF) {
                    yield(air)
                }
            }
        }
    }.toList()

    fun convert(chunk: Chunk): RemoteChunk {
        val chunkNms = chunk.asWrappedNMS()
        val worldNms = chunk.world.asWrappedNMS()
        val serverNms = worldNms.server.asWrappedNMS()
        val registryNms = serverNms.registryManager

        val blockEntityRegistry: NMSIRegistry<*, NMSTileEntityTypes<*>> = registryNms[RegistryAccess.BLOCK_ENTITY_TYPE_REGISTRY]
        val entityRegistry: NMSIRegistry<*, NMSEntityTypes<*>> = registryNms[RegistryAccess.ENTITY_TYPE_REGISTRY]
        val blockRegistry: NMSIRegistry<*, NMSBlock> = registryNms[RegistryAccess.BLOCK_REGISTRY]
        val biomeRegistry: NMSIRegistry<*, NMSBiomeBase> = registryNms[RegistryAccess.BIOME_REGISTRY]

        /*val chunkProvider = object : ChunkProvider {
            override fun getChunk(chunkX: Int, chunkZ: Int): BlockView? {
                return chunk.takeIf { chunkX == chunk.pos.x && chunkZ == chunk.pos.z }
            }

            override fun getWorld(): BlockView {
                return chunk.world
            }
        }

        val blockLightProvider = object : BlockLightStorage(chunkProvider) {}
        val skyLightProvider = object : SkyLightStorage(chunkProvider) {}*/
        val chunkHeightElementBits = NMSMathHelper.log2DeBruijn(chunkNms.height + 1)
        return RemoteChunk(
            chunk.x,
            chunk.z,
            chunkNms.minBuildHeight,
            chunkNms.height,
            convertBlockPalette(chunkNms, blockRegistry),
            chunkNms.tileEntities.entries.map { convertBlockEntity(it, blockEntityRegistry) },
            chunk.entities.map { convertEntity(it, entityRegistry) },
            convertBiomes(checkNotNull(chunkNms.biomeArray), biomeRegistry),
            RemoteLightMap(intArrayOf()),//convertLightMap(chunk, blockLightProvider),
            RemoteLightMap(intArrayOf()),//convertLightMap(chunk, skyLightProvider),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.MOTION_BLOCKING), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.MOTION_BLOCKING_NO_LEAVES), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.OCEAN_FLOOR), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.OCEAN_FLOOR_WG), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.WORLD_SURFACE), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.WORLD_SURFACE_WG), chunkHeightElementBits),
            convertBlockTickSchedule(chunkNms.blockTickScheduler),
            convertFluidTickSchedule(chunkNms.fluidTickScheduler),
            emptyList(),
            emptyList(),
            NbtFile("asyncmc:structures", OurNbtCompound().also { structures ->
                structures["START"] = OurNbtCompound(chunkNms.structureStarts.entries.map { (feature, start) ->
                    feature.name to start.toNbt(worldNms, chunkNms.pos)
                        .toSerializedNbtFile().deserialize().compound
                })
                structures["REFERENCES"] = OurNbtCompound(chunkNms.structureReferences.entries.map { (feature, ref) ->
                    feature.name to NbtLongArray(ref.toLongArray())
                })
            }).serialize()
        )
    }

    /*private fun convertLightMap(chunk: WorldChunk, lightProvider: LightStorage<*>): RemoteLightMap {
        return RemoteLightMap(
            chunk.sectionArray.asSequence().flatMapIndexed { index, _ ->
                checkNotNull(lightProvider.getLightSection(ChunkSectionPos.from(chunk.pos, index).asLong()))
                    .asByteArray()
                    .asSequence()
                    .map { it.toInt() }
            }.toList().toIntArray()
        )
    }*/

    private fun convertFluidTickSchedule(tickScheduler: NMSTickList<*>): SerializedNbtFile {
        return convertTickSchedule(tickScheduler)
    }

    private fun convertBlockTickSchedule(tickScheduler: NMSTickList<*>): SerializedNbtFile {
        return convertTickSchedule(tickScheduler)
    }

    private fun convertTickSchedule(tickScheduler: NMSTickList<*>): SerializedNbtFile {
        val nbt = tickScheduler.toNbt()
        val compound = NMSNBTTagCompound()
        compound["tickScheduler"] = nbt
        return compound.toSerializedNbtFile()
    }

    private fun convertBlockPalette(chunk: NMSChunk, blockRegistry: NMSIRegistry<*, NMSBlock>): List<RemotePaletteBlockStates> {
        val states = chunk.sections
            .flatMap { chunkSection ->
                if (chunkSection == null || NMSChunkSection.isEmpty(chunkSection)) {
                    emptyChunkSection.asSequence()
                } else {
                    sequence {
                        for (y in 0..0xF) {
                            for (x in 0..0xF) {
                                for (z in 0..0xF) {
                                    val mcState = chunkSection.getBlockState(x, y, z)
                                    val converted = convertBlockState(mcState, blockRegistry)
                                    yield(converted)
                                }
                            }
                        }
                    }
                }
            }

        return listOf(RemotePaletteBlockStates(states))
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertBlockState(mcState: NMSIBlockData, blockRegistry: NMSIRegistry<*, NMSBlock>): RemoteBlockState {
        return RemoteBlockState(
            id = checkNotNull(blockRegistry.getId(mcState.block)).toString(),
            properties = mcState.properties().entries.associate { (propertyNms, value) ->
                propertyNms.name to propertyNms.uncheckedName(value)
            }
        )
    }

    private fun convertBlockEntity(entry: Map.Entry<NMSBlockPos, NMSTileEntity>, registry: NMSIRegistry<*, NMSTileEntityTypes<*>>): RemoteEntity {
        val (pos, tile) = entry
        return RemoteEntity(
            id = checkNotNull(registry.getId(tile.tileType)).toString(),
            x = pos.x.toFloat(),
            y = pos.y.toFloat(),
            z = pos.z.toFloat(),
            nbt = tile.save(NMSNBTTagCompound()).toSerializedNbtFile()
        )
    }

    private fun convertEntity(entity: Entity, registry: NMSIRegistry<*, NMSEntityTypes<*>>): RemoteEntity {
        val entityNms = entity.asWrappedNMS()
        val pos = entity.location
        return RemoteEntity(
            id = checkNotNull(registry.getId(entityNms.entityType)).toString(),
            x = pos.x.toFloat(),
            y = pos.y.toFloat(),
            z = pos.z.toFloat(),
            nbt = entityNms.save(NMSNBTTagCompound()).toSerializedNbtFile()
        )
    }

    private fun convertBiomes(biomeMap: NMSBiomeStorage, registry: NMSIRegistry<*, NMSBiomeBase>): RemotePalettedBiomeMap {
        val biomes = biomeMap.toIntArray().asSequence()
            .map { checkNotNull(registry[it]) }
            .map { requireNotNull(registry.getId(it)).toString() }
            .toList()
        return RemotePalettedBiomeMap(biomes)
    }

    private fun convertHeightMap(heightmap: NMSHeightMap, chunkHeightElementBits: Int): RemoteHeightMap {
        val data = IntArray(16*16)
        NMSDataBits(
            chunkHeightElementBits,
            16*16,
            heightmap.asLongArray()
        ).forEachIndexed { index, height ->
            data[index] = height
        }
        return RemoteHeightMap(data)
    }
}
