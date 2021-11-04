package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.nbt.NBTTagList

@JvmInline
value class NMSNBTTagList(override val nms: NBTTagList) : NMSWrapper<NBTTagList> {
    constructor(): this(NBTTagList())
}
