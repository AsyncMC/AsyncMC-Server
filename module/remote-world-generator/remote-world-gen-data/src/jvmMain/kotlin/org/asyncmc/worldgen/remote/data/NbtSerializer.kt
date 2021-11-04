@file:JvmName("NbtSerializer")
package org.asyncmc.worldgen.remote.data

import br.com.gamemods.nbtmanipulator.NbtFile
import br.com.gamemods.nbtmanipulator.NbtIO
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

public fun NbtFile.serialize(): SerializedNbtFile {
    return SerializedNbtFile(
        littleEndian = false,
        compressed = true,
        data = ByteArrayOutputStream().use { bos ->
            DataOutputStream(bos).use { out ->
                NbtIO.writeNbtFile(out, this, compressed = true, littleEndian = false)
            }
            bos.toByteArray()
        },
    )
}

public fun SerializedNbtFile.deserialize(): NbtFile {
    return ByteArrayInputStream(data).use { bis ->
        DataInputStream(bis).use { `in` ->
            NbtIO.readNbtFile(`in`, compressed = this.compressed, littleEndian = this.littleEndian)
        }
    }
}
