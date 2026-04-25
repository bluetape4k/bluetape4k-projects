package io.bluetape4k.exposed.clickhouse.types

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table

/**
 * ClickHouse `Array(T)` 컬럼 타입.
 *
 * Kotlin [List]<T> 와 매핑됩니다.
 *
 * ## 주의
 * - `Array(Nullable(T))` 등 nullable element 는 미지원합니다 (null element 발견 시 명확한 오류).
 * - JDBC 가 [java.sql.Array], [List], 또는 native Java 배열로 반환할 수 있어 모두 방어적으로 처리합니다.
 *
 * @property inner 원소 컬럼 타입
 */
@Suppress("UNCHECKED_CAST")
class ClickHouseArrayColumnType<T: Any>(val inner: ColumnType<T>): ColumnType<List<T>>() {
    override fun sqlType(): String = "Array(${inner.sqlType()})"

    override fun valueFromDB(value: Any): List<T> = when (value) {
        is List<*> -> value.map { elem ->
            if (elem == null) error("Array element is null — Array(Nullable(T)) is not supported")
            inner.valueFromDB(elem) as T
        }
        is Array<*> -> value.map { elem ->
            if (elem == null) error("Array element is null — Array(Nullable(T)) is not supported")
            inner.valueFromDB(elem) as T
        }
        is java.sql.Array -> valueFromDB(value.array)
        is String -> error("Array value returned as String literal '$value' — unsupported. Report this as a ClickHouse JDBC issue.")
        else -> error("Unexpected Array value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: List<T>): Any =
        value.map { inner.notNullValueToDB(it) }.toTypedArray()
}

/** `Array(T)` 컬럼을 등록합니다. */
fun <T: Any> Table.chArray(name: String, innerType: ColumnType<T>): Column<List<T>> =
    registerColumn(name, ClickHouseArrayColumnType(innerType))
