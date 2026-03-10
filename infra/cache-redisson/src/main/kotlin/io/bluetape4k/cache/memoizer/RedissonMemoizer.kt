package io.bluetape4k.cache.memoizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.redisson.api.RMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [RMap]을 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val map: RMap<Int, Int> = redisson.getMap("squares")
 * val memoizer = map.memoizer { key -> key * key }
 * val result1 = memoizer(5)   // 계산 후 Redis에 저장
 * val result2 = memoizer(5)   // Redis에서 반환
 * ```
 *
 * @param T  key type
 * @param R  value type
 * @receiver Redisson [RMap] 인스턴스
 * @param evaluator 실행할 함수
 * @return [RedissonMemoizer] instance
 */
fun <T: Any, R: Any> RMap<T, R>.memoizer(evaluator: (T) -> R): RedissonMemoizer<T, R> =
    RedissonMemoizer(this, evaluator)

/**
 * [RMap]을 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { key: Int -> key * key }.memoizer(map)
 * val result = memoizer(5)
 * ```
 *
 * @param T key type
 * @param R value type
 * @receiver 실행할 함수
 * @param map Redisson [RMap] 인스턴스
 * @return [RedissonMemoizer] instance
 */
fun <T: Any, R: Any> ((T) -> R).memoizer(map: RMap<T, R>): RedissonMemoizer<T, R> =
    RedissonMemoizer(map, this)

/**
 * [evaluator]가 실행한 결과를 [map]에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 [evaluator]의 중복 실행을 줄입니다.
 *
 * ```kotlin
 * val map: RMap<Int, Int> = redisson.getMap("squares")
 * val memoizer = RedissonMemoizer(map) { key -> key * key }
 * val result1 = memoizer(5)  // 25 — 계산 후 Redis에 저장
 * val result2 = memoizer(5)  // 25 — Redis에서 반환
 * ```
 *
 * @property map       Redisson [RMap] 인스턴스
 * @property evaluator 실행할 함수
 */
class RedissonMemoizer<T: Any, R: Any>(
    val map: RMap<T, R>,
    val evaluator: (T) -> R,
): Memoizer<T, R> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<T, CompletableFuture<R>>()

    override fun invoke(key: T): R {
        inFlight[key]?.let { return it.join() }

        val promise = CompletableFuture<R>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing.join()

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
        log.debug { "모든 메모이제이션 값 삭제: map=${map.name}" }
        map.clear()
    }
}
