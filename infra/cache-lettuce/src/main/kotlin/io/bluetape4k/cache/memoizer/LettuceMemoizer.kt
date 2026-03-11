package io.bluetape4k.cache.memoizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.LettuceMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [LettuceMap]을 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val connection = redisClient.connect(LettuceLongCodec)
 * val map = LettuceMap<Long>(connection, "memoizer:factorial")
 * val memoizer = map.memoizer { n -> factorial(n) }
 * val result = memoizer(10L)
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
fun <K: Any, V: Any> LettuceMap<V>.memoizer(evaluator: (K) -> V): LettuceMemoizer<K, V> =
    LettuceMemoizer(this, evaluator)

/**
 * [LettuceMap]을 사용하는 동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { n: Long -> factorial(n) }.memoizer(map)
 * ```
 */
fun <K: Any, V: Any> ((K) -> V).memoizer(map: LettuceMap<V>): LettuceMemoizer<K, V> =
    LettuceMemoizer(map, this)

/**
 * [LettuceMap]을 사용하여 함수 실행 결과를 메모이제이션하는 동기 구현체입니다.
 *
 * Redisson의 RedissonMemoizer와 동일한 인터페이스를 제공합니다.
 * 동일 JVM에서 동시 요청 시 in-flight 연산을 공유하여 중복 실행을 방지합니다.
 *
 * ```kotlin
 * val connection = redisClient.connect(LettuceLongCodec)
 * val map = LettuceMap<Long>(connection, "memoizer:factorial")
 * val memoizer = LettuceMemoizer(map) { n -> factorial(n) }
 * val result1 = memoizer(10L)  // 계산 후 Redis에 저장
 * val result2 = memoizer(10L)  // Redis에서 반환
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @property map LettuceMap 인스턴스
 * @property evaluator 실행할 함수
 */
class LettuceMemoizer<K: Any, V: Any>(
    val map: LettuceMap<V>,
    val evaluator: (K) -> V,
): Memoizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, CompletableFuture<V>>()

    override fun invoke(key: K): V {
        inFlight[key]?.let { return it.join() }

        val promise = CompletableFuture<V>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing.join()

        try {
            val cached = map.get(key.toString())
            if (cached != null) {
                promise.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val isNew = map.putIfAbsent(key.toString(), evaluated)
            val winner = if (isNew) evaluated else (map.get(key.toString()) ?: evaluated)
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
