package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.TickList
import net.minecraft.world.level.TickListChunk
import net.minecraft.world.level.TickListEmpty
import net.minecraft.world.level.chunk.ProtoChunkTickList

@JvmInline
value class NMSTickList<T>(override val nms: TickList<T>) : NMSWrapper<TickList<T>> {
    fun toNbt(): NMSNBTTagList {
        return when (val tickList = nms) {
            is TickListChunk<*> -> NMSNBTTagList(tickList.b())
            is TickListEmpty<*> -> NMSNBTTagList()
            is ProtoChunkTickList<*> -> NMSNBTTagList(tickList.b())
            else -> throw UnsupportedOperationException("Unsupported tick scheduler type ${tickList::class.java.simpleName}")
        }
    }
}
