package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.levelgen.HeightMap

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSHeightMapType(override val nms: HeightMap.Type): NMSWrapper<HeightMap.Type> {
    companion object {
        inline val WORLD_SURFACE_WG get() = this["WORLD_SURFACE_WG"]
        inline val WORLD_SURFACE get() = this["WORLD_SURFACE"]
        inline val OCEAN_FLOOR_WG get() = this["OCEAN_FLOOR_WG"]
        inline val OCEAN_FLOOR get() = this["OCEAN_FLOOR"]
        inline val MOTION_BLOCKING get() = this["MOTION_BLOCKING"]
        inline val MOTION_BLOCKING_NO_LEAVES get() = this["MOTION_BLOCKING_NO_LEAVES"]

        @PublishedApi
        internal inline operator fun get(name: String) = NMSHeightMapType(checkNotNull(HeightMap.Type.a(name)))
    }
}
