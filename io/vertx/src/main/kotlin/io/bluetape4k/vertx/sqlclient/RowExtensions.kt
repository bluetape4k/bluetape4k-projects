package io.bluetape4k.vertx.sqlclient

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.data.Numeric
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.temporal.Temporal
import java.util.*

/**
 * 지정한 인덱스의 컬럼이 존재하는지 여부를 반환합니다.
 *
 * ```kotlin
 * val exists = row.hasColumn(0)
 * // exists == true  (첫 번째 컬럼이 있는 경우)
 * ```
 */
fun Row.hasColumn(index: Int): Boolean = index in 0 until size()

/**
 * 지정한 이름의 컬럼이 존재하는지 여부를 반환합니다.
 *
 * ```kotlin
 * val exists = row.hasColumn("name")
 * // exists == true  (name 컬럼이 있는 경우)
 * ```
 */
fun Row.hasColumn(columnName: String): Boolean = hasColumn(getColumnIndex(columnName))

/**
 * 지정한 컬럼의 값을 [T] 타입으로 변환하여 반환합니다. 변환 실패 시 null을 반환합니다.
 *
 * ```kotlin
 * val name: String? = row.valueAs<String>("name")
 * // name == "Alice" 또는 null (컬럼이 없거나 타입 불일치)
 * ```
 */
inline fun <reified T: Any> Row.valueAs(columnName: String): T? = getValue(columnName) as? T

/**
 * 지정한 컬럼이 존재하면 값을 반환하고, 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val value = row.getValueOrNull("optional_column")
 * // value == null  (컬럼이 없는 경우)
 * ```
 */
fun Row.getValueOrNull(columnName: String): Any? = if (hasColumn(columnName)) getValue(columnName) else null

/** [columnName] 컬럼이 존재하면 Boolean 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getBooleanOrNull(columnName: String): Boolean? = if (hasColumn(columnName)) getBoolean(columnName) else null

/** [columnName] 컬럼이 존재하면 Short 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getShortOrNull(columnName: String): Short? = if (hasColumn(columnName)) getShort(columnName) else null

/** [columnName] 컬럼이 존재하면 Int 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getIntOrNull(columnName: String): Int? = if (hasColumn(columnName)) getInteger(columnName) else null

/** [columnName] 컬럼이 존재하면 Long 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getLongOrNull(columnName: String): Long? = if (hasColumn(columnName)) getLong(columnName) else null

/** [columnName] 컬럼이 존재하면 Float 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getFloatOrNull(columnName: String): Float? = if (hasColumn(columnName)) getFloat(columnName) else null

/** [columnName] 컬럼이 존재하면 Double 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getDoubleOrNull(columnName: String): Double? = if (hasColumn(columnName)) getDouble(columnName) else null

/** [columnName] 컬럼이 존재하면 [Numeric] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getNumericOrNull(columnName: String): Numeric? = if (hasColumn(columnName)) getNumeric(columnName) else null

/** [columnName] 컬럼이 존재하면 String 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getStringOrNull(columnName: String): String? = if (hasColumn(columnName)) getString(columnName) else null

/** [columnName] 컬럼이 존재하면 JSON 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getJsonOrNull(columnName: String): Any? = if (hasColumn(columnName)) getJson(columnName) else null

/** [columnName] 컬럼이 존재하면 [JsonObject] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getJsonObjectOrNull(columnName: String): JsonObject? =
    if (hasColumn(columnName)) getJsonObject(columnName) else null

/** [columnName] 컬럼이 존재하면 [JsonArray] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getJsonArrayOrNull(columnName: String): JsonArray? =
    if (hasColumn(columnName)) getJsonArray(columnName) else null

/** [columnName] 컬럼이 존재하면 [Temporal] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getTemporalOrNull(columnName: String): Temporal? = if (hasColumn(columnName)) getTemporal(columnName) else null

/** [columnName] 컬럼이 존재하면 [LocalDate] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getLocalDateOrNull(columnName: String): LocalDate? =
    if (hasColumn(columnName)) getLocalDate(columnName) else null

/** [columnName] 컬럼이 존재하면 [LocalTime] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getLocalTimeOrNull(columnName: String): LocalTime? =
    if (hasColumn(columnName)) getLocalTime(columnName) else null

/** [columnName] 컬럼이 존재하면 [LocalDateTime] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getLocalDateTimeOrNull(columnName: String): LocalDateTime? =
    if (hasColumn(columnName)) getLocalDateTime(columnName) else null

/** [columnName] 컬럼이 존재하면 [OffsetDateTime] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getOffsetDateTimeOrNull(columnName: String): OffsetDateTime? =
    if (hasColumn(columnName)) getOffsetDateTime(columnName) else null

/** [columnName] 컬럼이 존재하면 [Buffer] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getBufferOrNull(columnName: String): Buffer? = if (hasColumn(columnName)) getBuffer(columnName) else null

/** [columnName] 컬럼이 존재하면 [UUID] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getUUIDOrNull(columnName: String): UUID? = if (hasColumn(columnName)) getUUID(columnName) else null

/** [columnName] 컬럼이 존재하면 [BigDecimal] 값을 반환하고, 없으면 null을 반환합니다. */
fun Row.getBigDecimalOrNull(columnName: String): BigDecimal? =
    if (hasColumn(columnName)) getBigDecimal(columnName) else null

/** [columnName] 컬럼이 존재하면 Boolean 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfBooleansOrNull(columnName: String): BooleanArray? =
    if (hasColumn(columnName)) getArrayOfBooleans(columnName)?.toBooleanArray() else null

/** [columnName] 컬럼이 존재하면 Short 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfShortsOrNull(columnName: String): ShortArray? =
    if (hasColumn(columnName)) getArrayOfShorts(columnName)?.toShortArray() else null

/** [columnName] 컬럼이 존재하면 Int 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfIntegersOrNull(columnName: String): IntArray? =
    if (hasColumn(columnName)) getArrayOfIntegers(columnName)?.toIntArray() else null

/** [columnName] 컬럼이 존재하면 Long 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfLongsOrNull(columnName: String): LongArray? =
    if (hasColumn(columnName)) getArrayOfLongs(columnName)?.toLongArray() else null

/** [columnName] 컬럼이 존재하면 Float 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfFloatsOrNull(columnName: String): FloatArray? =
    if (hasColumn(columnName)) getArrayOfFloats(columnName)?.toFloatArray() else null

/** [columnName] 컬럼이 존재하면 Double 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfDoublesOrNull(columnName: String): DoubleArray? =
    if (hasColumn(columnName)) getArrayOfDoubles(columnName)?.toDoubleArray() else null

/** [columnName] 컬럼이 존재하면 [Numeric] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfNumericsOrNull(columnName: String): Array<Numeric>? =
    if (hasColumn(columnName)) getArrayOfNumerics(columnName) else null

/** [columnName] 컬럼이 존재하면 String 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfStringsOrNull(columnName: String): Array<String>? =
    if (hasColumn(columnName)) getArrayOfStrings(columnName) else null

/** [columnName] 컬럼이 존재하면 [JsonObject] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfJsonObjectsOrNull(columnName: String): Array<JsonObject>? =
    if (hasColumn(columnName)) getArrayOfJsonObjects(columnName) else null

/** [columnName] 컬럼이 존재하면 [JsonArray] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfJsonArraysOrNull(columnName: String): Array<JsonArray>? =
    if (hasColumn(columnName)) getArrayOfJsonArrays(columnName) else null

/** [columnName] 컬럼이 존재하면 [Temporal] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfTemporalsOrNull(columnName: String): Array<Temporal>? =
    if (hasColumn(columnName)) getArrayOfTemporals(columnName) else null

/** [columnName] 컬럼이 존재하면 [LocalDate] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfLocalDatesOrNull(columnName: String): Array<LocalDate>? =
    if (hasColumn(columnName)) getArrayOfLocalDates(columnName) else null

/** [columnName] 컬럼이 존재하면 [LocalTime] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfLocalTimesOrNull(columnName: String): Array<LocalTime>? =
    if (hasColumn(columnName)) getArrayOfLocalTimes(columnName) else null

/** [columnName] 컬럼이 존재하면 [LocalDateTime] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfLocalDateTimesOrNull(columnName: String): Array<LocalDateTime>? =
    if (hasColumn(columnName)) getArrayOfLocalDateTimes(columnName) else null

/** [columnName] 컬럼이 존재하면 [OffsetDateTime] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfOffsetDatesTimesOrNull(columnName: String): Array<OffsetDateTime>? =
    if (hasColumn(columnName)) getArrayOfOffsetDateTimes(columnName) else null

/** [columnName] 컬럼이 존재하면 [Buffer] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfBuffersOrNull(columnName: String): Array<Buffer>? =
    if (hasColumn(columnName)) getArrayOfBuffers(columnName) else null

/** [columnName] 컬럼이 존재하면 [UUID] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfUUIDsOrNull(columnName: String): Array<UUID>? =
    if (hasColumn(columnName)) getArrayOfUUIDs(columnName) else null

/** [columnName] 컬럼이 존재하면 [BigDecimal] 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfBigDecimalsOrNull(columnName: String): Array<BigDecimal>? =
    if (hasColumn(columnName)) getArrayOfBigDecimals(columnName) else null

/** [columnName] 컬럼이 존재하면 JSON 배열을 반환하고, 없으면 null을 반환합니다. */
fun Row.getArrayOfJsonsOrNull(columnName: String): Array<Any>? =
    if (hasColumn(columnName)) getArrayOfJsons(columnName) else null

/**
 * [columnName] 컬럼이 존재하면 [T] 타입으로 변환하여 반환하고, 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val id: Long? = row.getOrNull<Long>("id")
 * // id == 42L 또는 null (컬럼 없는 경우)
 * ```
 */
inline fun <reified T: Any> Row.getOrNull(columnName: String): T? =
    if (hasColumn(columnName)) get(T::class.java, columnName) else null

/**
 * 현재 Row를 JSON 문자열로 인코딩합니다.
 *
 * ```kotlin
 * val json = row.jsonEncode()
 * // json == "{\"id\":1,\"name\":\"Alice\"}"
 * ```
 */
fun Row.jsonEncode(): String = toJson().encode()

/**
 * [RowSet]의 모든 행을 JSON 문자열 목록으로 변환하여 반환합니다.
 *
 * ```kotlin
 * val json = rowSet.jsonEncode()
 * // json == "{\"id\":1,...}, {\"id\":2,...}"
 * ```
 */
fun RowSet<Row>.jsonEncode(): String = joinToString { it.jsonEncode() }
