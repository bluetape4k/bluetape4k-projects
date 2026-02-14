package io.bluetape4k.bucket4j.coroutines

import io.bluetape4k.bucket4j.AbstractBucket4jTest
import io.bluetape4k.bucket4j.addBandwidth
import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.BucketListener
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class SuspendLocalBucketListenerTest: AbstractBucket4jTest() {

    @Test
    fun `대기 후 consume 성공 시 delayed 와 parked 이벤트가 발생한다`() = runTest {
        val listener = RecordingBucketListener()
        val bucket = SuspendLocalBucket(listener = listener) {
            addBandwidth {
                BandwidthBuilder.builder()
                    .capacity(1)
                    .refillIntervally(1, Duration.ofSeconds(1))
                    .build()
            }
        }

        val job = launch { bucket.consume(2) } // 최소 1초 대기
        runCurrent()

        listener.delayedNanos.value.shouldBeGreaterThan(0L)
        listener.parkedNanos.value shouldBeEqualTo 0L

        advanceTimeBy(1.seconds)
        runCurrent()
        job.cancelAndJoin()

        listener.parkedNanos.value.shouldBeGreaterThan(0L)
    }

    @Test
    fun `대기 중 취소되면 interrupted 이벤트가 발생한다`() = runTest {
        val listener = RecordingBucketListener()
        val bucket = SuspendLocalBucket(listener = listener) {
            addBandwidth {
                BandwidthBuilder.builder()
                    .capacity(1)
                    .refillIntervally(1, Duration.ofSeconds(1))
                    .build()
            }
        }

        val job = launch { bucket.consume(2) }
        runCurrent()
        job.cancelAndJoin()

        listener.interruptedCount.value shouldBeEqualTo 1L
    }

    private class RecordingBucketListener: BucketListener {
        val delayedNanos = atomic(0L)
        val parkedNanos = atomic(0L)
        val interruptedCount = atomic(0L)

        override fun onConsumed(tokens: Long) = Unit
        override fun onRejected(tokens: Long) = Unit

        override fun onDelayed(nanos: Long) {
            delayedNanos.addAndGet(nanos)
        }

        override fun onParked(nanos: Long) {
            parkedNanos.addAndGet(nanos)
        }

        override fun onInterrupted(e: InterruptedException?) {
            interruptedCount.incrementAndGet()
        }
    }
}
