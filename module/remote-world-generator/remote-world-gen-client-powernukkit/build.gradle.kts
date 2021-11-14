plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("org.jetbrains.kotlin.plugin.serialization")
}

val ktorVersion = "1.6.4"

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

val included by configurations.creating
val worldGenDataVersion = "0.1.0-SNAPSHOT"

dependencies {
    implementation("org.powernukkit:powernukkit:1.5.2.0-PN-SNAPSHOT")
    implementation("org.powernukkit.plugins:kotlin-plugin-lib:1.5.31+0.1.0+2021.11.13-SNAPSHOT")
    implementation("org.asyncmc:remote-world-gen-data:$worldGenDataVersion")
    included("org.asyncmc:remote-world-gen-data:$worldGenDataVersion") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.jetbrains")
    }
    includedImplementation("org.rauschig:jarchivelib:0.7.1")
}

val webServerJar = buildDir.resolve("resources/main/META-INF/libs")
tasks {
    build {
        finalizedBy(shadowJar)
    }

    val copyBackendJar = create<Copy>("copyBackendJar") {
        project(":module:remote-world-generator:remote-world-gen-server-paper").afterEvaluate {
            val backendJar = tasks.getByName("shadowJar") as Jar
            dependsOn(backendJar)
            from(backendJar.archiveFile)
            into(webServerJar)
            rename(".+\\.jar", "AsyncMcPaperWorldGenServer.plugin")
        }
    }

    shadowJar {
        dependsOn(copyBackendJar)
        mustRunAfter(copyBackendJar)
        configurations = listOf(included)
        minimize()
        /*from(webServerJar) {
            include("*.plugin")
            into("META-INF/libs")
        }*/
    }

    processResources {
        eachFile {
            filter<org.apache.tools.ant.filters.ReplaceTokens>(mapOf(
                "tokens" to mapOf(
                    "version" to project.version
                )
            ))
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }
}

fun DependencyHandlerScope.includedImplementation(dependencyNotation: Any) {
    implementation(dependencyNotation)
    included(dependencyNotation)
}

///////////////////////////////////////////////////////////////////
// Some fancy functions to allow you to debug your plugin easily //
// Just run ./gradlew run -q to run PowerNukkit with your plugin //
// Or execute the "debug" task in debug mode with your IDE       //
///////////////////////////////////////////////////////////////////

tasks {
    register<JavaExec>("debug") {
        dependsOn("createDebugJar")
        group = "Execution"
        description = "Run PowerNukkit with your plugin in debug mode (without Watchdog Thread)"
        workingDir = file("run")
        systemProperties = mapOf("file.encoding" to "UTF-8", "disableWatchdog" to true)
        mainClass.set("cn.nukkit.Nukkit")
        standardInput = System.`in`
        classpath = sourceSets.main.get().runtimeClasspath
    }

    register<JavaExec>("run") {
        dependsOn("createDebugJar")
        group = "Execution"
        description = "Run PowerNukkit with your plugin"
        mainClass.set("cn.nukkit.Nukkit")
        workingDir = file("run")
        systemProperties = mapOf("file.encoding" to "UTF-8")
        standardInput = System.`in`
        classpath = sourceSets.main.get().runtimeClasspath
    }

    register<Jar>("createDebugJar") {
        dependsOn(classes, "copyBackendJar")
        mustRunAfter("copyBackendJar")
        group = "Execution"
        description = "Creates a fake jar to make PowerNukkit load your plugin directly from the compiled classes"

        from(sourceSets.main.get().output.resourcesDir!!) {
            include("plugin.yml")
            include("nukkit.yml")
        }

        destinationDirectory.set(file("run/plugins"))
        archiveBaseName.set("__plugin_loader")
        archiveExtension.set("jar")
        archiveAppendix.set("")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}
