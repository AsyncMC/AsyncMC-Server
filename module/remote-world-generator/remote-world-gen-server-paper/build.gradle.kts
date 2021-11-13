plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

repositories {
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
}

val included by configurations.creating

val ktorVersion = "1.6.4"
val logbackVersion = "1.2.3"
val kotlinxSerializationVersion = "1.3.0"
val worldGenDataVersion = "0.1.0-SNAPSHOT"

dependencies {
    includedImplementation("org.asyncmc:remote-world-gen-data:$worldGenDataVersion")
    includedImplementation(project(":module:remote-world-generator:remote-world-gen-server-paper-nms"))
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    runtimeOnly(files("../remote-world-gen-server-paper-nms/libs/patched-paper-1.17.1-360.jar"))

    includedImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    includedImplementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
    includedImplementation("io.ktor:ktor-server-core:$ktorVersion")
    includedImplementation("io.ktor:ktor-serialization:$ktorVersion")
    includedImplementation("io.ktor:ktor-auth:$ktorVersion")
    includedImplementation("io.ktor:ktor-server-cio:$ktorVersion")
    //includedImplementation("ch.qos.logback:logback-classic:$logbackVersion")
    includedImplementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    includedImplementation("io.micrometer:micrometer-registry-prometheus:1.7.1")
}

tasks {
    build {
        finalizedBy(shadowJar)
    }
    shadowJar {
        configurations = listOf(included)
        minimize()
        dependencies {
            exclude(dependency("org.slf4j:slf4j-api"))
            exclude(dependency("org.fusesource.jansi:jansi"))
        }
    }
    create<Copy>("devPlugin") {
        dependsOn(shadowJar)
        from(shadowJar.get().archiveFile) {
            rename(".*.jar", "plugins/dev-plugin.jar")
        }
        from("../remote-world-gen-server-paper-nms/libs/patched-paper-1.17.1-360.jar") {
            rename(".*.jar", "full-paper-1.17.1.jar")
        }
        into(file("run").also { it.mkdirs() })
    }
}

fun DependencyHandlerScope.includedImplementation(dependencyNotation: Any) {
    implementation(dependencyNotation)
    included(dependencyNotation)
}

fun DependencyHandlerScope.includedImplementation(
    dependencyNotation: String?,
    dependencyConfiguration: ExternalModuleDependency.()->Unit
) {
    requireNotNull(dependencyNotation)
    included(dependencyNotation, dependencyConfiguration)
    implementation(dependencyNotation, dependencyConfiguration)
}
