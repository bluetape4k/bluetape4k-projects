package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation
import org.springframework.data.r2dbc.core.update
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update

/**
 * 갱신 조건에 맞는 엔티티를 업데이트하고 변경 건수를 반환합니다.
 *
 * ## 동작/계약
 * - `update<T>().matching(query).apply(update)` 결과를 대기합니다.
 * - 조건에 맞는 행이 없으면 `0`을 반환합니다.
 * - [Update]는 호출자가 구성한 값을 그대로 사용합니다.
 *
 * ```kotlin
 * val updated = operations.updateSuspending<Post>(query, Update.update("title", "Updated"))
 * // updated == 1L
 * ```
 *
 * @param query 갱신 조건
 * @param update 반영할 컬럼 변경 값
 */
suspend inline fun <reified T: Any> ReactiveUpdateOperation.updateSuspending(
    query: Query,
    update: Update,
): Long = update<T>().matching(query).apply(update).awaitSingle()
