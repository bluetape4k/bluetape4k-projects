package io.bluetape4k.cassandra.data

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.assertFailsWith

class CqlDurationSupportTest {

    companion object: KLogging()

    @Test
    fun `should convert Duration to CqlDuration`() {
        val duration = Duration.ofDays(42)
        val cqlDuration = duration.toCqlDuration()

        log.debug { "42 Days = $cqlDuration" }

        cqlDuration.months shouldBeEqualTo 0
        cqlDuration.days shouldBeEqualTo duration.toDays().toInt()
        cqlDuration.nanoseconds shouldBeEqualTo duration.toNanos()

        assertFailsWith<UnsupportedTemporalTypeException> {
            cqlDuration[ChronoUnit.SECONDS] shouldBeEqualTo 0L
        }
    }

    @Test
    fun `should negative duration convert to CqlDuration`() {
        val duration = Duration.ofDays(3).negated()
        val cqlDuration = duration.toCqlDuration()

        log.debug { "negated duration = $cqlDuration" }

        cqlDuration.months shouldBeEqualTo 0
        cqlDuration.days shouldBeEqualTo duration.toDays().toInt()
        cqlDuration.nanoseconds shouldBeEqualTo duration.toNanos()
    }

    @Test
    fun `should build CqlDuration`() {

        with(cqlDurationOf(3, 23, 42L)) {
            months shouldBeEqualTo 3
            days shouldBeEqualTo 23
            nanoseconds shouldBeEqualTo 42
        }

        with(cqlDurationOf(13, 42, 5)) {
            months shouldBeEqualTo 13
            days shouldBeEqualTo 42
            nanoseconds shouldBeEqualTo 5
        }

        with(cqlDurationOf(-13, -42, -5)) {
            months shouldBeEqualTo -13
            days shouldBeEqualTo -42
            nanoseconds shouldBeEqualTo -5
        }
    }

    @Test
    fun `all part should same sign`() {
        assertFailsWith<IllegalArgumentException> {
            with(cqlDurationOf(13, 42, -5)) {
                months shouldBeEqualTo 13
                days shouldBeEqualTo 42
                nanoseconds shouldBeEqualTo -5
            }
        }
    }
}
