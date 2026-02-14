@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.r2dbc.support

import io.bluetape4k.support.asBigDecimal
import io.bluetape4k.support.asBigDecimalOrNull
import io.bluetape4k.support.asBigInt
import io.bluetape4k.support.asBigIntOrNull
import io.bluetape4k.support.asBooleanOrNull
import io.bluetape4k.support.asByteArray
import io.bluetape4k.support.asByteArrayOrNull
import io.bluetape4k.support.asByteOrNull
import io.bluetape4k.support.asChar
import io.bluetape4k.support.asCharOrNull
import io.bluetape4k.support.asDate
import io.bluetape4k.support.asDateOrNull
import io.bluetape4k.support.asDouble
import io.bluetape4k.support.asDoubleOrNull
import io.bluetape4k.support.asFloat
import io.bluetape4k.support.asFloatOrNull
import io.bluetape4k.support.asInstant
import io.bluetape4k.support.asInstantOrNull
import io.bluetape4k.support.asIntOrNull
import io.bluetape4k.support.asLocalDate
import io.bluetape4k.support.asLocalDateOrNull
import io.bluetape4k.support.asLocalDateTime
import io.bluetape4k.support.asLocalDateTimeOrNull
import io.bluetape4k.support.asLocalTime
import io.bluetape4k.support.asLocalTimeOrNull
import io.bluetape4k.support.asLong
import io.bluetape4k.support.asLongOrNull
import io.bluetape4k.support.asOffsetDateTime
import io.bluetape4k.support.asOffsetDateTimeOrNull
import io.bluetape4k.support.asShortOrNull
import io.bluetape4k.support.asString
import io.bluetape4k.support.asStringOrNull
import io.bluetape4k.support.asTimestamp
import io.bluetape4k.support.asTimestampOrNull
import io.bluetape4k.support.asUUIDOrNull
import io.bluetape4k.support.asZonedDateTime
import io.bluetape4k.support.asZonedDateTimeOrNull
import io.r2dbc.spi.Readable
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.*

inline fun <reified T: Any> Readable.getAs(index: Int): T =
    getAsOrNull(index) ?: error("Column[$index] is null or unsupported type.")

inline fun <reified T: Any> Readable.getAs(name: String): T =
    getAsOrNull(name) ?: error("Column[$name] is null or unsupported type.")

inline fun <reified T: Any> Readable.getAsOrNull(index: Int): T? = get(index, T::class.java)
inline fun <reified T: Any> Readable.getAsOrNull(name: String): T? = get(name, T::class.java)

inline fun Readable.boolean(index: Int): Boolean = getAs(index)
inline fun Readable.boolean(name: String): Boolean = getAs(name)
inline fun Readable.booleanOrNull(index: Int): Boolean? = get(index).asBooleanOrNull()
inline fun Readable.booleanOrNull(name: String): Boolean? = get(name).asBooleanOrNull()

inline fun Readable.char(index: Int): Char = get(index).asChar()
inline fun Readable.char(name: String): Char = get(name).asChar()
inline fun Readable.charOrNull(index: Int): Char? = get(index).asCharOrNull()
inline fun Readable.charOrNull(name: String): Char? = get(name).asCharOrNull()

inline fun Readable.byte(index: Int): Byte = getAs(index)
inline fun Readable.byte(name: String): Byte = getAs(name)
inline fun Readable.byteOrNull(index: Int): Byte? = get(index).asByteOrNull()
inline fun Readable.byteOrNull(name: String): Byte? = get(name).asByteOrNull()

inline fun Readable.short(index: Int): Short = getAs(index)
inline fun Readable.short(name: String): Short = getAs(name)
inline fun Readable.shortOrNull(index: Int): Short? = get(index).asShortOrNull()
inline fun Readable.shortOrNull(name: String): Short? = get(name).asShortOrNull()

inline fun Readable.int(index: Int): Int = getAs(index)
inline fun Readable.int(name: String): Int = getAs(name)
inline fun Readable.intOrNull(index: Int): Int? = get(index).asIntOrNull()
inline fun Readable.intOrNull(name: String): Int? = get(name).asIntOrNull()

inline fun Readable.long(index: Int): Long = get(index).asLong()
inline fun Readable.long(name: String): Long = get(name).asLong()
inline fun Readable.longOrNull(index: Int): Long? = get(index).asLongOrNull()
inline fun Readable.longOrNull(name: String): Long? = get(name).asLongOrNull()

inline fun Readable.float(index: Int): Float = get(index).asFloat()
inline fun Readable.float(name: String): Float = get(name).asFloat()
inline fun Readable.floatOrNull(index: Int): Float? = get(index).asFloatOrNull()
inline fun Readable.floatOrNull(name: String): Float? = get(name).asFloatOrNull()

inline fun Readable.double(index: Int): Double = get(index).asDouble()
inline fun Readable.double(name: String): Double = get(name).asDouble()
inline fun Readable.doubleOrNull(index: Int): Double? = get(index).asDoubleOrNull()
inline fun Readable.doubleOrNull(name: String): Double? = get(name).asDoubleOrNull()

inline fun Readable.bigInt(index: Int): BigInteger = get(index).asBigInt()
inline fun Readable.bigInt(name: String): BigInteger = get(name).asBigInt()
inline fun Readable.bigIntOrNull(index: Int): BigInteger? = get(index).asBigIntOrNull()
inline fun Readable.bigIntOrNull(name: String): BigInteger? = get(name).asBigIntOrNull()

inline fun Readable.bigDecimal(index: Int): BigDecimal = get(index).asBigDecimal()
inline fun Readable.bigDecimal(name: String): BigDecimal = get(name).asBigDecimal()
inline fun Readable.bigDecimalOrNull(index: Int): BigDecimal? = get(index).asBigDecimalOrNull()
inline fun Readable.bigDecimalOrNull(name: String): BigDecimal? = get(name).asBigDecimalOrNull()

inline fun Readable.string(index: Int): String = get(index, String::class.java).asString()
inline fun Readable.string(name: String): String = get(name, String::class.java).asString()
inline fun Readable.stringOrNull(index: Int): String? = getAsOrNull<String>(index).asStringOrNull()
inline fun Readable.stringOrNull(name: String): String? = getAsOrNull<String>(name).asStringOrNull()

inline fun Readable.byteArray(index: Int): ByteArray = get(index).asByteArray()
inline fun Readable.byteArray(name: String): ByteArray = get(name).asByteArray()
inline fun Readable.byteArrayOrNull(index: Int): ByteArray? = get(index).asByteArrayOrNull()
inline fun Readable.byteArrayOrNull(name: String): ByteArray? = get(name).asByteArrayOrNull()

inline fun Readable.date(index: Int): Date = get(index).asDate()
inline fun Readable.date(name: String): Date = get(name).asDate()
inline fun Readable.dateOrNull(index: Int): Date? = get(index).asDateOrNull()
inline fun Readable.dateOrNull(name: String): Date? = get(name).asDateOrNull()

inline fun Readable.timestamp(index: Int): Timestamp = get(index).asTimestamp()
inline fun Readable.timestamp(name: String): Timestamp = get(name).asTimestamp()
inline fun Readable.timestampOrNull(index: Int): Timestamp? = get(index).asTimestampOrNull()
inline fun Readable.timestampOrNull(name: String): Timestamp? = get(name).asTimestampOrNull()

inline fun Readable.instant(index: Int): Instant = get(index).asInstant()
inline fun Readable.instant(name: String): Instant = get(name).asInstant()
inline fun Readable.instantOrNull(index: Int): Instant? = get(index).asInstantOrNull()
inline fun Readable.instantOrNull(name: String): Instant? = get(name).asInstantOrNull()

inline fun Readable.localDate(index: Int): LocalDate = get(index).asLocalDate()
inline fun Readable.localDate(name: String): LocalDate = get(name).asLocalDate()
inline fun Readable.localDateOrNull(index: Int): LocalDate? = get(index).asLocalDateOrNull()
inline fun Readable.localDateOrNull(name: String): LocalDate? = get(name).asLocalDateOrNull()

inline fun Readable.localTime(index: Int): LocalTime = get(index).asLocalTime()
inline fun Readable.localTime(name: String): LocalTime = get(name).asLocalTime()
inline fun Readable.localTimeOrNull(index: Int): LocalTime? = get(index).asLocalTimeOrNull()
inline fun Readable.localTimeOrNull(name: String): LocalTime? = get(name).asLocalTimeOrNull()

inline fun Readable.localDateTime(index: Int): LocalDateTime = get(index).asLocalDateTime()
inline fun Readable.localDateTime(name: String): LocalDateTime = get(name).asLocalDateTime()
inline fun Readable.localDateTimeOrNull(index: Int): LocalDateTime? = get(index).asLocalDateTimeOrNull()
inline fun Readable.localDateTimeOrNull(name: String): LocalDateTime? = get(name).asLocalDateTimeOrNull()

inline fun Readable.offsetDateTime(index: Int): OffsetDateTime = get(index).asOffsetDateTime()
inline fun Readable.offsetDateTime(name: String): OffsetDateTime = get(name).asOffsetDateTime()
inline fun Readable.offsetDateTimeOrNull(index: Int): OffsetDateTime? = get(index).asOffsetDateTimeOrNull()
inline fun Readable.offsetDateTimeOrNull(name: String): OffsetDateTime? = get(name).asOffsetDateTimeOrNull()

inline fun Readable.zonedDateTime(index: Int): ZonedDateTime = get(index).asZonedDateTime()
inline fun Readable.zonedDateTime(name: String): ZonedDateTime = get(name).asZonedDateTime()
inline fun Readable.zonedDateTimeOrNull(index: Int): ZonedDateTime? = get(index).asZonedDateTimeOrNull()
inline fun Readable.zonedDateTimeOrNull(name: String): ZonedDateTime? = get(name).asZonedDateTimeOrNull()

inline fun Readable.uuid(index: Int): UUID = getAs(index)
inline fun Readable.uuid(name: String): UUID = getAs(name)
inline fun Readable.uuidOrNull(index: Int): UUID? = get(index).asUUIDOrNull()
inline fun Readable.uuidOrNull(name: String): UUID? = get(name).asUUIDOrNull()
