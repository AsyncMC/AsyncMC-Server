package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.nbt.NBTTagInt

@JvmInline
value class NMSNBTTagInt(override val nms: NBTTagInt) : NMSWrapper<NBTTagInt> {
    inline val value get() = nms.asInt()
}
