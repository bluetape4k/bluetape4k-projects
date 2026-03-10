package io.bluetape4k.redis.lettuce.memorizer

import io.bluetape4k.cache.memorizer.AsyncMemorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.RedisMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [RedisMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memorizer = redisMap.asyncMemorizer { key -> expensiveCompute(key) }
 * val result = memorizer("key1").join()
 * ```
 *
 * @receiver [RedisMap] 인스턴스
 * @param evaluator 키를 받아 값을 계산하는 함수
 * @return [LettuceAsyncMemorizer] 인스턴스
 */
@Deprecated(
    message = "asyncMemorizer()는 asyncMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("asyncMemoizer(evaluator)", "io.bluetape4k.redis.lettuce.memoizer.asyncMemoizer"),
    level = DeprecationLevel.WARNING
)
fun RedisMap.asyncMemorizer(evaluator: (String) -> String): LettuceAsyncMemorizer =
    LettuceAsyncMemorizer(this, evaluator)

/**
 * [RedisMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memorizer = { key: String -> expensiveCompute(key) }.asyncMemorizer(redisMap)
 * val result = memorizer("key1").join()
 * ```
 *
 * @receiver 키를 받아 값을 계산하는 함수
 * @param map [RedisMap] 인스턴스
 * @return [LettuceAsyncMemorizer] 인스턴스
 */
@Deprecated(
    message = "asyncMemorizer()는 asyncMemoizer()로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("asyncMemoizer(map)", "io.bluetape4k.redis.lettuce.memoizer.asyncMemoizer"),
    level = DeprecationLevel.WARNING
)
fun ((String) -> String).asyncMemorizer(map: RedisMap): LettuceAsyncMemorizer =
    LettuceAsyncMemorizer(map, this)

/**
 * [RedisMap]을 사용하여 함수 실행 결과를 [CompletableFuture] 기반으로 메모이제이션하는 구현체입니다.
 *
 * 동일 JVM에서 동시 요청 시 in-flight [CompletableFuture]를 공유하여 중복 실행을 방지합니다.
 *
 * ```kotlin
 * val memorizer = LettuceAsyncMemorizer(redisMap) { key -> expensiveCompute(key) }
 * val result1 = memorizer("key1").join()
 * val result2 = memorizer("key1").join()  // 캐시된 값 반환
 * ```
 *
 * @property map       [RedisMap] 인스턴스
 * @property evaluator 키를 받아 값을 계산하는 함수
 */
@Deprecated(
    message = "LettuceAsyncMemorizer는 LettuceAsyncMemoizer로 이름이 변경되었습니다.",
    replaceWith = ReplaceWith("LettuceAsyncMemoizer", "io.bluetape4k.redis.lettuce.memoizer.LettuceAsyncMemoizer"),
    level = DeprecationLevel.WARNING
)
class LettuceAsyncMemorizer(
    val map: RedisMap,
    val evaluator: (String) -> String,
) : AsyncMemorizer<String, String> {

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
