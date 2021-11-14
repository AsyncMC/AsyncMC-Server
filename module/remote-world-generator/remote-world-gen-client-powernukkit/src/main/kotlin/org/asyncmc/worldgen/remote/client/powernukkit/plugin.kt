package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.Server
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import kotlin.io.use

internal val plugin by lazy {
    Server.getInstance().pluginManager.getPlugin("AsyncMcRemoteWorldGenClient") as AsyncMcWorldGenPowerNukkitClientPlugin
}

internal fun File.md5(): String {
    plugin.log.info { "Calculating MD5 hash of $this" }
    val digest = MessageDigest.getInstance("MD5")
    return inputStream().use { fis ->
        val bytes = ByteArray(8 * 1024)
        var read = 0
        while (read != -1) {
            digest.update(bytes, 0, read)
            read = fis.read(bytes)
        }
        digest.digest().joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    }.also {
        plugin.log.info { "MD5 of $this: $it" }
    }
}

internal fun File.validadeMD5(expected: String): Boolean {
    if (!isFile) {
        return false
    }
    val md5 = md5()
    return md5.equals(expected, ignoreCase = true)
}

internal fun File.unTarGzSkippingFirstDir(targetDir: File) {
    //ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
    //    .extract(this, targetDir)
    plugin.log.info { "Decompressing $this into $targetDir" }
    inputStream().buffered().use { rawInput ->
        GZIPInputStream(rawInput).use { gzipInput ->
            TarArchiveInputStream(gzipInput).use { input ->
                var entry: TarArchiveEntry? = input.nextTarEntry
                while (entry != null) {
                    val entryName = entry.name.let { if (it.startsWith('/')) it.substring(1) else it }.substringAfter('/')
                    val targetFile = targetDir.resolve(entryName)
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                        if ((entry.mode and 0b001_001_001) != 0) {
                            Files.setPosixFilePermissions(targetFile.toPath(), setOf(
                                PosixFilePermission.OWNER_EXECUTE,
                                PosixFilePermission.GROUP_EXECUTE,
                                PosixFilePermission.OTHERS_EXECUTE,
                            ))
                        }
                    }
                    entry = input.nextTarEntry
                }
            }
        }
    }
    plugin.log.info { "Decompression of $this completed" }
}

internal fun HttpStatement.asByteArrayFlow(): Flow<ByteArray> = flow {
    execute { httpResponse ->
        val channel: ByteReadChannel = httpResponse.receive()
        while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.isEmpty) {
                val bytes = packet.readBytes()
                emit(bytes)
            }
        }
    }
}

internal suspend fun HttpStatement.download(outputFile: File) {
    outputFile.outputStream().use { out ->
        this.asByteArrayFlow().collect {
            @Suppress("BlockingMethodInNonBlockingContext")
            out.write(it)
        }
    }
}
