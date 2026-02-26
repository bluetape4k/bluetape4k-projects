package io.bluetape4k.hazelcast.cache

import com.hazelcast.client.config.ClientConfig
import io.bluetape4k.hazelcast.AbstractHazelcastTest
import io.bluetape4k.hazelcast.hazelcastClient
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [HazelcastNearCache]의 CRUD 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Hazelcast 서버를 실행하고 클라이언트 Near Cache를 활성화하여 테스트합니다.
 */
class HazelcastNearCacheTest: AbstractHazelcastTest() {

    companion object: KLogging() {
        private const val MAP_NAME = "test-near-cache-map"
    }

    private lateinit var nearCache: HazelcastNearCache<String, String>

    @BeforeEach
    fun setup() {
        val nearCacheConfig = HazelcastNearCacheConfig(cacheName = MAP_NAME)
        val clientConfig = ClientConfig().apply {
            networkConfig.addAddress(hazelcastServer.url)
            addNearCacheConfig(nearCacheConfig.toNearCacheConfig())
        }
        val client = hazelcastClient(clientConfig)
        nearCache = HazelcastNearCache(client.getMap(MAP_NAME))
        nearCache.clear()
    }

    @Test
    fun `존재하지 않는 키 조회 시 null 반환`() {
        val result = nearCache["non-existent-key"]
        result.shouldBeNull()
    }

    @Test
    fun `set으로 저장 후 get으로 조회 가능`() {
        nearCache["key1"] = "value1"

        val result = nearCache["key1"]
        result shouldBeEqualTo "value1"
    }

    @Test
    fun `put으로 저장 시 이전 값 반환`() {
        val prev1 = nearCache.put("key2", "first")
        prev1.shouldBeNull()

        val prev2 = nearCache.put("key2", "second")
        prev2 shouldBeEqualTo "first"

        nearCache["key2"] shouldBeEqualTo "second"
    }

    @Test
    fun `putIfAbsent은 키가 없을 때만 저장`() {
        val prev1 = nearCache.putIfAbsent("key3", "original")
        prev1.shouldBeNull()

        val prev2 = nearCache.putIfAbsent("key3", "override")
        prev2 shouldBeEqualTo "original"

        nearCache["key3"] shouldBeEqualTo "original"
    }

    @Test
    fun `putAll로 여러 항목 일괄 저장 후 getAll로 조회`() {
        val entries = mapOf("a" to "alpha", "b" to "beta", "c" to "gamma")
        nearCache.putAll(entries)

        val result = nearCache.getAll(setOf("a", "b", "c"))
        result.size shouldBeEqualTo 3
        result["a"] shouldBeEqualTo "alpha"
        result["b"] shouldBeEqualTo "beta"
        result["c"] shouldBeEqualTo "gamma"
    }

    @Test
    fun `getAll에서 일부 키만 존재하는 경우 존재하는 항목만 반환`() {
        nearCache["exists"] = "value"

        val result = nearCache.getAll(setOf("exists", "missing"))
        result.size shouldBeEqualTo 1
        result["exists"] shouldBeEqualTo "value"
    }

    @Test
    fun `remove로 항목 삭제 후 null 반환`() {
        nearCache["del-key"] = "del-value"
        nearCache["del-key"].shouldNotBeNull()

        val removed = nearCache.remove("del-key")
        removed shouldBeEqualTo "del-value"

        nearCache["del-key"].shouldBeNull()
    }

    @Test
    fun `존재하지 않는 키를 remove해도 null 반환`() {
        val removed = nearCache.remove("no-such-key")
        removed.shouldBeNull()
    }

    @Test
    fun `removeAll로 여러 항목 일괄 삭제`() {
        nearCache["r1"] = "v1"
        nearCache["r2"] = "v2"
        nearCache["r3"] = "v3"

        nearCache.removeAll(setOf("r1", "r2"))

        nearCache["r1"].shouldBeNull()
        nearCache["r2"].shouldBeNull()
        nearCache["r3"] shouldBeEqualTo "v3"
    }

    @Test
    fun `containsKey로 키 존재 여부 확인`() {
        nearCache["ck-key"] = "ck-value"

        nearCache.containsKey("ck-key").shouldBeTrue()
        nearCache.containsKey("ck-missing").shouldBeFalse()
    }

    @Test
    fun `clear 후 모든 항목이 삭제됨`() {
        nearCache["c1"] = "v1"
        nearCache["c2"] = "v2"

        nearCache.clear()

        nearCache["c1"].shouldBeNull()
        nearCache["c2"].shouldBeNull()
    }

    @Test
    fun `size가 저장된 항목 수를 반환`() {
        nearCache.clear()
        nearCache["s1"] = "v1"
        nearCache["s2"] = "v2"

        nearCache.size shouldBeEqualTo 2
    }

    @Test
    fun `name이 맵 이름을 반환`() {
        nearCache.name shouldBeEqualTo MAP_NAME
    }
}
