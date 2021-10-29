import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.31"
}
val kotlinVersion = "1.5.31"


allprojects {
    ext["kotlinVersion"] = kotlinVersion
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

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin")
        }
    }
}

tasks {
    val rebuild = create("rebuild") {
        dependsOn(clean)
        inputs.file(projectDir.resolve("src/main/kotlin/module-info.java"))
    }

    run.configure {
        dependsOn(rebuild, jar)
        doFirst {
            jvmArgs = listOf(
                "--module-path", classpath.asPath
            )
            classpath = files()
        }
    }

    compileJava {
        dependsOn(compileKotlin)
        mustRunAfter(rebuild)
        doFirst {
            options.compilerArgs = listOf(
                "--module-path", classpath.asPath
            )
        }
    }

    compileKotlin {
        mustRunAfter(rebuild)
        destinationDirectory.set(compileJava.get().destinationDirectory)
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "16"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}
