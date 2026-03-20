package io.bluetape4k.spring4.cassandra

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.cassandra.core.ReactiveSelectOperation
import org.springframework.data.cassandra.core.ReactiveSelectOperation.SelectWithProjection
import org.springframework.data.cassandra.core.ReactiveSelectOperation.SelectWithQuery

/**
 * `SelectWithProjection` 결과 타입을 제네릭 [R]로 캐스팅한 조회 빌더를 반환합니다.
 *
 * ## 동작/계약
 * - 구현은 ``as(R::class.java)`` 호출을 단순 래핑합니다.
 * - 잘못된 타입 지정 시 이후 조회 단계에서 매핑 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val projected = selectOp.from(User::class.java).cast<User>()
 * // result == projected
 * ```
 */
inline fun <reified R: Any> SelectWithProjection<*>.cast(): SelectWithQuery<R> = `as`(R::class.java)

/**
 * 조회 결과 건수를 코루틴에서 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `count().awaitSingle()`을 호출합니다.
 * - 결과가 비어 있어도 Cassandra count 결과(예: `0L`)를 그대로 반환합니다.
 *
 * ```kotlin
 * val count = terminatingSelect.countSuspending()
 * // result == 0L
 * ```
 */
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.countSuspending(): Long = count().awaitSingle()

/**
 * 조회 결과 존재 여부를 코루틴에서 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `exists().awaitSingle()`을 호출합니다.
 * - 조건에 맞는 레코드가 하나 이상이면 `true`를 반환합니다.
 *
 * ```kotlin
 * val exists = terminatingSelect.existsSuspending()
 * // result == true
 * ```
 */
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.existsSuspending(): Boolean = exists().awaitSingle()

/**
 * 첫 번째 결과를 코루틴에서 단건으로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `first().awaitSingle()`을 호출합니다.
 * - 결과가 없으면 `awaitSingle()` 규칙에 따라 예외가 전파됩니다.
 *
 * ```kotlin
 * val user = terminatingSelect.first()
 * // result == user.id
 * ```
 */
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.first(): T = first().awaitSingle()

/**
 * 단건 조회 결과를 반환하고 없으면 `null`을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `one().awaitSingle()`을 호출합니다.
 * - Spring Data의 `one()`가 빈 결과를 `null`로 발행하면 그대로 `null`을 반환합니다.
 *
 * ```kotlin
 * val loaded = terminatingSelect.oneSuspending()
 * // result == loaded
 * ```
 */
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.oneSuspending(): T? = one().awaitSingle()

/**
 * 전체 조회 결과를 리스트로 수집해 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `all().collectList().awaitSingle()`을 호출합니다.
 * - 결과가 없으면 빈 리스트를 반환합니다.
 *
 * ```kotlin
 * val users = terminatingSelect.allSuspending()
 * // result == users.size
 * ```
 */
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.allSuspending(): List<T> =
    all().collectList().awaitSingle()
