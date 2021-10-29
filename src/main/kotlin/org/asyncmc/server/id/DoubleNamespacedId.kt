package org.asyncmc.server.id

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class DoubleNamespacedId(@PublishedApi internal val fullId: String): Identifier {
    public constructor(root: String, namespace: String, id: String): this("$root:$namespace:$id")
    public constructor(namespace: NamespacedId, id: String): this("$namespace:$id")
    public inline val root: String get() = fullId.substringBefore(':')
    public inline val namespace: NamespacedId get() = NamespacedId(fullId.substringBeforeLast(':'))
    public inline val id: String get() = fullId.substringAfterLast(':')

    init {
        val parts = fullId.split(':')
        require(parts.size == 3 && parts.all { it.matches(Identifier.VALID_PATTERN) }) {
            "Invalid double namespaced id: $fullId"
        }
    }
}
