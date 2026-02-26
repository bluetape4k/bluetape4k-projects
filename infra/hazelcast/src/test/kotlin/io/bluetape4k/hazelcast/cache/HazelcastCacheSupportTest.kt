package io.bluetape4k.hazelcast.cache

import com.hazelcast.client.config.ClientConfig
import io.bluetape4k.hazelcast.AbstractHazelcastTest
import io.bluetape4k.hazelcast.hazelcastClient
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [hazelcastNearCache], [hazelcastClientConfig], [ClientConfig.withNearCaches] 편의 함수를
 * 검증하는 통합 테스트입니다.
 *
 * Docker로 Hazelcast 서버를 실행하고 각 팩토리 함수 동작을 검증합니다.
 */
class HazelcastCacheSupportTest: AbstractHazelcastTest() {

    companion object: KLogging()

    @Test
    fun `hazelcastNearCache로 NearCache 생성 및 CRUD 검증`() {
        val mapName = "support-test-map-1"
        val nearCache = hazelcastNearCache<String, String>(
            mapName = mapName,
            addresses = arrayOf(hazelcastServer.url),
        )
        nearCache.clear()

        nearCache.shouldNotBeNull()
        nearCache.name shouldBeEqualTo mapName

        nearCache.put("key1", "value1")
        nearCache["key1"] shouldBeEqualTo "value1"

        nearCache.put("key2", "value2")
        nearCache["key2"] shouldBeEqualTo "value2"

        nearCache.clear()
    }

    @Test
    fun `ClientConfig 기반 hazelcastNearCache로 NearCache 생성`() {
        val mapName = "support-test-map-2"
        val nearCacheConfig = HazelcastNearCacheConfig(cacheName = mapName)
        val clientConfig = ClientConfig().apply {
            networkConfig.addAddress(hazelcastServer.url)
            addNearCacheConfig(nearCacheConfig.toNearCacheConfig())
        }

        val nearCache = hazelcastNearCache<String, Int>(
            mapName = mapName,
            clientConfig = clientConfig,
        )
        nearCache.clear()

        nearCache.shouldNotBeNull()
        nearCache.name shouldBeEqualTo mapName

        nearCache.put("score", 42)
        nearCache["score"] shouldBeEqualTo 42

        nearCache.clear()
    }

    @Test
    fun `hazelcastClientConfig로 ClientConfig 생성`() {
        val address = hazelcastServer.url
        val clientConfig = hazelcastClientConfig(address)

        clientConfig.shouldNotBeNull()
        clientConfig.networkConfig.addresses shouldBeEqualTo listOf(address)
    }

    @Test
    fun `withNearCaches로 여러 Near Cache 설정 일괄 적용`() {
        val mapName1 = "support-test-map-3"
        val mapName2 = "support-test-map-4"
        val address = hazelcastServer.url

        val clientConfig = hazelcastClientConfig(address)
            .withNearCaches(
                HazelcastNearCacheConfig(cacheName = mapName1),
                HazelcastNearCacheConfig(cacheName = mapName2),
            )

        clientConfig.shouldNotBeNull()
        clientConfig.getNearCacheConfig(mapName1).shouldNotBeNull()
        clientConfig.getNearCacheConfig(mapName2).shouldNotBeNull()

        val client = hazelcastClient(clientConfig)
        val nearCache1 = HazelcastNearCache<String, String>(client.getMap(mapName1))
        val nearCache2 = HazelcastNearCache<String, String>(client.getMap(mapName2))

        nearCache1.clear()
        nearCache2.clear()

        nearCache1.put("k1", "v1")
        nearCache2.put("k2", "v2")

        nearCache1["k1"] shouldBeEqualTo "v1"
        nearCache2["k2"] shouldBeEqualTo "v2"

        nearCache1.clear()
        nearCache2.clear()
    }
}
