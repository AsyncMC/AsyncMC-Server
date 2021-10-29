package org.asyncmc.server.module

import com.github.michaelbull.logging.InlineLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.asyncmc.server.AsyncMc

public abstract class Module(
    public val server: AsyncMc,
    public val descriptor: ModuleDescriptor<*>,
) {
    protected val log: InlineLogger = InlineLogger(this::class)
    private val _status = MutableStateFlow(Status.CONSTRUCTING)
    public val status: StateFlow<Status> = _status.asStateFlow()
    private lateinit var job: Job

    protected open fun CoroutineScope.onLoad() {
        // Does nothing by default
    }

    protected open fun CoroutineScope.onEnable() {
        // Does nothing by default
    }

    protected open fun CoroutineScope.onDisable() {

    }

    private fun expectedStatusFailed(action: String, vararg expected: Status): String {
        return "Could not $action because the module status is ${_status.value}, but was expected to be at ${expected.joinToString(" or ")}"
    }

    internal fun postCreation() {
        check(_status.compareAndSet(Status.CONSTRUCTING, Status.CONSTRUCTED)) {
            expectedStatusFailed("construct", Status.CONSTRUCTING)
        }
    }

    internal fun CoroutineScope.load() {
        try {
            log.info { "Loading the module ${descriptor.id}" }
            check(_status.compareAndSet(Status.CONSTRUCTED, Status.LOADING)) {
                expectedStatusFailed("load", Status.CONSTRUCTED)
            }
            onLoad()
            log.debug { "The module ${descriptor.id} was loaded successfully" }
            check(_status.compareAndSet(Status.LOADING, Status.LOADED)) {
                expectedStatusFailed("load", Status.LOADING)
            }
        } catch (e: Throwable) {
            log.error(e) { "Failed to load the module ${descriptor.id}" }
            _status.update { current ->
                when {
                    current <= Status.LOAD_FAILED -> Status.LOAD_FAILED
                    else -> Status.UNKNOWN
                }
            }
            throw e
        }
    }

    internal fun CoroutineScope.enable() {
        try {
            log.info { "Enabling the module ${descriptor.id}" }
            check(_status.compareAndSet(Status.LOADED, Status.ENABLING)
                    || _status.compareAndSet(Status.DISABLED, Status.ENABLING)) {
                expectedStatusFailed("enable", Status.CONSTRUCTED, Status.DISABLED)
            }
            onEnable()
            log.debug { "The module ${descriptor.id} was enabled successfully" }
            check(_status.compareAndSet(Status.ENABLING, Status.ENABLED)) {
                expectedStatusFailed("enable", Status.ENABLING)
            }
        } catch (e: Throwable) {
            log.error(e) { "Failed to enable module ${descriptor.id}" }
            _status.update { current ->
                when {
                    current <= Status.ENABLE_FAILED -> Status.ENABLE_FAILED
                    else -> Status.UNKNOWN
                }
            }
        }
    }

    internal fun CoroutineScope.disable() {
        try {
            log.info { "Disabling the module ${descriptor.id}" }
            check(_status.compareAndSet(Status.ENABLED, Status.DISABLING)) {
                expectedStatusFailed("disable", Status.ENABLING)
            }
            onEnable()
            log.debug { "Module ${descriptor.id} disabled successfully" }
            check(_status.compareAndSet(Status.DISABLING, Status.DISABLED)) {
                expectedStatusFailed("disable", Status.DISABLING)
            }
        } catch (e: Throwable) {
            log.error(e) { "Failed to disable module ${descriptor.id}" }
            _status.update { current ->
                when {
                    current <= Status.DISABLE_FAILED -> Status.DISABLE_FAILED
                    else -> Status.UNKNOWN
                }
            }
        }
    }

    final override fun toString(): String = "Module[${descriptor.id}]"

    public enum class Status {
        UNKNOWN,
        CONSTRUCTING,
        CONSTRUCTED,
        LOADING,
        LOAD_FAILED,
        LOADED,
        ENABLING,
        ENABLE_FAILED,
        ENABLED,
        DISABLING,
        DISABLE_FAILED,
        DISABLED
    }
}
