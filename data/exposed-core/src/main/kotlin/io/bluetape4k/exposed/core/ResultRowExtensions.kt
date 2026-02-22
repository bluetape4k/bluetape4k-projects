package io.bluetape4k.exposed.core

import io.bluetape4k.support.asBigDecimalOrNull
import io.bluetape4k.support.asBigIntOrNull
import io.bluetape4k.support.asBooleanOrNull
import io.bluetape4k.support.asByteArrayOrNull
import io.bluetape4k.support.asByteOrNull
import io.bluetape4k.support.asCharOrNull
import io.bluetape4k.support.asDateOrNull
import io.bluetape4k.support.asDoubleOrNull
import io.bluetape4k.support.asFloatOrNull
import io.bluetape4k.support.asInstantOrNull
import io.bluetape4k.support.asIntOrNull
import io.bluetape4k.support.asLocalDateOrNull
import io.bluetape4k.support.asLocalDateTimeOrNull
import io.bluetape4k.support.asLocalTimeOrNull
import io.bluetape4k.support.asLongOrNull
import io.bluetape4k.support.asOffsetDateTimeOrNull
import io.bluetape4k.support.asShortOrNull
import io.bluetape4k.support.asStringOrNull
import io.bluetape4k.support.asTimestampOrNull
import io.bluetape4k.support.asUUIDOrNull
import io.bluetape4k.support.asZonedDateTimeOrNull
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
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

@Suppress("UNCHECKED_CAST")
private fun ResultRow.anyValueOrNull(expression: Expression<*>): Any? =
    getOrNull(expression as Expression<Any?>)

private fun requiredError(expression: Expression<*>, typeName: String): Nothing =
    error("Expression[$expression] is null or not convertible to $typeName.")

fun ResultRow.getBoolean(expression: Expression<*>): Boolean =
    getBooleanOrNull(expression) ?: requiredError(expression, "Boolean")

fun ResultRow.getBooleanOrNull(expression: Expression<*>): Boolean? =
    anyValueOrNull(expression).asBooleanOrNull()

fun ResultRow.getChar(expression: Expression<*>): Char =
    getCharOrNull(expression) ?: requiredError(expression, "Char")

fun ResultRow.getCharOrNull(expression: Expression<*>): Char? =
    anyValueOrNull(expression).asCharOrNull()

fun ResultRow.getByte(expression: Expression<*>): Byte =
    getByteOrNull(expression) ?: requiredError(expression, "Byte")

fun ResultRow.getByteOrNull(expression: Expression<*>): Byte? =
    anyValueOrNull(expression).asByteOrNull()

fun ResultRow.getShort(expression: Expression<*>): Short =
    getShortOrNull(expression) ?: requiredError(expression, "Short")

fun ResultRow.getShortOrNull(expression: Expression<*>): Short? =
    anyValueOrNull(expression).asShortOrNull()

fun ResultRow.getInt(expression: Expression<*>): Int =
    getIntOrNull(expression) ?: requiredError(expression, "Int")

fun ResultRow.getIntOrNull(expression: Expression<*>): Int? =
    anyValueOrNull(expression).asIntOrNull()

fun ResultRow.getLong(expression: Expression<*>): Long =
    getLongOrNull(expression) ?: requiredError(expression, "Long")

fun ResultRow.getLongOrNull(expression: Expression<*>): Long? =
    anyValueOrNull(expression).asLongOrNull()

fun ResultRow.getFloat(expression: Expression<*>): Float =
    getFloatOrNull(expression) ?: requiredError(expression, "Float")

fun ResultRow.getFloatOrNull(expression: Expression<*>): Float? =
    anyValueOrNull(expression).asFloatOrNull()

fun ResultRow.getDouble(expression: Expression<*>): Double =
    getDoubleOrNull(expression) ?: requiredError(expression, "Double")

fun ResultRow.getDoubleOrNull(expression: Expression<*>): Double? =
    anyValueOrNull(expression).asDoubleOrNull()

fun ResultRow.getBigInt(expression: Expression<*>): BigInteger =
    getBigIntOrNull(expression) ?: requiredError(expression, "BigInteger")

fun ResultRow.getBigIntOrNull(expression: Expression<*>): BigInteger? =
    anyValueOrNull(expression).asBigIntOrNull()

fun ResultRow.getBigDecimal(expression: Expression<*>): BigDecimal =
    getBigDecimalOrNull(expression) ?: requiredError(expression, "BigDecimal")

fun ResultRow.getBigDecimalOrNull(expression: Expression<*>): BigDecimal? =
    anyValueOrNull(expression).asBigDecimalOrNull()

fun ResultRow.getString(expression: Expression<*>): String =
    getStringOrNull(expression) ?: requiredError(expression, "String")

fun ResultRow.getStringOrNull(expression: Expression<*>): String? =
    anyValueOrNull(expression).asStringOrNull()

fun ResultRow.getByteArray(expression: Expression<*>): ByteArray =
    getByteArrayOrNull(expression) ?: requiredError(expression, "ByteArray")

fun ResultRow.getByteArrayOrNull(expression: Expression<*>): ByteArray? =
    anyValueOrNull(expression).asByteArrayOrNull()

fun ResultRow.getDate(expression: Expression<*>): Date =
    getDateOrNull(expression) ?: requiredError(expression, "Date")

fun ResultRow.getDateOrNull(expression: Expression<*>): Date? =
    anyValueOrNull(expression).asDateOrNull()

fun ResultRow.getTimestamp(expression: Expression<*>): Timestamp =
    getTimestampOrNull(expression) ?: requiredError(expression, "Timestamp")

fun ResultRow.getTimestampOrNull(expression: Expression<*>): Timestamp? =
    anyValueOrNull(expression).asTimestampOrNull()

fun ResultRow.getInstant(expression: Expression<*>): Instant =
    getInstantOrNull(expression) ?: requiredError(expression, "Instant")

fun ResultRow.getInstantOrNull(expression: Expression<*>): Instant? =
    anyValueOrNull(expression).asInstantOrNull()

fun ResultRow.getLocalDate(expression: Expression<*>): LocalDate =
    getLocalDateOrNull(expression) ?: requiredError(expression, "LocalDate")

fun ResultRow.getLocalDateOrNull(expression: Expression<*>): LocalDate? =
    anyValueOrNull(expression).asLocalDateOrNull()

fun ResultRow.getLocalTime(expression: Expression<*>): LocalTime =
    getLocalTimeOrNull(expression) ?: requiredError(expression, "LocalTime")

fun ResultRow.getLocalTimeOrNull(expression: Expression<*>): LocalTime? =
    anyValueOrNull(expression).asLocalTimeOrNull()

fun ResultRow.getLocalDateTime(expression: Expression<*>): LocalDateTime =
    getLocalDateTimeOrNull(expression) ?: requiredError(expression, "LocalDateTime")

fun ResultRow.getLocalDateTimeOrNull(expression: Expression<*>): LocalDateTime? =
    anyValueOrNull(expression).asLocalDateTimeOrNull()

fun ResultRow.getOffsetDateTime(expression: Expression<*>): OffsetDateTime =
    getOffsetDateTimeOrNull(expression) ?: requiredError(expression, "OffsetDateTime")

fun ResultRow.getOffsetDateTimeOrNull(expression: Expression<*>): OffsetDateTime? =
    anyValueOrNull(expression).asOffsetDateTimeOrNull()

fun ResultRow.getZonedDateTime(expression: Expression<*>): ZonedDateTime =
    getZonedDateTimeOrNull(expression) ?: requiredError(expression, "ZonedDateTime")

fun ResultRow.getZonedDateTimeOrNull(expression: Expression<*>): ZonedDateTime? =
    anyValueOrNull(expression).asZonedDateTimeOrNull()

fun ResultRow.getUuid(expression: Expression<*>): UUID =
    getUuidOrNull(expression) ?: requiredError(expression, "UUID")

fun ResultRow.getUuidOrNull(expression: Expression<*>): UUID? =
    anyValueOrNull(expression).asUUIDOrNull()
