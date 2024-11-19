package io.bluetape4k.http.hc5.cache

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.client5.http.cache.HttpCacheEntry
import org.apache.hc.client5.http.cache.HttpCacheStorage
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpHead
import org.apache.hc.client5.http.impl.cache.CacheKeyGenerator
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.io.CloseMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class AbstractHttpCacheStorageTest: AbstractHc5Test() {

    companion object: KLogging()

    private lateinit var cacheStorage: HttpCacheStorage
    private lateinit var client: CloseableHttpClient

    protected abstract fun createCacheStorage(): HttpCacheStorage

    private val host = HttpHost("https", "nghttp2.org")

    @BeforeEach
    fun beforeEach() {
        cacheStorage = createCacheStorage() // InMemoryHttpCacheStorage.createObjectCache()
        client = cachingHttpClient(cacheStorage) { }
    }

    @AfterEach
    fun afterEach() {
        client.close(CloseMode.GRACEFUL)
    }

    @Test
    fun `create cache entries on get`() {
        val request = HttpGet("/httpbin/get")
        val key = CacheKeyGenerator.INSTANCE.generateKey(host, request)

        val response = client.execute(host, request) { it }
        response.code shouldBeEqualTo 200
        response.headers.shouldNotBeEmpty()
        val bodyBytes = EntityUtils.toByteArray(response.entity)
        log.debug { "body=${bodyBytes.toUtf8String()}" }

        val cacheEntry: HttpCacheEntry = cacheStorage.getEntry(key).shouldNotBeNull()
        cacheEntry.resource.get() shouldBeEqualTo bodyBytes
    }

    @Test
    fun `create cache entries on get with gzip`() {
        val request = HttpGet("/httpbin/gzip")
        val key = CacheKeyGenerator.INSTANCE.generateKey(host, request)

        val response = client.execute(host, request) { it }
        response.code shouldBeEqualTo 200
        response.headers.shouldNotBeEmpty()
        val bodyBytes = EntityUtils.toByteArray(response.entity)
        log.debug { "body=${bodyBytes.toUtf8String()}" }

        val cacheEntry: HttpCacheEntry = cacheStorage.getEntry(key).shouldNotBeNull()
        assertCacheEntry(cacheEntry)
    }

    @Test
    fun `create cache entries on head`() {
        val request = HttpHead("/httpbin/headers")
        val key = CacheKeyGenerator.INSTANCE.generateKey(host, request)

        val response = client.execute(host, request) { it }
        response.code shouldBeEqualTo 200
        response.headers.shouldNotBeEmpty()

        val cacheEntry: HttpCacheEntry = cacheStorage.getEntry(key).shouldNotBeNull()
        assertCacheEntry(cacheEntry)
    }

    @Test
    fun `create cache entries on options`() {
        val request = HttpHead("/httpbin/get")
        val key = CacheKeyGenerator.INSTANCE.generateKey(host, request)

        val response = client.execute(host, request) { it }
        response.code shouldBeEqualTo 200
        response.headers.shouldNotBeEmpty()

        val cacheEntry: HttpCacheEntry = cacheStorage.getEntry(key).shouldNotBeNull()
        assertCacheEntry(cacheEntry)
    }

    @Test
    fun `create cache entries on trace`() {
        val request = HttpHead("/httpbin/get")
        val key = CacheKeyGenerator.INSTANCE.generateKey(host, request)

        val response = client.execute(host, request) { it }
        response.code shouldBeEqualTo 200
        response.headers.shouldNotBeEmpty()

        val cacheEntry: HttpCacheEntry = cacheStorage.getEntry(key).shouldNotBeNull()
        assertCacheEntry(cacheEntry)
    }

    @Test
    fun `create cache entries on cache`() {
        val request = HttpHead("/httpbin/cache/123")
        val key = CacheKeyGenerator.INSTANCE.generateKey(host, request)

        val response = client.execute(host, request) { it }
        response.code shouldBeEqualTo 200
        response.headers.shouldNotBeEmpty()

        val cacheEntry: HttpCacheEntry = cacheStorage.getEntry(key).shouldNotBeNull()
        assertCacheEntry(cacheEntry)
    }

    private fun assertCacheEntry(cacheEntry: HttpCacheEntry) {
        cacheEntry.getLastHeader("Via").value.shouldContain("(cache)")
    }
}
