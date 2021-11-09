package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.entity.Entity
import cn.nukkit.level.format.generic.BaseFullChunk
import org.asyncmc.worldgen.remote.data.RemoteEntity
import java.util.*

internal open class GenericEntityFactory(private val id: String?): EntityFactory() {
    constructor(id: Int): this(id.toString())

    override val nukkitId: OptionalInt
        get() = id?.let { Entity.getSaveId(it) } ?: OptionalInt.empty()

    override fun createEntity(remoteEntity: RemoteEntity, chunk: BaseFullChunk): Entity? {
        return createEntity(remoteEntity, id, chunk)
    }
}
