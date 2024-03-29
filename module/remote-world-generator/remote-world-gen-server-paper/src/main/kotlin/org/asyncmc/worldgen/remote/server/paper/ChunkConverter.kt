package org.asyncmc.worldgen.remote.server.paper

import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtLongArray
import com.destroystokyo.paper.loottable.LootableBlockInventory
import org.asyncmc.worldgen.remote.data.*
import org.asyncmc.worldgen.remote.server.paper.access.nms.*
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.block.Container
import org.bukkit.entity.Entity
import org.bukkit.loot.LootContext
import java.util.concurrent.ThreadLocalRandom
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

    val registryNms = Bukkit.getServer().asWrappedNMS().registryManager
    val blockEntityRegistry: NMSIRegistry<*, NMSTileEntityTypes<*>> = registryNms[RegistryAccess.BLOCK_ENTITY_TYPE_REGISTRY]
    val entityRegistry: NMSIRegistry<*, NMSEntityTypes<*>> = registryNms[RegistryAccess.ENTITY_TYPE_REGISTRY]
    val blockRegistry: NMSIRegistry<*, NMSBlock> = registryNms[RegistryAccess.BLOCK_REGISTRY]
    val biomeRegistry: NMSIRegistry<*, NMSBiomeBase> = registryNms[RegistryAccess.BIOME_REGISTRY]

    fun convert(
        plugin: AsyncMcPaperWorldGenServer,
        chunk: Chunk,
        requestedChunkData: RequestedChunkData,
    ): RemoteChunk? {
        val includeHeightMaps = requestedChunkData.heightMaps
        val includeLightMaps =  requestedChunkData.lightMaps
        val includeStructures = requestedChunkData.structures
        val openTreasures = requestedChunkData.openedTreasures
        val chunkNms = chunk.asWrappedNMS()
        val worldNms = chunk.world.asWrappedNMS()

        val blockStates: List<RemotePaletteBlockStates>
        val tileEntities: List<RemoteBlockEntity>
        if (!requestedChunkData.blockStates) {
            blockStates = emptyList()
            tileEntities = emptyList()
        } else {
            val expectedTiles = mutableSetOf<NMSBlockPos>()
            blockStates = convertBlockPalette(chunkNms, blockRegistry, expectedTiles)
            val tiles = chunkNms.tileEntities
            if (!tiles.keys.containsAll(expectedTiles)) {
                plugin.logger.fine { "The following tile entities are missing: ${expectedTiles - tiles.keys}" }
                return null
            }
            tileEntities = if (!requestedChunkData.blockEntities) {
                emptyList()
            } else {
                tiles.map {
                    convertBlockEntity(plugin, it, blockEntityRegistry, chunk, openTreasures)
                }
            }
        }

        return RemoteChunk(
            chunk.x,
            chunk.z,
            chunkNms.minBuildHeight,
            chunkNms.height,
            blockStates,
            tileEntities,
            convertBiomes(checkNotNull(chunkNms.biomeArray), biomeRegistry),
            if (includeLightMaps) TODO() else null, //RemoteLightMap(intArrayOf()),//convertLightMap(chunk, blockLightProvider),
            //if (includeLightMaps) TODO() else null, //RemoteLightMap(intArrayOf()),//convertLightMap(chunk, skyLightProvider),
            if (includeHeightMaps) convertHeightMaps(chunkNms) else null,
            if (requestedChunkData.blockStates) convertPendingTicks(chunkNms) else null,
            if (!includeStructures) null else
                NbtFile("asyncmc:structures", OurNbtCompound().also { structures ->
                    structures["START"] = OurNbtCompound(chunkNms.structureStarts.entries.map { (feature, start) ->
                        feature.name to start.toNbt(worldNms, chunkNms.pos)
                            .toSerializedNbtFile().deserialize().compound
                    })
                    structures["REFERENCES"] = OurNbtCompound(chunkNms.structureReferences.entries.map { (feature, ref) ->
                        feature.name to NbtLongArray(ref.toLongArray())
                    })
                }).serialize(),
        )
    }

    private fun convertHeightMaps(chunkNms: NMSChunk): RemoteHeightMaps {
        val chunkHeightElementBits = NMSMathHelper.log2DeBruijn(chunkNms.height + 1)
        return RemoteHeightMaps(
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.MOTION_BLOCKING), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.MOTION_BLOCKING_NO_LEAVES), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.OCEAN_FLOOR), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.OCEAN_FLOOR_WG), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.WORLD_SURFACE), chunkHeightElementBits),
            convertHeightMap(chunkNms.getHeightmap(NMSHeightMapType.WORLD_SURFACE_WG), chunkHeightElementBits),
        )
    }

    private fun convertPendingTicks(chunkNms: NMSChunk): RemotePendingTickLists {
        return RemotePendingTickLists(
            convertPendingTickSchedule(chunkNms, chunkNms.blockTickScheduler),
            convertPendingTickSchedule(chunkNms, chunkNms.fluidTickScheduler),
        )
    }

    private fun convertPendingTickSchedule(chunkNms: NMSChunk, tickScheduler: NMSTickList<*>): List<RemotePendingTick> {
        val nbtList = tickScheduler.toNbt()
        return nbtList.mapIndexed { index, tag ->
            val compound = tag.asNBTCompound
            if (compound != null) {
                listOf(convertScheduledTick(compound))
            } else {
                tag.asNBTList?.let { convertProtoScheduledTick(chunkNms, it, index) } ?: emptyList()
            }
        }.flatten()
    }

    private fun convertProtoScheduledTick(chunkNms: NMSChunk, nbt: NMSNBTTagList, chunkSectionIndex: Int): List<RemotePendingTick> {
        return nbt.mapIndexed { _, nbtBlockPos ->
            val yOffset = chunkNms.minBuildHeight + chunkSectionIndex * 16
            nbtBlockPos.asNBTShort?.value?.toInt()?.let {
                val cx = it and 0xF
                val sy = it shr 4 and 0xF
                val cz = it shr 8 and 0xF
                val id = chunkNms.sections[chunkSectionIndex]?.getBlockState(cx, sy, cz)?.block?.let(blockRegistry::getId)
                RemotePendingTick(
                    x = cx,
                    y = yOffset + sy,
                    z = cz,
                    ticks = 20,
                    priority = 0,
                    blockId = id?.toString()
                )
            }
        }.filterNotNull()
    }

    private fun convertScheduledTick(nbt: NMSNBTTagCompound): RemotePendingTick {
        return RemotePendingTick(
            x = nbt.getInt("x"),
            y = nbt.getInt("y"),
            z = nbt.getInt("z"),
            ticks = -nbt.getInt("t"),
            priority = nbt.getInt("p"),
            blockId = nbt.getString("i")
        )
    }

    private fun convertBlockPalette(chunk: NMSChunk, blockRegistry: NMSIRegistry<*, NMSBlock>, expectedTiles: MutableSet<NMSBlockPos>): List<RemotePaletteBlockStates> {
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
                                    if (mcState.block.hasTile) {
                                        val chunkPos = chunk.pos
                                        expectedTiles += NMSBlockPos(
                                            chunkPos.firstBlockX + x,
                                            y + chunkSection.yPos,
                                            chunkPos.firstBlockZ + z,
                                        )
                                    }
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

    private fun convertBlockEntity(
        plugin: AsyncMcPaperWorldGenServer,
        entry: Map.Entry<NMSBlockPos, NMSTileEntity>,
        registry: NMSIRegistry<*, NMSTileEntityTypes<*>>,
        chunk: Chunk,
        openTreasures: Boolean
    ): RemoteBlockEntity {
        val (pos, tile) = entry
        val previousNbt = tile.save(NMSNBTTagCompound())
        var revert = false
        if (openTreasures && tile.isLootableInventoryHolder) {
            val state = chunk.getBlock(pos.x and 0xF, pos.y, pos.z and 0xF).state as? LootableBlockInventory
            val lootTable = state?.lootTable
            if (lootTable != null && state is Container) {
                val inv = state.inventory
                inv.clear()
                lootTable.fillInventory(
                    inv,
                    ThreadLocalRandom.current(),
                    LootContext.Builder((state as LootableBlockInventory).block.location).build()
                )
            }
            revert = true
        }
        return RemoteBlockEntity(
            id = checkNotNull(registry.getId(tile.tileType)).toString(),
            x = pos.x,
            y = pos.y,
            z = pos.z,
            nbt = tile.save(NMSNBTTagCompound()).toSerializedNbtFile()
        ).also {
            if (revert) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    tile.load(previousNbt)
                })
            }
        }
    }

    fun convertEntity(entity: Entity): RemoteEntity {
        val entityNms = entity.asWrappedNMS()
        val pos = entity.location
        return RemoteEntity(
            id = checkNotNull(entityRegistry.getId(entityNms.entityType)).toString(),
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
