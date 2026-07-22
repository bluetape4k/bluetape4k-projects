package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.Serializable
import java.time.Duration

class HttpIdempotencyValuesTest {

    @Test
    fun `request preserves ordered key multiplicity and deep copies values`() {
        val keys = mutableListOf("key-1", "key-2")
        val request = request(idempotencyKeys = keys)

        keys.clear()

        request.idempotencyKeys shouldBeEqualTo listOf("key-1", "key-2")
        request shouldBeEqualTo request.copy()
        request.hashCode() shouldBeEqualTo request.copy().hashCode()
    }

    @Test
    fun `request accepts intrinsically bounded malformed ingress fixtures`() {
        listOf("\u0001", " ", "\t", "é").forEach { key ->
            request(idempotencyKeys = listOf(key)).idempotencyKeys shouldBeEqualTo listOf(key)
        }
    }

    @Test
    fun `response canonicalizes and deep copies nested headers`() {
        val values = mutableListOf("v1")
        val headers = mutableMapOf("ETag" to values)
        val response = response(headers = headers)

        values += "v2"
        headers.clear()

        response.headers shouldBeEqualTo mapOf("etag" to listOf("v1"))
        response shouldBeEqualTo response.copy()
        response.hashCode() shouldBeEqualTo response.copy().hashCode()
    }

    @Test
    fun `public collection views are immutable and redact every rendering path`() {
        val request = request(idempotencyKeys = listOf("key-secret"))
        val response = response(
            body = "response-secret",
            headers = mapOf("Authorization-Secret" to listOf("Bearer header-secret")),
        )
        val config = config(replayHeaderAllowlist = setOf("Cookie-Secret"))

        listOf(
            request.toString(),
            request.idempotencyKeys.toString(),
            response.toString(),
            response.headers.toString(),
            response.headers.keys.toString(),
            response.headers.values.toString(),
            response.headers.entries.toString(),
            response.headers.entries.first().toString(),
            response.headers.values.first().toString(),
            config.toString(),
            config.replayHeaderAllowlist.toString(),
        ).forEach { rendering ->
            SENSITIVE_SENTINELS.forEach { sentinel -> rendering shouldNotContain sentinel }
        }

        assertFailsWith<ClassCastException> {
            @Suppress("UNCHECKED_CAST")
            (request.idempotencyKeys as MutableList<String>).add("mutation")
        }
        assertFailsWith<ClassCastException> {
            @Suppress("UNCHECKED_CAST")
            (response.headers as MutableMap<String, List<String>>)["x"] = listOf("mutation")
        }
        assertFailsWith<ClassCastException> {
            @Suppress("UNCHECKED_CAST")
            (config.replayHeaderAllowlist as MutableSet<String>).add("mutation")
        }
        val keyIterator = response.headers.keys.iterator()
        keyIterator.next()
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (keyIterator as MutableIterator<String>).remove()
        }
    }

    @Test
    fun `assertion diagnostics do not reveal caller controlled values`() {
        val failure = assertFailsWith<AssertionError> {
            request(idempotencyKeys = listOf("key-secret"), requestBody = "request-secret") shouldBeEqualTo
                    request(idempotencyKeys = listOf("other-secret"), requestBody = "other-body-secret")
        }

        throwableText(failure).also { text ->
            SENSITIVE_SENTINELS.forEach { sentinel -> text shouldNotContain sentinel }
        }
    }

    @Test
    fun `request intrinsic byte boundaries are exact`() {
        request(authenticationProfile = "가".repeat(170) + "aa")
        assertFailsWith<IllegalArgumentException> {
            request(authenticationProfile = "가".repeat(171))
        }
        request(operation = "x".repeat(1_024), resourceIdentity = "x".repeat(1_024))
        assertFailsWith<IllegalArgumentException> { request(operation = "x".repeat(1_025)) }
        assertFailsWith<IllegalArgumentException> { request(resourceIdentity = "x".repeat(1_025)) }
        request(idempotencyKeys = listOf("x".repeat(8_192)))
        assertFailsWith<IllegalArgumentException> { request(idempotencyKeys = listOf("x".repeat(8_193))) }
        request(requestBody = "x".repeat(16_777_216))
        assertFailsWith<IllegalArgumentException> { request(requestBody = "x".repeat(16_777_217)) }
        assertFailsWith<IllegalArgumentException> { request(idempotencyKeys = emptyList()) }
        assertFailsWith<IllegalArgumentException> { request(idempotencyKeys = listOf("a", "b", "c")) }
        assertFailsWith<IllegalArgumentException> { request(authenticationProfile = " ") }
        assertFailsWith<IllegalArgumentException> { request(operation = " ") }
        assertFailsWith<IllegalArgumentException> { request(resourceIdentity = " ") }
        assertFailsWith<IllegalArgumentException> { request(requestBody = "\uD800") }
    }

    @Test
    fun `response validates status problem grammar and header boundaries`() {
        response(statusCode = 100)
        response(statusCode = 599)
        assertFailsWith<IllegalArgumentException> { response(statusCode = 99) }
        assertFailsWith<IllegalArgumentException> { response(statusCode = 600) }
        response(problemCode = "idempotency_key_reused")
        assertFailsWith<IllegalArgumentException> { response(problemCode = "Not-Snake") }
        assertFailsWith<IllegalArgumentException> {
            response(headers = mapOf("ETag" to listOf("a"), "etag" to listOf("b")))
        }
        assertFailsWith<IllegalArgumentException> { response(headers = mapOf("bad header" to listOf("a"))) }
        response(headers = (1..100).associate { "x-$it" to listOf("v") })
        assertFailsWith<IllegalArgumentException> {
            response(headers = (1..101).associate { "x-$it" to listOf("v") })
        }
        response(headers = mapOf("x" to List(100) { "v" }))
        assertFailsWith<IllegalArgumentException> { response(headers = mapOf("x" to emptyList())) }
        assertFailsWith<IllegalArgumentException> { response(headers = mapOf("x" to List(101) { "v" })) }
        response(headers = mapOf("x" to listOf("v".repeat(65_536))))
        assertFailsWith<IllegalArgumentException> {
            response(headers = mapOf("x" to listOf("v".repeat(65_537))))
        }
        response(headers = mapOf("x" to List(15) { "v".repeat(65_536) } + "v".repeat(65_535)))
        assertFailsWith<IllegalArgumentException> {
            response(headers = mapOf("x" to List(16) { "v".repeat(65_536) }))
        }
        assertFailsWith<IllegalArgumentException> { response(headers = mapOf("x" to listOf("\uD800"))) }
    }

    @Test
    fun `response body byte boundary is exact`() {
        response(body = "x".repeat(16_777_216))
        assertFailsWith<IllegalArgumentException> { response(body = "x".repeat(16_777_217)) }
    }

    @Test
    fun `config accepts exact limits and rejects values outside them`() {
        config(waitTimeout = Duration.ofNanos(1))
        config(waitTimeout = Duration.ofSeconds(60))
        assertFailsWith<IllegalArgumentException> { config(waitTimeout = Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { config(waitTimeout = Duration.ofSeconds(60).plusNanos(1)) }
        assertFailsWith<IllegalArgumentException> { config(waitTimeout = Duration.ofSeconds(Long.MAX_VALUE)) }
        config(scenarioTimeout = Duration.ofSeconds(1))
        config(scenarioTimeout = Duration.ofSeconds(60))
        assertFailsWith<IllegalArgumentException> { config(scenarioTimeout = Duration.ofNanos(999_999_999)) }
        assertFailsWith<IllegalArgumentException> { config(scenarioTimeout = Duration.ofSeconds(61)) }
        assertFailsWith<IllegalArgumentException> { config(scenarioTimeout = Duration.ofSeconds(Long.MAX_VALUE)) }
        config(retention = Duration.ofNanos(1))
        config(retention = Duration.ofDays(365))
        assertFailsWith<IllegalArgumentException> { config(retention = Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> { config(retention = Duration.ofDays(365).plusNanos(1)) }
        assertFailsWith<IllegalArgumentException> { config(retention = Duration.ofSeconds(Long.MAX_VALUE)) }

        assertIntBoundaries(1, 10_000) { config(maxWaitersPerKey = it) }
        assertIntBoundaries(1, 8_191) { config(maxIdempotencyKeyBytes = it) }
        assertIntBoundaries(1, 16_777_215) { config(maxRequestBodyBytes = it) }
        assertIntBoundaries(1, 16_777_216) { config(maxReplayBodyBytes = it) }
        assertIntBoundaries(0, 100) { config(maxReplayHeaderNames = it) }
        assertIntBoundaries(1, 100) { config(maxReplayValuesPerHeader = it) }
        assertIntBoundaries(1, 65_536) { config(maxReplayHeaderValueBytes = it) }
        assertIntBoundaries(1, 1_048_576) { config(maxReplayHeaderBytes = it) }

        config(inFlightRetryAfter = Duration.ofSeconds(1), overflowRetryAfter = Duration.ofSeconds(86_400))
        listOf(
            Duration.ofSeconds(-1),
            Duration.ZERO,
            Duration.ofMillis(1),
            Duration.ofSeconds(86_401),
            Duration.ofSeconds(Long.MAX_VALUE),
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { config(inFlightRetryAfter = invalid) }
            assertFailsWith<IllegalArgumentException> { config(overflowRetryAfter = invalid) }
        }
    }

    @Test
    fun `config canonicalizes validates and deep copies allowlist`() {
        val allowlist = linkedSetOf("ETag", "X-Trace")
        val value = config(replayHeaderAllowlist = allowlist)
        allowlist.clear()

        value.replayHeaderAllowlist shouldBeEqualTo setOf("etag", "x-trace")
        value shouldBeEqualTo value.copy()
        value.hashCode() shouldBeEqualTo value.copy().hashCode()
        assertFailsWith<IllegalArgumentException> { config(replayHeaderAllowlist = setOf("bad header")) }
        assertFailsWith<IllegalArgumentException> { config(replayHeaderAllowlist = setOf("ETag", "etag")) }
        config(replayHeaderAllowlist = (1..100).mapTo(linkedSetOf()) { "x-$it" })
        assertFailsWith<IllegalArgumentException> {
            config(replayHeaderAllowlist = (1..101).mapTo(linkedSetOf()) { "x-$it" })
        }
    }

    @Test
    fun `quiescence rejects negative resources and has value semantics`() {
        val value = HttpIdempotencyQuiescence(0, 1, 2)
        value shouldBeEqualTo HttpIdempotencyQuiescence(0, 1, 2)
        value.hashCode() shouldBeEqualTo HttpIdempotencyQuiescence(0, 1, 2).hashCode()
        assertFailsWith<IllegalArgumentException> { HttpIdempotencyQuiescence(-1, 0, 0) }
        assertFailsWith<IllegalArgumentException> { HttpIdempotencyQuiescence(0, -1, 0) }
        assertFailsWith<IllegalArgumentException> { HttpIdempotencyQuiescence(0, 0, -1) }
    }

    @Test
    fun `all public values survive Java serialization and pin serial version one`() {
        val values = listOf<Serializable>(request(), response(), config(), HttpIdempotencyQuiescence(0, 0, 0))
        values.forEach { value -> javaRoundTrip(value) shouldBeEqualTo value }
        values.map { it.javaClass }.forEach { type ->
            ObjectStreamClass.lookup(type).serialVersionUID shouldBeEqualTo 1L
        }
    }

    @Test
    fun `deserialization rejects crafted invalid public values without secrets`() {
        val invalidValues = listOf(
            corrupt(request(idempotencyKeys = listOf("key-secret")), "idempotencyKeys", emptyList<String>()),
            corrupt(response(body = "response-secret"), "statusCode", 99),
            corrupt(config(replayHeaderAllowlist = setOf("Cookie-Secret")), "maxWaitersPerKey", 0),
            corrupt(HttpIdempotencyQuiescence(0, 0, 0), "activeWaiters", -1),
        )

        invalidValues.forEach { value ->
            val failure = assertFailsWith<InvalidObjectException> { javaRoundTrip(value) }
            throwableText(failure).also { text ->
                SENSITIVE_SENTINELS.forEach { sentinel -> text shouldNotContain sentinel }
            }
        }
    }

    private fun assertIntBoundaries(min: Int, max: Int, factory: (Int) -> Any) {
        factory(min)
        factory(max)
        if (min > Int.MIN_VALUE) assertFailsWith<IllegalArgumentException> { factory(min - 1) }
        if (max < Int.MAX_VALUE) assertFailsWith<IllegalArgumentException> { factory(max + 1) }
    }

    private fun request(
        authenticationProfile: String = "tenant-secret-principal",
        operation: String = "create-command",
        resourceIdentity: String = "resource-secret",
        idempotencyKeys: List<String> = listOf("key-secret"),
        requestBody: String = "request-secret",
    ) = HttpIdempotencyRequest(
        authenticationProfile = authenticationProfile,
        operation = operation,
        resourceIdentity = resourceIdentity,
        idempotencyKeys = idempotencyKeys,
        requestBody = requestBody,
    )

    private fun response(
        statusCode: Int = 201,
        body: String = "response-secret",
        headers: Map<String, List<String>> = mapOf("content-type" to listOf("application/json")),
        problemCode: String? = null,
    ) = HttpIdempotencyResponse(
        statusCode = statusCode,
        body = body,
        headers = headers,
        problemCode = problemCode,
    )

    private fun config(
        waitTimeout: Duration = Duration.ofSeconds(2),
        scenarioTimeout: Duration = Duration.ofSeconds(15),
        maxWaitersPerKey: Int = 2,
        retention: Duration = Duration.ofHours(1),
        inFlightRetryAfter: Duration = Duration.ofSeconds(1),
        overflowRetryAfter: Duration = Duration.ofSeconds(2),
        maxIdempotencyKeyBytes: Int = 255,
        maxRequestBodyBytes: Int = 64 * 1024,
        maxReplayBodyBytes: Int = 64 * 1024,
        maxReplayHeaderNames: Int = 8,
        maxReplayValuesPerHeader: Int = 4,
        maxReplayHeaderValueBytes: Int = 4 * 1024,
        maxReplayHeaderBytes: Int = 16 * 1024,
        replayHeaderAllowlist: Set<String> = emptySet(),
    ) = BoundedWaitHttpIdempotencyConformanceConfig(
        waitTimeout = waitTimeout,
        scenarioTimeout = scenarioTimeout,
        maxWaitersPerKey = maxWaitersPerKey,
        retention = retention,
        inFlightRetryAfter = inFlightRetryAfter,
        overflowRetryAfter = overflowRetryAfter,
        maxIdempotencyKeyBytes = maxIdempotencyKeyBytes,
        maxRequestBodyBytes = maxRequestBodyBytes,
        maxReplayBodyBytes = maxReplayBodyBytes,
        maxReplayHeaderNames = maxReplayHeaderNames,
        maxReplayValuesPerHeader = maxReplayValuesPerHeader,
        maxReplayHeaderValueBytes = maxReplayHeaderValueBytes,
        maxReplayHeaderBytes = maxReplayHeaderBytes,
        replayHeaderAllowlist = replayHeaderAllowlist,
    )

    private fun corrupt(value: Serializable, fieldName: String, replacement: Any): Serializable =
        value.apply {
            javaClass.getDeclaredField(fieldName).apply { isAccessible = true }.set(this, replacement)
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T: Serializable> javaRoundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(value) }
            output.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
    }

    private fun throwableText(failure: Throwable): String = buildString {
        val visited = mutableSetOf<Throwable>()
        fun appendFailure(current: Throwable?) {
            if (current == null || !visited.add(current)) return
            append(current.message.orEmpty())
            current.suppressed.forEach(::appendFailure)
            appendFailure(current.cause)
        }
        appendFailure(failure)
    }

    companion object {
        private val SENSITIVE_SENTINELS = listOf(
            "tenant-secret-principal",
            "resource-secret",
            "key-secret",
            "request-secret",
            "response-secret",
            "Authorization-Secret",
            "Cookie-Secret",
            "header-secret",
            "other-secret",
            "other-body-secret",
        )
    }
}
