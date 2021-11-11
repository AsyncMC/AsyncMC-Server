package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.entity.passive.EntityParrot
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.nbt.tag.CompoundTag
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal class ParrotEntityFactory: GenericEntityFactory(EntityParrot.NETWORK_ID) {
    override fun adjustNbt(
        remoteEntity: RemoteEntity,
        nukkitId: String,
        chunk: BaseFullChunk,
        entityNbt: CompoundTag,
        nbt: CompoundTag
    ): Boolean {
        if (entityNbt.containsInt("Variant")) {
            nbt.putInt("Variant", entityNbt.getInt("Variant"))
        }
        return super.adjustNbt(remoteEntity, nukkitId, chunk, entityNbt, nbt)
    }
}
