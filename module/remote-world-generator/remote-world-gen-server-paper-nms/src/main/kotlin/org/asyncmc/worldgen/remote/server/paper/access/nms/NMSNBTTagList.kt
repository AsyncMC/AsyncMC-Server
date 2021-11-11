package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.nbt.NBTTagList

@JvmInline
value class NMSNBTTagList(override val nms: NBTTagList) : NMSWrapper<NBTTagList> {
    constructor(): this(NBTTagList())
    inline fun <reified R> mapIndexed(mapper: (Int, NMSNBTBase) -> R): List<R> {
        return when (val size = nms.size) {
            0 -> emptyList()
            1 -> listOf(mapper(0, NMSNBTBase(nms[0])))
            else -> Array(size) { i ->
                mapper(i, NMSNBTBase(nms[i]))
            }.asList()
        }
    }
}
