package io.bluetape4k.hazelcast.memorizer

import com.hazelcast.map.IMap
import io.bluetape4k.cache.memorizer.SuspendMemorizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

/**
 * suspend [evaluator]의 결과를 Hazelcast [IMap]에 저장하는 [SuspendMemorizer] 생성 확장 함수입니다.
 *
 * ```kotlin
 * val map: IMap<Int, Int> = hazelcastClient.getMap("suspend-square")
 * val memorizer = map.suspendMemorizer { key -> key * key }
 * val result = memorizer(4)  // 16
 * ```
 *
 * @param K 입력 파라미터 타입
 * @param V 결과 타입
 * @param evaluator suspend 함수
 * @return [HazelcastSuspendMemorizer] 인스턴스
 */
fun <K: Any, V: Any> IMap<K, V>.suspendMemorizer(
    evaluator: suspend (K) -> V,
): HazelcastSuspendMemorizer<K, V> = HazelcastSuspendMemorizer(this, evaluator)

/**
 * suspend 함수를 [HazelcastSuspendMemorizer]로 감싸는 확장 함수입니다.
 *
 * @receiver suspend 함수
 * @param map 결과를 저장할 Hazelcast [IMap]
 * @return [HazelcastSuspendMemorizer] 인스턴스
 */
fun <K: Any, V: Any> (suspend (K) -> V).suspendMemorizer(
    map: IMap<K, V>,
): HazelcastSuspendMemorizer<K, V> = HazelcastSuspendMemorizer(map, this)

/**
 * suspend [evaluator] 결과를 Hazelcast [IMap]에 저장하는 [SuspendMemorizer] 구현체입니다.
 *
 * Near Cache가 활성화된 [IMap]을 사용하면 로컬 메모리에서 빠르게 결과를 조회할 수 있습니다.
 * Hazelcast 5.x `IMap.getAsync()` / `putAsync()` API를 코루틴에서 non-blocking으로 활용합니다.
 *
 * ```kotlin
 * val map: IMap<Int, Int> = hazelcastClient.getMap("suspend-square")
 * val memorizer = HazelcastSuspendMemorizer(map) { key -> key * key }
 * val result1 = memorizer(4)  // 16 (새로 계산)
 * val result2 = memorizer(4)  // 16 (캐시에서 조회)
 * ```
 *
 * @property map 결과를 저장할 Hazelcast [IMap]
 * @property evaluator suspend 함수
 */
class HazelcastSuspendMemorizer<K: Any, V: Any>(
    val map: IMap<K, V>,
    val evaluator: suspend (K) -> V,
): SuspendMemorizer<K, V> {

    companion object: KLogging()

    @Suppress("UNCHECKED_CAST")
    override suspend fun invoke(key: K): V {
        val cached: V? = map.getAsync(key).await()
        if (cached != null) {
            log.debug { "캐시 히트. key=$key" }
            return cached
        }
        log.debug { "캐시 미스 - 새 값 계산. key=$key" }
        val value = evaluator(key)
        map.putAsync(key, value).await()
        return value
    }

    /**
     * 캐시를 모두 비웁니다.
     */
    override suspend fun clear() {
        log.debug { "HazelcastSuspendMemorizer 캐시를 초기화합니다. map=${map.name}" }
        withContext(Dispatchers.IO) { map.clear() }
    }
}
