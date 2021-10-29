package org.asyncmc.server

import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.runBlocking
import org.asyncmc.server.module.ModuleDescriptor
import java.util.*

/**
 * Boot class to set up and launch the AsyncMC server.
 */
public object AsyncMcLauncher {
    private val log = InlineLogger()
    /**
     * Setup and launch the AsyncMC server with a console and a GUI depending on the arguments and file settings.
     */
    @JvmStatic
    public fun main(args: Array<String>): Unit = runBlocking {
        val server = AsyncMc()
        with(server.moduleManager){
            startNewModules(server, ServiceLoader.load(ModuleDescriptor::class.java))
        }
        server.run()
        log.info { "Rodou!" }
    }
}
