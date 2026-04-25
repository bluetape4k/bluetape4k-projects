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
        is BigInteger -> value.toLong().toULong()
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

/** `UInt8` 컬럼 등록 */
fun Table.chUByte(name: String): Column<UByte> =
    registerColumn(name, ClickHouseUByteColumnType())

/** `UInt16` 컬럼 등록 */
fun Table.chUShort(name: String): Column<UShort> =
    registerColumn(name, ClickHouseUShortColumnType())

/** `UInt32` 컬럼 등록 */
fun Table.chUInt(name: String): Column<UInt> =
    registerColumn(name, ClickHouseUIntColumnType())

/** `UInt64` 컬럼 등록 — [ULong] 매핑 (성능 우선) */
fun Table.chULong(name: String): Column<ULong> =
    registerColumn(name, ClickHouseULongColumnType())

/** `UInt64` 컬럼 등록 — [BigInteger] 매핑 (overflow-safe) */
fun Table.chUInt64BigInt(name: String): Column<BigInteger> =
    registerColumn(name, ClickHouseUInt64BigIntColumnType())
