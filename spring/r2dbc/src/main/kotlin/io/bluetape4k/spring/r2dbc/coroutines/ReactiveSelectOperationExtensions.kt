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
 * [Query] 조건으로 존재 여부를 조회합니다.
 *
 * @param query 조회 조건
 * @return 존재 여부
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.existsSuspending(query: Query): Boolean =
    select<T>().matching(query).awaitExists()

@Deprecated(
    message = "existsSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("existsSuspending<T>(query)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendExists(query: Query): Boolean =
    existsSuspending<T>(query)

/**
 * [Query] 조건의 건수를 조회합니다.
 *
 * @param query 조회 조건
 * @return 조회된 건수
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.countSuspending(query: Query): Long =
    select<T>().matching(query).awaitCount()

@Deprecated(
    message = "countSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("countSuspending<T>(query)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendCount(query: Query): Long =
    countSuspending<T>(query)

/**
 * 전체 건수를 조회합니다.
 *
 * @return 전체 건수
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.countAllSuspending(): Long =
    countSuspending<T>(Query.empty())

@Deprecated(
    message = "countAllSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("countAllSuspending<T>()"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendCountAll(): Long =
    countAllSuspending<T>()

/**
 * [Query] 조건으로 조회한 결과를 [Flow]로 반환합니다.
 *
 * @param query 조회 조건
 * @return 조회된 엔티티 [Flow]
 */
inline fun <reified T: Any> R2dbcEntityOperations.selectSuspending(query: Query): Flow<T> =
    select<T>().matching(query).flow()

@Deprecated(
    message = "selectSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectSuspending<T>(query)"),
)
inline fun <reified T: Any> R2dbcEntityOperations.suspendSelect(query: Query): Flow<T> =
    selectSuspending<T>(query)

/**
 * 모든 엔티티를 조회합니다.
 *
 * @return 조회된 엔티티 [Flow]
 */
inline fun <reified T: Any> R2dbcEntityOperations.selectAllSuspending(): Flow<T> =
    selectSuspending(Query.empty())

@Deprecated(
    message = "selectAllSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectAllSuspending<T>()"),
)
inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectAll(): Flow<T> =
    selectAllSuspending<T>()

/**
 * [Query] 조건으로 단건을 조회합니다.
 *
 * @param query 조회 조건
 * @return 조회된 엔티티
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.selectOneSuspending(query: Query): T =
    select<T>().matching(query).one().awaitSingle()

@Deprecated(
    message = "selectOneSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneSuspending<T>(query)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectOne(query: Query): T =
    selectOneSuspending<T>(query)

/**
 * [Query] 조건으로 단건을 조회하고 없으면 null을 반환합니다.
 *
 * @param query 조회 조건
 * @return 조회된 엔티티 또는 null
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.selectOneOrNullSuspending(query: Query): T? =
    select<T>().matching(query).one().awaitSingleOrNull()

@Deprecated(
    message = "selectOneOrNullSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectOneOrNullSuspending<T>(query)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectOneOrNull(query: Query): T? =
    selectOneOrNullSuspending<T>(query)

/**
 * [Query] 조건으로 첫 번째 엔티티를 조회합니다.
 *
 * @param query 조회 조건
 * @return 조회된 엔티티
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.selectFirstSuspending(query: Query): T =
    select<T>().matching(query).first().awaitSingle()

@Deprecated(
    message = "selectFirstSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectFirstSuspending<T>(query)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectFirst(query: Query): T =
    selectFirstSuspending<T>(query)

/**
 * [Query] 조건으로 첫 번째 엔티티를 조회하고 없으면 null을 반환합니다.
 *
 * @param query 조회 조건
 * @return 조회된 엔티티 또는 null
 */
suspend inline fun <reified T: Any> R2dbcEntityOperations.selectFirstOrNullSuspending(query: Query): T? =
    select<T>().matching(query).first().awaitSingleOrNull()

@Deprecated(
    message = "selectFirstOrNullSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("selectFirstOrNullSuspending<T>(query)"),
)
suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectFirstOrNull(query: Query): T? =
    selectFirstOrNullSuspending<T>(query)
