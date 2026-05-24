package io.bluetape4k.http.hc5.cache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class CacheConfigTest {

    companion object : KLogging()

    @Test
    fun `cacheConfig DSL로 CacheConfig 생성`() {
        val config = cacheConfig {
            setMaxCacheEntries(500)
            setMaxObjectSize(32 * 1024L)
        }
        config.shouldNotBeNull()
        config.maxCacheEntries shouldBeEqualTo 500
        config.maxObjectSize shouldBeEqualTo 32 * 1024L
    }

    @Test
    fun `memoryCacheConfigOf 기본값으로 생성`() {
        val config = memoryCacheConfigOf()
        config.shouldNotBeNull()
        config.maxCacheEntries shouldBeEqualTo 1_000
        config.maxObjectSize shouldBeEqualTo 64 * 1024L
    }

    @Test
    fun `memoryCacheConfigOf 커스텀 값으로 생성`() {
        val config = memoryCacheConfigOf(maxEntries = 200, maxObjectSizeBytes = 16 * 1024L)
        config.shouldNotBeNull()
        config.maxCacheEntries shouldBeEqualTo 200
        config.maxObjectSize shouldBeEqualTo 16 * 1024L
    }

    @Test
    fun `fileCacheConfigOf 기본값으로 생성`() {
        val config = fileCacheConfigOf()
        config.shouldNotBeNull()
        config.maxCacheEntries shouldBeEqualTo 10_000
        config.maxObjectSize shouldBeEqualTo 1024 * 1024L
    }

    @Test
    fun `fileCacheConfigOf 커스텀 값으로 생성`() {
        val config = fileCacheConfigOf(maxEntries = 5_000, maxObjectSizeBytes = 512 * 1024L)
        config.shouldNotBeNull()
        config.maxCacheEntries shouldBeEqualTo 5_000
        config.maxObjectSize shouldBeEqualTo 512 * 1024L
    }
}
