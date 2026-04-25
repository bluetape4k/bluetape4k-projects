package io.bluetape4k.exposed.clickhouse.functions

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Function
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.LongColumnType
import org.jetbrains.exposed.v1.core.QueryBuilder

/**
 * ClickHouse dateDiff 함수에서 사용하는 시간 단위 열거형.
 *
 * ClickHouse dateDiff의 unit 파라미터 값과 동일합니다.
 *
 * ```kotlin
 * dateDiff(DateDiffUnit.day, Events.startDate, Events.endDate)
 * ```
 */
enum class DateDiffUnit {
    second, minute, hour, day, week, month, quarter, year;

    /** SQL에 삽입될 단위 문자열 (작은따옴표 포함) */
    val sqlValue: String get() = "'$name'"
}

/**
 * ClickHouse `toYYYYMM` 함수.
 *
 * 날짜/시간 expression을 `YYYYMM` 형식의 [Int]로 변환합니다.
 *
 * ```sql
 * SELECT toYYYYMM(created_at) -- e.g. 202403
 * ```
 *
 * 사용 예:
 * ```kotlin
 * Events.select(Events.createdAt.toYYYYMM())
 * ```
 *
 * @param T 입력 expression의 타입.
 */
class ToYYYYMM<T>(val expr: Expression<T>) : Function<Int>(IntegerColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("toYYYYMM(")
        queryBuilder.append(expr)
        queryBuilder.append(")")
    }
}

/**
 * Expression에 ClickHouse `toYYYYMM()` 함수를 적용합니다.
 *
 * ```kotlin
 * val yyyymm = Events.createdAt.toYYYYMM()
 * ```
 */
fun <T> Expression<T>.toYYYYMM(): ToYYYYMM<T> = ToYYYYMM(this)

/**
 * ClickHouse `toYYYYMMDD` 함수.
 *
 * 날짜/시간 expression을 `YYYYMMDD` 형식의 [Int]로 변환합니다.
 *
 * ```sql
 * SELECT toYYYYMMDD(created_at) -- e.g. 20240315
 * ```
 *
 * 사용 예:
 * ```kotlin
 * Events.select(Events.createdAt.toYYYYMMDD())
 * ```
 *
 * @param T 입력 expression의 타입.
 */
class ToYYYYMMDD<T>(val expr: Expression<T>) : Function<Int>(IntegerColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("toYYYYMMDD(")
        queryBuilder.append(expr)
        queryBuilder.append(")")
    }
}

/**
 * Expression에 ClickHouse `toYYYYMMDD()` 함수를 적용합니다.
 *
 * ```kotlin
 * val yyyymmdd = Events.createdAt.toYYYYMMDD()
 * ```
 */
fun <T> Expression<T>.toYYYYMMDD(): ToYYYYMMDD<T> = ToYYYYMMDD(this)

/**
 * ClickHouse `dateDiff` 함수.
 *
 * 두 날짜/시간 expression의 차이를 지정 단위([DateDiffUnit])로 반환합니다.
 * 결과 타입은 [Long]입니다.
 *
 * ```sql
 * SELECT dateDiff('day', start_date, end_date)
 * ```
 *
 * 사용 예:
 * ```kotlin
 * dateDiff(DateDiffUnit.day, Events.startDate, Events.endDate)
 * ```
 *
 * @param T 입력 expression의 타입.
 * @property unit 시간 단위 ([DateDiffUnit]).
 * @property from 시작 날짜/시간 expression.
 * @property to 종료 날짜/시간 expression.
 */
class DateDiff<T>(
    val unit: DateDiffUnit,
    val from: Expression<T>,
    val to: Expression<T>,
) : Function<Long>(LongColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("dateDiff(")
        queryBuilder.append(unit.sqlValue)
        queryBuilder.append(", ")
        queryBuilder.append(from)
        queryBuilder.append(", ")
        queryBuilder.append(to)
        queryBuilder.append(")")
    }
}

/**
 * ClickHouse `dateDiff` 함수를 생성합니다.
 *
 * ```kotlin
 * val diff = dateDiff(DateDiffUnit.hour, Events.startDate, Events.endDate)
 * ```
 *
 * @param unit 시간 단위 ([DateDiffUnit]).
 * @param from 시작 날짜/시간 expression.
 * @param to 종료 날짜/시간 expression.
 */
fun <T> dateDiff(unit: DateDiffUnit, from: Expression<T>, to: Expression<T>): DateDiff<T> =
    DateDiff(unit, from, to)

/**
 * ClickHouse `toStartOfInterval` 함수.
 *
 * expression을 지정 초 단위의 interval로 반올림(버림)합니다.
 * 결과 타입은 [java.time.Instant]입니다.
 *
 * ```sql
 * SELECT toStartOfInterval(event_time, INTERVAL 300 SECOND)
 * ```
 *
 * 사용 예:
 * ```kotlin
 * Events.createdAt.toStartOfInterval(300L) // 5분 단위로 버림
 * ```
 *
 * @param T 입력 expression의 타입.
 * @property expr 입력 날짜/시간 expression.
 * @property intervalSeconds 간격(초).
 */
class ToStartOfInterval<T>(
    val expr: Expression<T>,
    val intervalSeconds: Long,
) : Function<java.time.Instant>(org.jetbrains.exposed.v1.javatime.JavaInstantColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("toStartOfInterval(")
        queryBuilder.append(expr)
        queryBuilder.append(", INTERVAL $intervalSeconds SECOND)")
    }
}

/**
 * Expression에 ClickHouse `toStartOfInterval()` 함수를 적용합니다.
 *
 * ```kotlin
 * val bucketed = Events.createdAt.toStartOfInterval(3600L) // 1시간 단위
 * ```
 *
 * @param intervalSeconds 간격(초).
 */
fun <T> Expression<T>.toStartOfInterval(intervalSeconds: Long): ToStartOfInterval<T> =
    ToStartOfInterval(this, intervalSeconds)
