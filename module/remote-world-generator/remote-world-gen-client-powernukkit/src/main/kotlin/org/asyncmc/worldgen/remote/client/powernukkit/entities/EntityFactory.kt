package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.entity.Entity
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.math.Vector3
import cn.nukkit.nbt.tag.CompoundTag
import cn.nukkit.nbt.tag.DoubleTag
import cn.nukkit.nbt.tag.FloatTag
import cn.nukkit.nbt.tag.Tag
import org.asyncmc.worldgen.remote.client.powernukkit.deserializeForNukkit
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal abstract class EntityFactory {
    abstract fun createEntity(remoteEntity: RemoteEntity, chunk: BaseFullChunk): Entity?

    protected open fun adjustNbt(
        remoteEntity: RemoteEntity,
        nukkitId: String,
        chunk: BaseFullChunk,
        entityNbt: CompoundTag,
        nbt: CompoundTag,
    ): Boolean = true

    protected open fun createEntity(remoteEntity: RemoteEntity, nukkitId: String?, chunk: BaseFullChunk): Entity? {
        if (nukkitId.isNullOrBlank() || nukkitId in unknownEntities) {
            return null
        }
        val entityNbt = remoteEntity.nbt.deserializeForNukkit()

        val pos = if (entityNbt.containsList("Pos", Tag.TAG_Double) && entityNbt.getList("Pos").size() == 3) {
            val pos = entityNbt.getList("Pos", DoubleTag::class.java)
            Vector3(pos[0].data, pos[1].data, pos[2].data)
        } else {
            Vector3(remoteEntity.x.toDouble(), remoteEntity.y.toDouble(), remoteEntity.z.toDouble())
        }

        val motion = if (entityNbt.containsList("Motion", Tag.TAG_Double) && entityNbt.getList("Motion").size() == 3) {
            val motion = entityNbt.getList("Motion", DoubleTag::class.java)
            Vector3(motion[0].data, motion[1].data, motion[2].data)
        } else {
            Vector3()
        }

        val rotation = if (entityNbt.containsList("Rotation", Tag.TAG_Float) && entityNbt.getList("Rotation").size() == 2) {
            val rotation = entityNbt.getList("Rotation", FloatTag::class.java)
            floatArrayOf(rotation[0].data, rotation[1].data)
        } else {
            FloatArray(2)
        }

        val nbt = Entity.getDefaultNBT(
            pos,
            motion,
            rotation[0],
            rotation[1],
        )

        nbt.putFloat("FallDistance", entityNbt.getFloat("FallDistance"))
        nbt.putShort("Fire", entityNbt.getShort("Fire"))
        nbt.putShort("Air", entityNbt.getShort("Air"))
        nbt.putBoolean("OnGround", entityNbt.getBoolean("OnGround"))
        nbt.putBoolean("Invulnerable", entityNbt.getBoolean("Invulnerable"))
        nbt.putFloat("Scale", 1F)
        if (entityNbt.containsString("CustomName")) {
            nbt.putString("CustomName", entityNbt.getString("CustomName"))
        }
        if (entityNbt.containsByte("CustomNameVisible")) {
            nbt.putBoolean("CustomNameVisible", entityNbt.getBoolean("CustomNameVisible"))
        }
        if (adjustNbt(remoteEntity, nukkitId, chunk, entityNbt, nbt)) {
            val entity = Entity.createEntity(nukkitId, chunk, nbt)
            if (entity == null) {
                unknownEntities += nukkitId
            }
            return entity
        }
        return null
    }

    companion object {
        val unknownEntities = mutableSetOf<String>()
    }
}
