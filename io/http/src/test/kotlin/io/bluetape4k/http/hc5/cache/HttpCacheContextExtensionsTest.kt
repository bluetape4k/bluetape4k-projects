package io.bluetape4k.http.hc5.cache

import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.apache.hc.client5.http.cache.CacheResponseStatus
import org.apache.hc.client5.http.cache.HttpCacheContext
import org.junit.jupiter.api.Test

class HttpCacheContextExtensionsTest {

    companion object : KLogging()

    private fun contextWith(status: CacheResponseStatus): HttpCacheContext =
        HttpCacheContext.create().also { it.setCacheResponseStatus(status) }

    @Test
    fun `isHit - CACHE_HIT이면 true`() {
        contextWith(CacheResponseStatus.CACHE_HIT).isHit.shouldBeTrue()
    }

    @Test
    fun `isHit - CACHE_MISS이면 false`() {
        contextWith(CacheResponseStatus.CACHE_MISS).isHit.shouldBeFalse()
    }

    @Test
    fun `isMiss - CACHE_MISS이면 true`() {
        contextWith(CacheResponseStatus.CACHE_MISS).isMiss.shouldBeTrue()
    }

    @Test
    fun `isMiss - CACHE_HIT이면 false`() {
        contextWith(CacheResponseStatus.CACHE_HIT).isMiss.shouldBeFalse()
    }

    @Test
    fun `isValidated - VALIDATED이면 true`() {
        contextWith(CacheResponseStatus.VALIDATED).isValidated.shouldBeTrue()
    }

    @Test
    fun `isValidated - CACHE_HIT이면 false`() {
        contextWith(CacheResponseStatus.CACHE_HIT).isValidated.shouldBeFalse()
    }

    @Test
    fun `isCacheModuleResponse - CACHE_MODULE_RESPONSE이면 true`() {
        contextWith(CacheResponseStatus.CACHE_MODULE_RESPONSE).isCacheModuleResponse.shouldBeTrue()
    }

    @Test
    fun `isCacheModuleResponse - CACHE_HIT이면 false`() {
        contextWith(CacheResponseStatus.CACHE_HIT).isCacheModuleResponse.shouldBeFalse()
    }

    @Test
    fun `cacheStatusDescription - 각 상태의 이름을 반환`() {
        contextWith(CacheResponseStatus.CACHE_HIT).cacheStatusDescription() shouldBeEqualTo "CACHE_HIT"
        contextWith(CacheResponseStatus.CACHE_MISS).cacheStatusDescription() shouldBeEqualTo "CACHE_MISS"
        contextWith(CacheResponseStatus.VALIDATED).cacheStatusDescription() shouldBeEqualTo "VALIDATED"
        contextWith(CacheResponseStatus.CACHE_MODULE_RESPONSE).cacheStatusDescription() shouldBeEqualTo "CACHE_MODULE_RESPONSE"
    }

    @Test
    fun `cacheStatusDescription - 상태 미설정 시 unknown 반환`() {
        val ctx = HttpCacheContext.create()
        ctx.cacheStatusDescription() shouldBeEqualTo "unknown"
    }

    @Test
    fun `logCacheStatus - label 없이 호출 가능`() {
        contextWith(CacheResponseStatus.CACHE_HIT).logCacheStatus(log)
    }

    @Test
    fun `logCacheStatus - label 포함 호출 가능`() {
        contextWith(CacheResponseStatus.CACHE_MISS).logCacheStatus(log, label = "test")
    }
}
