package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.r2dbc.core.ReactiveInsertOperation
import org.springframework.data.r2dbc.core.insert

/**
 * 엔티티를 저장하고 저장된 엔티티를 반환합니다.
 *
 * @param entity 저장할 엔티티
 * @return 저장된 엔티티
 */
suspend inline fun <reified T: Any> ReactiveInsertOperation.insertSuspending(entity: T): T =
    insert<T>().using(entity).awaitSingle()

@Deprecated(
    message = "insertSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("insertSuspending(entity)"),
)
suspend inline fun <reified T: Any> ReactiveInsertOperation.suspendInsert(entity: T): T =
    insertSuspending(entity)

/**
 * 엔티티를 저장하고 없으면 null을 반환합니다.
 *
 * @param entity 저장할 엔티티
 * @return 저장된 엔티티 또는 null
 */
suspend inline fun <reified T: Any> ReactiveInsertOperation.insertOrNullSuspending(entity: T): T? =
    insert<T>().using(entity).awaitSingleOrNull()

@Deprecated(
    message = "insertOrNullSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("insertOrNullSuspending(entity)"),
)
suspend inline fun <reified T: Any> ReactiveInsertOperation.suspendInsertOrNull(entity: T): T? =
    insertOrNullSuspending(entity)
