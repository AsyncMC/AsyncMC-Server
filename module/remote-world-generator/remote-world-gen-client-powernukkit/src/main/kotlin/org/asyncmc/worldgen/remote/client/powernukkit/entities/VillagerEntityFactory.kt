package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.entity.passive.EntityVillager
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.nbt.tag.CompoundTag
import org.asyncmc.worldgen.remote.client.powernukkit.plugin
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
        if (nbt.getString("ProfessionV2Identifier").takeUnless { it.isBlank() || it == "minecraft:none" } == null
            && plugin.config.getBoolean("randomize-villager-professions", true)
        ) {
            nbt.putString("ProfessionV2Identifier", villagerProfessions.keys.random())
        }
        if (villagerData.containsString("type")) {
            nbt.putString("VillagerV2Type", villagerData.getString("type"))
        }
        return super.adjustNbt(remoteEntity, nukkitId, chunk, entityNbt, nbt)
    }

    companion object {
        val villagerProfessions = mapOf(
            "minecraft:none" to 0,
            "minecraft:farmer" to 1,
            "minecraft:fisherman" to 2,
            "minecraft:shepherd" to 3,
            "minecraft:fletcher" to 4,
            "minecraft:librarian" to 5,
            "minecraft:cartographer" to 6,
            "minecraft:cleric" to 7,
            "minecraft:armorer" to 8,
            "minecraft:weaponsmith" to 9,
            "minecraft:toolsmith" to 10,
            "minecraft:butcher" to 11,
            "minecraft:leatherworker" to 12,
            "minecraft:mason" to 13,
            "minecraft:nitwit" to 14,
        )
    }
}
