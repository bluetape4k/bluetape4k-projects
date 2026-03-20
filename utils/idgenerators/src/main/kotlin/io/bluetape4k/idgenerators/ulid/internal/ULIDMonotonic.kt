package io.bluetape4k.idgenerators.ulid.internal

import io.bluetape4k.idgenerators.ulid.ULID
import io.bluetape4k.logging.KLogging

internal class ULIDMonotonic(
    private val factory: ULID.Factory,
) : ULID.Monotonic {
    companion object : KLogging() {
        val DefaultMonotonic = ULIDMonotonic(ULID)
    }

    override fun nextULID(
        previous: ULID,
        timestamp: Long,
    ): ULID =
        when (previous.timestamp) {
            timestamp -> previous.increment()
            else -> factory.nextULID(timestamp)
        }

    override fun nextULIDStrict(
        previous: ULID,
        timestamp: Long,
    ): ULID? {
        val result = nextULID(previous, timestamp)
        return if (result > previous) result else null
    }
}
