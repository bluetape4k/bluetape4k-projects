package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation
import org.springframework.data.r2dbc.core.update
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update

/**
 * [Query] 조건에 해당하는 엔티티를 갱신하고 갱신된 건수를 반환합니다.
 *
 * @param query 갱신 조건
 * @param update 갱신할 값
 * @return 갱신된 건수
 */
suspend inline fun <reified T: Any> ReactiveUpdateOperation.updateSuspending(query: Query, update: Update): Long =
    update<T>().matching(query).apply(update).awaitSingle()

@Deprecated(
    message = "updateSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("updateSuspending<T>(query, update)"),
)
suspend inline fun <reified T: Any> ReactiveUpdateOperation.suspendUpdate(query: Query, update: Update): Long =
    updateSuspending<T>(query, update)
