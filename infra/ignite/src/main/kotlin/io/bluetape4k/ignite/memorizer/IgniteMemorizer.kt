package io.bluetape4k.ignite.memorizer

import io.bluetape4k.cache.memorizer.Memorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.IgniteCache

/**
 * Apache Ignite 2.x [IgniteCache]를 저장소로 사용하는 [Memorizer] 구현체입니다.
 *
 * 분산 환경에서 함수 호출 결과를 Ignite 캐시에 저장합니다.
 * [IgniteEmbeddedNearCache]와 함께 사용하면 로컬 Near Cache 레이어도 활용할 수 있습니다.
 *
 * ```kotlin
 * val ignite = igniteEmbedded { igniteInstanceName = "my-node" }
 * val cache: IgniteCache<Int, Int> = ignite.getOrCreateCache("square-cache")
 * val memorizer = cache.memorizer { key -> key * key }
 * val result = memorizer(4)  // 16
 * ```
 *
 * @param K 입력 파라미터 타입
 * @param V 결과 타입
 */
class IgniteMemorizer<K: Any, V: Any>(
    private val cache: IgniteCache<K, V>,
    private val evaluator: (K) -> V,
): Memorizer<K, V> {

    companion object: KLogging()

    override fun invoke(key: K): V {
        val cached = cache.get(key)
        if (cached != null) {
            log.debug { "캐시 히트. cache=${cache.name}, key=$key" }
            return cached
        }
        log.debug { "캐시 미스 - 새 값 계산. cache=${cache.name}, key=$key" }
        return evaluator(key).also { cache.put(key, it) }
    }

    override fun clear() {
        log.debug { "IgniteMemorizer 캐시 초기화. cache=${cache.name}" }
        cache.clear()
    }
}

/**
 * Ignite 2.x [IgniteCache]에 [IgniteMemorizer]를 생성하는 확장 함수입니다.
 */
fun <K: Any, V: Any> IgniteCache<K, V>.memorizer(evaluator: (K) -> V): IgniteMemorizer<K, V> =
    IgniteMemorizer(this, evaluator)
