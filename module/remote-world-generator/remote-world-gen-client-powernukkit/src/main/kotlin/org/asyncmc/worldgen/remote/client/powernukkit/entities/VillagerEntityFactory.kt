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

        val villagerData = entityNbt.getCompound("VillagerData")
        nbt.putInt("TradeExperience", entityNbt.getInt("Xp"))
        nbt.putInt("TradeTier", villagerData.getInt("level"))
        if (villagerData.containsString("profession")) {
            nbt.putString("ProfessionV2Identifier", villagerData.getString("profession"))
        }
        if (villagerData.containsString("type")) {
            nbt.putString("VillagerV2Type", villagerData.getString("type"))
        }
        return super.adjustNbt(remoteEntity, nukkitId, chunk, entityNbt, nbt)
    }
}
