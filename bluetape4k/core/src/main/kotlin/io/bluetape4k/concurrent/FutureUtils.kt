package io.bluetape4k.concurrent

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.StructuredTaskScope

/**
 * 비동기 작업을 위한 유틸리티 함수들을 제공합니다.
 */
object FutureUtils {

    // TODO: 이건 VirtualThreadUtils 로 이동 
    /**
     * [futures] 중 가장 처음에 완료되는 것을 반환하고, 나머지 futures 는 취소합니다.
     *
     * ```
     * val future1 = async { delay(1000); 1 }
     * val future2 = async { delay(2000); 2 }
     * val future3 = async { delay(3000); 3 }
     * val first = firstCompleted(listOf(future1, future2, future3))    // 1
     * ```
     *
     * @param V
     * @param futures [CompletableFuture]의 컬렉션
     * @return 가장 먼저 완료된 [CompletableFuture]
     */
    fun <V> firstCompleted(
        futures: Iterable<CompletableFuture<V>>,
    ): CompletableFuture<V> {
        val promise = CompletableFuture<V>()

        return StructuredTaskScope.ShutdownOnSuccess<V>("first-completed", Thread.ofVirtual().factory()).use { scope ->
            futures.forEach { item ->
                scope.fork {
                    item.get()
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
     * `List<CompletableFuture>`을 `CompletableFuture<List>` 로 변환합니다.
     *
     * ```
     * val futures = listOf(
     *    futureOf { 1 },
     *    futureOf { 2 },
     *    futureOf { 3 },
     * )
     * val result = allAsList(futures).join()    // [1, 2, 3]
     * ```
     *
     * @param futures 결과를 기다리는 CompletableFuture 컬렉션
     * @param executor Executor
     * @return 모두 완료된 결과. CompletableFuture<List<V>> 인스턴스
     */
    fun <V> allAsList(
        futures: Iterable<CompletableFuture<V>>,
        executor: Executor = ForkJoinExecutor,
    ): CompletableFuture<List<V>> {

        return CompletableFuture.supplyAsync<List<V>> {
            CompletableFuture.allOf(*futures.toList().toTypedArray()).join()
            futures.map { it.get() }
        }
    }

    /**
     * [futures] 중에 가장 먼저 완료되는 결과를 반환하는 [CompletableFuture]를 생성합니다.
     *
     * ```
     * val future1 = async { delay(1000); 1 }
     * val future2 = async { delay(2000); 2 }
     * val future3 = async { delay(3000); 3 }
     * val first = anyOf(listOf(future1, future2, future3))    // 1
     * ```
     *
     * @param futures 결과를 기다리는 CompletableFuture 컬렉션
     * @return 가장 먼저 완료된 CompletableFuture의 결과를 반환하는 CompletableFuture
     */
    @Suppress("UNCHECKED_CAST")
    fun <V> anyOf(
        futures: Iterable<CompletableFuture<V>>,
    ): CompletableFuture<V> {
        return CompletableFuture.anyOf(*futures.toList().toTypedArray()) as CompletableFuture<V>
    }

    /**
     * `CompletableFuture` 컬렉션에서 성공한 결과들만 반환하도록 합니다.
     *
     * ```
     * val futures = listOf(
     *   futureOf { 1 },
     *   futureOf { throw RuntimeException() },
     *   futureOf { 3 },
     * )
     * val result = successfulAsList(futures).join()    // [1, 3]
     * ```
     *
     * @receiver Iterable<CompletableFuture<V>>
     * @param executor Executor
     * @return CompletableFuture<List<V>>
     */
    fun <V> successfulAsList(
        futures: Iterable<CompletableFuture<V>>,
        executor: Executor = ForkJoinExecutor,
    ): CompletableFuture<List<V>> {
        return CompletableFuture.supplyAsync<List<V>> {
            runCatching { CompletableFuture.allOf(*futures.toList().toTypedArray()).join() }
            futures.filter { it.isSuccess }.map { it.get() }
        }
//        return futures.fold(completableFutureOf(mutableListOf<V>())) { futureAcc, future ->
//            futureAcc.flatMap(executor) { acc ->
//                future.map(executor) {
//                    it?.run { acc.add(this) }
//                    acc
//                }
//            }
//        }.map(executor) { it.toList() }
    }


    /**
     * [iterator]의 결과를 이용하여 [op] 함수를 이용하여 결과값을 계산합니다.
     *
     * ```
     * val futures = listOf(
     *  futureOf { 1 },
     *  futureOf { 2 },
     *  futureOf { 3 },
     * )
     * val result = fold(futures) { acc, value -> acc + value }.join()    // 6
     * ```
     *
     * @param iterator Iterator<CompletableFuture<V>> 계산할 결과를 가지고 있는 Iterator
     * @param initial R 초기값
     * @param executor Executor
     * @param op Function2<R, V, R> 계산할 함수
     */
    fun <V, R> fold(
        iterator: Iterator<CompletableFuture<V>>,
        initial: R,
        executor: Executor = ForkJoinExecutor,
        op: (R, V) -> R,
    ): CompletableFuture<R> {
        return if (!iterator.hasNext()) completableFutureOf(initial)
        else iterator.next().flatMap(executor) { fold(iterator, op(initial, it), executor, op) }
    }

    /**
     * [futures]의 결과를 이용하여 [op] 함수를 이용하여 결과값을 계산합니다.
     *
     * ```
     * val futures = listOf(
     *  futureOf { 1 },
     *  futureOf { 2 },
     *  futureOf { 3 },
     * )
     * val result = fold(futures) { acc, value -> acc + value }.join()    // 6
     * ```
     *
     * @param futures Iterable<CompletableFuture<V>> 계산할 결과를 가지고 있는 Iterable
     * @param initial R 초기값
     * @param executor Executor
     * @param op Function2<R, V, R> 계산할 함수
     */
    fun <V, R> fold(
        futures: Iterable<CompletableFuture<V>>,
        initial: R,
        executor: Executor = ForkJoinExecutor,
        op: (R, V) -> R,
    ): CompletableFuture<R> {
        return fold(futures.iterator(), initial, executor, op)
    }

    /**
     * [iterator]의 결과를 이용하여 [op] 함수를 이용하여 결과값을 계산합니다.
     *
     * ```
     * val futures = listOf(
     *  futureOf { 1 },
     *  futureOf { 2 },
     *  futureOf { 3 },
     * )
     * val result = reduce(futures) { acc, value -> acc + value }.join()    // 6
     * ```
     *
     * @param iterator Iterator<CompletableFuture<V>> 계산할 결과를 가지고 있는 Iterator
     * @param executor Executor
     * @param op Function2<R, V, R> 계산할 함수
     */
    fun <V> reduce(
        iterator: Iterator<CompletableFuture<V>>,
        executor: Executor = ForkJoinExecutor,
        op: (V, V) -> V,
    ): CompletableFuture<V> {
        return if (!iterator.hasNext()) throw UnsupportedOperationException("Empty collection can't be reduced.")
        else iterator.next().flatMap(executor) { fold(iterator, it, executor, op) }
    }

    /**
     * [futures]의 결과를 이용하여 [op] 함수를 이용하여 결과값을 계산합니다.
     *
     * ```
     * val futures = listOf(
     *  futureOf { 1 },
     *  futureOf { 2 },
     *  futureOf { 3 },
     * )
     * val result = reduce(futures) { acc, value -> acc + value }.join()    // 6
     * ```
     *
     * @param futures Iterable<CompletableFuture<V>> 계산할 결과를 가지고 있는 Iterable
     * @param executor Executor
     * @param op Function2<R, V, R> 계산할 함수
     */
    fun <V> reduce(
        futures: Iterable<CompletableFuture<V>>,
        executor: Executor = ForkJoinExecutor,
        op: (V, V) -> V,
    ): CompletableFuture<V> {
        return reduce(futures.iterator(), executor, op)
    }

    /**
     * [futures]의 결과를 [action] 함수를 이용하여 변환합니다.
     *
     * ```
     * val futures = listOf(
     *  futureOf { 1 },
     *  futureOf { 2 },
     *  futureOf { 3 },
     * )
     * val result = transform(futures) { it * 2 }.join()    // [2, 4, 6]
     * ```
     *
     * @param futures Iterable<CompletableFuture<V>> 변환할 결과를 가지고 있는 Iterable
     * @param executor Executor
     * @param action Function1<V, R> 변환할 함수
     */
    fun <V, R> transform(
        futures: Iterable<CompletableFuture<V>>,
        executor: Executor = ForkJoinExecutor,
        action: (V) -> R,
    ): CompletableFuture<List<R>> {
        return futures.fold(completableFutureOf(mutableListOf<R>())) { futureAcc, future ->
            futureAcc.zip(future, executor) { acc, result ->
                acc.add(action(result))
                acc
            }
        }.map(executor) { it.toList() }
    }

    /**
     * [futures]의 결과를 [combiner] 함수를 이용하여 결합한 값을 반환합니다.
     *
     * ```
     * val futures = listOf(
     *  futureOf { 1 },
     *  futureOf { 2 },
     *  futureOf { 3 },
     * )
     * val result = combine(futures) { it.sum() }.join()    // 6
     * ```
     *
     * @param futures Iterable<CompletableFuture<V>> 변환할 결과를 가지고 있는 Iterable
     * @param executor Executor
     * @param action Function1<V, R> 변환할 함수
     */
    fun <R> combine(
        futures: Iterable<CompletableFuture<out Any?>>,
        executor: Executor = ForkJoinExecutor,
        combiner: (List<Any?>) -> R,
    ): CompletableFuture<R> {
        return futures
            .fold(completableFutureOf(mutableListOf<Any?>())) { futureAcc, future ->
                futureAcc.zip(future, executor) { acc, result ->
                    acc.add(result)
                    acc
                }
            }
            .map(executor) { it.toList() }
            .thenApplyAsync({ result -> combiner(result) }, executor)
    }
}
