package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.block.BlockID
import cn.nukkit.block.BlockItemFrame.HAS_MAP
import cn.nukkit.block.BlockItemFrame.HAS_PHOTO
import cn.nukkit.blockproperty.CommonBlockProperties.FACING_DIRECTION
import cn.nukkit.blockstate.BlockState
import cn.nukkit.level.format.generic.BaseFullChunk
import cn.nukkit.math.BlockFace
import org.asyncmc.worldgen.remote.client.powernukkit.RemoteToPowerNukkitConverter
import org.asyncmc.worldgen.remote.client.powernukkit.deserializeForNukkit
import org.asyncmc.worldgen.remote.client.powernukkit.plugin
import org.asyncmc.worldgen.remote.data.RemoteChunk
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal class ItemFrameEntityConverter: EntityFactory() {
    private val water = BlockState.of(BlockID.WATER)
    private val torch = BlockState.of(BlockID.TORCH)

    override fun createEntity(remoteChunk: RemoteChunk, remoteEntity: RemoteEntity, chunk: BaseFullChunk) {
        val nbt = remoteEntity.nbt.deserializeForNukkit()
        val bx = nbt.getInt("TileX")
        val by = nbt.getInt("TileY")
        val bz = nbt.getInt("TileZ")
        val support = when (val b = nbt.getByte("Facing")) {
            0 -> BlockFace.DOWN
            1 -> BlockFace.UP
            2 -> BlockFace.NORTH
            3 -> BlockFace.SOUTH
            4 -> BlockFace.WEST
            5 -> BlockFace.EAST
            else -> {
                plugin.log.warn { "Unexpected item frame block face: $b" }
                BlockFace.NORTH
            }
        }

        val cx = bx - (chunk.x shl 4)
        val cz = bz - (chunk.z shl 4)
        val cy = by
        if (cx !in 0..15 || cy !in 0..255 || cz !in 0..15) {
            return
        }
        val currentMain = chunk.getBlockStateAt(cx, cy, cz, 0)
        if (currentMain == torch) {
            return
        }
        val itemId = nbt.getCompound("Item").getString("id")
        val state = BlockState.of(BlockID.ITEM_FRAME_BLOCK)
            .withProperty(FACING_DIRECTION, support)
            .withProperty(HAS_MAP, RemoteToPowerNukkitConverter.isMap(itemId))
            .withProperty(HAS_PHOTO, RemoteToPowerNukkitConverter.isPhoto(itemId))

        if (currentMain == water) {
            chunk.setBlockStateAt(cx, cy, cz, 1, water)
        }
        chunk.setBlockStateAt(cx, cy, cz, 0, state)
    }
}
