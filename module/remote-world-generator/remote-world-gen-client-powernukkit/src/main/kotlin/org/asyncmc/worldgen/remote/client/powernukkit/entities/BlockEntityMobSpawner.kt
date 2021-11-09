package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.block.BlockID
import cn.nukkit.blockentity.BlockEntitySpawnable
import cn.nukkit.level.format.FullChunk
import cn.nukkit.nbt.tag.CompoundTag


class BlockEntityMobSpawner(chunk: FullChunk?, nbt: CompoundTag?) : BlockEntitySpawnable(chunk, nbt) {
    override fun isBlockEntityValid(): Boolean {
        return levelBlock.id == BlockID.MOB_SPAWNER
    }

    override fun getSpawnCompound(): CompoundTag {
        val tag = CompoundTag()
            .putString("id", MOB_SPAWNER)
            .putInt("x", x.toInt())
            .putInt("y", y.toInt())
            .putInt("z", z.toInt())

        if (namedTag.containsString("EntityIdentifier")) {
            tag.putString("EntityIdentifier", namedTag.getString("EntityIdentifier"))
        } /*else if (namedTag.containsInt("EntityId")) {
            val id = namedTag.getInt("EntityId")
            RemoteToPowerNukkitConverter.getEntityNamespacedId(id)?.let { namespaced ->
                tag.putString("EntityIdentifier", namespaced)
            }
        }*/ else {
            tag.putInt("EntityId", namedTag.getInt("EntityId"))
        }

        return tag
    }
}
