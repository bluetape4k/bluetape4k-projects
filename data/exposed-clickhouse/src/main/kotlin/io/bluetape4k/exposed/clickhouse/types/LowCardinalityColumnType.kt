package io.bluetape4k.exposed.clickhouse.types

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * ClickHouse `LowCardinality(T)` 컬럼 타입.
 *
 * 반복값이 많은 컬럼의 스토리지/쿼리 성능을 최적화합니다.
 * inner 타입은 일반적으로 `String` 또는 `FixedString(N)` 을 권장합니다.
 *
 * @property inner inner 컬럼 타입
 */
class LowCardinalityColumnType<T: Any>(val inner: ColumnType<T>): ColumnType<T>() {
    override fun sqlType(): String = "LowCardinality(${inner.sqlType()})"

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): T = inner.valueFromDB(value) as T
    override fun notNullValueToDB(value: T): Any = inner.notNullValueToDB(value)
}

/**
 * `LowCardinality(String)` 컬럼을 등록합니다. (권장 — 안전한 inner 타입)
 */
fun Table.lowCardinalityString(name: String): Column<String> =
    registerColumn(name, LowCardinalityColumnType(ClickHouseStringColumnType()))

/**
 * `LowCardinality(T)` 컬럼을 등록합니다.
 *
 * inner 타입의 책임은 호출자가 보장해야 합니다.
 */
fun <T: Any> Table.lowCardinality(name: String, innerType: ColumnType<T>): Column<T> =
    registerColumn(name, LowCardinalityColumnType(innerType))
