package io.bluetape4k.cache.memoizer.hazelcast

import com.hazelcast.map.IMap
import io.bluetape4k.cache.memoizer.SuspendMemoizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * [IMap]을 사용하는 코루틴 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val imap: IMap<Int, Int> = hazelcastClient.getMap("squares")
 * val memoizer = imap.suspendMemoizer { key -> computeExpensive(key) }
 * val result = memoizer(5)
 * ```
 *
 * @param K  key type
 * @param V  value type
 * @receiver Hazelcast [IMap] 인스턴스
 * @param evaluator 실행할 suspend 함수
 * @return [SuspendHazelcastMemoizer] instance
 */
fun <K: Any, V: Any> IMap<K, V>.suspendMemoizer(evaluator: suspend (K) -> V): SuspendHazelcastMemoizer<K, V> =
    SuspendHazelcastMemoizer(this, evaluator)

/**
 * [IMap]을 사용하는 코루틴 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val fn: suspend (Int) -> Int = { key -> computeExpensive(key) }
 * val memoizer = fn.memoizer(imap)
 * val result = memoizer(5)
 * ```
 *
 * @param K key type
 * @param V value type
 * @receiver 실행할 suspend 함수
 * @param imap Hazelcast [IMap] 인스턴스
 * @return [SuspendHazelcastMemoizer] instance
 */
fun <K: Any, V: Any> (suspend (K) -> V).memoizer(imap: IMap<K, V>): SuspendHazelcastMemoizer<K, V> =
    SuspendHazelcastMemoizer(imap, this)

/**
 * [evaluator]가 실행한 결과를 [imap]에 저장하고, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 * `IMap.getAsync()`로 캐시를 조회하고, 없으면 [evaluator]를 실행 후 저장합니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 중복 실행을 줄입니다.
 *
 * ```kotlin
 * val imap: IMap<Int, Int> = hazelcastClient.getMap("squares")
 * val memoizer = SuspendHazelcastMemoizer(imap) { key -> computeExpensive(key) }
 * val result1 = memoizer(5)  // 계산 후 저장
 * val result2 = memoizer(5)  // Hazelcast에서 반환
 * ```
 *
 * @property imap      Hazelcast [IMap] 인스턴스
 * @property evaluator 실행할 suspend 함수
 */
class SuspendHazelcastMemoizer<K: Any, V: Any>(
    val imap: IMap<K, V>,
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
            val cached = imap.getAsync(key).await()
            if (cached != null) {
                deferred.complete(cached)
                return cached
            }

            val evaluated = evaluator(key)
            val winner = withContext(Dispatchers.IO) { imap.putIfAbsent(key, evaluated) } ?: evaluated
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
        log.debug { "모든 메모이제이션 값 삭제: map=${imap.name}" }
        withContext(Dispatchers.IO) { imap.clear() }
    }
}
