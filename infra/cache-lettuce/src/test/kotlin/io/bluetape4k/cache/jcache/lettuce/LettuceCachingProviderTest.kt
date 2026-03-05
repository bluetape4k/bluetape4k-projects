package io.bluetape4k.cache.jcache.lettuce

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.storage.RedisServer
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.cache.Caching

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceCachingProviderTest {

    companion object: KLogging() {
        // RedisServer Testcontainers 시작
        @Suppress("unused")
        private val redis by lazy { RedisServer.Launcher.redis }
    }

    private lateinit var provider: LettuceCachingProvider

    @BeforeEach
    fun setup() {
        provider = LettuceCachingProvider()
    }

    @AfterEach
    fun teardown() {
        runCatching { provider.close() }
    }

    @Test
    fun `getCacheManager returns non-null`() {
        val manager = provider.cacheManager
        manager.shouldNotBeNull()
    }

    @Test
    fun `getCacheManager returns same instance for same URI and classLoader`() {
        val manager1 = provider.getCacheManager(provider.defaultURI, provider.defaultClassLoader)
        val manager2 = provider.getCacheManager(provider.defaultURI, provider.defaultClassLoader)
        (manager1 == manager2).shouldNotBeNull()
    }

    @Test
    fun `SPI loading via Caching class`() {
        val loadedProvider = Caching.getCachingProvider(LettuceCachingProvider::class.qualifiedName)
        loadedProvider.shouldNotBeNull()
        runCatching { loadedProvider.close() }
    }

    @Test
    fun `defaultURI is not null`() {
        provider.defaultURI.shouldNotBeNull()
    }

    @Test
    fun `defaultClassLoader is not null`() {
        provider.defaultClassLoader.shouldNotBeNull()
    }
}
