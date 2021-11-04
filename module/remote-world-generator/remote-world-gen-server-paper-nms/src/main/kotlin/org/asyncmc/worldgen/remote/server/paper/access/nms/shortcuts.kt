@file:Suppress("NOTHING_TO_INLINE")

package org.asyncmc.worldgen.remote.server.paper.access.nms

import org.bukkit.Chunk
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.entity.Entity

inline fun Server.asWrappedNMS() = NMSServer(this)
inline fun Chunk.asWrappedNMS() = NMSChunk(this)
inline fun World.asWrappedNMS() = NMSWorld(this)
inline fun Entity.asWrappedNMS() = NMSEntity(this)
