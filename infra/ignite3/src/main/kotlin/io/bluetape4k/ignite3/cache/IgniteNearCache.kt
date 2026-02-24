package io.bluetape4k.ignite3.cache

import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.client.IgniteClient

/**
 * Apache Ignite 3.x를 Back Cache로 사용하는 [NearCache]의 타입 별칭입니다.
 *
 * Front Cache는 Caffeine(JCache), Back Cache는 [Ignite3JCache]를 사용합니다.
 * Ignite 3.x 씬 클라이언트는 JCache 이벤트 리스너를 지원하지 않으므로,
 * 이벤트 기반 front cache 동기화 대신 폴링 기반 만료 감지로 최종 일관성을 제공합니다.
 *
 * ### Redis NearCache와의 차이
 * - Redis: backCache 변경 시 Pub/Sub 이벤트 → frontCache 즉시 갱신
 * - Ignite3: 이벤트 없음 → `checkBackCacheExpiration` 폴링으로 최종 일관성만 제공
 *
 * @see NearCache
 * @see Ignite3JCache
 */
typealias IgniteNearCache<K, V> = NearCache<K, V>

private val log = KLogging().log

/**
 * Ignite 3.x 클라이언트와 [IgniteNearCacheConfig]를 사용하여 [IgniteNearCache] ([NearCache])를 생성합니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param client Ignite 3.x 씬 클라이언트
 * @param config NearCache 설정 (테이블 이름, 키/값 타입 포함)
 * @return [IgniteNearCache] ([NearCache]) 인스턴스
 */
fun <K: Any, V: Any> igniteNearCache(
    client: IgniteClient,
    config: IgniteNearCacheConfig<K, V>,
): IgniteNearCache<K, V> {
    log.debug { "Ignite3JCache 기반 NearCache 생성. tableName=${config.tableName}" }
    val backCache = Ignite3JCache(
        tableName = config.tableName,
        client = client,
        keyType = config.keyType,
        valueType = config.valueType,
        keyColumn = config.keyColumn,
        valueColumn = config.valueColumn,
    )
    return NearCache(config, backCache)
}
