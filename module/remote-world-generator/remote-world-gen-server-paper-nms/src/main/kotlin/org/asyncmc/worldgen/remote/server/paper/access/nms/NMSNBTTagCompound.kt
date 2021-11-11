package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.nbt.NBTTagCompound

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSNBTTagCompound(override val nms: NBTTagCompound): NMSWrapper<NBTTagCompound> {
    constructor(): this(NBTTagCompound())

    inline operator fun set(key: String, value: NMSNBTTagList) {
        nms[key] = value.nms
    }

    inline fun getInt(key: String): Int {
        return nms.getInt(key)
    }

    inline fun getString(key: String): String {
        return nms.getString(key)
    }
}
