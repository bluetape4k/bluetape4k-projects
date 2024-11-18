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


fun Row.hasColumn(index: Int): Boolean = index in 0 until size()
fun Row.hasColumn(columnName: String): Boolean = hasColumn(getColumnIndex(columnName))

inline fun <reified T: Any> Row.valueAs(columnName: String): T? {
    return getValue(columnName) as? T
}

fun Row.getValueOrNull(columnName: String): Any? {
    return if (hasColumn(columnName)) getValue(columnName) else null
}

fun Row.getBooleanOrNull(columnName: String): Boolean? {
    return if (hasColumn(columnName)) getBoolean(columnName) else null
}

fun Row.getShortOrNull(columnName: String): Short? {
    return if (hasColumn(columnName)) getShort(columnName) else null
}

fun Row.getIntOrNull(columnName: String): Int? {
    return if (hasColumn(columnName)) getInteger(columnName) else null
}

fun Row.getLongOrNull(columnName: String): Long? {
    return if (hasColumn(columnName)) getLong(columnName) else null
}

fun Row.getFloatOrNull(columnName: String): Float? {
    return if (hasColumn(columnName)) getFloat(columnName) else null
}

fun Row.getDoubleOrNull(columnName: String): Double? {
    return if (hasColumn(columnName)) getDouble(columnName) else null
}

fun Row.getNumericOrNull(columnName: String): Numeric? {
    return if (hasColumn(columnName)) getNumeric(columnName) else null
}

fun Row.getStringOrNull(columnName: String): String? {
    return if (hasColumn(columnName)) getString(columnName) else null
}

fun Row.getJsonOrNull(columnName: String): Any? {
    return if (hasColumn(columnName)) getJson(columnName) else null
}

fun Row.getJsonObjectOrNull(columnName: String): JsonObject? {
    return if (hasColumn(columnName)) getJsonObject(columnName) else null
}

fun Row.getJsonArrayOrNull(columnName: String): JsonArray? {
    return if (hasColumn(columnName)) getJsonArray(columnName) else null
}

fun Row.getTemporalOrNull(columnName: String): Temporal? {
    return if (hasColumn(columnName)) getTemporal(columnName) else null
}

fun Row.getLocalDateOrNull(columnName: String): LocalDate? {
    return if (hasColumn(columnName)) getLocalDate(columnName) else null
}

fun Row.getLocalTimeOrNull(columnName: String): LocalTime? {
    return if (hasColumn(columnName)) getLocalTime(columnName) else null
}

fun Row.getLocalDateTimeOrNull(columnName: String): LocalDateTime? {
    return if (hasColumn(columnName)) getLocalDateTime(columnName) else null
}

fun Row.getOffsetDateTimeOrNull(columnName: String): OffsetDateTime? {
    return if (hasColumn(columnName)) getOffsetDateTime(columnName) else null
}

fun Row.getBufferOrNull(columnName: String): Buffer? {
    return if (hasColumn(columnName)) getBuffer(columnName) else null
}

fun Row.getUUIDOrNull(columnName: String): UUID? {
    return if (hasColumn(columnName)) getUUID(columnName) else null
}

fun Row.getBigDecimalOrNull(columnName: String): BigDecimal? {
    return if (hasColumn(columnName)) getBigDecimal(columnName) else null
}

fun Row.getArrayOfBooleansOrNull(columnName: String): BooleanArray? {
    return if (hasColumn(columnName)) getArrayOfBooleans(columnName)?.toBooleanArray() else null
}

fun Row.getArrayOfShortsOrNull(columnName: String): ShortArray? {
    return if (hasColumn(columnName)) getArrayOfShorts(columnName)?.toShortArray() else null
}

fun Row.getArrayOfIntegersOrNull(columnName: String): IntArray? {
    return if (hasColumn(columnName)) getArrayOfIntegers(columnName)?.toIntArray() else null
}

fun Row.getArrayOfLongsOrNull(columnName: String): LongArray? {
    return if (hasColumn(columnName)) getArrayOfLongs(columnName)?.toLongArray() else null
}

fun Row.getArrayOfFloatsOrNull(columnName: String): FloatArray? {
    return if (hasColumn(columnName)) getArrayOfFloats(columnName)?.toFloatArray() else null
}

fun Row.getArrayOfDoublesOrNull(columnName: String): DoubleArray? {
    return if (hasColumn(columnName)) getArrayOfDoubles(columnName)?.toDoubleArray() else null
}

fun Row.getArrayOfNumericsOrNull(columnName: String): Array<Numeric>? {
    return if (hasColumn(columnName)) getArrayOfNumerics(columnName) else null
}

fun Row.getArrayOfStringsOrNull(columnName: String): Array<String>? {
    return if (hasColumn(columnName)) getArrayOfStrings(columnName) else null
}

fun Row.getArrayOfJsonObjectsOrNull(columnName: String): Array<JsonObject>? {
    return if (hasColumn(columnName)) getArrayOfJsonObjects(columnName) else null
}

fun Row.getArrayOfJsonArraysOrNull(columnName: String): Array<JsonArray>? {
    return if (hasColumn(columnName)) getArrayOfJsonArrays(columnName) else null
}

fun Row.getArrayOfTemporalsOrNull(columnName: String): Array<Temporal>? {
    return if (hasColumn(columnName)) getArrayOfTemporals(columnName) else null
}

fun Row.getArrayOfLocalDatesOrNull(columnName: String): Array<LocalDate>? {
    return if (hasColumn(columnName)) getArrayOfLocalDates(columnName) else null
}

fun Row.getArrayOfLocalTimesOrNull(columnName: String): Array<LocalTime>? {
    return if (hasColumn(columnName)) getArrayOfLocalTimes(columnName) else null
}

fun Row.getArrayOfLocalDateTimesOrNull(columnName: String): Array<LocalDateTime>? {
    return if (hasColumn(columnName)) getArrayOfLocalDateTimes(columnName) else null
}

fun Row.getArrayOfOffsetDatesTimesOrNull(columnName: String): Array<OffsetDateTime>? {
    return if (hasColumn(columnName)) getArrayOfOffsetDateTimes(columnName) else null
}

fun Row.getArrayOfBuffersOrNull(columnName: String): Array<Buffer>? {
    return if (hasColumn(columnName)) getArrayOfBuffers(columnName) else null
}

fun Row.getArrayOfUUIDsOrNull(columnName: String): Array<UUID>? {
    return if (hasColumn(columnName)) getArrayOfUUIDs(columnName) else null
}

fun Row.getArrayOfBigDecimalsOrNull(columnName: String): Array<BigDecimal>? {
    return if (hasColumn(columnName)) getArrayOfBigDecimals(columnName) else null
}

fun Row.getArrayOfJsonsOrNull(columnName: String): Array<Any>? {
    return if (hasColumn(columnName)) getArrayOfJsons(columnName) else null
}

inline fun <reified T: Any> Row.getOrNull(columnName: String): T? {
    return if (hasColumn(columnName)) get(T::class.java, columnName) else null
}

fun Row.jsonEncode(): String = toJson().encode()

fun RowSet<Row>.jsonEncode(): String = joinToString { it.jsonEncode() }
