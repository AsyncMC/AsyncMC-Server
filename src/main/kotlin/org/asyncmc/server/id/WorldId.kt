package org.asyncmc.server.id

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class WorldId(public val fullWorldId: DoubleNamespacedId): Identifier {
    public inline val moduleId: ModuleId get() = ModuleId(fullWorldId.namespace)
    public inline val id: String get() = fullWorldId.id
    public companion object {
        @JvmName("getInstance")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun invoke(moduleId: String): WorldId = WorldId(DoubleNamespacedId(moduleId))
    }
}
