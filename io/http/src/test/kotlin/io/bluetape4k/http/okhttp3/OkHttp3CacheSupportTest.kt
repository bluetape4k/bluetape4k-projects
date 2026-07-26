package io.bluetape4k.http.okhttp3

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.http.AbstractHttpTest
import io.bluetape4k.logging.KLogging
import okhttp3.Request
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OkHttp3CacheSupportTest: AbstractHttpTest() {

    companion object: KLogging()

    @Test
    fun `okhttp3ClientWithCache 생성`(@TempDir tempDir: File) {
        val client = okhttp3ClientWithCache(cacheDir = tempDir)
        client.shouldNotBeNull()
        client.cache.shouldNotBeNull()
        client.cache!!.maxSize() shouldBeEqualTo 50L * 1024 * 1024
    }

    @Test
    fun `okhttp3ClientWithCache maxCacheMb 커스텀 설정`(@TempDir tempDir: File) {
        val client = okhttp3ClientWithCache(cacheDir = tempDir, maxCacheMb = 100L)
        client.cache!!.maxSize() shouldBeEqualTo 100L * 1024 * 1024
    }

    @Test
    fun `Cache metrics 초기 상태`(@TempDir tempDir: File) {
        val client = okhttp3ClientWithCache(cacheDir = tempDir)
        val cache = client.cache!!
        val metrics = cache.metrics()

        metrics.requestCount.shouldBeZero()
        metrics.hitCount.shouldBeZero()
        metrics.networkCount.shouldBeZero()
        metrics.hitRate shouldBeEqualTo 0.0
    }

    @Test
    fun `Cache metrics GET 요청 후 업데이트`(@TempDir tempDir: File) {
        val client = okhttp3ClientWithCache(cacheDir = tempDir)
        val cache = client.cache!!

        val request = Request.Builder().url("$httpbinBaseUrl/get").build()
        client.newCall(request).execute().use { /* consume body */ }

        val metrics = cache.metrics()
        metrics.requestCount shouldBeEqualTo 1
    }

    @Test
    fun `Cache logMetrics label 없이 호출 가능`(@TempDir tempDir: File) {
        val client = okhttp3ClientWithCache(cacheDir = tempDir)
        client.cache!!.logMetrics(log)
    }

    @Test
    fun `Cache logMetrics label 포함 호출 가능`(@TempDir tempDir: File) {
        val client = okhttp3ClientWithCache(cacheDir = tempDir)
        client.cache!!.logMetrics(log, label = "test-cache")
    }

    @Test
    fun `okhttp3ClientWithCache builder 블록 적용`(@TempDir tempDir: File) {
        val client = okhttp3ClientWithCache(cacheDir = tempDir) {
            connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        }
        client.shouldNotBeNull()
        client.cache.shouldNotBeNull()
    }
}
