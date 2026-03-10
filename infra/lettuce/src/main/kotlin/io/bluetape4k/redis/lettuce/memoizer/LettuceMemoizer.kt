package io.bluetape4k.redis.lettuce.memoizer

import io.bluetape4k.cache.memoizer.Memoizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.RedisMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [RedisMap]을 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = redisMap.memoizer { key -> expensiveCompute(key) }
 * val result1 = memoizer("key1")
 * val result2 = memoizer("key1")  // 캐시된 값 반환
 * ```
 */
fun RedisMap.memoizer(evaluator: (String) -> String): LettuceMemoizer =
    LettuceMemoizer(this, evaluator)

/**
 * [RedisMap]을 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { key: String -> expensiveCompute(key) }.memoizer(redisMap)
 * val result = memoizer("key1")
 * ```
 */
fun ((String) -> String).memoizer(map: RedisMap): LettuceMemoizer =
    LettuceMemoizer(map, this)

/**
 * [RedisMap]을 사용하여 함수 실행 결과를 메모이제이션하는 동기 구현체입니다.
 *
 * 동일 JVM에서 동시 요청 시 in-flight 연산을 공유하여 중복 실행을 방지합니다.
 *
 * ```kotlin
 * val memoizer = LettuceMemoizer(redisMap) { key -> expensiveCompute(key) }
 * val result1 = memoizer("key1")  // 계산 후 Redis에 저장
 * val result2 = memoizer("key1")  // Redis에서 반환
 * ```
 */
class LettuceMemoizer(
    val map: RedisMap,
    val evaluator: (String) -> String,
) : Memoizer<String, String> {

    companion object : KLogging()

    private val inFlight = ConcurrentHashMap<String, CompletableFuture<String>>()

    override fun invoke(key: String): String {
        inFlight[key]?.let { return it.join() }

        val promise = CompletableFuture<String>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing.join()

        try {
            val cached = map.get(key)
            if (cached != null) {
                promise.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val isNew = map.putIfAbsent(key, evaluated)
            val winner = if (isNew) evaluated else (map.get(key) ?: evaluated)
            promise.complete(winner)
            return winner
        } catch (e: Throwable) {
            promise.completeExceptionally(e)
            throw e
        } finally {
            inFlight.remove(key, promise)
        }
    }

    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: mapKey=${map.mapKey}" }
        map.clear()
    }
}
