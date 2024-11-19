package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class DurationSupportTest {

    companion object: KLogging() {
        val durationIsoFormat: Regex =
            """P(?<year>\d)Y(?<month>\d)M(?<day>\d)DT(?<hour>\d)H(?<minute>\d)M(?<second>\d)\.(?<mills>\d{3})S""".toRegex()
    }

    @Test
    fun `format duration with ISO format`() {
        val zero = 0.asMillis().formatISO()
        zero shouldBeEqualTo "P0Y0M0DT0H0M0.000S"

        val fiveSeconds = 5000L.asMillis().formatISO()
        fiveSeconds shouldBeEqualTo "P0Y0M0DT0H0M5.000S"

        val twoHours = 2.asHours().formatISO()
        twoHours shouldBeEqualTo "P0Y0M0DT2H0M0.000S"

        val nineDaysAndFiveHours = (9.asDays() + 5.asHours()).formatISO()
        nineDaysAndFiveHours shouldBeEqualTo "P0Y0M9DT5H0M0.000S"

        val nineDaysMinusFiveHours = (9.asDays() + (-5).asHours()).formatISO()
        nineDaysMinusFiveHours shouldBeEqualTo "P0Y0M8DT19H0M0.000S"
    }

    @Test
    fun `format duration with HMS format`() {
        val zero = 0.asMillis().formatHMS()
        zero shouldBeEqualTo "00:00:00.000"

        val fiveSeconds = 5000L.asMillis().formatHMS()
        fiveSeconds shouldBeEqualTo "00:00:05.000"

        val twoHours = 2.asHours().formatHMS()
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
}
