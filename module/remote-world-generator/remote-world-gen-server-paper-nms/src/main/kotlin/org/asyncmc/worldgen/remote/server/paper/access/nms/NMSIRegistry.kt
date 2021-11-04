package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.core.IRegistry

@Suppress("NOTHING_TO_INLINE")
data class NMSIRegistry<N, E: NMSWrapper<out N>>(
    override val nms: IRegistry<N>,
    val wrapperFactory: (N) -> E,
): NMSWrapper<IRegistry<N>> {
    inline fun getId(type: N): NMSMinecraftKey? {
        return nms.getKey(type)?.let { NMSMinecraftKey(it) }
    }

    inline fun getId(typeWrapper: E): NMSMinecraftKey? {
        return getId(typeWrapper.nms)
    }

    inline operator fun get(id: Int): E? {
        return nms.fromId(id)?.let(wrapperFactory)
    }
}
