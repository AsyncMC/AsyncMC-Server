plugins {
    kotlin("jvm")
}

repositories {
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
}

dependencies {
    compileOnly(files("libs/patched-paper-1.17.1-360.jar"))
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
}
