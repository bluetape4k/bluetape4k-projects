package io.bluetape4k.coroutines.support

import io.bluetape4k.support.requireNotEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

/**
 * 두 `Deferred`가 모두 완료되면 결과를 결합해 새 `Deferred`로 반환합니다.
 *
 * ## 동작/계약
 * - 내부 `async`에서 `src1.await()`와 `src2.await()`를 기다린 뒤 `zipper`를 적용합니다.
 * - 둘 중 하나라도 실패/취소되면 해당 예외/취소가 반환 `Deferred`로 전파됩니다.
 * - `coroutineStart`로 반환 `Deferred`의 시작 시점을 제어할 수 있습니다.
 *
 * ```kotlin
 * val result = scope.zip(d1, d2) { a, b -> a + b }.await()
 * // result == d1.await() + d2.await()
 * ```
 * @param src1 첫 번째 입력 `Deferred`입니다.
 * @param src2 두 번째 입력 `Deferred`입니다.
 * @param coroutineStart 반환 `Deferred`의 시작 모드입니다.
 * @param zipper 두 결과를 결합하는 함수입니다.
 */
inline fun <T1, T2, R> CoroutineScope.zip(
    src1: Deferred<T1>,
    src2: Deferred<T2>,
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline zipper: (T1, T2) -> R,
): Deferred<R> = async(start = coroutineStart) {
    zipper(src1.await(), src2.await())
}

/**
 * `Deferred` 완료 값을 변환하는 새 `Deferred`를 생성합니다.
 *
 * ## 동작/계약
 * - 수신 `Deferred`를 `await()`한 뒤 `transform`을 적용합니다.
 * - 수신 `Deferred`의 실패/취소 또는 `transform` 예외는 그대로 전파됩니다.
 * - 결과 `Deferred`는 현재 `coroutineScope`에서 `async(start = coroutineStart)`로 생성됩니다.
 *
 * ```kotlin
 * val out = deferred.map { it * 10 }.await()
 * // out == deferred.await() * 10
 * ```
 * @param coroutineStart 반환 `Deferred`의 시작 모드입니다.
 * @param transform 완료 값을 변환하는 suspend 함수입니다.
 */
suspend inline fun <T, R> Deferred<T>.map(
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline transform: suspend (T) -> R,
): Deferred<R> = coroutineScope {
    val self = this@map

    async(start = coroutineStart) {
        transform(self.await())
    }
}

/**
 * `Deferred<Collection<T>>`의 모든 원소를 `flatMap`으로 확장해 새 컬렉션으로 반환합니다.
 *
 * ## 동작/계약
 * - 수신 컬렉션을 `await()`한 뒤 각 원소에 `transform`을 적용하고 결과를 평탄화합니다.
 * - 반환 타입은 `List<R>`이지만 시그니처는 `Collection<R>`입니다.
 * - 수신 `Deferred` 실패/취소 또는 `transform` 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val out = deferred.mapAll { listOf(it, it) }.await()
 * // out == 입력 원소별 Iterable을 평탄화한 컬렉션
 * ```
 */
suspend inline fun <K, T: Collection<K>, R> Deferred<T>.mapAll(
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline transform: (K) -> Iterable<R>,
): Deferred<Collection<R>> = coroutineScope {
    val self = this@mapAll

    async(start = coroutineStart) {
        self.await().flatMap { transform(it) }
    }
}

/**
 * `Deferred<Collection<T>>`의 각 원소를 1:1로 변환해 새 컬렉션으로 반환합니다.
 *
 * ## 동작/계약
 * - 수신 컬렉션을 `await()`한 뒤 `map`을 적용합니다.
 * - 반환 타입은 `List<R>`이지만 시그니처는 `Collection<R>`입니다.
 * - 수신 `Deferred` 실패/취소 또는 `transform` 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val out = deferred.concatMap { it.toString() }.await()
 * // out == 입력 컬렉션과 같은 크기의 변환 결과 컬렉션
 * ```
 */
suspend inline fun <K, T: Collection<K>, R> Deferred<T>.concatMap(
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline transform: (K) -> R,
): Deferred<Collection<R>> = coroutineScope {
    val self = this@concatMap

    async(start = coroutineStart) {
        self.await().map { transform(it) }
    }
}

/**
 * 여러 `Deferred` 중 가장 먼저 완료된 값을 반환합니다.
 *
 * ## 동작/계약
 * - `args.requireNotEmpty("args")`로 빈 입력을 허용하지 않습니다.
 * - `select`로 가장 먼저 완료된 `Deferred`의 값을 즉시 반환합니다.
 * - 나머지 `Deferred`는 취소하지 않고 그대로 둡니다.
 *
 * ```kotlin
 * val winner = awaitAny(d1, d2, d3)
 * // winner == 가장 먼저 완료된 Deferred 값
 * ```
 * @param args 대기할 `Deferred` 목록입니다. 최소 1개 이상이어야 합니다.
 */
suspend fun <T> awaitAny(vararg args: Deferred<T>): T {
    args.requireNotEmpty("args")
    return select { args.forEach { arg -> arg.onAwait { it } } }
}

/**
 * 컬렉션 확장 버전의 [awaitAny]입니다.
 *
 * ## 동작/계약
 * - `requireNotEmpty("deferreds")`로 빈 컬렉션 입력을 허용하지 않습니다.
 * - 원소가 1개면 바로 `await()`하여 반환합니다.
 * - 원소가 여러 개면 `select`로 가장 먼저 완료된 값을 반환합니다.
 *
 * ```kotlin
 * val winner = deferreds.awaitAny()
 * // winner == 가장 먼저 완료된 Deferred 값
 * ```
 */
suspend fun <T> Collection<Deferred<T>>.awaitAny(): T {
    requireNotEmpty("deferreds")
    if (size == 1) {
        return first().await()
    }
    return select { forEach { item -> item.onAwait { it } } }
}

/**
 * 가장 먼저 완료된 값을 반환하고 나머지 `Deferred`를 취소합니다.
 *
 * ## 동작/계약
 * - `requireNotEmpty("deferreds")`로 빈 컬렉션 입력을 허용하지 않습니다.
 * - 원소가 1개면 취소 없이 바로 `await()` 값을 반환합니다.
 * - 원소가 여러 개면 첫 완료 값을 반환하고, 나머지에는 `cancel()`을 시도합니다.
 *
 * ```kotlin
 * val winner = deferreds.awaitAnyAndCancelOthers()
 * // winner == 가장 먼저 완료된 Deferred 값
 * ```
 */
suspend fun <T> Collection<Deferred<T>>.awaitAnyAndCancelOthers(): T {
    requireNotEmpty("deferreds")
    if (size == 1) {
        return first().await()
    }
    val firstAwaited = select {
        forEachIndexed { index, deferred ->
            deferred.onAwait { IndexedValue(index, it) }
        }
    }
    val firstAwaitedIndex = firstAwaited.index
    forEachIndexed { index, deferred ->
        if (index != firstAwaitedIndex)
            runCatching { deferred.cancel() }
    }
    return firstAwaited.value
}
