package io.bluetape4k.jwt.provider

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.jwt.keychain.repository.inmemory.InMemoryKeyChainRepository
import io.jsonwebtoken.JwtParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.concurrent.ConcurrentLinkedQueue

@Execution(ExecutionMode.SAME_THREAD)
class JwtParserSupportTest {

    @Test
    fun `parser cache reuses one parser concurrently and removes it when provider closes`() {
        val initialCacheSize = jwtParserCache.size
        val repository = InMemoryKeyChainRepository()
        val provider = DefaultJwtProvider.forTesting(
            keyChainRepository = repository,
            rotationIntervalMillis = 60_000,
        )
        val parsers = ConcurrentLinkedQueue<JwtParser>()

        try {
            MultithreadingTester()
                .workers(8)
                .rounds(16)
                .add { parsers += provider.currentJwtParser() }
                .run()

            parsers.distinct().size.shouldBeEqualTo(1)
            jwtParserCache.containsKey(provider).shouldBeTrue()
        } finally {
            provider.close()
            repository.close()
        }

        jwtParserCache.containsKey(provider).shouldBeFalse()
        jwtParserCache.size.shouldBeEqualTo(initialCacheSize)
    }

    @Test
    fun `closing many providers removes every parser cache entry`() {
        val initialCacheSize = jwtParserCache.size
        val providers = (0 until 32).map { index ->
            FixedJwtProvider(kid = "parser-cache-$index")
        }

        try {
            providers.forEach { provider ->
                val jwt = provider.compose { subject = "parser-cache" }
                provider.parse(jwt)
                jwtParserCache.containsKey(provider).shouldBeTrue()
            }
        } finally {
            providers.forEach { it.close() }
        }

        providers.count(jwtParserCache::containsKey).shouldBeEqualTo(0)
        jwtParserCache.size.shouldBeEqualTo(initialCacheSize)
    }
}
