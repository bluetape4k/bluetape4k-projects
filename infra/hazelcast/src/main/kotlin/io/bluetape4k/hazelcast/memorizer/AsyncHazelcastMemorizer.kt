package io.bluetape4k.hazelcast.memorizer

import com.hazelcast.map.IMap
import io.bluetape4k.cache.memorizer.AsyncMemorizer
import io.bluetape4k.concurrent.completableFutureOf
import io.bluetape4k.concurrent.flatMap
import io.bluetape4k.concurrent.map
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap

/**
 * 비동기 [evaluator]의 결과를 Hazelcast [IMap]에 저장하는 [AsyncMemorizer] 생성 확장 함수입니다.
 *
 * ```kotlin
 * val map: IMap<Int, Int> = hazelcastClient.getMap("async-square")
 * val memorizer = map.asyncMemorizer { key ->
 *     CompletableFuture.supplyAsync { key * key }
 * }
 * val result = memorizer(4).get()  // 16
 * ```
 *
 * @param K 입력 파라미터 타입
 * @param V 결과 타입
 * @param evaluator 비동기 결과를 반환하는 함수
 * @return [AsyncHazelcastMemorizer] 인스턴스
 */
fun <K: Any, V: Any> IMap<K, V>.asyncMemorizer(
    evaluator: (K) -> CompletionStage<V>,
): AsyncHazelcastMemorizer<K, V> = AsyncHazelcastMemorizer(this, evaluator)

/**
 * 비동기 함수를 [AsyncHazelcastMemorizer]로 감싸는 확장 함수입니다.
 *
 * @receiver 비동기 결과를 반환하는 함수
 * @param map 결과를 저장할 Hazelcast [IMap]
 * @return [AsyncHazelcastMemorizer] 인스턴스
 */
fun <K: Any, V: Any> ((K) -> CompletionStage<V>).asyncMemorizer(
    map: IMap<K, V>,
): AsyncHazelcastMemorizer<K, V> = AsyncHazelcastMemorizer(map, this)

/**
 * 비동기 [evaluator] 결과를 Hazelcast [IMap]에 저장하는 [AsyncMemorizer] 구현체입니다.
 *
 * Near Cache가 활성화된 [IMap]을 사용하면 로컬 메모리에서 빠르게 결과를 조회할 수 있습니다.
 * `inFlight` 맵으로 동일 키에 대한 중복 평가를 방지합니다.
 * Hazelcast 5.x `IMap.getAsync()` / `putAsync()` API를 활용합니다.
 *
 * ```kotlin
 * val map: IMap<Int, Int> = hazelcastClient.getMap("async-square")
 * val memorizer = AsyncHazelcastMemorizer(map) { key ->
 *     CompletableFuture.supplyAsync { key * key }
 * }
 * val result1 = memorizer(4).get()  // 16 (새로 계산)
 * val result2 = memorizer(4).get()  // 16 (캐시에서 조회)
 * ```
 *
 * @property map 결과를 저장할 Hazelcast [IMap]
 * @property evaluator 비동기 결과를 반환하는 함수
 */
class AsyncHazelcastMemorizer<K: Any, V: Any>(
    val map: IMap<K, V>,
    val evaluator: (K) -> CompletionStage<V>,
): AsyncMemorizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, CompletableFuture<V>>()

    @Suppress("UNCHECKED_CAST")
    override fun invoke(key: K): CompletableFuture<V> {
        return inFlight.computeIfAbsent(key) {
            val promise = map.getAsync(key).toCompletableFuture()
                .flatMap { cached ->
                    if (cached != null) {
                        log.debug { "캐시 히트. key=$key" }
                        completableFutureOf(cached)
                    } else {
                        log.debug { "캐시 미스 - 새 값 계산. key=$key" }
                        evaluator(key)
                            .toCompletableFuture()
                            .flatMap { value ->
                                // putAsync는 이전 값을 반환하므로, 계산된 value를 그대로 반환
                                (map.putAsync(key, value) as CompletableFuture<V?>)
                                    .map { value }
                            }
                    }
                }

            promise.whenComplete { _, _ -> inFlight.remove(key, promise) }
            promise
        }
    }

    /**
     * 캐시를 모두 비웁니다.
     */
    override fun clear() {
        log.debug { "AsyncHazelcastMemorizer 캐시를 초기화합니다. map=${map.name}" }
        map.clear()
    }
}
