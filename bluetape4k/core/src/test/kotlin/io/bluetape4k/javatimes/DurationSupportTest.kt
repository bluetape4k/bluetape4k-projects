package io.bluetape4k.javatimes

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldFailCheck
import io.bluetape4k.assertions.shouldFailRequire
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationSupportTest {

    companion object: KLogging() {
        val durationIsoFormat: Regex =
            """P(?<year>\d)Y(?<month>\d)M(?<day>\d)DT(?<hour>\d)H(?<minute>\d)M(?<second>\d)\.(?<mills>\d{3})S""".toRegex()
    }

    @Test
    fun `format duration with ISO format`() {
        val zero = 0.millis().formatISO()
        zero shouldBeEqualTo "P0Y0M0DT0H0M0.000S"

        val fiveSeconds = 5000L.millis().formatISO()
        fiveSeconds shouldBeEqualTo "P0Y0M0DT0H0M5.000S"

        val twoHours = 2.hours().formatISO()
        twoHours shouldBeEqualTo "P0Y0M0DT2H0M0.000S"

        val nineDaysAndFiveHours = (9.days() + 5.hours()).formatISO()
        nineDaysAndFiveHours shouldBeEqualTo "P0Y0M9DT5H0M0.000S"

        val nineDaysMinusFiveHours = (9.days() + (-5).hours()).formatISO()
        nineDaysMinusFiveHours shouldBeEqualTo "P0Y0M8DT19H0M0.000S"
    }

    @Test
    fun `format duration with HMS format`() {
        val zero = 0.millis().formatHMS()
        zero shouldBeEqualTo "00:00:00.000"

        val fiveSeconds = 5000L.millis().formatHMS()
        fiveSeconds shouldBeEqualTo "00:00:05.000"

        val twoHours = 2.hours().formatHMS()
        twoHours shouldBeEqualTo "02:00:00.000"
    }

    @Test
    fun `parse ISO Formatted Duration`() {
        val matchResult = durationIsoFormat.matchEntire("P0Y0M5DT4H3M9.123S")
        matchResult.shouldNotBeNull()

        val year = matchResult.groups["year"]?.value?.toInt()
        val month = matchResult.groups["month"]?.value?.toInt()
        val day = matchResult.groups["day"]?.value?.toInt()
        val hour = matchResult.groups["hour"]?.value?.toInt()
        log.debug { "year=$year, month=$month, day=$day, hour=$hour" }

        val (y, m, d, h, min, s, ms) = matchResult.destructured
        log.debug { "y=$y, m=$m, d=$d, h=$h, min=$min, s=$s, ms=$ms" }
    }

    @Test
    fun `parse ISO formatted duration`() {
        val duration = parseIsoFormattedDuration("P0Y0M5DT4H3M9.123S")
        duration.shouldNotBeNull()
    }

    @Test
    fun `duration sign require and check helpers are complementary`() {
        val positive = Duration.ofSeconds(1)
        val negative = Duration.ofSeconds(-1)

        positive.requirePositiveDuration("duration") shouldBeEqualTo positive
        negative.requireZeroOrNegativeDuration("duration") shouldBeEqualTo negative
        Duration.ZERO.requireZeroOrPositiveDuration("duration") shouldBeEqualTo Duration.ZERO
        Duration.ZERO.requireZeroOrNegativeDuration("duration") shouldBeEqualTo Duration.ZERO

        shouldFailRequire { Duration.ZERO.requirePositiveDuration("duration") }
        shouldFailRequire { positive.requireZeroOrNegativeDuration("duration") }
        shouldFailRequire { negative.requireZeroOrPositiveDuration("duration") }
        shouldFailRequire { negative.requirePositiveDuration("duration") }

        positive.checkPositiveDuration("duration") shouldBeEqualTo positive
        negative.checkZeroOrNegativeDuration("duration") shouldBeEqualTo negative
        Duration.ZERO.checkZeroOrPositiveDuration("duration") shouldBeEqualTo Duration.ZERO
        Duration.ZERO.checkZeroOrNegativeDuration("duration") shouldBeEqualTo Duration.ZERO

        shouldFailCheck { Duration.ZERO.checkPositiveDuration("duration") }
        shouldFailCheck { positive.checkZeroOrNegativeDuration("duration") }
        shouldFailCheck { negative.checkZeroOrPositiveDuration("duration") }
        shouldFailCheck { negative.checkPositiveDuration("duration") }
    }
}
