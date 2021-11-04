package org.asyncmc.module.remote_world_generator

import kotlinx.coroutines.CoroutineScope
import org.asyncmc.server.AsyncMc
import org.asyncmc.server.id.ModuleId
import org.asyncmc.server.module.Module
import org.asyncmc.server.module.ModuleDescriptor

public class RemoteWorldGeneratorModule(server: AsyncMc, descriptor: ModuleDescriptor<*>) : Module(server, descriptor) {
    public class Descriptor: ModuleDescriptor<RemoteWorldGeneratorModule>(
        id = ModuleId("asyncmc", "remote_world_generator")
    )

    override fun CoroutineScope.onEnable() {
        TODO("Not yet implemented")
    }
}
