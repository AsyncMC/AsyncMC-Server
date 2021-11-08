package org.asyncmc.worldgen.remote.client.powernukkit

import cn.nukkit.nbt.NBTIO
import cn.nukkit.nbt.tag.CompoundTag
import org.asyncmc.worldgen.remote.data.SerializedNbtFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder

fun SerializedNbtFile.deserializeForNukkit(): CompoundTag {
    val byteOrder =  if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
    return if (compressed) {
        NBTIO.readNetworkCompressed(ByteArrayInputStream(data), byteOrder)
    } else {
        NBTIO.read(ByteArrayInputStream(data), byteOrder)
    }
}

fun CompoundTag.serialize(): SerializedNbtFile {
    return SerializedNbtFile(
        littleEndian = false,
        compressed = true,
        data = ByteArrayOutputStream().also { bos ->
            NBTIO.writeNetworkGZIPCompressed(this, bos, ByteOrder.BIG_ENDIAN)
        }.toByteArray()
    )
}
