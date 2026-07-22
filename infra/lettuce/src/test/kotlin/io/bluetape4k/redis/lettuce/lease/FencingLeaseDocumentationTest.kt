package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldContentEqual
import io.bluetape4k.junit5.coroutines.runSuspendIO
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
    fun `README locales preserve the fencing safety contract and executable examples`() = runSuspendIO {
        val english = readModuleFile("README.md")
        val korean = readModuleFile("README.ko.md")

        markerOrder(english) shouldContentEqual REQUIRED_MARKERS
        markerOrder(korean) shouldContentEqual REQUIRED_MARKERS
        headingLevels(english) shouldContentEqual REQUIRED_HEADING_LEVELS
        headingLevels(korean) shouldContentEqual REQUIRED_HEADING_LEVELS
        REQUIRED_POLICY_FRAGMENTS.forEach { fragment ->
            english shouldContain fragment
            korean shouldContain fragment
        }
        REQUIRED_SECTION_CONTRACTS.forEach { (marker, orderedTerms) ->
            assertOrderedTerms(section(english, marker), orderedTerms)
            assertOrderedTerms(section(korean, marker), orderedTerms)
        }

        val resilienceSource = codeBlock(section(english, "resilience"), "kotlin")
        resilienceSource shouldBeEqualTo codeBlock(section(korean, "resilience"), "kotlin")
        val sqlSource = codeBlock(section(english, "downstream-guard"), "sql")
        sqlSource shouldBeEqualTo codeBlock(section(korean, "downstream-guard"), "sql")
        val diagnosticSource = codeBlock(section(english, "diagnostics"), "lua")
        diagnosticSource shouldBeEqualTo codeBlock(section(korean, "diagnostics"), "lua")
        diagnosticSource shouldBeEqualTo LettuceFencingLeaseRecoveryTest.FENCING_DIAGNOSTIC_LUA

        assertExecutableResilienceExample(resilienceSource)
        assertDownstreamGuardExample(sqlSource)
        assertDiagnosticExample(diagnosticSource)
    }

    @Test
    fun `public fencing API has English KDoc for caller owned safety boundaries`() {
        val sourceByFile = KDocSource.entries.associateWith { readModuleFile(it.relativePath) }
        sourceByFile.values.forEach { source ->
            val declarations = PUBLIC_DECLARATION_PATTERN.findAll(source).toList()
            declarations.isNotEmpty().shouldBeTrue()
            declarations.forEach { declaration ->
                assertKDocImmediatelyBefore(source, declaration.range.first)
            }
            declarations.asSequence()
                .filter { it.value.trimStart().removePrefix("public ").startsWith("data class") }
                .forEach { declaration ->
                    assertDataClassPropertiesDocumented(source, declaration.range.first)
                }
        }

        val combined = sourceByFile.values.joinToString("\n")
        REQUIRED_KDOC_FRAGMENTS.forEach(combined::shouldContain)
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
        requireNotNull(Regex("${Regex.escape(prefix)}($valuePattern)").find(this)) {
            "Missing documented resilience call: $prefix"
        }.groupValues[1]

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
            val tag = LettuceTestUtils.randomName().substringAfter(':')
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

    private fun markerOrder(readme: String): List<String> = MARKER_PATTERN.findAll(readme)
        .map { it.groupValues[1] }
        .toList()

    private fun headingLevels(readme: String): List<Int> = MARKER_WITH_HEADING_PATTERN.findAll(readme)
        .map { it.groupValues[2].length }
        .toList()

    private fun section(readme: String, marker: String): String {
        val start = readme.indexOf("<!-- fencing-lease:$marker -->").shouldBeGreaterOrEqualTo(0)
        val next = readme.indexOf("<!-- fencing-lease:", start + 1)
        return readme.substring(start, if (next >= 0) next else readme.length)
    }

    private fun codeBlock(section: String, language: String): String {
        val opening = "```$language"
        val start = section.indexOf(opening).shouldBeGreaterOrEqualTo(0) + opening.length
        val end = section.indexOf("```", start).shouldBeGreaterOrEqualTo(start)
        return section.substring(start, end).trim()
    }

    private fun assertOrderedTerms(section: String, orderedTerms: List<String>) {
        var cursor = 0
        orderedTerms.forEach { term ->
            val next = section.indexOf(term, cursor, ignoreCase = true)
            next shouldBeGreaterOrEqualTo cursor
            cursor = next + term.length
        }
    }

    private fun assertKDocImmediatelyBefore(source: String, declarationIndex: Int) {
        val prefix = source.substring(0, declarationIndex).trimEnd()
        prefix.endsWith("*/").shouldBeTrue()
        val kdocStart = prefix.lastIndexOf("/**").shouldBeGreaterOrEqualTo(0)
        prefix.lastIndexOf("/*") shouldBeEqualTo kdocStart
    }

    private fun assertDataClassPropertiesDocumented(source: String, declarationIndex: Int) {
        val openingParenthesis = source.indexOf('(', declarationIndex).shouldBeGreaterOrEqualTo(declarationIndex)
        val closingParenthesis = matchingParenthesis(source, openingParenthesis)
        val declarationHeader = source.substring(openingParenthesis + 1, closingParenthesis)
        val kdoc = kdocImmediatelyBefore(source, declarationIndex)
        PROPERTY_PATTERN.findAll(declarationHeader).forEach { property ->
            kdoc shouldContain "@property ${property.groupValues[1]}"
        }
    }

    private fun matchingParenthesis(source: String, openingIndex: Int): Int {
        var depth = 0
        source.forEachIndexed { index, character ->
            if (index < openingIndex) return@forEachIndexed
            when (character) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        error("Unclosed data-class primary constructor at offset $openingIndex")
    }

    private fun kdocImmediatelyBefore(source: String, declarationIndex: Int): String {
        val prefix = source.substring(0, declarationIndex).trimEnd()
        val start = prefix.lastIndexOf("/**").shouldBeGreaterOrEqualTo(0)
        return prefix.substring(start)
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

    private enum class KDocSource(val relativePath: String) {
        VALUE("src/main/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseValue.kt"),
        RESULT("src/main/kotlin/io/bluetape4k/redis/lettuce/lease/FencingLeaseResult.kt"),
        BLOCKING("src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceFencingLease.kt"),
        SUSPENDING("src/main/kotlin/io/bluetape4k/redis/lettuce/lease/LettuceSuspendFencingLease.kt"),
    }

    private companion object {
        val REQUIRED_MARKERS = listOf(
            "basic",
            "downstream-guard",
            "resilience",
            "recovery",
            "diagnostics",
            "caller-actions",
            "security-telemetry",
            "limitations",
        )
        val REQUIRED_HEADING_LEVELS = listOf(3, 4, 4, 4, 4, 4, 4, 4)
        val REQUIRED_POLICY_FRAGMENTS = listOf(
            "LettuceFencingLeaseConfig",
            "namespace",
            "resourceName",
            "epoch",
            "bootstrap",
            "CounterUnavailable",
            "(epoch, sequence)",
            "affectedRows == 1",
            ".withRetry(retry)",
            ".withCircuitBreaker(circuitBreaker)",
            ".withBulkhead(bulkhead)",
            "BackendFailure",
            "pause",
            "drain",
            "CAS",
            "readiness",
            "rollout",
            "resume",
            "EVAL_RO",
            "LEASE_NO_TTL",
            "operation",
            "result",
            "kind",
            "namespace/resource/owner/token/fingerprint",
            "exactly-once",
            "business idempotency",
            "durable correctness",
        )
        val REQUIRED_SECTION_CONTRACTS = mapOf(
            "resilience" to listOf(
                ".maxAttempts(", ".waitDuration(", ".retryOnResult", ".retryOnException",
                ".slidingWindowSize(", ".minimumNumberOfCalls(", ".failureRateThreshold(",
                ".recordResult", ".ignoreException", ".maxConcurrentCalls(", ".maxWaitDuration(",
                ".withRetry(retry)", ".withCircuitBreaker(circuitBreaker)", ".withBulkhead(bulkhead)",
            ),
            "caller-actions" to listOf(
                "Initialized", "AlreadyInitialized", "Acquired", "AlreadyOwned", "Owned", "Renewed", "Released",
                "acquire", "Contended", "inspect", "Contended", "Lost", "OwnershipMismatch", "CounterUnavailable",
                "SequenceExhausted", "IntegrityFailure", "BackendFailure",
            ),
            "recovery" to listOf(
                "pause", "block old acquire", "drain", "CAS", "bootstrap", "readiness", "rollout",
                "confirm old absence", "resume",
            ),
        )
        val REQUIRED_KDOC_FRAGMENTS = listOf(
            "durable external authority",
            "ambiguous",
            "same owner ID",
            "stable resource identity",
            "not strictly greater",
            "Redis Cluster",
            "ordering, not durable business correctness",
            "must not be lowered",
        )
        val MARKER_PATTERN = Regex("<!-- fencing-lease:([a-z-]+) -->")
        val MARKER_WITH_HEADING_PATTERN = Regex(
            "<!-- fencing-lease:([a-z-]+) -->\\s*\\n(#{3,4}) [^\\n]+",
        )
        val PUBLIC_DECLARATION_PATTERN = Regex(
            """(?m)^[ \t]*(?!(?:private|internal|protected)\b)""" +
                """(?:public\s+)?(?:(?:data\s+)?(?:class|object)\s+\w+|enum\s+class\s+\w+|""" +
                """sealed\s+interface\s+\w+|companion\s+object|constructor\s*\(|""" +
                """(?:override\s+)?(?:suspend\s+)?fun\s+\w+\s*\()""",
        )
        val PROPERTY_PATTERN = Regex("""\bval\s+(\w+)\s*:""")
        val ACQUIRED = FencingAcquireResult.Acquired(FencingToken(1, 1))
        val BACKEND_FAILURE = FencingAcquireResult.BackendFailure(
            FencingLeaseBackendFailure(FencingBackendFailureKind.CONNECTION),
        )
    }
}
