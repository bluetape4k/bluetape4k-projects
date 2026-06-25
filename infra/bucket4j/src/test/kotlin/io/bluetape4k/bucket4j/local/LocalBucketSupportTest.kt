package io.bluetape4k.bucket4j.local

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.local.SynchronizationStrategy
import org.junit.jupiter.api.Test
import java.time.Duration

class LocalBucketSupportTest {

    companion object: KLogging()

    /**
     * [localBucket] DSL 이 제공된 builder 블록을 적용해 정상적으로 버킷을 생성하는지 검증한다.
     */
    @Test
    fun `localBucket DSL 은 builder 블록을 적용한 LocalBucket 을 반환한다`() {
        val bucket = localBucket {
            addLimit(
                BandwidthBuilder.builder()
                    .capacity(10)
                    .refillIntervally(10, Duration.ofSeconds(1))
                    .build()
            )
            withMillisecondPrecision()
            withSynchronizationStrategy(SynchronizationStrategy.LOCK_FREE)
        }

        bucket.shouldNotBeNull()
        bucket.availableTokens shouldBeEqualTo 10L
    }

    @Test
    fun `localBucket 은 DSL 에서 지정한 대역폭으로 토큰 소비를 제한한다`() {
        val bucket = localBucket {
            val simple = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofSeconds(1))
                .build()
            addLimit(simple)
        }

        bucket.tryConsume(5).shouldBeTrue()
        bucket.tryConsume(1).shouldBeFalse()
    }

    /**
     * [localBucketOf] 바이너리 스냅샷 복원이 올바른지 검증한다.
     * LocalBucket 의 상태(토큰 수)가 스냅샷으로 직렬화되고 다시 복원될 수 있어야 한다.
     */
    @Test
    fun `localBucketOf 는 바이너리 스냅샷으로부터 LocalBucket 을 복원한다`() {
        val original = localBucket {
            val simple = Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofSeconds(1))
                .build()
            addLimit(simple)
        }
        // 토큰 3개를 먼저 소비한 상태로 스냅샷 생성
        original.tryConsume(3).shouldBeTrue()

        val snapshot = original.toBinarySnapshot()
        val restored = localBucketOf(snapshot)

        restored.availableTokens shouldBeEqualTo 7L
    }

    /**
     * [localBucketOf] JSON 스냅샷 복원이 올바른지 검증한다.
     */
    @Test
    fun `localBucketOf 는 JSON 호환 스냅샷으로부터 LocalBucket 을 복원한다`() {
        val original = localBucket {
            val simple = Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofSeconds(1))
                .build()
            addLimit(simple)
        }
        original.tryConsume(4).shouldBeTrue()

        val snapshot = original.toJsonCompatibleSnapshot()
        val restored = localBucketOf(snapshot)

        restored.availableTokens shouldBeEqualTo 6L
    }
}
