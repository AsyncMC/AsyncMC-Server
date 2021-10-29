package org.asyncmc.server.math

import kotlinx.serialization.Serializable

@Serializable
public data class EntityPosAndLook(val pos: EntityPos, val look: PitchYaw) {
    public constructor(x: Float, y: Float, z: Float, pitch: Float, yaw: Float): this(EntityPos(x, y, z), PitchYaw(pitch, yaw))
    public constructor(x: Float, y: Float, z: Float, pitchYaw: PitchYaw): this(EntityPos(x, y, z), pitchYaw)
    public constructor(pos: EntityPos, pitch: Float, yaw: Float): this(pos, PitchYaw(pitch, yaw))

    public inline val x: Float get() = pos.x
    public inline val y: Float get() = pos.y
    public inline val z: Float get() = pos.z
    public inline val pitch: Float get() = look.pitch
    public inline val yaw: Float get() = look.yaw

    public fun toBlockPos(): BlockPos = BlockPos(x.toInt(), y.toInt(), z.toInt())
    public fun toChunkPos(): ChunkPos = ChunkPos(x.toInt() shr 4, z.toInt() shr 4)
}
