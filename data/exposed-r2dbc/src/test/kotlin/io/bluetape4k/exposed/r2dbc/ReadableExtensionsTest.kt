package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.r2dbc.spi.Readable
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.UUID
import kotlin.test.assertFailsWith

/**
 * [ReadableExtensions] 확장 함수 단위 테스트입니다.
 *
 * 인덱스/이름 기반 접근, null 처리, 타입별 변환 동작을 [FakeReadable]로 검증합니다.
 */
class ReadableExtensionsTest {

    companion object: KLoggingChannel()

    private class FakeReadable(
        private val valuesByIndex: Map<Int, Any?> = emptyMap(),
        private val valuesByName: Map<String, Any?> = emptyMap(),
    ): Readable {
        override fun <T: Any?> get(index: Int, type: Class<T>): T? {
            val value = valuesByIndex[index] ?: return null
            if (!type.isInstance(value)) return null
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override fun <T: Any?> get(name: String, type: Class<T>): T? {
            val value = valuesByName[name] ?: return null
            if (!type.isInstance(value)) return null
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override fun get(index: Int): Any? = valuesByIndex[index]
        override fun get(name: String): Any? = valuesByName[name]
    }

    // region getAs / getAsOrNull

    @Test
    fun `getAs는 인덱스 기반 값을 타입으로 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 123))
        readable.getAs<Int>(0) shouldBeEqualTo 123
    }

    @Test
    fun `getAs는 이름 기반 값을 타입으로 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("name" to "alpha"))
        readable.getAs<String>("name") shouldBeEqualTo "alpha"
    }

    @Test
    fun `getAsOrNull은 인덱스 기반 null 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getAsOrNull<Int>(0).shouldBeNull()
    }

    @Test
    fun `getAsOrNull은 이름 기반 null 값을 그대로 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("name" to null))
        readable.getAsOrNull<String>("name").shouldBeNull()
    }

    @Test
    fun `getAs는 인덱스 기반 null 값이면 상세 메시지로 예외를 던진다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(1 to null))
        val ex = assertFailsWith<IllegalStateException> { readable.getAs<String>(1) }
        val msg = ex.message.shouldNotBeNull()
        msg shouldContain "Column[1]"
        msg shouldContain "String"
    }

    @Test
    fun `getAs는 이름 기반 null 값이면 상세 메시지로 예외를 던진다`() {
        val readable = FakeReadable(valuesByName = mapOf("col" to null))
        val ex = assertFailsWith<IllegalStateException> { readable.getAs<String>("col") }
        val msg = ex.message.shouldNotBeNull()
        msg shouldContain "Column[col]"
        msg shouldContain "String"
    }

    // endregion

    // region String

    @Test
    fun `getString은 인덱스 기반 String 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to "hello"))
        readable.getString(0) shouldBeEqualTo "hello"
    }

    @Test
    fun `getString은 이름 기반 String 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("col" to "world"))
        readable.getString("col") shouldBeEqualTo "world"
    }

    @Test
    fun `getStringOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getStringOrNull(0).shouldBeNull()
    }

    @Test
    fun `getStringOrNull은 이름 기반 String 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("col" to "value"))
        readable.getStringOrNull("col") shouldBeEqualTo "value"
    }

    @Test
    fun `getStringOrNull은 이름 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("col" to null))
        readable.getStringOrNull("col").shouldBeNull()
    }

    // endregion

    // region Boolean

    @Test
    fun `getBoolean은 인덱스 기반 Boolean 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to true))
        readable.getBoolean(0) shouldBeEqualTo true
    }

    @Test
    fun `getBoolean은 이름 기반 Boolean 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("flag" to false))
        readable.getBoolean("flag") shouldBeEqualTo false
    }

    @Test
    fun `getBooleanOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getBooleanOrNull(0).shouldBeNull()
    }

    @Test
    fun `getBooleanOrNull은 이름 기반 Boolean 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("flag" to true))
        readable.getBooleanOrNull("flag") shouldBeEqualTo true
    }

    // endregion

    // region Char

    @Test
    fun `getChar은 인덱스 기반 Char 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 'A'))
        readable.getChar(0) shouldBeEqualTo 'A'
    }

    @Test
    fun `getChar은 이름 기반 Char 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("ch" to 'Z'))
        readable.getChar("ch") shouldBeEqualTo 'Z'
    }

    @Test
    fun `getCharOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getCharOrNull(0).shouldBeNull()
    }

    @Test
    fun `getCharOrNull은 이름 기반 Char 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("ch" to 'X'))
        readable.getCharOrNull("ch") shouldBeEqualTo 'X'
    }

    // endregion

    // region Byte

    @Test
    fun `getByte는 인덱스 기반 Byte 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 42.toByte()))
        readable.getByte(0) shouldBeEqualTo 42.toByte()
    }

    @Test
    fun `getByte는 이름 기반 Byte 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("b" to 7.toByte()))
        readable.getByte("b") shouldBeEqualTo 7.toByte()
    }

    @Test
    fun `getByteOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getByteOrNull(0).shouldBeNull()
    }

    @Test
    fun `getByteOrNull은 이름 기반 Byte 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("b" to 1.toByte()))
        readable.getByteOrNull("b") shouldBeEqualTo 1.toByte()
    }

    // endregion

    // region Short

    @Test
    fun `getShort는 인덱스 기반 Short 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 100.toShort()))
        readable.getShort(0) shouldBeEqualTo 100.toShort()
    }

    @Test
    fun `getShort는 이름 기반 Short 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("s" to 200.toShort()))
        readable.getShort("s") shouldBeEqualTo 200.toShort()
    }

    @Test
    fun `getShortOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getShortOrNull(0).shouldBeNull()
    }

    @Test
    fun `getShortOrNull은 이름 기반 Short 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("s" to 300.toShort()))
        readable.getShortOrNull("s") shouldBeEqualTo 300.toShort()
    }

    // endregion

    // region Int

    @Test
    fun `getInt는 인덱스 기반 Int 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 42))
        readable.getInt(0) shouldBeEqualTo 42
    }

    @Test
    fun `getInt는 이름 기반 Int 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("num" to 99))
        readable.getInt("num") shouldBeEqualTo 99
    }

    @Test
    fun `getIntOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getIntOrNull(0).shouldBeNull()
    }

    @Test
    fun `getIntOrNull은 이름 기반 Int 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("num" to 123))
        readable.getIntOrNull("num") shouldBeEqualTo 123
    }

    // endregion

    // region Long

    @Test
    fun `getLong은 인덱스 기반 Long 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 1000L))
        readable.getLong(0) shouldBeEqualTo 1000L
    }

    @Test
    fun `getLong은 이름 기반 Long 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("id" to 9999L))
        readable.getLong("id") shouldBeEqualTo 9999L
    }

    @Test
    fun `getLongOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getLongOrNull(0).shouldBeNull()
    }

    @Test
    fun `getLongOrNull은 이름 기반 Long 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("id" to 5000L))
        readable.getLongOrNull("id") shouldBeEqualTo 5000L
    }

    // endregion

    // region Float

    @Test
    fun `getFloat는 인덱스 기반 Float 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 3.14f))
        readable.getFloat(0) shouldBeEqualTo 3.14f
    }

    @Test
    fun `getFloat는 이름 기반 Float 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("f" to 1.5f))
        readable.getFloat("f") shouldBeEqualTo 1.5f
    }

    @Test
    fun `getFloatOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getFloatOrNull(0).shouldBeNull()
    }

    @Test
    fun `getFloatOrNull은 이름 기반 Float 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("f" to 2.5f))
        readable.getFloatOrNull("f") shouldBeEqualTo 2.5f
    }

    // endregion

    // region Double

    @Test
    fun `getDouble은 인덱스 기반 Double 값을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 2.718))
        readable.getDouble(0) shouldBeEqualTo 2.718
    }

    @Test
    fun `getDouble은 이름 기반 Double 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("d" to 1.414))
        readable.getDouble("d") shouldBeEqualTo 1.414
    }

    @Test
    fun `getDoubleOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getDoubleOrNull(0).shouldBeNull()
    }

    @Test
    fun `getDoubleOrNull은 이름 기반 Double 값을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("d" to 0.577))
        readable.getDoubleOrNull("d") shouldBeEqualTo 0.577
    }

    // endregion

    // region BigDecimal

    @Test
    fun `getBigDecimal은 인덱스 기반 BigDecimal 값을 반환한다`() {
        val value = BigDecimal("12345.678")
        val readable = FakeReadable(valuesByIndex = mapOf(0 to value))
        readable.getBigDecimal(0) shouldBeEqualTo value
    }

    @Test
    fun `getBigDecimal은 이름 기반 BigDecimal 값을 반환한다`() {
        val value = BigDecimal("99.99")
        val readable = FakeReadable(valuesByName = mapOf("price" to value))
        readable.getBigDecimal("price") shouldBeEqualTo value
    }

    @Test
    fun `getBigDecimalOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getBigDecimalOrNull(0).shouldBeNull()
    }

    @Test
    fun `getBigDecimalOrNull은 이름 기반 BigDecimal 값을 반환한다`() {
        val value = BigDecimal("1.0")
        val readable = FakeReadable(valuesByName = mapOf("amount" to value))
        readable.getBigDecimalOrNull("amount") shouldBeEqualTo value
    }

    // endregion

    // region ByteArray

    @Test
    fun `getByteArray는 인덱스 기반 ByteArray 값을 반환한다`() {
        val bytes = byteArrayOf(1, 2, 3)
        val readable = FakeReadable(valuesByIndex = mapOf(0 to bytes))
        readable.getByteArray(0).toList() shouldBeEqualTo bytes.toList()
    }

    @Test
    fun `getByteArray는 이름 기반 ByteArray 값을 반환한다`() {
        val bytes = byteArrayOf(10, 20)
        val readable = FakeReadable(valuesByName = mapOf("data" to bytes))
        readable.getByteArray("data").toList() shouldBeEqualTo bytes.toList()
    }

    @Test
    fun `getByteArrayOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getByteArrayOrNull(0).shouldBeNull()
    }

    @Test
    fun `getByteArrayOrNull은 이름 기반 ByteArray 값을 반환한다`() {
        val bytes = byteArrayOf(5, 6, 7)
        val readable = FakeReadable(valuesByName = mapOf("data" to bytes))
        readable.getByteArrayOrNull("data")?.toList() shouldBeEqualTo bytes.toList()
    }

    // endregion

    // region Date / Timestamp / Instant

    @Test
    fun `getDate는 인덱스 기반 Date 값을 반환한다`() {
        val date = Date(0L)
        val readable = FakeReadable(valuesByIndex = mapOf(0 to date))
        readable.getDate(0) shouldBeEqualTo date
    }

    @Test
    fun `getDate는 이름 기반 Date 값을 반환한다`() {
        val date = Date(1_000L)
        val readable = FakeReadable(valuesByName = mapOf("dt" to date))
        readable.getDate("dt") shouldBeEqualTo date
    }

    @Test
    fun `getDateOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getDateOrNull(0).shouldBeNull()
    }

    @Test
    fun `getDateOrNull은 이름 기반 Date 값을 반환한다`() {
        val date = Date(2_000L)
        val readable = FakeReadable(valuesByName = mapOf("dt" to date))
        readable.getDateOrNull("dt") shouldBeEqualTo date
    }

    @Test
    fun `getTimestamp는 인덱스 기반 Timestamp 값을 반환한다`() {
        val ts = Timestamp(System.currentTimeMillis())
        val readable = FakeReadable(valuesByIndex = mapOf(0 to ts))
        readable.getTimestamp(0) shouldBeEqualTo ts
    }

    @Test
    fun `getTimestamp는 이름 기반 Timestamp 값을 반환한다`() {
        val ts = Timestamp(0L)
        val readable = FakeReadable(valuesByName = mapOf("ts" to ts))
        readable.getTimestamp("ts") shouldBeEqualTo ts
    }

    @Test
    fun `getTimestampOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getTimestampOrNull(0).shouldBeNull()
    }

    @Test
    fun `getTimestampOrNull은 이름 기반 Timestamp 값을 반환한다`() {
        val ts = Timestamp(1000L)
        val readable = FakeReadable(valuesByName = mapOf("ts" to ts))
        readable.getTimestampOrNull("ts") shouldBeEqualTo ts
    }

    @Test
    fun `getInstant는 인덱스 기반 Instant 값을 반환한다`() {
        val instant = Instant.ofEpochSecond(1_000_000L)
        val readable = FakeReadable(valuesByIndex = mapOf(0 to instant))
        readable.getInstant(0) shouldBeEqualTo instant
    }

    @Test
    fun `getInstant는 이름 기반 Instant 값을 반환한다`() {
        val instant = Instant.EPOCH
        val readable = FakeReadable(valuesByName = mapOf("ts" to instant))
        readable.getInstant("ts") shouldBeEqualTo instant
    }

    @Test
    fun `getInstantOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getInstantOrNull(0).shouldBeNull()
    }

    @Test
    fun `getInstantOrNull은 이름 기반 Instant 값을 반환한다`() {
        val instant = Instant.now()
        val readable = FakeReadable(valuesByName = mapOf("ts" to instant))
        readable.getInstantOrNull("ts") shouldBeEqualTo instant
    }

    // endregion

    // region LocalDate / LocalTime / LocalDateTime / OffsetDateTime

    @Test
    fun `getLocalDate는 인덱스 기반 LocalDate 값을 반환한다`() {
        val date = LocalDate.of(2026, 4, 27)
        val readable = FakeReadable(valuesByIndex = mapOf(0 to date))
        readable.getLocalDate(0) shouldBeEqualTo date
    }

    @Test
    fun `getLocalDate는 이름 기반 LocalDate 값을 반환한다`() {
        val date = LocalDate.of(2000, 1, 1)
        val readable = FakeReadable(valuesByName = mapOf("d" to date))
        readable.getLocalDate("d") shouldBeEqualTo date
    }

    @Test
    fun `getLocalDateOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getLocalDateOrNull(0).shouldBeNull()
    }

    @Test
    fun `getLocalDateOrNull은 이름 기반 LocalDate 값을 반환한다`() {
        val date = LocalDate.of(2024, 6, 15)
        val readable = FakeReadable(valuesByName = mapOf("d" to date))
        readable.getLocalDateOrNull("d") shouldBeEqualTo date
    }

    @Test
    fun `getLocalTime은 인덱스 기반 LocalTime 값을 반환한다`() {
        val time = LocalTime.of(12, 30, 0)
        val readable = FakeReadable(valuesByIndex = mapOf(0 to time))
        readable.getLocalTime(0) shouldBeEqualTo time
    }

    @Test
    fun `getLocalTime은 이름 기반 LocalTime 값을 반환한다`() {
        val time = LocalTime.MIDNIGHT
        val readable = FakeReadable(valuesByName = mapOf("t" to time))
        readable.getLocalTime("t") shouldBeEqualTo time
    }

    @Test
    fun `getLocalTimeOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getLocalTimeOrNull(0).shouldBeNull()
    }

    @Test
    fun `getLocalTimeOrNull은 이름 기반 LocalTime 값을 반환한다`() {
        val time = LocalTime.NOON
        val readable = FakeReadable(valuesByName = mapOf("t" to time))
        readable.getLocalTimeOrNull("t") shouldBeEqualTo time
    }

    @Test
    fun `getLocalDateTime은 인덱스 기반 LocalDateTime 값을 반환한다`() {
        val dt = LocalDateTime.of(2026, 4, 27, 10, 0, 0)
        val readable = FakeReadable(valuesByIndex = mapOf(0 to dt))
        readable.getLocalDateTime(0) shouldBeEqualTo dt
    }

    @Test
    fun `getLocalDateTime은 이름 기반 LocalDateTime 값을 반환한다`() {
        val dt = LocalDateTime.of(2000, 1, 1, 0, 0, 0)
        val readable = FakeReadable(valuesByName = mapOf("dt" to dt))
        readable.getLocalDateTime("dt") shouldBeEqualTo dt
    }

    @Test
    fun `getLocalDateTimeOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getLocalDateTimeOrNull(0).shouldBeNull()
    }

    @Test
    fun `getLocalDateTimeOrNull은 이름 기반 LocalDateTime 값을 반환한다`() {
        val dt = LocalDateTime.now()
        val readable = FakeReadable(valuesByName = mapOf("dt" to dt))
        readable.getLocalDateTimeOrNull("dt") shouldBeEqualTo dt
    }

    @Test
    fun `getOffsetDateTime은 인덱스 기반 OffsetDateTime 값을 반환한다`() {
        val odt = OffsetDateTime.of(2026, 4, 27, 9, 0, 0, 0, ZoneOffset.UTC)
        val readable = FakeReadable(valuesByIndex = mapOf(0 to odt))
        readable.getOffsetDateTime(0) shouldBeEqualTo odt
    }

    @Test
    fun `getOffsetDateTime은 이름 기반 OffsetDateTime 값을 반환한다`() {
        val odt = OffsetDateTime.of(2000, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(9))
        val readable = FakeReadable(valuesByName = mapOf("odt" to odt))
        readable.getOffsetDateTime("odt") shouldBeEqualTo odt
    }

    @Test
    fun `getOffsetDateTimeOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getOffsetDateTimeOrNull(0).shouldBeNull()
    }

    @Test
    fun `getOffsetDateTimeOrNull은 이름 기반 OffsetDateTime 값을 반환한다`() {
        val odt = OffsetDateTime.now(ZoneOffset.UTC)
        val readable = FakeReadable(valuesByName = mapOf("odt" to odt))
        readable.getOffsetDateTimeOrNull("odt") shouldBeEqualTo odt
    }

    // endregion

    // region UUID

    @Test
    fun `getUuidOrNull은 인덱스 기반 UUID 값을 반환한다`() {
        val uuid = UUID.randomUUID()
        val readable = FakeReadable(valuesByIndex = mapOf(0 to uuid))
        readable.getUuidOrNull(0) shouldBeEqualTo uuid
    }

    @Test
    fun `getUuidOrNull은 인덱스 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        readable.getUuidOrNull(0).shouldBeNull()
    }

    @Test
    fun `getUuidOrNull은 이름 기반 UUID 값을 반환한다`() {
        val uuid = UUID.randomUUID()
        val readable = FakeReadable(valuesByName = mapOf("id" to uuid))
        readable.getUuidOrNull("id") shouldBeEqualTo uuid
    }

    @Test
    fun `getUuidOrNull은 이름 기반 null 이면 null 을 반환한다`() {
        val readable = FakeReadable(valuesByName = mapOf("id" to null))
        readable.getUuidOrNull("id").shouldBeNull()
    }

    @Test
    fun `getUuid는 인덱스 기반 UUID 값을 반환한다`() {
        val uuid = UUID.randomUUID()
        val readable = FakeReadable(valuesByIndex = mapOf(0 to uuid))
        readable.getUuid(0) shouldBeEqualTo uuid
    }

    @Test
    fun `getUuid는 이름 기반 UUID 값을 반환한다`() {
        val uuid = UUID.randomUUID()
        val readable = FakeReadable(valuesByName = mapOf("uid" to uuid))
        readable.getUuid("uid") shouldBeEqualTo uuid
    }

    @Test
    fun `getUuid는 인덱스 기반 null이면 예외를 던진다`() {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        val ex = assertFailsWith<IllegalStateException> { readable.getUuid(0) }
        val msg = ex.message.shouldNotBeNull()
        msg shouldContain "Column[0]"
    }

    @Test
    fun `getUuid는 이름 기반 null이면 예외를 던진다`() {
        val readable = FakeReadable(valuesByName = mapOf("uid" to null))
        val ex = assertFailsWith<IllegalStateException> { readable.getUuid("uid") }
        val msg = ex.message.shouldNotBeNull()
        msg shouldContain "Column[uid]"
    }

    // endregion

    // region ExposedBlob

    @Test
    fun `getExposedBlob은 byte array를 ExposedBlob으로 변환한다`() = runTest {
        val bytes = "blob-value".toByteArray()
        val readable = FakeReadable(valuesByName = mapOf("blob" to bytes))

        val blob = readable.getExposedBlob("blob")
        blob.bytes.toList() shouldBeEqualTo bytes.toList()
    }

    @Test
    fun `getExposedBlobOrNull은 byte buffer를 변환하고 원본 position을 보존한다`() = runTest {
        val buffer = ByteBuffer.wrap("abcdef".toByteArray()).apply { position(2) }
        val readable = FakeReadable(valuesByName = mapOf("blob" to buffer))

        val blob = readable.getExposedBlobOrNull("blob")
        blob.shouldNotBeNull()
        blob.bytes.toList() shouldBeEqualTo "cdef".toByteArray().toList()
        buffer.position() shouldBeEqualTo 2
    }

    @Test
    fun `getExposedBlobOrNull은 지원하지 않는 타입이면 null을 반환한다`() = runTest {
        val readable = FakeReadable(valuesByName = mapOf("blob" to 123))
        readable.getExposedBlobOrNull("blob").shouldBeNull()
    }

    @Test
    fun `getExposedBlob은 이름 기반 미지원 타입일 때 예외를 던진다`() = runTest {
        val readable = FakeReadable(valuesByName = mapOf("blob" to 123))
        val ex = assertFailsWith<IllegalStateException> { readable.getExposedBlob("blob") }
        val msg = ex.message.shouldNotBeNull()
        msg shouldContain "Column[blob]"
        msg shouldContain "unsupported blob value type"
    }

    @Test
    fun `getExposedBlobOrNull은 인덱스 기반 byte array를 변환한다`() = runTest {
        val bytes = "index-blob".toByteArray()
        val readable = FakeReadable(valuesByIndex = mapOf(0 to bytes))

        val blob = readable.getExposedBlobOrNull(0)
        blob.shouldNotBeNull()
        blob.bytes.toList() shouldBeEqualTo bytes.toList()
    }

    @Test
    fun `getExposedBlob은 인덱스 기반 미지원 타입일 때 예외를 던진다`() = runTest {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to 42))
        val ex = assertFailsWith<IllegalStateException> { readable.getExposedBlob(0) }
        val msg = ex.message.shouldNotBeNull()
        msg shouldContain "Column[0]"
        msg shouldContain "unsupported blob value type"
    }

    @Test
    fun `getExposedBlob은 인덱스 기반 null일 때 예외를 던진다`() = runTest {
        val readable = FakeReadable(valuesByIndex = mapOf(0 to null))
        val ex = assertFailsWith<IllegalStateException> { readable.getExposedBlob(0) }
        val msg = ex.message.shouldNotBeNull()
        msg shouldContain "Column[0]"
        msg shouldContain "unsupported blob value type"
    }

    @Test
    fun `getExposedBlob은 이름 기반 null일 때 예외를 던진다`() = runTest {
        val readable = FakeReadable(valuesByName = mapOf("col" to null))
        val ex = assertFailsWith<IllegalStateException> { readable.getExposedBlob("col") }
        val msg = ex.message.shouldNotBeNull()
        msg shouldContain "Column[col]"
        msg shouldContain "unsupported blob value type"
    }

    // endregion
}
