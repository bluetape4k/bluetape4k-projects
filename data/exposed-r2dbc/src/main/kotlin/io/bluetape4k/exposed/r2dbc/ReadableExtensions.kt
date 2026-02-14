@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.r2dbc.spi.Blob
import io.r2dbc.spi.Readable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.*

/**
 * 지정한 [index] 컬럼 값을 [T]로 조회합니다.
 *
 * 값이 `null`이면, 컬럼 인덱스와 타입 정보를 포함한 예외를 발생시킵니다.
 */
inline fun <reified T: Any> Readable.getAs(index: Int): T =
    get(index, T::class.java) ?: error("Column[$index] is null. Expected type=${T::class.java.simpleName}.")

/**
 * 지정한 [name] 컬럼 값을 [T]로 조회합니다.
 *
 * 값이 `null`이면, 컬럼명과 타입 정보를 포함한 예외를 발생시킵니다.
 */
inline fun <reified T: Any> Readable.getAs(name: String): T =
    get(name, T::class.java) ?: error("Column[$name] is null. Expected type=${T::class.java.simpleName}.")

inline fun <reified T: Any> Readable.getAsOrNull(index: Int): T? = get(index, T::class.java)
inline fun <reified T: Any> Readable.getAsOrNull(name: String): T? = get(name, T::class.java)

inline fun Readable.getString(index: Int): String = getAs<String>(index)
inline fun Readable.getString(label: String): String = getAs<String>(label)
inline fun Readable.getStringOrNull(index: Int): String? = getAsOrNull<String>(index)
inline fun Readable.getStringOrNull(label: String): String? = getAsOrNull<String>(label)

inline fun Readable.getBoolean(index: Int): Boolean = getAs<Boolean>(index)
inline fun Readable.getBoolean(label: String): Boolean = getAs<Boolean>(label)
inline fun Readable.getBooleanOrNull(index: Int): Boolean? = getAsOrNull<Boolean>(index)
inline fun Readable.getBooleanOrNull(label: String): Boolean? = getAsOrNull<Boolean>(label)

inline fun Readable.getChar(index: Int): Char = getAs<Char>(index)
inline fun Readable.getChar(label: String): Char = getAs<Char>(label)
inline fun Readable.getCharOrNull(index: Int): Char? = getAsOrNull<Char>(index)
inline fun Readable.getCharOrNull(label: String): Char? = getAsOrNull<Char>(label)

inline fun Readable.getByte(index: Int): Byte = getAs<Byte>(index)
inline fun Readable.getByte(label: String): Byte = getAs<Byte>(label)
inline fun Readable.getByteOrNull(index: Int): Byte? = getAsOrNull<Byte>(index)
inline fun Readable.getByteOrNull(label: String): Byte? = getAsOrNull<Byte>(label)

inline fun Readable.getShort(index: Int): Short = getAs<Short>(index)
inline fun Readable.getShort(label: String): Short = getAs<Short>(label)
inline fun Readable.getShortOrNull(index: Int): Short? = getAsOrNull<Short>(index)
inline fun Readable.getShortOrNull(label: String): Short? = getAsOrNull<Short>(label)

inline fun Readable.getInt(index: Int): Int = getAs<Int>(index)
inline fun Readable.getInt(label: String): Int = getAs<Int>(label)
inline fun Readable.getIntOrNull(index: Int): Int? = getAsOrNull<Int>(index)
inline fun Readable.getIntOrNull(label: String): Int? = getAsOrNull<Int>(label)

inline fun Readable.getLong(index: Int): Long = getAs<Long>(index)
inline fun Readable.getLong(label: String): Long = getAs<Long>(label)
inline fun Readable.getLongOrNull(index: Int): Long? = getAsOrNull<Long>(index)
inline fun Readable.getLongOrNull(label: String): Long? = getAsOrNull<Long>(label)

inline fun Readable.getFloat(index: Int): Float = getAs<Float>(index)
inline fun Readable.getFloat(label: String): Float = getAs<Float>(label)
inline fun Readable.getFloatOrNull(index: Int): Float? = getAsOrNull<Float>(index)
inline fun Readable.getFloatOrNull(label: String): Float? = getAsOrNull<Float>(label)

inline fun Readable.getDouble(index: Int): Double = getAs<Double>(index)
inline fun Readable.getDouble(label: String): Double = getAs<Double>(label)
inline fun Readable.getDoubleOrNull(index: Int): Double? = getAsOrNull<Double>(index)
inline fun Readable.getDoubleOrNull(label: String): Double? = getAsOrNull<Double>(label)

inline fun Readable.getBigDecimal(index: Int): BigDecimal = getAs<BigDecimal>(index)
inline fun Readable.getBigDecimal(label: String): BigDecimal = getAs<BigDecimal>(label)
inline fun Readable.getBigDecimalOrNull(index: Int): BigDecimal? = getAsOrNull<BigDecimal>(index)
inline fun Readable.getBigDecimalOrNull(label: String): BigDecimal? = getAsOrNull<BigDecimal>(label)

inline fun Readable.getByteArray(index: Int): ByteArray = getAs<ByteArray>(index)
inline fun Readable.getByteArray(label: String): ByteArray = getAs<ByteArray>(label)
inline fun Readable.getByteArrayOrNull(index: Int): ByteArray? = getAsOrNull<ByteArray>(index)
inline fun Readable.getByteArrayOrNull(label: String): ByteArray? = getAsOrNull<ByteArray>(label)

inline fun Readable.getDate(index: Int): Date = getAs<Date>(index)
inline fun Readable.getDate(label: String): Date = getAs<Date>(label)
inline fun Readable.getDateOrNull(index: Int): Date? = getAsOrNull<Date>(index)
inline fun Readable.getDateOrNull(label: String): Date? = getAsOrNull<Date>(label)

inline fun Readable.getTimestamp(index: Int): Timestamp = getAs<Timestamp>(index)
inline fun Readable.getTimestamp(label: String): Timestamp = getAs<Timestamp>(label)
inline fun Readable.getTimestampOrNull(index: Int): Timestamp? = getAsOrNull<Timestamp>(index)
inline fun Readable.getTimestampOrNull(label: String): Timestamp? = getAsOrNull<Timestamp>(label)

inline fun Readable.getInstant(index: Int): Instant = getAs<Instant>(index)
inline fun Readable.getInstant(label: String): Instant = getAs<Instant>(label)
inline fun Readable.getInstantOrNull(index: Int): Instant? = getAsOrNull<Instant>(index)
inline fun Readable.getInstantOrNull(label: String): Instant? = getAsOrNull<Instant>(label)

inline fun Readable.getLocalDate(index: Int): LocalDate = getAs<LocalDate>(index)
inline fun Readable.getLocalDate(label: String): LocalDate = getAs<LocalDate>(label)
inline fun Readable.getLocalDateOrNull(index: Int): LocalDate? = getAsOrNull<LocalDate>(index)
inline fun Readable.getLocalDateOrNull(label: String): LocalDate? = getAsOrNull<LocalDate>(label)

inline fun Readable.getLocalTime(index: Int): LocalTime = getAs<LocalTime>(index)
inline fun Readable.getLocalTime(label: String): LocalTime = getAs<LocalTime>(label)
inline fun Readable.getLocalTimeOrNull(index: Int): LocalTime? = getAsOrNull<LocalTime>(index)
inline fun Readable.getLocalTimeOrNull(label: String): LocalTime? = getAsOrNull<LocalTime>(label)

inline fun Readable.getLocalDateTime(index: Int): LocalDateTime = getAs<LocalDateTime>(index)
inline fun Readable.getLocalDateTime(label: String): LocalDateTime = getAs<LocalDateTime>(label)
inline fun Readable.getLocalDateTimeOrNull(index: Int): LocalDateTime? = getAsOrNull<LocalDateTime>(index)
inline fun Readable.getLocalDateTimeOrNull(label: String): LocalDateTime? = getAsOrNull<LocalDateTime>(label)

inline fun Readable.getOffsetDateTime(index: Int): OffsetDateTime = getAs<OffsetDateTime>(index)
inline fun Readable.getOffsetDateTime(label: String): OffsetDateTime = getAs<OffsetDateTime>(label)
inline fun Readable.getOffsetDateTimeOrNull(index: Int): OffsetDateTime? = getAsOrNull<OffsetDateTime>(index)
inline fun Readable.getOffsetDateTimeOrNull(label: String): OffsetDateTime? = getAsOrNull<OffsetDateTime>(label)

inline fun Readable.getUuid(index: Int): UUID = getAs<UUID>(index)
inline fun Readable.getUuid(label: String): UUID = getAs<UUID>(label)
inline fun Readable.getUuidOrNull(index: Int): UUID? = getAsOrNull<UUID>(index)
inline fun Readable.getUuidOrNull(label: String): UUID? = getAsOrNull<UUID>(label)

/**
 * [index] 컬럼의 바이너리 값을 [ExposedBlob]으로 조회합니다.
 *
 * `exposed-r2dbc` 환경에서는 코루틴 기반 사용을 전제로 하므로 suspend API만 제공합니다.
 */
suspend fun Readable.getExposedBlob(index: Int): ExposedBlob =
    getExposedBlobOrNull(index) ?: error("Column[$index] is null or unsupported blob value type")

/**
 * [index] 컬럼의 바이너리 값을 [ExposedBlob]으로 조회합니다.
 *
 * `exposed-r2dbc` 환경에서는 코루틴 기반 사용을 전제로 하므로 suspend API만 제공합니다.
 */
suspend fun Readable.getExposedBlobOrNull(index: Int): ExposedBlob? {
    getByteArrayOrNull(index)?.let { return it.toExposedBlob() }
    return get(index).toExposedBlobOrNull()
}

/**
 * [label] 컬럼의 바이너리 값을 [ExposedBlob]으로 조회합니다.
 *
 * `exposed-r2dbc` 환경에서는 코루틴 기반 사용을 전제로 하므로 suspend API만 제공합니다.
 */
suspend fun Readable.getExposedBlob(label: String): ExposedBlob =
    getExposedBlobOrNull(label) ?: error("Column[$label] is null or unsupported blob value type")


/**
 * [label] 컬럼의 바이너리 값을 [ExposedBlob]으로 조회합니다.
 *
 * `exposed-r2dbc` 환경에서는 코루틴 기반 사용을 전제로 하므로 suspend API만 제공합니다.
 */
suspend fun Readable.getExposedBlobOrNull(label: String): ExposedBlob? {
    getByteArrayOrNull(label)?.let { return it.toExposedBlob() }
    return get(label).toExposedBlobOrNull()
}

private suspend fun Any?.toExposedBlobOrNull(): ExposedBlob? =
    when (this) {
        is ExposedBlob -> this
        is ByteArray   -> toExposedBlob()
        is ByteBuffer  -> {
            val view = slice()
            val bytes = ByteArray(view.remaining())
            view.get(bytes)
            bytes.toExposedBlob()
        }
        is InputStream -> toExposedBlob()
        is Blob        -> {
            ByteArrayOutputStream().use { out ->
                stream().asFlow()
                    .collect { buf ->
                        withContext(Dispatchers.IO) {
                            val bytes = ByteArray(buf.remaining())
                            buf.get(bytes)
                            out.write(bytes)
                        }
                    }
                out.toByteArray().toExposedBlob()
            }
        }
        else           -> null
    }
