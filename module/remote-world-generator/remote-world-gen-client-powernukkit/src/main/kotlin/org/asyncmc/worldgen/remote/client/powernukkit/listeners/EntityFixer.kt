package org.asyncmc.worldgen.remote.client.powernukkit.listeners

import cn.nukkit.entity.Entity
import cn.nukkit.entity.data.ByteEntityData
import cn.nukkit.entity.data.IntEntityData
import cn.nukkit.entity.data.IntPositionEntityData
import cn.nukkit.entity.mob.EntityShulker
import cn.nukkit.entity.passive.EntityStrider
import cn.nukkit.event.EventHandler
import cn.nukkit.event.EventPriority
import cn.nukkit.event.Listener
import cn.nukkit.event.entity.EntitySpawnEvent
import cn.nukkit.math.BlockFace
import cn.nukkit.nbt.tag.IntTag
import cn.nukkit.nbt.tag.ListTag
import cn.nukkit.nbt.tag.Tag

class EntityFixer: Listener {
    /*@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    internal fun onPacket(ev: DataPacketSendEvent) {
        val dataPacket = ev.packet
        if (dataPacket is BlockEntityDataPacket) {
            val nbt = NBTIO.read(dataPacket.namedTag, ByteOrder.LITTLE_ENDIAN, true)
            if (nbt.getString("id") == BlockEntity.MOB_SPAWNER
                && nbt.containsInt("EntityId")
                && !nbt.containsString("EntityIdentifier")
            ) {
                val id = RemoteToPowerNukkitConverter.getEntityNamespacedId(nbt.getInt("EntityId")) ?: return
                nbt.putString("EntityIdentifier", id)
                dataPacket.namedTag = NBTIO.write(nbt, ByteOrder.LITTLE_ENDIAN, true)
                if (dataPacket.isEncoded) {
                    dataPacket.isEncoded = false
                    dataPacket.tryEncode()
                }
            }
        }
    }*/

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    internal fun onEntitySpawn(ev: EntitySpawnEvent) {
        when (val entity = ev.entity) {
            is EntityStrider -> {
                entity.fireProof = true
                entity.extinguish()
            }
            is EntityShulker -> {
                if (!entity.namedTag.containsByte("Color")) {
                    entity.namedTag.putByte("Color", 16)
                }

                if (!entity.namedTag.containsList("AttachPos", Tag.TAG_Int)) {
                    for (face in BlockFace.values()) {
                        val block = entity.level.getBlock(entity.getSide(face))
                        if (block.isSideFull(face.opposite)) {
                            entity.namedTag.putList(ListTag<IntTag>("AttachPos")
                                .add(IntTag("", block.x.toInt()))
                                .add(IntTag("", block.y.toInt()))
                                .add(IntTag("", block.z.toInt()))
                            )
                            entity.namedTag.putByte("AttachFace", face.index)
                        }
                    }
                }

                entity.setDataProperty(IntEntityData(Entity.DATA_VARIANT, entity.namedTag.getByte("Color")))
                entity.setDataProperty(ByteEntityData(Entity.DATA_SHULKER_ATTACH_FACE, entity.namedTag.getByte("AttachFace")))
                if (entity.namedTag.containsList("AttachPos", Tag.TAG_Int)) {
                    val pos = entity.namedTag.getList("AttachPos", IntTag::class.java)
                    if (pos.size() == 3) {
                        entity.setDataProperty(IntPositionEntityData(
                            Entity.DATA_SHULKER_ATTACH_POS,
                            pos[0].data,
                            pos[1].data,
                            pos[2].data
                        ))
                    }
                }
            }
        }
    }
}
