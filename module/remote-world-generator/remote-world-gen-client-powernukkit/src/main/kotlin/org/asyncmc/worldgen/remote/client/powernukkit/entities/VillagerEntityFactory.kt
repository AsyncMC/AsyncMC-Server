package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.entity.passive.EntityVillager
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.nbt.tag.CompoundTag
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal class VillagerEntityFactory: GenericEntityFactory(EntityVillager.NETWORK_ID) {
    override fun adjustNbt(
        remoteEntity: RemoteEntity,
        nukkitId: String,
        chunk: BaseFullChunk,
        entityNbt: CompoundTag,
        nbt: CompoundTag
    ): Boolean {
        //if (entityNbt.containsCompound("Offers")) {
        //    // TODO persistingOffers
        //}

        nbt.putInt("TradeExperience", entityNbt.getInt("Xp"))
        nbt.putInt("TradeTier", entityNbt.getCompound("VillagerData").getInt("level"))
        return super.adjustNbt(remoteEntity, nukkitId, chunk, entityNbt, nbt)
    }
}
