package io.bluetape4k.cache.memoizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.await
import org.redisson.api.RMap
import java.util.concurrent.ConcurrentHashMap

/**
 * [RMap]을 사용하는 코루틴 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val map: RMap<Int, Int> = redisson.getMap("squares")
 * val memoizer = map.suspendMemoizer { key -> computeExpensive(key) }
 * val result = memoizer(5)
 * ```
 *
 * @param T  key type
 * @param R  value type
 * @receiver Redisson [RMap] 인스턴스
 * @param evaluator 실행할 suspend 함수
 * @return [RedissonSuspendMemoizer] instance
 */
fun <T: Any, R: Any> RMap<T, R>.suspendMemoizer(evaluator: suspend (T) -> R): RedissonSuspendMemoizer<T, R> =
    RedissonSuspendMemoizer(this, evaluator)

/**
 * [RMap]을 사용하는 코루틴 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = (suspend { key: Int -> computeExpensive(key) }).memoizer(map)
 * val result = memoizer(5)
 * ```
 *
 * @param T key type
 * @param R value type
 * @receiver 실행할 suspend 함수
 * @param map Redisson [RMap] 인스턴스
 * @return [RedissonSuspendMemoizer] instance
 */
fun <T: Any, R: Any> (suspend (T) -> R).memoizer(map: RMap<T, R>): RedissonSuspendMemoizer<T, R> =
    RedissonSuspendMemoizer(map, this)

/**
 * [evaluator]가 실행한 결과를 [map]에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 [evaluator]의 중복 실행을 줄입니다.
 *
 * ```kotlin
 * val map: RMap<Int, Int> = redisson.getMap("squares")
 * val memoizer = RedissonSuspendMemoizer(map) { key -> computeExpensive(key) }
 * val result1 = memoizer(5)  // 계산 후 저장
 * val result2 = memoizer(5)  // Redis에서 반환
 * ```
 *
 * @property map       Redisson [RMap] 인스턴스
 * @property evaluator 실행할 suspend 함수
 */
class RedissonSuspendMemoizer<T: Any, R: Any>(
    val map: RMap<T, R>,
    val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<T, Deferred<R>>()

    override suspend fun invoke(key: T): R {
        inFlight[key]?.let { return it.await() }

        val deferred = CompletableDeferred<R>()
        val existing = inFlight.putIfAbsent(key, deferred)
        if (existing != null) return existing.await()

        try {
            val cached = map.getAsync(key).await()
            if (cached != null) {
                deferred.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val winner = map.putIfAbsentAsync(key, evaluated).await() ?: evaluated
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
        log.debug { "모든 메모이제이션 값 삭제: map=${map.name}" }
        map.clearAsync().await()
    }
}
