package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.entity.Entity
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.math.Vector3
import cn.nukkit.nbt.tag.CompoundTag
import cn.nukkit.nbt.tag.DoubleTag
import cn.nukkit.nbt.tag.FloatTag
import org.asyncmc.worldgen.remote.client.powernukkit.deserializeForNukkit
import org.asyncmc.worldgen.remote.data.RemoteChunk
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal abstract class EntityFactory {
    abstract fun createEntity(remoteChunk: RemoteChunk, remoteEntity: RemoteEntity, chunk: BaseFullChunk): Entity?

    protected open fun adjustNbt(
        remoteChunk: RemoteChunk,
        remoteEntity: RemoteEntity,
        nukkitId: String,
        chunk: BaseFullChunk,
        entityNbt: CompoundTag,
        nbt: CompoundTag,
    ): Boolean = true

    protected open fun createEntity(remoteChunk: RemoteChunk, remoteEntity: RemoteEntity, nukkitId: String?, chunk: BaseFullChunk): Entity? {
        if (nukkitId.isNullOrBlank() || nukkitId in unknownEntities) {
            return null
        }
        val entityNbt = remoteEntity.nbt.deserializeForNukkit()
        val pos = entityNbt.getList("Pos", DoubleTag::class.java)
        val motion = entityNbt.getList("Motion", DoubleTag::class.java)
        val rotation = entityNbt.getList("Rotation", FloatTag::class.java)
        val nbt = Entity.getDefaultNBT(
            Vector3(pos[0].data, pos[1].data, pos[2].data),
            Vector3(motion[0].data, motion[1].data, motion[2].data),
            rotation[0].data,
            rotation[1].data
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
        if (adjustNbt(remoteChunk, remoteEntity, nukkitId, chunk, entityNbt, nbt)) {
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
