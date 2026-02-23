package io.bluetape4k.ignite3.memorizer

import io.bluetape4k.cache.memorizer.Memorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.table.KeyValueView

/**
 * Apache Ignite 3.x [KeyValueView]를 저장소로 사용하는 [Memorizer] 구현체입니다.
 *
 * ```kotlin
 * val view = client.keyValueView<Int, Int>("SQUARE_CACHE")
 * val memorizer = view.memorizer { key -> key * key }
 * val result = memorizer(4)  // 16
 * ```
 */
class IgniteMemorizer<K: Any, V: Any>(
    private val view: KeyValueView<K, V>,
    private val evaluator: (K) -> V,
): Memorizer<K, V> {

    companion object: KLogging()

    override fun invoke(key: K): V {
        val cached = view.get(null, key)
        if (cached != null) {
            log.debug { "캐시 히트. key=$key" }
            return cached
        }
        log.debug { "캐시 미스 - 새 값 계산. key=$key" }
        return evaluator(key).also { view.put(null, key, it) }
    }

    override fun clear() {
        log.debug { "IgniteMemorizer.clear()는 지원하지 않습니다. Ignite 관리 API를 사용하세요." }
    }
}

/**
 * Ignite 3.x [KeyValueView]에 [IgniteMemorizer]를 생성하는 확장 함수입니다.
 */
fun <K: Any, V: Any> KeyValueView<K, V>.memorizer(evaluator: (K) -> V): IgniteMemorizer<K, V> =
    IgniteMemorizer(this, evaluator)
