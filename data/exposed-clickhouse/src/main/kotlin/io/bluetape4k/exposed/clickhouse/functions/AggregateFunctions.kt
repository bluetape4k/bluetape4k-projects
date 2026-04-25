package io.bluetape4k.exposed.clickhouse.functions

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.DoubleColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.QueryBuilder

/**
 * ClickHouse `argMax(value, key)` — key가 최대인 row의 value를 반환합니다.
 *
 * ```sql
 * SELECT argMax(event_name, event_id) FROM events
 * ```
 *
 * @param V value expression의 타입 (non-null).
 * @param K key expression의 타입.
 * @property value 반환할 값 expression.
 * @property key 최대값 기준 expression.
 * @property columnType value의 컬럼 타입.
 */
class ArgMax<V : Any, K>(
    val value: Expression<V>,
    val key: Expression<K>,
    columnType: IColumnType<V>,
) : Function<V>(columnType) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("argMax(")
        queryBuilder.append(value)
        queryBuilder.append(", ")
        queryBuilder.append(key)
        queryBuilder.append(")")
    }
}

/**
 * ClickHouse `argMax(value, key)` 함수 빌더.
 *
 * key 컬럼이 최대인 row의 value를 반환합니다.
 *
 * ```kotlin
 * val latestPrice = argMax(Orders.price, Orders.eventTime)
 * ```
 *
 * @param value 반환할 값 컬럼.
 * @param key 최대값 기준 expression.
 */
fun <V : Any, K> argMax(value: Column<V>, key: Expression<K>): ArgMax<V, K> =
    ArgMax(value, key, value.columnType)

/**
 * ClickHouse `argMin(value, key)` — key가 최소인 row의 value를 반환합니다.
 *
 * ```sql
 * SELECT argMin(event_name, event_id) FROM events
 * ```
 *
 * @param V value expression의 타입 (non-null).
 * @param K key expression의 타입.
 * @property value 반환할 값 expression.
 * @property key 최소값 기준 expression.
 * @property columnType value의 컬럼 타입.
 */
class ArgMin<V : Any, K>(
    val value: Expression<V>,
    val key: Expression<K>,
    columnType: IColumnType<V>,
) : Function<V>(columnType) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("argMin(")
        queryBuilder.append(value)
        queryBuilder.append(", ")
        queryBuilder.append(key)
        queryBuilder.append(")")
    }
}

/**
 * ClickHouse `argMin(value, key)` 함수 빌더.
 *
 * key 컬럼이 최소인 row의 value를 반환합니다.
 *
 * ```kotlin
 * val firstEventName = argMin(Events.eventName, Events.eventId)
 * ```
 *
 * @param value 반환할 값 컬럼.
 * @param key 최소값 기준 expression.
 */
fun <V : Any, K> argMin(value: Column<V>, key: Expression<K>): ArgMin<V, K> =
    ArgMin(value, key, value.columnType)

/**
 * ClickHouse `quantile(level)(expr)` — 분위수를 계산합니다.
 *
 * level은 0.0~1.0 범위여야 하며, 결과는 [Double]로 반환됩니다.
 *
 * ```sql
 * SELECT quantile(0.95)(response_time) FROM requests
 * ```
 *
 * 사용 예:
 * ```kotlin
 * val p95 = quantile(0.95, Requests.responseTime)
 * ```
 *
 * @param T 입력 expression의 타입.
 * @property level 분위수 레벨 (0.0~1.0).
 * @property expr 대상 expression.
 */
class Quantile<T>(
    val level: Double,
    val expr: Expression<T>,
) : Function<Double>(DoubleColumnType()) {
    init {
        require(level in 0.0..1.0) { "quantile level must be in 0.0..1.0, got: $level" }
    }

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("quantile($level)(")
        queryBuilder.append(expr)
        queryBuilder.append(")")
    }
}

/**
 * ClickHouse `quantile(level)(expr)` 함수 빌더.
 *
 * level은 0.0~1.0 범위여야 합니다.
 *
 * ```kotlin
 * val median = quantile(0.5, Events.eventId)
 * val p95 = quantile(0.95, Events.eventId)
 * ```
 *
 * @param level 분위수 레벨 (0.0~1.0).
 * @param expr 대상 expression.
 */
fun <T> quantile(level: Double, expr: Expression<T>): Quantile<T> = Quantile(level, expr)

/**
 * ClickHouse `uniq(expr, ...)` — HyperLogLog 기반 근사 count distinct.
 *
 * 정확도보다 속도가 우선인 경우 사용합니다. 정확한 결과가 필요하면 [UniqExact]를 사용하세요.
 *
 * ```sql
 * SELECT uniq(user_id) FROM events
 * ```
 *
 * 사용 예:
 * ```kotlin
 * val approxDistinct = uniq(Events.region)
 * ```
 *
 * @property exprs count distinct 대상 expression 목록.
 */
class Uniq(vararg val exprs: Expression<*>) : Function<Long>(LongColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("uniq(")
        exprs.forEachIndexed { idx, expr ->
            if (idx > 0) queryBuilder.append(", ")
            queryBuilder.append(expr)
        }
        queryBuilder.append(")")
    }
}

/**
 * ClickHouse `uniq(expr, ...)` 함수 빌더 (HyperLogLog 근사 count distinct).
 *
 * ```kotlin
 * val approxDistinct = uniq(Events.region)
 * ```
 *
 * @param exprs count distinct 대상 expression 목록.
 */
fun uniq(vararg exprs: Expression<*>): Uniq = Uniq(*exprs)

/**
 * ClickHouse `uniqExact(expr, ...)` — 정확한 count distinct.
 *
 * [Uniq]보다 느리지만 정확한 결과를 반환합니다.
 *
 * ```sql
 * SELECT uniqExact(region) FROM events
 * ```
 *
 * 사용 예:
 * ```kotlin
 * val exactDistinct = uniqExact(Events.region)
 * ```
 *
 * @property exprs count distinct 대상 expression 목록.
 */
class UniqExact(vararg val exprs: Expression<*>) : Function<Long>(LongColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("uniqExact(")
        exprs.forEachIndexed { idx, expr ->
            if (idx > 0) queryBuilder.append(", ")
            queryBuilder.append(expr)
        }
        queryBuilder.append(")")
    }
}

/**
 * ClickHouse `uniqExact(expr, ...)` 함수 빌더 (정확한 count distinct).
 *
 * ```kotlin
 * val exactDistinct = uniqExact(Events.region)
 * ```
 *
 * @param exprs count distinct 대상 expression 목록.
 */
fun uniqExact(vararg exprs: Expression<*>): UniqExact = UniqExact(*exprs)
