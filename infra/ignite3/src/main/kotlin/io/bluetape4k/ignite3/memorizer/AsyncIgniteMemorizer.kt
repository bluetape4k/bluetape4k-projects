package io.bluetape4k.ignite3.memorizer

import io.bluetape4k.cache.memorizer.AsyncMemorizer
import io.bluetape4k.concurrent.completableFutureOf
import io.bluetape4k.concurrent.flatMap
import io.bluetape4k.concurrent.map
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.table.KeyValueView
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap

/**
 * 비동기 [evaluator]의 결과를 Apache Ignite 3.x [KeyValueView]에 저장하는 [AsyncMemorizer] 생성 확장 함수입니다.
 *
 * ```kotlin
 * val view = igniteClient.tables().table("SQUARE_CACHE")!!
 *     .keyValueView(Long::class.javaObjectType, Long::class.javaObjectType)
 * val memorizer = view.asyncMemorizer { key ->
 *     CompletableFuture.supplyAsync { key * key }
 * }
 * val result = memorizer(4L).get()  // 16L
 * ```
 *
 * @param K 입력 파라미터 타입
 * @param V 결과 타입
 * @param evaluator 비동기 결과를 반환하는 함수
 * @return [AsyncIgniteMemorizer] 인스턴스
 */
fun <K: Any, V: Any> KeyValueView<K, V>.asyncMemorizer(
    evaluator: (K) -> CompletionStage<V>,
): AsyncIgniteMemorizer<K, V> = AsyncIgniteMemorizer(this, evaluator)

/**
 * 비동기 함수를 [AsyncIgniteMemorizer]로 감싸는 확장 함수입니다.
 *
 * @receiver 비동기 결과를 반환하는 함수
 * @param view 결과를 저장할 Ignite 3.x [KeyValueView]
 * @return [AsyncIgniteMemorizer] 인스턴스
 */
fun <K: Any, V: Any> ((K) -> CompletionStage<V>).asyncMemorizer(
    view: KeyValueView<K, V>,
): AsyncIgniteMemorizer<K, V> = AsyncIgniteMemorizer(view, this)

/**
 * 비동기 [evaluator] 결과를 Apache Ignite 3.x [KeyValueView]에 저장하는 [AsyncMemorizer] 구현체입니다.
 *
 * `inFlight` 맵으로 동일 키에 대한 중복 평가를 방지합니다.
 * `KeyValueView.getAsync()` / `putIfAbsentAsync()` 네이티브 [CompletableFuture] API를 활용합니다.
 *
 * ```kotlin
 * val view = igniteClient.tables().table("SQUARE_CACHE")!!
 *     .keyValueView(Long::class.javaObjectType, Long::class.javaObjectType)
 * val memorizer = AsyncIgniteMemorizer(view) { key ->
 *     CompletableFuture.supplyAsync { key * key }
 * }
 * val result1 = memorizer(4L).get()  // 16L (새로 계산)
 * val result2 = memorizer(4L).get()  // 16L (캐시에서 조회)
 * ```
 *
 * @property view 결과를 저장할 Apache Ignite 3.x [KeyValueView]
 * @property evaluator 비동기 결과를 반환하는 함수
 */
class AsyncIgniteMemorizer<K: Any, V: Any>(
    val view: KeyValueView<K, V>,
    val evaluator: (K) -> CompletionStage<V>,
): AsyncMemorizer<K, V> {

    companion object: KLogging()

    private val inFlight = ConcurrentHashMap<K, CompletableFuture<V>>()

    override fun invoke(key: K): CompletableFuture<V> {
        return inFlight.computeIfAbsent(key) {
            val promise = view.getAsync(null, key)
                .flatMap { cached ->
                    if (cached != null) {
                        log.debug { "캐시 히트. key=$key" }
                        completableFutureOf(cached)
                    } else {
                        log.debug { "캐시 미스 - 새 값 계산. key=$key" }
                        evaluator(key)
                            .toCompletableFuture()
                            .flatMap { value ->
                                view.putIfAbsentAsync(null, key, value)
                                    .map { inserted -> value }
                            }
                    }
                }
                .toCompletableFuture()

            promise.whenComplete { _, _ -> inFlight.remove(key, promise) }
            promise
        }
    }

    /**
     * 캐시 삭제는 Ignite 관리 API로 처리해야 합니다.
     * 이 구현체는 아무 동작도 수행하지 않습니다.
     */
    override fun clear() {
        log.debug { "AsyncIgniteMemorizer.clear()는 지원하지 않습니다. Ignite 관리 API를 사용하세요." }
    }
}
