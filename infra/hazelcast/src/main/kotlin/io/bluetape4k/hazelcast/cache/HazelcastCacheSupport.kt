package io.bluetape4k.hazelcast.cache

import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.map.IMap

/**
 * Near Cache가 활성화된 Hazelcast [IMap]을 감싸는 [HazelcastNearCache]를 생성합니다.
 *
 * ```kotlin
 * val nearCache = hazelcastNearCache<String, MyEntity>(
 *     mapName = "my-map",
 *     addresses = arrayOf("localhost:5701"),
 *     nearCacheConfig = HazelcastNearCacheConfig("my-map"),
 * )
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param mapName IMap 이름
 * @param addresses Hazelcast 서버 주소 목록
 * @param nearCacheConfig Near Cache 설정
 * @return [HazelcastNearCache] 인스턴스
 */
fun <K: Any, V: Any> hazelcastNearCache(
    mapName: String,
    vararg addresses: String = arrayOf("localhost:5701"),
    nearCacheConfig: HazelcastNearCacheConfig = HazelcastNearCacheConfig(mapName),
): HazelcastNearCache<K, V> {
    val clientConfig = ClientConfig().apply {
        networkConfig.addAddress(*addresses)
        addNearCacheConfig(nearCacheConfig.toNearCacheConfig())
    }
    val client = HazelcastClient.newHazelcastClient(clientConfig)
    return HazelcastNearCache(client.getMap(mapName))
}

/**
 * 기존 [ClientConfig]로 Near Cache가 활성화된 [HazelcastNearCache]를 생성합니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @param mapName IMap 이름
 * @param clientConfig Hazelcast 클라이언트 설정 (Near Cache 설정이 포함되어야 함)
 * @return [HazelcastNearCache] 인스턴스
 */
fun <K: Any, V: Any> hazelcastNearCache(
    mapName: String,
    clientConfig: ClientConfig,
): HazelcastNearCache<K, V> {
    val client = HazelcastClient.newHazelcastClient(clientConfig)
    return HazelcastNearCache(client.getMap(mapName))
}

/**
 * Near Cache 설정이 포함된 [ClientConfig]를 생성합니다.
 *
 * ```kotlin
 * val config = hazelcastClientConfig("localhost:5701") {
 *     addNearCacheConfig(HazelcastNearCacheConfig("orders").toNearCacheConfig())
 *     addNearCacheConfig(HazelcastNearCacheConfig("products").toNearCacheConfig())
 * }
 * ```
 *
 * @param addresses Hazelcast 서버 주소 목록
 * @param setup [ClientConfig] 추가 설정 블록
 * @return 설정된 [ClientConfig]
 */
fun hazelcastClientConfig(
    vararg addresses: String = arrayOf("localhost:5701"),
    setup: ClientConfig.() -> Unit = {},
): ClientConfig = ClientConfig().apply {
    networkConfig.addAddress(*addresses)
    setup()
}

/**
 * [ClientConfig]에 [HazelcastNearCacheConfig]를 일괄 적용합니다.
 *
 * @param nearCacheConfigs 적용할 Near Cache 설정 목록
 * @return 수정된 [ClientConfig]
 */
fun ClientConfig.withNearCaches(vararg nearCacheConfigs: HazelcastNearCacheConfig): ClientConfig = apply {
    nearCacheConfigs.forEach { addNearCacheConfig(it.toNearCacheConfig()) }
}
