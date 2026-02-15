package io.bluetape4k.concurrent

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor
import java.util.concurrent.StructuredTaskScope
import java.util.function.BiFunction

/**
 * [CompletionStage]의 예외정보를 가져온다.
 * 예외로 완료되지 않은 CompletionStage라면 [IllegalStateException]을 발생시킨다.
 * 예외 없이 null을 반환받으려면 [getExceptionOrNull]을 사용하세요.
 *
 * ```
 * val future = CompletableFuture.failedFuture(RuntimeException("Something went wrong"))
 * val ex = future.getException()
 *
 * ex shouldBeInstanceOf RuntimeException::class
 * ex.message shouldBeEqualTo "Something went wrong"
 * ```
 *
 * @throws IllegalStateException 예외로 완료되지 않은 경우
 */
fun <T> CompletionStage<T>.getException(): Throwable? {
    val future = this.toCompletableFuture()
    if (!future.isCompletedExceptionally) {
        error("$this was not completed exceptionally.")
    }
    return try {
        future.join()
        null
    } catch (e: CompletionException) {
        e.cause
    }
}

/**
 * [CompletionStage]의 예외정보를 가져온다.
 * 성공적으로 완료된 CompletionStage라면 null을 반환한다.
 *
 * ```
 * val future: CompletableFuture<Int> = CompletableFuture.failedFuture(RuntimeException("error"))
 *
 * val ex: Throwable? = future.getExceptionOrNull()
 *
 * ex shouldBeInstanceOf RuntimeException::class
 * ex.message shouldBeEqualTo "Something went wrong"
 * ```
 */
fun <T> CompletionStage<T>.getExceptionOrNull(): Throwable? {
    val future = this.toCompletableFuture()
    return if (!future.isDone || !future.isCompletedExceptionally) {
        null
    } else {
        try {
            future.join()
            null
        } catch (e: CompletionException) {
            e.cause
        }
    }
}

/**
 * [CompletionStage]의 컬렉션을 `CompletableFuture<List<*>>` 로 변환합니다.
 *
 * ```
 * val futures: List<CompletableFuture<Int>> = listOf(...)
 * val results: CompletableFuture<List<Int>> = futures.sequence()
 * ```
 *
 * @receiver Iterable<CompletionStage<out Any>>
 * @param executor Executor (default: ForkJoinExecutor)
 * @return CompletableFuture<List<*>>
 */
fun Iterable<CompletionStage<out Any>>.sequence(executor: Executor = ForkJoinExecutor): CompletableFuture<List<*>> {
    val initial = completableFutureOf(mutableListOf<Any>())
    return fold(initial) { futureAcc, future ->
        futureAcc.zip(future, executor) { acc, result ->
            acc.apply { add(result) }
        }
    }.map(executor) { it.toList() }
}

/**
 * Generic 수형의 [CompletionStage] 컬렉션을 [List]의 [CompletableFuture]로 변환합니다.
 *
 * ```
 * val futures: List<CompletableFuture<Int>> = listOf(...)
 * val results: CompletableFuture<List<Int>> = futures.sequence()
 * ```
 *
 * @param executor Executor (default: ForkJoinExecutor)
 * @return `CompletableFuture<List<T>>`
 */
fun <T> Collection<CompletionStage<out T>>.sequence(executor: Executor = ForkJoinExecutor): CompletableFuture<List<T>> {
    val initial = completableFutureOf(mutableListOf<T>())
    return fold(initial) { futureAcc, future ->
        futureAcc.zip(future, executor) { acc, result ->
            acc.apply { add(result) }
        }
    }.map(executor) { it.toList() }
}

/**
 * [CompletionStage]의 컬렉션을 `CompletableFuture<List<*>>` 로 변환합니다.
 *
 * ```
 * val futures: List<CompletableFuture<Int>> = listOf(...)
 * val results: CompletableFuture<List<Int>> = futures.allAsList()
 * ```
 *
 * @receiver Iterable<CompletionStage<out Any>>
 * @param executor Executor
 * @return CompletableFuture<List<*>>
 * @see sequence
 */
inline fun Iterable<CompletionStage<out Any>>.allAsList(executor: Executor = ForkJoinExecutor): CompletableFuture<List<*>> =
    sequence(executor)


/**
 * Generic 수형의 [CompletionStage] 컬렉션을 [List]의 [CompletableFuture]로 변환합니다.
 *
 * ```
 * val futures: List<CompletableFuture<Int>> = listOf(...)
 * val results: CompletableFuture<List<Int>> = futures.allAsList()
 * ```
 *
 * @param executor Executor (default: ForkJoinExecutor)
 * @return `CompletableFuture<List<T>>`
 * @see sequence
 */
inline fun <T> List<CompletionStage<out T>>.allAsList(executor: Executor = ForkJoinExecutor): CompletableFuture<List<T>> =
    sequence(executor)

/**
 * Generic 수형의 [CompletionStage] 컬렉션의 예외가 있는 경우 [defaultValueMapper]로 매핑하여 [List]의 [CompletableFuture]로 변환합니다.
 *
 * ```
 * val futures: List<CompletableFuture<Int>> = listOf(...)
 * val results: CompletableFuture<List<Int>> = futures.successfulAsList { 0 }
 * ```
 *
 * @param executor Executor (default: ForkJoinExecutor)
 * @return `CompletableFuture<List<T>>`
 */
fun <T> List<CompletionStage<T>>.successfulAsList(
    executor: Executor = ForkJoinExecutor,
    defaultValueMapper: (Throwable) -> T,
): CompletableFuture<List<T>> {
    return map { f -> f.toCompletableFuture().recover<T>(defaultValueMapper) }.sequence(executor)
}

/**
 * 복수의 [CompletionStage] 중 가장 처음에 완료되는 것을 반환하고, 나머지는 취소합니다.
 *
 * ```
 * val futures: List<CompletableFuture<Int>> = listOf(...)
 * val result: CompletableFuture<Int> = futures.firstCompleted()
 * ```
 *
 * @param T  [CompletionStage]의 결과 수형
 * @receiver [CompletionStage]의 컬렉션
 * @return 가장 먼저 완료된 [CompletableFuture]
 *
 * @see CompletableFuture.anyOf
 */
fun <T> Iterable<CompletionStage<T>>.firstCompleted(): CompletableFuture<T> {
    val promise = CompletableFuture<T>()

    // TODO: JDK 21의 StructuredTaskScope 를 사용하는데, 이를 JDK 버전에 상관없이 사용할 수 있도록 한다.
    return StructuredTaskScope.ShutdownOnSuccess<T>("first-completed", Thread.ofVirtual().factory()).use { scope ->
        this@firstCompleted.forEach { item ->
            scope.fork {
                item.toCompletableFuture().get()
            }
        }

        scope.join()
        try {
            promise.complete(scope.result())
        } catch (e: Throwable) {
            promise.completeExceptionally(e)
        }
        promise
    }
}

/**
 * 결과를 다른 값으로 mapping 하도록 합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val mapped: CompletableFuture<String> = future.map { it.toString() }
 * ```
 *
 * @receiver CompletionStage<V> 원본 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param mapper Function1<V, R> 결과를 다른 값으로 변환하는 함수
 * @return CompletionStage<R>
 */
inline fun <V, R> CompletionStage<V>.map(
    executor: Executor = ForkJoinExecutor,
    crossinline mapper: (V) -> R,
): CompletionStage<R> = thenApplyAsync({ mapper(it) }, executor)

/**
 * 결과를 다른 값으로 mapping 하도록 합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val mapped: CompletableFuture<String> = future.flatMap { futureOf(it.toString()) }
 * ```
 *
 * @receiver CompletionStage<V> 원본 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param mapper Function1<V, CompletionStage<R>> 결과를 다른 [CompletionStage]로 변환하는 함수
 * @return CompletionStage<R>
 */
inline fun <V, R> CompletionStage<V>.flatMap(
    executor: Executor = ForkJoinExecutor,
    crossinline mapper: (V) -> CompletionStage<R>,
): CompletionStage<R> = thenComposeAsync({ mapper(it) }, executor)

/**
 * 결과를 [handler]를 이용하여 다른 값으로 mapping 하도록 합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val result = future.mapResult { v, e -> v?.toString() ?: e?.message ?: "unknown" }
 * ```
 *
 * @receiver CompletionStage<V> 원본 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param handler Function2<V?, Throwable?, R> 결과를 다른 값으로 변환하는 함수
 * @return CompletionStage<R>
 */
inline fun <V, R> CompletionStage<V>.mapResult(
    executor: Executor = ForkJoinExecutor,
    crossinline handler: (V?, Throwable?) -> R,
): CompletionStage<R> = handleAsync({ v, e -> handler(v, e) }, executor)

/**
 * `CompletionStage<CompletionStage<V>>`를 `CompletionStage<V>`로 변환합니다.
 *
 * ```
 * val future: CompletableFuture<CompletableFuture<Int>> = CompletableFuture.completedFuture(CompletableFuture.completedFuture(42))
 * val result: CompletableFuture<Int> = future.flatten()
 * ```
 *
 * @receiver CompletionStage<out CompletionStage<V>> 원본 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @return CompletionStage<V>
 */
fun <V> CompletionStage<out CompletionStage<V>>.flatten(executor: Executor = ForkJoinExecutor): CompletionStage<V> =
    flatMap(executor) { it }

/**
 * `CompletionStage<CompletionStage<V>>`를 `CompletionStage<V>`로 변환합니다.
 *
 * ```
 * val future: CompletableFuture<CompletableFuture<Int>> = CompletableFuture.completedFuture(CompletableFuture.completedFuture(42))
 * val result: CompletableFuture<Int> = future.dereference()
 * ```
 *
 * @receiver CompletionStage<out CompletionStage<V>> 원본 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @return CompletionStage<V>
 */
fun <V> CompletionStage<out CompletionStage<V>>.dereference(executor: Executor = ForkJoinExecutor): CompletionStage<V> =
    flatten(executor)

/**
 * `CompletionStage<V>`를 `CompletionStage<CompletionStage<V>>`로 변환합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val result: CompletableFuture<CompletableFuture<Int>> = future.wrap()
 * ```
 *
 * @receiver CompletionStage<V> 원본 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @return CompletionStage<CompletionStage<V>>
 */
fun <V> CompletionStage<V>.wrap(executor: Executor = ForkJoinExecutor): CompletionStage<CompletionStage<V>> =
    map(executor) { CompletableFuture.completedFuture(it) }


/**
 * 두 개의 [CompletionStage]를 순서대로 작업하여, 결과를 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val result: CompletableFuture<String> = combineOf(a, b) { a, b -> "$a $b" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B) -> R 두 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
fun <R, A, B> combineOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B) -> R,
): CompletionStage<R> = a.thenCombineAsync(b, BiFunction(combiner), executor)

/**
 * 세 개의 [CompletionStage]를 순서대로 작업하고, 결과를 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val c: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)
 * val result: CompletableFuture<String> = combineOf(a, b, c) { a, b, c -> "$a $b $c" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param C 세 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param c 세 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B, C) -> R 세 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
@Suppress("UNCHECKED_CAST")
fun <R, A, B, C> combineOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    c: CompletionStage<C>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B, C) -> R,
): CompletionStage<R> {
    return listOf(a, b, c).sequence(executor).map { list ->
        combiner(list[0] as A, list[1] as B, list[2] as C)
    }
}

/**
 * 네 개의 [CompletionStage]를 순서대로 작업하여, 결과들을 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val c: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)
 * val d: CompletableFuture<Double> = CompletableFuture.completedFuture(3.14)
 * val result: CompletableFuture<String> = combineOf(a, b, c, d) { a, b, c, d -> "$a $b $c $d" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param C 세 번째 [CompletionStage]의 결과 수형
 * @param D 네 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param c 세 번째 [CompletionStage]
 * @param d 네 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B, C, D) -> R 네 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
@Suppress("UNCHECKED_CAST")
fun <R, A, B, C, D> combineOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    c: CompletionStage<C>,
    d: CompletionStage<D>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B, C, D) -> R,
): CompletionStage<R> {
    return listOf(a, b, c, d).sequence(executor).map { list ->
        combiner(list[0] as A, list[1] as B, list[2] as C, list[3] as D)
    }
}

/**
 * 다섯 개의 [CompletionStage]를 순서대로 작업하여, 결과들을 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val c: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)
 * val d: CompletableFuture<Double> = CompletableFuture.completedFuture(3.14)
 * val e: CompletableFuture<Long> = CompletableFuture.completedFuture(1000L)
 * val result: CompletableFuture<String> = combineOf(a, b, c, d, e) { a, b, c, d, e -> "$a $b $c $d $e" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param C 세 번째 [CompletionStage]의 결과 수형
 * @param D 네 번째 [CompletionStage]의 결과 수형
 * @param E 다섯 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param c 세 번째 [CompletionStage]
 * @param d 네 번째 [CompletionStage]
 * @param e 다섯 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B, C, D, E) -> R 다섯 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
@Suppress("UNCHECKED_CAST")
fun <R, A, B, C, D, E> combineOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    c: CompletionStage<C>,
    d: CompletionStage<D>,
    e: CompletionStage<E>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B, C, D, E) -> R,
): CompletionStage<R> {
    return listOf(a, b, c, d, e).sequence(executor).map { list ->
        combiner(list[0] as A, list[1] as B, list[2] as C, list[3] as D, list[4] as E)
    }
}

/**
 * 여섯 개의 [CompletionStage]를 순서대로 작업하여, 결과들을 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val c: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)
 * val d: CompletableFuture<Double> = CompletableFuture.completedFuture(3.14)
 * val e: CompletableFuture<Long> = CompletableFuture.completedFuture(1000L)
 * val f: CompletableFuture<Char> = CompletableFuture.completedFuture('A')
 * val result: CompletableFuture<String> = combineOf(a, b, c, d, e, f) { a, b, c, d, e, f -> "$a $b $c $d $e $f" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param C 세 번째 [CompletionStage]의 결과 수형
 * @param D 네 번째 [CompletionStage]의 결과 수형
 * @param E 다섯 번째 [CompletionStage]의 결과 수형
 * @param F 여섯 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param c 세 번째 [CompletionStage]
 * @param d 네 번째 [CompletionStage]
 * @param e 다섯 번째 [CompletionStage]
 * @param f 여섯 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B, C, D, E, F) -> R 여섯 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
@Suppress("UNCHECKED_CAST")
fun <R, A, B, C, D, E, F> combineOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    c: CompletionStage<C>,
    d: CompletionStage<D>,
    e: CompletionStage<E>,
    f: CompletionStage<F>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B, C, D, E, F) -> R,
): CompletionStage<R> {
    return listOf(a, b, c, d, e, f).sequence(executor).map { list ->
        combiner(list[0] as A, list[1] as B, list[2] as C, list[3] as D, list[4] as E, list[5] as F)
    }
}

/**
 * 두 개의 [CompletionStage]를 병렬로 작업하여, 결과를 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val result: CompletableFuture<String> = combineFutureOf(a, b) { a, b -> "$a $b" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B) -> R 두 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
@Suppress("UNCHECKED_CAST")
fun <R, A, B> combineFutureOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B) -> CompletionStage<R>,
): CompletionStage<R> {
    return listOf(a, b).sequence(executor).flatMap { list ->
        combiner(list[0] as A, list[1] as B)
    }
}

/**
 * 세 개의 [CompletionStage]를 병렬로 작업하여, 결과를 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val c: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)
 * val result: CompletableFuture<String> = combineFutureOf(a, b, c) { a, b, c -> "$a $b $c" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param C 세 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param c 세 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B, C) -> R 세 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
@Suppress("UNCHECKED_CAST")
fun <R, A, B, C> combineFutureOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    c: CompletionStage<C>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B, C) -> CompletionStage<R>,
): CompletionStage<R> {
    return listOf(a, b, c).sequence(executor).flatMap { list ->
        combiner(list[0] as A, list[1] as B, list[2] as C)
    }
}

/**
 * 네 개의 [CompletionStage]를 병렬로 작업하여, 결과를 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val c: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)
 * val d: CompletableFuture<Double> = CompletableFuture.completedFuture(3.14)
 * val result: CompletableFuture<String> = combineFutureOf(a, b, c, d) { a, b, c, d -> "$a $b $c $d" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param C 세 번째 [CompletionStage]의 결과 수형
 * @param D 네 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param c 세 번째 [CompletionStage]
 * @param d 네 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B, C, D) -> R 네 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
@Suppress("UNCHECKED_CAST")
fun <R, A, B, C, D> combineFutureOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    c: CompletionStage<C>,
    d: CompletionStage<D>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B, C, D) -> CompletionStage<R>,
): CompletionStage<R> {
    return listOf(a, b, c, d).sequence(executor).flatMap { list ->
        combiner(list[0] as A, list[1] as B, list[2] as C, list[3] as D)
    }
}

/**
 * 다섯 개의 [CompletionStage]를 병렬로 작업하여, 결과를 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val c: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)
 * val d: CompletableFuture<Double> = CompletableFuture.completedFuture(3.14)
 * val e: CompletableFuture<Long> = CompletableFuture.completedFuture(1000L)
 * val result: CompletableFuture<String> = combineFutureOf(a, b, c, d, e) { a, b, c, d, e -> "$a $b $c $d $e" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param C 세 번째 [CompletionStage]의 결과 수형
 * @param D 네 번째 [CompletionStage]의 결과 수형
 * @param E 다섯 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param c 세 번째 [CompletionStage]
 * @param d 네 번째 [CompletionStage]
 * @param e 다섯 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B, C, D, E) -> R 다섯 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
@Suppress("UNCHECKED_CAST")
fun <R, A, B, C, D, E> combineFutureOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    c: CompletionStage<C>,
    d: CompletionStage<D>,
    e: CompletionStage<E>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B, C, D, E) -> CompletionStage<R>,
): CompletionStage<R> {
    return listOf(a, b, c, d, e).sequence(executor).flatMap { list ->
        combiner(list[0] as A, list[1] as B, list[2] as C, list[3] as D, list[4] as E)
    }
}

/**
 * 여섯 개의 [CompletionStage]를 병렬로 작업하여, 결과를 [combiner]로 결합합니다.
 *
 * ```
 * val a: CompletableFuture<Int> = CompletableFuture.completedFuture(42)
 * val b: CompletableFuture<String> = CompletableFuture.completedFuture("Hello")
 * val c: CompletableFuture<Boolean> = CompletableFuture.completedFuture(true)
 * val d: CompletableFuture<Double> = CompletableFuture.completedFuture(3.14)
 * val e: CompletableFuture<Long> = CompletableFuture.completedFuture(1000L)
 * val f: CompletableFuture<Char> = CompletableFuture.completedFuture('A')
 * val result: CompletableFuture<String> = combineFutureOf(a, b, c, d, e, f) { a, b, c, d, e, f -> "$a $b $c $d $e $f" }
 * ```
 *
 * @param R 결과 수형
 * @param A 첫 번째 [CompletionStage]의 결과 수형
 * @param B 두 번째 [CompletionStage]의 결과 수형
 * @param C 세 번째 [CompletionStage]의 결과 수형
 * @param D 네 번째 [CompletionStage]의 결과 수형
 * @param E 다섯 번째 [CompletionStage]의 결과 수형
 * @param F 여섯 번째 [CompletionStage]의 결과 수형
 * @param a 첫 번째 [CompletionStage]
 * @param b 두 번째 [CompletionStage]
 * @param c 세 번째 [CompletionStage]
 * @param d 네 번째 [CompletionStage]
 * @param e 다섯 번째 [CompletionStage]
 * @param f 여섯 번째 [CompletionStage]
 * @param executor Executor (default: ForkJoinExecutor)
 * @param combiner (A, B, C, D, E, F) -> R 여섯 개의 결과를 결합하는 함수
 * @return CompletionStage<R>
 */
@Suppress("UNCHECKED_CAST")
fun <R, A, B, C, D, E, F> combineFutureOf(
    a: CompletionStage<A>,
    b: CompletionStage<B>,
    c: CompletionStage<C>,
    d: CompletionStage<D>,
    e: CompletionStage<E>,
    f: CompletionStage<F>,
    executor: Executor = ForkJoinExecutor,
    combiner: (A, B, C, D, E, F) -> CompletionStage<R>,
): CompletionStage<R> {
    return listOf(a, b, c, d, e, f).sequence(executor).flatMap { list ->
        combiner(list[0] as A, list[1] as B, list[2] as C, list[3] as D, list[4] as E, list[5] as F)
    }
}
