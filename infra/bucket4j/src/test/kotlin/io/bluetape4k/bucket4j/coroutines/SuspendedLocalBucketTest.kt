package io.bluetape4k.bucket4j.coroutines

import io.bluetape4k.bucket4j.AbstractBucket4jTest
import io.bluetape4k.bucket4j.addBandwidth
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.bucket4j.BandwidthBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class SuspendedLocalBucketTest: AbstractBucket4jTest() {

    companion object: KLoggingChannel()

    private lateinit var bucket: SuspendLocalBucket

    @BeforeEach
    fun beforeEach() {
        bucket = SuspendLocalBucket {
            addBandwidth {
                BandwidthBuilder.builder()
                    .capacity(5)                                              // 5개의 토큰을 보유
                    .refillIntervally(1, Duration.ofSeconds(1)) // 1초에 1개의 토큰을 보충
                    .build()
            }
        }
    }

    @Nested
    inner class SuspendConsume {

        @Test
        fun `소비해야 할 토큰이 0인 경우 예외를 던져야 한다`() = runTest {
            assertFailsWith<IllegalArgumentException> {
                bucket.consume(0L)
            }
        }

        @Test
        fun `소비해야 할 토큰이 0보다 작은 경우 예외를 던져야 한다`() = runTest {
            assertFailsWith<IllegalArgumentException> {
                bucket.consume(-1L)
            }
        }

        @Test
        fun `필요한 토큰 보충 시간 동안 지연되어야 한다`() = runTest {
            val done = AtomicBoolean(false)

            // 9개의 토큰을 소비하려고 한다 (기본 5개에 1촟당 1개씩 보충)
            val job = launch {
                bucket.consume(9L)
                done.set(true)
            }

            // 4개의 토큰이 더 필요하므로, 4초가 지연되어야 한다
            done.get().shouldBeFalse()
            advanceTimeBy(3.seconds)            // 3초 밖에 지나지 않았으므로 아니다
            done.get().shouldBeFalse()

            advanceTimeBy(1.seconds)            // 토탈 4초가 지났으므로 4개의 토큰이 모두 보충되었다

            await atMost Duration.ofSeconds(2) until { done.get() }
            done.get().shouldBeTrue()

            job.cancel()
        }
    }

    @Nested
    inner class SuspendTryConsume {

        @Test
        fun `소비해야 할 토큰이 0 이하인 경우 예외를 던져야 한다`() = runTest {
            assertFailsWith<IllegalArgumentException> {
                bucket.tryConsume(0L)
            }

            assertFailsWith<IllegalArgumentException> {
                bucket.tryConsume(-1L)
            }
        }

        @Test
        fun `최대 대기 시간이 0 이하이면 예외를 던져야 한다`() = runTest {
            assertFailsWith<IllegalArgumentException> {
                bucket.tryConsume(1L, Duration.ZERO)
            }

            assertFailsWith<IllegalArgumentException> {
                bucket.tryConsume(1L, Duration.ofSeconds(-1))
            }
        }

        @Test
        fun `보유 토큰(5) 보다 많은 토큰을 소비하려고 시도하면서 대기시간이 짧으면 즉시 false를 반환한다`() = runTest {
            // 5개를 보유하고 있다 
            bucket.tryConsume(5L + 1L, Duration.ofMillis(10)).shouldBeFalse()
        }

        @Test
        fun `보유 토큰(5) 보다 많은 토큰을 소비하려고 하면 대기 시간까지 대기했다가 결과를 반환한다`() = runTest {
            val done = AtomicBoolean(false)

            // 9개의 토큰을 소비하려고 한다 (기본 5개에 1촟당 1개씩 보충)
            val task = async {
                val consumed = bucket.tryConsume(9L, Duration.ofSeconds(5))
                done.set(true)
                consumed
            }

            // 4개의 토큰이 더 필요하므로, 4초가 지연되어야 한다
            done.get().shouldBeFalse()
            advanceTimeBy(3.seconds)            // 3초 밖에 지나지 않았으므로 아니다
            done.get().shouldBeFalse()

            advanceTimeBy(1.seconds)            // 토탈 4초가 지났으므로 4개의 토큰이 모두 보충되었다

            await atMost Duration.ofSeconds(1) until { done.get() }

            done.get().shouldBeTrue()
            task.await().shouldBeTrue()
        }
    }
}
