@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.concurrent

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * 지정한 block을 비동기로 실행하고, [CompletableFuture]를 반환합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf {
 *      Thread.sleep(1000)
 *      42
 * }
 * val result = future.get()   // 42
 * ```
 *
 * @param executor [block]을 수행할 [Executor]
 * @param block 비동기로 수행할 코드 블럭
 * @return CompletableFuture<V>
 */
inline fun <V> futureOf(
    executor: Executor = ForkJoinExecutor,
    crossinline block: () -> V,
): CompletableFuture<V> =
    CompletableFuture.supplyAsync({ block() }, executor)

/**
 * 실행을 동기 방식으로 수행하지만, [CompletableFuture]로 변환합니다.
 *
 * @param block 비동기로 수행할 코드 블럭
 * @return CompletableFuture<V>
 */
inline fun <V> immediateFutureOf(crossinline block: () -> V): CompletableFuture<V> =
    futureOf(DirectExecutor, block)

/**
 * [value] 값을 가진 [CompletableFuture]를 생성합니다.
 *
 * ```
 * val future:CompletableFuture<Int> = completableFutureOf(42)
 * val result = future.get()  // 42
 * ```
 */
inline fun <V> completableFutureOf(value: V): CompletableFuture<V> =
    CompletableFuture.completedFuture(value)

/**
 * [cause] 예외를 가진 [CompletableFuture]를 생성합니다.
 */
inline fun <V> failedCompletableFutureOf(cause: Throwable): CompletableFuture<V> =
    CompletableFuture.failedFuture(cause)

/**
 * 비동기 작업 수행에 timeout 을 적용합니다.
 *
 * ```
 * val future:CompletableFuture<Unit> = futureWithTimeout(Duration.ofSeconds(1)) {
 *   // 1초 이내에 종료되지 않으면 실패로 간주하고, 작업을 중단합니다.
 *   // coroutines task()
 * }
 * ```
 *
 * @param V 결과값 수형
 * @param timeout 최대 수행 시간
 * @param block 수행할 코드 블럭
 * @return [block]의 실행 결과, [timeout] 시간 내에 종료되지 않으면 null 을 반환하는 [CompletableFuture] 인스턴스
 */
inline fun <V> futureWithTimeout(timeout: Duration, crossinline block: () -> V): CompletableFuture<V> =
    futureWithTimeout(timeout.inWholeMilliseconds, block)

/**
 * 비동기 작업 수행에 timeout 을 적용합니다.
 *
 * ```
 * val future:CompletableFuture<Unit> = futureWithTimeout(1000L) {
 *   // 1초 이내에 종료되지 않으면 실패로 간주하고, 작업을 중단합니다.
 *   // coroutines task()
 * }
 * ```
 *
 * @param V
 * @param timeoutMillis 최대 수행 시간 (밀리초)
 * @param block 수행할 코드 블럭
 * @return [block]의 실행 결과, [timeoutMillis] 시간 내에 종료되지 않으면 실패했음을 나타내는 [CompletableFuture] 인스턴스
 */
inline fun <V> futureWithTimeout(
    timeoutMillis: Long = 1000L,
    crossinline block: () -> V,
): CompletableFuture<V> {
    val executor = Executors.newVirtualThreadPerTaskExecutor()

    return CompletableFuture
        .supplyAsync({ block() }, executor)
        .orTimeout(timeoutMillis.coerceAtLeast(10L), TimeUnit.MILLISECONDS)
        .whenComplete { _, _ ->
            executor.shutdown()
        }
}


/**
 * [CompletableFuture]가 완료되면 결과 값을 [mapper]를 이용하여 변환한 값을 반환하도록 합니다.
 *
 * ```
 * val name: Long = completableFutureOf("1234").map { it.toLong() }
 * ```
 */
inline fun <V, R> CompletableFuture<V>.map(
    executor: Executor = ForkJoinExecutor,
    @BuilderInference crossinline mapper: (value: V) -> R,
): CompletableFuture<R> =
    thenApplyAsync({ mapper(it) }, executor)


/**
 * [CompletableFuture]가 완료되면 결과 값을 [mapper]를 이용하여 변환한 값을 반환하도록 합니다.
 *
 * ```
 * val name: Long = completableFutureOf("123").flatMap { futureOf { it.toLong() } }
 * ```
 *
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param mapper 변환 함수
 */
inline fun <V, R> CompletableFuture<V>.flatMap(
    executor: Executor = ForkJoinExecutor,
    crossinline mapper: (value: V) -> CompletionStage<R>,
): CompletableFuture<R> =
    thenComposeAsync({ mapper(it) }, executor)

/**
 * [CompletableFuture]가 완료되면 결과 값을 [handler]를 이용하여 변환한 값을 반환하도록 합니다.
 *
 * ```
 * val name: Long = completableFutureOf("123").handle { value, error -> value?.toLong() ?: 0 }
 * ```
 *
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param handler 변환 함수
 * @return CompletableFuture<R>
 */
inline fun <V, R> CompletableFuture<V>.mapResult(
    executor: Executor = ForkJoinExecutor,
    crossinline handler: (value: V?, error: Throwable?) -> R,
): CompletableFuture<R> =
    handleAsync({ v, e -> handler(v, e) }, executor)

/**
 * `CompletableFuture<CompletableFuture<V>>`를 `CompletableFuture<V>`로 평탄화합니다.
 *
 * ```
 * val future: CompletableFuture<CompletableFuture<Int>> = futureOf { futureOf { 42 } }
 * val result: CompletableFuture<Int> = future.flatten()
 * ```
 * @receiver CompletableFuture<out CompletableFuture<V>> 평탄화할 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @return CompletableFuture<V> 평탄화된 [CompletableFuture]
 * @see [CompletableFuture.dereference]
 */
fun <V> CompletableFuture<out CompletableFuture<V>>.flatten(
    executor: Executor = ForkJoinExecutor,
): CompletableFuture<V> =
    flatMap(executor) { it }

/**
 * `CompletableFuture<CompletableFuture<V>>`를 `CompletableFuture<V>`로 평탄화합니다.
 *
 * @receiver CompletableFuture<out CompletableFuture<V>> 평탄화할 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @return CompletableFuture<V> 평탄화된 [CompletableFuture]
 * @see [CompletableFuture.flatten]
 */
fun <V> CompletableFuture<out CompletableFuture<V>>.dereference(
    executor: Executor = ForkJoinExecutor,
): CompletableFuture<V> =
    flatten(executor)

/**
 * `CompletableFuture<V>`를 `CompletableFuture<CompletableFuture<V>>`로 감싸서 반환합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { 42 }
 * val wrapped: CompletableFuture<CompletableFuture<Int>> = future.wrap()
 * ```
 *
 * @receiver CompletableFuture<V> 감쌀 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @return CompletableFuture<CompletableFuture<V>> 감싼 [CompletableFuture]
 */
fun <V> CompletableFuture<V>.wrap(
    executor: Executor = ForkJoinExecutor,
): CompletableFuture<CompletableFuture<V>> =
    map(executor) { CompletableFuture.completedFuture(it) }

/**
 * `CompletableFuture<V>` 완료되면 결과를 [predicate]를 통해 검증하고, 검증에 실패하면 예외를 발생시킵니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { 42 }
 * val result: CompletableFuture<Int> = future.filter { it > 10 }
 * ```
 *
 * @receiver CompletableFuture<V> 검증할 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param predicate 검증 함수
 */
inline fun <V> CompletableFuture<V>.filter(
    executor: Executor = ForkJoinExecutor,
    crossinline predicate: (value: V) -> Boolean,
): CompletableFuture<V> {
    return map(executor) {
        if (predicate(it)) it
        else throw NoSuchElementException("CompletableFuture.filters predicate is not satisfied. result=$it")
    }
}

/**
 * `CompletableFuture<A>` 와 `CompletableFuture<B>` 를 결합하여 `CompletableFuture<R>` 를 생성합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { 42 }
 * val other: CompletableFuture<String> = futureOf { "Hello" }
 * val result: CompletableFuture<String> = future.zip(other) { a, b -> "$a $b" }
 * val value: String = result.get()  // "42 Hello"
 * ```
 * @receiver CompletableFuture<A> 첫 번째 [CompletableFuture]
 * @param other CompletableFuture<B> 두 번째 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param zipper 결합 함수
 * @return CompletableFuture<R> 결합된 [CompletableFuture]
 */
inline fun <A, B, R> CompletableFuture<A>.zip(
    other: CompletionStage<out B>,
    executor: Executor = ForkJoinExecutor,
    crossinline zipper: (A, B) -> R,
): CompletableFuture<R> =
    thenCombineAsync(other, { a, b -> zipper(a, b) }, executor)

/**
 * `CompletableFuture<A>` 와 `CompletionStage<B>` 를 결합하여 `CompletableFuture<Pair<A, B>>` 를 생성합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { 42 }
 * val other: CompletableFuture<String> = futureOf { "Hello" }
 * val result: CompletableFuture<Pair<Int, String>> = future.zip(other)
 * val value: Pair<Int, String> = result.get()  // (42, "Hello")
 * ```
 *
 * @receiver CompletableFuture<A> 첫 번째 [CompletableFuture]
 * @param other CompletionStage<B> 두 번째 [CompletionStage]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @return CompletableFuture<Pair<A, B>> 결합된 [CompletableFuture]
 * @see [CompletableFuture.zip]
 */
fun <A, B> CompletableFuture<A>.zip(
    other: CompletionStage<out B>,
    executor: Executor = ForkJoinExecutor,
): CompletableFuture<Pair<A, B>> =
    thenCombineAsync(other, { a, b -> a to b }, executor)


/**
 * 예외가 발생하면 보상하는 함수를 통한 결과 값을 반환하도록 합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { throw RuntimeException("error") }
 * val result: CompletableFuture<Int> = future.recover { 42 }
 * val value: Int = result.get()  // 42
 * ```
 *
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param action 예외 발생 시 복구 코드
 * @return CompletableFuture<V> 복구된 [CompletableFuture]
 */
inline fun <V> CompletableFuture<V>.recover(
    crossinline action: (error: Throwable) -> V,
): CompletableFuture<V> =
    exceptionally { e -> action(e.cause ?: e) }


/**
 * 예외가 발생하면 보상하는 함수를 통한 결과 값을 반환하도록 합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { throw RuntimeException("error") }
 * val result: CompletableFuture<Int> = future.recoverWith { futureOf { 42 } }
 * val value: Int = result.get()  // 42
 * ```
 *
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param action 예외 발생 시 복구 코드
 * @return CompletableFuture<V> 복구된 [CompletableFuture]
 *
 * @see [CompletableFuture.fallbackTo]
 */
inline fun <V> CompletableFuture<V>.recoverWith(
    executor: Executor = ForkJoinExecutor,
    crossinline action: (error: Throwable) -> CompletableFuture<V>,
): CompletableFuture<V> {
    val promise = CompletableFuture<V>()

    onComplete(
        executor,
        successHandler = { result -> promise.complete(result) },
        failureHandler = {
            action(it).onComplete(
                executor,
                successHandler = { result -> promise.complete(result) },
                failureHandler = { error -> promise.completeExceptionally(error) })
        })

    return promise
}

/**
 * [CompletableFuture]가 예외를 발생 시킬 시에, 예외를 매핑한 후 예외를 발생시킵니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { throw RuntimeException("error") }
 * val result: CompletableFuture<Int> = future.mapError { IllegalArgumentException("error") }
 * val value: Int = result.get()  // throw IllegalArgumentException("error")
 * ```
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param E 매핑할 예외 수형
 * @param mapper 예외 매핑 함수
 * @return CompletableFuture<V> 매핑된 [CompletableFuture]
 * @see [CompletableFuture.recover]
 */
inline fun <V, reified E: Throwable> CompletableFuture<V>.mapError(
    crossinline mapper: (error: E) -> Throwable,
): CompletableFuture<V> =
    exceptionally {
        when (val error = it.cause ?: it) {
            is E -> throw mapper(error)
            else -> throw error
        }
    }

/**
 * 예외가 발생하면 보상하는 함수를 통한 결과 값을 반환하도록 합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { throw RuntimeException("error") }
 * val result: CompletableFuture<Int> = future.fallbackTo { futureOf { 42 } }
 * val value: Int = result.get()  // 42
 * ```
 *
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param fallback 예외 발생 시 복구 코드
 * @return CompletableFuture<V> 복구된 [CompletableFuture]
 *
 * @see [CompletableFuture.recover]
 * @see [CompletableFuture.recoverWith]
 */
inline fun <V> CompletableFuture<V>.fallbackTo(
    executor: Executor = ForkJoinExecutor,
    crossinline fallback: () -> CompletableFuture<V>,
): CompletableFuture<V> =
    recoverWith(executor) { fallback() }


/**
 * [CompletableFuture]가 실패하면 [errorHandler]를 실행합니다.
 *
 * ```
 * futureOf { throw RuntimeException("error") }
 *    .onFailure { error -> log.error(error) }
 *    .get()  // throw RuntimeException("error")
 * ```
 *
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param errorHandler 실패 시 실행할 함수
 * @return CompletableFuture<V> 실패 시 실행된 [CompletableFuture]
 */
inline fun <V> CompletableFuture<V>.onFailure(
    executor: Executor = ForkJoinExecutor,
    crossinline errorHandler: (error: Throwable) -> Unit,
): CompletableFuture<V> =
    whenCompleteAsync(
        { _, error ->
            if (error != null) {
                errorHandler(error.cause ?: error)
            }
        },
        executor
    )

/**
 * [CompletableFuture]가 완료되면 [successHandler]를 실행합니다.
 *
 * ```
 * futureOf { 42 }
 *   .onSuccess { result -> log.info(result) }
 *   .get()  // 42
 * ```
 *
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param successHandler 성공 시 실행할 함수
 * @return CompletableFuture<V> 성공 시 실행된 [CompletableFuture]
 */
inline fun <V> CompletableFuture<V>.onSuccess(
    executor: Executor = ForkJoinExecutor,
    crossinline successHandler: (result: V) -> Unit,
): CompletableFuture<V> =
    whenCompleteAsync(
        { result, error -> if (error == null) successHandler(result) },
        executor
    )

/**
 * [CompletableFuture]가 완료되면 [successHandler]를 실행하고, 실패하면 [failureHandler]를 실행합니다.
 *
 * ```
 * futureOf { 42 }
 *  .onComplete(
 *    successHandler = { result -> log.info(result) },
 *    failureHandler = { error -> log.error(error) }
 *  )
 *  .get()  // 42
 * ```
 *
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param successHandler 성공 시 실행할 함수
 * @param failureHandler 실패 시 실행할 함수
 * @return CompletableFuture<V> 성공 또는 실패 시 실행된 [CompletableFuture]
 */
inline fun <V> CompletableFuture<V>.onComplete(
    executor: Executor = ForkJoinExecutor,
    crossinline successHandler: (result: V) -> Unit,
    crossinline failureHandler: (error: Throwable) -> Unit,
): CompletableFuture<V> =
    whenCompleteAsync(
        { result, error ->
            if (error != null) failureHandler(error.cause ?: error)
            else successHandler(result)
        },
        executor
    )

/**
 * [CompletableFuture]가 완료되면 [completionHandler]를 실행합니다.
 *
 * ```
 * futureOf { 42 }
 *   .onComplete { result, error -> log.info(result) }
 *   .get()  // 42
 * ```
 *
 * @receiver CompletableFuture<V> 원본 [CompletableFuture]
 * @param executor Executor 비동기 작업을 수행할 [Executor]
 * @param completionHandler 완료 시 실행할 함수
 * @return CompletableFuture<V> 완료 시 실행된 [CompletableFuture]
 */
inline fun <V> CompletableFuture<V>.onComplete(
    executor: Executor = ForkJoinExecutor,
    crossinline completionHandler: (result: V?, error: Throwable?) -> Unit,
): CompletableFuture<V> =
    whenCompleteAsync(
        { result, error -> completionHandler(result, error) },
        executor
    )

/**
 * [CompletableFuture] 가 실패한 것인지 확인합니다.
 */
val <V> CompletableFuture<V>.isFailed: Boolean get() = this.isCompletedExceptionally

/**
 * [CompletableFuture] 가 성공한 것인지 확인합니다.
 * 완료되었으며, 예외 없이 정상적으로 완료되고, 취소되지 않은 경우에만 `true`를 반환합니다.
 */
val <V> CompletableFuture<V>.isSuccess: Boolean
    get() = this.isDone && !this.isCompletedExceptionally && !this.isCancelled

/**
 * 제한된 사간([duration]) ]안에 [CompletableFuture]의 결과값을 반환합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { Thread.sleep(1000); 42 }
 * val result: Int = future.join(2.seconds)  // 42
 * ```
 *
 * @param duration 최대 대기 시간
 * @return V 결과값
 * @throws [java.util.concurrent.TimeoutException] 제한된 시간 내에 결과값을 얻지 못한 경우
 */
fun <V> CompletableFuture<V>.join(duration: Duration): V {
    return try {
        get(duration.inWholeNanoseconds, TimeUnit.NANOSECONDS)
    } catch (e: Throwable) {
        throw e.cause ?: e
    }
}

/**
 * 제한된 사간안에 [CompletableFuture]의 결과값을 반환하거나, [defaultValue]를 반환합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { Thread.sleep(1000); 42 }
 * val result: Int = future.join(2.seconds, 0)  // 42
 * ```
 *
 * @param duration 최대 대기 시간
 * @param defaultValue 기본값
 * @return V 결과값
 * @throws [java.util.concurrent.TimeoutException] 제한된 시간 내에 결과값을 얻지 못한 경우
 */
fun <V> CompletableFuture<V>.join(duration: Duration, defaultValue: V): V =
    runCatching { join(duration) ?: defaultValue }.getOrDefault(defaultValue)

/**
 * 제한된 사간안에 [CompletableFuture]의 결과값을 반환하거나, null을 반환합니다.
 *
 * ```
 * val future: CompletableFuture<Int> = futureOf { Thread.sleep(1000); 42 }
 * val result: Int? = future.joinOrNull(2.seconds)  // 42
 * ```
 *
 * @param duration 최대 대기 시간
 * @return V? 결과값 또는 null
 */
fun <V> CompletableFuture<V>.joinOrNull(duration: Duration): V? =
    runCatching { get(duration.inWholeNanoseconds, TimeUnit.NANOSECONDS) }.getOrNull()
