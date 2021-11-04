plugins {
    application
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.31"
}
apply<KotlinJavaModuleFixer>()
val kotlinVersion: String by project.extra

allprojects {
    group = "org.asyncmc"

    repositories {
        mavenCentral()
    }
}

version = "0.0.1-SNAPSHOT"
application {
    mainClass.set("org.asyncmc.server.AsyncMcLauncher")
    mainModule.set("org.asyncmc.server")
    executableDir = "run"
}

dependencies {
    api(kotlin("stdlib-jdk8", kotlinVersion))
    api("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    api("org.jetbrains:annotations:22.0.0")
    api("org.slf4j:slf4j-api:1.7.32")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    runtimeOnly("org.slf4j:slf4j-jdk14:1.7.32")
    testImplementation(kotlin("test", kotlinVersion))
}

kotlin {
    explicitApi()
}
