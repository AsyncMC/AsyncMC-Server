import org.asyncmc.server.module.ModuleDescriptor;

module org.asyncmc.server {
    uses ModuleDescriptor;

    requires kotlin.stdlib;
    requires kotlinx.coroutines.core.jvm;
    requires org.jetbrains.annotations;
    requires kotlinx.serialization.json;
    requires kotlin.inline.logger.jvm;

    exports org.asyncmc.server;
    exports org.asyncmc.server.id;
    exports org.asyncmc.server.module;
}
