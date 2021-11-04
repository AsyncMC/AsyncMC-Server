plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

apply<AsyncMcModule>()

dependencies {
    implementation(rootProject)
}

kotlin {
    explicitApi()
}
