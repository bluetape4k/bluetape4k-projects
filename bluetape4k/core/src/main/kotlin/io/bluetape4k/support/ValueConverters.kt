package io.bluetape4k.support

import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.Charset
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAccessor
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * 객체를 Double 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "1.0".asDouble() shouldBeEqualTo 1.0
 * "1".asDouble() shouldBeEqualTo 1.0
 * "".asDouble() shouldBeEqualTo 0.0
 * ```
 * @param defaultValue 변환 실패 시 대체 값
 */
fun Any?.asDouble(defaultValue: Double = 0.0): Double = asDoubleOrNull() ?: defaultValue

/**
 * 객체를 Double 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "1.0".asDoubleOrNull() shouldBeEqualTo 1.0
 * "1".asDoubleOrNull() shouldBeEqualTo 1.0
 * "".asDoubleOrNull() shouldBeEqualTo null
 * ```
 */
fun Any?.asDoubleOrNull(): Double? = runCatching {
    when (this) {
        null            -> null
        is Double       -> this
        is Number       -> this.toDouble()
        is CharSequence -> this.toString().toDouble()
        else            -> this.toString().parseNumber()
    }
}.getOrNull()

/**
 * 객체를 Float 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "1.0".asFloat() shouldBeEqualTo 1.0F
 * "1".asFloat() shouldBeEqualTo 1.0F
 * "".asFloat() shouldBeEqualTo 0.0F
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 */
fun Any?.asFloat(defaultValue: Float = 0.0F): Float = asFloatOrNull() ?: defaultValue

/**
 * 객체를 Float 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "1.0".asFloatOrNull() shouldBeEqualTo 1.0F
 * "1".asFloatOrNull() shouldBeEqualTo 1.0F
 * "".asFloatOrNull() shouldBeEqualTo null
 * ```
 */
fun Any?.asFloatOrNull(): Float? = runCatching {
    when (this) {
        null            -> null
        is Float        -> this
        is Number       -> this.toFloat()
        is CharSequence -> this.toString().parseNumber()
        else            -> this.asDoubleOrNull()?.toFloat()
    }
}.getOrNull()

/**
 * 객체를 Long 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "1".asLong() shouldBeEqualTo 1L
 * "1.0".asLong() shouldBeEqualTo 1L
 * "".asLong() shouldBeEqualTo 0L
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 */
fun Any?.asLong(defaultValue: Long = 0L): Long = asLongOrNull() ?: defaultValue

/**
 * 객체를 Long 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "1".asLongOrNull() shouldBeEqualTo 1L
 * "1.0".asLongOrNull() shouldBeEqualTo 1L
 * "".asLongOrNull() shouldBeEqualTo null
 * ```
 */
fun Any?.asLongOrNull(): Long? = runCatching {
    when (this) {
        null            -> null
        is Long         -> this
        is Number       -> this.toLong()
        is CharSequence -> this.toString().toLong()
        else            -> this.asBigDecimalOrNull()?.toLong()
    }
}.getOrNull()

/**
 * 객체를 Int 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "1".asInt() shouldBeEqualTo 1
 * "1.0".asInt() shouldBeEqualTo 1
 * "".asInt() shouldBeEqualTo 0
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return Int 변환 결과
 */
fun Any?.asInt(defaultValue: Int = 0): Int = asIntOrNull() ?: defaultValue

/**
 * 객체를 Int 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "1".asIntOrNull() shouldBeEqualTo 1
 * "1.0".asIntOrNull() shouldBeEqualTo 1
 * "".asIntOrNull() shouldBeEqualTo null
 * ```
 */
fun Any?.asIntOrNull(): Int? = runCatching {
    when (this) {
        null      -> null
        is Int    -> this
        is Number -> this.toInt()
        else      -> this.asLongOrNull()?.toInt()
    }
}.getOrNull()

/**
 * 객체를 Short 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "1".asShort() shouldBeEqualTo 1.toShort()
 * "1.0".asShort() shouldBeEqualTo 1.toShort()
 * "".asShort() shouldBeEqualTo 0.toShort()
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return Short 변환 결과
 */
fun Any?.asShort(defaultValue: Short = 0): Short = asShortOrNull() ?: defaultValue

/**
 * 객체를 Short 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "1".asShortOrNull() shouldBeEqualTo 1.toShort()
 * "1.0".asShortOrNull() shouldBeEqualTo 1.toShort()
 * "".asShortOrNull() shouldBeEqualTo null
 * ```
 */
fun Any?.asShortOrNull(): Short? = runCatching {
    when (this) {
        null      -> null
        is Short  -> this
        is Number -> this.toShort()
        else      -> this.asLongOrNull()?.toShort()
    }
}.getOrNull()

/**
 * 객체를 Byte 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "1".asByte() shouldBeEqualTo 1.toByte()
 * "1.0".asByte() shouldBeEqualTo 1.toByte()
 * "".asByte() shouldBeEqualTo 0.toByte()
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return Byte 변환 결과
 */
fun Any?.asByte(defaultValue: Byte = 0.toByte()): Byte = asByteOrNull() ?: defaultValue

/**
 * 객체를 Byte 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "1".asByteOrNull() shouldBeEqualTo 1.toByte()
 * "1.0".asByteOrNull() shouldBeEqualTo 1.toByte()
 * "".asByteOrNull() shouldBeEqualTo null
 * ```
 */
fun Any?.asByteOrNull(): Byte? = runCatching {
    when (this) {
        null      -> null
        is Char   -> this.code.toByte()
        is Byte   -> this
        is Number -> this.toByte()
        else      -> this.asLongOrNull()?.toByte()
    }
}.getOrNull()

/**
 * 객체를 Char 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "a".asChar() shouldBeEqualTo 'a'
 * "1".asChar() shouldBeEqualTo '1'
 * "".asChar() shouldBeEqualTo 0.toChar()
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return Char 변환 결과
 */
fun Any?.asChar(defaultValue: Char = 0.toChar()): Char = asCharOrNull() ?: defaultValue

/**
 * 객체를 Char 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "a".asCharOrNull() shouldBeEqualTo 'a'
 * "1".asCharOrNull() shouldBeEqualTo '1'
 * "".asCharOrNull() shouldBeEqualTo null
 * ```
 */
fun Any?.asCharOrNull(): Char? = runCatching {
    when (this) {
        null            -> null
        is Char         -> this
        is Byte         -> this.toInt().toChar()
        is CharSequence -> if (this.length == 1) first() else asIntOrNull()?.toChar()
        else            -> asIntOrNull()?.toChar()
    }
}.getOrNull()

/**
 * 객체를 Boolean 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "true".asBoolean() shouldBeEqualTo true
 * "false".asBoolean() shouldBeEqualTo false
 * "1".asBoolean() shouldBeEqualTo true
 * "0".asBoolean() shouldBeEqualTo false
 * "y".asBoolean() shouldBeEqualTo true
 * "n".asBoolean() shouldBeEqualTo false
 * "".asBoolean() shouldBeEqualTo false
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return Boolean 변환 결과
 */
fun Any?.asBoolean(defaultValue: Boolean = false): Boolean = asBooleanOrNull() ?: defaultValue

/**
 * 객체를 Boolean 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "true".asBoolean() shouldBeEqualTo true
 * "false".asBoolean() shouldBeEqualTo false
 * "1".asBoolean() shouldBeEqualTo true
 * "0".asBoolean() shouldBeEqualTo false
 * "y".asBoolean() shouldBeEqualTo true
 * "n".asBoolean() shouldBeEqualTo false
 * "".asBoolean() shouldBeEqualTo null
 * ```
 *
 * @return Boolean 변환 결과
 */
fun Any?.asBooleanOrNull(): Boolean? = runCatching {
    when (this) {
        null       -> null
        is Boolean -> this
        is Number  -> this.toInt() != 0
        is Char    -> this == 'y' || this == 'Y'
        else       -> this.toString().toBoolean()
    }
}.getOrNull()

/**
 * 객체를 [BigDecimal] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "1.0".asBigDecimal() shouldBeEqualTo BigDecimal("1.0")
 * "1".asBigDecimal() shouldBeEqualTo BigDecimal("1")
 * "".asBigDecimal() shouldBeEqualTo BigDecimal.ZERO
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return BigDecimal 변환 결과
 */
fun Any?.asBigDecimal(defaultValue: BigDecimal = BigDecimal.ZERO): BigDecimal = asBigDecimalOrNull() ?: defaultValue

/**
 * 객체를 [BigDecimal] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "1.0".asBigDecimalOrNull() shouldBeEqualTo BigDecimal("1.0")
 * "1".asBigDecimalOrNull() shouldBeEqualTo BigDecimal("1")
 * "".asBigDecimalOrNull() shouldBeEqualTo null
 * ```
 *
 * @return BigDecimal 변환 결과
 */
fun Any?.asBigDecimalOrNull(): BigDecimal? = runCatching {
    when (this) {
        null          -> null
        is BigDecimal -> this
        is Number     -> BigDecimal.valueOf(this.toDouble())
        else          -> BigDecimal(toString())
    }
}.getOrNull()

/**
 * 객체를 [BigInteger] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "1".asBigInt() shouldBeEqualTo BigInteger.ONE
 * "1.0".asBigInt() shouldBeEqualTo BigInteger.ONE
 * "".asBigInt() shouldBeEqualTo BigInteger.ZERO
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return BigInteger 변환 결과
 */
fun Any?.asBigInt(defaultValue: BigInteger = BigInteger.ZERO): BigInteger = asBigIntOrNull() ?: defaultValue

/**
 * 객체를 [BigInteger] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "1".asBigIntOrNull() shouldBeEqualTo BigInteger.ONE
 * "1.0".asBigIntOrNull() shouldBeEqualTo BigInteger.ONE
 * "".asBigIntOrNull() shouldBeEqualTo null
 * ```
 *
 * @return BigInteger 변환 결과
 */
fun Any?.asBigIntOrNull(): BigInteger? = runCatching {
    when (this) {
        null          -> null
        is BigInteger -> this
        is Number     -> BigInteger.valueOf(this.toLong())
        else          -> BigInteger(toString())
    }
}.getOrNull()

/**
 * 객체를 [String] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * 1.asString() shouldBeEqualTo "1"
 * 1.0.asString() shouldBeEqualTo "1.0"
 * null.asString() shouldBeEqualTo ""
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return String 변환 결과
 */
fun Any?.asString(defaultValue: String = ""): String = asStringOrNull() ?: defaultValue

/**
 * 객체를 [String] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * 1.asStringOrNull() shouldBeEqualTo "1"
 * 1.0.asStringOrNull() shouldBeEqualTo "1.0"
 * null.asStringOrNull() shouldBeEqualTo null
 * ```
 *
 * @return String 변환 결과
 */
fun Any?.asStringOrNull(): String? = this?.toString()

internal val SIMPLE_DATE_FORMAT = SimpleDateFormat()

/**
 * 객체를 [Date] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "2021-01-01".asDate() shouldBeEqualTo Date(1609459200000)
 * 1609459200000.asDate() shouldBeEqualTo Date(1609459200000)
 * null.asDate() shouldBeEqualTo Date(0)
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return String 변환 결과
 */
fun Any?.asDate(defaultValue: Date = Date(0L)): Date = asDateOrNull() ?: defaultValue

/**
 * 객체를 [Date] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "2021-01-01".asDateOrNull() shouldBeEqualTo Date(1609459200000)
 * 1609459200000.asDateOrNull() shouldBeEqualTo Date(1609459200000)
 * null.asDateOrNull() shouldBeEqualTo null
 * ```
 *
 * @return String 변환 결과
 */
fun Any?.asDateOrNull(): Date? = runCatching {
    when (this) {
        null                -> null
        is Number           -> Date(this.toLong())
        is Date             -> this
        is Instant          -> Date.from(this)
        is TemporalAccessor -> Date.from(Instant.from(this))
        else                -> SIMPLE_DATE_FORMAT.parse(asString())
    }
}.getOrNull()

/**
 * 객체를 [Timestamp] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "2021-01-01".asTimestamp() shouldBeEqualTo Timestamp(1609459200000)
 * 1609459200000.asTimestamp() shouldBeEqualTo Timestamp(1609459200000)
 * null.asTimestamp() shouldBeEqualTo Timestamp(0)
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return Timestamp 변환 결과
 */
fun Any?.asTimestamp(
    defaultValue: Timestamp = Timestamp(0L),
): Timestamp = asTimestampOrNull() ?: defaultValue

/**
 * 객체를 [Timestamp] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "2021-01-01".asTimestampOrNull() shouldBeEqualTo Timestamp(1609459200000)
 * 1609459200000.asTimestampOrNull() shouldBeEqualTo Timestamp(1609459200000)
 * null.asTimestampOrNull() shouldBeEqualTo null
 * ```
 *
 * @return Timestamp 변환 결과
 */
fun Any?.asTimestampOrNull(): Timestamp? = runCatching {
    when (this) {
        null                -> null
        is Number           -> Timestamp(this.toLong())
        is Timestamp        -> this
        is Instant          -> Timestamp.from(this)
        is TemporalAccessor -> Timestamp.from(Instant.from(this))
        else                -> Timestamp.valueOf(this.asString())
    }
}.getOrNull()

/**
 * 객체를 [Instant] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "2021-01-01".asInstant() shouldBeEqualTo Instant.parse("2021-01-01T00:00:00Z")
 * 1609459200000.asInstant() shouldBeEqualTo Instant.ofEpochMilli(1609459200000)
 * null.asInstant() shouldBeEqualTo Instant.ofEpochMilli(0)
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값
 * @return Instant 변환 결과
 */
fun Any?.asInstant(
    defaultValue: Instant = Instant.ofEpochMilli(0L),
): Instant = asInstantOrNull() ?: defaultValue

/**
 * 객체를 [Instant] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "2021-01-01".asInstantOrNull() shouldBeEqualTo Instant.parse("2021-01-01T00:00:00Z")
 * 1609459200000.asInstantOrNull() shouldBeEqualTo Instant.ofEpochMilli(1609459200000)
 * null.asInstantOrNull() shouldBeEqualTo null
 * ```
 *
 * @return Instant 변환 결과
 */
fun Any?.asInstantOrNull(): Instant? = runCatching {
    when (this) {
        null                -> null
        is Number           -> Instant.ofEpochMilli(this.toLong())
        is Instant          -> this
        is TemporalAccessor -> Instant.from(this)
        else                -> Instant.parse(this.asString())
    }
}.getOrNull()

/**
 * 객체를 [LocalDate] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "2021-01-01".asLocalDate() shouldBeEqualTo LocalDate.parse("2021-01-01")
 * 1609459200000.asLocalDate() shouldBeEqualTo LocalDate.of(2021, 1, 1)
 * null.asLocalDate() shouldBeEqualTo LocalDate.MIN
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값 [LocalDate.MIN]
 * @return LocalDate 변환 결과
 */
fun Any?.asLocalDate(
    defaultValue: LocalDate = LocalDate.MIN,
): LocalDate = asLocalDateOrNull() ?: defaultValue

/**
 * 객체를 [LocalDate] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "2021-01-01".asLocalDateOrNull() shouldBeEqualTo LocalDate.parse("2021-01-01")
 * 1609459200000.asLocalDateOrNull() shouldBeEqualTo LocalDate.of(2021, 1, 1)
 * null.asLocalDateOrNull() shouldBeEqualTo null
 * ```
 *
 * @return LocalDate 변환 결과
 */
fun Any?.asLocalDateOrNull(): LocalDate? = runCatching {
    when (this) {
        null                -> null
        is LocalDate        -> this
        is Instant          -> LocalDate.ofInstant(this, ZoneId.systemDefault())
        is TemporalAccessor -> LocalDate.from(this)
        else                -> LocalDate.parse(this.toString())
    }
}.getOrNull()

/**
 * 객체를 [LocalTime] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "12:00:00".asLocalTime() shouldBeEqualTo LocalTime.parse("12:00:00")
 * 1609459200000.asLocalTime() shouldBeEqualTo LocalTime.of(12, 0, 0)
 * null.asLocalTime() shouldBeEqualTo LocalTime.MIN
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값 [LocalTime.MIN]
 * @return LocalTime 변환 결과
 */
fun Any?.asLocalTime(defaultValue: LocalTime = LocalTime.MIN): LocalTime = asLocalTimeOrNull() ?: defaultValue

/**
 * 객체를 [LocalTime] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "12:00:00".asLocalTimeOrNull() shouldBeEqualTo LocalTime.parse("12:00:00")
 * 1609459200000.asLocalTimeOrNull() shouldBeEqualTo LocalTime.of(12, 0, 0)
 * null.asLocalTimeOrNull() shouldBeEqualTo null
 * ```
 *
 * @return LocalTime 변환 결과
 */
fun Any?.asLocalTimeOrNull(): LocalTime? = runCatching {
    when (this) {
        null                -> null
        is LocalTime        -> this
        is Instant          -> LocalTime.ofInstant(this, ZoneId.systemDefault())
        is TemporalAccessor -> LocalTime.from(this)
        else                -> LocalTime.parse(this.toString())
    }
}.getOrNull()

/**
 * 객체를 [LocalDateTime] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "2021-01-01T12:00:00".asLocalDateTime() shouldBeEqualTo LocalDateTime.parse("2021-01-01T12:00:00")
 * 1609459200000.asLocalDateTime() shouldBeEqualTo LocalDateTime.of(2021, 1, 1, 12, 0, 0)
 * null.asLocalDateTime() shouldBeEqualTo LocalDateTime.MIN
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값 [LocalDateTime.MIN]
 * @return LocalDateTime 변환 결과
 */
fun Any?.asLocalDateTime(defaultValue: LocalDateTime = LocalDateTime.MIN): LocalDateTime =
    asLocalDateTimeOrNull() ?: defaultValue

/**
 * 객체를 [LocalDateTime] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "2021-01-01T12:00:00".asLocalDateTimeOrNull() shouldBeEqualTo LocalDateTime.parse("2021-01-01T12:00:00")
 * 1609459200000.asLocalDateTimeOrNull() shouldBeEqualTo LocalDateTime.of(2021, 1, 1, 12, 0, 0)
 * null.asLocalDateTimeOrNull() shouldBeEqualTo null
 * ```
 *
 * @return LocalDateTime 변환 결과
 */
fun Any?.asLocalDateTimeOrNull(): LocalDateTime? = runCatching {
    when (this) {
        null                -> null
        is LocalDateTime    -> this
        is Instant          -> LocalDateTime.ofInstant(this, ZoneId.systemDefault())
        is TemporalAccessor -> LocalDateTime.from(this)
        else                -> LocalDateTime.parse(this.toString())
    }
}.getOrNull()

/**
 * 객체를 [OffsetDateTime] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "2021-01-01T12:00:00Z".asOffsetDateTime() shouldBeEqualTo OffsetDateTime.parse("2021-01-01T12:00:00Z")
 * 1609459200000.asOffsetDateTime() shouldBeEqualTo OffsetDateTime.ofInstant(Instant.ofEpochMilli(1609459200000), ZoneId.systemDefault())
 * null.asOffsetDateTime() shouldBeEqualTo OffsetDateTime.MIN
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값 [OffsetDateTime.MIN]
 * @return OffsetDateTime 변환 결과
 */
fun Any?.asOffsetDateTime(defaultValue: OffsetDateTime = OffsetDateTime.MIN): OffsetDateTime =
    asOffsetDateTimeOrNull() ?: defaultValue

/**
 * 객체를 [OffsetDateTime] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "2021-01-01T12:00:00Z".asOffsetDateTimeOrNull() shouldBeEqualTo OffsetDateTime.parse("2021-01-01T12:00:00Z")
 * 1609459200000.asOffsetDateTimeOrNull() shouldBeEqualTo OffsetDateTime.ofInstant(Instant.ofEpochMilli(1609459200000), ZoneId.systemDefault())
 * null.asOffsetDateTimeOrNull() shouldBeEqualTo null
 * ```
 *
 * @return OffsetDateTime 변환 결과
 */
fun Any?.asOffsetDateTimeOrNull(): OffsetDateTime? = runCatching {
    when (this) {
        null                -> null
        is OffsetDateTime   -> this
        is TemporalAccessor -> OffsetDateTime.from(this)
        else                -> OffsetDateTime.parse(this.toString())
    }
}.getOrNull()

/**
 * 객체를 [UUID] 수형으로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "24738134-9d88-6645-4ec8-d63aa2031015".asUUID() shouldBeEqualTo UUID.fromString("24738134-9d88-6645-4ec8-d63aa2031015")
 * null.asUUID() shouldBeEqualTo UUID.randomUUID()
 * ```
 *
 * @param defaultValue 변환 실패 시 대체 값 (기본값: [UUID.randomUUID])
 * @return UUID 변환 결과
 */
fun Any?.asUUID(defaultValue: UUID = UUID.randomUUID()): UUID = asUUIDOrNull() ?: defaultValue

/**
 * 객체를 [UUID] 수형으로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "24738134-9d88-6645-4ec8-d63aa2031015".asUUIDOrNull() shouldBeEqualTo UUID.fromString("24738134-9d88-6645-4ec8-d63aa2031015")
 * null.asUUIDOrNull() shouldBeEqualTo null
 * ```
 *
 * @return UUID 변환 결과
 */
fun Any?.asUUIDOrNull(): UUID? = runCatching {
    when (this) {
        null    -> null
        is UUID -> this
        else    -> UUID.fromString(this.toString())
    }
}.getOrNull()

/**
 * 객체를 [ByteArray]로 변환합니다. 변환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * "hello".asByteArray() shouldBeEqualTo "hello".toByteArray()
 * null.asByteArray() shouldBeEqualTo emptyByteArray
 * ```
 *
 * @param charset 문자 인코딩 (기본값: UTF-8)
 * @param defaultValue 변환 실패 시 대체 값 (기본값: 빈 ByteArray)
 * @return ByteArray 변환 결과
 */
fun Any?.asByteArray(charset: Charset = Charsets.UTF_8, defaultValue: ByteArray = emptyByteArray): ByteArray =
    asByteArrayOrNull(charset) ?: defaultValue

/**
 * 객체를 [ByteArray]로 변환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```
 * "hello".asByteArrayOrNull() shouldBeEqualTo "hello".toByteArray()
 * null.asByteArrayOrNull() shouldBeEqualTo null
 * ```
 *
 * @param charset 문자 인코딩 (기본값: UTF-8)
 * @return ByteArray 변환 결과
 */
fun Any?.asByteArrayOrNull(charset: Charset = Charsets.UTF_8): ByteArray? = runCatching {
    when (this) {
        null         -> null
        is ByteArray -> this
        else         -> toString().toByteArray(charset)
    }
}.getOrNull()

//
// Floor, Round for specific decimal point
//

//private val decimalFormats = ConcurrentHashMap<Int, DecimalFormat>()
//
//private fun getDecimalFormat(decimalCount: Int): DecimalFormat =
//    decimalFormats.computeIfAbsent(decimalCount) { dc ->
//        if (dc > 0) DecimalFormat("." + "#".repeat(dc))
//        else DecimalFormat("#")
//    }

/**
 * 객체를 [Float]로 변환하면서 [decimalCount] 자릿수에서 내림을 수행합니다.
 *
 * ```
 * 1.00123456.asFloatFloor(2) shouldBeEqualTo 1.00
 * 1.00123456.asFloatFloor(1) shouldBeEqualTo 1.0
 * "13567.6".asFloatFloor(-2) shouldBeEqualTo 13500.0
 * ```
 *
 * @param decimalCount 자릿 수
 * @param defaultValue 변환 실패 시 대체 값 (기본값: 0.0)
 * @return Float 변환 결과
 */
fun Any?.asFloatFloor(decimalCount: Int = 0, defaultValue: Float = 0.0F): Float =
    asFloatFloorOrNull(decimalCount) ?: defaultValue

/**
 * 객체를 [Float]로 변환하면서 [decimalCount] 자릿수에서 내림을 수행합니다. 반환 실패 시 null을 반환합니다.
 *
 * ```
 * 1.00123456.asFloatFloorOrNull(2) shouldBeEqualTo 1.00
 * 1.00123456.asFloatFloorOrNull(1) shouldBeEqualTo 1.0
 * "13567.6".asFloatFloorOrNull(-2) shouldBeEqualTo 13500.0
 * null.asFloatFloorOrNull() shouldBeEqualTo null
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @return Float 변환 결과
 */
fun Any?.asFloatFloorOrNull(decimalCount: Int = 0): Float? = this?.run {
    runCatching {
        if (decimalCount == 0) {
            return this.asLong().toFloat()
        }
        val decimal = 10.0.pow(decimalCount).toFloat()
        floor(asFloat() * decimal) / decimal
    }.getOrNull()
}

/**
 * 객체를 [Float]로 변환하면서 [decimalCount] 자릿수에서 반올림을 수행합니다.
 *
 * ```
 * 1.00123456.asFloatRound(2) shouldBeEqualTo 1.00
 * 1.00123456.asFloatRound(1) shouldBeEqualTo 1.0
 * "13567.6".asFloatRound(-2) shouldBeEqualTo 13600.0
 * null.asFloatRound() shouldBeEqualTo 0.0
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @param defaultValue 변환 실패 시 대체 값 (기본값: 0.0)
 * @return Float 변환 결과
 */
fun Any?.asFloatRound(decimalCount: Int = 0, defaultValue: Float = 0.0F): Float =
    asFloatRoundOrNull(decimalCount) ?: defaultValue

/**
 * 객체를 [Float]로 변환하면서 [decimalCount] 자릿수에서 반올림을 수행합니다. 반환 실패 시 null을 반환합니다.
 *
 * ```
 * 1.00123456.asFloatRoundOrNull(2) shouldBeEqualTo 1.00
 * 1.00123456.asFloatRoundOrNull(1) shouldBeEqualTo 1.0
 * "13567.6".asFloatRoundOrNull(-2) shouldBeEqualTo 13600.0
 * null.asFloatRoundOrNull() shouldBeEqualTo null
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @return Float 변환 결과
 */
fun Any?.asFloatRoundOrNull(decimalCount: Int = 0): Float? = this?.run {
    runCatching {
        if (decimalCount == 0) {
            return this.asLong().toFloat()
        }
        val decimal = 10.0.pow(decimalCount).toFloat()
        (this.asFloat() * decimal).roundToLong() / decimal
    }.getOrNull()
}

/**
 * 객체를 [Float]로 변환하면서 [decimalCount] 자릿수에서 올림을 수행합니다.
 *
 * ```
 * 1.00123456.asFloatCeil(2) shouldBeEqualTo 1.01
 * 1.00123456.asFloatCeil(1) shouldBeEqualTo 1.1
 * "13567.6".asFloatCeil(-2) shouldBeEqualTo 13600.0
 * null.asFloatCeil() shouldBeEqualTo 0.0
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @param defaultValue 변환 실패 시 대체 값 (기본값: 0.0)
 * @return Float 변환 결과
 */
fun Any?.asFloatCeil(decimalCount: Int = 0, defaultValue: Float = 0.0F): Float =
    asFloatCeilOrNull(decimalCount) ?: defaultValue

/**
 * 객체를 [Float]로 변환하면서 [decimalCount] 자릿수에서 올림을 수행합니다. 반환 실패 시 null을 반환합니다.
 *
 * ```
 * 1.00123456.asFloatCeilOrNull(2) shouldBeEqualTo 1.01
 * 1.00123456.asFloatCeilOrNull(1) shouldBeEqualTo 1.1
 * "13567.6".asFloatCeilOrNull(-2) shouldBeEqualTo 13600.0
 * null.asFloatCeilOrNull() shouldBeEqualTo null
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @return Float 변환 결과
 */
fun Any?.asFloatCeilOrNull(decimalCount: Int = 0): Float? = this?.run {
    runCatching {
        if (decimalCount == 0) {
            return this.asLong().toFloat()
        }

        val decimal = 10.0.pow(decimalCount).toFloat()
        ceil(asFloat() * decimal) / decimal
    }.getOrNull()
}

/**
 * 객체를 [Double]로 변환하면서 [decimalCount] 자릿수에서 내림을 수행합니다.
 *
 * ```
 * 1.00123456.asDoubleFloor(2) shouldBeEqualTo 1.00
 * 1.00123456.asDoubleFloor(1) shouldBeEqualTo 1.0
 * "13567.6".asDoubleFloor(-2) shouldBeEqualTo 13500.0
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @param defaultValue 변환 실패 시 대체 값 (기본값: 0.0)
 * @return Double 변환 결과
 */
fun Any?.asDoubleFloor(decimalCount: Int = 0, defaultValue: Double = 0.0): Double =
    asDoubleFloorOrNull(decimalCount) ?: defaultValue

/**
 * 객체를 [Double]로 변환하면서 [decimalCount] 자릿수에서 내림을 수행합니다. 반환 실패 시 null을 반환합니다.
 *
 * ```
 * 1.00123456.asDoubleFloorOrNull(2) shouldBeEqualTo 1.00
 * 1.00123456.asDoubleFloorOrNull(1) shouldBeEqualTo 1.0
 * "13567.6".asDoubleFloorOrNull(-2) shouldBeEqualTo 13500.0
 * null.asDoubleFloorOrNull() shouldBeEqualTo null
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @return Double 변환 결과
 */
fun Any?.asDoubleFloorOrNull(decimalCount: Int = 0): Double? = this?.run {
    runCatching {
        if (decimalCount == 0) {
            return this.asLong().toDouble()
        }

        val decimal = 10.0.pow(decimalCount)
        floor(asDouble() * decimal) / decimal
    }.getOrNull()
}

/**
 * 객체를 [Double]로 변환하면서 [decimalCount] 자릿수에서 반올림을 수행합니다. 반환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * 1.00123456.asDoubleRound(2) shouldBeEqualTo 1.00
 * 1.00123456.asDoubleRound(1) shouldBeEqualTo 1.0
 * "13567.6".asDoubleRound(-2) shouldBeEqualTo 13600.0
 * null.asDoubleRound() shouldBeEqualTo 0.0
 * ```
 * @param decimalCount 자릿 수 (기본값: 0)
 * @param defaultValue 변환 실패 시 대체 값 (기본값: 0.0)
 * @return Double 변환 결과
 */
fun Any?.asDoubleRound(decimalCount: Int = 0, defaultValue: Double = 0.0): Double =
    asDoubleRoundOrNull(decimalCount) ?: defaultValue

/**
 * 객체를 [Double]로 변환하면서 [decimalCount] 자릿수에서 반올림을 수행합니다. 반환 실패 시 null을 반환합니다.
 *
 * ```
 * 1.00123456.asDoubleRoundOrNull(2) shouldBeEqualTo 1.00
 * 1.00123456.asDoubleRoundOrNull(1) shouldBeEqualTo 1.0
 * "13567.6".asDoubleRoundOrNull(-2) shouldBeEqualTo 13600.0
 * null.asDoubleRoundOrNull() shouldBeEqualTo null
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @return Double 변환 결과
 */
fun Any?.asDoubleRoundOrNull(decimalCount: Int = 0): Double? = this?.run {
    runCatching {
        if (decimalCount == 0) {
            return this.asLong().toDouble()
        }
        val decimal = 10.0.pow(decimalCount)
        (asDouble() * decimal).roundToLong() / decimal
    }.getOrNull()
}

/**
 * 객체를 [Double]로 변환하면서 [decimalCount] 자릿수에서 올림을 수행합니다. 반환 실패 시 [defaultValue]로 대체합니다.
 *
 * ```
 * 1.00123456.asDoubleCeil(2) shouldBeEqualTo 1.01
 * 1.00123456.asDoubleCeil(1) shouldBeEqualTo 1.1
 * "13567.6".asDoubleCeil(-2) shouldBeEqualTo 13600.0
 * null.asDoubleCeil() shouldBeEqualTo 0.0
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @param defaultValue 변환 실패 시 대체 값 (기본값: 0.0)
 * @return Double 변환 결과
 */
fun Any?.asDoubleCeil(decimalCount: Int = 0, defaultValue: Double = 0.0): Double =
    asDoubleCeilOrNull(decimalCount) ?: defaultValue

/**
 * 객체를 [Double]로 변환하면서 [decimalCount] 자릿수에서 올림을 수행합니다. 반환 실패 시 null을 반환합니다.
 *
 * ```
 * 1.00123456.asDoubleCeilOrNull(2) shouldBeEqualTo 1.01
 * 1.00123456.asDoubleCeilOrNull(1) shouldBeEqualTo 1.1
 * "13567.6".asDoubleCeilOrNull(-2) shouldBeEqualTo 13600.0
 * null.asDoubleCeilOrNull() shouldBeEqualTo null
 * ```
 *
 * @param decimalCount 자릿 수 (기본값: 0)
 * @return Double 변환 결과
 */
fun Any?.asDoubleCeilOrNull(decimalCount: Int = 0): Double? = this?.run {
    runCatching {
        if (decimalCount == 0) {
            return this.asLong().toDouble()
        }
        val decimal = 10.0.pow(decimalCount)
        ceil(asDouble() * decimal) / decimal
    }.getOrNull()
}
