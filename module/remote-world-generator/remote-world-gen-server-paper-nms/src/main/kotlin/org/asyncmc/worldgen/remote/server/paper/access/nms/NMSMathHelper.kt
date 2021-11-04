package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.util.MathHelper

@Suppress("NOTHING_TO_INLINE")
object NMSMathHelper {
    inline fun log2DeBruijn(value: Int): Int = MathHelper.e(value)
}
