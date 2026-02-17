package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Duration
import java.time.Period

/**
 * [DateSupport.kt]에 대한 테스트
 */
class DateSupportTest {

    companion object: KLogging()

    @Test
    fun `dateOf로 Date 생성`() {
        val now = System.currentTimeMillis()
        val date1 = dateOf(now)
        date1.time shouldBeEqualTo now

        val date2 = dateOf()
        date2.time shouldBeGreaterThan 0L
    }

    @Test
    fun `Date plus Date 연산`() {
        val date1 = dateOf(1000L)
        val date2 = dateOf(2000L)

        val result = date1 + date2
        result.time shouldBeEqualTo 3000L
    }

    @Test
    fun `Date plus Long 연산`() {
        val date = dateOf(1000L)

        val result = date + 500L
        result.time shouldBeEqualTo 1500L
    }

    @Test
    fun `Date plus Duration 연산`() {
        val date = dateOf(1000L)
        val duration = Duration.ofMillis(500)

        val result = date + duration
        result.time shouldBeEqualTo 1500L
    }

    @Test
    fun `Date plus Period 연산`() {
        val date = dateOf(0L)
        val period = Period.ofDays(2)

        val result = date + period
        result.time shouldBeEqualTo 2 * MILLIS_IN_DAY
    }

    @Test
    fun `Date minus Date 연산`() {
        val date1 = dateOf(3000L)
        val date2 = dateOf(1000L)

        val result = date1 - date2
        result.time shouldBeEqualTo 2000L
    }

    @Test
    fun `Date minus Long 연산`() {
        val date = dateOf(1500L)

        val result = date - 500L
        result.time shouldBeEqualTo 1000L
    }

    @Test
    fun `Date minus Duration 연산`() {
        val date = dateOf(1500L)
        val duration = Duration.ofMillis(500)

        val result = date - duration
        result.time shouldBeEqualTo 1000L
    }

    @Test
    fun `Date minus Period 연산`() {
        val date = dateOf(5 * MILLIS_IN_DAY)
        val period = Period.ofDays(2)

        val result = date - period
        result.time shouldBeEqualTo 3 * MILLIS_IN_DAY
    }

    @Test
    fun `Timestamp plus Timestamp 연산`() {
        val ts1 = Timestamp(1000L)
        val ts2 = Timestamp(2000L)

        val result = ts1 + ts2
        result.time shouldBeEqualTo 3000L
    }

    @Test
    fun `Timestamp plus Long 연산`() {
        val ts = Timestamp(1000L)

        val result = ts + 500L
        result.time shouldBeEqualTo 1500L
    }

    @Test
    fun `Timestamp plus Duration 연산`() {
        val ts = Timestamp(1000L)
        val duration = Duration.ofMillis(500)

        val result = ts + duration
        result.time shouldBeEqualTo 1500L
    }

    @Test
    fun `Timestamp plus Period 연산`() {
        val ts = Timestamp(0L)
        val period = Period.ofDays(2)

        val result = ts + period
        result.time shouldBeEqualTo 2 * MILLIS_IN_DAY
    }

    @Test
    fun `Timestamp minus Timestamp 연산`() {
        val ts1 = Timestamp(3000L)
        val ts2 = Timestamp(1000L)

        val result = ts1 - ts2
        result.time shouldBeEqualTo 2000L
    }

    @Test
    fun `Timestamp minus Long 연산`() {
        val ts = Timestamp(1500L)

        val result = ts - 500L
        result.time shouldBeEqualTo 1000L
    }

    @Test
    fun `Timestamp minus Duration 연산`() {
        val ts = Timestamp(1500L)
        val duration = Duration.ofMillis(500)

        val result = ts - duration
        result.time shouldBeEqualTo 1000L
    }

    @Test
    fun `Timestamp minus Period 연산`() {
        val ts = Timestamp(5 * MILLIS_IN_DAY)
        val period = Period.ofDays(2)

        val result = ts - period
        result.time shouldBeEqualTo 3 * MILLIS_IN_DAY
    }

    @Test
    fun `Date와 Timestamp 혼합 연산`() {
        val date = dateOf(1000L)
        val ts = Timestamp(500L)

        // Date는 Date로, Timestamp는 Timestamp로 반환
        (date + 100L).time shouldBeEqualTo 1100L
        (ts + 100L).time shouldBeEqualTo 600L
    }
}
