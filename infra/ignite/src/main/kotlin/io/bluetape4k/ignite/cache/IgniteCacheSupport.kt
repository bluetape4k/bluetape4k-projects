package io.bluetape4k.ignite.cache

import org.apache.ignite.Ignite
import org.apache.ignite.IgniteCache
import org.apache.ignite.client.IgniteClient

/**
 * Ignite 2.x 임베디드 노드에서 Near Cache가 활성화된 [IgniteCache]를 가져오는 확장 함수입니다.
 *
 * ```kotlin
 * val ignite = igniteEmbedded { igniteInstanceName = "my-node" }
 * val nearCache = ignite.getOrCreateEmbeddedNearCache<String, Order>(
 *     config = IgniteNearCacheConfig(cacheName = "ORDERS")
 * )
 * ```
 */
fun <K: Any, V: Any> Ignite.getOrCreateEmbeddedNearCache(
    config: IgniteNearCacheConfig,
): IgniteEmbeddedNearCache<K, V> = IgniteEmbeddedNearCache(this, config)

/**
 * Ignite 2.x 씬 클라이언트에서 Caffeine + [org.apache.ignite.client.ClientCache] 2-Tier NearCache를 생성합니다.
 *
 * ```kotlin
 * val client = igniteClient("localhost:10800")
 * val nearCache = client.getOrCreateClientNearCache<String, Order>(
 *     config = IgniteNearCacheConfig(cacheName = "ORDERS")
 * )
 * ```
 */
fun <K: Any, V: Any> IgniteClient.getOrCreateClientNearCache(
    config: IgniteNearCacheConfig,
): IgniteClientNearCache<K, V> = IgniteClientNearCache(this, config)
