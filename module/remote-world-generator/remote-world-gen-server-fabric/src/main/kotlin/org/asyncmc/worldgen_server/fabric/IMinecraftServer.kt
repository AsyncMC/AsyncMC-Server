package org.asyncmc.worldgen_server.fabric

import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelStorage
import java.util.concurrent.Executor

interface IMinecraftServer {
    val theWorkerExecutor: Executor
    val theSession: LevelStorage.Session
    val theServer: MinecraftServer
}
