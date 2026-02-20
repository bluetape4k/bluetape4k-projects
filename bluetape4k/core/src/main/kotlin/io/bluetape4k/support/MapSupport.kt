package io.bluetape4k.support

import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.*

private fun Map<String, Any?>.requireValue(name: String): Any {
    return requireNotNull(this[name]) { "Map[$name] is missing or null." }
}

fun Map<String, Any?>.boolean(name: String): Boolean = requireValue(name).asBoolean()
fun Map<String, Any?>.booleanOrNull(name: String): Boolean? = this[name].asBooleanOrNull()

fun Map<String, Any?>.char(name: String): Char = requireValue(name).asChar()
fun Map<String, Any?>.charOrNull(name: String): Char? = this[name].asCharOrNull()

fun Map<String, Any?>.byte(name: String): Byte = requireValue(name).asByte()
fun Map<String, Any?>.byteOrNull(name: String): Byte? = this[name].asByteOrNull()

fun Map<String, Any?>.short(name: String): Short = requireValue(name).asShort()
fun Map<String, Any?>.shortOrNull(name: String): Short? = this[name].asShortOrNull()

fun Map<String, Any?>.int(name: String): Int = requireValue(name).asInt()
fun Map<String, Any?>.intOrNull(name: String): Int? = this[name].asIntOrNull()

fun Map<String, Any?>.long(name: String): Long = requireValue(name).asLong()
fun Map<String, Any?>.longOrNull(name: String): Long? = this[name].asLongOrNull()

fun Map<String, Any?>.bigDecimal(name: String): BigDecimal = requireValue(name).asBigDecimal()
fun Map<String, Any?>.bigDecimalOrNull(name: String): BigDecimal? = this[name].asBigDecimalOrNull()

fun Map<String, Any?>.string(name: String): String = requireValue(name).asString()
fun Map<String, Any?>.stringOrNull(name: String): String? = this[name].asStringOrNull()

fun Map<String, Any?>.byteArray(name: String): ByteArray = requireValue(name).asByteArray()
fun Map<String, Any?>.byteArrayOrNull(name: String): ByteArray? = this[name].asByteArrayOrNull()

fun Map<String, Any?>.date(name: String): Date = requireValue(name).asDate()
fun Map<String, Any?>.dateOrNull(name: String): Date? = this[name].asDateOrNull()

fun Map<String, Any?>.timestamp(name: String): Timestamp = requireValue(name).asTimestamp()
fun Map<String, Any?>.timestampOrNull(name: String): Timestamp? = this[name].asTimestampOrNull()

fun Map<String, Any?>.instant(name: String): Instant = requireValue(name).asInstant()
fun Map<String, Any?>.instantOrNull(name: String): Instant? = this[name].asInstantOrNull()

fun Map<String, Any?>.localDate(name: String): LocalDate = requireValue(name).asLocalDate()
fun Map<String, Any?>.localDateOrNull(name: String): LocalDate? = this[name].asLocalDateOrNull()

fun Map<String, Any?>.localTime(name: String): LocalTime = requireValue(name).asLocalTime()
fun Map<String, Any?>.localTimeOrNull(name: String): LocalTime? = this[name].asLocalTimeOrNull()

fun Map<String, Any?>.localDateTime(name: String): LocalDateTime = requireValue(name).asLocalDateTime()
fun Map<String, Any?>.localDateTimeOrNull(name: String): LocalDateTime? = this[name].asLocalDateTimeOrNull()

fun Map<String, Any?>.offsetDateTime(name: String): OffsetDateTime = requireValue(name).asOffsetDateTime()
fun Map<String, Any?>.offsetDateTimeOrNull(name: String): OffsetDateTime? = this[name].asOffsetDateTimeOrNull()

fun Map<String, Any?>.uuid(name: String): UUID = requireValue(name).asUUID()
fun Map<String, Any?>.uuidOrNull(name: String): UUID? = this[name].asUUIDOrNull()
