package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation
import org.springframework.data.r2dbc.core.delete
import org.springframework.data.relational.core.query.Query

/**
 * [Query] 조건에 해당하는 엔티티를 삭제하고 삭제된 건수를 반환합니다.
 *
 * @param query 삭제 조건
 * @return 삭제된 건수
 */
suspend inline fun <reified T: Any> ReactiveDeleteOperation.deleteSuspending(query: Query): Long =
    delete<T>().matching(query).all().awaitSingle()

@Deprecated(
    message = "deleteSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("deleteSuspending<T>(query)"),
)
suspend inline fun <reified T: Any> ReactiveDeleteOperation.suspendDelete(query: Query): Long =
    deleteSuspending<T>(query)

/**
 * 모든 엔티티를 삭제하고 삭제된 건수를 반환합니다.
 *
 * @return 삭제된 건수
 */
suspend inline fun <reified T: Any> ReactiveDeleteOperation.deleteAllSuspending(): Long =
    delete<T>().matching(Query.empty()).all().awaitSingle()

@Deprecated(
    message = "deleteAllSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("deleteAllSuspending<T>()"),
)
suspend inline fun <reified T: Any> ReactiveDeleteOperation.suspendDeleteAll(): Long =
    deleteAllSuspending<T>()
