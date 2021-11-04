@file:JvmName("NbtSerializer")
package org.asyncmc.worldgen.remote.server.paper

import org.asyncmc.worldgen.remote.data.SerializedNbtFile
import org.asyncmc.worldgen.remote.server.paper.access.nms.NMSNBTTagCompound
import org.asyncmc.worldgen.remote.server.paper.access.nms.NMSNbtIo
import java.io.ByteArrayOutputStream

fun NMSNBTTagCompound.toSerializedNbtFile(): SerializedNbtFile {
    val data = ByteArrayOutputStream().use { bos ->
        NMSNbtIo.writeCompressed(this, bos)
        bos.toByteArray()
    }
    return SerializedNbtFile(littleEndian = false, compressed = true, data = data)
}
