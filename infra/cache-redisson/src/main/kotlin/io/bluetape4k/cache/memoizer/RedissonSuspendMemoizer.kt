package io.bluetape4k.cache.memoizer

import io.bluetape4k.logging.coroutines.KLoggingChannel
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
 * ## 경쟁 조건 처리
 * 1. 캐시 miss 시 [CompletableDeferred]를 `inFlight`에 등록한다.
 * 2. 동일 키를 동시에 요청한 다른 코루틴은 등록된 deferred를 `await()`으로 대기한다.
 * 3. 먼저 등록한 코루틴이 [evaluator]를 실행하고 결과를 Redis에 저장한 뒤 deferred를 완료시킨다.
 * 4. 완료 후 `inFlight`에서 해당 키를 제거한다.
 *
 * ## 코루틴 취소 주의사항
 * 대기 중인 코루틴이 취소되면 해당 코루틴의 `await()` 호출은 [kotlinx.coroutines.CancellationException]을
 * 던지지만, in-flight deferred 자체는 자동으로 취소되지 않습니다.
 * 즉, [evaluator]를 실행 중인 코루틴이 취소되지 않는 한 연산은 계속 진행됩니다.
 * 취소 전파가 필요한 경우 별도의 취소 처리 로직이 필요합니다.
 *
 * ```kotlin
 * val map: RMap<Int, Int> = redisson.getMap("squares")
 * val memoizer = RedissonSuspendMemoizer(map) { key -> computeExpensive(key) }
 * val result1 = memoizer(5)  // 계산 후 저장
 * val result2 = memoizer(5)  // Redis에서 반환
 * ```
 *
 * @property map       결과를 저장하고 조회할 Redisson [RMap] 인스턴스
 * @property evaluator 캐시 miss 시 실행할 suspend 함수
 */
class RedissonSuspendMemoizer<T: Any, R: Any>(
    val map: RMap<T, R>,
    val evaluator: suspend (T) -> R,
): SuspendMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val inFlight = ConcurrentHashMap<T, Deferred<R>>()

    /**
     * 주어진 [key]에 대한 결과를 suspend 방식으로 반환한다.
     *
     * - Redis([map])에 캐시된 값이 있으면 즉시 반환한다.
     * - 캐시 miss 시 [evaluator]를 실행하고 결과를 Redis에 저장한 뒤 반환한다.
     * - 동일 키에 대한 동시 요청은 동일한 [Deferred]를 공유하여 [evaluator] 중복 실행을 방지한다.
     * - [evaluator]에서 예외가 발생하면 deferred를 예외 상태로 완료하고 예외를 다시 던진다.
     *
     * @param key 조회할 키
     * @return 키에 대응하는 값
     */
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

    /**
     * Redis([map])에 저장된 모든 메모이제이션 값을 비동기로 삭제한다.
     */
    override suspend fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: map=${map.name}" }
        map.clearAsync().await()
    }
}
