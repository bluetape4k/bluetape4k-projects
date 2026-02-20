package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class MapSupportTest {
    @Test
    fun `boolean 값을 조회한다`() {
        val map = mapOf("active" to true, "inactive" to false, "nullable" to null)

        map.boolean("active") shouldBeEqualTo true
        map.boolean("inactive") shouldBeEqualTo false
        map.booleanOrNull("active") shouldBeEqualTo true
        map.booleanOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `int 값을 조회한다`() {
        val map = mapOf("count" to 42, "nullable" to null)

        map.int("count") shouldBeEqualTo 42
        map.intOrNull("count") shouldBeEqualTo 42
        map.intOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `long 값을 조회한다`() {
        val map = mapOf("id" to 123456789L, "nullable" to null)

        map.long("id") shouldBeEqualTo 123456789L
        map.longOrNull("id") shouldBeEqualTo 123456789L
        map.longOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `string 값을 조회한다`() {
        val map = mapOf("name" to "John Doe", "nullable" to null)

        map.string("name") shouldBeEqualTo "John Doe"
        map.stringOrNull("name") shouldBeEqualTo "John Doe"
        map.stringOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `bigDecimal 값을 조회한다`() {
        val map = mapOf("price" to BigDecimal("12345.67"), "nullable" to null)

        map.bigDecimal("price") shouldBeEqualTo BigDecimal("12345.67")
        map.bigDecimalOrNull("price") shouldBeEqualTo BigDecimal("12345.67")
        map.bigDecimalOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `uuid 값을 조회한다`() {
        val uuid = UUID.randomUUID()
        val map = mapOf("id" to uuid, "nullable" to null)

        map.uuid("id") shouldBeEqualTo uuid
        map.uuidOrNull("id") shouldBeEqualTo uuid
        map.uuidOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `instant 값을 조회한다`() {
        val instant = Instant.now()
        val map = mapOf("created_at" to instant, "nullable" to null)

        map.instant("created_at") shouldBeEqualTo instant
        map.instantOrNull("created_at") shouldBeEqualTo instant
        map.instantOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `localDate 값을 조회한다`() {
        val date = LocalDate.of(2026, 2, 19)
        val map = mapOf("birth_date" to date, "nullable" to null)

        map.localDate("birth_date") shouldBeEqualTo date
        map.localDateOrNull("birth_date") shouldBeEqualTo date
        map.localDateOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `localTime 값을 조회한다`() {
        val time = LocalTime.of(10, 30, 45)
        val map = mapOf("start_time" to time, "nullable" to null)

        map.localTime("start_time") shouldBeEqualTo time
        map.localTimeOrNull("start_time") shouldBeEqualTo time
        map.localTimeOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `localDateTime 값을 조회한다`() {
        val dateTime = LocalDateTime.of(2026, 2, 19, 10, 30, 45)
        val map = mapOf("event_time" to dateTime, "nullable" to null)

        map.localDateTime("event_time") shouldBeEqualTo dateTime
        map.localDateTimeOrNull("event_time") shouldBeEqualTo dateTime
        map.localDateTimeOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `offsetDateTime 값을 조회한다`() {
        val dateTime = OffsetDateTime.of(2026, 2, 19, 10, 30, 45, 0, ZoneOffset.ofHours(9))
        val map = mapOf("created_at" to dateTime, "nullable" to null)

        map.offsetDateTime("created_at") shouldBeEqualTo dateTime
        map.offsetDateTimeOrNull("created_at") shouldBeEqualTo dateTime
        map.offsetDateTimeOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `byteArray 값을 조회한다`() {
        val bytes = kotlin.byteArrayOf(1, 2, 3, 4, 5)
        val map = mapOf("data" to bytes, "nullable" to null)

        map.byteArray("data").shouldNotBeNull()
        map.byteArrayOrNull("data").shouldNotBeNull()
        map.byteArrayOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `byte 값을 조회한다`() {
        val map = mapOf("flag" to 0x01.toByte(), "nullable" to null)

        map.byte("flag") shouldBeEqualTo 0x01.toByte()
        map.byteOrNull("flag") shouldBeEqualTo 0x01.toByte()
        map.byteOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `short 값을 조회한다`() {
        val map = mapOf("code" to 1000.toShort(), "nullable" to null)

        map.short("code") shouldBeEqualTo 1000.toShort()
        map.shortOrNull("code") shouldBeEqualTo 1000.toShort()
        map.shortOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `char 값을 조회한다`() {
        val map = mapOf("initial" to 'A', "nullable" to null)

        map.char("initial") shouldBeEqualTo 'A'
        map.charOrNull("initial") shouldBeEqualTo 'A'
        map.charOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `date 값을 조회한다`() {
        val date = Date()
        val map = mapOf("created" to date, "nullable" to null)

        map.date("created") shouldBeEqualTo date
        map.dateOrNull("created") shouldBeEqualTo date
        map.dateOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `timestamp 값을 조회한다`() {
        val timestamp = Timestamp(System.currentTimeMillis())
        val map = mapOf("ts" to timestamp, "nullable" to null)

        map.timestamp("ts") shouldBeEqualTo timestamp
        map.timestampOrNull("ts") shouldBeEqualTo timestamp
        map.timestampOrNull("nullable").shouldBeNull()
    }

    @Test
    fun `존재하지 않는 key는 예외를 던진다`() {
        val map = mapOf("id" to 1)

        val exception = assertThrows<IllegalArgumentException> {
            map.int("missing")
        }

        exception.message shouldBeEqualTo "Map[missing] is missing or null."
    }
}
