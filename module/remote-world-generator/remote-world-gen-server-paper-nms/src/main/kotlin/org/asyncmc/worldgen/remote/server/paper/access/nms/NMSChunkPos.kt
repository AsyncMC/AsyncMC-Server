package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.ChunkCoordIntPair

@JvmInline
value class NMSChunkPos(override val nms: ChunkCoordIntPair) : NMSWrapper<ChunkCoordIntPair>
