package org.asyncmc.worldgen.remote.server.paper

import kotlinx.coroutines.suspendCancellableCoroutine
import org.bukkit.plugin.Plugin
import java.util.concurrent.Callable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend inline fun <R> callSync(plugin: Plugin, callable: Callable<R>): R {
    return suspendCancellableCoroutine { continuation ->
        val task = plugin.server.scheduler.callSyncMethod(plugin, callable)
        continuation.invokeOnCancellation {
            task.cancel(true)
        }
        try {
            continuation.resume(task.get())
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}

