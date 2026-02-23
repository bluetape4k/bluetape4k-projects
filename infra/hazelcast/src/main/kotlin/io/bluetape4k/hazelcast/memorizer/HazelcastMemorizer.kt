package io.bluetape4k.hazelcast.memorizer

import com.hazelcast.map.IMap
import io.bluetape4k.cache.memorizer.Memorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug

/**
 * Hazelcast [IMap]을 저장소로 사용하는 [Memorizer] 구현체입니다.
 *
 * 분산 환경에서 함수 호출 결과를 캐싱합니다. Near Cache가 활성화된 [IMap]을 사용하면
 * 로컬 메모리에서 빠르게 결과를 조회할 수 있습니다.
 *
 * ```kotlin
 * val map: IMap<Int, Int> = hazelcastClient.getMap("square-cache")
 * val memorizer = map.memorizer { key -> key * key }
 * val result1 = memorizer(4)  // 16 (새로 계산)
 * val result2 = memorizer(4)  // 16 (캐시에서 조회)
 * ```
 *
 * @param K 입력 파라미터 타입
 * @param V 결과 타입
 * @property map 결과를 저장할 Hazelcast [IMap]
 * @property evaluator 실제 계산을 수행하는 함수
 */
class HazelcastMemorizer<K: Any, V: Any>(
    private val map: IMap<K, V>,
    private val evaluator: (K) -> V,
): Memorizer<K, V> {

    companion object: KLogging()

    /**
     * 캐시에서 값을 조회하거나, 없으면 [evaluator]를 호출해 결과를 계산하고 캐시에 저장합니다.
     *
     * @param key 입력 파라미터
     * @return 캐시된 결과 또는 새로 계산된 결과
     */
    override fun invoke(key: K): V {
        return map.computeIfAbsent(key) { k ->
            log.debug { "캐시 미스 - 새로운 값을 계산합니다. key=$k" }
            evaluator(k)
        }
    }

    /**
     * 캐시를 모두 비웁니다.
     */
    override fun clear() {
        log.debug { "HazelcastMemorizer 캐시를 초기화합니다. map=${map.name}" }
        map.clear()
    }
}

/**
 * Hazelcast [IMap]에 [HazelcastMemorizer]를 생성하는 확장 함수입니다.
 *
 * ```kotlin
 * val map: IMap<String, Result> = client.getMap("results")
 * val memorizer = map.memorizer { key -> compute(key) }
 * ```
 *
 * @param K 입력 파라미터 타입
 * @param V 결과 타입
 * @param evaluator 실제 계산을 수행하는 함수
 * @return [HazelcastMemorizer] 인스턴스
 */
fun <K: Any, V: Any> IMap<K, V>.memorizer(evaluator: (K) -> V): HazelcastMemorizer<K, V> =
    HazelcastMemorizer(this, evaluator)
