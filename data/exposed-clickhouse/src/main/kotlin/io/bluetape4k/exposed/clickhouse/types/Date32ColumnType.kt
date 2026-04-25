package io.bluetape4k.exposed.clickhouse.types

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import java.time.LocalDate

/**
 * ClickHouse `Date32` 컬럼 타입.
 *
 * `Date` (1970-01-01 ~ 2149-06-06) 보다 확장된 범위 (1900-01-01 ~ 2299-12-31) 의 날짜를 [LocalDate]로 매핑합니다.
 */
class Date32ColumnType: ColumnType<LocalDate>() {
    override fun sqlType(): String = "Date32"

    override fun valueFromDB(value: Any): LocalDate = when (value) {
        is LocalDate -> value
        is java.sql.Date -> value.toLocalDate()
        is String -> LocalDate.parse(value)
        else -> error("Unexpected Date32 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: LocalDate): Any = java.sql.Date.valueOf(value)
}

/** `Date32` 컬럼을 등록합니다. */
fun Table.date32(name: String): Column<LocalDate> =
    registerColumn(name, Date32ColumnType())
