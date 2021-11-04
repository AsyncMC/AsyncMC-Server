package org.asyncmc.worldgen.remote.server.paper.access.nms

import net.minecraft.util.DataBits
import java.util.function.IntConsumer

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class NMSDataBits(override val nms: DataBits): NMSWrapper<DataBits> {
    constructor(elementBits: Int, size: Int, storage: LongArray): this(DataBits(elementBits, size, storage))
    inline fun forEach(consumer: IntConsumer) {
        nms.forEach { _, data ->
            consumer.accept(data)
        }
    }

    inline fun forEachIndexed(noinline consumer: (Int, Int) -> Unit) {
        nms.forEach { index, data ->
            consumer(index, data)
        }
    }
}
