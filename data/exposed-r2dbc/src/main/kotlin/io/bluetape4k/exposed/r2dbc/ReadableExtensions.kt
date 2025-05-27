package io.bluetape4k.exposed.r2dbc

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

fun Readable.getString(index: Int): String = getAs<String>(index)
fun Readable.getString(label: String): String = getAs<String>(label)
fun Readable.getStringOrNull(index: Int): String? = getAsOrNull<String>(index)
fun Readable.getStringOrNull(label: String): String? = getAsOrNull<String>(label)

fun Readable.getBoolean(index: Int): Boolean = getAs<Boolean>(index)
fun Readable.getBoolean(label: String): Boolean = getAs<Boolean>(label)
fun Readable.getBooleanOrNull(index: Int): Boolean? = getAsOrNull<Boolean>(index)
fun Readable.getBooleanOrNull(label: String): Boolean? = getAsOrNull<Boolean>(label)

fun Readable.getChar(index: Int): Char = getAs<Char>(index)
fun Readable.getChar(label: String): Char = getAs<Char>(label)
fun Readable.getCharOrNull(index: Int): Char? = getAsOrNull<Char>(index)
fun Readable.getCharOrNull(label: String): Char? = getAsOrNull<Char>(label)

fun Readable.getByte(index: Int): Byte = getAs<Byte>(index)
fun Readable.getByte(label: String): Byte = getAs<Byte>(label)
fun Readable.getByteOrNull(index: Int): Byte? = getAsOrNull<Byte>(index)
fun Readable.getByteOrNull(label: String): Byte? = getAsOrNull<Byte>(label)

fun Readable.getShort(index: Int): Short = getAs<Short>(index)
fun Readable.getShort(label: String): Short = getAs<Short>(label)
fun Readable.getShortOrNull(index: Int): Short? = getAsOrNull<Short>(index)
fun Readable.getShortOrNull(label: String): Short? = getAsOrNull<Short>(label)

fun Readable.getInt(index: Int): Int = getAs<Int>(index)
fun Readable.getInt(label: String): Int = getAs<Int>(label)
fun Readable.getIntOrNull(index: Int): Int? = getAsOrNull<Int>(index)
fun Readable.getIntOrNull(label: String): Int? = getAsOrNull<Int>(label)

fun Readable.getLong(index: Int): Long = getAs<Long>(index)
fun Readable.getLong(label: String): Long = getAs<Long>(label)
fun Readable.getLongOrNull(index: Int): Long? = getAsOrNull<Long>(index)
fun Readable.getLongOrNull(label: String): Long? = getAsOrNull<Long>(label)

fun Readable.getFloat(index: Int): Float = getAs<Float>(index)
fun Readable.getFloat(label: String): Float = getAs<Float>(label)
fun Readable.getFloatOrNull(index: Int): Float? = getAsOrNull<Float>(index)
fun Readable.getFloatOrNull(label: String): Float? = getAsOrNull<Float>(label)

fun Readable.getDouble(index: Int): Double = getAs<Double>(index)
fun Readable.getDouble(label: String): Double = getAs<Double>(label)
fun Readable.getDoubleOrNull(index: Int): Double? = getAsOrNull<Double>(index)
fun Readable.getDoubleOrNull(label: String): Double? = getAsOrNull<Double>(label)

fun Readable.getBigDecimal(index: Int): BigDecimal = getAs<BigDecimal>(index)
fun Readable.getBigDecimal(label: String): BigDecimal = getAs<BigDecimal>(label)
fun Readable.getBigDecimalOrNull(index: Int): BigDecimal? = getAsOrNull<BigDecimal>(index)
fun Readable.getBigDecimalOrNull(label: String): BigDecimal? = getAsOrNull<BigDecimal>(label)

fun Readable.getByteArray(index: Int): ByteArray = getAs<ByteArray>(index)
fun Readable.getByteArray(label: String): ByteArray = getAs<ByteArray>(label)
fun Readable.getByteArrayOrNull(index: Int): ByteArray? = getAsOrNull<ByteArray>(index)
fun Readable.getByteArrayOrNull(label: String): ByteArray? = getAsOrNull<ByteArray>(label)

fun Readable.getDate(index: Int): Date = getAs<Date>(index)
fun Readable.getDate(label: String): Date = getAs<Date>(label)
fun Readable.getDateOrNull(index: Int): Date? = getAsOrNull<Date>(index)
fun Readable.getDateOrNull(label: String): Date? = getAsOrNull<Date>(label)

fun Readable.getTimestamp(index: Int): Timestamp = getAs<Timestamp>(index)
fun Readable.getTimestamp(label: String): Timestamp = getAs<Timestamp>(label)
fun Readable.getTimestampOrNull(index: Int): Timestamp? = getAsOrNull<Timestamp>(index)
fun Readable.getTimestampOrNull(label: String): Timestamp? = getAsOrNull<Timestamp>(label)

fun Readable.getInstant(index: Int): Instant = getAs<Instant>(index)
fun Readable.getInstant(label: String): Instant = getAs<Instant>(label)
fun Readable.getInstantOrNull(index: Int): Instant? = getAsOrNull<Instant>(index)
fun Readable.getInstantOrNull(label: String): Instant? = getAsOrNull<Instant>(label)

fun Readable.getLocalDate(index: Int): LocalDate = getAs<LocalDate>(index)
fun Readable.getLocalDate(label: String): LocalDate = getAs<LocalDate>(label)
fun Readable.getLocalDateOrNull(index: Int): LocalDate? = getAsOrNull<LocalDate>(index)
fun Readable.getLocalDateOrNull(label: String): LocalDate? = getAsOrNull<LocalDate>(label)

fun Readable.getLocalTime(index: Int): LocalTime = getAs<LocalTime>(index)
fun Readable.getLocalTime(label: String): LocalTime = getAs<LocalTime>(label)
fun Readable.getLocalTimeOrNull(index: Int): LocalTime? = getAsOrNull<LocalTime>(index)
fun Readable.getLocalTimeOrNull(label: String): LocalTime? = getAsOrNull<LocalTime>(label)

fun Readable.getLocalDateTime(index: Int): LocalDateTime = getAs<LocalDateTime>(index)
fun Readable.getLocalDateTime(label: String): LocalDateTime = getAs<LocalDateTime>(label)
fun Readable.getLocalDateTimeOrNull(index: Int): LocalDateTime? = getAsOrNull<LocalDateTime>(index)
fun Readable.getLocalDateTimeOrNull(label: String): LocalDateTime? = getAsOrNull<LocalDateTime>(label)

fun Readable.getOffsetDateTime(index: Int): OffsetDateTime = getAs<OffsetDateTime>(index)
fun Readable.getOffsetDateTime(label: String): OffsetDateTime = getAs<OffsetDateTime>(label)
fun Readable.getOffsetDateTimeOrNull(index: Int): OffsetDateTime? = getAsOrNull<OffsetDateTime>(index)
fun Readable.getOffsetDateTimeOrNull(label: String): OffsetDateTime? = getAsOrNull<OffsetDateTime>(label)

fun Readable.getUuid(index: Int): UUID = getAs<UUID>(index)
fun Readable.getUuid(label: String): UUID = getAs<UUID>(label)
fun Readable.getUuidOrNull(index: Int): UUID? = getAsOrNull<UUID>(index)
fun Readable.getUuidOrNull(label: String): UUID? = getAsOrNull<UUID>(label)
