package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger

class LockObservationTest {

    companion object: KLogging()

    @Test
    fun `observation payload exposes only allowlisted bounded dimensions`() {
        val dimensions = LockDimensions(
            objectKind = LockKind.FAIR,
            operation = LockOperation.ACQUIRE,
            outcome = LockOutcome.CONTENDED,
            failureKind = null,
            leasePolicy = LockLeasePolicyKind.WATCHDOG,
        )
        val observation = LockObservation.Counter(
            LockCounterName.OPERATION_TOTAL,
            delta = 1,
            dimensions = dimensions,
        )

        dimensions.javaClass.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .sorted() shouldBeEqualTo listOf("failureKind", "leasePolicy", "objectKind", "operation", "outcome")

        Serializable::class.java.isAssignableFrom(observation.javaClass).shouldBeFalse()
    }

    @Test
    fun `default sink is no-op and throwing sink is isolated`() {
        val calls = AtomicInteger()
        val observation = LockObservation.Event(
            LockEvent(
                LockKind.DISTRIBUTED,
                LockOperation.RELEASE,
                LockOutcome.SUCCEEDED,
                null,
                LockLeasePolicyKind.FIXED,
            ),
        )

        LockObservationSink.NOOP.recordSafely(observation)

        LockObservationSink {
            calls.incrementAndGet()
            error("sink-secret")
        }.recordSafely(observation)

        calls.get() shouldBeEqualTo 1
    }

    @Test
    fun `observation enum sets are fixed and contain no caller labels`() {
        LockOperation.entries.map { it.name } shouldBeEqualTo
                listOf("ACQUIRE", "INSPECT", "RECONCILE", "RENEW", "RELEASE", "DOWNGRADE", "CLEANUP", "CLOSE")
        LockOutcome.entries.map { it.name } shouldBeEqualTo
                listOf(
                    "SUCCEEDED",
                    "CONTENDED",
                    "TIMED_OUT",
                    "CANCELLED",
                    "AMBIGUOUS",
                    "OWNERSHIP_LOST",
                    "CAPACITY_REJECTED",
                    "BACKEND_FAILED",
                    "INTEGRITY_FAILED",
                    "CLOSED",
                )
        LockLeasePolicyKind.entries.map { it.name } shouldBeEqualTo listOf("FIXED", "WATCHDOG", "NONE")
        LockFailureMetricKind.entries.size shouldBeEqualTo 8

        LockCounterName.entries.forEach {
            log.debug { "LockCounterName:$it" }
        }
        LockCounterName.entries.size shouldBeEqualTo 10

        LockGaugeName.entries.forEach {
            log.debug { "LockGaugeName:$it" }
        }
        LockGaugeName.entries.size shouldBeEqualTo 6

        LockHistogramName.entries.forEach {
            log.debug { "LockHistogramName:$it" }
        }
        LockHistogramName.entries.size shouldBeEqualTo 4
    }
}
