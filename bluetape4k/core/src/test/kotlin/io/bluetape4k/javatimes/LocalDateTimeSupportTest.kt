package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class LocalDateTimeSupportTest {

    companion object: KLogging()

    @Test
    fun `LocalDateTime 변환 기본 zone은 UTC이다`() {
        val ldt = localDateTimeOf(2021, 3, 1, 12, 30, 45, 123)

        ldt.toOffsetDateTime().offset shouldBeEqualTo ZoneOffset.UTC
        ldt.toZonedDateTime().offset shouldBeEqualTo ZoneOffset.UTC
    }

    @Test
    fun `LocalDateTime 을 ZoneId로 ZonedDateTime 변환할 수 있다`() {
        val ldt = localDateTimeOf(2021, 3, 1, 12, 30, 45, 123)
        val zoneId = ZoneId.of("Asia/Seoul")

        ldt.toZonedDateTime(zoneId).zone shouldBeEqualTo zoneId
    }

    @Test
    fun `LocalDate toInstant 는 UTC 자정 기준이다`() {
        val localDate = LocalDate.of(1970, 1, 1)
        localDate.toInstant().toEpochMilli() shouldBeEqualTo 0L
    }

    @Test
    fun `LocalTime toInstant 는 기본값으로 epoch date UTC를 사용한다`() {
        val localTime = LocalTime.of(1, 0, 0)
        localTime.toInstant().toEpochMilli() shouldBeEqualTo 3_600_000L
    }

    @Test
    fun `LocalTime toInstant 는 baseDate와 offset 인자를 반영한다`() {
        val localTime = LocalTime.of(0, 0, 0)
        val baseDate = LocalDate.of(1970, 1, 2)
        val offset = ZoneOffset.ofHours(9)

        localTime.toInstant(baseDate, offset).toEpochMilli() shouldBeEqualTo 54_000_000L
    }
}
