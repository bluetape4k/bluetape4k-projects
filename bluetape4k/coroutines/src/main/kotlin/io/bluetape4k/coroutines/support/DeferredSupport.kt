package io.bluetape4k.coroutines.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

/**
 * 두 개의 [Deferred]의 값을 하나의 [Deferred]로 만듭니다.
 *
 * ```
 * val deferred1 = async { 1 }
 * val deferred2 = async { 2 }
 * val deferred3 = zip(deferred1, deferred2) { a, b -> a + b }
 * deferred3.await() // 3
 * ```
 *
 * @param src1 [Deferred] 인스턴스
 * @param src2 [Deferred] 인스턴스
 * @param coroutineStart [CoroutineStart] 값
 * @param zipper 두 [Deferred] 값의 zip 함수
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
 * Deferred 의 값을 [transform]로 변환하여 새로운 Deferred 를 만듭니다.
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

suspend inline fun <K, T: Collection<K>, R> Deferred<T>.mapAll(
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline transform: (K) -> Iterable<R>,
): Deferred<Collection<R>> = coroutineScope {
    val self = this@mapAll

    async(start = coroutineStart) {
        self.await().flatMap { transform(it) }
    }
}

suspend inline fun <K, T: Collection<K>, R> Deferred<T>.concatMap(
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    crossinline transform: (K) -> R,
): Deferred<Collection<R>> = coroutineScope {
    val self = this@concatMap

    async(start = coroutineStart) {
        self.await().map { transform(it) }
    }
}

suspend fun <T> awaitAny(vararg args: Deferred<T>): T {
    require(args.isNotEmpty())
    return select { args.forEach { arg -> arg.onAwait { it } } }
}

suspend fun <T> Collection<Deferred<T>>.awaitAny(): T {
    require(this.isNotEmpty())
    return select { forEach { item -> item.onAwait { it } } }
}

suspend fun <T> Collection<Deferred<T>>.awaitAnyAndCancelOthers(): T {
    require(this.isNotEmpty())
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
