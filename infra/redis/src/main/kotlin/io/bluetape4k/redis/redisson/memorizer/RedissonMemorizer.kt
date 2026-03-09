package io.bluetape4k.redis.redisson.memorizer

import io.bluetape4k.cache.memorizer.Memorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.redisson.api.RMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [evaluator]가 실행한 결과를 Redis 에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ```
 * val map: RMap<Int, Int> = redisson.getMap("map")
 * val memorizer = map.memorizer { key -> key * key }
 * val result1 = memorizer(4) // 16
 * val result2 = memorizer(4) // 16  // memorized value
 * ```
 *
 * @param T  key type
 * @param R  value type
 * @receiver Redisson [RMap] 인스턴스
 * @param evaluator 실행할 함수
 * @return [RedissonMemorizer] instance
 */
fun <T: Any, R: Any> RMap<T, R>.memorizer(evaluator: (T) -> R): RedissonMemorizer<T, R> =
    RedissonMemorizer(this, evaluator)

/**
 * 함수의 실행 결과를 Redisson `RMap` 에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ```
 * val map: RMap<String, Int> = redisson.getMap("map")
 * val memorizer = { key: Int -> key * key }.memorizer(map)
 * val result1 = memorizer(4) // 16
 * val result2 = memorizer(4) // 16  // memorized value
 * ```
 *
 * @param T key type
 * @param R value type
 * @receiver 실행할 함수
 * @param map Redisson [RMap] 인스턴스
 * @return [RedissonMemorizer] instance
 */
fun <T: Any, R: Any> ((T) -> R).memorizer(map: RMap<T, R>): RedissonMemorizer<T, R> =
    RedissonMemorizer(map, this)

/**
 * [evaluator]가 실행한 결과를 [map]에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 [evaluator]의 중복 실행을 줄입니다.
 *
 * ```
 * val map: RMap<Int, Int> = redisson.getMap("map")
 * val memorizer = RedissonMemorizer(map) { key -> key * key }
 * val result1 = memorizer(4) // 16
 * val result2 = memorizer(4) // 16  // memorized value
 * ```
 *
 * @property map       Redisson [RMap] 인스턴스
 * @property evaluator 실행할 함수
 */
class RedissonMemorizer<T: Any, R: Any>(
    val map: RMap<T, R>,
    val evaluator: (T) -> R,
): Memorizer<T, R> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<T, CompletableFuture<R>>()

    override fun invoke(key: T): R {
        inFlight[key]?.let { return it.join() }

        val promise = CompletableFuture<R>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) {
            return existing.join()
        }

        try {
            val cached = map.get(key)
            if (cached != null) {
                promise.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val winner = map.putIfAbsent(key, evaluated) ?: evaluated
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
        log.debug { "Clear all memorized values. map=${map.name}" }
        map.clear()
    }
}
