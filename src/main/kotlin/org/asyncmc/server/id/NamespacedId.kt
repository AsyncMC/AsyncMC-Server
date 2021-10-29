package org.asyncmc.server.id

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class NamespacedId(public val fullId: String): Identifier {
    public constructor(namespace: String, id: String): this("$namespace:$id")
    public inline val namespace: String get() = fullId.substringBefore(':')
    public inline val id: String get() = fullId.substringAfter(':')

    init {
        val parts = fullId.split(':')
        require(parts.size == 2 && parts.all { it.matches(Identifier.VALID_PATTERN) }) {
            "Invalid namespaced id: $fullId"
        }
    }
}
