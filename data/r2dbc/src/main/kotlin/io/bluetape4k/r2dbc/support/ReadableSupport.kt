package io.bluetape4k.r2dbc.support

import io.bluetape4k.support.asBigDecimal
import io.bluetape4k.support.asBigDecimalOrNull
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
import io.r2dbc.spi.Readable
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.*

inline fun <reified T: Any> Readable.getAs(index: Int): T = get(index, T::class.java)!!
inline fun <reified T: Any> Readable.getAs(name: String): T = get(name, T::class.java)!!
inline fun <reified T: Any> Readable.getAsOrNull(index: Int): T? = get(index, T::class.java)
inline fun <reified T: Any> Readable.getAsOrNull(name: String): T? = get(name, T::class.java)

fun Readable.boolean(index: Int): Boolean = getAs<Boolean>(index)
fun Readable.boolean(name: String): Boolean = getAs<Boolean>(name)
fun Readable.booleanOrNull(index: Int): Boolean? = get(index).asBooleanOrNull()
fun Readable.booleanOrNull(name: String): Boolean? = get(name).asBooleanOrNull()

fun Readable.char(index: Int): Char = get(index).asChar()
fun Readable.char(name: String): Char = get(name).asChar()
fun Readable.charOrNull(index: Int): Char? = get(index).asCharOrNull()
fun Readable.charOrNull(name: String): Char? = get(name).asCharOrNull()

fun Readable.byte(index: Int): Byte = getAs<Byte>(index)
fun Readable.byte(name: String): Byte = getAs<Byte>(name)
fun Readable.byteOrNull(index: Int): Byte? = get(index).asByteOrNull()
fun Readable.byteOrNull(name: String): Byte? = get(name).asByteOrNull()

fun Readable.short(index: Int): Short = getAs<Short>(index)
fun Readable.short(name: String): Short = getAs<Short>(name)
fun Readable.shortOrNull(index: Int): Short? = get(index).asShortOrNull()
fun Readable.shortOrNull(name: String): Short? = get(name).asShortOrNull()

fun Readable.int(index: Int): Int = getAs<Int>(index)
fun Readable.int(name: String): Int = getAs<Int>(name)
fun Readable.intOrNull(index: Int): Int? = get(index).asIntOrNull()
fun Readable.intOrNull(name: String): Int? = get(name).asIntOrNull()

fun Readable.long(index: Int): Long = get(index).asLong()
fun Readable.long(name: String): Long = get(name).asLong()
fun Readable.longOrNull(index: Int): Long? = get(index).asLongOrNull()
fun Readable.longOrNull(name: String): Long? = get(name).asLongOrNull()

fun Readable.float(index: Int): Float = get(index).asFloat()
fun Readable.float(name: String): Float = get(name).asFloat()
fun Readable.floatOrNull(index: Int): Float? = get(index).asFloatOrNull()
fun Readable.floatOrNull(name: String): Float? = get(name).asFloatOrNull()

fun Readable.double(index: Int): Double = get(index).asDouble()
fun Readable.double(name: String): Double = get(name).asDouble()
fun Readable.doubleOrNull(index: Int): Double? = get(index).asDoubleOrNull()
fun Readable.doubleOrNull(name: String): Double? = get(name).asDoubleOrNull()

fun Readable.bigDecimal(index: Int): BigDecimal = get(index).asBigDecimal()
fun Readable.bigDecimal(name: String): BigDecimal = get(name).asBigDecimal()
fun Readable.bigDecimalOrNull(index: Int): BigDecimal? = get(index).asBigDecimalOrNull()
fun Readable.bigDecimalOrNull(name: String): BigDecimal? = get(name).asBigDecimalOrNull()

fun Readable.string(index: Int): String = get(index, String::class.java).asString()
fun Readable.string(name: String): String = get(name, String::class.java).asString()
fun Readable.stringOrNull(index: Int): String? = getAsOrNull<String>(index).asStringOrNull()
fun Readable.stringOrNull(name: String): String? = getAsOrNull<String>(name).asStringOrNull()

fun Readable.byteArray(index: Int): ByteArray = get(index).asByteArray()
fun Readable.byteArray(name: String): ByteArray = get(name).asByteArray()
fun Readable.byteArrayOrNull(index: Int): ByteArray? = get(index).asByteArrayOrNull()
fun Readable.byteArrayOrNull(name: String): ByteArray? = get(name).asByteArrayOrNull()

fun Readable.date(index: Int): Date = get(index).asDate()
fun Readable.date(name: String): Date = get(name).asDate()
fun Readable.dateOrNull(index: Int): Date? = get(index).asDateOrNull()
fun Readable.dateOrNull(name: String): Date? = get(name).asDateOrNull()

fun Readable.timestamp(index: Int): Timestamp = get(index).asTimestamp()
fun Readable.timestamp(name: String): Timestamp = get(name).asTimestamp()
fun Readable.timestampOrNull(index: Int): Timestamp? = get(index).asTimestampOrNull()
fun Readable.timestampOrNull(name: String): Timestamp? = get(name).asTimestampOrNull()

fun Readable.instant(index: Int): Instant = get(index).asInstant()
fun Readable.instant(name: String): Instant = get(name).asInstant()
fun Readable.instantOrNull(index: Int): Instant? = get(index).asInstantOrNull()
fun Readable.instantOrNull(name: String): Instant? = get(name).asInstantOrNull()

fun Readable.localDate(index: Int): LocalDate = get(index).asLocalDate()
fun Readable.localDate(name: String): LocalDate = get(name).asLocalDate()
fun Readable.localDateOrNull(index: Int): LocalDate? = get(index).asLocalDateOrNull()
fun Readable.localDateOrNull(name: String): LocalDate? = get(name).asLocalDateOrNull()

fun Readable.localTime(index: Int): LocalTime = get(index).asLocalTime()
fun Readable.localTime(name: String): LocalTime = get(name).asLocalTime()
fun Readable.localTimeOrNull(index: Int): LocalTime? = get(index).asLocalTimeOrNull()
fun Readable.localTimeOrNull(name: String): LocalTime? = get(name).asLocalTimeOrNull()

fun Readable.localDateTime(index: Int): LocalDateTime = get(index).asLocalDateTime()
fun Readable.localDateTime(name: String): LocalDateTime = get(name).asLocalDateTime()
fun Readable.localDateTimeOrNull(index: Int): LocalDateTime? = get(index).asLocalDateTimeOrNull()
fun Readable.localDateTimeOrNull(name: String): LocalDateTime? = get(name).asLocalDateTimeOrNull()

fun Readable.offsetDateTime(index: Int): OffsetDateTime = get(index).asOffsetDateTime()
fun Readable.offsetDateTime(name: String): OffsetDateTime = get(name).asOffsetDateTime()
fun Readable.offsetDateTimeOrNull(index: Int): OffsetDateTime? = get(index).asOffsetDateTimeOrNull()
fun Readable.offsetDateTimeOrNull(name: String): OffsetDateTime? = get(name).asOffsetDateTimeOrNull()

fun Readable.uuid(index: Int): UUID = getAs<UUID>(index)
fun Readable.uuid(name: String): UUID = getAs<UUID>(name)
fun Readable.uuidOrNull(index: Int): UUID? = get(index).asUUIDOrNull()
fun Readable.uuidOrNull(name: String): UUID? = get(name).asUUIDOrNull()
