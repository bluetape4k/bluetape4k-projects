package io.bluetape4k.idgenerators.ulid.internal

import io.bluetape4k.idgenerators.ulid.ULID
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic

internal class ULIDStatefulMonotonic(
    private val factory: ULID.Factory,
    private val monotonic: ULID.Monotonic,
) : ULID.StatefulMonotonic {
    companion object : KLogging()

    private val previousRef = atomic<ULID?>(null)

    override fun nextULID(timestamp: Long): ULID {
        while (true) {
            val prev = previousRef.value
            val result = if (prev == null) factory.nextULID(timestamp) else monotonic.nextULID(prev, timestamp)
            if (previousRef.compareAndSet(prev, result)) {
                return result
            }
        }
    }

    override fun nextULIDStrict(timestamp: Long): ULID? {
        while (true) {
            val prev = previousRef.value
            val result = if (prev == null) factory.nextULID(timestamp) else monotonic.nextULIDStrict(prev, timestamp)
            if (previousRef.compareAndSet(prev, result ?: prev)) {
                return result
            }
        }
    }

    override fun randomULID(timestamp: Long): String = factory.randomULID(timestamp)

    override fun fromByteArray(data: ByteArray): ULID = factory.fromByteArray(data)

    override fun parseULID(ulidString: String): ULID = factory.parseULID(ulidString)
}
