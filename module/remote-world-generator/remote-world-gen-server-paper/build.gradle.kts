plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

repositories {
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
}

val ktorVersion = "1.6.4"
val logbackVersion = "1.2.3"
val kotlinxSerializationVersion = "1.3.0"

dependencies {
    implementation("org.asyncmc:remote-world-gen-data:0.1.0-SNAPSHOT")
    implementation(project(":module:remote-world-generator:remote-world-gen-server-paper-nms"))
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    runtimeOnly(files("../remote-world-gen-server-paper-nms/libs/patched-paper-1.17.1-360.jar"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.7.1")
}

tasks {
    build {
        finalizedBy(shadowJar)
    }
    shadowJar {
        minimize()
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
