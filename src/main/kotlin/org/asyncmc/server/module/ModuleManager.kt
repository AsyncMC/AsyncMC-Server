package org.asyncmc.server.module

import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.CoroutineScope
import org.asyncmc.server.AsyncMc
import org.asyncmc.server.id.ModuleId
import org.asyncmc.server.module.ModuleDescriptor.*

public class ModuleManager(public val server: AsyncMc) {
    private val log = InlineLogger()
    public var pendingModules: Map<ModuleId, ModuleDescriptor<*>> = emptyMap(); private set
    public var loadedModules: Map<ModuleId, Module> = emptyMap(); private set
    public var enabledModules: Map<ModuleId, Module> = emptyMap(); private set

    internal fun CoroutineScope.startNewModules(server: AsyncMc, moduleDescriptors: Iterable<ModuleDescriptor<*>>): List<Module> {
        moduleDescriptors.forEach {
            addPendingModule(it)
        }
        val loaded = loadPendingModules(server)
        return enableModules(loaded)
    }

    internal fun CoroutineScope.disableAllModules(): List<Module> {
        return disableModules(enabledModules.values)
    }

    private fun addPendingModule(descriptor: ModuleDescriptor<*>) {
        check(descriptor.id !in enabledModules) {
            "The module ${descriptor.id} is already enabled"
        }
        check(descriptor.id !in loadedModules) {
            "The module ${descriptor.id} is already loaded"
        }
        check(descriptor.id !in pendingModules) {
            "The module ${descriptor.id} is already added to the list of pending modules"
        }
        pendingModules = pendingModules + (descriptor.id to descriptor)
    }

    private fun CoroutineScope.loadPendingModules(server: AsyncMc): List<Module> {
        val recentlyLoaded = ArrayList<Module>(pendingModules.size)
        while (pendingModules.isNotEmpty()) {
            val modulesToLoad = pendingModules.values.sortedWith(LoadOrderComparator).takeUnless { it.isEmpty() }
                ?: return emptyList()
            modulesToLoad.forEach { moduleDescriptor ->
                try {
                    val module = loadModule(server, moduleDescriptor)
                    recentlyLoaded += module
                } catch (e: Exception) {
                    log.trace(e) { "Could not load the module ${moduleDescriptor.id}" }
                } finally {
                    pendingModules = pendingModules - moduleDescriptor.id
                }
            }
        }
        return recentlyLoaded
    }

    private fun CoroutineScope.enableModules(modulesToEnabled: Iterable<Module>): List<Module> {
        return modulesToEnabled
            .sortedWith { a, b -> compareValuesBy(a, b, EnableOrderComparator, Module::descriptor) }
            .filter { module ->
                try {
                    enableModule(module)
                    true
                } catch (e: Exception) {
                    log.trace(e) { "Could not enable the ${module.descriptor.id}" }
                    false
                }
            }
    }

    private fun CoroutineScope.disableModules(modulesToDisable: Iterable<Module>): List<Module> {
        return modulesToDisable
            .sortedWith { a, b -> compareValuesBy(a, b, DisableOrderComparator, Module::descriptor) }
            .filter { module ->
                try {
                    disableModule(module)
                    true
                } catch (e: Exception) {
                    log.trace(e) { "Could not disable the module ${module.descriptor.id}" }
                    false
                }
            }
    }

    private fun CoroutineScope.loadModule(server: AsyncMc, descriptor: ModuleDescriptor<*>): Module {
        require(descriptor.id !in loadedModules) {
            "The module ${descriptor.id} is already loaded"
        }
        val missing = (descriptor.dependsOn - loadedModules.keys) - descriptor.loadAfter.filter { it in pendingModules }
        require(missing.isEmpty()) {
            "Cannot load the module ${descriptor.id} because the following dependencies are missing: $missing"
        }
        val module = descriptor.createInstance(server)
        try {
            with(module) {
                load()
            }
        } catch (e: Exception) {
            throw IllegalStateException("Could not load the module ${descriptor.id} because it threw an exception while loading", e)
        }
        loadedModules = loadedModules + (descriptor.id to module)
        return module
    }

    private fun CoroutineScope.enableModule(module: Module) {
        val descriptor = module.descriptor
        check(descriptor.id !in enabledModules) {
            "The module ${descriptor.id} is already enabled"
        }
        check(descriptor.id in loadedModules) {
            "The module ${descriptor.id} is not loaded"
        }
        val missing = (enabledModules.keys - descriptor.dependsOn) - descriptor.enableAfter.filter { it in loadedModules }
        require(missing.isEmpty()) {
            "Cannot enable the module ${descriptor.id} because the following dependencies are not loaded or enabled: $missing"
        }
        try {
            with(module) {
                enable()
            }
        } catch (e: Exception) {
            throw IllegalStateException("Could not enable the module ${descriptor.id} because it threw an exception while enabling", e)
        }
    }

    private fun CoroutineScope.disableModule(module: Module) {
        val descriptor = module.descriptor
        check(descriptor.id in enabledModules) {
            "The module ${descriptor.id} is not enabled"
        }
        try {
            with(module) {
                disable()
            }
        } catch (e: Exception) {
            throw IllegalStateException("Could not disable the module ${descriptor.id} because it threw an exception while disabling", e)
        }
    }
}
