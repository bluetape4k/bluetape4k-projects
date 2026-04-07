package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

class YearQuarterTest {
    companion object: KLogging()

    @Test
    fun `YearQuarter 생성 - 년도와 분기로 생성`() {
        val yq = YearQuarter(2024, Quarter.Q1)
        yq.year shouldBeEqualTo 2024
        yq.quarter shouldBeEqualTo Quarter.Q1
    }

    @Test
    fun `YearQuarter 생성 - LocalDateTime으로 생성`() {
        val ldt = LocalDateTime.of(2024, 4, 15, 0, 0)
        val yq = YearQuarter(ldt)
        yq.year shouldBeEqualTo 2024
        yq.quarter shouldBeEqualTo Quarter.Q2
    }

    @Test
    fun `YearQuarter 생성 - OffsetDateTime으로 생성`() {
        val odt = OffsetDateTime.of(2024, 7, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC)
        val yq = YearQuarter(odt)
        yq.year shouldBeEqualTo 2024
        yq.quarter shouldBeEqualTo Quarter.Q3
    }

    @Test
    fun `YearQuarter 생성 - ZonedDateTime으로 생성`() {
        val zdt = ZonedDateTime.of(2024, 10, 1, 0, 0, 0, 0, java.time.ZoneId.of("UTC"))
        val yq = YearQuarter(zdt)
        yq.year shouldBeEqualTo 2024
        yq.quarter shouldBeEqualTo Quarter.Q4
    }

    @Test
    fun `YearQuarter 동등성 비교`() {
        val yq1 = YearQuarter(2024, Quarter.Q1)
        val yq2 = YearQuarter(2024, Quarter.Q1)
        val yq3 = YearQuarter(2024, Quarter.Q2)

        yq1 shouldBeEqualTo yq2
        yq1 shouldNotBeEqualTo yq3
    }

    @Test
    fun `모든 분기에 대해 월별 YearQuarter 매핑이 올바른지 검증`() {
        val monthToQuarter =
            mapOf(
                1 to Quarter.Q1,
                2 to Quarter.Q1,
                3 to Quarter.Q1,
                4 to Quarter.Q2,
                5 to Quarter.Q2,
                6 to Quarter.Q2,
                7 to Quarter.Q3,
                8 to Quarter.Q3,
                9 to Quarter.Q3,
                10 to Quarter.Q4,
                11 to Quarter.Q4,
                12 to Quarter.Q4,
            )

        monthToQuarter.forEach { (month, expectedQuarter) ->
            val ldt = LocalDateTime.of(2024, month, 1, 0, 0)
            val yq = YearQuarter(ldt)
            yq.quarter shouldBeEqualTo expectedQuarter
        }
    }
}
