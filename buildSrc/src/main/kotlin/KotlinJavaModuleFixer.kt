
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class KotlinJavaModuleFixer: Plugin<Project> {
    override fun apply(project: Project) {
        project.setup()
    }

    private fun Project.setup() {
        this.extra["kotlinVersion"] = "1.5.31"
        repositories {
            mavenCentral()
        }

        the<JavaPluginExtension>().apply {
            sourceCompatibility = JavaVersion.VERSION_16
            targetCompatibility = JavaVersion.VERSION_16
        }

        the<SourceSetContainer>().apply {
            getByName("main").apply {
                project.the<KotlinJvmProjectExtension>().apply {
                    java {
                        srcDirs(*sourceSets.getByName("main").kotlin.srcDirs.toTypedArray())
                    }
                }
            }
        }

        tasks.apply {
            val rebuild = create("rebuild") {
                dependsOn("clean")
                inputs.file(projectDir.resolve("src/main/kotlin/module-info.java"))
            }

            getByName("run") {
                this as JavaExec
                dependsOn(rebuild, "jar")
                doFirst {
                    jvmArgs = listOf(
                        "--module-path", classpath.asPath
                    )
                    classpath = files()
                }
            }

            val compileJava: JavaCompile = getByName("compileJava") {
                this as JavaCompile
                dependsOn("compileKotlin")
                mustRunAfter(rebuild)
                doFirst {
                    options.compilerArgs = listOf(
                        "--module-path", classpath.asPath
                    )
                }
            } as JavaCompile

            getByName("compileKotlin") {
                this as KotlinCompile
                mustRunAfter(rebuild)
                destinationDirectory.set(compileJava.destinationDirectory)
            }

            getByName("jar") {
                this as Jar
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }

            withType<KotlinCompile>().configureEach {
                kotlinOptions {
                    jvmTarget = "16"
                    freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
                }
            }
        }
    }
}
