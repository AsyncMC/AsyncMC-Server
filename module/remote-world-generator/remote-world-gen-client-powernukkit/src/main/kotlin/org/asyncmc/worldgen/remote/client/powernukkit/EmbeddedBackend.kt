package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.utils.Config
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import org.intellij.lang.annotations.Language
import java.io.File

internal class EmbeddedBackend(private val plugin: AsyncMcWorldGenPowerNukkitClientPlugin) {
    private inline val log get() = plugin.log
    private val paperFolder get() = plugin.dataFolder.resolve("paper")
    private val pluginFolder get() = paperFolder.resolve("plugins")
    private val pluginDataFolder get() = pluginFolder.resolve("AsyncMcPaperWorldGenServer")
    private val jdkFolder get() = plugin.dataFolder.resolve("amazon-corretto-jdk-16")
    private val isWin get() = System.getProperty("os.name").lowercase().contains("windows")
    private val javaExe get() = jdkFolder.resolve("bin/java"+ (if (isWin) ".exe" else ""))

    private val paperJar get() = paperFolder.resolve("paper-custom.jar").takeIf { it.isFile }
        ?: paperFolder.resolve(PAPER_JAR)

    private val custom get() = paperJar.name == "paper-custom.jar"

    private var process: Process? = null

    private fun validatePaper(paperJar: File): Boolean {
        return paperJar.validadeMD5(PAPER_MD5)
    }

    private suspend fun downloadPaper(httpClient: HttpClient, paperJar: File) {
        log.info { "Downloading $PAPER_URL" }
        httpClient.get<HttpStatement>(PAPER_URL) {
            userAgent(USER_AGENT)
            header("Referer", PAPER_REFERER)
        }.download(paperJar)

        require(validatePaper(paperJar)) {
            "The download of the paper server jar has failed. The MD5 hash mismatch."
        }

        log.info { "Download of ParperMC completed" }
    }

    private suspend fun downloadJdk16(httpClient: HttpClient) {
        val jdkFolder = jdkFolder
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        val jdkFilename = when {
            "nux" in os -> when {
                "x64" in arch || "amd64" in arch -> "amazon-corretto-16-x64-linux-jdk.tar.gz"
                "arm64" in arch -> "amazon-corretto-16-aarch64-linux-jdk.tar.gz"
                else -> throw UnsupportedOperationException("Unsupported Linux arch $arch, please setup Java 16 manually")
            }
            "windows" in os -> when {
                "64" in arch -> "amazon-corretto-16-x64-windows-jdk.zip"
                else -> throw UnsupportedOperationException("Unsupported Windows arch $arch, please setup Java 16 manually")
            }
            "mac" in os || "darwin" in os -> when {
                "64" in arch -> "amazon-corretto-16-x64-macos-jdk.tar.gz"
                else -> throw UnsupportedOperationException("Unsupported MacOS arch $arch, please setup Java 16 manually")
            }
            else -> throw UnsupportedOperationException("Unsupported OS $os with arch $arch, please setup Java 16 manually")
        }
        val jdkUrl = "https://corretto.aws/downloads/latest/$jdkFilename"
        val jdkMd5Url = "https://corretto.aws/downloads/latest_checksum/$jdkFilename"
        val localJdkFile = jdkFolder.resolve("amazon-corretto-16.tar.gz")
        coroutineScope {
            val jdkDownload = launch(Dispatchers.IO) {
                plugin.log.info { "Downloading Amazon Corretto JDK 16 ($os $arch) from $jdkUrl into $localJdkFile" }
                httpClient.get<HttpStatement>(jdkUrl) {
                    userAgent(USER_AGENT)
                }.download(localJdkFile)
                plugin.log.info { "Download of Amazon Corretto JDK 16 completed." }
            }
            val md5Hash = httpClient.get<String>(jdkMd5Url) {
                userAgent(USER_AGENT)
            }
            jdkDownload.join()
            check(localJdkFile.validadeMD5(md5Hash)) {
                "Failed to download JDK 16, the MD5 hash mismatch!"
            }
        }
        localJdkFile.unTarGzSkippingFirstDir(jdkFolder)
    }

    private fun createStaticBackendFiles() {
        val paperFolder = paperFolder
        val pluginDataFolder = pluginDataFolder

        paperFolder.resolve("eula.txt").writeText("eula=true")
        paperFolder.resolve("server.properties").bufferedWriter().use { out ->
            out.write("server-ip=127.0.0.1"); out.newLine()
            out.write("server-port=0"); out.newLine()
            out.write("white-list=true"); out.newLine()
            out.write("difficulty=easy"); out.newLine()
            out.write("spawn-monsters=true"); out.newLine()
            out.write("spawn-npcs=true"); out.newLine()
            out.write("spawn-animals=true"); out.newLine()
            out.write("sync-chunk-writes=false"); out.newLine()
            out.write("enable-query=false"); out.newLine()
            out.write("enable-rcon=false"); out.newLine()
        }
        pluginDataFolder.resolve("config.yml").bufferedWriter().use { out ->
            @Language("yaml")
            val yaml = """
                webserver:
                  port: 0
                  secret-appid: generate
                  secret-token: generate
                  enable:
                    metrics: false
                    compression: false
                    CORS: true
                    call-logging: true
                  log:
                    success: false
                    locked: true # It's a normal phase of this webserver when a request is done but Minecraft didn't finish the chunk yet
                    others: true
                  CORS-hosts:
                    - "127.0.0.1"
                """.trimIndent()
            out.write(yaml)
        }
    }

    internal fun CoroutineScope.configureBackendFiles(): List<Job> {
        val pluginDataFolder = pluginDataFolder
        val jdkFolder = jdkFolder

        pluginDataFolder.mkdirs()
        jdkFolder.mkdirs()

        val httpClient by lazy {
            HttpClient(CIO)
        }

        val jobs = mutableListOf<Job>()
        if (!custom) {
            jobs += launch(Dispatchers.IO) {
                if (!validatePaper(paperJar)) {
                    downloadPaper(httpClient, paperJar)
                }
            }
        }

        if (!javaExe.isFile) {
            jobs += launch(Dispatchers.IO) {
                downloadJdk16(httpClient)
            }
        }

        jobs += launch(Dispatchers.IO) {
            plugin.useResource("META-INF/libs/AsyncMcPaperWorldGenServer.plugin") { input ->
                pluginFolder.resolve("AsyncMcPaperWorldGenServer.jar").outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        jobs += launch(Dispatchers.IO) {
            createStaticBackendFiles()
        }

        return jobs
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    internal suspend fun startBackend(): EmbeddedBackendData {
        val portsFile = pluginDataFolder.resolve(".currentPorts")
        portsFile.delete()

        val jdkFolder = jdkFolder
        val javaExec = javaExe
        val jvmArgs = if (plugin.config.isList("backend.embedded.jvm-args")) {
            plugin.config.getStringList("backend.embedded.jvm-args")
        } else {
            plugin.config.getString("backend.embedded.jvm-args").split(argsSplitter)
        }
        val programArgs = if (plugin.config.isList("backend.embedded.program-args")) {
            plugin.config.getStringList("backend.embedded.program-args")
        } else {
            plugin.config.getString("backend.embedded.program-args").split(argsSplitter)
        }

        val command = sequence {
            yield(javaExec.absolutePath)
            yieldAll(jvmArgs)
            yield("-jar")
            yield(paperJar.absolutePath)
            yieldAll(programArgs)
        }.toList()

        kill()
        val process = ProcessBuilder()
            .apply { environment()["JAVA_HOME"] = jdkFolder.absolutePath }
            .directory(paperFolder)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .command(command)
            .start()

        log.info {
            "Executing command with JAVA_HOME at $jdkFolder, and at the directory $paperFolder:\n${
                command.joinToString(
                    separator = "\", \"",
                    prefix = "\"",
                    postfix = "\""
                ) { it.replace("\\", "\\\\").replace("\"", "\\\"") }
            }"
        }

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                kill()
            }
        })

        this.process = process

        while (process.isAlive && !portsFile.isFile) {
            delay(50)
        }

        val port = portsFile.readLines().first().trim().toInt()
        val backendConfig = Config(pluginDataFolder.resolve("config.yml"), Config.YAML)
        return EmbeddedBackendData(
            port = port,
            secretAppId = backendConfig.getString("webserver.secret-security-appid"),
            secretToken = backendConfig.getString("webserver.secret-security-token"),
        )
    }

    fun stop() {
        process?.let { process ->
            if (process.isAlive) {
                log.info { "Sending SIGTERM to the backend" }
                process.destroy()
            }
        }
    }

    fun join() {
        process?.onExit()?.join()
    }

    fun kill() {
        process?.let { process ->
            if (process.isAlive) {
                log.warn { "Sending SIGKILL to the process" }
                process.destroyForcibly()
            }
        }
    }

    internal data class EmbeddedBackendData(
        val port: Int,
        val secretAppId: String,
        val secretToken: String,
    )

    private companion object {
        private const val PAPER_MD5 = "5ee485a2396e322608cf392be066742d"
        private const val PAPER_URL = "https://papermc.io/api/v2/projects/paper/versions/1.17.1/builds/381/downloads/paper-1.17.1-381.jar"
        private const val PAPER_JAR = "paper-1.17.1-381.jar"
        private const val PAPER_REFERER = "https://papermc.io/downloads"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36"
        private val argsSplitter = Regex("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")
    }
}
