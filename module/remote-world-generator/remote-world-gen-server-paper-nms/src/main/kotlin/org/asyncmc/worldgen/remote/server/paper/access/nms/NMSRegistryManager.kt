package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.core.IRegistry
import net.minecraft.core.IRegistryCustom

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSRegistryManager(override val nms: IRegistryCustom.Dimension): NMSWrapper<IRegistryCustom.Dimension> {
    inline operator fun <E, W: NMSWrapper<E>> get(key: NMSResourceKey<IRegistry<E>, NMSIRegistry<E, W>>): NMSIRegistry<E, W> {
        return key.wrapperFactory(nms.d(key.nms))
    }
}
