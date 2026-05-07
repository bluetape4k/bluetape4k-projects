package io.bluetape4k.exposed.cache.redis

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

/**
 * [RedisRepositoryResilienceConfig] 단위 테스트.
 *
 * 기본값 생성, 커스텀 생성, data class 동등성, 입력 검증을 수행합니다.
 * Resilience 설정이 잘못 전달될 경우(예: 음수 retryMaxAttempts, 0ms timeout)
 * 실제 Resilience4j 사용 시 런타임 오류가 발생하므로 생성 시점에서 차단합니다.
 */
class RedisRepositoryResilienceConfigTest {

    companion object : KLogging()

    // ----------------------------------------------------------------
    // 기본값 생성
    // ----------------------------------------------------------------

    @Test
    fun `기본값으로 생성 시 명세된 기본값을 가진다`() {
        val config = RedisRepositoryResilienceConfig()

        config.retryMaxAttempts shouldBeEqualTo 3
        config.retryWaitDuration shouldBeEqualTo Duration.ofMillis(500)
        config.retryExponentialBackoff.shouldBeTrue()
        config.circuitBreakerEnabled.shouldBeFalse()
        config.timeoutDuration shouldBeEqualTo Duration.ofSeconds(2)
    }

    // ----------------------------------------------------------------
    // 커스텀 생성
    // ----------------------------------------------------------------

    @Test
    fun `커스텀 값으로 생성하면 해당 값이 그대로 저장된다`() {
        val config = RedisRepositoryResilienceConfig(
            retryMaxAttempts = 5,
            retryWaitDuration = Duration.ofMillis(200),
            retryExponentialBackoff = false,
            circuitBreakerEnabled = true,
            timeoutDuration = Duration.ofSeconds(5),
        )

        config.retryMaxAttempts shouldBeEqualTo 5
        config.retryWaitDuration shouldBeEqualTo Duration.ofMillis(200)
        config.retryExponentialBackoff.shouldBeFalse()
        config.circuitBreakerEnabled.shouldBeTrue()
        config.timeoutDuration shouldBeEqualTo Duration.ofSeconds(5)
    }

    // ----------------------------------------------------------------
    // data class 동등성
    // ----------------------------------------------------------------

    @Test
    fun `동일한 값으로 생성된 두 인스턴스는 동등하다`() {
        val config1 = RedisRepositoryResilienceConfig(retryMaxAttempts = 5)
        val config2 = RedisRepositoryResilienceConfig(retryMaxAttempts = 5)
        config1 shouldBeEqualTo config2
    }

    @Test
    fun `copy로 생성한 인스턴스는 변경된 필드만 다르다`() {
        val original = RedisRepositoryResilienceConfig()
        val copied = original.copy(circuitBreakerEnabled = true)

        copied.circuitBreakerEnabled.shouldBeTrue()
        // 나머지 필드는 동일해야 한다
        copied.retryMaxAttempts shouldBeEqualTo original.retryMaxAttempts
        copied.retryWaitDuration shouldBeEqualTo original.retryWaitDuration
        copied.timeoutDuration shouldBeEqualTo original.timeoutDuration
    }

    // ----------------------------------------------------------------
    // 입력 검증
    // ----------------------------------------------------------------

    @Test
    fun `retryMaxAttempts가 0이면 IllegalArgumentException이 발생한다`() {
        assertThrows<IllegalArgumentException> {
            RedisRepositoryResilienceConfig(retryMaxAttempts = 0)
        }
    }

    @Test
    fun `retryMaxAttempts가 음수이면 IllegalArgumentException이 발생한다`() {
        assertThrows<IllegalArgumentException> {
            RedisRepositoryResilienceConfig(retryMaxAttempts = -1)
        }
    }

    @Test
    fun `retryWaitDuration이 0이면 IllegalArgumentException이 발생한다`() {
        assertThrows<IllegalArgumentException> {
            RedisRepositoryResilienceConfig(retryWaitDuration = Duration.ZERO)
        }
    }

    @Test
    fun `retryWaitDuration이 음수이면 IllegalArgumentException이 발생한다`() {
        assertThrows<IllegalArgumentException> {
            RedisRepositoryResilienceConfig(retryWaitDuration = Duration.ofMillis(-1))
        }
    }

    @Test
    fun `timeoutDuration이 0이면 IllegalArgumentException이 발생한다`() {
        assertThrows<IllegalArgumentException> {
            RedisRepositoryResilienceConfig(timeoutDuration = Duration.ZERO)
        }
    }

    @Test
    fun `timeoutDuration이 음수이면 IllegalArgumentException이 발생한다`() {
        assertThrows<IllegalArgumentException> {
            RedisRepositoryResilienceConfig(timeoutDuration = Duration.ofMillis(-1))
        }
    }

    // ----------------------------------------------------------------
    // Serializable 직렬화 라운드트립
    // ----------------------------------------------------------------

    @Test
    fun `Java 직렬화 라운드트립을 통해 원본과 동일한 객체가 복원된다`() {
        // 분산 캐시(Lettuce/Redisson)가 설정 객체를 직렬화할 때 데이터 유실이 없어야 한다.
        val original = RedisRepositoryResilienceConfig(
            retryMaxAttempts = 7,
            circuitBreakerEnabled = true,
        )

        val bytes = java.io.ByteArrayOutputStream().use { baos ->
            java.io.ObjectOutputStream(baos).use { oos -> oos.writeObject(original) }
            baos.toByteArray()
        }
        val restored = java.io.ByteArrayInputStream(bytes).use { bais ->
            java.io.ObjectInputStream(bais).use { ois -> ois.readObject() as RedisRepositoryResilienceConfig }
        }

        restored shouldBeEqualTo original
    }
}
