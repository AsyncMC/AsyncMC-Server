package org.asyncmc.worldgen_server.fabric.mixins

import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelStorage
import org.asyncmc.worldgen_server.fabric.AsyncMcFabricWorldGenServer
import org.asyncmc.worldgen_server.fabric.IMinecraftServer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.concurrent.Executor

@Mixin(MinecraftServer::class)
abstract class MinecraftServerMixIn: IMinecraftServer {
    @field:Shadow
    private val workerExecutor: Executor? = null

    @field:Shadow
    private val session: LevelStorage.Session? = null

    override val theWorkerExecutor: Executor
        get() = checkNotNull(workerExecutor)

    override val theSession: LevelStorage.Session
        get() = checkNotNull(session)

    @Suppress("CAST_NEVER_SUCCEEDS")
    override val theServer: MinecraftServer
        get() = this as MinecraftServer

    @Inject(method = ["prepareStartRegion"], at = [At("HEAD")])
    fun prepareStartRegion(callbackInfo: CallbackInfo) {
        AsyncMcFabricWorldGenServer.startService(this)
    }

    @Inject(method = ["stop"], at = [At("HEAD")])
    fun stop(callbackInfo: CallbackInfo) {
        try {
            AsyncMcFabricWorldGenServer.stopService()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
