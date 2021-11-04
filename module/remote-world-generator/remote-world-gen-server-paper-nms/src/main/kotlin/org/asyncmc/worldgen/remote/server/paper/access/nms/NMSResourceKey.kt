package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.resources.ResourceKey

data class NMSResourceKey<T, W: NMSWrapper<T>>(
    override val nms: ResourceKey<T>,
    val wrapperFactory: (T) -> W,
): NMSWrapper<ResourceKey<T>>
