package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.nbt.NBTTagShort

@JvmInline
value class NMSNBTTagShort(override val nms: NBTTagShort) : NMSWrapper<NBTTagShort> {
    inline val value get() = nms.asShort()
}
