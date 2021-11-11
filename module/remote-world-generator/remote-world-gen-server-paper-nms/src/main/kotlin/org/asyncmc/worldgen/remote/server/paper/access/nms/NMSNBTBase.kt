package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagShort

@JvmInline
value class NMSNBTBase(override val nms: NBTBase) : NMSWrapper<NBTBase> {
    inline val asNBTShort: NMSNBTTagShort? get() = (nms as? NBTTagShort)?.let(::NMSNBTTagShort)
    inline val asNBTList: NMSNBTTagList? get() = (nms as? NBTTagList)?.let(::NMSNBTTagList)
    inline val asNBTCompound: NMSNBTTagCompound? get() = (nms as? NBTTagCompound)?.let(::NMSNBTTagCompound)
}
