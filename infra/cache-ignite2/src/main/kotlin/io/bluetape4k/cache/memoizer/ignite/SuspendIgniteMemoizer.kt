package io.bluetape4k.cache.memoizer.ignite

import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.ignite.client.ClientCache
import java.util.concurrent.ConcurrentHashMap

/**
 * [ClientCache]를 사용하는 코루틴 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val cache: ClientCache<Int, Int> = igniteClient.getOrCreateCache("squares")
 * val memoizer = cache.suspendMemoizer { key -> computeExpensive(key) }
 * val result = memoizer(5)
 * ```
 *
 * @param K  key type
 * @param V  value type
 * @receiver Ignite [ClientCache] 인스턴스
 * @param evaluator 실행할 suspend 함수
 * @return [SuspendIgniteMemoizer] instance
 */
fun <K: Any, V: Any> ClientCache<K, V>.suspendMemoizer(evaluator: suspend (K) -> V): SuspendIgniteMemoizer<K, V> =
    SuspendIgniteMemoizer(this, evaluator)

/**
 * [ClientCache]를 사용하는 코루틴 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val fn: suspend (Int) -> Int = { key -> computeExpensive(key) }
 * val memoizer = fn.memoizer(cache)
 * val result = memoizer(5)
 * ```
 *
 * @param K key type
 * @param V value type
 * @receiver 실행할 suspend 함수
 * @param cache Ignite [ClientCache] 인스턴스
 * @return [SuspendIgniteMemoizer] instance
 */
fun <K: Any, V: Any> (suspend (K) -> V).memoizer(cache: ClientCache<K, V>): SuspendIgniteMemoizer<K, V> =
    SuspendIgniteMemoizer(cache, this)

/**
 * [evaluator]가 실행한 결과를 [cache]에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 * `ClientCache`는 네이티브 비동기 API를 제공하지 않으므로 [Dispatchers.IO]에서 블로킹 호출을 래핑합니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 중복 실행을 줄입니다.
 *
 * ```kotlin
 * val cache: ClientCache<Int, Int> = igniteClient.getOrCreateCache("squares")
 * val memoizer = SuspendIgniteMemoizer(cache) { key -> computeExpensive(key) }
 * val result1 = memoizer(5)  // 계산 후 저장
 * val result2 = memoizer(5)  // Ignite에서 반환
 * ```
 *
 * @property cache     Ignite [ClientCache] 인스턴스
 * @property evaluator 실행할 suspend 함수
 */
class SuspendIgniteMemoizer<K: Any, V: Any>(
    val cache: ClientCache<K, V>,
    val evaluator: suspend (K) -> V,
): SuspendMemoizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, Deferred<V>>()

    override suspend fun invoke(key: K): V {
        inFlight[key]?.let { return it.await() }

        val deferred = CompletableDeferred<V>()
        val existing = inFlight.putIfAbsent(key, deferred)
        if (existing != null) return existing.await()

        try {
            val cached = withContext(Dispatchers.IO) { cache.get(key) }
            if (cached != null) {
                deferred.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val winner = withContext(Dispatchers.IO) {
                val isNew = cache.putIfAbsent(key, evaluated)
                if (isNew) evaluated else (cache.get(key) ?: evaluated)
            }
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
        log.debug { "모든 메모이제이션 값 삭제: cache=${cache.name}" }
        withContext(Dispatchers.IO) { cache.clear() }
    }
}
