package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.resilience4j.SuspendDecorators
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Test
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

internal class MultiKeyLeaseDocumentationTest {

    @Test
    fun `README locales preserve policy structure and executable recovery`() = runSuspendIO {
        val english = readModuleFile("README.md")
        val korean = readModuleFile("README.ko.md")
        markerOrder(english) shouldBeEqualTo REQUIRED_MARKERS
        markerOrder(korean) shouldBeEqualTo REQUIRED_MARKERS
        headingLevels(english) shouldBeEqualTo REQUIRED_HEADING_LEVELS
        headingLevels(korean) shouldBeEqualTo REQUIRED_HEADING_LEVELS
        REQUIRED_POLICY_FRAGMENTS.forEach { fragment ->
            english.contains(fragment).shouldBeTrue()
            korean.contains(fragment).shouldBeTrue()
        }
        REQUIRED_SECTION_CONTRACTS.forEach { (marker, orderedTerms) ->
            assertOrderedTerms(section(english, marker), orderedTerms)
            assertOrderedTerms(section(korean, marker), orderedTerms)
        }

        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { connection ->
            val tag = LettuceTestUtils.randomName()
            val keys = listOf("ticket:{$tag}:ip", "ticket:{$tag}:user")
            val ownerToken = Base58.randomString(22)
            val commands = connection.sync()
            val lease = LettuceSuspendMultiKeyLease(connection)
            val retryable: (Throwable) -> Boolean = {
                it is IOException || it is RedisConnectionException || it is RedisCommandTimeoutException
            }
            val retry = Retry.of(
                "documentation-$tag",
                RetryConfig.custom<Any?>()
                    .maxAttempts(2)
                    .waitDuration(Duration.ofMillis(50))
                    .retryOnException(retryable)
                    .build(),
            )
            val circuitBreaker = CircuitBreaker.of(
                "documentation-$tag",
                CircuitBreakerConfig.custom()
                    .slidingWindowSize(20)
                    .minimumNumberOfCalls(10)
                    .failureRateThreshold(50.0F)
                    .recordException(retryable)
                    .ignoreException { it is CancellationException }
                    .build(),
            )
            val bulkhead = Bulkhead.of(
                "documentation-$tag",
                BulkheadConfig.custom()
                    .maxConcurrentCalls(32)
                    .maxWaitDuration(Duration.ofMillis(100))
                    .build(),
            )
            val attempts = AtomicInteger()
            try {
                commands.del(*keys.toTypedArray())
                val acquireResult = SuspendDecorators.ofSupplier {
                    val result = lease.acquire(keys, ownerToken, Duration.ofSeconds(10))
                    if (attempts.incrementAndGet() == 1) throw IOException("response lost after Redis success")
                    result
                }
                    .withRetry(retry)
                    .withCircuitBreaker(circuitBreaker)
                    .withBulkhead(bulkhead)
                    .invoke()

                acquireResult.shouldBeInstanceOf<MultiKeyAcquireResult.AlreadyOwned>()
                acquireAction(acquireResult) shouldBeEqualTo "recover"
                attempts.get() shouldBeEqualTo 2
                recoverAfterAmbiguousMutation(lease, keys, ownerToken)
                    .shouldBeInstanceOf<MultiKeyInspectResult.Owned>()
            } finally {
                commands.del(*keys.toTypedArray())
            }
        }
    }

    private suspend fun recoverAfterAmbiguousMutation(
        lease: LettuceSuspendMultiKeyLease,
        keys: List<String>,
        ownerToken: String,
    ): MultiKeyInspectResult = lease.inspect(keys, ownerToken)

    private fun acquireAction(result: MultiKeyAcquireResult): String = when (result) {
        MultiKeyAcquireResult.Acquired -> "start"
        is MultiKeyAcquireResult.AlreadyOwned -> "recover"
        is MultiKeyAcquireResult.PartialOwnership -> "reconcile"
        is MultiKeyAcquireResult.Conflicted -> "reject"
    }

    private fun markerOrder(readme: String): List<String> = MARKER_PATTERN.findAll(readme)
        .map { it.groupValues[1] }
        .toList()

    private fun headingLevels(readme: String): List<Int> = MARKER_WITH_HEADING_PATTERN.findAll(readme)
        .map { it.groupValues[2].length }
        .toList()

    private fun section(readme: String, marker: String): String {
        val start = readme.indexOf("<!-- multi-key-lease:$marker -->")
        require(start >= 0) { "Missing README marker: $marker" }
        val next = readme.indexOf("<!-- multi-key-lease:", start + 1)
        return readme.substring(start, if (next >= 0) next else readme.length)
    }

    private fun assertOrderedTerms(section: String, orderedTerms: List<String>) {
        var cursor = 0
        orderedTerms.forEach { term ->
            val next = section.indexOf(term, cursor, ignoreCase = true)
            next shouldBeGreaterOrEqualTo cursor
            cursor = next + term.length
        }
    }

    private fun readModuleFile(name: String): String {
        val workingDirectory = Path.of(System.getProperty("user.dir"))
        val moduleDirectory = if (workingDirectory.endsWith(Path.of("infra", "lettuce"))) {
            workingDirectory
        } else {
            workingDirectory.resolve("infra/lettuce")
        }
        return Files.readString(moduleDirectory.resolve(name))
    }

    private companion object {
        val REQUIRED_MARKERS: List<String> = listOf(
            "basic",
            "resilience",
            "recovery",
            "security-telemetry",
            "migration",
            "lost-token",
        )
        val REQUIRED_HEADING_LEVELS: List<Int> = listOf(3, 4, 4, 4, 4, 4)
        val REQUIRED_POLICY_FRAGMENTS: List<String> = listOf(
            ".withRetry(retry)",
            ".withCircuitBreaker(circuitBreaker)",
            ".withBulkhead(bulkhead)",
            "Duration.ofMillis(50)",
            ".slidingWindowSize(20)",
            ".minimumNumberOfCalls(10)",
            ".maxConcurrentCalls(32)",
            "Duration.ofMillis(100)",
            "operation",
            "result",
            "exception",
            "key/token",
            "JWT",
            "PII",
            "ACL",
            "TLS",
            "dual-write",
            "CompletableFuture",
            "caller wait",
            "Redis server execution",
        )
        val REQUIRED_SECTION_CONTRACTS: Map<String, List<String>> = mapOf(
            "recovery" to listOf(
                "acquire", "Acquired", "AlreadyOwned", "PartialOwnership", "Conflicted",
                "inspect", "Owned", "Lost", "PartialOwnership", "Conflicted",
                "renew", "Renewed", "PartialLoss", "Lost", "OwnershipMismatch",
                "release", "Released", "PartialRelease", "Lost", "OwnershipMismatch",
            ),
            "migration" to listOf(
                "1.", "shared slot", "durable", "2.", "writer", "3.", "TTL", "token",
                "4.", "namespace", "hash-tag", "token", "5.", "dual-write", "6.", "rollback",
            ),
            "lost-token" to listOf(
                "persistent key", "MultiKeyLeaseIntegrityException", "namespace/key", "namespace",
                "durable authority",
            ),
        )
        val MARKER_PATTERN: Regex = Regex("<!-- multi-key-lease:([a-z-]+) -->")
        val MARKER_WITH_HEADING_PATTERN: Regex = Regex(
            "<!-- multi-key-lease:([a-z-]+) -->\\s*\\n(#{3,4}) [^\\n]+",
        )
    }
}
