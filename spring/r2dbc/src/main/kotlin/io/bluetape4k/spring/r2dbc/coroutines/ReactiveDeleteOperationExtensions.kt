package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation
import org.springframework.data.r2dbc.core.delete
import org.springframework.data.relational.core.query.Query

/**
 * 삭제 조건에 해당하는 엔티티를 삭제하고 삭제 건수를 반환합니다.
 *
 * ## 동작/계약
 * - `delete<T>().matching(query).all()` 결과를 대기해 삭제 건수를 반환합니다.
 * - 조건에 맞는 행이 없으면 `0`을 반환합니다.
 * - 수신 객체는 변경하지 않으며 삭제 쿼리만 생성합니다.
 *
 * ```kotlin
 * val deleted = operations.deleteSuspending<Post>(query)
 * // deleted == 1L
 * ```
 *
 * @param query 삭제 조건
 */
suspend inline fun <reified T: Any> ReactiveDeleteOperation.deleteSuspending(query: Query): Long =
    delete<T>().matching(query).all().awaitSingle()

/**
 * [deleteSuspending]의 이전 이름을 제공합니다.
 *
 * ## 동작/계약
 * - 구현은 [deleteSuspending]으로 위임됩니다.
 * - 반환값은 삭제 건수입니다.
 *
 * ```kotlin
 * val deleted = operations.suspendDelete<Post>(Query.empty())
 * // deleted >= 0L
 * ```
 */
@Deprecated(
    message = "deleteSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("deleteSuspending<T>(query)"),
)
suspend inline fun <reified T: Any> ReactiveDeleteOperation.suspendDelete(query: Query): Long =
    deleteSuspending<T>(query)

/**
 * 전체 엔티티를 삭제하고 삭제 건수를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `Query.empty()`를 사용해 전체 삭제를 수행합니다.
 * - 반환값은 실제로 삭제된 행 수입니다.
 *
 * ```kotlin
 * val deleted = operations.deleteAllSuspending<Post>()
 * // deleted >= 0L
 * ```
 */
suspend inline fun <reified T: Any> ReactiveDeleteOperation.deleteAllSuspending(): Long =
    delete<T>().matching(Query.empty()).all().awaitSingle()

/**
 * [deleteAllSuspending]의 이전 이름을 제공합니다.
 *
 * ## 동작/계약
 * - 구현은 [deleteAllSuspending]으로 위임됩니다.
 * - 전체 삭제 건수를 반환합니다.
 *
 * ```kotlin
 * val deleted = operations.suspendDeleteAll<Post>()
 * // deleted >= 0L
 * ```
 */
@Deprecated(
    message = "deleteAllSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("deleteAllSuspending<T>()"),
)
suspend inline fun <reified T: Any> ReactiveDeleteOperation.suspendDeleteAll(): Long =
    deleteAllSuspending<T>()
