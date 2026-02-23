package io.bluetape4k.ignite.cache

/**
 * Apache Ignite 2.x 씬 클라이언트 기반 2-Tier NearCache의 별칭입니다.
 *
 * Caffeine(Front Cache) + [org.apache.ignite.client.ClientCache](Back Cache) 구조입니다.
 * 임베디드 모드에서 진정한 Near Cache를 원한다면 [IgniteEmbeddedNearCache]를 사용하세요.
 *
 * @see IgniteClientNearCache
 * @see IgniteEmbeddedNearCache
 */
typealias IgniteNearCache<K, V> = IgniteClientNearCache<K, V>
