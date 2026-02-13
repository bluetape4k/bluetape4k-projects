package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class JavaTimeDurationSupportTest {

    @Test
    fun `duration unaryMinus는 부호를 반전한다`() {
        (-Duration.ofSeconds(3)) shouldBeEqualTo Duration.ofSeconds(-3)
    }

    @Test
    fun `duration 부호 판별 확장함수`() {
        Duration.ZERO.isNotPositive.shouldBeTrue()
        Duration.ofSeconds(-1).isNotPositive.shouldBeTrue()
        Duration.ofSeconds(1).isNotPositive.shouldBeFalse()

        Duration.ZERO.isNotNegative.shouldBeTrue()
        Duration.ofSeconds(1).isNotNegative.shouldBeTrue()
        Duration.ofSeconds(-1).isNotNegative.shouldBeFalse()
    }

    @Test
    fun `duration 환산 및 nanosOfMillis`() {
        val duration = Duration.ofMillis(1234).plusNanos(567_890)

        duration.inMillis() shouldBeEqualTo 1234L
        duration.inNanos() shouldBeEqualTo 1_234_567_890L
        duration.nanosOfMillis() shouldBeEqualTo 567_890
    }

    @Test
    fun `durationOf는 두 temporal 사이 duration을 생성한다`() {
        val start = Instant.parse("2020-01-01T00:00:00Z")
        val end = Instant.parse("2020-01-01T00:00:05Z")

        durationOf(start, end) shouldBeEqualTo Duration.ofSeconds(5)
    }
}

