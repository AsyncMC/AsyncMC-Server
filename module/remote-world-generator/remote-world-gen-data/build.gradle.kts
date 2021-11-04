plugins {
    kotlin("multiplatform") version "1.5.31"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.31"
    java
}

group = "org.asyncmc"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

kotlin {
    explicitApi()
}

java {
    targetCompatibility = JavaVersion.VERSION_16
    sourceCompatibility = JavaVersion.VERSION_16
}

kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */
    jvm {
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains:annotations:22.0.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api("br.com.gamemods:nbt-manipulator:3.1.0")
            }
        }
    }
}
tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
            jvmTarget = "16"
        }
    }
}
