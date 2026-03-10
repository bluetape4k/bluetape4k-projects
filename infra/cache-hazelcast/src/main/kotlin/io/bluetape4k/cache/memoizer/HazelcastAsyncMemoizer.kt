package io.bluetape4k.cache.memoizer

import com.hazelcast.map.IMap
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * [IMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val imap: IMap<Int, Int> = hazelcastClient.getMap("squares")
 * val memoizer = imap.asyncMemoizer { key -> key * key }
 * val result = memoizer(5).get()  // 25
 * ```
 *
 * @param evaluator 수행할 함수
 * @return [AsyncHazelcastMemoizer] 인스턴스
 */
fun <K: Any, V: Any> IMap<K, V>.asyncMemoizer(evaluator: (K) -> V): AsyncHazelcastMemoizer<K, V> =
    AsyncHazelcastMemoizer(this, evaluator)

/**
 * [IMap]을 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { key: Int -> key * key }.asyncMemoizer(imap)
 * val result = memoizer(5).get()
 * ```
 *
 * @receiver 실행할 함수
 * @param imap Hazelcast [IMap] 인스턴스
 * @return [AsyncHazelcastMemoizer] 인스턴스
 */
fun <K: Any, V: Any> ((K) -> V).asyncMemoizer(imap: IMap<K, V>): AsyncHazelcastMemoizer<K, V> =
    AsyncHazelcastMemoizer(imap, this)

/**
 * [evaluator] 결과를 Hazelcast [IMap]에 저장하는 비동기 메모이저입니다.
 * `IMap.getAsync()`로 캐시를 조회하고, 없으면 [evaluator]를 실행 후 저장합니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 중복 실행을 줄입니다.
 *
 * ```kotlin
 * val imap: IMap<Int, Int> = hazelcastClient.getMap("squares")
 * val memoizer = AsyncHazelcastMemoizer(imap) { key -> key * key }
 * val result = memoizer(5).get()  // 25
 * ```
 *
 * @property imap      Hazelcast [IMap] 인스턴스
 * @property evaluator 실행할 함수
 */
class AsyncHazelcastMemoizer<K: Any, V: Any>(
    val imap: IMap<K, V>,
    val evaluator: (K) -> V,
): AsyncMemoizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, CompletableFuture<V>>()

    override fun invoke(key: K): CompletableFuture<V> {
        return inFlight.computeIfAbsent(key) {
            val promise = imap.getAsync(key).thenCompose { cached ->
                if (cached != null) {
                    CompletableFuture.completedFuture(cached)
                } else {
                    CompletableFuture.supplyAsync {
                        val value = evaluator(key)
                        imap.putIfAbsent(key, value) ?: value
                    }
                }
            }.toCompletableFuture()

            promise.whenComplete { _, _ -> inFlight.remove(key, promise) }
            promise
        }
    }

    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: map=${imap.name}" }
        inFlight.clear()
        imap.clear()
    }
}
