package io.bluetape4k.protobuf

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DateTimeSupportTest {
    companion object: KLogging()

    // ── LocalDate <-> ProtoDate ────────────────────────────────────────────

    @Test
    fun `toLocalDate - ProtoDate를 LocalDate로 변환한다`() {
        val protoDate = ProtoDate.newBuilder()
            .setYear(2024)
            .setMonth(3)
            .setDay(15)
            .build()

        val localDate = protoDate.toLocalDate()

        localDate.shouldNotBeNull()
        localDate.year shouldBeEqualTo 2024
        localDate.monthValue shouldBeEqualTo 3
        localDate.dayOfMonth shouldBeEqualTo 15
    }

    @Test
    fun `toProtoDate - LocalDate를 ProtoDate로 변환한다`() {
        val localDate = LocalDate.of(2024, 1, 2)

        val protoDate = localDate.toProtoDate()

        protoDate.shouldNotBeNull()
        protoDate.year shouldBeEqualTo 2024
        protoDate.month shouldBeEqualTo 1
        protoDate.day shouldBeEqualTo 2
    }

    @Test
    fun `toLocalDate 왕복 변환 - ProtoDate → LocalDate → ProtoDate`() {
        val original = ProtoDate.newBuilder()
            .setYear(2025)
            .setMonth(12)
            .setDay(31)
            .build()

        val restored = original.toLocalDate().toProtoDate()

        restored.year shouldBeEqualTo original.year
        restored.month shouldBeEqualTo original.month
        restored.day shouldBeEqualTo original.day
    }

    @Test
    fun `toProtoDate 왕복 변환 - LocalDate → ProtoDate → LocalDate`() {
        val original = LocalDate.of(2023, 6, 20)

        val restored = original.toProtoDate().toLocalDate()

        restored shouldBeEqualTo original
    }

    // ── LocalTime <-> ProtoTime ────────────────────────────────────────────

    @Test
    fun `toLocalTime - ProtoTime을 LocalTime으로 변환한다`() {
        val protoTime = ProtoTime.newBuilder()
            .setHours(10)
            .setMinutes(30)
            .setSeconds(45)
            .setNanos(123_456_789)
            .build()

        val localTime = protoTime.toLocalTime()

        localTime.shouldNotBeNull()
        localTime.hour shouldBeEqualTo 10
        localTime.minute shouldBeEqualTo 30
        localTime.second shouldBeEqualTo 45
        localTime.nano shouldBeEqualTo 123_456_789
    }

    @Test
    fun `toProtoTime - LocalTime을 ProtoTime으로 변환한다`() {
        val localTime = LocalTime.of(10, 11, 12, 999)

        val protoTime = localTime.toProtoTime()

        protoTime.shouldNotBeNull()
        protoTime.hours shouldBeEqualTo 10
        protoTime.minutes shouldBeEqualTo 11
        protoTime.seconds shouldBeEqualTo 12
        protoTime.nanos shouldBeEqualTo 999
    }

    @Test
    fun `toLocalTime 왕복 변환 - LocalTime → ProtoTime → LocalTime`() {
        val original = LocalTime.of(8, 15, 30, 0)

        val restored = original.toProtoTime().toLocalTime()

        restored shouldBeEqualTo original
    }

    // ── LocalDateTime <-> ProtoDateTime ───────────────────────────────────

    @Test
    fun `toLocalDateTime - ProtoDateTime을 LocalDateTime으로 변환한다`() {
        val protoDateTime = ProtoDateTime.newBuilder()
            .setYear(2024)
            .setMonth(7)
            .setDay(4)
            .setHours(12)
            .setMinutes(30)
            .setSeconds(0)
            .setNanos(0)
            .build()

        val localDateTime = protoDateTime.toLocalDateTime()

        localDateTime.shouldNotBeNull()
        localDateTime.year shouldBeEqualTo 2024
        localDateTime.monthValue shouldBeEqualTo 7
        localDateTime.dayOfMonth shouldBeEqualTo 4
        localDateTime.hour shouldBeEqualTo 12
        localDateTime.minute shouldBeEqualTo 30
        localDateTime.second shouldBeEqualTo 0
    }

    @Test
    fun `toProtoDateTime - LocalDateTime을 ProtoDateTime으로 변환한다`() {
        val localDateTime = LocalDateTime.of(2024, 1, 2, 3, 4, 5, 6)

        val protoDateTime = localDateTime.toProtoDateTime()

        protoDateTime.shouldNotBeNull()
        protoDateTime.year shouldBeEqualTo 2024
        protoDateTime.month shouldBeEqualTo 1
        protoDateTime.day shouldBeEqualTo 2
        protoDateTime.hours shouldBeEqualTo 3
        protoDateTime.minutes shouldBeEqualTo 4
        protoDateTime.seconds shouldBeEqualTo 5
        protoDateTime.nanos shouldBeEqualTo 6
    }

    @Test
    fun `toLocalDateTime 왕복 변환 - LocalDateTime → ProtoDateTime → LocalDateTime`() {
        val original = LocalDateTime.of(2023, 11, 15, 9, 0, 0, 0)

        val restored = original.toProtoDateTime().toLocalDateTime()

        restored shouldBeEqualTo original
    }

    @Test
    fun `경계값 - 자정 변환이 올바르다`() {
        val midnight = LocalDateTime.of(2024, 1, 1, 0, 0, 0, 0)
        val proto = midnight.toProtoDateTime()
        val restored = proto.toLocalDateTime()
        restored shouldBeEqualTo midnight
    }
}
