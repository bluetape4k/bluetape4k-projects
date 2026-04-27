package io.bluetape4k.http.hc5.cache

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CachingHttpClientBuilderTest: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `cachingHttpClient DSL 로 생성`() {
        val client: CloseableHttpClient = cachingHttpClient { }
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `cachingHttpClient cacheStorage 포함 생성`() {
        val storage = InMemoryHttpCacheStorage.createObjectCache()
        val client: CloseableHttpClient = cachingHttpClient(storage) { }
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `memoryCachingHttpClientOf 생성`() {
        val client: CloseableHttpClient = memoryCachingHttpClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `fileCachingHttpClientOf 생성`(@TempDir tempDir: File) {
        val client: CloseableHttpClient = fileCachingHttpClientOf(tempDir)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `memoryCachingHttpClientOf 로 GET 요청`() {
        memoryCachingHttpClientOf().use { client ->
            val response = client.execute(HttpGet("$httpbinBaseUrl/get")) { it }
            log.debug { "Caching GET status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }

    @Test
    fun `fileCachingHttpClientOf 로 GET 요청`(@TempDir tempDir: File) {
        fileCachingHttpClientOf(tempDir).use { client ->
            val response = client.execute(HttpGet("$httpbinBaseUrl/get")) { it }
            log.debug { "File Caching GET status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }
}
