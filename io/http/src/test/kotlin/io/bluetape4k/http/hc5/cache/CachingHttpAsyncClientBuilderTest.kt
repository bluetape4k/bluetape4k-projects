package io.bluetape4k.http.hc5.cache

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.methods.toProducer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

class CachingHttpAsyncClientBuilderTest: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `cachingHttpAsyncClient DSL 로 생성`() {
        val client: CloseableHttpAsyncClient = cachingHttpAsyncClient { }
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `memoryCachingHttpAsyncClientOf 생성`() {
        val client: CloseableHttpAsyncClient = memoryCachingHttpAsyncClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `fileCachingHttpAsyncClientOf 생성`(@TempDir tempDir: File) {
        val client: CloseableHttpAsyncClient = fileCachingHttpAsyncClientOf(tempDir)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `memoryCachingHttpAsyncClientOf 로 GET 요청`() {
        memoryCachingHttpAsyncClientOf().use { client ->
            client.start()
            val request = SimpleRequestBuilder.get("$httpbinBaseUrl/get").build()
            val future = client.execute(
                request.toProducer(),
                SimpleResponseConsumer.create(),
                HttpClientContext.create(),
                null
            )
            val response = future.get(10, TimeUnit.SECONDS)
            log.debug { "Async Caching GET status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }

    @Test
    fun `fileCachingHttpAsyncClientOf 로 GET 요청`(@TempDir tempDir: File) {
        fileCachingHttpAsyncClientOf(tempDir).use { client ->
            client.start()
            val request = SimpleRequestBuilder.get("$httpbinBaseUrl/get").build()
            val future = client.execute(
                request.toProducer(),
                SimpleResponseConsumer.create(),
                HttpClientContext.create(),
                null
            )
            val response = future.get(10, TimeUnit.SECONDS)
            log.debug { "File Async Caching GET status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }

    @Test
    fun `memoryCachingHttpAsyncClientOf 파라미터 커스텀 생성`() {
        val client: CloseableHttpAsyncClient = memoryCachingHttpAsyncClientOf(maxEntries = 500, maxObjectSizeBytes = 32 * 1024L)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `fileCachingHttpAsyncClientOf 파라미터 커스텀 생성`(@TempDir tempDir: File) {
        val client: CloseableHttpAsyncClient = fileCachingHttpAsyncClientOf(tempDir, maxCacheMb = 50L, maxObjectSizeBytes = 512 * 1024L)
        client.shouldNotBeNull()
        client.close()
    }
}
