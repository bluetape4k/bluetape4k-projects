package io.bluetape4k.redis.lettuce.memoizer

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.LettuceMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [RedisMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = redisMap.asyncMemoizer { key -> expensiveCompute(key) }
 * val result = memoizer("key1").join()
 * ```
 */
fun LettuceMap<String>.asyncMemoizer(evaluator: (String) -> String): LettuceAsyncMemoizer =
    LettuceAsyncMemoizer(this, evaluator)

/**
 * [RedisMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { key: String -> expensiveCompute(key) }.asyncMemoizer(redisMap)
 * val result = memoizer("key1").join()
 * ```
 */
fun ((String) -> String).asyncMemoizer(map: LettuceMap<String>): LettuceAsyncMemoizer =
    LettuceAsyncMemoizer(map, this)

/**
 * [RedisMap]을 사용하여 함수 실행 결과를 [CompletableFuture] 기반으로 메모이제이션하는 구현체입니다.
 */
class LettuceAsyncMemoizer(
    val map: LettuceMap<String>,
    val evaluator: (String) -> String,
) : AsyncMemoizer<String, String> {

    companion object : KLogging()

    private val inFlight = ConcurrentHashMap<String, CompletableFuture<String>>()

    override fun invoke(key: String): CompletableFuture<String> {
        inFlight[key]?.let { return it }

        val promise = CompletableFuture<String>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing

        map.getAsync(key).thenCompose { cached ->
            if (cached != null) {
                CompletableFuture.completedFuture(cached)
            } else {
                val evaluated = evaluator(key)
                map.putIfAbsentAsync(key, evaluated).thenCompose { isNew ->
                    if (isNew) CompletableFuture.completedFuture(evaluated)
                    else map.getAsync(key).thenApply { it ?: evaluated }
                }
            }
        }.whenComplete { result, ex ->
            if (ex != null) {
                promise.completeExceptionally(ex)
            } else {
                promise.complete(result)
            }
            inFlight.remove(key, promise)
        }

        return promise
    }

    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: mapKey=${map.mapKey}" }
        map.clear()
    }
}
