import org.asyncmc.server.module.ModuleDescriptor;

@SuppressWarnings("JavaRequiresAutoModule")
module org.asyncmc.server {
    uses ModuleDescriptor;

    requires transitive kotlin.stdlib;
    requires transitive kotlinx.serialization.json;
    requires transitive org.jetbrains.annotations;

    requires transitive kotlinx.coroutines.core.jvm; // Automatic
    requires transitive kotlin.inline.logger.jvm; // Automatic

    exports org.asyncmc.server;
    exports org.asyncmc.server.id;
    exports org.asyncmc.server.module;
}
