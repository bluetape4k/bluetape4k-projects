package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.r2dbc.core.awaitCount
import org.springframework.data.r2dbc.core.awaitExists
import org.springframework.data.r2dbc.core.flow
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.query.Query

/**
 * 조회 조건에 해당하는 엔티티의 존재 여부를 반환합니다.
 *
 * ## 동작/계약
 * - `select<T>().matching(query).awaitExists()`로 위임합니다.
 * - 수신 객체를 변경하지 않고 읽기 쿼리만 수행합니다.
 *
 * ```kotlin
 * val exists = operations.existsSuspending<Post>(Query.empty())
 * // exists == true
 * ```
 *
 * @param query 조회 조건
 */
suspend inline fun <reified T : Any> R2dbcEntityOperations.existsSuspending(query: Query): Boolean =
    select<T>().matching(query).awaitExists()

/**
 * 조회 조건에 해당하는 행 수를 반환합니다.
 *
 * ## 동작/계약
 * - `awaitCount()` 결과를 그대로 반환합니다.
 * - 조건에 맞는 데이터가 없으면 `0`을 반환합니다.
 *
 * ```kotlin
 * val count = operations.countSuspending<Post>(Query.empty())
 * // count >= 0L
 * ```
 *
 * @param query 조회 조건
 */
suspend inline fun <reified T : Any> R2dbcEntityOperations.countSuspending(query: Query): Long =
    select<T>().matching(query).awaitCount()

/**
 * 전체 행 수를 조회합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `Query.empty()`를 사용해 [countSuspending]에 위임합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val count = operations.countAllSuspending<Post>()
 * // count >= 0L
 * ```
 */
suspend inline fun <reified T : Any> R2dbcEntityOperations.countAllSuspending(): Long =
    countSuspending<T>(Query.empty())

/**
 * 조회 조건 결과를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - Reactor Publisher를 코루틴 [Flow]로 어댑트하여 지연 수집합니다.
 * - `Flow`를 수집할 때 SQL이 실행됩니다.
 *
 * ```kotlin
 * val posts = operations.selectAllSuspending<Post>().toList()
 * // posts.isNotEmpty() == true
 * ```
 *
 * @param query 조회 조건
 */
inline fun <reified T : Any> R2dbcEntityOperations.selectSuspending(query: Query): Flow<T> =
    select<T>().matching(query).flow()

/**
 * 전체 엔티티를 [Flow]로 조회합니다.
 *
 * ## 동작/계약
 * - `Query.empty()`로 전체 조회를 수행합니다.
 * - 결과가 없으면 빈 `Flow`가 반환됩니다.
 *
 * ```kotlin
 * val posts = operations.selectAllSuspending<Post>().toList()
 * // posts.isNotEmpty() == true
 * ```
 */
inline fun <reified T : Any> R2dbcEntityOperations.selectAllSuspending(): Flow<T> = selectSuspending(Query.empty())

/**
 * 조회 조건에서 단건을 반환합니다.
 *
 * ## 동작/계약
 * - 결과가 정확히 1건일 때만 값을 반환합니다.
 * - 결과가 0건/복수 건이면 하위 API 예외가 전파됩니다.
 *
 * ```kotlin
 * val post = operations.selectOneSuspending<Post>(Query.query(Criteria.where("id").isEqual(1L)))
 * // post.id == 1L
 * ```
 *
 * @param query 조회 조건
 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException 조회 결과가 단건이 아닐 때 발생할 수 있습니다.
 */
suspend inline fun <reified T : Any> R2dbcEntityOperations.selectOneSuspending(query: Query): T =
    select<T>().matching(query).one().awaitSingle()

/**
 * 조회 조건에서 단건을 조회하고 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 결과가 1건이면 해당 엔티티를 반환합니다.
 * - 결과가 0건이면 `null`을 반환합니다.
 * - 복수 건이면 하위 API 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * val missing = operations.selectOneOrNullSuspending<Post>(Query.query(Criteria.where("id").isEqual(-1L)))
 * // missing == null
 * ```
 *
 * @param query 조회 조건
 */
suspend inline fun <reified T : Any> R2dbcEntityOperations.selectOneOrNullSuspending(query: Query): T? =
    select<T>().matching(query).one().awaitSingleOrNull()

/**
 * 조회 조건에서 첫 번째 엔티티를 반환합니다.
 *
 * ## 동작/계약
 * - 첫 번째 행만 소비하고 나머지는 무시합니다.
 * - 결과가 없으면 하위 API에서 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * val first = operations.selectFirstSuspending<Post>(Query.empty())
 * // first.id != null
 * ```
 *
 * @param query 조회 조건
 */
suspend inline fun <reified T : Any> R2dbcEntityOperations.selectFirstSuspending(query: Query): T =
    select<T>().matching(query).first().awaitSingle()

/**
 * 조회 조건에서 첫 번째 엔티티를 조회하고 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 첫 번째 행이 있으면 반환하고, 없으면 `null`을 반환합니다.
 * - 단건 보장을 요구하지 않으며 첫 결과만 사용합니다.
 *
 * ```kotlin
 * val first = operations.selectFirstOrNullSuspending<Post>(Query.query(Criteria.where("id").isEqual(-1L)))
 * // first == null
 * ```
 *
 * @param query 조회 조건
 */
suspend inline fun <reified T : Any> R2dbcEntityOperations.selectFirstOrNullSuspending(query: Query): T? =
    select<T>().matching(query).first().awaitSingleOrNull()
