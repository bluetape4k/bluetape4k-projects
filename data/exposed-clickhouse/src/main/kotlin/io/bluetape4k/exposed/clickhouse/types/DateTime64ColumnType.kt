package io.bluetape4k.exposed.clickhouse.types

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import java.time.Instant

/**
 * ClickHouse `DateTime64(precision, 'UTC')` 컬럼 타입.
 *
 * [Instant]를 정밀도(precision)에 따라 매핑합니다. 기본 [precision]은 3 (밀리초).
 *
 * @property precision 소수점 초 자릿수 (0~9, 기본값 3=밀리초)
 */
class DateTime64ColumnType(val precision: Int = 3): ColumnType<Instant>() {

    init {
        require(precision in 0..9) { "DateTime64 precision must be in 0..9: $precision" }
    }

    override fun sqlType(): String = "DateTime64($precision, 'UTC')"

    override fun valueFromDB(value: Any): Instant = when (value) {
        is Instant -> value
        is java.sql.Timestamp -> value.toInstant()
        is java.time.LocalDateTime -> value.toInstant(java.time.ZoneOffset.UTC)
        is java.time.OffsetDateTime -> value.toInstant()
        is Long -> Instant.ofEpochMilli(value)
        is String -> Instant.parse(value)
        else -> error("Unexpected DateTime64 value: $value (${value::class.simpleName})")
    }

    override fun notNullValueToDB(value: Instant): Any =
        java.sql.Timestamp.from(value)
}

/**
 * `DateTime64(precision, 'UTC')` 컬럼을 등록합니다. 기본 [precision]=3 (밀리초).
 */
fun Table.dateTime64(name: String, precision: Int = 3): Column<Instant> =
    registerColumn(name, DateTime64ColumnType(precision))
