package io.bluetape4k.cache.memoizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.apache.ignite.client.ClientCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * [ClientCache]를 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val cache: ClientCache<Int, Int> = igniteClient.getOrCreateCache("squares")
 * val memoizer = cache.asyncMemoizer { key -> key * key }
 * val result = memoizer(5).get()  // 25
 * ```
 *
 * @param evaluator 수행할 함수
 * @return [IgniteAsyncMemoizer] 인스턴스
 */
fun <K: Any, V: Any> ClientCache<K, V>.asyncMemoizer(evaluator: (K) -> V): IgniteAsyncMemoizer<K, V> =
    IgniteAsyncMemoizer(this, evaluator)

/**
 * [ClientCache]를 사용하는 비동기 메모이저 확장 함수입니다.
 *
 * ```kotlin
 * val memoizer = { key: Int -> key * key }.asyncMemoizer(cache)
 * val result = memoizer(5).get()
 * ```
 *
 * @receiver 실행할 함수
 * @param cache Ignite [ClientCache] 인스턴스
 * @return [IgniteAsyncMemoizer] 인스턴스
 */
fun <K: Any, V: Any> ((K) -> V).asyncMemoizer(cache: ClientCache<K, V>): IgniteAsyncMemoizer<K, V> =
    IgniteAsyncMemoizer(cache, this)

/**
 * [evaluator] 결과를 Ignite [ClientCache]에 저장하는 비동기 메모이저입니다.
 * `ClientCache`는 네이티브 비동기 API를 제공하지 않으므로 [CompletableFuture.supplyAsync]로 래핑합니다.
 * 같은 JVM에서 동일 키가 동시에 요청되면 in-flight 연산을 공유하여 중복 실행을 줄입니다.
 *
 * ARM64 환경에서 Ignite thin client의 `cache.get()` 호출이 무한 대기할 수 있으므로
 * 모든 캐시 연산에 타임아웃 보호를 적용합니다.
 *
 * ```kotlin
 * val cache: ClientCache<Int, Int> = igniteClient.getOrCreateCache("squares")
 * val memoizer = IgniteAsyncMemoizer(cache) { key -> key * key }
 * val result = memoizer(5).get()  // 25
 * ```
 *
 * @property cache     Ignite [ClientCache] 인스턴스
 * @property evaluator 실행할 함수
 */
class IgniteAsyncMemoizer<K: Any, V: Any>(
    val cache: ClientCache<K, V>,
    val evaluator: (K) -> V,
): AsyncMemoizer<K, V> {

    companion object: KLogging() {
        private const val CACHE_TIMEOUT_MS = 10_000L

        /**
         * Ignite thin client의 블로킹 I/O를 위한 전용 executor.
         * ForkJoinPool.commonPool()은 스레드 수가 제한적이어서
         * cache.get() 등이 오래 걸리면 스레드 고갈이 발생할 수 있습니다.
         */
        private val executor = Executors.newVirtualThreadPerTaskExecutor()
    }

    private val inFlight = ConcurrentHashMap<K, CompletableFuture<V>>()

    override fun invoke(key: K): CompletableFuture<V> {
        // 1. in-flight 확인 또는 신규 등록
        val promise = CompletableFuture<V>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing

        // 2. Ignite에서 캐시 조회 후, miss 시 evaluator 실행 (VirtualThread에서 블로킹 I/O 실행)
        CompletableFuture.supplyAsync({
            val cached = cacheGetSafe(key)
            if (cached != null) {
                cached
            } else {
                val value: V = evaluator(key)
                cachePutSafe(key, value)
                value
            }
        }, executor).whenComplete { result, error ->
            inFlight.remove(key)
            if (error != null) promise.completeExceptionally(error)
            else promise.complete(result)
        }

        return promise
    }

    /**
     * 타임아웃이 적용된 캐시 조회.
     * ARM64 환경에서 `cache.get()`이 무한 대기할 수 있으므로 별도 스레드에서 타임아웃을 적용합니다.
     * 타임아웃 또는 예외 발생 시 null(cache miss)로 처리합니다.
     */
    private fun cacheGetSafe(key: K): V? {
        return try {
            CompletableFuture.supplyAsync({ cache.get(key) }, executor)
                .get(CACHE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            log.warn { "캐시 조회 타임아웃 (key=$key, cache=${cache.name}), cache miss 처리" }
            null
        } catch (e: Exception) {
            log.warn(e) { "캐시 조회 실패 (key=$key, cache=${cache.name}), cache miss 처리" }
            null
        }
    }

    /**
     * 타임아웃이 적용된 캐시 저장.
     * 저장 실패 또는 타임아웃 시 무시합니다.
     */
    private fun cachePutSafe(key: K, value: V) {
        try {
            CompletableFuture.supplyAsync({ cache.putIfAbsent(key, value) }, executor)
                .get(CACHE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            log.warn(e) { "캐시 저장 실패 (key=$key, cache=${cache.name}), 무시" }
        }
    }

    override fun clear() {
        log.debug { "모든 메모이제이션 값 삭제: cache=${cache.name}" }
        inFlight.clear()
        cache.clear()
    }
}
