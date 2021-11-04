import org.asyncmc.module.remote_world_generator.RemoteWorldGeneratorModule;
import org.asyncmc.server.module.ModuleDescriptor;

module org.asyncmc.module.remote_world_generator {
    requires org.asyncmc.server;

    provides ModuleDescriptor with RemoteWorldGeneratorModule.Descriptor;
}
