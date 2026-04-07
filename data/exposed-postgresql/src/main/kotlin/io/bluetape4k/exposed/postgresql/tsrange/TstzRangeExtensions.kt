package io.bluetape4k.exposed.postgresql.tsrange

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ComparisonOp
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.time.Instant

/**
 * [TimestampRange] 타입의 컬럼을 테이블에 등록한다.
 *
 * PostgreSQL에서는 `TSTZRANGE` 네이티브 타입을, 그 외 DB에서는 `VARCHAR(120)`를 사용한다.
 *
 * ```kotlin
 * object EventTable: LongIdTable("events") {
 *     val period = tstzRange("period")
 * }
 * // EventTable.period.name == "period"
 * ```
 *
 * @param name 컬럼 이름
 * @return [TimestampRange] 타입의 [Column]
 */
fun Table.tstzRange(name: String): Column<TimestampRange> =
    registerColumn(name, TstzRangeColumnType())

/**
 * PostgreSQL `&&` 연산자를 나타내는 SQL 표현식.
 *
 * 두 TSTZRANGE 값이 겹치는지 확인한다.
 * PostgreSQL 전용이며 다른 dialect에서는 사용할 수 없다.
 *
 * SQL: `left_expr && right_expr`
 */
class TstzRangeOverlapsOp(
    expr1: Expression<*>,
    expr2: Expression<*>,
): ComparisonOp(expr1, expr2, "&&")

/**
 * PostgreSQL `@>` 연산자를 나타내는 SQL 표현식 (range가 instant를 포함).
 *
 * TSTZRANGE 값이 특정 timestamp를 포함하는지 확인한다.
 * PostgreSQL 전용이며 다른 dialect에서는 사용할 수 없다.
 *
 * SQL: `range_expr @> timestamp_expr::timestamptz`
 */
class TstzRangeContainsInstantOp(
    private val rangeExpr: Expression<TimestampRange>,
    private val instantExpr: Expression<Instant>,
): Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append(rangeExpr)
            append(" @> ")
            append(instantExpr)
            append("::timestamptz")
        }
    }
}

/**
 * PostgreSQL `@>` 연산자를 나타내는 SQL 표현식 (range가 range를 포함).
 *
 * TSTZRANGE 값이 다른 TSTZRANGE를 완전히 포함하는지 확인한다.
 * PostgreSQL 전용이며 다른 dialect에서는 사용할 수 없다.
 *
 * SQL: `left_expr @> right_expr`
 */
class TstzRangeContainsRangeOp(
    expr1: Expression<*>,
    expr2: Expression<*>,
): ComparisonOp(expr1, expr2, "@>")

/**
 * PostgreSQL `-|-` 연산자를 나타내는 SQL 표현식.
 *
 * 두 TSTZRANGE 값이 인접한지(adjacent) 확인한다.
 * PostgreSQL 전용이며 다른 dialect에서는 사용할 수 없다.
 *
 * SQL: `left_expr -|- right_expr`
 */
class TstzRangeAdjacentOp(
    expr1: Expression<*>,
    expr2: Expression<*>,
): ComparisonOp(expr1, expr2, "-|-")

/**
 * 이 TSTZRANGE 컬럼이 다른 TSTZRANGE 컬럼과 겹치는지 확인하는 `&&` 연산자.
 *
 * **PostgreSQL 전용**: 다른 dialect에서 호출하면 [IllegalStateException]이 발생한다.
 *
 * ```kotlin
 * val query = EventTable.selectAll()
 *     .where { EventTable.period.overlaps(OtherTable.period) }
 * // SQL: ... WHERE events.period && other_table.period
 * ```
 *
 * @param other 비교할 TSTZRANGE 컬럼
 * @return `&&` 연산 결과를 나타내는 [Op]<[Boolean]>
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<TimestampRange>.overlaps(other: Column<TimestampRange>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) { "overlaps (&&) 는 PostgreSQL dialect 에서만 지원됩니다." }
    return TstzRangeOverlapsOp(this, other)
}

/**
 * 이 TSTZRANGE 컬럼이 특정 [Instant]를 포함하는지 확인하는 `@>` 연산자.
 *
 * **PostgreSQL 전용**: 다른 dialect에서 호출하면 [IllegalStateException]이 발생한다.
 *
 * ```kotlin
 * val now = Instant.now()
 * val active = EventTable.selectAll()
 *     .where { EventTable.period.contains(now.asLiteral()) }
 * // SQL: ... WHERE events.period @> now()::timestamptz
 * ```
 *
 * @param instant 포함 여부를 확인할 timestamp [Expression]
 * @return `@>` 연산 결과를 나타내는 [Op]<[Boolean]>
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<TimestampRange>.contains(instant: Expression<Instant>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) { "contains (@>) 는 PostgreSQL dialect 에서만 지원됩니다." }
    return TstzRangeContainsInstantOp(this, instant)
}

/**
 * 이 TSTZRANGE 컬럼이 다른 TSTZRANGE 컬럼을 완전히 포함하는지 확인하는 `@>` 연산자.
 *
 * **PostgreSQL 전용**: 다른 dialect에서 호출하면 [IllegalStateException]이 발생한다.
 *
 * ```kotlin
 * val results = EventTable.selectAll()
 *     .where { EventTable.period.containsRange(SubTable.period) }
 * // SQL: ... WHERE events.period @> sub_table.period
 * ```
 *
 * @param other 포함 여부를 확인할 TSTZRANGE 컬럼
 * @return `@>` 연산 결과를 나타내는 [Op]<[Boolean]>
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<TimestampRange>.containsRange(other: Column<TimestampRange>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) { "containsRange (@>) 는 PostgreSQL dialect 에서만 지원됩니다." }
    return TstzRangeContainsRangeOp(this, other)
}

/**
 * 이 TSTZRANGE 컬럼이 다른 TSTZRANGE 컬럼과 인접한지 확인하는 `-|-` 연산자.
 *
 * **PostgreSQL 전용**: 다른 dialect에서 호출하면 [IllegalStateException]이 발생한다.
 *
 * ```kotlin
 * val results = EventTable.selectAll()
 *     .where { EventTable.period.isAdjacentTo(NextTable.period) }
 * // SQL: ... WHERE events.period -|- next_table.period
 * ```
 *
 * @param other 인접 여부를 확인할 TSTZRANGE 컬럼
 * @return `-|-` 연산 결과를 나타내는 [Op]<[Boolean]>
 * @throws IllegalStateException PostgreSQL이 아닌 dialect에서 호출 시
 */
fun Column<TimestampRange>.isAdjacentTo(other: Column<TimestampRange>): Op<Boolean> {
    check(currentDialect is PostgreSQLDialect) { "isAdjacentTo (-|-) 는 PostgreSQL dialect 에서만 지원됩니다." }
    return TstzRangeAdjacentOp(this, other)
}
