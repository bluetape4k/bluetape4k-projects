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

/**
 * 표현식 값을 `Boolean`으로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val active = row.getBoolean(UsersTable.active)
 * // active == true
 * ```
 */
fun ResultRow.getBoolean(expression: Expression<*>): Boolean =
    getBooleanOrNull(expression) ?: requiredError(expression, "Boolean")

/**
 * 표현식 값을 `Boolean`으로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val active = row.getBooleanOrNull(UsersTable.active)
 * // active == true 또는 null
 * ```
 */
fun ResultRow.getBooleanOrNull(expression: Expression<*>): Boolean? =
    anyValueOrNull(expression).asBooleanOrNull()

/**
 * 표현식 값을 `Char`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val code = row.getChar(CodesTable.code)
 * // code == 'A'
 * ```
 */
fun ResultRow.getChar(expression: Expression<*>): Char =
    getCharOrNull(expression) ?: requiredError(expression, "Char")

/**
 * 표현식 값을 `Char`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val code = row.getCharOrNull(CodesTable.code)
 * // code == 'A' 또는 null
 * ```
 */
fun ResultRow.getCharOrNull(expression: Expression<*>): Char? =
    anyValueOrNull(expression).asCharOrNull()

/**
 * 표현식 값을 `Byte`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val flags = row.getByte(RecordsTable.flags)
 * // flags == 0x01.toByte()
 * ```
 */
fun ResultRow.getByte(expression: Expression<*>): Byte =
    getByteOrNull(expression) ?: requiredError(expression, "Byte")

/**
 * 표현식 값을 `Byte`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val flags = row.getByteOrNull(RecordsTable.flags)
 * // flags != null 또는 null
 * ```
 */
fun ResultRow.getByteOrNull(expression: Expression<*>): Byte? =
    anyValueOrNull(expression).asByteOrNull()

/**
 * 표현식 값을 `Short`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val count = row.getShort(StatsTable.count)
 * // count > 0
 * ```
 */
fun ResultRow.getShort(expression: Expression<*>): Short =
    getShortOrNull(expression) ?: requiredError(expression, "Short")

/**
 * 표현식 값을 `Short`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val count = row.getShortOrNull(StatsTable.count)
 * // count != null 또는 null
 * ```
 */
fun ResultRow.getShortOrNull(expression: Expression<*>): Short? =
    anyValueOrNull(expression).asShortOrNull()

/**
 * 표현식 값을 `Int`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val age = row.getInt(UsersTable.age)
 * // age == 30
 * ```
 */
fun ResultRow.getInt(expression: Expression<*>): Int =
    getIntOrNull(expression) ?: requiredError(expression, "Int")

/**
 * 표현식 값을 `Int`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val age = row.getIntOrNull(UsersTable.age)
 * // age == 30 또는 null
 * ```
 */
fun ResultRow.getIntOrNull(expression: Expression<*>): Int? =
    anyValueOrNull(expression).asIntOrNull()

/**
 * 표현식 값을 `Long`으로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val id = row.getLong(UsersTable.id)
 * // id > 0L
 * ```
 */
fun ResultRow.getLong(expression: Expression<*>): Long =
    getLongOrNull(expression) ?: requiredError(expression, "Long")

/**
 * 표현식 값을 `Long`으로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val id = row.getLongOrNull(UsersTable.id)
 * // id != null
 * ```
 */
fun ResultRow.getLongOrNull(expression: Expression<*>): Long? =
    anyValueOrNull(expression).asLongOrNull()

/**
 * 표현식 값을 `Float`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val score = row.getFloat(ResultsTable.score)
 * // score == 98.5f
 * ```
 */
fun ResultRow.getFloat(expression: Expression<*>): Float =
    getFloatOrNull(expression) ?: requiredError(expression, "Float")

/**
 * 표현식 값을 `Float`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val score = row.getFloatOrNull(ResultsTable.score)
 * // score != null 또는 null
 * ```
 */
fun ResultRow.getFloatOrNull(expression: Expression<*>): Float? =
    anyValueOrNull(expression).asFloatOrNull()

/**
 * 표현식 값을 `Double`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val lat = row.getDouble(LocationsTable.latitude)
 * // lat == 37.5665
 * ```
 */
fun ResultRow.getDouble(expression: Expression<*>): Double =
    getDoubleOrNull(expression) ?: requiredError(expression, "Double")

/**
 * 표현식 값을 `Double`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val lat = row.getDoubleOrNull(LocationsTable.latitude)
 * // lat != null 또는 null
 * ```
 */
fun ResultRow.getDoubleOrNull(expression: Expression<*>): Double? =
    anyValueOrNull(expression).asDoubleOrNull()

/**
 * 표현식 값을 `BigInteger`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val total = row.getBigInt(TotalsTable.amount)
 * // total > BigInteger.ZERO
 * ```
 */
fun ResultRow.getBigInt(expression: Expression<*>): BigInteger =
    getBigIntOrNull(expression) ?: requiredError(expression, "BigInteger")

/**
 * 표현식 값을 `BigInteger`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val total = row.getBigIntOrNull(TotalsTable.amount)
 * // total != null 또는 null
 * ```
 */
fun ResultRow.getBigIntOrNull(expression: Expression<*>): BigInteger? =
    anyValueOrNull(expression).asBigIntOrNull()

/**
 * 표현식 값을 `BigDecimal`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val price = row.getBigDecimal(ProductsTable.price)
 * // price == BigDecimal("9.99")
 * ```
 */
fun ResultRow.getBigDecimal(expression: Expression<*>): BigDecimal =
    getBigDecimalOrNull(expression) ?: requiredError(expression, "BigDecimal")

/**
 * 표현식 값을 `BigDecimal`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val price = row.getBigDecimalOrNull(ProductsTable.price)
 * // price != null 또는 null
 * ```
 */
fun ResultRow.getBigDecimalOrNull(expression: Expression<*>): BigDecimal? =
    anyValueOrNull(expression).asBigDecimalOrNull()

/**
 * 표현식 값을 `String`으로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val name = row.getString(UsersTable.name)
 * // name.isNotBlank()
 * ```
 */
fun ResultRow.getString(expression: Expression<*>): String =
    getStringOrNull(expression) ?: requiredError(expression, "String")

/**
 * 표현식 값을 `String`으로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val name = row.getStringOrNull(UsersTable.name)
 * // name != null 또는 null
 * ```
 */
fun ResultRow.getStringOrNull(expression: Expression<*>): String? =
    anyValueOrNull(expression).asStringOrNull()

/**
 * 표현식 값을 `ByteArray`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val data = row.getByteArray(BlobsTable.data)
 * // data.isNotEmpty()
 * ```
 */
fun ResultRow.getByteArray(expression: Expression<*>): ByteArray =
    getByteArrayOrNull(expression) ?: requiredError(expression, "ByteArray")

/**
 * 표현식 값을 `ByteArray`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val data = row.getByteArrayOrNull(BlobsTable.data)
 * // data != null 또는 null
 * ```
 */
fun ResultRow.getByteArrayOrNull(expression: Expression<*>): ByteArray? =
    anyValueOrNull(expression).asByteArrayOrNull()

/**
 * 표현식 값을 `Date`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val createdAt = row.getDate(EventsTable.createdAt)
 * // createdAt.time > 0
 * ```
 */
fun ResultRow.getDate(expression: Expression<*>): Date =
    getDateOrNull(expression) ?: requiredError(expression, "Date")

/**
 * 표현식 값을 `Date`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val createdAt = row.getDateOrNull(EventsTable.createdAt)
 * // createdAt != null 또는 null
 * ```
 */
fun ResultRow.getDateOrNull(expression: Expression<*>): Date? =
    anyValueOrNull(expression).asDateOrNull()

/**
 * 표현식 값을 `Timestamp`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val ts = row.getTimestamp(LogsTable.timestamp)
 * // ts.time > 0
 * ```
 */
fun ResultRow.getTimestamp(expression: Expression<*>): Timestamp =
    getTimestampOrNull(expression) ?: requiredError(expression, "Timestamp")

/**
 * 표현식 값을 `Timestamp`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val ts = row.getTimestampOrNull(LogsTable.timestamp)
 * // ts != null 또는 null
 * ```
 */
fun ResultRow.getTimestampOrNull(expression: Expression<*>): Timestamp? =
    anyValueOrNull(expression).asTimestampOrNull()

/**
 * 표현식 값을 `Instant`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val createdAt = row.getInstant(EventsTable.createdAt)
 * // createdAt.epochSecond > 0
 * ```
 */
fun ResultRow.getInstant(expression: Expression<*>): Instant =
    getInstantOrNull(expression) ?: requiredError(expression, "Instant")

/**
 * 표현식 값을 `Instant`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val createdAt = row.getInstantOrNull(EventsTable.createdAt)
 * // createdAt != null 또는 null
 * ```
 */
fun ResultRow.getInstantOrNull(expression: Expression<*>): Instant? =
    anyValueOrNull(expression).asInstantOrNull()

/**
 * 표현식 값을 `LocalDate`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val birthday = row.getLocalDate(UsersTable.birthday)
 * // birthday.year >= 1900
 * ```
 */
fun ResultRow.getLocalDate(expression: Expression<*>): LocalDate =
    getLocalDateOrNull(expression) ?: requiredError(expression, "LocalDate")

/**
 * 표현식 값을 `LocalDate`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val birthday = row.getLocalDateOrNull(UsersTable.birthday)
 * // birthday != null 또는 null
 * ```
 */
fun ResultRow.getLocalDateOrNull(expression: Expression<*>): LocalDate? =
    anyValueOrNull(expression).asLocalDateOrNull()

/**
 * 표현식 값을 `LocalTime`으로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val openTime = row.getLocalTime(StoresTable.openTime)
 * // openTime.hour >= 0
 * ```
 */
fun ResultRow.getLocalTime(expression: Expression<*>): LocalTime =
    getLocalTimeOrNull(expression) ?: requiredError(expression, "LocalTime")

/**
 * 표현식 값을 `LocalTime`으로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val openTime = row.getLocalTimeOrNull(StoresTable.openTime)
 * // openTime != null 또는 null
 * ```
 */
fun ResultRow.getLocalTimeOrNull(expression: Expression<*>): LocalTime? =
    anyValueOrNull(expression).asLocalTimeOrNull()

/**
 * 표현식 값을 `LocalDateTime`으로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val createdAt = row.getLocalDateTime(OrdersTable.createdAt)
 * // createdAt.year >= 2000
 * ```
 */
fun ResultRow.getLocalDateTime(expression: Expression<*>): LocalDateTime =
    getLocalDateTimeOrNull(expression) ?: requiredError(expression, "LocalDateTime")

/**
 * 표현식 값을 `LocalDateTime`으로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val createdAt = row.getLocalDateTimeOrNull(OrdersTable.createdAt)
 * // createdAt != null 또는 null
 * ```
 */
fun ResultRow.getLocalDateTimeOrNull(expression: Expression<*>): LocalDateTime? =
    anyValueOrNull(expression).asLocalDateTimeOrNull()

/**
 * 표현식 값을 `OffsetDateTime`으로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val updatedAt = row.getOffsetDateTime(EventsTable.updatedAt)
 * // updatedAt.offset != null
 * ```
 */
fun ResultRow.getOffsetDateTime(expression: Expression<*>): OffsetDateTime =
    getOffsetDateTimeOrNull(expression) ?: requiredError(expression, "OffsetDateTime")

/**
 * 표현식 값을 `OffsetDateTime`으로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val updatedAt = row.getOffsetDateTimeOrNull(EventsTable.updatedAt)
 * // updatedAt != null 또는 null
 * ```
 */
fun ResultRow.getOffsetDateTimeOrNull(expression: Expression<*>): OffsetDateTime? =
    anyValueOrNull(expression).asOffsetDateTimeOrNull()

/**
 * 표현식 값을 `ZonedDateTime`으로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val scheduledAt = row.getZonedDateTime(MeetingsTable.scheduledAt)
 * // scheduledAt.zone != null
 * ```
 */
fun ResultRow.getZonedDateTime(expression: Expression<*>): ZonedDateTime =
    getZonedDateTimeOrNull(expression) ?: requiredError(expression, "ZonedDateTime")

/**
 * 표현식 값을 `ZonedDateTime`으로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val scheduledAt = row.getZonedDateTimeOrNull(MeetingsTable.scheduledAt)
 * // scheduledAt != null 또는 null
 * ```
 */
fun ResultRow.getZonedDateTimeOrNull(expression: Expression<*>): ZonedDateTime? =
    anyValueOrNull(expression).asZonedDateTimeOrNull()

/**
 * 표현식 값을 `UUID`로 변환하고 실패 시 예외를 던집니다.
 *
 * ```kotlin
 * val id = row.getUuid(DocumentsTable.id)
 * // id.toString().length == 36
 * ```
 */
fun ResultRow.getUuid(expression: Expression<*>): UUID =
    getUuidOrNull(expression) ?: requiredError(expression, "UUID")

/**
 * 표현식 값을 `UUID`로 변환하고 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val id = row.getUuidOrNull(DocumentsTable.id)
 * // id != null 또는 null
 * ```
 */
fun ResultRow.getUuidOrNull(expression: Expression<*>): UUID? =
    anyValueOrNull(expression).asUUIDOrNull()
