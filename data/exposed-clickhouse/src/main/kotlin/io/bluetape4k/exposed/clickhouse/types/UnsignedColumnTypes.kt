package io.bluetape4k.exposed.clickhouse.types

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import java.math.BigInteger

/**
 * ClickHouse `UInt8` 컬럼 타입. Kotlin [UByte] 와 매핑됩니다.
 *
 * JDBC 전송 시 Short 로 변환합니다 (UByte 는 0..255 범위).
 */
class ClickHouseUByteColumnType: ColumnType<UByte>() {
    override fun sqlType(): String = "UInt8"

    override fun valueFromDB(value: Any): UByte = when (value) {
        is UByte -> value
        is Number -> value.toInt().toUByte()
        is String -> value.toUByte()
        else -> error("Unexpected UInt8 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: UByte): Any = value.toShort()
}

/**
 * ClickHouse `UInt16` 컬럼 타입. Kotlin [UShort] 와 매핑됩니다.
 *
 * JDBC 전송 시 Int 로 변환합니다 (UShort 는 0..65535 범위).
 */
class ClickHouseUShortColumnType: ColumnType<UShort>() {
    override fun sqlType(): String = "UInt16"

    override fun valueFromDB(value: Any): UShort = when (value) {
        is UShort -> value
        is Number -> value.toLong().toUShort()
        is String -> value.toUShort()
        else -> error("Unexpected UInt16 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: UShort): Any = value.toInt()
}

/**
 * ClickHouse `UInt32` 컬럼 타입. Kotlin [UInt] 와 매핑됩니다.
 *
 * JDBC 전송 시 Long 으로 변환합니다 (UInt 는 0..4294967295 범위).
 */
class ClickHouseUIntColumnType: ColumnType<UInt>() {
    override fun sqlType(): String = "UInt32"

    override fun valueFromDB(value: Any): UInt = when (value) {
        is UInt -> value
        is Number -> value.toLong().toUInt()
        is String -> value.toUInt()
        else -> error("Unexpected UInt32 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: UInt): Any = value.toLong()
}

/**
 * ClickHouse `UInt64` 컬럼 타입. Kotlin [ULong] 와 매핑됩니다.
 *
 * JDBC 전송 시 Long 으로 변환합니다.
 * 주의: `2^63` 이상의 값은 [java.math.BigInteger] 기반의 [ClickHouseUInt64BigIntColumnType] 사용을 고려하세요.
 */
class ClickHouseULongColumnType: ColumnType<ULong>() {
    override fun sqlType(): String = "UInt64"

    override fun valueFromDB(value: Any): ULong = when (value) {
        is ULong -> value
        is Long -> value.toULong()
        is BigInteger -> {
            // 2^63 이상의 값은 Long.toULong()으로 truncation 없이 변환 가능 (ULong은 2^64-1까지 지원)
            // BigInteger가 ULong 범위(0..2^64-1)를 초과하면 명시적 에러
            require(value.signum() >= 0 && value.bitLength() <= 64) {
                "BigInteger value $value is out of ULong range (0..2^64-1). Use chUInt64BigInt() for values >= 2^63."
            }
            value.toLong().toULong()
        }
        is Number -> value.toLong().toULong()
        is String -> value.toULong()
        else -> error("Unexpected UInt64 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: ULong): Any = value.toLong()
}

/**
 * ClickHouse `UInt64` 컬럼 타입 — overflow-safe 변형. Kotlin [BigInteger] 와 매핑됩니다.
 *
 * `2^63` 이상의 UInt64 값을 안전히 다루어야 할 때 사용합니다.
 */
class ClickHouseUInt64BigIntColumnType: ColumnType<BigInteger>() {
    override fun sqlType(): String = "UInt64"

    override fun valueFromDB(value: Any): BigInteger = when (value) {
        is BigInteger -> value
        is Number -> BigInteger.valueOf(value.toLong())
        is String -> BigInteger(value)
        else -> error("Unexpected UInt64 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: BigInteger): Any = value
}

// ────────────────────────────────────────────────────────────────────────────────
// Table extension builders
// ────────────────────────────────────────────────────────────────────────────────

/**
 * ClickHouse `UInt8` 컬럼을 등록합니다. [UByte] (0..255) 와 매핑됩니다.
 *
 * ```kotlin
 * object EventTable : Table("events") {
 *     val status = chUByte("status")
 * }
 * ```
 *
 * @param name 컬럼명
 */
fun Table.chUByte(name: String): Column<UByte> =
    registerColumn(name, ClickHouseUByteColumnType())

/**
 * ClickHouse `UInt16` 컬럼을 등록합니다. [UShort] (0..65535) 와 매핑됩니다.
 *
 * ```kotlin
 * object MetricsTable : Table("metrics") {
 *     val port = chUShort("port")
 * }
 * ```
 *
 * @param name 컬럼명
 */
fun Table.chUShort(name: String): Column<UShort> =
    registerColumn(name, ClickHouseUShortColumnType())

/**
 * ClickHouse `UInt32` 컬럼을 등록합니다. [UInt] (0..4294967295) 와 매핑됩니다.
 *
 * ```kotlin
 * object EventTable : Table("events") {
 *     val userId = chUInt("user_id")
 * }
 * ```
 *
 * @param name 컬럼명
 */
fun Table.chUInt(name: String): Column<UInt> =
    registerColumn(name, ClickHouseUIntColumnType())

/**
 * ClickHouse `UInt64` 컬럼을 등록합니다. [ULong] 매핑 (성능 우선).
 *
 * `2^63` 이상의 값이 필요한 경우 [chUInt64BigInt]를 사용하세요.
 *
 * ```kotlin
 * object EventTable : Table("events") {
 *     val eventId = chULong("event_id")
 * }
 * ```
 *
 * @param name 컬럼명
 */
fun Table.chULong(name: String): Column<ULong> =
    registerColumn(name, ClickHouseULongColumnType())

/**
 * ClickHouse `UInt64` 컬럼을 등록합니다. [BigInteger] 매핑 (overflow-safe).
 *
 * `2^63` 이상의 UInt64 값을 안전하게 처리해야 할 때 사용합니다.
 *
 * ```kotlin
 * object EventTable : Table("events") {
 *     val largeCounter = chUInt64BigInt("large_counter")
 * }
 * ```
 *
 * @param name 컬럼명
 */
fun Table.chUInt64BigInt(name: String): Column<BigInteger> =
    registerColumn(name, ClickHouseUInt64BigIntColumnType())
