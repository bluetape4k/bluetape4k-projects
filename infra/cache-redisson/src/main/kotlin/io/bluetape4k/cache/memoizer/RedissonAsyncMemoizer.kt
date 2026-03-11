package io.bluetape4k.cache.memoizer

import io.bluetape4k.concurrent.completableFutureOf
import io.bluetape4k.concurrent.flatMap
import io.bluetape4k.concurrent.map
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.redisson.api.RMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap

/**
 * [RMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val map: RMap<Int, Int> = redisson.getMap("squares")
 * val memoizer = map.asyncMemoizer { key -> CompletableFuture.supplyAsync { key * key } }
 * val result = memoizer(5).get()  // 25
 * ```
 *
 * @param evaluator 수행할 비동기 함수
 * @return [RedissonAsyncMemoizer] 인스턴스
 */
fun <T: Any, R: Any> RMap<T, R>.asyncMemoizer(evaluator: (T) -> CompletionStage<R>): RedissonAsyncMemoizer<T, R> =
    RedissonAsyncMemoizer(this, evaluator)

/**
 * [RMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { key: Int -> CompletableFuture.supplyAsync { key * key } }.asyncMemoizer(map)
 * val result = memoizer(5).get()
 * ```
 *
 * @receiver 실행할 비동기 함수
 * @param map 수행결과를 저장할 Redisson [RMap] 인스턴스
 * @return [RedissonAsyncMemoizer] 인스턴스
 */
fun <T: Any, R: Any> ((T) -> CompletionStage<R>).asyncMemoizer(map: RMap<T, R>): RedissonAsyncMemoizer<T, R> =
    RedissonAsyncMemoizer(map, this)

/**
 * 비동기 [evaluator] 결과를 Redis에 저장하는 메모이저입니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 [evaluator]의 중복 실행을 줄입니다.
 *
 * ## 동작 방식
 * 1. `inFlight`에 해당 키가 없으면 새 [CompletableFuture] promise를 등록한다.
 * 2. Redis([map])에 캐시된 값이 있으면 promise를 즉시 완료한다.
 * 3. 캐시 miss 시 [evaluator]를 비동기 실행하고, 결과를 `putIfAbsentAsync`로 Redis에 저장한다.
 * 4. promise 완료 후 `inFlight`에서 해당 키를 제거한다.
 *
 * ```kotlin
 * val map: RMap<Int, Int> = redisson.getMap("squares")
 * val memoizer = AsyncRedissonMemoizer(map) { key -> CompletableFuture.supplyAsync { key * key } }
 * val result = memoizer(5).get()  // 25
 * ```
 *
 * @property map 수행결과를 저장할 Redisson [RMap] 인스턴스
 * @property evaluator 수행할 비동기 함수
 */
class RedissonAsyncMemoizer<T: Any, R: Any>(
    val map: RMap<T, R>,
    val evaluator: (T) -> CompletionStage<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val inFlight = ConcurrentHashMap<T, CompletableFuture<R>>()

    /**
     * 주어진 [key]에 대한 비동기 결과를 반환한다.
     *
     * - Redis([map])에 캐시된 값이 있으면 완료된 [CompletableFuture]를 반환한다.
     * - 캐시 miss 시 [evaluator]를 비동기 실행하고 결과를 Redis에 저장한다.
     * - 동일 키에 대한 동시 요청은 동일한 [CompletableFuture]를 공유하여 [evaluator] 중복 실행을 방지한다.
     *
     * @param key 조회할 키
     * @return 키에 대응하는 값의 [CompletableFuture]
     */
    override fun invoke(key: T): CompletableFuture<R> {
        // 1. in-flight 확인 또는 신규 등록
        val promise = CompletableFuture<R>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing

        // 2. Redis에서 캐시 조회 후, miss 시 evaluator 실행 (Virtual Thread-safe)
        map.getAsync(key)
            .toCompletableFuture()
            .whenComplete { cached, error ->
                if (error != null) {
                    inFlight.remove(key)
                    promise.completeExceptionally(error)
                } else if (cached != null) {
                    inFlight.remove(key)
                    promise.complete(cached)
                } else {
                    runCatching { evaluator(key).toCompletableFuture() }
                        .fold(
                            onSuccess = { future ->
                                future.whenComplete { value, evalError ->
                                    inFlight.remove(key)
                                    if (evalError != null) {
                                        promise.completeExceptionally(evalError)
                                    } else {
                                        map.putIfAbsentAsync(key, value)
                                        promise.complete(value)
                                    }
                                }
                            },
                            onFailure = { evalError ->
                                inFlight.remove(key)
                                promise.completeExceptionally(evalError)
                            }
                        )
                }
            }

        return promise
    }

    /**
     * in-flight 항목과 Redis([map])에 저장된 모든 메모이제이션 값을 삭제한다.
     *
     * **주의**: 진행 중인 in-flight 연산이 있을 때 호출하면 해당 연산의 결과가
     * Redis에 저장되지 않을 수 있습니다. clear 후 새 요청이 들어오면 [evaluator]가 재실행됩니다.
     */
    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: map=${map.name}" }
        inFlight.clear()
        map.clear()
    }
}
