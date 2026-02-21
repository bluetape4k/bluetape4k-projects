package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalUnit

/**
 * [TemporalAmountSupport.kt]에 대한 테스트
 */
class TemporalAmountSupportTest {

    companion object: KLogging()

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
        assertThrows<IllegalArgumentException> {
            Period.ofYears(1).millis
        }
        assertThrows<IllegalArgumentException> {
            Period.ofMonths(3).nanos
        }
    }

    @Test
    fun `toDurationExact와 toDurationOrNull 동작`() {
        Period.ofDays(2).toDurationExact() shouldBeEqualTo Duration.ofDays(2)
        Period.ofDays(2).toDurationOrNull() shouldBeEqualTo Duration.ofDays(2)

        assertThrows<IllegalArgumentException> {
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

        assertThrows<IllegalArgumentException> {
            amount.toDurationExact()
        }
        amount.toDurationOrNull() shouldBeEqualTo null
    }

    @Test
    fun `Int temporalAmount - YEARS`() {
        val amount = 3.temporalAmount(ChronoUnit.YEARS)
        amount shouldBeEqualTo Period.ofYears(3)
    }

    @Test
    fun `Int temporalAmount - MONTHS`() {
        val amount = 6.temporalAmount(ChronoUnit.MONTHS)
        amount shouldBeEqualTo Period.ofMonths(6)
    }

    @Test
    fun `Int temporalAmount - WEEKS`() {
        val amount = 2.temporalAmount(ChronoUnit.WEEKS)
        amount shouldBeEqualTo Period.ofWeeks(2)
    }

    @Test
    fun `Int temporalAmount - DAYS`() {
        val amount = 5.temporalAmount(ChronoUnit.DAYS)
        amount shouldBeEqualTo Duration.ofDays(5)
    }

    @Test
    fun `Int temporalAmount - HOURS`() {
        val amount = 12.temporalAmount(ChronoUnit.HOURS)
        amount shouldBeEqualTo Duration.ofHours(12)
    }

    @Test
    fun `Int temporalAmount - MINUTES`() {
        val amount = 30.temporalAmount(ChronoUnit.MINUTES)
        amount shouldBeEqualTo Duration.ofMinutes(30)
    }

    @Test
    fun `Int temporalAmount - SECONDS`() {
        val amount = 45.temporalAmount(ChronoUnit.SECONDS)
        amount shouldBeEqualTo Duration.ofSeconds(45)
    }

    @Test
    fun `Int temporalAmount - MILLIS`() {
        val amount = 500.temporalAmount(ChronoUnit.MILLIS)
        amount shouldBeEqualTo Duration.ofMillis(500)
    }

    @Test
    fun `Int temporalAmount - MICROS`() {
        val amount = 1000.temporalAmount(ChronoUnit.MICROS)
        amount shouldBeEqualTo Duration.ofNanos(1_000_000)
    }

    @Test
    fun `Int temporalAmount - NANOS`() {
        val amount = 123456789.temporalAmount(ChronoUnit.NANOS)
        amount shouldBeEqualTo Duration.ofNanos(123456789)
    }

    @Test
    fun `Long temporalAmount - YEARS`() {
        val amount = 5L.temporalAmount(ChronoUnit.YEARS)
        amount shouldBeEqualTo Period.ofYears(5)
    }

    @Test
    fun `Long temporalAmount - MONTHS`() {
        val amount = 8L.temporalAmount(ChronoUnit.MONTHS)
        amount shouldBeEqualTo Period.ofMonths(8)
    }

    @Test
    fun `Long temporalAmount - WEEKS`() {
        val amount = 3L.temporalAmount(ChronoUnit.WEEKS)
        amount shouldBeEqualTo Period.ofWeeks(3)
    }

    @Test
    fun `Long temporalAmount - DAYS`() {
        val amount = 10L.temporalAmount(ChronoUnit.DAYS)
        amount shouldBeEqualTo Duration.ofDays(10)
    }

    @Test
    fun `Long temporalAmount - HOURS`() {
        val amount = 24L.temporalAmount(ChronoUnit.HOURS)
        amount shouldBeEqualTo Duration.ofHours(24)
    }

    @Test
    fun `Long temporalAmount - MINUTES`() {
        val amount = 60L.temporalAmount(ChronoUnit.MINUTES)
        amount shouldBeEqualTo Duration.ofMinutes(60)
    }

    @Test
    fun `Long temporalAmount - SECONDS`() {
        val amount = 120L.temporalAmount(ChronoUnit.SECONDS)
        amount shouldBeEqualTo Duration.ofSeconds(120)
    }

    @Test
    fun `Long temporalAmount - MILLIS`() {
        val amount = 2000L.temporalAmount(ChronoUnit.MILLIS)
        amount shouldBeEqualTo Duration.ofMillis(2000)
    }

    @Test
    fun `Long temporalAmount - MICROS`() {
        val amount = 5000L.temporalAmount(ChronoUnit.MICROS)
        amount shouldBeEqualTo Duration.ofNanos(5_000_000)
    }

    @Test
    fun `Long temporalAmount - NANOS`() {
        val amount = 987654321L.temporalAmount(ChronoUnit.NANOS)
        amount shouldBeEqualTo Duration.ofNanos(987654321)
    }

    @Test
    fun `temporalAmount - 지원하지 않는 ChronoUnit은 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            1.temporalAmount(ChronoUnit.CENTURIES)
        }

        assertThrows<IllegalArgumentException> {
            1L.temporalAmount(ChronoUnit.DECADES)
        }

        assertThrows<IllegalArgumentException> {
            1.temporalAmount(ChronoUnit.ERAS)
        }
    }

    @Test
    fun `Long temporalAmount - Int 범위를 넘는 YEARS는 예외 발생`() {
        assertThrows<IllegalArgumentException> {
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
