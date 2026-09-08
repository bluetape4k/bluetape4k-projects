package io.bluetape4k.cache.jcache

import ch.qos.logback.classic.Level
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import java.net.URI
import javax.cache.Caching

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceCachingProviderTest {

    companion object: KLogging()

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
        manager1 shouldBeSameInstanceAs manager2
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

    @Test
    fun `URI credentials are redacted in provider logs`() {
        val loggerName = LettuceCachingProvider::class.java.name
        val logger = LoggerFactory.getLogger(loggerName) as ch.qos.logback.classic.Logger
        val previousLevel = logger.level
        val password = "provider-secret"
        val uri = URI("redis://cache-user:$password@localhost:6379/0")

        InMemoryLogbackAppender(loggerName).use { appender ->
            try {
                logger.level = Level.DEBUG
                provider.getCacheManager(uri, provider.defaultClassLoader)
                provider.close(uri, provider.defaultClassLoader)

                val messages = appender.messages.joinToString("\n")
                messages shouldContain "redis://<redacted>@localhost:6379/0"
                messages shouldNotContain password
            } finally {
                logger.level = previousLevel
            }
        }
    }
}
