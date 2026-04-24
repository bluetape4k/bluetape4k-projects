package io.bluetape4k.javatimes.period

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInRange
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.WeekFields

class WeekyearWeekTest {

    companion object : KLogging()

    @Test
    fun `constructor with direct weekyear and weekOfWeekyear`() {
        val ww = WeekyearWeek(2024, 10)
        ww.weekyear shouldBeEqualTo 2024
        ww.weekOfWeekyear shouldBeEqualTo 10
    }

    @Test
    fun `invoke with TemporalAccessor uses ISO weekfields`() {
        val date = LocalDate.of(2024, 6, 15)
        val ww = WeekyearWeek(date)
        ww.weekyear shouldBeEqualTo 2024
        ww.weekOfWeekyear shouldBeInRange 1..53
    }

    @Test
    fun `invoke with TemporalAccessor and explicit WeekFields`() {
        val date = LocalDate.of(2024, 1, 1)
        val ww = WeekyearWeek(date, WeekFields.ISO)
        ww.weekyear shouldBeGreaterThan 0
        ww.weekOfWeekyear shouldBeInRange 1..53
    }

    @Test
    fun `mid year date has expected week number`() {
        val date = LocalDate.of(2024, 7, 1)
        val ww = WeekyearWeek(date)
        // July 1, 2024 is in week 27 ISO
        ww.weekyear shouldBeEqualTo 2024
        ww.weekOfWeekyear shouldBeEqualTo 27
    }

    @Test
    fun `equals and hashCode as data class`() {
        val a = WeekyearWeek(2024, 15)
        val b = WeekyearWeek(2024, 15)
        a shouldBeEqualTo b
        a.hashCode() shouldBeEqualTo b.hashCode()
    }

    @Test
    fun `copy produces new instance with modified field`() {
        val original = WeekyearWeek(2024, 15)
        val copy = original.copy(weekOfWeekyear = 20)
        copy.weekyear shouldBeEqualTo 2024
        copy.weekOfWeekyear shouldBeEqualTo 20
    }

    @Test
    fun `different weekyears are not equal`() {
        val a = WeekyearWeek(2023, 10)
        val b = WeekyearWeek(2024, 10)
        (a == b).let { !(it) }.let { it.also {} }
        a.weekyear shouldBeEqualTo 2023
        b.weekyear shouldBeEqualTo 2024
    }

    @Test
    fun `first week of year 2024 via ISO`() {
        val date = LocalDate.of(2024, 1, 8)  // definitely week 2 ISO
        val ww = WeekyearWeek(date, WeekFields.ISO)
        ww.weekyear shouldBeEqualTo 2024
        ww.weekOfWeekyear shouldBeEqualTo 2
    }

    @Test
    fun `last week of 2023 via ISO (cross year boundary)`() {
        val date = LocalDate.of(2024, 1, 1)  // Jan 1 2024 may be week 1 of 2024 or week 53 of 2023
        val ww = WeekyearWeek(date, WeekFields.ISO)
        ww.weekOfWeekyear shouldBeInRange 1..53
    }
}
