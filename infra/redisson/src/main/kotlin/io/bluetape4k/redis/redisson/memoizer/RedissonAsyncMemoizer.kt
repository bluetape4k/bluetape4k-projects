package io.bluetape4k.redis.redisson.memoizer

import io.bluetape4k.cache.memoizer.AsyncMemoizer
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

    override fun invoke(key: T): CompletableFuture<R> {
        return inFlight.computeIfAbsent(key) {
            val promise = map
                .getAsync(key)
                .toCompletableFuture()
                .flatMap { cached ->
                    if (cached != null) {
                        completableFutureOf(cached)
                    } else {
                        evaluator(key)
                            .toCompletableFuture()
                            .flatMap { value ->
                                map.putIfAbsentAsync(key, value)
                                    .toCompletableFuture()
                                    .map { previous -> previous ?: value }
                            }
                    }
                }
                .toCompletableFuture()

            promise.whenComplete { _, _ -> inFlight.remove(key, promise) }
            promise
        }
    }

    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: map=${map.name}" }
        inFlight.clear()
        map.clear()
    }
}
