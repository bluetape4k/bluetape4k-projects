package io.bluetape4k.redis.lettuce.memoizer

import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.RedisMap
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * [RedisMap]을 사용하는 코루틴 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = redisMap.suspendMemoizer { key -> expensiveComputeSuspend(key) }
 * val result = memoizer("key1")
 * ```
 */
fun RedisMap.suspendMemoizer(evaluator: suspend (String) -> String): LettuceSuspendMemoizer =
    LettuceSuspendMemoizer(this, evaluator)

/**
 * [RedisMap]을 사용하여 함수 실행 결과를 코루틴 기반으로 메모이제이션하는 구현체입니다.
 */
class LettuceSuspendMemoizer(
    val map: RedisMap,
    val evaluator: suspend (String) -> String,
) : SuspendMemoizer<String, String> {

    companion object : KLogging()

    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<String>>()

    override suspend fun invoke(key: String): String {
        inFlight[key]?.let { return it.await() }

        val deferred = CompletableDeferred<String>()
        val existing = inFlight.putIfAbsent(key, deferred)
        if (existing != null) return existing.await()

        try {
            val cached = map.getSuspending(key)
            if (cached != null) {
                deferred.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val isNew = map.putIfAbsentSuspending(key, evaluated)
            val winner = if (isNew) evaluated else (map.getSuspending(key) ?: evaluated)
            deferred.complete(winner)
            return winner
        } catch (e: Throwable) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            inFlight.remove(key, deferred)
        }
    }

    override suspend fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: mapKey=${map.mapKey}" }
        map.clearSuspending()
    }
}
