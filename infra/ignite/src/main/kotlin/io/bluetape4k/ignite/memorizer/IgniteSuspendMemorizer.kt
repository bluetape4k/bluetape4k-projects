package io.bluetape4k.ignite.memorizer

import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.apache.ignite.IgniteCache
import org.apache.ignite.lang.IgniteFuture
import java.util.concurrent.CompletableFuture

/**
 * [IgniteFuture]를 [CompletableFuture]로 변환합니다.
 *
 * [IgniteFuture]는 [java.util.concurrent.Future]를 구현하지 않으므로
 * `listen()` 콜백을 사용하여 변환합니다.
 */
private fun <T> IgniteFuture<T>.toCompletableFuture(): CompletableFuture<T> {
    val cf = CompletableFuture<T>()
    listen { f ->
        try {
            cf.complete(f.get())
        } catch (e: Throwable) {
            cf.completeExceptionally(e.cause ?: e)
        }
    }
    return cf
}

/**
 * suspend [evaluator]의 결과를 Apache Ignite 2.x [IgniteCache]에 저장하는 [SuspendMemorizer] 생성 확장 함수입니다.
 *
 * ```kotlin
 * val cache: IgniteCache<Int, Int> = ignite.getOrCreateCache("suspend-square")
 * val memorizer = cache.suspendMemorizer { key -> key * key }
 * val result = memorizer(4)  // 16
 * ```
 *
 * @param K 입력 파라미터 타입
 * @param V 결과 타입
 * @param evaluator suspend 함수
 * @return [IgniteSuspendMemorizer] 인스턴스
 */
fun <K: Any, V: Any> IgniteCache<K, V>.suspendMemorizer(
    evaluator: suspend (K) -> V,
): IgniteSuspendMemorizer<K, V> = IgniteSuspendMemorizer(this, evaluator)

/**
 * suspend 함수를 [IgniteSuspendMemorizer]로 감싸는 확장 함수입니다.
 *
 * @receiver suspend 함수
 * @param cache 결과를 저장할 Ignite [IgniteCache]
 * @return [IgniteSuspendMemorizer] 인스턴스
 */
fun <K: Any, V: Any> (suspend (K) -> V).suspendMemorizer(
    cache: IgniteCache<K, V>,
): IgniteSuspendMemorizer<K, V> = IgniteSuspendMemorizer(cache, this)

/**
 * suspend [evaluator] 결과를 Apache Ignite 2.x [IgniteCache]에 저장하는 [SuspendMemorizer] 구현체입니다.
 *
 * `IgniteCache.getAsync()` / `putAsync()` 비동기 API를 코루틴에서 non-blocking으로 활용합니다.
 * [IgniteFuture]를 [CompletableFuture]로 변환하여 코루틴에서 `await()`합니다.
 *
 * ```kotlin
 * val cache: IgniteCache<Int, Int> = ignite.getOrCreateCache("suspend-square")
 * val memorizer = IgniteSuspendMemorizer(cache) { key -> key * key }
 * val result1 = memorizer(4)  // 16 (새로 계산)
 * val result2 = memorizer(4)  // 16 (캐시에서 조회)
 * ```
 *
 * @property cache 결과를 저장할 Apache Ignite 2.x [IgniteCache]
 * @property evaluator suspend 함수
 */
class IgniteSuspendMemorizer<K: Any, V: Any>(
    val cache: IgniteCache<K, V>,
    val evaluator: suspend (K) -> V,
): SuspendMemorizer<K, V> {

    companion object: KLogging()

    @Suppress("UNCHECKED_CAST")
    override suspend fun invoke(key: K): V {
        val cached: V? = cache.getAsync(key).toCompletableFuture().await() as V?
        if (cached != null) {
            log.debug { "캐시 히트. key=$key" }
            return cached
        }
        log.debug { "캐시 미스 - 새 값 계산. key=$key" }
        val value = evaluator(key)
        (cache.putAsync(key, value) as IgniteFuture<Any?>).toCompletableFuture().await()
        return value
    }

    /**
     * 캐시를 모두 비웁니다.
     */
    override suspend fun clear() {
        log.debug { "IgniteSuspendMemorizer 캐시를 초기화합니다. cache=${cache.name}" }
        withContext(Dispatchers.IO) { cache.clear() }
    }
}
