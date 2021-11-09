package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.entity.Entity
import cn.nukkit.entity.data.ByteEntityData
import cn.nukkit.entity.mob.EntityShulker
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.math.Vector3
import cn.nukkit.math.VectorMath
import cn.nukkit.nbt.tag.CompoundTag
import cn.nukkit.nbt.tag.IntTag
import cn.nukkit.nbt.tag.ListTag
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal class ShulkerEntityFactory: GenericEntityFactory(EntityShulker.NETWORK_ID) {
    override fun adjustNbt(
        remoteEntity: RemoteEntity,
        nukkitId: String,
        chunk: BaseFullChunk,
        entityNbt: CompoundTag,
        nbt: CompoundTag
    ): Boolean {
        if (entityNbt.containsByte("Color")) {
            nbt.putByte("Color", entityNbt.getByte("Color"))
        }
        if (entityNbt.containsByte("Peek")) {
            nbt.putByte("Peek", entityNbt.getByte("Peek"))
        }
        if (entityNbt.containsInt("APX") && entityNbt.containsInt("APY") && entityNbt.containsInt("APZ")) {
            val x = entityNbt.getInt("APX")
            val y = entityNbt.getInt("APY")
            val z = entityNbt.getInt("APZ")
            nbt.putList(ListTag<IntTag>("AttachPos")
                .add(IntTag("", x))
                .add(IntTag("", y))
                .add(IntTag("", z))
            )
            val entityPos = Vector3(remoteEntity.x.toDouble(), remoteEntity.y.toDouble(), remoteEntity.z.toDouble())
            val blockPos = Vector3(x.toDouble(), y.toDouble(), z.toDouble())
            val attachedFace = VectorMath.calculateFace(blockPos, entityPos)
            nbt.putByte("AttachFace", attachedFace.index)
        }
        return super.adjustNbt(remoteEntity, nukkitId, chunk, entityNbt, nbt)
    }

    override fun createEntity(remoteEntity: RemoteEntity, nukkitId: String?, chunk: BaseFullChunk): Entity? {
        val entity = super.createEntity(remoteEntity, nukkitId, chunk) ?: return null
        entity.setDataProperty(ByteEntityData(Entity.DATA_COLOR_2, entity.namedTag.getByte("Color")), false)
        return entity
    }
}
