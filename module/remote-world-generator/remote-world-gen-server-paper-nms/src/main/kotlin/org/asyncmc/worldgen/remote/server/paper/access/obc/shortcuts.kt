@file:Suppress("NOTHING_TO_INLINE")

package org.asyncmc.worldgen.remote.server.paper.access.obc

import org.bukkit.Chunk
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Entity

inline fun Server.asWrappedOBC() = OBCServer(this)
inline fun Chunk.asWrappedOBC() = OBCChunk(this)
inline fun World.asWrappedOBC() = OBCWorld(this)
inline fun Entity.asWrappedOBC() = OBCEntity(this)
