package io.bluetape4k.ignite3.memorizer

import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.future.await
import org.apache.ignite.table.KeyValueView

/**
 * suspend [evaluator]의 결과를 Apache Ignite 3.x [KeyValueView]에 저장하는 [SuspendMemorizer] 생성 확장 함수입니다.
 *
 * ```kotlin
 * val view = igniteClient.tables().table("SQUARE_CACHE")!!
 *     .keyValueView(Long::class.javaObjectType, Long::class.javaObjectType)
 * val memorizer = view.suspendMemorizer { key -> key * key }
 * val result = memorizer(4L)  // 16L
 * ```
 *
 * @param K 입력 파라미터 타입
 * @param V 결과 타입
 * @param evaluator suspend 함수
 * @return [IgniteSuspendMemorizer] 인스턴스
 */
fun <K: Any, V: Any> KeyValueView<K, V>.suspendMemorizer(
    evaluator: suspend (K) -> V,
): IgniteSuspendMemorizer<K, V> = IgniteSuspendMemorizer(this, evaluator)

/**
 * suspend 함수를 [IgniteSuspendMemorizer]로 감싸는 확장 함수입니다.
 *
 * @receiver suspend 함수
 * @param view 결과를 저장할 Ignite 3.x [KeyValueView]
 * @return [IgniteSuspendMemorizer] 인스턴스
 */
fun <K: Any, V: Any> (suspend (K) -> V).suspendMemorizer(
    view: KeyValueView<K, V>,
): IgniteSuspendMemorizer<K, V> = IgniteSuspendMemorizer(view, this)

/**
 * suspend [evaluator] 결과를 Apache Ignite 3.x [KeyValueView]에 저장하는 [SuspendMemorizer] 구현체입니다.
 *
 * `KeyValueView.getAsync()` / `putIfAbsentAsync()` 네이티브 [java.util.concurrent.CompletableFuture] API를
 * 코루틴에서 non-blocking으로 활용합니다.
 *
 * ```kotlin
 * val view = igniteClient.tables().table("SQUARE_CACHE")!!
 *     .keyValueView(Long::class.javaObjectType, Long::class.javaObjectType)
 * val memorizer = IgniteSuspendMemorizer(view) { key -> key * key }
 * val result1 = memorizer(4L)  // 16L (새로 계산)
 * val result2 = memorizer(4L)  // 16L (캐시에서 조회)
 * ```
 *
 * @property view 결과를 저장할 Apache Ignite 3.x [KeyValueView]
 * @property evaluator suspend 함수
 */
class IgniteSuspendMemorizer<K: Any, V: Any>(
    val view: KeyValueView<K, V>,
    val evaluator: suspend (K) -> V,
): SuspendMemorizer<K, V> {

    companion object: KLogging()

    override suspend fun invoke(key: K): V {
        val cached = view.getAsync(null, key).await()
        if (cached != null) {
            log.debug { "캐시 히트. key=$key" }
            return cached
        }
        log.debug { "캐시 미스 - 새 값 계산. key=$key" }
        val value = evaluator(key)
        view.putIfAbsentAsync(null, key, value).await()
        return value
    }

    /**
     * 캐시 삭제는 Ignite 관리 API로 처리해야 합니다.
     * 이 구현체는 아무 동작도 수행하지 않습니다.
     */
    override suspend fun clear() {
        log.debug { "IgniteSuspendMemorizer.clear()는 지원하지 않습니다. Ignite 관리 API를 사용하세요." }
    }
}
