package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.entity.Entity
import cn.nukkit.level.format.generic.BaseFullChunk
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal open class GenericEntityFactory(private val id: String?): EntityFactory() {
    override fun createEntity(remoteEntity: RemoteEntity, chunk: BaseFullChunk): Entity? {
        return createEntity(remoteEntity, id, chunk)
    }
}
