package org.asyncmc.server.id

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class WorldGeneratorId(public val fullGeneratorId: DoubleNamespacedId): Identifier {
    public inline val moduleId: ModuleId get() = ModuleId(fullGeneratorId.namespace)
    public inline val id: String get() = fullGeneratorId.id
    public companion object {
        @JvmName("getInstance")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun invoke(moduleId: String): WorldGeneratorId = WorldGeneratorId(DoubleNamespacedId(moduleId))
    }
}
