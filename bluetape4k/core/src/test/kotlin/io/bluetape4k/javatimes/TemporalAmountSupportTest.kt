package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit
import java.util.stream.Stream

/**
 * [TemporalAmountSupport.kt]에 대한 테스트
 */
class TemporalAmountSupportTest {

    companion object: KLogging() {
        @JvmStatic
        fun intTemporalAmountCases(): Stream<Arguments> = Stream.of(
            Arguments.of(3, ChronoUnit.YEARS, Period.ofYears(3)),
            Arguments.of(6, ChronoUnit.MONTHS, Period.ofMonths(6)),
            Arguments.of(2, ChronoUnit.WEEKS, Period.ofWeeks(2)),
            Arguments.of(5, ChronoUnit.DAYS, Duration.ofDays(5)),
            Arguments.of(12, ChronoUnit.HOURS, Duration.ofHours(12)),
            Arguments.of(30, ChronoUnit.MINUTES, Duration.ofMinutes(30)),
            Arguments.of(45, ChronoUnit.SECONDS, Duration.ofSeconds(45)),
            Arguments.of(500, ChronoUnit.MILLIS, Duration.ofMillis(500)),
            Arguments.of(1000, ChronoUnit.MICROS, Duration.ofNanos(1_000_000)),
            Arguments.of(123456789, ChronoUnit.NANOS, Duration.ofNanos(123456789)),
        )

        @JvmStatic
        fun longTemporalAmountCases(): Stream<Arguments> = Stream.of(
            Arguments.of(5L, ChronoUnit.YEARS, Period.ofYears(5)),
            Arguments.of(8L, ChronoUnit.MONTHS, Period.ofMonths(8)),
            Arguments.of(3L, ChronoUnit.WEEKS, Period.ofWeeks(3)),
            Arguments.of(10L, ChronoUnit.DAYS, Duration.ofDays(10)),
            Arguments.of(24L, ChronoUnit.HOURS, Duration.ofHours(24)),
            Arguments.of(60L, ChronoUnit.MINUTES, Duration.ofMinutes(60)),
            Arguments.of(120L, ChronoUnit.SECONDS, Duration.ofSeconds(120)),
            Arguments.of(2000L, ChronoUnit.MILLIS, Duration.ofMillis(2000)),
            Arguments.of(5000L, ChronoUnit.MICROS, Duration.ofNanos(5_000_000)),
            Arguments.of(987654321L, ChronoUnit.NANOS, Duration.ofNanos(987654321)),
        )
    }

    @Test
    fun `Duration의 nanos 속성`() {
        val duration = Duration.ofSeconds(1, 500_000_000)
        duration.nanos shouldBeEqualTo 1_500_000_000.0
        duration.nanosLong shouldBeEqualTo 1_500_000_000L
    }

    @Test
    fun `Duration의 millis 속성`() {
        val duration = Duration.ofSeconds(5)
        duration.millis shouldBeEqualTo 5000L

        val duration2 = Duration.ofMillis(1234)
        duration2.millis shouldBeEqualTo 1234L
    }

    @Test
    fun `Duration의 isZero 속성`() {
        Duration.ZERO.isZero.shouldBeTrue()
        Duration.ofMillis(0).isZero.shouldBeTrue()
        Duration.ofSeconds(1).isZero.shouldBeFalse()
        Duration.ofNanos(1).isZero.shouldBeFalse()
    }

    @Test
    fun `Duration의 isPositive 속성`() {
        Duration.ofSeconds(1).isPositive.shouldBeTrue()
        Duration.ofMillis(100).isPositive.shouldBeTrue()
        Duration.ZERO.isPositive.shouldBeFalse()
        Duration.ofSeconds(-1).isPositive.shouldBeFalse()
    }

    @Test
    fun `Duration의 isNegative 속성`() {
        Duration.ofSeconds(-1).isNegative.shouldBeTrue()
        Duration.ofMillis(-100).isNegative.shouldBeTrue()
        Duration.ZERO.isNegative.shouldBeFalse()
        Duration.ofSeconds(1).isNegative.shouldBeFalse()
    }

    @Test
    fun `TemporalAmount의 sign 속성`() {
        Duration.ofSeconds(-1).sign shouldBeEqualTo -1
        Duration.ZERO.sign shouldBeEqualTo 0
        Duration.ofNanos(1).sign shouldBeEqualTo 1
    }

    @Test
    fun `TemporalAmount의 isNotPositive와 isNotNegative 속성`() {
        Duration.ofSeconds(-1).isNotPositive.shouldBeTrue()
        Duration.ofSeconds(-1).isNotNegative.shouldBeFalse()

        Duration.ZERO.isNotPositive.shouldBeTrue()
        Duration.ZERO.isNotNegative.shouldBeTrue()

        Duration.ofSeconds(1).isNotPositive.shouldBeFalse()
        Duration.ofSeconds(1).isNotNegative.shouldBeTrue()
    }

    @Test
    fun `Period의 millis 속성 - Days만 계산됨`() {
        // Period는 날짜 기반이므로 Duration으로 변환 시 Days는 계산 가능
        val duration = Duration.ofDays(2)
        duration.millis shouldBeEqualTo (2 * 24 * 60 * 60 * 1000L)

        // Period는 정확한 millis 계산이 불가능하므로 Duration 사용
        val period = Period.ofDays(2)
        // Period는 units()를 통해 접근해야 함
        period.days shouldBeEqualTo 2
    }

    @Test
    fun `Period에 years 또는 months가 있으면 millis 변환은 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            Period.ofYears(1).millis
        }
        assertFailsWith<IllegalArgumentException> {
            Period.ofMonths(3).nanos
        }
    }

    @Test
    fun `toDurationExact와 toDurationOrNull 동작`() {
        Period.ofDays(2).toDurationExact() shouldBeEqualTo Duration.ofDays(2)
        Period.ofDays(2).toDurationOrNull() shouldBeEqualTo Duration.ofDays(2)

        assertFailsWith<IllegalArgumentException> {
            Period.ofMonths(1).toDurationExact()
        }
        Period.ofMonths(1).toDurationOrNull() shouldBeEqualTo null
    }

    @Test
    fun `지원 불가 TemporalUnit이 포함된 TemporalAmount는 변환 실패한다`() {
        val amount = object: java.time.temporal.TemporalAmount {
            override fun get(unit: TemporalUnit): Long = if (unit == ChronoUnit.CENTURIES) 1L else 0L
            override fun getUnits(): MutableList<TemporalUnit> = mutableListOf(ChronoUnit.CENTURIES)
            override fun addTo(temporal: Temporal): Temporal = temporal
            override fun subtractFrom(temporal: Temporal): Temporal = temporal
        }

        assertFailsWith<IllegalArgumentException> {
            amount.toDurationExact()
        }
        amount.toDurationOrNull() shouldBeEqualTo null
    }

    @ParameterizedTest(name = "Int temporalAmount - {1}")
    @MethodSource("intTemporalAmountCases")
    fun `Int temporalAmount - ChronoUnit별 변환`(amount: Int, unit: ChronoUnit, expected: TemporalAmount) {
        amount.temporalAmount(unit) shouldBeEqualTo expected
    }

    @ParameterizedTest(name = "Long temporalAmount - {1}")
    @MethodSource("longTemporalAmountCases")
    fun `Long temporalAmount - ChronoUnit별 변환`(amount: Long, unit: ChronoUnit, expected: TemporalAmount) {
        amount.temporalAmount(unit) shouldBeEqualTo expected
    }

    @Test
    fun `temporalAmount - 지원하지 않는 ChronoUnit은 예외 발생`() {
        assertFailsWith<IllegalArgumentException> {
            1.temporalAmount(ChronoUnit.CENTURIES)
        }

        assertFailsWith<IllegalArgumentException> {
            1L.temporalAmount(ChronoUnit.DECADES)
        }

        assertFailsWith<IllegalArgumentException> {
            1.temporalAmount(ChronoUnit.ERAS)
        }
    }

    @Test
    fun `Long temporalAmount - Int 범위를 넘는 YEARS는 예외 발생`() {
        assertFailsWith<IllegalArgumentException> {
            (Int.MAX_VALUE.toLong() + 1L).temporalAmount(ChronoUnit.YEARS)
        }
    }

    @Test
    fun `Int와 Long temporalAmount 결과는 동일`() {
        5.temporalAmount(ChronoUnit.DAYS) shouldBeEqualTo 5L.temporalAmount(ChronoUnit.DAYS)
        10.temporalAmount(ChronoUnit.HOURS) shouldBeEqualTo 10L.temporalAmount(ChronoUnit.HOURS)
        3.temporalAmount(ChronoUnit.YEARS) shouldBeEqualTo 3L.temporalAmount(ChronoUnit.YEARS)
    }
}
