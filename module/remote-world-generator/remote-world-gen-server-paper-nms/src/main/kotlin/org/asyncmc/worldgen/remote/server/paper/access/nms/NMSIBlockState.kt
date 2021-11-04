package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.level.block.state.properties.IBlockState

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSIBlockState<T: Comparable<T>>(override val nms: IBlockState<T>): NMSWrapper<IBlockState<T>> {
    inline val name: String get() = nms.name
    inline fun name(value: T): String {
        return nms.a(value)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun uncheckedName(value: Any): String {
        val prop = this as NMSIBlockState<Comparable<Comparable<*>>>
        return prop.name(value as Comparable<Comparable<*>>)
    }
}
