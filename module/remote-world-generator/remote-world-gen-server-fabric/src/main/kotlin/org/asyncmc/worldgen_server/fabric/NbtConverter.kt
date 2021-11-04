@file:JvmName("NbtSerializer")
package org.asyncmc.worldgen_server.fabric

import net.minecraft.nbt.NbtIo
import org.asyncmc.worldgen_server.data.SerializedNbtFile
import java.io.ByteArrayOutputStream
import net.minecraft.nbt.NbtCompound as MinecraftNbtCompound

fun MinecraftNbtCompound.toSerializedNbtFile(): SerializedNbtFile {
    val data = ByteArrayOutputStream().use { bos ->
        NbtIo.writeCompressed(this, bos)
        bos.toByteArray()
    }
    return SerializedNbtFile(littleEndian = false, compressed = true, data = data)
}
