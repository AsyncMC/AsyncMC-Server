package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.entity.Entity
import org.asyncmc.worldgen.remote.server.paper.access.obc.asWrappedOBC
import org.bukkit.entity.Entity as BukkitEntity

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSEntity(val bukkit: BukkitEntity): NMSWrapper<Entity> {
    inline val obc get() = bukkit.asWrappedOBC().obc

    override val nms: Entity get() = obc.handle
    inline val entityType: NMSEntityTypes<*>
        get() = NMSEntityTypes(nms.entityType)

    inline fun save(nbtTagCompound: NBTTagCompound): NBTTagCompound {
        return nms.save(nbtTagCompound)
    }

    inline fun save(nbtTagCompound: NMSNBTTagCompound): NMSNBTTagCompound {
        return NMSNBTTagCompound(save(nbtTagCompound.nms))
    }
}
