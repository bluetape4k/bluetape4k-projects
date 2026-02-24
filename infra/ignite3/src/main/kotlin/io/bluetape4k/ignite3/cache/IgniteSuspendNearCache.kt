package io.bluetape4k.ignite3.cache

import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.jcache.coroutines.CaffeineSuspendCache
import io.bluetape4k.cache.nearcache.coroutines.NearSuspendCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.client.IgniteClient

/**
 * Apache Ignite 3.x를 Back Cache로 사용하는 [NearSuspendCache]의 타입 별칭입니다.
 *
 * Front Cache는 [CaffeineSuspendCache], Back Cache는 [Ignite3SuspendCache]를 사용합니다.
 * [NearSuspendCache]는 cache-aside 패턴을 지원하므로 Front Cache miss 시 Back Cache에서 조회하여
 * Ignite 3.x 씬 클라이언트의 이벤트 리스너 미지원을 보완합니다.
 *
 * @see NearSuspendCache
 * @see Ignite3SuspendCache
 */
typealias IgniteSuspendNearCache<K, V> = NearSuspendCache<K, V>

private val log = KLogging().log

/**
 * Ignite 3.x 클라이언트와 [IgniteNearCacheConfig]를 사용하여 [IgniteSuspendNearCache] ([NearSuspendCache])를 생성합니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param client Ignite 3.x 씬 클라이언트
 * @param config NearCache 설정 (테이블 이름, 키/값 타입 포함)
 * @return [IgniteSuspendNearCache] ([NearSuspendCache]) 인스턴스
 */
fun <K: Any, V: Any> igniteNearSuspendCache(
    client: IgniteClient,
    config: IgniteNearCacheConfig<K, V>,
): IgniteSuspendNearCache<K, V> {
    log.debug { "Ignite3SuspendCache 기반 NearSuspendCache 생성. tableName=${config.tableName}" }
    val backCache = Ignite3SuspendCache(
        tableName = config.tableName,
        client = client,
        keyType = config.keyType,
        valueType = config.valueType,
        keyColumn = config.keyColumn,
        valueColumn = config.valueColumn,
    )
    val asyncCache = Caffeine.newBuilder().maximumSize(10_000).buildAsync<K, V>()
    val frontCache = CaffeineSuspendCache(asyncCache)
    return NearSuspendCache(frontCache, backCache, config.checkExpiryPeriod)
}
