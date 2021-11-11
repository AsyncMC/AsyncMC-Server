package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.entity.passive.EntityPanda
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.nbt.tag.CompoundTag
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal class PandaEntityFactory: GenericEntityFactory(EntityPanda.NETWORK_ID) {
    override fun adjustNbt(
        remoteEntity: RemoteEntity,
        nukkitId: String,
        chunk: BaseFullChunk,
        entityNbt: CompoundTag,
        nbt: CompoundTag
    ): Boolean {
        if (entityNbt.containsString("HiddenGene")) {
            nbt.putString("HiddenGene", entityNbt.getString("HiddenGene"))
        }
        if (entityNbt.containsString("MainGene")) {
            nbt.putString("MainGene", entityNbt.getString("MainGene"))
        }
        return super.adjustNbt(remoteEntity, nukkitId, chunk, entityNbt, nbt)
    }
}
