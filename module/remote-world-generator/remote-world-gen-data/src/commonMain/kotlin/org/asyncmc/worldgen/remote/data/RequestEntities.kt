package org.asyncmc.worldgen.remote.data

import kotlinx.serialization.Serializable

@Serializable
public data class RequestEntities (
    val monsters: Boolean = true,
    val animals: Boolean = true,
    val structureEntities: Boolean = true,
    val otherEntities: Boolean = true,
) {
    public val isNotFiltered: Boolean get() = this == withNoFilters
    public companion object {
        public val withNoFilters: RequestEntities = RequestEntities()
    }
}
