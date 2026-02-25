package io.bluetape4k.ignite.cache

import io.bluetape4k.ignite.AbstractIgniteTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [getOrCreateClientNearCache] 확장 함수의 NearCache 생성 및 기본 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Ignite 2.x 서버를 실행하고, 씬 클라이언트로 연결하여 테스트합니다.
 */
class IgniteCacheSupportTest: AbstractIgniteTest() {

    companion object: KLogging() {
        private const val CACHE_NAME = "support-test-near-cache"
    }

    @Test
    fun `getOrCreateClientNearCache로 NearCache 생성 및 이름 확인`() {
        val config = IgniteNearCacheConfig(cacheName = CACHE_NAME)
        val nearCache: IgniteClientNearCache<String, String> = igniteClient.getOrCreateClientNearCache(config)

        nearCache.shouldNotBeNull()
        nearCache.name shouldBeEqualTo CACHE_NAME
    }

    @Test
    fun `NearCache로 기본 CRUD 동작 확인`() {
        val config = IgniteNearCacheConfig(cacheName = CACHE_NAME)
        val nearCache: IgniteClientNearCache<String, String> = igniteClient.getOrCreateClientNearCache(config)

        nearCache.clearFrontCache()
        nearCache.backCache.clear()

        // put
        nearCache.put("crud-key", "crud-value")

        // get
        val result = nearCache.get("crud-key")
        result shouldBeEqualTo "crud-value"

        // containsKey
        nearCache.containsKey("crud-key").shouldBeTrue()
        nearCache.containsKey("non-existent").shouldBeFalse()
    }
}
