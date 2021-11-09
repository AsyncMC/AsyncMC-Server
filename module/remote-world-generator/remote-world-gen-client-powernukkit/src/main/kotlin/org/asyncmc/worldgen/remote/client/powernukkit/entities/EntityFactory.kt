package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.block.BlockID
import cn.nukkit.entity.Entity
import cn.nukkit.item.Item
import cn.nukkit.item.ItemID
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.math.Vector3
import cn.nukkit.nbt.NBTIO
import cn.nukkit.nbt.tag.*
import org.asyncmc.worldgen.remote.client.powernukkit.RemoteToPowerNukkitConverter
import org.asyncmc.worldgen.remote.client.powernukkit.deserializeForNukkit
import org.asyncmc.worldgen.remote.data.RemoteEntity
import java.util.*

internal abstract class EntityFactory {
    abstract val nukkitId: OptionalInt

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

        val age = entityNbt.getInt("Age")
        nbt.putBoolean("IsBaby", age < 0)
        nbt.putBoolean("Baby", age < 0) // MobPlugin support
        nbt.putInt("Age", age) // MobPlugin support

        //if (entityNbt.containsString("CustomName")) {
        //    nbt.putString("CustomName", entityNbt.getString("CustomName"))
        //}

        //if (entityNbt.containsByte("CustomNameVisible")) {
        //    nbt.putBoolean("CustomNameVisible", entityNbt.getBoolean("CustomNameVisible"))
        //}

        if (entityNbt.containsByte("PersistenceRequired") && entityNbt.getBoolean("PersistenceRequired")) {
            nbt.putBoolean("Persistent", true)
        }

        if (entityNbt.containsCompound("SaddleItem")) {
            val saddle = RemoteToPowerNukkitConverter.convertItem(entityNbt.getCompound("SaddleItem")) ?: air
            if (saddle.id == ItemID.SADDLE) {
                nbt.putBoolean("Saddled", true)
            }
        }

        if (entityNbt.containsByte("ChestedHorse")) {
            nbt.putBoolean("Chested", true)
        }

        if (entityNbt.getInt("AngerTime") > 0) {
            nbt.putBoolean("IsAngry", true)
        }


        if (entityNbt.containsInt("Variant")) {
            nbt.putInt("Variant", entityNbt.getInt("Variant"))
        }

        if (entityNbt.containsList("ArmorItems", Tag.TAG_Compound)) {
            val armor = entityNbt.getList("ArmorItems", CompoundTag::class.java)
            val feet = RemoteToPowerNukkitConverter.convertItem(armor[0]) ?: air
            val legs = RemoteToPowerNukkitConverter.convertItem(armor[1]) ?: air
            val chest = RemoteToPowerNukkitConverter.convertItem(armor[2]) ?: air
            var head = RemoteToPowerNukkitConverter.convertItem(armor[3]) ?: air
            if (head.id == ItemID.BANNER) {
                nbt.putBoolean("IsIllagerCaptain", true)
                head = air
            }
            nbt.putList(ListTag<CompoundTag>("Armor")
                .add(NBTIO.putItemHelper(head))
                .add(NBTIO.putItemHelper(chest))
                .add(NBTIO.putItemHelper(legs))
                .add(NBTIO.putItemHelper(feet))
            )
        }

        if (entityNbt.containsList("HandItems", Tag.TAG_Compound)) {
            val hands = entityNbt.getList("HandItems", CompoundTag::class.java)
            if (hands.size() > 0) {
                val hand = RemoteToPowerNukkitConverter.convertItem(hands[0]) ?: air
                nbt.putList(ListTag<CompoundTag>("Mainhand").add(NBTIO.putItemHelper(hand)))
            }
            if (hands.size() > 1) {
                val hand = RemoteToPowerNukkitConverter.convertItem(hands[0]) ?: air
                nbt.putList(ListTag<CompoundTag>("Offhand").add(NBTIO.putItemHelper(hand)))
            }
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
        private val air get() = Item.getBlock(BlockID.AIR)
        val unknownEntities = mutableSetOf<String>()
    }
}
