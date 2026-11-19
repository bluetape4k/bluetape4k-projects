package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.resilience4j.SuspendDecorators
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

internal class FencingLeaseDocumentationTest {

    @Test
    fun `documented fencing examples remain executable`() = runSuspendIO {
        val english = readModuleFile("README.md")
        val resilienceSource = codeBlock(section(english, "resilience"), "kotlin")
        val sqlSource = codeBlock(section(english, "downstream-guard"), "sql")
        val diagnosticSource = codeBlock(section(english, "diagnostics"), "lua")
        diagnosticSource shouldBeEqualTo LettuceFencingLeaseRecoveryTest.FENCING_DIAGNOSTIC_LUA

        assertExecutableResilienceExample(resilienceSource)
        assertDownstreamGuardExample(sqlSource)
        assertDiagnosticExample(diagnosticSource)
    }

    private suspend fun assertExecutableResilienceExample(resilienceSource: String) {
        val documentedConfig = documentedResilienceConfig(resilienceSource)
        val attempts = AtomicInteger()
        val retry = Retry.of(
            "fencing-documentation",
            RetryConfig.custom<FencingAcquireResult>()
                .maxAttempts(documentedConfig.maxAttempts)
                .waitDuration(Duration.ofMillis(documentedConfig.retryWaitMillis))
                .retryOnResult { it is FencingAcquireResult.BackendFailure }
                .retryOnException { false }
                .build(),
        )
        val circuitBreaker = CircuitBreaker.of(
            "fencing-documentation",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(documentedConfig.slidingWindowSize)
                .minimumNumberOfCalls(documentedConfig.minimumNumberOfCalls)
                .failureRateThreshold(documentedConfig.failureRateThreshold)
                .recordResult { it is FencingAcquireResult.BackendFailure }
                .ignoreException { true }
                .build(),
        )
        val bulkhead = Bulkhead.of(
            "fencing-documentation",
            BulkheadConfig.custom()
                .maxConcurrentCalls(documentedConfig.maxConcurrentCalls)
                .maxWaitDuration(Duration.ofMillis(documentedConfig.maxWaitMillis))
                .build(),
        )

        val result = SuspendDecorators.ofSupplier {
            if (attempts.incrementAndGet() == 1) BACKEND_FAILURE else ACQUIRED
        }
            .withRetry(retry)
            .withCircuitBreaker(circuitBreaker)
            .withBulkhead(bulkhead)
            .invoke()

        result shouldBeEqualTo ACQUIRED
        attempts.get() shouldBeEqualTo 2
    }

    private fun documentedResilienceConfig(source: String): DocumentedResilienceConfig =
        DocumentedResilienceConfig(
            maxAttempts = source.intArgument(".maxAttempts("),
            retryWaitMillis = source.longArgument(".waitDuration(Duration.ofMillis("),
            slidingWindowSize = source.intArgument(".slidingWindowSize("),
            minimumNumberOfCalls = source.intArgument(".minimumNumberOfCalls("),
            failureRateThreshold = source.floatArgument(".failureRateThreshold("),
            maxConcurrentCalls = source.intArgument(".maxConcurrentCalls("),
            maxWaitMillis = source.longArgument(".maxWaitDuration(Duration.ofMillis("),
        )

    private fun String.intArgument(prefix: String): Int = argument(prefix, "[0-9]+").toInt()

    private fun String.longArgument(prefix: String): Long = argument(prefix, "[0-9]+").toLong()

    private fun String.floatArgument(prefix: String): Float =
        argument(prefix, "[0-9]+(?:\\.[0-9]+)?F").removeSuffix("F").toFloat()

    private fun String.argument(prefix: String, valuePattern: String): String =
        requireNotNull(
            Regex("${Regex.escape(prefix)}($valuePattern)").find(this)
        ) { "Missing documented resilience call: $prefix" }
            .groupValues[1]

    private fun assertDownstreamGuardExample(sqlSource: String) {
        sqlSource shouldContain "NOT NULL DEFAULT 0"
        sqlSource shouldContain "(fence_epoch, fence_sequence) < (:epoch, :sequence)"

        val store = GuardedStore()
        store.update("invoice-42", FencingToken(9, 1), "payment-1") shouldBeEqualTo 1
        store.update("invoice-42", FencingToken(9, 1), "payment-2") shouldBeEqualTo 0
        store.update("invoice-42", FencingToken(8, Long.MAX_VALUE), "payment-3") shouldBeEqualTo 0
        store.update("invoice-42", FencingToken(9, 2), "payment-1") shouldBeEqualTo 1
        store.row("invoice-42") shouldBeEqualTo GuardedRow(FencingToken(9, 2), "payment-1")
    }

    private fun assertDiagnosticExample(diagnosticSource: String) {
        diagnosticSource.indexOf("redis.call('STRLEN', KEYS[2])") shouldBeLessThan
                diagnosticSource.indexOf("redis.call('GET', KEYS[2])")
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val tag = LettuceTestUtils.randomName().substringAfterLast(':')
            val config = LettuceFencingLeaseConfig("documentation", tag, 81)
            val keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
            val commands = connection.sync()
            try {
                commands.set(keys.counter, "9".repeat(20))
                commands.evalReadOnly<String>(
                    diagnosticSource,
                    ScriptOutputType.MULTI,
                    arrayOf(keys.lease, keys.counter),
                    config.epoch.toString(),
                ) shouldBeEqualTo listOf("COUNTER_INVALID", "0")

                commands.set(keys.counter, "1")
                commands.hset(
                    keys.lease,
                    mapOf("owner" to "owner", "epoch" to "81", "sequence" to "1"),
                )
                commands.evalReadOnly<String>(
                    diagnosticSource,
                    ScriptOutputType.MULTI,
                    arrayOf(keys.lease, keys.counter),
                    config.epoch.toString(),
                ) shouldBeEqualTo listOf("LEASE_NO_TTL", "1")
            } finally {
                commands.del(keys.lease, keys.counter)
            }
        }
    }

    private fun section(readme: String, marker: String): String {
        val start = readme.indexOf("<!-- fencing-lease:$marker -->")
        val next = readme.indexOf("<!-- fencing-lease:", start + 1)
        return readme.substring(start, if (next >= 0) next else readme.length)
    }

    private fun codeBlock(section: String, language: String): String {
        val opening = "```$language"
        val start = section.indexOf(opening) + opening.length
        val end = section.indexOf("```", start)
        return section.substring(start, end).trim()
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

    private data class GuardedRow(
        val token: FencingToken,
        val businessIdempotencyKey: String,
    )

    private class GuardedStore {
        private val rows = mutableMapOf<String, GuardedRow>()

        fun update(resourceId: String, token: FencingToken, businessIdempotencyKey: String): Int {
            val current = rows[resourceId]
            if (current != null && token <= current.token) return 0
            rows[resourceId] = GuardedRow(token, businessIdempotencyKey)
            return 1
        }

        fun row(resourceId: String): GuardedRow? = rows[resourceId]
    }

    private data class DocumentedResilienceConfig(
        val maxAttempts: Int,
        val retryWaitMillis: Long,
        val slidingWindowSize: Int,
        val minimumNumberOfCalls: Int,
        val failureRateThreshold: Float,
        val maxConcurrentCalls: Int,
        val maxWaitMillis: Long,
    )

    private companion object: KLogging() {
        val ACQUIRED = FencingAcquireResult.Acquired(FencingToken(1, 1))
        val BACKEND_FAILURE = FencingAcquireResult.BackendFailure(
            FencingLeaseBackendFailure(FencingBackendFailureKind.CONNECTION),
        )
    }
}
