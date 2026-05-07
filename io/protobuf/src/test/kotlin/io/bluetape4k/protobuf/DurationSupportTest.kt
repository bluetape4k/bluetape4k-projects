package io.bluetape4k.protobuf

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class DurationSupportTest {
    companion object : KLogging()

    @Test
    fun `PROTO_DURATION_MIN 상수는 최소값을 가진다`() {
        PROTO_DURATION_MIN.shouldNotBeNull()
        PROTO_DURATION_MIN.seconds shouldBeEqualTo -315_576_000_000L
    }

    @Test
    fun `PROTO_DURATION_MAX 상수는 최대값을 가진다`() {
        PROTO_DURATION_MAX.shouldNotBeNull()
        PROTO_DURATION_MAX.seconds shouldBeEqualTo 315_576_000_000L
    }

    @Test
    fun `PROTO_DURATION_ZERO 상수는 0초를 나타낸다`() {
        PROTO_DURATION_ZERO.shouldNotBeNull()
        PROTO_DURATION_ZERO.seconds shouldBeEqualTo 0L
        PROTO_DURATION_ZERO.nanos shouldBeEqualTo 0
    }

    @Test
    fun `protoDurationOfSeconds - 초 단위 Duration을 생성한다`() {
        val d = protoDurationOfSeconds(30L)
        d.toSeconds() shouldBeEqualTo 30L
    }

    @Test
    fun `protoDurationOfMillis - 밀리초 단위 Duration을 생성한다`() {
        val d = protoDurationOfMillis(1500L)
        d.toMillis() shouldBeEqualTo 1500L
    }

    @Test
    fun `protoDurationOfMicros - 마이크로초 단위 Duration을 생성한다`() {
        val d = protoDurationOfMicros(2_000_000L)
        d.toMicros() shouldBeEqualTo 2_000_000L
    }

    @Test
    fun `protoDurationOfNanos - 나노초 단위 Duration을 생성한다`() {
        val d = protoDurationOfNanos(1_000_000_000L)
        d.toNanos() shouldBeEqualTo 1_000_000_000L
    }

    @Test
    fun `protoDurationOfMinutes - 분 단위 Duration을 생성한다`() {
        val d = protoDurationOfMinutes(5L)
        d.toSeconds() shouldBeEqualTo 300L
    }

    @Test
    fun `protoDurationOfHours - 시간 단위 Duration을 생성한다`() {
        val d = protoDurationOfHours(3L)
        d.toSeconds() shouldBeEqualTo 10_800L
    }

    @Test
    fun `protoDurationOfDays - 일 단위 Duration을 생성한다`() {
        val d = protoDurationOfDays(2L)
        d.toSeconds() shouldBeEqualTo 172_800L
    }

    @Test
    fun `protoDurationOf(String) - 문자열을 Duration으로 파싱한다`() {
        val d = protoDurationOf("1.500s")
        d.toMillis() shouldBeEqualTo 1500L
    }

    @Test
    fun `protoDurationOf(JavaDuration) - Java Duration을 Protobuf Duration으로 변환한다`() {
        val javaDuration = java.time.Duration.ofMillis(1200L)
        val proto = protoDurationOf(javaDuration)
        proto.toMillis() shouldBeEqualTo 1200L
    }

    @Test
    fun `toJavaDuration - Protobuf Duration을 Java Duration으로 변환한다`() {
        val proto = protoDurationOfMillis(1500L)
        val java = proto.toJavaDuration()
        java.toMillis() shouldBeEqualTo 1500L
    }

    @Test
    fun `toJavaDuration 왕복 변환 - Java Duration → Protobuf → Java Duration`() {
        val original = java.time.Duration.ofSeconds(5L, 123_456_789L)
        val proto = protoDurationOf(original)
        val restored = proto.toJavaDuration()
        restored shouldBeEqualTo original
    }

    @Test
    fun `asString - Duration을 문자열로 변환한다`() {
        val d = protoDurationOfMillis(1500L)
        d.asString() shouldBeEqualTo "1.500s"
    }

    @Test
    fun `isValid - 유효 범위 Duration은 true를 반환한다`() {
        protoDurationOfSeconds(10L).isValid.shouldBeTrue()
    }

    @Test
    fun `isPositive - 양수 Duration은 true를 반환한다`() {
        protoDurationOfSeconds(1L).isPositive.shouldBeTrue()
    }

    @Test
    fun `isNegative - 음수 Duration은 true를 반환한다`() {
        protoDurationOfSeconds(-1L).isNegative.shouldBeTrue()
    }

    @Test
    fun `isPositive - ZERO는 false를 반환한다`() {
        PROTO_DURATION_ZERO.isPositive.shouldBeFalse()
    }

    @Test
    fun `compareTo - 짧은 Duration이 긴 Duration보다 작다`() {
        val short = protoDurationOfSeconds(1L)
        val long = protoDurationOfSeconds(2L)
        (short.compareTo(long) < 0).shouldBeTrue()
    }

    @Test
    fun `plus 연산자 - 두 Duration을 더한다`() {
        val sum = protoDurationOfSeconds(1L) + protoDurationOfMillis(500L)
        sum.toMillis() shouldBeEqualTo 1500L
    }

    @Test
    fun `minus 연산자 - 두 Duration을 뺀다`() {
        val diff = protoDurationOfSeconds(2L) - protoDurationOfMillis(500L)
        diff.toMillis() shouldBeEqualTo 1500L
    }

    @Test
    fun `toDays - 시간 초과 분은 버린다`() {
        val d = protoDurationOfHours(25L)
        d.toDays() shouldBeEqualTo 1L
    }

    @Test
    fun `toHours - 분 초과는 버린다`() {
        val d = protoDurationOfMinutes(90L)
        d.toHours() shouldBeEqualTo 1L
    }

    @Test
    fun `toMinutes - 초 초과는 버린다`() {
        val d = protoDurationOfSeconds(90L)
        d.toMinutes() shouldBeEqualTo 1L
    }

    @Test
    fun `toSecondsAsDouble - 소수점 포함 초 변환`() {
        val d = protoDurationOfMillis(1500L)
        d.toSecondsAsDouble() shouldBeEqualTo 1.5
    }
}
