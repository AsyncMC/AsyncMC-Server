package org.asyncmc.server

import kotlinx.coroutines.coroutineScope
import org.asyncmc.server.module.ModuleManager
import org.asyncmc.server.world.WorldManager

public class AsyncMc {
    public val moduleManager: ModuleManager = ModuleManager(this)
    public val worldManager: WorldManager = WorldManager(this)
    internal suspend fun run(): Unit = coroutineScope {

    }
}
