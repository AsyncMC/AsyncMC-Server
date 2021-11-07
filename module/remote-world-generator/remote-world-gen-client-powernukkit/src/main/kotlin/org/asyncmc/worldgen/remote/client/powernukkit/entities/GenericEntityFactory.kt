package org.asyncmc.worldgen.remote.client.powernukkit.entities

import cn.nukkit.level.format.generic.BaseFullChunk
import org.asyncmc.worldgen.remote.data.RemoteChunk
import org.asyncmc.worldgen.remote.data.RemoteEntity

internal open class GenericEntityFactory(private val id: String?): EntityFactory() {
    override fun createEntity(remoteChunk: RemoteChunk, remoteEntity: RemoteEntity, chunk: BaseFullChunk) {
        createEntity(remoteChunk, remoteEntity, id, chunk)
    }
}
