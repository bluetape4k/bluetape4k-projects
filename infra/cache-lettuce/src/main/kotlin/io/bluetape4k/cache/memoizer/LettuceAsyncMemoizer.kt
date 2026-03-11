package io.bluetape4k.cache.memoizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.LettuceMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap

/**
 * [LettuceMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val connection = redisClient.connect(LettuceLongCodec)
 * val map = LettuceMap<Long>(connection, "memoizer:factorial")
 * val memoizer = map.asyncMemoizer { n -> CompletableFuture.supplyAsync { factorial(n) } }
 * val result = memoizer(10L).join()
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
fun <K: Any, V: Any> LettuceMap<V>.asyncMemoizer(evaluator: (K) -> CompletionStage<V>): LettuceAsyncMemoizer<K, V> =
    LettuceAsyncMemoizer(this, evaluator)

/**
 * [LettuceMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { n: Long -> CompletableFuture.supplyAsync { factorial(n) } }.asyncMemoizer(map)
 * ```
 */
fun <K: Any, V: Any> ((K) -> CompletionStage<V>).asyncMemoizer(map: LettuceMap<V>): LettuceAsyncMemoizer<K, V> =
    LettuceAsyncMemoizer(map, this)

/**
 * [LettuceMap]을 사용하여 함수 실행 결과를 [CompletableFuture] 기반으로 메모이제이션하는 구현체입니다.
 *
 * Redisson의 RedissonAsyncMemoizer와 동일한 인터페이스를 제공합니다.
 * 동일 JVM에서 동시 요청 시 in-flight 연산을 공유하여 중복 실행을 방지합니다.
 *
 * ```kotlin
 * val connection = redisClient.connect(LettuceLongCodec)
 * val map = LettuceMap<Long>(connection, "memoizer:factorial")
 * val memoizer = LettuceAsyncMemoizer(map) { n -> CompletableFuture.supplyAsync { factorial(n) } }
 * val result1 = memoizer(10L).join()  // 계산 후 Redis에 저장
 * val result2 = memoizer(10L).join()  // Redis에서 반환
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 * @property map LettuceMap 인스턴스
 * @property evaluator 실행할 비동기 함수
 */
class LettuceAsyncMemoizer<K: Any, V: Any>(
    val map: LettuceMap<V>,
    val evaluator: (K) -> CompletionStage<V>,
): AsyncMemoizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, CompletableFuture<V>>()

    override fun invoke(key: K): CompletableFuture<V> {
        return inFlight.computeIfAbsent(key) {
            val promise = map.getAsync(key.toString())
                .thenCompose { cached ->
                    if (cached != null) {
                        CompletableFuture.completedFuture(cached)
                    } else {
                        evaluator(key).toCompletableFuture()
                            .thenCompose { value ->
                                map.putIfAbsentAsync(key.toString(), value)
                                    .thenCompose { isNew ->
                                        if (isNew) CompletableFuture.completedFuture(value)
                                        else map.getAsync(key.toString()).thenApply { it ?: value }
                                    }
                            }
                    }
                }

            promise.whenComplete { _, _ -> inFlight.remove(key, promise) }
            promise
        }
    }

    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: mapKey=${map.mapKey}" }
        map.clear()
    }
}
