package io.bluetape4k.cache.cache2k

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.time.Duration

class Cache2kSupportExtTest {

    companion object: KLogging()

    @Test
    fun `defaultCache2kManager는 null이 아니다`() {
        defaultCache2kManager.shouldNotBeNull()
    }

    @Test
    fun `defaultCache2kManager는 동일 인스턴스를 반환한다`() {
        val m1 = defaultCache2kManager
        val m2 = defaultCache2kManager
        (m1 === m2) shouldBeEqualTo true
    }

    @Test
    fun `cache2kConfiguration - 이름과 설정으로 Cache2kConfig 생성`() {
        val cacheName = "config-test-${System.nanoTime()}"
        val config = cache2kConfiguration<String, Int>(cacheName) {
            entryCapacity = 500
            expireAfterWrite = Duration.ofSeconds(30)
        }

        config.shouldNotBeNull()
        config.name shouldBeEqualTo cacheName
        config.entryCapacity shouldBeEqualTo 500L
    }

    @Test
    fun `cache2kConfiguration - 빈 name이면 IllegalArgumentException 발생`() {
        assertFailsWith<IllegalArgumentException> {
            cache2kConfiguration<String, Int>("") { }
        }
    }

    @Test
    fun `getCache2k - 존재하지 않는 캐시는 null 반환`() {
        val result = getCache2k<String, Int>("non-existent-cache-${System.nanoTime()}")
        result shouldBeEqualTo null
    }

    @Test
    fun `getOrCreateCache2k - 새 캐시 생성 후 반환`() {
        val cacheName = "get-or-create-test-${System.nanoTime()}"
        val cache = getOrCreateCache2k<String, Int>(cacheName) {
            name(cacheName)
            entryCapacity(100)
        }

        cache.shouldNotBeNull()
        cache.put("hello", 42)
        cache.get("hello") shouldBeEqualTo 42

        cache.close()
    }

    @Test
    fun `getOrCreateCache2k - 빈 name이면 IllegalArgumentException 발생`() {
        assertFailsWith<IllegalArgumentException> {
            getOrCreateCache2k<String, Int>("") { }
        }
    }

    @Test
    fun `cache2k builder - 이름과 entryCapacity 설정`() {
        val cacheName = "builder-test-${System.nanoTime()}"
        val cache = cache2k<String, String> {
            name(cacheName)
            entryCapacity(200)
        }.build()

        cache.shouldNotBeNull()
        cache.name shouldBeEqualTo cacheName

        cache.put("key", "value")
        cache.get("key") shouldBeEqualTo "value"

        cache.close()
    }
}
