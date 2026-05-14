package io.bluetape4k.bucket4j

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.github.bucket4j.Bucket
import io.github.bucket4j.TokensInheritanceStrategy
import org.junit.jupiter.api.Test
import java.time.Duration
import io.bluetape4k.assertions.assertFailsWith

class ConfigurationSupportTest {

    companion object: KLogging()

    /**
     * [bucketConfiguration] DSL 이 bandwidth 를 정상 등록하는지 검증한다.
     * 내부적으로 BucketConfiguration.builder() 를 래핑하므로 bandwidths 수를 통해 동작을 확인한다.
     */
    @Test
    fun `bucketConfiguration DSL 은 bandwidth 를 올바르게 등록한다`() {
        val config = bucketConfiguration {
            addLimit {
                it.capacity(10).refillIntervally(10, Duration.ofSeconds(1))
            }
        }

        config.shouldNotBeNull()
        config.bandwidths.size shouldBeEqualTo 1
        config.bandwidths.first().capacity shouldBeEqualTo 10L
    }

    @Test
    fun `bucketConfiguration DSL 은 여러 bandwidth 를 등록할 수 있다`() {
        val config = bucketConfiguration {
            addLimit { it.capacity(100).refillIntervally(100, Duration.ofMinutes(1)) }
            addLimit { it.capacity(20).refillIntervally(20, Duration.ofSeconds(1)) }
        }

        config.bandwidths.size shouldBeEqualTo 2
    }

    /**
     * [addBandwidth] 는 builder 체이닝을 지원하므로 동일 builder 에 연속 호출해도 결과가 누적된다.
     */
    @Test
    fun `addBandwidth 확장 함수는 builder 를 체이닝하며 bandwidth 를 추가한다`() {
        val config = bucketConfiguration {
            addBandwidth { io.github.bucket4j.Bandwidth.simple(50, Duration.ofSeconds(10)) }
            addBandwidth { io.github.bucket4j.Bandwidth.simple(5, Duration.ofSeconds(1)) }
        }

        config.bandwidths.size shouldBeEqualTo 2
    }

    @Test
    fun `bucketConfiguration 은 호출마다 새 BucketConfiguration 인스턴스를 반환한다`() {
        val config1 = bucketConfiguration {
            addLimit { it.capacity(10).refillIntervally(10, Duration.ofSeconds(1)) }
        }
        val config2 = bucketConfiguration {
            addLimit { it.capacity(10).refillIntervally(10, Duration.ofSeconds(1)) }
        }

        // 동일한 설정이라도 별개 인스턴스여야 한다
        (config1 !== config2) shouldBeEqualTo true
    }

    @Test
    fun `addBandwidth supplier 에서 예외 발생 시 그대로 전파된다`() {
        assertFailsWith<IllegalStateException> {
            bucketConfiguration {
                addBandwidth { error("bandwidth 생성 실패") }
            }
        }
    }

    @Test
    fun `bandwidth id 는 configuration replacement 를 위해 보존된다`() {
        val config = bucketConfiguration {
            addLimit { it.capacity(10).refillGreedy(10, Duration.ofSeconds(1)).id("burst") }
            addLimit { it.capacity(100).refillGreedy(100, Duration.ofMinutes(1)).id("sustained") }
        }

        config.bandwidths.map { it.id } shouldBeEqualTo listOf("burst", "sustained")
    }

    @Test
    fun `identified bandwidth 는 replaceConfiguration 에서 proportional token 을 보존한다`() {
        val initial = bucketConfiguration {
            addLimit { it.capacity(10).refillGreedy(10, Duration.ofSeconds(1)).id("burst") }
        }
        val replacement = bucketConfiguration {
            addLimit { it.capacity(20).refillGreedy(20, Duration.ofSeconds(1)).id("burst") }
        }
        val bucket = Bucket.builder()
            .addLimit(initial.bandwidths.first())
            .build()

        bucket.tryConsume(5)
        bucket.replaceConfiguration(replacement, TokensInheritanceStrategy.PROPORTIONALLY)

        bucket.availableTokens shouldBeEqualTo 10
    }
}
