package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.nbt.tag.CompoundTag
import org.asyncmc.worldgen.remote.data.RemoteChunk
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal class GiantEntityFactory: GenericEntityFactory("Zombie") {
    override fun adjustNbt(
        remoteChunk: RemoteChunk,
        remoteEntity: RemoteEntity,
        nukkitId: String,
        chunk: BaseFullChunk,
        entityNbt: CompoundTag,
        nbt: CompoundTag
    ): Boolean {
        super.adjustNbt(remoteChunk, remoteEntity, nukkitId, chunk, entityNbt, nbt)
        nbt.putFloat("Scale", 10F)
        return true
    }
}
