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
 * ## 스레드 안전성
 * 내부 `inFlight` 맵은 [ConcurrentHashMap] 기반이므로 멀티스레드 환경에서 안전하게 사용할 수 있습니다.
 * 단, [evaluator] 내부에서 블로킹 I/O가 발생하는 경우 해당 스레드가 블로킹됩니다.
 * 코루틴 환경에서는 `Dispatchers.IO`에서 호출하거나 [RedissonSuspendMemoizer]를 사용하세요.
 *
 * ## 경쟁 조건 처리
 * 1. 캐시 miss 시 [CompletableFuture] promise를 `inFlight`에 등록한다.
 * 2. 동시에 같은 키를 요청한 다른 스레드는 등록된 promise를 `join()`으로 대기한다.
 * 3. 먼저 등록한 스레드가 [evaluator]를 실행하고 결과를 [map]에 저장한 뒤 promise를 완료시킨다.
 * 4. 완료 후 `inFlight`에서 해당 키를 제거한다.
 *
 * ```kotlin
 * val map: RMap<Int, Int> = redisson.getMap("squares")
 * val memoizer = RedissonMemoizer(map) { key -> key * key }
 * val result1 = memoizer(5)  // 25 — 계산 후 Redis에 저장
 * val result2 = memoizer(5)  // 25 — Redis에서 반환
 * ```
 *
 * @property map       결과를 저장하고 조회할 Redisson [RMap] 인스턴스
 * @property evaluator 캐시 miss 시 실행할 동기 함수
 */
class RedissonMemoizer<T: Any, R: Any>(
    val map: RMap<T, R>,
    val evaluator: (T) -> R,
): Memoizer<T, R> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<T, CompletableFuture<R>>()

    /**
     * 주어진 [key]에 대한 결과를 반환한다.
     *
     * - Redis([map])에 캐시된 값이 있으면 즉시 반환한다.
     * - 캐시 miss 시 [evaluator]를 실행하고 결과를 Redis에 저장한 뒤 반환한다.
     * - 동일 키에 대한 동시 요청이 있으면 먼저 진행 중인 연산의 결과를 공유하여
     *   [evaluator]가 중복 실행되지 않도록 한다.
     * - [evaluator]에서 예외가 발생하면 promise를 예외 상태로 완료하고 예외를 다시 던진다.
     *
     * @param key 조회할 키
     * @return 키에 대응하는 값
     */
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

    /**
     * Redis([map])에 저장된 모든 메모이제이션 값을 삭제한다.
     */
    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: map=${map.name}" }
        map.clear()
    }
}
