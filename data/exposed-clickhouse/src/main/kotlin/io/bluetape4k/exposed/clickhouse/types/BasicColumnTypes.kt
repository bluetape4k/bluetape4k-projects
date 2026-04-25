package io.bluetape4k.exposed.clickhouse.types

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * ClickHouse `String` 컬럼 타입.
 *
 * 가변 길이 문자열을 저장합니다.
 */
class ClickHouseStringColumnType: ColumnType<String>() {
    override fun sqlType(): String = "String"

    override fun valueFromDB(value: Any): String = when (value) {
        is String -> value
        else -> value.toString()
    }

    override fun notNullValueToDB(value: String): Any = value
}

/**
 * ClickHouse `FixedString(N)` 컬럼 타입.
 *
 * 고정 길이 [length] 의 바이트 길이를 가지는 문자열을 저장합니다.
 *
 * @property length 문자열의 고정 바이트 길이.
 */
class ClickHouseFixedStringColumnType(val length: Int): ColumnType<String>() {
    init {
        require(length > 0) { "FixedString length must be > 0: $length" }
    }

    override fun sqlType(): String = "FixedString($length)"

    override fun valueFromDB(value: Any): String = when (value) {
        is String -> value
        else -> value.toString()
    }

    override fun notNullValueToDB(value: String): Any = value
}

/**
 * ClickHouse `Float32` 컬럼 타입. Kotlin [Float] 와 매핑됩니다.
 */
class ClickHouseFloat32ColumnType: ColumnType<Float>() {
    override fun sqlType(): String = "Float32"

    override fun valueFromDB(value: Any): Float = when (value) {
        is Float -> value
        is Number -> value.toFloat()
        is String -> value.toFloat()
        else -> error("Unexpected Float32 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: Float): Any = value
}

/**
 * ClickHouse `Float64` 컬럼 타입. Kotlin [Double] 와 매핑됩니다.
 */
class ClickHouseFloat64ColumnType: ColumnType<Double>() {
    override fun sqlType(): String = "Float64"

    override fun valueFromDB(value: Any): Double = when (value) {
        is Double -> value
        is Number -> value.toDouble()
        is String -> value.toDouble()
        else -> error("Unexpected Float64 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: Double): Any = value
}

/**
 * ClickHouse `Int8` 컬럼 타입. Kotlin [Byte] 와 매핑됩니다.
 */
class ClickHouseInt8ColumnType: ColumnType<Byte>() {
    override fun sqlType(): String = "Int8"

    override fun valueFromDB(value: Any): Byte = when (value) {
        is Byte -> value
        is Number -> value.toByte()
        is String -> value.toByte()
        else -> error("Unexpected Int8 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: Byte): Any = value
}

/**
 * ClickHouse `Int16` 컬럼 타입. Kotlin [Short] 와 매핑됩니다.
 */
class ClickHouseInt16ColumnType: ColumnType<Short>() {
    override fun sqlType(): String = "Int16"

    override fun valueFromDB(value: Any): Short = when (value) {
        is Short -> value
        is Number -> value.toShort()
        is String -> value.toShort()
        else -> error("Unexpected Int16 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: Short): Any = value
}

/**
 * ClickHouse `Int32` 컬럼 타입. Kotlin [Int] 와 매핑됩니다.
 */
class ClickHouseInt32ColumnType: ColumnType<Int>() {
    override fun sqlType(): String = "Int32"

    override fun valueFromDB(value: Any): Int = when (value) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toInt()
        else -> error("Unexpected Int32 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: Int): Any = value
}

/**
 * ClickHouse `Int64` 컬럼 타입. Kotlin [Long] 와 매핑됩니다.
 */
class ClickHouseInt64ColumnType: ColumnType<Long>() {
    override fun sqlType(): String = "Int64"

    override fun valueFromDB(value: Any): Long = when (value) {
        is Long -> value
        is Number -> value.toLong()
        is String -> value.toLong()
        else -> error("Unexpected Int64 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: Long): Any = value
}

/**
 * ClickHouse `Nullable(T)` 컬럼 타입.
 *
 * 내부 [inner] 타입을 nullable 로 wrap 합니다.
 *
 * 주의: Exposed 가 추가로 ` NULL` suffix 를 붙이지 않도록 [nullable] 은 false 로 유지합니다.
 *
 * @param T inner 타입의 non-null 형태.
 * @property inner wrap 대상 컬럼 타입.
 */
class ClickHouseNullableColumnType<T: Any>(val inner: ColumnType<T>): ColumnType<T>() {

    init {
        // Exposed 레이어에서 null 값 설정을 허용. DDL의 NULL 키워드는 sanitizeForClickHouse가 제거.
        nullable = true
    }

    override fun sqlType(): String = "Nullable(${inner.sqlType()})"

    override fun valueFromDB(value: Any): T? = inner.valueFromDB(value)

    override fun notNullValueToDB(value: T): Any = inner.notNullValueToDB(value)
}

// ────────────────────────────────────────────────────────────────────────────────
// Table extension builders
// ────────────────────────────────────────────────────────────────────────────────

/** `String` 컬럼 등록 */
fun Table.chString(name: String): Column<String> =
    registerColumn(name, ClickHouseStringColumnType())

/** `FixedString([length])` 컬럼 등록 */
fun Table.fixedString(name: String, length: Int): Column<String> =
    registerColumn(name, ClickHouseFixedStringColumnType(length))

/** `Float32` 컬럼 등록 */
fun Table.chFloat32(name: String): Column<Float> =
    registerColumn(name, ClickHouseFloat32ColumnType())

/** `Float64` 컬럼 등록 */
fun Table.chFloat64(name: String): Column<Double> =
    registerColumn(name, ClickHouseFloat64ColumnType())

/** `Int8` 컬럼 등록 */
fun Table.chInt8(name: String): Column<Byte> =
    registerColumn(name, ClickHouseInt8ColumnType())

/** `Int16` 컬럼 등록 */
fun Table.chInt16(name: String): Column<Short> =
    registerColumn(name, ClickHouseInt16ColumnType())

/** `Int32` 컬럼 등록 */
fun Table.chInt32(name: String): Column<Int> =
    registerColumn(name, ClickHouseInt32ColumnType())

/** `Int64` 컬럼 등록 */
fun Table.chInt64(name: String): Column<Long> =
    registerColumn(name, ClickHouseInt64ColumnType())

/** `Nullable(T)` 컬럼 등록 — [innerType] 으로 inner 타입을 명시합니다. */
fun <T: Any> Table.chNullable(name: String, innerType: ColumnType<T>): Column<T?> =
    registerColumn<T?>(name, ClickHouseNullableColumnType(innerType))
