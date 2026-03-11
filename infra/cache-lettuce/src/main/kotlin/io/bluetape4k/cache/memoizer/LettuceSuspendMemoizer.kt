package io.bluetape4k.cache.memoizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.LettuceSuspendMap
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

/**
 * [LettuceSuspendMap]을 사용하는 코루틴 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val connection = redisClient.connect(LettuceLongCodec)
 * val map = LettuceSuspendMap<Long>(connection, "memoizer:factorial")
 * val memoizer = map.suspendMemoizer { n -> factorial(n) }
 * val result = memoizer(10L)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
fun <K: Any, V: Any> LettuceSuspendMap<V>.suspendMemoizer(evaluator: suspend (K) -> V): LettuceSuspendMemoizer<K, V> =
    LettuceSuspendMemoizer(this, evaluator)

/**
 * [LettuceSuspendMap]을 사용하여 함수 실행 결과를 코루틴 기반으로 메모이제이션하는 구현체입니다.
 *
 * 동일 JVM에서 동시 요청 시 in-flight 연산을 공유하여 중복 실행을 방지합니다.
 *
 * ```kotlin
 * val connection = redisClient.connect(LettuceLongCodec)
 * val map = LettuceSuspendMap<Long>(connection, "memoizer:factorial")
 * val memoizer = LettuceSuspendMemoizer(map) { n -> factorial(n) }
 * val result1 = memoizer(10L)  // 계산 후 Redis에 저장
 * val result2 = memoizer(10L)  // Redis에서 반환
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @property map LettuceSuspendMap 인스턴스
 * @property evaluator 실행할 suspend 함수
 */
class LettuceSuspendMemoizer<K: Any, V: Any>(
    val map: LettuceSuspendMap<V>,
    val evaluator: suspend (K) -> V,
): SuspendMemoizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, CompletableDeferred<V>>()

    override suspend fun invoke(key: K): V {
        inFlight[key]?.let { return it.await() }

        val deferred = CompletableDeferred<V>()
        val existing = inFlight.putIfAbsent(key, deferred)
        if (existing != null) return existing.await()

        try {
            val cached = map.get(key.toString())
            if (cached != null) {
                deferred.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val isNew = map.putIfAbsent(key.toString(), evaluated)
            val winner = if (isNew) evaluated else (map.get(key.toString()) ?: evaluated)
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
        map.clear()
    }
}
