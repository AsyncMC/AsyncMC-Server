package org.asyncmc.server.module

import org.asyncmc.server.AsyncMc
import org.asyncmc.server.id.ModuleId
import org.asyncmc.server.util.HumanStringComparator
import java.lang.reflect.Constructor
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


/**
 *
 */
public abstract class ModuleDescriptor<E: Module> (
    moduleId: String,
    public val dependsOn: Set<ModuleId> = emptySet(),
    softDependsOn: Set<ModuleId> = emptySet(),
    loadAfter: Set<ModuleId> = emptySet(),
    public val loadBefore: Set<ModuleId> = emptySet(),
    enableAfter: Set<ModuleId> = emptySet(),
    public val enableBefore: Set<ModuleId> = emptySet(),
    public val disableAfter: Set<ModuleId> = emptySet(),
    disableBefore: Set<ModuleId> = emptySet(),
) {
    public val id: ModuleId = ModuleId(moduleId)

    private val moduleClass: Class<E> = javaClass.genericSuperclass.let { genericSuperclass ->
        var type: Type = genericSuperclass

        while (type !is ParameterizedType || type.rawType !== Module::class.java) {
            type = if (type is ParameterizedType) {
                (type.rawType as Class<*>).genericSuperclass
            } else {
                (type as Class<*>).genericSuperclass
            }
        }

        @Suppress("UNCHECKED_CAST")
        type.actualTypeArguments[0] as Class<E>
    }

    private val moduleConstructor: Constructor<E> = try {
        moduleClass.getConstructor(AsyncMc::class.java, ModuleDescriptor::class.java)
    } catch (e: NoSuchMethodException) {
        throw IllegalArgumentException(
            "The module $id don't have a `public constructor(server: AsyncMc, descriptor: ModuleDescriptor<*>)`",
            e
        )
    }

    public val loadAfter: Set<ModuleId> = loadAfter + (dependsOn + softDependsOn - loadBefore)
    public val enableAfter: Set<ModuleId> = enableAfter + (dependsOn + softDependsOn - enableBefore)
    public val disableBefore: Set<ModuleId> = disableBefore + (dependsOn + softDependsOn - disableAfter)

    init {
        require(moduleClass != Module::class.java) { "Invalid module type" }
        if (this.id.organization == "asyncmc") {
            require(moduleClass.packageName.startsWith("org.asyncmc.")) {
                "Only AsyncMC can use the \"asyncmc\" namespace. Attempted to use $id"
            }
        }
        require(moduleClass.constructors.any { it.parameters.asList() == listOf(AsyncMc::class.java, ModuleDescriptor::class.java) }) {
            "The module $id don't have a `public constructor(server: AsyncMc, descriptor: ModuleDescriptor<*>)`"
        }
        require(id !in this.loadBefore && id !in this.loadBefore
                && id !in this.enableBefore && id !in this.enableAfter
                && id !in this.disableBefore && id !in this.disableAfter
                && id !in this.dependsOn && id !in softDependsOn
        ) {
            "The module $id tried to declare a dependency on itself."
        }
        (this.loadBefore.firstOrNull { it in this.loadAfter }
            ?: this.loadAfter.firstOrNull { it in this.loadBefore }
                )?.let { throw IllegalArgumentException("Cannot load $it before and after $id at the same time") }
        (this.enableBefore.firstOrNull { it in this.enableAfter }
            ?: this.enableAfter.firstOrNull { it in this.enableBefore }
                )?.let { throw IllegalArgumentException("Cannot enable $it before and after $id at the same time") }
        (this.disableBefore.firstOrNull { it in this.disableAfter }
            ?: this.disableAfter.firstOrNull { it in this.disableBefore }
                )?.let { throw IllegalArgumentException("Cannot disable $it before and after $id at the same time") }
    }

    internal fun createInstance(server: AsyncMc): E {
        val instance = moduleConstructor.newInstance(server, this)
        instance.postCreation()
        return instance
    }

    public abstract class AbstractOrderComparator(
        private val orderName: String,
        private val nullFirst: Boolean = true,
    ): Comparator<ModuleDescriptor<*>> {
        protected abstract val ModuleDescriptor<*>.before: Set<ModuleId>
        protected abstract val ModuleDescriptor<*>.after: Set<ModuleId>

        private fun incompatibleCrossReference(o1: ModuleDescriptor<*>, o2: ModuleDescriptor<*>): Nothing {
            throw IllegalStateException(
                "Cannot load ${o1.id} and ${o2.id} because they have an incompatible cross-reference in $orderName order"
            )
        }

        final override fun compare(o1: ModuleDescriptor<*>?, o2: ModuleDescriptor<*>?): Int {
            return when {
                o1 == null && o2 == null -> 0
                o1 == null -> if (nullFirst) -1 else 1
                o2 == null -> if (nullFirst) 1 else -1
                o2.id in o1.before -> {
                    if (o1.id in o2.before) incompatibleCrossReference(o1, o2)
                    -1
                }
                o2.id in o1.after -> {
                    if(o1.id in o2.after) incompatibleCrossReference(o1, o2)
                    1
                }
                o1.id in o2.before -> {
                    if (o2.id in o1.before) incompatibleCrossReference(o1, o2)
                    -1
                }
                o1.id in o2.after -> {
                    if (o2.id in o1.after) incompatibleCrossReference(o1, o2)
                    1
                }
                else -> HumanStringComparator.getInstance().compare(o1.id.toString(), o2.id.toString())
            }
        }
    }

    public object LoadOrderComparator: AbstractOrderComparator("load") {
        override val ModuleDescriptor<*>.before: Set<ModuleId>
            get() = loadBefore

        override val ModuleDescriptor<*>.after: Set<ModuleId>
            get() = loadAfter
    }

    public object EnableOrderComparator: AbstractOrderComparator("enable") {
        override val ModuleDescriptor<*>.before: Set<ModuleId>
            get() = enableBefore
        override val ModuleDescriptor<*>.after: Set<ModuleId>
            get() = enableAfter
    }

    public object DisableOrderComparator: AbstractOrderComparator("disable") {
        override val ModuleDescriptor<*>.before: Set<ModuleId>
            get() = disableBefore
        override val ModuleDescriptor<*>.after: Set<ModuleId>
            get() = disableAfter
    }
}
