package io.bluetape4k.cache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class JCachingTest {

    companion object: KLogging()

    @Test
    fun `JCaching Cache2k - getOrCreate 캐시 생성`() {
        val cache = JCaching.Cache2k.getOrCreate<String, Int>("jcaching-cache2k-test")
        cache.shouldNotBeNull()

        cache.put("key1", 100)
        cache.get("key1") shouldBeEqualTo 100
        cache.containsKey("key1").shouldBeTrue()
        cache.containsKey("missing").shouldBeFalse()
        cache["missing"].shouldBeNull()

        cache.remove("key1").shouldBeTrue()
        cache.containsKey("key1").shouldBeFalse()
    }

    @Test
    fun `JCaching Caffeine - getOrCreate 캐시 생성`() {
        val cache = JCaching.Caffeine.getOrCreate<String, String>("jcaching-caffeine-test")
        cache.shouldNotBeNull()

        cache.putIfAbsent("hello", "world").shouldBeTrue()
        cache.putIfAbsent("hello", "other").shouldBeFalse()
        cache.get("hello") shouldBeEqualTo "world"

        cache.close()
    }

    @Test
    fun `JCaching EhCache - getOrCreate 캐시 생성`() {
        val cache = JCaching.EhCache.getOrCreate<String, Long>("jcaching-ehcache-test")
        cache.shouldNotBeNull()

        cache.put("count", 42L)
        cache.get("count") shouldBeEqualTo 42L

        cache.remove("count")
        cache.get("count").shouldBeNull()

        cache.close()
    }

    @Test
    fun `JCaching Cache2k cacheManager - 동일 인스턴스 반환`() {
        val m1 = JCaching.Cache2k.cacheManager
        val m2 = JCaching.Cache2k.cacheManager
        (m1 === m2).shouldBeTrue()
    }

    @Test
    fun `JCaching Caffeine cacheManager - 동일 인스턴스 반환`() {
        val m1 = JCaching.Caffeine.cacheManager
        val m2 = JCaching.Caffeine.cacheManager
        (m1 === m2).shouldBeTrue()
    }

    @Test
    fun `JCaching EhCache cacheManager - 동일 인스턴스 반환`() {
        val m1 = JCaching.EhCache.cacheManager
        val m2 = JCaching.EhCache.cacheManager
        (m1 === m2).shouldBeTrue()
    }
}
