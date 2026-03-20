package io.bluetape4k.spring4.r2dbc.coroutines

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.r2dbc.core.ReactiveInsertOperation
import org.springframework.data.r2dbc.core.insert

/**
 * 엔티티를 저장하고 저장된 엔티티를 반환합니다.
 *
 * ## 동작/계약
 * - `insert<T>().using(entity)`를 수행한 뒤 단건 결과를 대기합니다.
 * - 저장 실패나 매핑 실패 예외는 그대로 전파됩니다.
 * - 수신 객체를 변경하지 않고 insert 파이프라인만 새로 구성합니다.
 *
 * ```kotlin
 * val saved = operations.insertSuspending(createPost())
 * // saved.id != null
 * ```
 *
 * @param entity 저장할 엔티티
 */
suspend inline fun <reified T : Any> ReactiveInsertOperation.insertSuspending(entity: T): T =
    insert<T>().using(entity).awaitSingle()

/**
 * 엔티티를 저장하고 결과가 비어 있으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - `awaitSingleOrNull()`을 사용해 결과 부재를 `null`로 매핑합니다.
 * - 일반적인 insert 성공 경로에서는 저장된 엔티티를 반환합니다.
 *
 * ```kotlin
 * val saved = operations.insertOrNullSuspending(createPost())
 * // saved?.id != null
 * ```
 *
 * @param entity 저장할 엔티티
 */
suspend inline fun <reified T : Any> ReactiveInsertOperation.insertOrNullSuspending(entity: T): T? =
    insert<T>().using(entity).awaitSingleOrNull()
