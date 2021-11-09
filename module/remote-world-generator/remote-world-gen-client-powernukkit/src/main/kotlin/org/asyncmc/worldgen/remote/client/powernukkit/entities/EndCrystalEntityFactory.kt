package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.nbt.tag.CompoundTag
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal class EndCrystalEntityFactory: GenericEntityFactory("EndCrystal") {
    override fun adjustNbt(
        remoteEntity: RemoteEntity,
        nukkitId: String,
        chunk: BaseFullChunk,
        entityNbt: CompoundTag,
        nbt: CompoundTag
    ): Boolean {
        nbt.putBoolean("ShowBottom", entityNbt.getBoolean("ShowBottom"))
        return super.adjustNbt(remoteEntity, nukkitId, chunk, entityNbt, nbt)
    }
}
