package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.nbt.NBTCompressedStreamTools
import java.io.OutputStream

@Suppress("NOTHING_TO_INLINE")
object NMSNbtIo {
    inline fun writeCompressed(nbt: NMSNBTTagCompound, stream: OutputStream) {
        NBTCompressedStreamTools.a(nbt.nms, stream)
    }
}
