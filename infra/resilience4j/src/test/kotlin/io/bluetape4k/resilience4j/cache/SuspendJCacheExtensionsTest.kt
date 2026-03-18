package io.bluetape4k.resilience4j.cache

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class SuspendJCacheExtensionsTest {

    companion object: KLoggingChannel()

    private fun newSuspendCache(name: String = "test-${System.nanoTime()}"): SuspendCache<String, String> {
        val jcache = CaffeineJCacheProvider.getJCache<String, String>(name)
        jcache.clear()
        return SuspendCache.of(jcache)
    }

    @Test
    fun `withSuspendCache - 캐시 미스 시 loader를 실행하고 값을 캐시한다`() = runSuspendTest {
        val cache = newSuspendCache()
        var loadCount = 0

        val result = withSuspendCache(cache, "key1") {
            loadCount++
            "value1"
        }

        result shouldBeEqualTo "value1"
        loadCount shouldBeEqualTo 1
        cache.containsKey("key1").shouldBeTrue()
    }

    @Test
    fun `withSuspendCache - 캐시 히트 시 loader를 실행하지 않는다`() = runSuspendTest {
        val cache = newSuspendCache()
        var loadCount = 0

        // 첫 번째 호출 - 미스
        withSuspendCache(cache, "key1") {
            loadCount++
            "value1"
        }

        // 두 번째 호출 - 히트
        val result = withSuspendCache(cache, "key1") {
            loadCount++
            "should not be called"
        }

        result shouldBeEqualTo "value1"
        loadCount shouldBeEqualTo 1
    }

    @Test
    fun `withSuspendCache - 서로 다른 키는 독립적으로 캐시된다`() = runSuspendTest {
        val cache = newSuspendCache()
        var loadCount = 0

        val r1 = withSuspendCache(cache, "key1") { loadCount++; "v1" }
        val r2 = withSuspendCache(cache, "key2") { loadCount++; "v2" }
        val r3 = withSuspendCache(cache, "key1") { loadCount++; "should not run" }

        r1 shouldBeEqualTo "v1"
        r2 shouldBeEqualTo "v2"
        r3 shouldBeEqualTo "v1"
        loadCount shouldBeEqualTo 2
    }

    @Test
    fun `decorateSuspendSupplier - supplier가 캐시 함수로 변환된다`() = runSuspendTest {
        val cache = newSuspendCache()
        var loadCount = 0

        val cachedFn = cache.decorateSuspendSupplier {
            loadCount++
            "global-value"
        }

        val r1 = cachedFn("key1")
        val r2 = cachedFn("key1")

        r1 shouldBeEqualTo "global-value"
        r2 shouldBeEqualTo "global-value"
        loadCount shouldBeEqualTo 1
    }

    @Test
    fun `decorateSuspendFunction - key 기반 loader가 캐시 함수로 변환된다`() = runSuspendTest {
        val cache = newSuspendCache()
        var loadCount = 0

        val cachedFn = cache.decorateSuspendFunction { key: String ->
            loadCount++
            "value-for-$key"
        }

        val r1 = cachedFn("k1")
        val r2 = cachedFn("k2")
        val r3 = cachedFn("k1")

        r1 shouldBeEqualTo "value-for-k1"
        r2 shouldBeEqualTo "value-for-k2"
        r3 shouldBeEqualTo "value-for-k1"
        loadCount shouldBeEqualTo 2
    }

    @Test
    fun `executeSuspendFunction - 캐시 미스 시 loader를 실행한다`() = runSuspendTest {
        val cache = newSuspendCache()
        var loadCount = 0

        val result = cache.executeSuspendFunction("k1") {
            loadCount++
            "result"
        }

        result shouldBeEqualTo "result"
        loadCount shouldBeEqualTo 1
    }

    @Test
    fun `executeSuspendFunction - 캐시 히트 시 loader를 건너뛴다`() = runSuspendTest {
        val cache = newSuspendCache()
        var loadCount = 0

        cache.executeSuspendFunction("k1") { loadCount++; "result" }
        val result2 = cache.executeSuspendFunction("k1") { loadCount++; "should not run" }

        result2 shouldBeEqualTo "result"
        loadCount shouldBeEqualTo 1
    }

    @Test
    fun `SuspendCache metrics - 캐시 히트와 미스 수를 추적한다`() = runSuspendTest {
        val cache = newSuspendCache()

        withSuspendCache(cache, "k1") { "v1" }  // miss
        withSuspendCache(cache, "k1") { "v1" }  // hit
        withSuspendCache(cache, "k2") { "v2" }  // miss
        withSuspendCache(cache, "k2") { "v2" }  // hit
        withSuspendCache(cache, "k2") { "v2" }  // hit

        cache.metrics.getNumberOfCacheMisses() shouldBeEqualTo 2
        cache.metrics.getNumberOfCacheHits() shouldBeEqualTo 3
    }

    @Test
    fun `SuspendCache containsKey - 존재하는 키에 대해 true를 반환한다`() = runSuspendTest {
        val cache = newSuspendCache()

        cache.containsKey("nonexistent").shouldBeFalse()

        withSuspendCache(cache, "k1") { "v1" }

        cache.containsKey("k1").shouldBeTrue()
        cache.containsKey("k2").shouldBeFalse()
    }
}
