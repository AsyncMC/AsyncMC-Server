package org.asyncmc.server.id

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class ModuleId(public val fullModuleId: NamespacedId): Identifier {
    public inline val organization: String get() = fullModuleId.namespace
    public inline val id: String get() = fullModuleId.id
    public companion object {
        @JvmName("getInstance")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun invoke(moduleId: String): ModuleId = ModuleId(NamespacedId(moduleId))
    }
}
