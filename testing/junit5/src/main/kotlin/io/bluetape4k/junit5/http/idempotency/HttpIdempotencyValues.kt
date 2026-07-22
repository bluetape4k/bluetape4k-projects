package io.bluetape4k.junit5.http.idempotency

import java.io.InvalidObjectException
import java.io.ObjectStreamException
import java.io.Serializable
import java.nio.CharBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.Duration
import java.util.Collections
import java.util.TreeSet

/**
 * Synthetic HTTP command used by the bounded-wait conformance runner.
 *
 * [authenticationProfile] selects adapter-owned authentication state rather than accepting a
 * caller-trusted tenant. [idempotencyKeys] preserves one normal or deliberately malformed ingress
 * value, or two ordered values for the duplicate-header negative scenario. Diagnostic rendering
 * always redacts authentication state, resource identity, keys, and body.
 */
class HttpIdempotencyRequest(
    val authenticationProfile: String,
    val operation: String,
    val resourceIdentity: String,
    idempotencyKeys: List<String>,
    val requestBody: String,
): Serializable {

    val idempotencyKeys: List<String> = RedactedImmutableList(idempotencyKeys)

    init {
        require(authenticationProfile.isNotBlank()) { "authenticationProfile must not be blank." }
        requireBoundedUtf8(authenticationProfile, MAX_AUTHENTICATION_PROFILE_BYTES, "authenticationProfile")
        require(operation.isNotBlank()) { "operation must not be blank." }
        requireBoundedUtf8(operation, MAX_OPERATION_BYTES, "operation")
        require(resourceIdentity.isNotBlank()) { "resourceIdentity must not be blank." }
        requireBoundedUtf8(resourceIdentity, MAX_RESOURCE_IDENTITY_BYTES, "resourceIdentity")
        require(this.idempotencyKeys.size in 1..2) { "idempotencyKeys must contain one or two values." }
        this.idempotencyKeys.forEach { key ->
            requireBoundedUtf8(key, MAX_IDEMPOTENCY_KEY_BYTES, "idempotencyKey")
        }
        requireBoundedUtf8(requestBody, MAX_BODY_BYTES, "requestBody")
    }

    /** Returns a validated immutable copy while preserving ordered key multiplicity. */
    fun copy(
        authenticationProfile: String = this.authenticationProfile,
        operation: String = this.operation,
        resourceIdentity: String = this.resourceIdentity,
        idempotencyKeys: List<String> = this.idempotencyKeys,
        requestBody: String = this.requestBody,
    ): HttpIdempotencyRequest = HttpIdempotencyRequest(
        authenticationProfile = authenticationProfile,
        operation = operation,
        resourceIdentity = resourceIdentity,
        idempotencyKeys = idempotencyKeys,
        requestBody = requestBody,
    )

    override fun equals(other: Any?): Boolean =
        other is HttpIdempotencyRequest &&
                authenticationProfile == other.authenticationProfile &&
                operation == other.operation &&
                resourceIdentity == other.resourceIdentity &&
                idempotencyKeys == other.idempotencyKeys &&
                requestBody == other.requestBody

    override fun hashCode(): Int {
        var result = authenticationProfile.hashCode()
        result = 31 * result + operation.hashCode()
        result = 31 * result + resourceIdentity.hashCode()
        result = 31 * result + idempotencyKeys.hashCode()
        return 31 * result + requestBody.hashCode()
    }

    override fun toString(): String =
        "HttpIdempotencyRequest(authenticationProfile=<redacted>, operation=$operation, " +
                "resourceIdentity=<redacted>, idempotencyKeys=<redacted>, requestBody=<redacted>)"

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = restoreSerializedValue("HttpIdempotencyRequest") { copy() }

    companion object {
        private const val MAX_AUTHENTICATION_PROFILE_BYTES = 512
        private const val MAX_OPERATION_BYTES = 1_024
        private const val MAX_RESOURCE_IDENTITY_BYTES = 1_024
        private const val MAX_IDEMPOTENCY_KEY_BYTES = 8_192
        private const val MAX_BODY_BYTES = 16_777_216
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Immutable HTTP response snapshot observed by the conformance runner.
 *
 * Header names are normalized and nested values are defensively copied. Diagnostic rendering
 * redacts the body and every header name and value.
 */
class HttpIdempotencyResponse(
    val statusCode: Int,
    val body: String,
    headers: Map<String, List<String>>,
    val problemCode: String? = null,
): Serializable {

    val headers: Map<String, List<String>> = canonicalHeaders(headers)

    init {
        require(statusCode in 100..599) { "statusCode must be in range 100..599." }
        requireBoundedUtf8(body, MAX_BODY_BYTES, "body")
        require(problemCode == null || problemCode.matches(LOWER_SNAKE_CASE)) {
            "problemCode must be lower snake case."
        }
    }

    /** Returns a validated immutable copy with defensively copied headers. */
    fun copy(
        statusCode: Int = this.statusCode,
        body: String = this.body,
        headers: Map<String, List<String>> = this.headers,
        problemCode: String? = this.problemCode,
    ): HttpIdempotencyResponse = HttpIdempotencyResponse(
        statusCode = statusCode,
        body = body,
        headers = headers,
        problemCode = problemCode,
    )

    override fun equals(other: Any?): Boolean =
        other is HttpIdempotencyResponse &&
                statusCode == other.statusCode &&
                body == other.body &&
                headers == other.headers &&
                problemCode == other.problemCode

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + body.hashCode()
        result = 31 * result + headers.hashCode()
        return 31 * result + (problemCode?.hashCode() ?: 0)
    }

    override fun toString(): String =
        "HttpIdempotencyResponse(statusCode=$statusCode, body=<redacted>, " +
                "headers=<redacted>, problemCode=$problemCode)"

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = restoreSerializedValue("HttpIdempotencyResponse") { copy() }

    companion object {
        private const val MAX_BODY_BYTES = 16_777_216
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Instance-scoped limits and retry hints for the bounded-wait conformance profile.
 *
 * These values configure a synthetic test application and runner. They are not production
 * defaults or a substitute for application capacity planning. Header allowlist rendering is
 * always redacted.
 */
class BoundedWaitHttpIdempotencyConformanceConfig(
    val waitTimeout: Duration,
    val scenarioTimeout: Duration,
    val maxWaitersPerKey: Int,
    val retention: Duration,
    val inFlightRetryAfter: Duration,
    val overflowRetryAfter: Duration,
    val maxIdempotencyKeyBytes: Int,
    val maxRequestBodyBytes: Int,
    val maxReplayBodyBytes: Int,
    val maxReplayHeaderNames: Int,
    val maxReplayValuesPerHeader: Int,
    val maxReplayHeaderValueBytes: Int,
    val maxReplayHeaderBytes: Int,
    replayHeaderAllowlist: Set<String> = emptySet(),
): Serializable {

    val replayHeaderAllowlist: Set<String> = canonicalHeaderNames(replayHeaderAllowlist)

    init {
        require(waitTimeout > Duration.ZERO && waitTimeout <= MAX_WAIT_TIMEOUT) {
            "waitTimeout must be positive and at most 60 seconds."
        }
        require(scenarioTimeout in MIN_SCENARIO_TIMEOUT..MAX_SCENARIO_TIMEOUT) {
            "scenarioTimeout must be between 1 and 60 seconds."
        }
        require(retention > Duration.ZERO && retention <= MAX_RETENTION) {
            "retention must be positive and at most 365 days."
        }
        requireInRange(maxWaitersPerKey, 1, 10_000, "maxWaitersPerKey")
        requireInRange(maxIdempotencyKeyBytes, 1, 8_191, "maxIdempotencyKeyBytes")
        requireInRange(maxRequestBodyBytes, 1, 16_777_215, "maxRequestBodyBytes")
        requireInRange(maxReplayBodyBytes, 1, 16_777_216, "maxReplayBodyBytes")
        requireInRange(maxReplayHeaderNames, 0, 100, "maxReplayHeaderNames")
        requireInRange(maxReplayValuesPerHeader, 1, 100, "maxReplayValuesPerHeader")
        requireInRange(maxReplayHeaderValueBytes, 1, 65_536, "maxReplayHeaderValueBytes")
        requireInRange(maxReplayHeaderBytes, 1, 1_048_576, "maxReplayHeaderBytes")
        require(inFlightRetryAfter.isPositiveWholeSecond()) {
            "inFlightRetryAfter must be a positive whole number of seconds up to 86400."
        }
        require(overflowRetryAfter.isPositiveWholeSecond()) {
            "overflowRetryAfter must be a positive whole number of seconds up to 86400."
        }
    }

    /** Returns a validated immutable copy of this instance-scoped test configuration. */
    fun copy(
        waitTimeout: Duration = this.waitTimeout,
        scenarioTimeout: Duration = this.scenarioTimeout,
        maxWaitersPerKey: Int = this.maxWaitersPerKey,
        retention: Duration = this.retention,
        inFlightRetryAfter: Duration = this.inFlightRetryAfter,
        overflowRetryAfter: Duration = this.overflowRetryAfter,
        maxIdempotencyKeyBytes: Int = this.maxIdempotencyKeyBytes,
        maxRequestBodyBytes: Int = this.maxRequestBodyBytes,
        maxReplayBodyBytes: Int = this.maxReplayBodyBytes,
        maxReplayHeaderNames: Int = this.maxReplayHeaderNames,
        maxReplayValuesPerHeader: Int = this.maxReplayValuesPerHeader,
        maxReplayHeaderValueBytes: Int = this.maxReplayHeaderValueBytes,
        maxReplayHeaderBytes: Int = this.maxReplayHeaderBytes,
        replayHeaderAllowlist: Set<String> = this.replayHeaderAllowlist,
    ): BoundedWaitHttpIdempotencyConformanceConfig = BoundedWaitHttpIdempotencyConformanceConfig(
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

    override fun equals(other: Any?): Boolean =
        other is BoundedWaitHttpIdempotencyConformanceConfig &&
                waitTimeout == other.waitTimeout &&
                scenarioTimeout == other.scenarioTimeout &&
                maxWaitersPerKey == other.maxWaitersPerKey &&
                retention == other.retention &&
                inFlightRetryAfter == other.inFlightRetryAfter &&
                overflowRetryAfter == other.overflowRetryAfter &&
                maxIdempotencyKeyBytes == other.maxIdempotencyKeyBytes &&
                maxRequestBodyBytes == other.maxRequestBodyBytes &&
                maxReplayBodyBytes == other.maxReplayBodyBytes &&
                maxReplayHeaderNames == other.maxReplayHeaderNames &&
                maxReplayValuesPerHeader == other.maxReplayValuesPerHeader &&
                maxReplayHeaderValueBytes == other.maxReplayHeaderValueBytes &&
                maxReplayHeaderBytes == other.maxReplayHeaderBytes &&
                replayHeaderAllowlist == other.replayHeaderAllowlist

    override fun hashCode(): Int {
        var result = waitTimeout.hashCode()
        result = 31 * result + scenarioTimeout.hashCode()
        result = 31 * result + maxWaitersPerKey
        result = 31 * result + retention.hashCode()
        result = 31 * result + inFlightRetryAfter.hashCode()
        result = 31 * result + overflowRetryAfter.hashCode()
        result = 31 * result + maxIdempotencyKeyBytes
        result = 31 * result + maxRequestBodyBytes
        result = 31 * result + maxReplayBodyBytes
        result = 31 * result + maxReplayHeaderNames
        result = 31 * result + maxReplayValuesPerHeader
        result = 31 * result + maxReplayHeaderValueBytes
        result = 31 * result + maxReplayHeaderBytes
        return 31 * result + replayHeaderAllowlist.hashCode()
    }

    override fun toString(): String =
        "BoundedWaitHttpIdempotencyConformanceConfig(" +
                "waitTimeout=$waitTimeout, scenarioTimeout=$scenarioTimeout, " +
                "maxWaitersPerKey=$maxWaitersPerKey, retention=$retention, " +
                "inFlightRetryAfter=$inFlightRetryAfter, overflowRetryAfter=$overflowRetryAfter, " +
                "maxIdempotencyKeyBytes=$maxIdempotencyKeyBytes, " +
                "maxRequestBodyBytes=$maxRequestBodyBytes, maxReplayBodyBytes=$maxReplayBodyBytes, " +
                "maxReplayHeaderNames=$maxReplayHeaderNames, " +
                "maxReplayValuesPerHeader=$maxReplayValuesPerHeader, " +
                "maxReplayHeaderValueBytes=$maxReplayHeaderValueBytes, " +
                "maxReplayHeaderBytes=$maxReplayHeaderBytes, replayHeaderAllowlist=<redacted>)"

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any =
        restoreSerializedValue("BoundedWaitHttpIdempotencyConformanceConfig") { copy() }

    companion object {
        private val MAX_WAIT_TIMEOUT: Duration = Duration.ofSeconds(60)
        private val MIN_SCENARIO_TIMEOUT: Duration = Duration.ofSeconds(1)
        private val MAX_SCENARIO_TIMEOUT: Duration = Duration.ofSeconds(60)
        private val MAX_RETENTION: Duration = Duration.ofDays(365)
        private const val serialVersionUID: Long = 1L
    }
}

/** Reports adapter-owned waiters, gates, and child tasks that must be zero after each scenario. */
data class HttpIdempotencyQuiescence(
    val activeWaiters: Int,
    val openGates: Int,
    val activeChildTasks: Int,
): Serializable {

    init {
        require(activeWaiters >= 0) { "activeWaiters must not be negative." }
        require(openGates >= 0) { "openGates must not be negative." }
        require(activeChildTasks >= 0) { "activeChildTasks must not be negative." }
    }

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = restoreSerializedValue("HttpIdempotencyQuiescence") { copy() }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

@JvmSynthetic
internal fun requireBoundedUtf8(value: String, maxBytes: Int, field: String): Int {
    val bytes = try {
        Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(CharBuffer.wrap(value))
            .remaining()
    } catch (_: CharacterCodingException) {
        throw IllegalArgumentException("$field must be valid UTF-8.")
    }
    require(bytes <= maxBytes) { "$field exceeds its byte limit." }
    return bytes
}

private fun requireInRange(value: Int, minimum: Int, maximum: Int, field: String) {
    require(value in minimum..maximum) { "$field must be in range $minimum..$maximum." }
}

private fun canonicalHeaders(source: Map<String, List<String>>): Map<String, List<String>> {
    require(source.size <= 100) { "headers contain too many names." }
    val canonical = linkedMapOf<String, List<String>>()
    var aggregateBytes = 0L
    source.entries.sortedBy { it.key.lowercase() }.forEach { (name, values) ->
        require(name.isHttpToken()) { "headers contain an invalid name." }
        val normalizedName = name.lowercase()
        require(normalizedName !in canonical) { "headers contain duplicate names." }
        require(values.size in 1..100) { "headers contain an invalid value count." }
        aggregateBytes = addHeaderBytes(
            aggregateBytes,
            normalizedName.toByteArray(Charsets.UTF_8).size,
        )
        val copiedValues = values.map { value ->
            aggregateBytes = addHeaderBytes(
                aggregateBytes,
                requireBoundedUtf8(value, 65_536, "headerValue"),
            )
            value
        }
        canonical[normalizedName] = RedactedImmutableList(copiedValues)
    }
    return RedactedImmutableMap(canonical)
}

private fun addHeaderBytes(current: Long, additional: Int): Long =
    (current + additional).also { total ->
        require(total <= 1_048_576L) { "headers exceed the aggregate byte limit." }
    }

private fun canonicalHeaderNames(source: Set<String>): Set<String> {
    require(source.size <= 100) { "replayHeaderAllowlist contains too many names." }
    val normalized = TreeSet<String>()
    source.sortedBy(String::lowercase).forEach { name ->
        require(name.isHttpToken()) { "replayHeaderAllowlist contains an invalid name." }
        require(normalized.add(name.lowercase())) { "replayHeaderAllowlist contains duplicate names." }
    }
    return RedactedImmutableSet(normalized)
}

private fun Duration.isPositiveWholeSecond(): Boolean =
    !isZero && !isNegative && nano == 0 && seconds in 1L..86_400L

private fun String.isHttpToken(): Boolean =
    isNotEmpty() && all { character ->
        character in '0'..'9' || character in 'A'..'Z' || character in 'a'..'z' ||
                character in HTTP_TOKEN_PUNCTUATION
    }

private inline fun <T> restoreSerializedValue(typeName: String, factory: () -> T): T =
    try {
        factory()
    } catch (_: Exception) {
        throw InvalidObjectException("Invalid serialized $typeName.")
    }

private class RedactedImmutableList<E>(source: Collection<E>): AbstractList<E>(), Serializable {
    private val content: List<E> = Collections.unmodifiableList(ArrayList(source))

    override val size: Int get() = content.size
    override fun get(index: Int): E = content[index]
    override fun subList(fromIndex: Int, toIndex: Int): List<E> =
        RedactedImmutableList(content.subList(fromIndex, toIndex))
    override fun reversed(): List<E> = RedactedImmutableList(content.reversed())
    override fun equals(other: Any?): Boolean = content == other
    override fun hashCode(): Int = content.hashCode()
    override fun toString(): String = REDACTED

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private class RedactedImmutableSet<E>(source: Collection<E>): AbstractSet<E>(), Serializable {
    private val content: Set<E> = Collections.unmodifiableSet(LinkedHashSet(source))

    override val size: Int get() = content.size
    override fun iterator(): Iterator<E> = content.iterator()
    override fun contains(element: E): Boolean = content.contains(element)
    override fun equals(other: Any?): Boolean = content == other
    override fun hashCode(): Int = content.hashCode()
    override fun toString(): String = REDACTED

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private class RedactedImmutableMap<K, V>(source: Map<K, V>): AbstractMap<K, V>(), Serializable {
    private val content: Map<K, V> = Collections.unmodifiableMap(LinkedHashMap(source))
    private val redactedEntries: Set<Map.Entry<K, V>> =
        RedactedImmutableSet(content.entries.map { RedactedEntry(it.key, it.value) })
    private val redactedKeys: Set<K> = RedactedImmutableSet(content.keys)
    private val redactedValues: Collection<V> = RedactedImmutableList(content.values)

    override val entries: Set<Map.Entry<K, V>> get() = redactedEntries
    override val keys: Set<K> get() = redactedKeys
    override val values: Collection<V> get() = redactedValues
    override fun get(key: K): V? = content[key]
    override fun containsKey(key: K): Boolean = content.containsKey(key)
    override fun containsValue(value: V): Boolean = content.containsValue(value)
    override fun equals(other: Any?): Boolean = content == other
    override fun hashCode(): Int = content.hashCode()
    override fun toString(): String = REDACTED

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private class RedactedEntry<K, V>(
    override val key: K,
    override val value: V,
): Map.Entry<K, V>, Serializable {

    override fun equals(other: Any?): Boolean =
        other is Map.Entry<*, *> && key == other.key && value == other.value

    override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
    override fun toString(): String = REDACTED

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private val HTTP_TOKEN_PUNCTUATION: Set<Char> =
    charArrayOf('!', '#', '$', '%', '&', '\'', '*', '+', '-', '.', '^', '_', '`', '|', '~').toSet()
private val LOWER_SNAKE_CASE = Regex("[a-z][a-z0-9]*(?:_[a-z0-9]+)*")
private const val REDACTED = "<redacted>"
