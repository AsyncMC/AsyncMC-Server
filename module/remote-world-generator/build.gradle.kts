plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "org.asyncmc.module"

dependencies {
    implementation(rootProject)
}
