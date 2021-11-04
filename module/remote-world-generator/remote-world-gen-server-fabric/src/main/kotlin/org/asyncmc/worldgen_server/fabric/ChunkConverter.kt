package org.asyncmc.worldgen_server.fabric

import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtLongArray
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.fluid.Fluid
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.server.world.SimpleTickScheduler
import net.minecraft.state.property.Property
import net.minecraft.util.collection.PackedIntegerArray
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.registry.Registry
import net.minecraft.world.BlockView
import net.minecraft.world.ChunkTickScheduler
import net.minecraft.world.Heightmap
import net.minecraft.world.TickScheduler
import net.minecraft.world.biome.source.BiomeArray
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.chunk.ChunkProvider
import net.minecraft.world.chunk.ChunkSection
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.world.chunk.light.BlockLightStorage
import net.minecraft.world.chunk.light.LightStorage
import net.minecraft.world.chunk.light.SkyLightStorage
import org.asyncmc.worldgen_server.data.*
import br.com.gamemods.nbtmanipulator.NbtCompound as OurNbtCompound

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

    fun convert(chunk: WorldChunk, theServer: MinecraftServer): RemoteChunk {
        val blockEntityRegistry = theServer.registryManager[Registry.BLOCK_ENTITY_TYPE_KEY]
        val entityRegistry = theServer.registryManager[Registry.ENTITY_TYPE_KEY]
        val chunkProvider = object : ChunkProvider {
            override fun getChunk(chunkX: Int, chunkZ: Int): BlockView? {
                return chunk.takeIf { chunkX == chunk.pos.x && chunkZ == chunk.pos.z }
            }

            override fun getWorld(): BlockView {
                return chunk.world
            }
        }

        val blockLightProvider = object : BlockLightStorage(chunkProvider) {}
        val skyLightProvider = object : SkyLightStorage(chunkProvider) {}
        return RemoteChunk(
            chunk.pos.x,
            chunk.pos.z,
            convertBlockPalette(chunk, theServer),
            chunk.blockEntities.entries.map { convertBlockEntity(it, blockEntityRegistry) },
            (chunk.world as ServerWorld).iterateEntities()
                .filter { it.world == chunk.world }
                .toList().asSequence()
                .filter { it.chunkPos == chunk.pos }
                .map { convertEntity(it, entityRegistry) }
                .toList(),
            convertBiomes(checkNotNull(chunk.biomeArray), theServer),
            convertLightMap(chunk, blockLightProvider),
            convertLightMap(chunk, skyLightProvider),
            convertHeightMap(chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING), chunk),
            convertHeightMap(chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES), chunk),
            convertHeightMap(chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR), chunk),
            convertHeightMap(chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG), chunk),
            convertHeightMap(chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE), chunk),
            convertHeightMap(chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG), chunk),
            convertBlockTickSchedule(chunk.blockTickScheduler),
            convertFluidTickSchedule(chunk.fluidTickScheduler),
            emptyList(),
            emptyList(),
            NbtFile("asyncmc:structures", OurNbtCompound().also { structures ->
                structures["START"] = OurNbtCompound(chunk.structureStarts.entries.map { (feature, start) ->
                    feature.name to start.toNbt(chunk.world as ServerWorld, chunk.pos)
                        .toSerializedNbtFile().deserialize().compound
                })
                structures["REFERENCES"] = OurNbtCompound(chunk.structureReferences.entries.map { (feature, ref) ->
                    feature.name to NbtLongArray(ref.toLongArray())
                })
            }).serialize()
        )
    }

    private fun convertLightMap(chunk: WorldChunk, lightProvider: LightStorage<*>): RemoteLightMap {
        return RemoteLightMap(
            chunk.sectionArray.asSequence().flatMapIndexed { index, _ ->
                checkNotNull(lightProvider.getLightSection(ChunkSectionPos.from(chunk.pos, index).asLong()))
                    .asByteArray()
                    .asSequence()
                    .map { it.toInt() }
            }.toList().toIntArray()
        )
    }

    private fun convertFluidTickSchedule(tickScheduler: TickScheduler<Fluid>): SerializedNbtFile {
        return convertTickSchedule(tickScheduler)
    }

    private fun convertBlockTickSchedule(tickScheduler: TickScheduler<Block>): SerializedNbtFile {
        return convertTickSchedule(tickScheduler)
    }

    private fun convertTickSchedule(tickScheduler: TickScheduler<*>): SerializedNbtFile {
        val nbt = when (tickScheduler) {
            is ChunkTickScheduler<*> -> tickScheduler.toNbt()
            is SimpleTickScheduler<*> -> tickScheduler.toNbt()
            else -> throw UnsupportedOperationException("Unsupported tick scheduler type ${tickScheduler::class.java.simpleName}")
        }
        val compound = NbtCompound()
        compound.put("tickScheduler", nbt)
        return compound.toSerializedNbtFile()
    }

    private fun convertBlockPalette(chunk: WorldChunk, theServer: MinecraftServer): List<RemotePaletteBlockStates> {
        val blockRegistry = theServer.registryManager[Registry.BLOCK_KEY]
        val states = chunk.sectionArray.asSequence()
            .flatMap { chunkSection ->
                if (ChunkSection.isEmpty(chunkSection)) {
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
            }.toList()

        return listOf(RemotePaletteBlockStates(states))
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertBlockState(mcState: BlockState, blockRegistry: Registry<Block>): RemoteBlockState {
        return RemoteBlockState(
            id = checkNotNull(blockRegistry.getId(mcState.block)).toString(),
            properties = mcState.entries.entries.associate { (property, value) ->
                property as Property<Comparable<Comparable<*>>>
                property.name to property.name(value as Comparable<Comparable<*>>)
            }
        )
    }

    private fun convertBlockEntity(entry: Map.Entry<BlockPos, BlockEntity>, registry: Registry<BlockEntityType<*>>): RemoteEntity {
        return RemoteEntity(
            id = checkNotNull(registry.getId(entry.value.type)).toString(),
            x = entry.key.x.toFloat(),
            y = entry.key.y.toFloat(),
            z = entry.key.z.toFloat(),
            nbt = entry.value.writeNbt(NbtCompound()).toSerializedNbtFile()
        )
    }

    private fun convertEntity(entity: Entity, registry: Registry<EntityType<*>>): RemoteEntity {
        return RemoteEntity(
            id = checkNotNull(registry.getId(entity.type)).toString(),
            x = entity.x.toFloat(),
            y = entity.y.toFloat(),
            z = entity.z.toFloat(),
            nbt = entity.writeNbt(NbtCompound()).toSerializedNbtFile()
        )
    }

    private fun convertBiomes(biomeMap: BiomeArray, theServer: MinecraftServer): RemotePalettedBiomeMap {
        val registry = theServer.registryManager[Registry.BIOME_KEY]
        val biomes = biomeMap.toIntArray().asSequence()
            .map { checkNotNull(registry[it]) }
            .map { requireNotNull(registry.getId(it)).toString() }
            .toList()
        return RemotePalettedBiomeMap(biomes)
    }

    private fun convertHeightMap(heightmap: Heightmap, chunk: Chunk): RemoteHeightMap {
        val data = IntArray(16*16)
        var index = 0
        PackedIntegerArray(
            MathHelper.log2DeBruijn(chunk.height + 1),
            16*16,
            heightmap.asLongArray()
        ).forEach { height ->
            data[index++] = height
        }
        return RemoteHeightMap(data)
    }
}
