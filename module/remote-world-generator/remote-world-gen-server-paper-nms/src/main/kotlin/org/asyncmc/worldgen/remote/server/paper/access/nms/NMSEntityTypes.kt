package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes

@JvmInline
value class NMSEntityTypes<T: Entity>(override val nms: EntityTypes<T>): NMSWrapper<EntityTypes<T>>
