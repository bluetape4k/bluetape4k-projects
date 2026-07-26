# Issue #1055 HTTP Idempotency Conformance Implementation Plan

> **For agentic
workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-junit5`에 bounded-wait HTTP idempotency 공통 runner를 추가하고 Ktor `testApplication`과 Spring MockMvc가 같은 17개 black-box scenario를 통과하게 한다.

**Architecture:** 공용 module은 serializable value, framework-neutral adapter contract, deterministic scenario runner만 제공한다. Ktor와 Spring test source는 같은 config를 HTTP ingress와 test control에 연결하는 독립 in-memory reference application을 가지며, persistence/store/filter/plugin/telemetry API는 추가하지 않는다. Runner는 caller structured scope에서 실행하고 자체 monotonic watchdog scheduler만 `finally`에서 닫는다.

**Tech
Stack:** Kotlin 2.3, Java 21, JUnit 5, kotlinx-coroutines, bluetape4k-assertions, Ktor `testApplication`, Spring MockMvc.

---

## 파일 구조와 변경 책임

| 파일                                                                                                                                                   | 책임                                                                                | write scope                    |
|--------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|--------------------------------|
| `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyValues.kt`                                                        | serializable request/response/config/quiescence value와 intrinsic/config validation | public values only             |
| `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/BoundedWaitHttpIdempotencyAdapter.kt`                                            | framework-neutral suspend adapter contract                                          | public interface only          |
| `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/BoundedWaitHttpIdempotencyConformance.kt`                                        | public runner, real-time watchdog, scenario orchestration과 cleanup                 | public entrypoint + lifecycle  |
| `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyScenarioFixtures.kt`                                              | internal deterministic requests/responses와 safe assertion helpers                  | internal fixtures              |
| `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyTerminalScenarios.kt`                                             | first/replay/conflict/tenant/auth/failure terminal scenario                         | internal scenarios             |
| `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyInFlightScenarios.kt`                                             | wait/deadline/overflow/cancellation/abandon/disconnect scenario                     | internal concurrency scenarios |
| `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyBoundaryScenarios.kt`                                             | expiry, malformed/oversized, replay header/body, repeated fan-in scenario           | internal boundary scenarios    |
| `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/*Test.kt`                                                                        | value, redaction, serialization, watchdog, scenario regression tests                | shared fixture tests           |
| `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/InMemoryBoundedWaitHttpIdempotencyAdapter.kt`                                    | runner test double; production export 금지                                          | shared tests only              |
| `ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/idempotency/KtorHttpIdempotencyConformanceTest.kt`                                            | 실제 Ktor HTTP exchange와 독립 fake application proof                               | Ktor tests only                |
| `spring-boot/core/src/test/kotlin/io/bluetape4k/spring/idempotency/SpringHttpIdempotencyConformanceTest.kt`                                            | 실제 MockMvc exchange와 독립 fake application proof                                 | Spring tests only              |
| `testing/junit5/README.md`, `testing/junit5/README.ko.md`                                                                                              | locale-parity module usage와 결과표                                                 | module docs                    |
| `docs/manual/en/modules/bluetape4k-junit5/http-idempotency-conformance.md`, `docs/manual/ko/modules/bluetape4k-junit5/http-idempotency-conformance.md` | 정책 선택, 운영, caller, durable proof boundary                                     | manual chapters                |
| `docs/manual/en/modules/bluetape4k-junit5.md`, `docs/manual/ko/modules/bluetape4k-junit5.md`                                                           | 새 chapter/source/test link                                                         | manual landing                 |
| `CHANGELOG.md`                                                                                                                                         | `1.12.0` additive fixture와 rollback/proof boundary                                 | release note                   |

`testing/junit5/build.gradle.kts`, `ktor/testing/build.gradle.kts`, `spring-boot/core/build.gradle.kts`는 현재 필요한 coroutine, JUnit, Ktor test host, Spring test와 `bluetape4k-junit5` edge를 이미 가진다. dependency 변경은 계획하지 않는다. Ktor/Spring fake engine의 중복은 의도적이다. 이를 shared fake engine으로 추출하면 두 framework가 독립 application behavior를 증명하지 못하므로, 공용으로 재사용하는 것은 value/adapter/runner뿐이다.

## Task 1: Public serializable values와 validation

**Complexity:** Medium **Depends on:** 승인된 spec commits `996776195`, `2c055437c`
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`
**Rollback:** 이 task commit만 revert하면 public API가 생기기 전 상태로 돌아간다.

**Files:**

- Create: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyValues.kt`
- Create: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyValuesTest.kt`

- [ ] **Step 1: constructor, redaction, content equality 실패 test 작성**

```kotlin
@Test
fun `request and response render only redacted metadata`() {
    val request = request(idempotencyKey = "key-secret", requestBody = "body-secret")
    val response = HttpIdempotencyResponse(
        statusCode = 201,
        body = "response-secret",
        headers = mapOf("authorization" to listOf("Bearer header-secret")),
    )

    request.toString() shouldNotContain "key-secret"
    request.toString() shouldNotContain "body-secret"
    response.toString() shouldNotContain "response-secret"
    response.toString() shouldNotContain "header-secret"
}

@Test
fun `response deep copies nested header values`() {
    val values = mutableListOf("v1")
    val headers = mutableMapOf("ETag" to values)
    val response = HttpIdempotencyResponse(200, "ok", headers)

    values += "v2"
    headers.clear()

    response.headers shouldBeEqualTo mapOf("etag" to listOf("v1"))
}

@Test
fun `request deep copies duplicate header values`() {
    val keys = mutableListOf("key-1", "key-2")
    val request = HttpIdempotencyRequest("tenant-a", "create", "resource-1", keys, "{}")
    keys.clear()
    request.idempotencyKeys shouldBeEqualTo listOf("key-1", "key-2")
}
```

- [ ] **Step 2: targeted test가 unresolved symbol로 실패하는지 확인**

Run: `./gradlew :bluetape4k-junit5:test --tests '*HttpIdempotencyValuesTest' --no-configuration-cache`
Expected: `HttpIdempotencyRequest`, `HttpIdempotencyResponse` unresolved로 FAIL.

- [ ] **Step 3: value class 최소 구현 작성**

```kotlin
class HttpIdempotencyRequest(
    val authenticationProfile: String,
    val operation: String,
    val resourceIdentity: String,
    idempotencyKeys: List<String>,
    val requestBody: String,
): Serializable {
    val idempotencyKeys: List<String> = RedactedImmutableList(idempotencyKeys)

    init {
        requireBoundedUtf8(authenticationProfile, 512, "authenticationProfile", requireNonBlank = true)
        requireBoundedUtf8(operation, 1_024, "operation", requireNonBlank = true)
        requireBoundedUtf8(resourceIdentity, 1_024, "resourceIdentity", requireNonBlank = true)
        require(this.idempotencyKeys.size in 1..2) { "idempotencyKeys must contain one or two values." }
        this.idempotencyKeys.forEach { key ->
            requireBoundedUtf8(key, 8_192, "idempotencyKey")
        }
        requireBoundedUtf8(requestBody, 16_777_216, "requestBody")
    }

    override fun toString(): String =
        "HttpIdempotencyRequest(authenticationProfile=<redacted>, operation=$operation, " +
                "resourceIdentity=<redacted>, idempotencyKeys=<redacted>, requestBody=<redacted>)"

    /** Returns a validated immutable copy while preserving ordered key multiplicity. */
    fun copy(
        authenticationProfile: String = this.authenticationProfile,
        operation: String = this.operation,
        resourceIdentity: String = this.resourceIdentity,
        idempotencyKeys: List<String> = this.idempotencyKeys,
        requestBody: String = this.requestBody,
    ) = HttpIdempotencyRequest(
        authenticationProfile = authenticationProfile,
        operation = operation,
        resourceIdentity = resourceIdentity,
        idempotencyKeys = idempotencyKeys,
        requestBody = requestBody,
    )

    override fun equals(other: Any?): Boolean =
        other is HttpIdempotencyRequest && authenticationProfile == other.authenticationProfile &&
                operation == other.operation && resourceIdentity == other.resourceIdentity &&
                idempotencyKeys == other.idempotencyKeys && requestBody == other.requestBody

    override fun hashCode(): Int {
        var result = authenticationProfile.hashCode()
        result = 31 * result + operation.hashCode()
        result = 31 * result + resourceIdentity.hashCode()
        result = 31 * result + idempotencyKeys.hashCode()
        return 31 * result + requestBody.hashCode()
    }

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = restoreSerializedValue("HttpIdempotencyRequest") {
        copy()
    }

    companion object { private const val serialVersionUID: Long = 1L }
}

class HttpIdempotencyResponse(
    val statusCode: Int,
    val body: String,
    headers: Map<String, List<String>>,
    val problemCode: String? = null,
): Serializable {
    val headers: Map<String, List<String>> = canonicalHeaders(headers)

    init {
        require(statusCode in 100..599) { "statusCode must be in 100..599." }
        requireBoundedUtf8(body, 16_777_216, "body")
        require(problemCode == null || problemCode.matches(LOWER_SNAKE_CASE)) {
            "problemCode must be lower snake case."
        }
    }

    override fun equals(other: Any?): Boolean =
        other is HttpIdempotencyResponse && statusCode == other.statusCode && body == other.body &&
                headers == other.headers && problemCode == other.problemCode

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + body.hashCode()
        result = 31 * result + headers.hashCode()
        return 31 * result + (problemCode?.hashCode() ?: 0)
    }
    override fun toString(): String =
        "HttpIdempotencyResponse(statusCode=$statusCode, body=<redacted>, headers=<redacted>, problemCode=$problemCode)"

    /** Returns a validated immutable copy with defensively copied headers. */
    fun copy(
        statusCode: Int = this.statusCode,
        body: String = this.body,
        headers: Map<String, List<String>> = this.headers,
        problemCode: String? = this.problemCode,
    ) = HttpIdempotencyResponse(
        statusCode = statusCode,
        body = body,
        headers = headers,
        problemCode = problemCode,
    )

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = restoreSerializedValue("HttpIdempotencyResponse") { copy() }

    companion object { private const val serialVersionUID: Long = 1L }
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
        aggregateBytes += normalizedName.toByteArray(Charsets.UTF_8).size
        val copiedValues = values.map { value ->
            aggregateBytes += requireBoundedUtf8(value, 65_536, "headerValue")
            value
        }
        canonical[normalizedName] = RedactedImmutableList(copiedValues)
    }
    require(aggregateBytes <= 1_048_576L) { "headers exceed the aggregate byte limit." }
    return RedactedImmutableMap(canonical)
}

internal fun requireBoundedUtf8(
    value: String,
    maxBytes: Int,
    field: String,
    requireNonBlank: Boolean = false,
): Int {
    require(!requireNonBlank || value.isNotBlank()) { "$field must not be blank." }
    val bytes = try {
        Charsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .encode(CharBuffer.wrap(value))
            .remaining()
    } catch (_: CharacterCodingException) {
        throw IllegalArgumentException("$field must be valid UTF-8.")
    }
    require(bytes <= maxBytes) { "$field exceeds its intrinsic byte limit." }
    return bytes
}

private fun String.isHttpToken(): Boolean =
    isNotEmpty() && all { it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' || it in HTTP_TOKEN_PUNCTUATION }

private val HTTP_TOKEN_PUNCTUATION =
    charArrayOf('!', '#', '$', '%', '&', '\'', '*', '+', '-', '.', '^', '_', '`', '|', '~').toSet()
private val LOWER_SNAKE_CASE = Regex("[a-z][a-z0-9]*(?:_[a-z0-9]+)*")

private inline fun <T> restoreSerializedValue(typeName: String, factory: () -> T): T =
    try {
        factory()
    } catch (_: Exception) {
        throw InvalidObjectException("Invalid serialized $typeName.")
    }
```

- [ ] **Step 4: config/quiescence validation과 Java round-trip test 작성**

```kotlin
@Test
fun `config rejects invalid duration and capacity bounds`() {
    assertFailsWith<IllegalArgumentException> { config(waitTimeout = Duration.ZERO) }
    assertFailsWith<IllegalArgumentException> { config(maxWaitersPerKey = 0) }
    assertFailsWith<IllegalArgumentException> { config(maxRequestBodyBytes = 16_777_216) }
}

@Test
fun `constructors reject intrinsic byte grammar and canonical header collisions`() {
    assertFailsWith<IllegalArgumentException> { request(idempotencyKey = "x".repeat(8_193)) }
    assertFailsWith<IllegalArgumentException> {
        HttpIdempotencyResponse(200, "ok", mapOf("ETag" to listOf("a"), "etag" to listOf("b")))
    }
    assertFailsWith<IllegalArgumentException> {
        config(replayHeaderAllowlist = setOf("ETag", "etag"))
    }
}

@Test
fun `request can carry intrinsically bounded malformed fixtures`() {
    request(idempotencyKey = "\u0001").idempotencyKeys shouldBeEqualTo listOf("\u0001")
    request(idempotencyKey = " ").idempotencyKeys shouldBeEqualTo listOf(" ")
    request(idempotencyKey = "\t").idempotencyKeys shouldBeEqualTo listOf("\t")
    request(idempotencyKey = "é").idempotencyKeys shouldBeEqualTo listOf("é")
}

@Test
fun `all public values survive Java serialization`() {
    javaRoundTrip(request()) shouldBeEqualTo request()
    javaRoundTrip(response()) shouldBeEqualTo response()
    javaRoundTrip(config()) shouldBeEqualTo config()
    javaRoundTrip(HttpIdempotencyQuiescence(0, 0, 0)) shouldBeEqualTo
            HttpIdempotencyQuiescence(0, 0, 0)
}

@Test
fun `all public values pin serial version one`() {
    listOf(
        HttpIdempotencyRequest::class.java,
        HttpIdempotencyResponse::class.java,
        BoundedWaitHttpIdempotencyConformanceConfig::class.java,
        HttpIdempotencyQuiescence::class.java,
    ).forEach { type ->
        ObjectStreamClass.lookup(type).serialVersionUID shouldBeEqualTo 1L
    }
}

private fun <T: Serializable> javaRoundTrip(value: T): T {
    val bytes = ByteArrayOutputStream().use { output ->
        ObjectOutputStream(output).use { it.writeObject(value) }
        output.toByteArray()
    }
    @Suppress("UNCHECKED_CAST")
    return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
}

private fun request(
    authenticationProfile: String = "tenant-a-writer",
    idempotencyKey: String = "key-1",
    idempotencyKeys: List<String> = listOf(idempotencyKey),
    requestBody: String = "{}",
) = HttpIdempotencyRequest(
    authenticationProfile = authenticationProfile,
    operation = "create-command",
    resourceIdentity = "resource-1",
    idempotencyKeys = idempotencyKeys,
    requestBody = requestBody,
)

private fun response() = HttpIdempotencyResponse(
    statusCode = 201,
    body = "{\"status\":\"created\"}",
    headers = mapOf("content-type" to listOf("application/json")),
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
```

- [ ] **Step 5: config/quiescence, canonical header와 readResolve 구현**

```kotlin
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
        require(waitTimeout > Duration.ZERO && waitTimeout <= Duration.ofSeconds(60))
        require(scenarioTimeout in Duration.ofSeconds(1)..Duration.ofSeconds(60))
        require(retention > Duration.ZERO && retention <= Duration.ofDays(365))
        require(maxWaitersPerKey in 1..10_000)
        require(maxIdempotencyKeyBytes in 1..8_191)
        require(maxRequestBodyBytes in 1..16_777_215)
        require(maxReplayBodyBytes in 1..16_777_216)
        require(maxReplayHeaderNames in 0..100)
        require(maxReplayValuesPerHeader in 1..100)
        require(maxReplayHeaderValueBytes in 1..65_536)
        require(maxReplayHeaderBytes in 1..1_048_576)
        require(inFlightRetryAfter.isPositiveWholeSecond())
        require(overflowRetryAfter.isPositiveWholeSecond())
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

    override fun equals(other: Any?): Boolean =
        other is BoundedWaitHttpIdempotencyConformanceConfig &&
                waitTimeout == other.waitTimeout && scenarioTimeout == other.scenarioTimeout &&
                maxWaitersPerKey == other.maxWaitersPerKey && retention == other.retention &&
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

    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any = restoreSerializedValue("BoundedWaitHttpIdempotencyConformanceConfig") {
        copy()
    }

    companion object { private const val serialVersionUID: Long = 1L }
}

private fun Duration.isPositiveWholeSecond(): Boolean =
    !isZero && !isNegative && nano == 0 && seconds in 1L..86_400L

private fun canonicalHeaderNames(source: Set<String>): Set<String> {
    require(source.size <= 100) { "replayHeaderAllowlist contains too many names." }
    val normalized = source.map { name ->
        require(name.isHttpToken()) { "replayHeaderAllowlist contains an invalid name." }
        name.lowercase()
    }
    require(normalized.distinct().size == source.size) {
        "replayHeaderAllowlist contains duplicate names."
    }
    return RedactedImmutableSet(TreeSet(normalized))
}

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

    companion object { private const val serialVersionUID: Long = 1L }
}
```

`HttpIdempotencyValuesTest`는 reflection으로 invariant를 깨뜨린 request/response/config/quiescence를 serialize한 뒤 deserialize하여, 각 `readResolve`가 raw field/value/cause를 포함하지 않는
`InvalidObjectException`으로 거절하는 crafted-state case도 포함한다. redaction test는 `toString`, assertion failure message, serialization failure message와 nested header collection rendering 네 경로에서 각기 다른 sentinel가 나타나지 않음을 확인한다. parameterized sentinel set은 authentication profile의 tenant/principal, resource identity, 각 key, request/ response body, mixed-case `Authorization`/`Cookie` header name과 value를 포함한다. throwable 검사는 top-level message뿐 아니라 cause와 suppressed chain을 순회해 각 sentinel에 `shouldNotContain`을 적용한다.

같은 file의 private `RedactedImmutableList`, `RedactedImmutableSet`, `RedactedImmutableMap`과 redacted
`Map.Entry` view는 source를 먼저 immutable copy하고 `List`/`Set`/`Map` content equality·hash·iteration/ lookup·Java serialization은 유지하되 모든 `toString()`을 `<redacted>`로 반환한다. map의 `keys`, `values`,
`entries` view와 `entries.first()`도 각각 redacted wrapper를 사용한다. 각 wrapper는 `Serializable`과 private
`serialVersionUID = 1L`을 갖고 mutation method를 노출하지 않는다. request keys, response header map와 nested value list, config allowlist가 이 wrapper를 사용한다. direct tests는 object, property collection, map keys/values/entries, single entry와 nested list rendering, Java mutable cast mutation 실패, round-trip 뒤 content equality를 모두 검증한다.

RED/GREEN parameterized matrix는 identity `512/513`, operation/resource `1,024/1,025`, key
`8,192/8,193`, body `16,777,216/16,777,217`, response status `99/100/599/600`, lower-snake problem code, header name count/value count/individual/aggregate byte ceiling의 exact/max+1을 포함한다. Config는 각 numeric bound의 min-1/min/max/max+1, duration `1ns`, maximum, overflow conversion, Retry-After fractional/zero/86,400/86,401초, allowlist invalid token과 case-collision을 검증한다.

- [ ] **Step 6: 모든 public value에 English KDoc 추가**

```kotlin
/**
 * Synthetic HTTP command used by the bounded-wait conformance runner.
 *
 * [authenticationProfile] selects adapter-owned authentication state; it is not a caller-
 * trusted tenant header. [idempotencyKeys] carries one normal or deliberately malformed ingress
 * value, or two values only for the duplicate-header negative scenario. Rendering always redacts
 * keys, identities, and body.
 */

/**
 * Immutable HTTP response snapshot observed by the conformance runner.
 *
 * Header names are normalized and nested values are defensively copied. Rendering redacts
 * the body and every header name and value.
 */

/**
 * Instance-scoped limits and retry hints for the bounded-wait conformance profile.
 *
 * These values configure the test application and runner; they are not production defaults.
 */

/** Reports adapter-owned resources that must be quiescent after each scenario. */
```

- [ ] **Step 7: values test와 detekt 실행**

Run: `./gradlew :bluetape4k-junit5:test --tests '*HttpIdempotencyValuesTest' :bluetape4k-junit5:detekt :bluetape4k-junit5:detektTest --no-configuration-cache`
Expected: value tests PASS, detekt 0 violations. Assertion은 `assertFailsWith`, `shouldBeEqualTo`, `shouldNotContain`을 사용하고 Boolean wrapper를 남기지 않는다.

- [ ] **Step 8: Lore commit**

```bash
git add testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyValues.kt \
  testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyValuesTest.kt
git commit -m "Keep idempotency test values bounded and safe to serialize" \
  -m "Constraint: Public value objects must remain framework neutral and redact caller-controlled data" \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: HttpIdempotencyValuesTest and junit5 detekt passed"
```

## Task 2: Adapter contract, watchdog와 runner lifecycle

**Complexity:** High **Depends on:** Task 1 **Pattern
skills:** `bluetape-kotlin-patterns`, `kotlin-coroutines-skill`, `test-driven-development`
**Rollback:** runner commit을 revert해도 Task 1 values는 독립적으로 제거 가능하다.

**Files:**

- Create: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/BoundedWaitHttpIdempotencyAdapter.kt`
- Create: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/BoundedWaitHttpIdempotencyConformance.kt`
- Create: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyScenarioFixtures.kt`
- Create: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/BoundedWaitHttpIdempotencyConformanceLifecycleTest.kt`

- [ ] **Step 1: watchdog timeout/cancellation/cleanup 실패 test 작성**

```kotlin
@Test
fun `stalled adapter fails with redacted scenario and closes watchdog`() = runSuspendIO {
    val adapter = StalledAdapter()
    val error = assertFailsWith<AssertionError> {
        runConformanceScenarios(
            adapter = adapter,
            config = config(scenarioTimeout = Duration.ofSeconds(1)),
            scenarios = listOf(
                ConformanceScenario("first-request") { target, _ ->
                    target.exchange(request(idempotencyKey = "key-secret", requestBody = "body-secret"))
                }
            ),
        )
    }

    error.message.orEmpty() shouldContain "scenario=first-request"
    error.message.orEmpty() shouldNotContain "key-secret"
    adapter.cancelledCount shouldBeEqualTo 1
    adapter.resetCount shouldBeEqualTo 1
}

@Test
fun `runner rejects fan-in workload above the shared proof budget before exchange`() = runSuspendIO {
    val adapter = CountingAdapter()

    assertFailsWith<IllegalArgumentException> {
        runConformanceScenarios(
            adapter = adapter,
            config = config(maxWaitersPerKey = 33),
            scenarios = listOf(
                ConformanceScenario("never-runs") { target, _ ->
                    target.exchange(request())
                },
            ),
        )
    }

    adapter.exchangeCount shouldBeEqualTo 0
}
```

- [ ] **Step 2: lifecycle test가 entrypoint unresolved로 실패하는지 확인**

Run: `./gradlew :bluetape4k-junit5:test --tests '*BoundedWaitHttpIdempotencyConformanceLifecycleTest' --no-configuration-cache`
Expected: adapter/lifecycle runner symbol unresolved로 FAIL.

- [ ] **Step 3: public adapter contract 작성**

```kotlin
interface BoundedWaitHttpIdempotencyAdapter {
    /** Sends one synthetic request through the real framework HTTP boundary. */
    suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse
    /** Suspends until the request owns one business execution. */
    suspend fun awaitOwnerStarted(request: HttpIdempotencyRequest)
    /** Suspends until exactly [expected] same-fingerprint waiters are registered. */
    suspend fun awaitWaiterCount(request: HttpIdempotencyRequest, expected: Int)
    /** Releases the owner with a terminal replayable [outcome]. */
    suspend fun completeOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse)
    /** Arms a post-commit response-delivery hold before [request] starts. */
    suspend fun holdOwnerResponseDelivery(request: HttpIdempotencyRequest)
    /** Releases a response delivery hold; cancellation/reset also reclaim it exactly once. */
    suspend fun releaseOwnerResponseDelivery(request: HttpIdempotencyRequest)
    /** Releases the owner with a transient non-replayable [outcome]. */
    suspend fun abandonOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse)
    /** Advances only the adapter's behavioral virtual clock. */
    suspend fun advanceTimeBy(duration: Duration)
    /** Clears scenario-owned records, waiters, gates, and child work. */
    suspend fun resetScenario()
    /** Returns committed business executions for [request]'s server-resolved scope. */
    fun sideEffectCount(request: HttpIdempotencyRequest): Int
    /** Reports resources that must be zero after scenario cleanup. */
    fun quiescence(): HttpIdempotencyQuiescence
}
```

`holdOwnerResponseDelivery`는 owner 시작 전에 호출한다. 이 control은 terminal commit과 HTTP response delivery 사이의 disconnect를 wall-clock race 없이 재현하며, owner cancellation 또는 scenario reset이 발생하면 adapter가 hold를 정확히 한 번 회수해야 한다.

- [ ] **Step 4: fixture/config preflight, response bounds와 watchdog cleanup 구현**

```kotlin
internal data class ConformanceScenario(
    val name: String,
    val run: suspend (
        BoundedWaitHttpIdempotencyAdapter,
        BoundedWaitHttpIdempotencyConformanceConfig,
    ) -> Unit,
)

internal suspend fun runConformanceScenarios(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
    scenarios: List<ConformanceScenario>,
) {
    validateNormalFixtureCompatibility(config)
    require(config.maxWaitersPerKey <= MAX_CONFORMANCE_WAITERS_PER_KEY) {
        "The conformance runner supports at most $MAX_CONFORMANCE_WAITERS_PER_KEY waiters per key."
    }
    Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread.ofPlatform().daemon().name("http-idempotency-watchdog").unstarted(runnable)
    }.use { watchdog ->
        var runnerFailure: Throwable? = null
        try {
            scenarios.forEach { scenario ->
                runOneScenario(watchdog, adapter, config, scenario)
            }
        } catch (failure: Throwable) {
            runnerFailure = failure
        }

        val finalCleanupFailure = try {
            withContext(NonCancellable) {
                withMonotonicWatchdog(watchdog, config.scenarioTimeout, "final-cleanup") {
                    adapter.resetScenario()
                }
            }
            null
        } catch (failure: Throwable) {
            failure
        }
        val shutdownFailure = try {
            watchdog.shutdownNow()
            check(watchdog.awaitTermination(5, TimeUnit.SECONDS)) {
                "HTTP idempotency watchdog did not terminate."
            }
            null
        } catch (failure: Throwable) {
            failure
        }

        val primary = runnerFailure ?: finalCleanupFailure ?: shutdownFailure
        primary?.let { failure ->
            listOfNotNull(finalCleanupFailure, shutdownFailure)
                .filterNot { it === failure }
                .forEach(failure::addSuppressed)
            throw failure
        }
    }
}

private suspend fun runOneScenario(
    watchdog: ScheduledExecutorService,
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
    scenario: ConformanceScenario,
) {
    var scenarioFailure: Throwable? = null
    try {
        withMonotonicWatchdog(watchdog, config.scenarioTimeout, scenario.name) {
            scenario.run(adapter, config)
        }
    } catch (failure: Throwable) {
        scenarioFailure = failure
    }

    val cleanupFailure = try {
        withContext(NonCancellable) {
            withMonotonicWatchdog(watchdog, config.scenarioTimeout, "${scenario.name}-cleanup") {
                adapter.resetScenario()
                adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
            }
        }
        null
    } catch (failure: Throwable) {
        failure
    }

    scenarioFailure?.let { failure ->
        cleanupFailure?.let(failure::addSuppressed)
        throw failure
    }
    cleanupFailure?.let { throw it }
}

private suspend fun <T> withMonotonicWatchdog(
    watchdog: ScheduledExecutorService,
    timeout: Duration,
    scenario: String,
    block: suspend () -> T,
): T = coroutineScope {
    val work = async(start = CoroutineStart.UNDISPATCHED) { block() }
    val expired = CompletableDeferred<Unit>()
    val timeoutTask = watchdog.schedule(
        { expired.complete(Unit) },
        timeout.toNanos(),
        TimeUnit.NANOSECONDS,
    )
    try {
        select {
            work.onAwait { it }
            expired.onAwait {
                work.cancel(CancellationException("scenario watchdog expired"))
                throw AssertionError("HTTP idempotency conformance timed out; scenario=$scenario; values redacted")
            }
        }
    } finally {
        timeoutTask.cancel(false)
        if (work.isActive) work.cancelAndJoin()
    }
}

internal suspend fun exchangeChecked(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
    request: HttpIdempotencyRequest,
    allowConfiguredRequestOverflow: Boolean = false,
): HttpIdempotencyResponse {
    if (!allowConfiguredRequestOverflow) validateRequestAgainstInstance(request, config)
    return adapter.exchange(request).also { response -> validateReplaySnapshot(response, config) }
}

private fun validateNormalFixtureCompatibility(config: BoundedWaitHttpIdempotencyConformanceConfig) {
    representativeNormalRequests().forEach { validateRequestAgainstInstance(it, config) }
    representativeTerminalResponses().forEach { validateReplaySnapshot(it, config) }
}

private fun validateRequestAgainstInstance(
    request: HttpIdempotencyRequest,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    request.idempotencyKeys.forEach { key ->
        requireBoundedUtf8(key, config.maxIdempotencyKeyBytes, "idempotencyKey")
    }
    requireBoundedUtf8(request.requestBody, config.maxRequestBodyBytes, "requestBody")
}

private fun validateReplaySnapshot(
    response: HttpIdempotencyResponse,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    requireBoundedUtf8(response.body, config.maxReplayBodyBytes, "responseBody")
    val replayHeaders = response.headers.filterKeys { name ->
        name == "content-type" || name in config.replayHeaderAllowlist
    }
    require(replayHeaders.size <= config.maxReplayHeaderNames) { "Replay snapshot has too many header names." }
    var aggregateBytes = 0L
    replayHeaders.forEach { (name, values) ->
        require(values.size <= config.maxReplayValuesPerHeader) { "Replay snapshot has too many values." }
        aggregateBytes += name.toByteArray(Charsets.UTF_8).size
        values.forEach { value ->
            aggregateBytes += requireBoundedUtf8(value, config.maxReplayHeaderValueBytes, "replayHeaderValue")
        }
    }
    require(aggregateBytes <= config.maxReplayHeaderBytes) { "Replay snapshot exceeds aggregate bytes." }
}
```

`MAX_CONFORMANCE_WAITERS_PER_KEY`는 test workload 전용 상수 `32`다. config value 자체의 허용 범위
`1..10_000`은 유지하되 public runner는 adapter를 호출하기 전에 `33` 이상을 거절한다. 이 제한은 production capacity 권고가 아니라 shared Ktor/MockMvc proof가 한 round에 만드는 동시 request와 memory를 bounded하게 유지하기 위한 것이다. lifecycle test는 `32`가 통과하고 `33`은 adapter invocation 0으로 실패하는지 검증한다. production limit이 더 크면 adopter는 runner에 `32` 이하의 대표 test instance를 제공하고 별도 load test에서 실제 상한을 검증한다.

같은 task에서 `HttpIdempotencyScenarioFixtures.kt`에 `request(...)`, `createdResponse()`, transient/ deterministic failure response와 `representativeNormalRequests()`/`representativeTerminalResponses()`를 internal로 추가한다. representative list는 fan-in의 최장 generated key와 모든 정상 body/terminal response shape를 포함하며 negative oversize/malformed input은 제외한다. 따라서 Task 2 runner는 Task 3 이후 artifact에 의존하지 않고 compile된다.

`representativeNormalRequests()`는 fan-in의 최장 generated key를 포함한 모든 정상 fixture shape,
`representativeTerminalResponses()`는 success/deterministic failure/transient response shape를 반환한다. preflight는 adapter 호출 전 stable/redacted `IllegalArgumentException`으로 실패한다. 모든 scenario의 HTTP 호출은 `exchangeChecked`를 사용하고, oversized key/body negative vector만
`allowConfiguredRequestOverflow = true`로 request-side check를 건너뛴다. response snapshot 검증은 항상 실행하며 protocol-only header가 아니라 `content-type`과 configured replay allowlist 후보에 bounds를 적용한다. lifecycle test는 too-small valid config에서 adapter exchange count 0과 oversized response의 redacted bound failure를 직접 검증한다.

- [ ] **Step 5: watchdog success/timeout/reset/cancellation failure matrix 통과**

Run: `./gradlew :bluetape4k-junit5:test --tests '*BoundedWaitHttpIdempotencyConformanceLifecycleTest' --no-configuration-cache`
Expected: 정상, scenario timeout, caller cancellation, reset exception, reset의 `awaitCancellation()`, quiescence mismatch를 각각 실행해 원래 failure와 suppressed cleanup failure가 redacted 상태로 보존된다. 모든 case에서 `http-idempotency-watchdog` live thread와 adapter child job은 0이다. Adapter contract test는
`resetScenario`가 cancellation-cooperative suspend function이어야 하며 blocking I/O를 직접 수행하지 않는다는 전제를 확인한다.

- [ ] **Step 6: public adapter English KDoc에 caller scope와 ownership 추가**

```kotlin
/**
 * Connects a caller-owned HTTP test application to the bounded-wait conformance runner.
 *
 * The runner invokes this adapter in the caller's structured scope. The caller closes the
 * HTTP application and any blocking dispatcher after the runner returns. Implementations
 * must preserve cancellation and never log raw keys, request bodies, responses, or headers.
 */
```

- [ ] **Step 7: Lore commit**

```bash
git add testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency \
  testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/BoundedWaitHttpIdempotencyConformanceLifecycleTest.kt
git commit -m "Bound every idempotency scenario without owning caller coroutines" \
  -m "Constraint: Behavioral virtual time must stay separate from the monotonic watchdog" \
  -m "Rejected: Coroutine withTimeout on the caller dispatcher | a virtual Delay could hang the fail-safe" \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: Conformance lifecycle tests passed"
```

## Task 3: Terminal, scope와 authorization scenarios

**Complexity:** High **Depends on:** Task 2 **Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`
**Rollback:** terminal scenario file와 test double commit을 함께 revert한다.

**Files:**

- Create: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyTerminalScenarios.kt`
- Modify: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyScenarioFixtures.kt`
- Create: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/InMemoryBoundedWaitHttpIdempotencyAdapter.kt`
- Create: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyTerminalScenariosTest.kt`

- [ ] **Step 1: first/replay/conflict/tenant/auth/terminal failure test 작성**

```kotlin
@Test
fun `terminal scenario group preserves one execution and tenant isolation`() = runSuspendIO {
    val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(config())

    runConformanceScenarios(adapter, config(), terminalScenarios())

    adapter.completedScenarioCount shouldBeEqualTo 6
    adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
}
```

- [ ] **Step 2: scenario test가 missing implementation으로 실패하는지 확인**

Run: `./gradlew :bluetape4k-junit5:test --tests '*HttpIdempotencyTerminalScenariosTest' --no-configuration-cache`
Expected: `terminalScenarios`와 test adapter unresolved로 FAIL.

- [ ] **Step 3: test-only independent state machine 최소 구현**

```kotlin
private data class TestRecord(
    val fingerprint: String,
    var state: State,
    var response: HttpIdempotencyResponse? = null,
    var expiresAt: Instant? = null,
)

private sealed interface State {
    data object InFlight: State
    data object Terminal: State
    data object Abandoned: State
}

private sealed interface ExchangeAction {
    data class Owner(val scope: String): ExchangeAction
    data class Waiter(val scope: String, val waiterId: Long): ExchangeAction
    data class Immediate(val response: HttpIdempotencyResponse): ExchangeAction
}

override suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse {
    authenticateAndAuthorize(request)?.let { return it }
    validateIngress(request)?.let { return it }
    val scope = serverResolvedScope(request)
    val fingerprint = digest(request.operation, request.resourceIdentity, request.requestBody)
    val action = mutex.withLock { decideExchange(scope, fingerprint) }
    return when (action) {
        is ExchangeAction.Owner -> runOwner(action.scope)
        is ExchangeAction.Waiter -> awaitOwner(action.scope, action.waiterId)
        is ExchangeAction.Immediate -> action.response
    }
}
```

- [ ] **Step 4: terminal scenario assertions 구현**

```kotlin
internal fun terminalScenarios(): List<ConformanceScenario> = listOf(
    ConformanceScenario("first-request") { adapter, config -> assertFirstRequest(adapter, config) },
    ConformanceScenario("terminal-replay") { adapter, config -> assertFirstAndReplay(adapter, config) },
    ConformanceScenario("payload-conflict") { adapter, config ->
        assertDifferentPayloadConflictIsImmediate(adapter, config)
    },
    ConformanceScenario("tenant-isolation") { adapter, config -> assertCrossTenantIsolation(adapter, config) },
    ConformanceScenario("authorization-before-lookup") { adapter, config ->
        assertUnauthorizedIsRecordIndistinguishable(adapter, config)
    },
    ConformanceScenario("terminal-failure-replay") { adapter, config ->
        assertDeterministicTerminalFailureReplay(adapter, config)
    },
)

private suspend fun assertDifferentPayloadConflictIsImmediate(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    val owner = request(idempotencyKey = "conflict-key", requestBody = "A")
    val conflict = owner.copy(requestBody = "B")
    coroutineScope {
        val ownerJob = async { exchangeChecked(adapter, config, owner) }
        adapter.awaitOwnerStarted(owner)
        exchangeChecked(adapter, config, conflict).problemCode shouldBeEqualTo "idempotency_key_reused"
        adapter.sideEffectCount(owner) shouldBeEqualTo 1
        adapter.completeOwner(owner, createdResponse())
        ownerJob.await().statusCode shouldBeEqualTo 201
        exchangeChecked(adapter, config, owner).headers["idempotency-replayed"] shouldBeEqualTo listOf("true")
    }
}
```

`assertUnauthorizedIsRecordIndistinguishable`는 authorized profile로 foreign record를 먼저 terminal로 만든 뒤, unauthenticated와 read-only unauthorized profile 각각에 대해 record-present key와 absent key를 요청한다. 각 profile의 present/absent response는 status/body/canonical headers/problem code가
`shouldBeEqualTo`이고, unauthorized request의 waiter/side-effect count는 모두 0이어야 한다. 이 assertion은 auth가 lookup보다 먼저라는 observable proof이며 internal lookup counter는 추가하지 않는다.

- [ ] **Step 5: terminal group와 전체 junit5 test 실행**

Run: `./gradlew :bluetape4k-junit5:test --tests '*HttpIdempotencyTerminalScenariosTest' --no-configuration-cache`
Expected: all terminal scenarios PASS; raw sentinel values absent from failure diagnostics.

- [ ] **Step 6: Lore commit**

```bash
git add testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyTerminalScenarios.kt \
  testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency
git commit -m "Prove terminal replay without leaking security scope" \
  -m "Constraint: Authentication and authorization must complete before idempotency lookup" \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: Terminal conformance scenarios passed"
```

## Task 4: In-flight race, cancellation과 abandon scenarios

**Complexity:** High **Depends on:** Task 3 **Pattern
skills:** `bluetape-kotlin-patterns`, `kotlin-coroutines-skill`, `test-driven-development`
**Rollback/rerun:** race failure 시 이 task의 virtual clock/gate test만 반복하고 framework task로 진행하지 않는다.

**Files:**

- Create: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyInFlightScenarios.kt`
- Create: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyInFlightScenariosTest.kt`
- Modify: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/InMemoryBoundedWaitHttpIdempotencyAdapter.kt`

- [ ] **Step 1: deadline exact boundary와 slot recovery 실패 test 작성**

```kotlin
@Test
fun `in-flight scenarios choose one atomic result and reclaim every waiter`() = runSuspendIO {
    val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(config(maxWaitersPerKey = 2))

    runConformanceScenarios(adapter, config(maxWaitersPerKey = 2), inFlightScenarios())

    adapter.completedScenarioCount shouldBeEqualTo 7
    adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    adapter.maximumObservedWaiters shouldBeEqualTo 2
}

@Test
fun `one nanosecond timeout preserves before exact and after ordering`() = runSuspendIO {
    val oneNanosecondConfig = config(waitTimeout = Duration.ofNanos(1))
    val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(oneNanosecondConfig)

    runConformanceScenarios(
        adapter = adapter,
        config = oneNanosecondConfig,
        scenarios = listOf(deadlineOrderingScenario()),
    )

    adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
}
```

- [ ] **Step 2: 새 scenario가 unresolved로 실패하는지 확인**

Run: `./gradlew :bluetape4k-junit5:test --tests '*HttpIdempotencyInFlightScenariosTest' --no-configuration-cache`
Expected: `inFlightScenarios` unresolved로 FAIL.

- [ ] **Step 3: wait, exact deadline, timeout/overflow immediate assertion 구현**

```kotlin
internal fun inFlightScenarios(): List<ConformanceScenario> = listOf(
    ConformanceScenario("bounded-wait") { adapter, config -> assertWaiterReceivesOwnerTerminal(adapter, config) },
    deadlineOrderingScenario(),
    ConformanceScenario("wait-timeout") { adapter, config -> assertTimeoutReclaimsSlot(adapter, config) },
    ConformanceScenario("waiter-overflow") { adapter, config -> assertOverflowIsImmediate(adapter, config) },
    ConformanceScenario("waiter-cancellation") { adapter, config ->
        assertCancellationReclaimsSlot(adapter, config)
    },
    ConformanceScenario("transient-abandon") { adapter, config ->
        assertTransientAbandonElectsOneRetryOwner(adapter, config)
    },
    ConformanceScenario("owner-disconnect") { adapter, config ->
        assertOwnerDisconnectBeforeAndAfterCommit(adapter, config)
    },
)

internal fun deadlineOrderingScenario(): ConformanceScenario =
    ConformanceScenario("deadline-ordering") { adapter, config -> assertDeadlineOrdering(adapter, config) }

private suspend fun assertDeadlineOrdering(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    assertDeadlineOrdering(adapter, config.waitTimeout.minusNanos(1), expectedCode = null)
    assertDeadlineOrdering(adapter, config.waitTimeout, expectedCode = "idempotency_in_flight")
    assertDeadlineOrdering(adapter, config.waitTimeout.plusNanos(1), expectedCode = "idempotency_in_flight")
}
```

`Duration.ofNanos(1)` behavioral test는 zero-nanosecond (before) completion이 terminal replay를 선택하고, 정확히 1ns (at)와 2ns (after)는 timeout을 선택하는지 직접 검증한다. 세 subcase 모두 waiter slot과 gate를 0으로 회수하며 constructor acceptance만으로 1ns 지원을 주장하지 않는다.

`assertTimeoutReclaimsSlot`은 `409`, `idempotency_in_flight`, 정확히 하나의
`Retry-After: ${config.inFlightRetryAfter.seconds}` delta-seconds value, owner 유지, timed-out waiter slot 회수와 replacement waiter admission을 함께 검증한다. `assertOverflowIsImmediate`는 owner release나 clock advance 전에 `429`, `idempotency_waiters_exceeded`, 정확히 하나의
`Retry-After: ${config.overflowRetryAfter.seconds}` value를 검증하고 waiter/execution count가 변하지 않음을 확인한다. header는 숫자 하나이고 `> 0`임을 `shouldBeEqualTo`/`shouldHaveSize`로 직접 판정한다.

- [ ] **Step 4: cancellation을 terminal failure로 저장하지 않는 test double 구현**

```kotlin
catch (e: CancellationException) {
    withContext(NonCancellable) {
        mutex.withLock {
            cleanupRegistrationExactlyOnce(request)
        }
    }
    throw e
}
```

실제 cancellation test는 cleanup mutex를 다른 coroutine이 잡은 상태에서 waiter/owner job을 취소하고, lock을 해제한 뒤 slot 0, terminal 저장 0, concurrent retry의 single replacement owner와 원래
`CancellationException` 재전파를 검증한다. `NonCancellable`은 위 최소 state-cleanup 구간에만 둔다.
`cleanupRegistrationExactlyOnce`는 request job에 기록한 registration role을 보고 waiter면 해당 slot만 제거하고 owner면 terminal commit 전일 때만 ownership을 abandon하며, 이미 정리된 role에는 no-op이다.

- [ ] **Step 5: concurrency test를 20회 반복 실행**

Run: `for i in {1..20}; do ./gradlew :bluetape4k-junit5:test --tests '*HttpIdempotencyInFlightScenariosTest' --no-configuration-cache --rerun-tasks || exit 1; done`
Expected: 20/20 PASS, timeout, cancellation 또는 lingering job 없음.

- [ ] **Step 6: Lore commit**

```bash
git add testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency \
  testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency
git commit -m "Make bounded waiting deterministic at every lifecycle edge" \
  -m "Constraint: Exact deadline chooses timeout and every exit path reclaims its waiter slot exactly once" \
  -m "Rejected: Wall-clock sleeps | they cannot prove race ordering or cleanup" \
  -m "Confidence: high" -m "Scope-risk: broad" \
  -m "Tested: In-flight scenarios passed 20 consecutive runs"
```

## Task 5: Retention, ingress safety, replay bounds와 fan-in stress

**Complexity:** High **Depends on:** Task 4 **Pattern
skills:** `bluetape-kotlin-patterns`, `kotlin-coroutines-skill`, `test-driven-development`
**Rollback/rerun:** stress가 불안정하면 round/worker를 낮추지 말고 barrier 또는 cleanup 원인을 수정한 뒤 같은 명령을 재실행한다.

**Files:**

- Create: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyBoundaryScenarios.kt`
- Modify: `testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency/BoundedWaitHttpIdempotencyConformance.kt`
- Create: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/HttpIdempotencyBoundaryScenariosTest.kt`
- Modify: `testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency/InMemoryBoundedWaitHttpIdempotencyAdapter.kt`

- [ ] **Step 1: expiry, oversized ingress와 unsafe replay 실패 test 작성**

```kotlin
@Test
fun `boundary scenarios reject unsafe snapshots and elect one owner after expiry`() = runSuspendIO {
    val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(config(maxRequestBodyBytes = 64))

    runConformanceScenarios(adapter, config(maxRequestBodyBytes = 64), boundaryScenarios())

    adapter.completedScenarioCount shouldBeEqualTo 4
    adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    adapter.unsafeReplayPersistCount shouldBeEqualTo 0
}
```

- [ ] **Step 2: boundary test가 missing scenario로 실패하는지 확인**

Run: `./gradlew :bluetape4k-junit5:test --tests '*HttpIdempotencyBoundaryScenariosTest' --no-configuration-cache`
Expected: `boundaryScenarios` unresolved로 FAIL.

- [ ] **Step 3: expiry/key/body/header boundary scenario 구현**

```kotlin
internal fun boundaryScenarios(): List<ConformanceScenario> = listOf(
    ConformanceScenario("retention-expiry") { adapter, config ->
        assertExpiryBeforeAtAndAfterBoundary(adapter, config)
        assertConcurrentRetryElectsOneOwnerAfterExpiry(adapter, config)
    },
    ConformanceScenario("ingress-bounds") { adapter, config ->
        assertOversizedKeyAndBodyRejectedThroughExchange(adapter, config)
        assertMalformedKeysRejectedBeforeOwnership(adapter, config)
        assertAmbiguousTupleRemainsDistinct(adapter, config)
        assertCanonicalPayloadBehavior(adapter, config)
    },
    ConformanceScenario("replay-snapshot-bounds") { adapter, config ->
        assertReplayHeaderDenylistAndAggregateBounds(adapter, config)
    },
    ConformanceScenario("repeated-fan-in") { adapter, config -> assertRepeatedFanInAcrossKeys(adapter, config) },
)

private suspend fun assertOversizedKeyAndBodyRejectedThroughExchange(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    val oversizedBody = "x".repeat(config.maxRequestBodyBytes + 1)
    val response = exchangeChecked(
        adapter,
        config,
        request(requestBody = oversizedBody),
        allowConfiguredRequestOverflow = true,
    )
    response.statusCode shouldBeEqualTo 413
    adapter.quiescence().activeWaiters shouldBeEqualTo 0
    adapter.sideEffectCount(request(requestBody = oversizedBody)) shouldBeEqualTo 0
}
```

Ingress scenario의 exact vectors는 다음과 같다.

- duplicate values와 transport-legal/application-invalid `" "`, HTAB `"\t"`, non-ASCII `"é"`는 real HTTP header로 전송해 route invocation 1, stable `400`, waiter 0, side effect 0을 검증한다. Ktor 3.5의
  `HeadersBuilder`가 거절하는 C0 `"\u0001"`은 value/crafted-serialization validation과 Ktor client-boundary rejection에서만 검증하며 server-ingress conformance vector라고 주장하지 않는다.
- 두 server-resolved tenant/resource/key tuple은 delimiter 없는 단순 concat 결과가 같도록 만들되 domain separator + byte-length framing에서는 다르게 구성한다. 두 request는 서로 replay/conflict하지 않고 각각 side effect 1을 가진다.
- JSON field-order만 다른 두 body는 reference application의 canonical form이 같아 terminal replay되고, duplicate-member처럼 reference canonicalizer가 단일 의미를 만들 수 없는 body는 ownership 전에 `400`으로 거절된다. fixture는 특정 JSON/hash library를 public API에 노출하지 않는다.

- [ ] **Step 4: barrier-aligned custom harness로 repeated fan-in/overflow 구현**

```kotlin
repeat(5) { round ->
    val keys = List(3) { index -> "fan-in-$round-$index" }
    coroutineScope {
        val completions = Channel<Pair<Int, HttpIdempotencyResponse>>(Channel.UNLIMITED)
        val owners = keys.map { key ->
            async(start = CoroutineStart.UNDISPATCHED) {
                exchangeChecked(adapter, config, request(idempotencyKey = key))
            }
        }
        keys.forEach { key -> adapter.awaitOwnerStarted(request(idempotencyKey = key)) }

        val attempts = keys.mapIndexed { keyIndex, key ->
            List(config.maxWaitersPerKey + 2) {
                async(start = CoroutineStart.UNDISPATCHED) {
                    exchangeChecked(adapter, config, request(idempotencyKey = key)).also { response ->
                        completions.send(keyIndex to response)
                    }
                }
            }
        }
        keys.forEach { key -> adapter.awaitWaiterCount(request(idempotencyKey = key), config.maxWaitersPerKey) }

        val overflowByIndex = List(keys.size * 2) { completions.receive() }.groupBy({ it.first }, { it.second })
        keys.indices.forEach { keyIndex ->
            overflowByIndex.getValue(keyIndex).also { immediate ->
                immediate shouldHaveSize 2
            }.forEach { response ->
                response.statusCode shouldBeEqualTo 429
                response.problemCode shouldBeEqualTo "idempotency_waiters_exceeded"
                response.headers["retry-after"] shouldBeEqualTo listOf(config.overflowRetryAfter.seconds.toString())
            }
        }

        adapter.completeOwner(request(idempotencyKey = keys.first()), createdResponse())
        attempts.first().awaitAll()
        keys.drop(1).forEach { key -> adapter.completeOwner(request(idempotencyKey = key), createdResponse()) }

        owners.awaitAll().forEach { it.statusCode shouldBeEqualTo 201 }
        attempts.forEachIndexed { keyIndex, responses ->
            val observed = responses.awaitAll()
            observed.count { it.statusCode == 429 } shouldBeEqualTo 2
            observed.count { it.statusCode == 201 } shouldBeEqualTo config.maxWaitersPerKey
            observed.filter { it.statusCode == 201 }.forEach { response ->
                response.headers["idempotency-replayed"] shouldBeEqualTo listOf("true")
            }
            adapter.sideEffectCount(request(idempotencyKey = keys[keyIndex])) shouldBeEqualTo 1
        }
        completions.cancel()
    }
}
adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
```

이 scenario는 `SuspendedJobTester`를 사용하지 않는다. 기존 tester는 개별 attempt의 `Deferred`
completion과 key별 waiter-registration barrier를 노출하지 않아 exact admitted/overflow count를 결정론적으로 판정할 수 없다. raw key가 assertion의 collection/map rendering에 노출되지 않도록 completion은 numeric key index만 운반한다. 5 rounds, 3 keys, runner 상한 32에서는 최악에도 510 attempt와 15 owner, round당 105 동시 request, completion channel 최대 96 retained response로 제한된다. 이 이유와 workload budget을 scenario test KDoc과 final review evidence에 남긴다.

`assertReplayHeaderDenylistAndAggregateBounds`는 safe `content-type`/allowlisted `etag`가 replay되고 mixed-case `Authorization`, `Cookie`, `Set-Cookie` 이름과 값은 first/replay diagnostics와 replay response에 없음을 검증한다. header name/value/individual/aggregate 및 body max+1 outcome은 terminal snapshot commit 전에 stable failure로 ownership을 abandon하며 committed side-effect와 persisted snapshot count가 0이다. 뒤따른 bounded retry는 새 owner 하나만 선출한다.

- [ ] **Step 5: boundary와 repeated stress 실행**

Run: `./gradlew :bluetape4k-junit5:test --tests '*HttpIdempotencyBoundaryScenariosTest' --no-configuration-cache`
Expected: expiry exact boundary, oversized request via `exchange`, unsafe headers/body, 5-round fan-in에서 key별 owner 1, admitted `maxWaitersPerKey`, overflow 2, cross-key blocking isolation, slot 회수와 bounded termination이 모두 PASS.

- [ ] **Step 6: public entrypoint에 세 scenario group 연결하고 KDoc 작성**

```kotlin
/**
 * Verifies the bounded-wait HTTP idempotency profile through the supplied HTTP adapter.
 *
 * The caller owns the HTTP application and blocking dispatcher. The runner owns and closes
 * only its monotonic watchdog scheduler. Passing this fixture proves observable HTTP behavior,
 * not durable persistence, restart recovery, or exactly-once external side effects.
 * The shared proof accepts at most 32 waiters per key; larger production limits need load tests.
 */
suspend fun assertBoundedWaitHttpIdempotencyConformance(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) = runConformanceScenarios(
    adapter = adapter,
    config = config,
    scenarios = terminalScenarios() + inFlightScenarios() + boundaryScenarios(),
)
```

`terminalScenarios()` 6개, `inFlightScenarios()` 7개, `boundaryScenarios()` 4개로 정확히 17개의 named scenario를 만든다. 각 scenario가 독립 watchdog, `resetScenario`와 quiescence 검증을 가지므로 앞 scenario의 state나 실패가 뒤 scenario로 전파되지 않는다.

- [ ] **Step 7: 전체 shared runner unit proof 실행**

Run: `./gradlew :bluetape4k-junit5:test --no-configuration-cache`
Expected: 기존 281 baseline tests와 신규 fixture tests 전부 PASS; no leaked thread/job warning.

- [ ] **Step 8: Lore commit**

```bash
git add testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency \
  testing/junit5/src/test/kotlin/io/bluetape4k/junit5/http/idempotency
git commit -m "Bound idempotency retention and replay under hostile fan-in" \
  -m "Constraint: Negative request-size vectors must traverse the real adapter exchange" \
  -m "Confidence: high" -m "Scope-risk: broad" \
  -m "Tested: Full junit5 tests and repeated fan-in scenarios passed"
```

## Task 6: Ktor `testApplication` reference proof

**Complexity:** High **Depends on:** Task 5 **Pattern
skills:** `bluetape-kotlin-patterns`, `kotlin-coroutines-skill`, `test-driven-development`
**Rollback:** Ktor test file만 제거하면 shared public fixture는 유지된다.

**Files:**

- Create: `ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/idempotency/KtorHttpIdempotencyConformanceTest.kt`

- [ ] **Step 1: same runner 호출 test 작성**

```kotlin
@Test
fun `Ktor testApplication satisfies bounded wait HTTP idempotency conformance`() = testApplication {
    val config = conformanceConfig()
    val fakeApplication = KtorFakeIdempotencyApplication(config)
    application { fakeApplication.installRoutes(this) }

    val adapter = KtorBoundedWaitHttpIdempotencyAdapter(client, fakeApplication, config)
    assertBoundedWaitHttpIdempotencyConformance(adapter, config)
}
```

- [ ] **Step 2: Ktor test가 missing adapter/routes로 실패하는지 확인**

Run: `./gradlew :bluetape4k-ktor-testing:test --tests '*KtorHttpIdempotencyConformanceTest' --no-configuration-cache`
Expected: Ktor fake application/adapter unresolved로 FAIL.

- [ ] **Step 3: 실제 HTTP route와 server-resolved auth profile 구현**

```kotlin
post("/commands/{resourceId}") {
    val principal = authProfiles.resolve(call.request.header("X-Test-Auth-Profile"))
        ?: return@post call.respond(HttpStatusCode.Unauthorized)
    if (!principal.canWrite) return@post call.respond(HttpStatusCode.Forbidden)
    val keyValues = call.request.headers.getAll("Idempotency-Key").orEmpty()
    val body = when (val bounded = call.receiveBoundedUtf8(config.maxRequestBodyBytes)) {
        is BoundedBodyRead.Value -> bounded.value
        BoundedBodyRead.TooLarge -> return@post call.respond(HttpStatusCode.PayloadTooLarge)
        BoundedBodyRead.Malformed -> return@post call.respond(HttpStatusCode.BadRequest)
    }
    call.respond(execute(principal.tenant, call.parameters.getValue("resourceId"), keyValues, body))
}

private suspend fun ApplicationCall.receiveBoundedUtf8(maxBytes: Int): BoundedBodyRead {
    request.contentLength()?.let { declared ->
        if (declared > maxBytes) return BoundedBodyRead.TooLarge
    }
    val bytes = receiveChannel().readRemaining(maxBytes.toLong() + 1L).readBytes()
    if (bytes.size > maxBytes) return BoundedBodyRead.TooLarge
    return try {
        val value = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
        BoundedBodyRead.Value(value)
    } catch (_: CharacterCodingException) {
        BoundedBodyRead.Malformed
    }
}
```

Ktor test는 declared `Content-Length`가 limit를 넘는 경우와 length 없이 chunked body가 `max + 1`
byte에 도달하는 경우를 각각 실행한다. 둘 다 lookup/owner 획득 전에 `413`, waiter 0, side effect 0이며 bounded reader가 `max + 1`보다 더 읽지 않았음을 test counter로 확인한다.

- [ ] **Step 4: adapter exchange/control mapping 구현**

```kotlin
override suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse {
    val response = client.post("/commands/${request.resourceIdentity}") {
        header("X-Test-Auth-Profile", request.authenticationProfile)
        headers {
            request.idempotencyKeys.forEach { append("Idempotency-Key", it) }
        }
        setBody(request.requestBody)
    }
    return HttpIdempotencyResponse(
        statusCode = response.status.value,
        body = response.bodyAsText(),
        headers = response.headers.entries().associate { it.key to it.value },
        problemCode = response.headers["X-Problem-Code"],
    )
}
```

같은 Ktor test file의 malformed-ingress test는 duplicate values와 `" "`, `"\t"`, `"é"` 각각에 대해 실제
`client.post`를 수행하고, route entry counter가 요청당 정확히 1 증가한 뒤 application validation이 `400`을 반환하는지 확인한다. 별도 C0 `"\u0001"` test는 `HeadersBuilder.append`가 route counter 증가 없이
`IllegalArgumentException`을 던지는 transport-client boundary만 고정한다. 따라서 shared 17 scenarios에는 모든 adapter가 전송할 수 있는 세 application-invalid value만 들어가고, Ktor 전용 transport 제약은 독립 assertion으로 남는다.

- [ ] **Step 5: Ktor reference test와 module test 실행**

Run: `./gradlew :bluetape4k-ktor-testing:test --no-configuration-cache`
Expected: 동일 17 scenario가 skip/override 없이 PASS; transport-legal malformed value는 route를 통과해 application `400`, C0는 client boundary에서 거절되고 test application 종료 후 quiescence 0.

- [ ] **Step 6: Lore commit**

```bash
git add ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/idempotency/KtorHttpIdempotencyConformanceTest.kt
git commit -m "Prove the bounded idempotency contract through real Ktor HTTP" \
  -m "Constraint: The Ktor reference remains a test application and exports no production plugin" \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: bluetape4k-ktor-testing tests passed"
```

## Task 7: Spring MockMvc reference proof

**Complexity:** High **Depends on:** Task 6 **Pattern
skills:** `bluetape-kotlin-patterns`, `kotlin-spring`, `kotlin-coroutines-skill`, `test-driven-development`
**Rollback/rerun:** MockMvc executor/thread leak가 있으면 Spring task만 반복하며 Ktor/shared proof를 변경하지 않는다.

**Files:**

- Create: `spring-boot/core/src/test/kotlin/io/bluetape4k/spring/idempotency/SpringHttpIdempotencyConformanceTest.kt`

- [ ] **Step 1: same runner + outer executor ownership test 작성**

```kotlin
@Test
fun `Spring MockMvc satisfies bounded wait HTTP idempotency conformance`() = runSuspendIO {
    val config = conformanceConfig()
    val application = SpringFakeIdempotencyApplication(config)
    Executors.newFixedThreadPool(8).asCoroutineDispatcher().use { dispatcher ->
        val mockMvc = MockMvcBuilders.standaloneSetup(IdempotencyController(application))
            .addFilters(BoundedBodyFilter(config.maxRequestBodyBytes))
            .build()
        val adapter = SpringBoundedWaitHttpIdempotencyAdapter(mockMvc, application, dispatcher, config)
        assertBoundedWaitHttpIdempotencyConformance(adapter, config)
    }
}
```

- [ ] **Step 2: Spring test가 missing controller/adapter로 실패하는지 확인**

Run: `./gradlew :bluetape4k-spring-boot-core:test --tests '*SpringHttpIdempotencyConformanceTest' --no-configuration-cache`
Expected: Spring fake application/controller/adapter unresolved로 FAIL.

- [ ] **Step 3: standalone controller와 server-resolved auth 구현**

```kotlin
@PostMapping("/commands/{resourceId}")
fun command(
    @RequestHeader("X-Test-Auth-Profile") authenticationProfile: String?,
    @RequestHeader("Idempotency-Key") keys: List<String>,
    @PathVariable resourceId: String,
    @RequestBody body: String,
): ResponseEntity<String> {
    val principal = authProfiles.resolve(authenticationProfile)
        ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
    if (!principal.canWrite) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    return application.execute(principal.tenant, resourceId, keys, body).toResponseEntity()
}
```

`BoundedBodyFilter`는 controller argument binding 전에 `Content-Length > max`를 즉시 `413`으로 거절한다. 길이가 없거나 신뢰할 수 없으면 servlet input을 `max + 1` byte까지만 읽고, 초과면 `413`, strict UTF-8 decode 실패면 `400`을 반환한다. 성공 시 bounded byte copy를 반환하는
`HttpServletRequestWrapper`로 chain을 계속한다. dedicated filter test는 `MockHttpServletRequest`의 declared-length와 `contentLength = -1` 경로를 각각 실행해 controller/lookup invocation 0과 bounded read count를 검증한다.

- [ ] **Step 4: interruptible bounded dispatcher에서 MockMvc exchange 구현**

```kotlin
override suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse = runInterruptible(dispatcher) {
    val result = mockMvc.perform(
        post("/commands/{resourceId}", request.resourceIdentity)
            .header("X-Test-Auth-Profile", request.authenticationProfile)
            .header("Idempotency-Key", *request.idempotencyKeys.toTypedArray())
            .contentType(MediaType.APPLICATION_JSON)
            .content(request.requestBody)
    ).andReturn().response
    HttpIdempotencyResponse(
        statusCode = result.status,
        body = result.contentAsString,
        headers = result.headerNames.associateWith { result.getHeaders(it) },
        problemCode = result.getHeader("X-Problem-Code"),
    )
}
```

dedicated lifecycle test는 test controller가 `CountDownLatch.await()`에서 의도적으로 멈추게 한 뒤
`scenarioTimeout` watchdog을 발생시킨다. `runInterruptible`이 blocking MockMvc thread에 interrupt를 전달하고, 원래 timeout assertion이 bounded하게 반환하며, adapter cleanup/reset 후 caller-owned executor를 닫았을 때 해당 name prefix의 live thread가 0인지 검증한다. controller는 `InterruptedException` 관측 count도 1로 기록한다. 단순 `withContext(dispatcher)`는 coroutine cancellation만 전달하고 Java blocking call을 깨우지 못하므로 사용하지 않는다.

- [ ] **Step 5: Spring reference test와 module test 실행**

Run: `./gradlew :bluetape4k-spring-boot-core:test --no-configuration-cache`
Expected: 동일 17 scenario PASS; deliberately blocked exchange가 timeout 안에 interrupt되고 outer dispatcher/executor close 후 live owned thread 0.

- [ ] **Step 6: Lore commit**

```bash
git add spring-boot/core/src/test/kotlin/io/bluetape4k/spring/idempotency/SpringHttpIdempotencyConformanceTest.kt
git commit -m "Prove the bounded idempotency contract through Spring MockMvc" \
  -m "Constraint: Blocking MockMvc exchange must be interruptible on a caller-owned bounded dispatcher" \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: bluetape4k-spring-boot-core tests passed"
```

## Task 8: Bilingual docs, caller guidance와 release note

**Complexity:** Medium **Depends on:** Tasks 6-7의 실제 compile-checked examples **Pattern
skills:** `bluetape-writer`, `bluetape-kotlin-patterns`
**Rollback:** repository docs는 이 task commit만 revert한다. adopter는 fixture 호출 제거 또는 이전 library version pin으로 opt-out하며 production data rollback은 없다. 정책 자체를 바꾸면 API version과 client migration 안내를 먼저 제공한다.

**Files:**

- Modify: `testing/junit5/README.md`
- Modify: `testing/junit5/README.ko.md`
- Create: `docs/manual/en/modules/bluetape4k-junit5/http-idempotency-conformance.md`
- Create: `docs/manual/ko/modules/bluetape4k-junit5/http-idempotency-conformance.md`
- Modify: `docs/manual/en/modules/bluetape4k-junit5.md`
- Modify: `docs/manual/ko/modules/bluetape4k-junit5.md`
- Modify: `CHANGELOG.md`

- [ ] **Step 1: README English/Korean에 같은 결과표와 config example 추가**

```kotlin
val config = BoundedWaitHttpIdempotencyConformanceConfig(
    waitTimeout = Duration.ofSeconds(2),
    scenarioTimeout = Duration.ofSeconds(15),
    maxWaitersPerKey = 8,
    retention = Duration.ofHours(24),
    inFlightRetryAfter = Duration.ofSeconds(1),
    overflowRetryAfter = Duration.ofSeconds(2),
    maxIdempotencyKeyBytes = 255,
    maxRequestBodyBytes = 64 * 1024,
    maxReplayBodyBytes = 64 * 1024,
    maxReplayHeaderNames = 8,
    maxReplayValuesPerHeader = 4,
    maxReplayHeaderValueBytes = 4 * 1024,
    maxReplayHeaderBytes = 16 * 1024,
)
assertBoundedWaitHttpIdempotencyConformance(adapter, config)
```

- [ ] **Step 2: caller action과 supported/unsupported matrix를 locale parity로 작성**

두 README 모두 다음 내용을 같은 행/문단 순서로 둔다.

- caller action 5행: ambiguous/retriable response, terminal replay, changed-payload conflict, retention expiry, new business intent
- support matrix 4행: bounded UTF-8 command, binary/large/multipart/streaming, SSE/WebSocket/long-running, external provider side effect
- auth/authorization-before-lookup, server-resolved tenant scope, raw key/payload/tenant logging 금지
- replay allowlist와 non-overridable cookie/authorization denylist
- per-key waiter limit은 tenant/global connection/rate limit을 대체하지 않는다는 abuse boundary
- shared proof의 `maxWaitersPerKey <= 32` workload budget은 production 권고값이 아니며, 더 큰 production limit은 대표 test instance와 별도 load test로 검증한다는 구분
- Ktor/Spring compile-checked test source link, caller-owned scope/executor setup과 cleanup까지 포함한 lifecycle
- in-memory observable HTTP proof만 제공하며 atomic commit/restart/crash/external exactly-once는 adopter가 integration test로 증명한다는 boundary

- [ ] **Step 3: English manual chapter 작성**

Chapter sections: `Problem`, `Policy selection`, `Suitability gate`, `Caller key lifecycle`, `Capacity and abuse`,
`Signals and actions`, `Transaction and crash proof`, `Cancellation and retention`, `Supported inputs`,
`Adoption and rollback`, `Ktor example`, `Spring example`, `Limitations`, `Verification`. `Policy selection`은 immediate rejection/bounded wait/status resource를 비교한다. `Suitability gate`는 latency percentile + wait timeout < upstream deadline, duplicate fan-in, per-key/global connection budget, retry horizon을 판정한다.
`Signals and actions`는 waiter/timeout/overflow/conflict/abandon/replay 증가마다 owning capacity layer와 안전한 대응을 표로 둔다. `Adoption and rollback`은 test dependency → application adapter → fixture → durable integration checks → client docs 순서, fixture 제거/이전 version pin, 정책 변경 시 API versioning/client migration을 명시한다.

- [ ] **Step 4: Korean manual chapter를 의미 parity로 작성**

Korean sections: `문제`, `정책 선택`, `적합성 gate`, `호출자 key lifecycle`, `용량과 악용`, `신호와 대응`,
`transaction과 crash 증명`, `cancellation과 retention`, `지원 입력`, `도입과 철회`, `Ktor 예제`,
`Spring 예제`, `제한`, `검증`. 표의 행 순서, stable code, source link와 adoption/rollback 의미는 English와 동일하게 유지한다.

- [ ] **Step 5: landing page와 changelog link 추가**

```markdown
### Added

- Added an opt-in bounded-wait HTTP idempotency conformance fixture for JUnit 5,
  with identical Ktor and Spring MockMvc reference proofs. It validates observable
  HTTP behavior only; adopters still own atomic persistence, restart recovery,
  authorization, rate limiting, and external-side-effect idempotency
  ([#1055](https://github.com/bluetape4k/bluetape4k-projects/issues/1055)). Adoption
  is opt-in and can be rolled back by removing the fixture call or pinning the
  previous library version; changing the public policy requires client migration.
```

- [ ] **Step 6: locale/source link 검증**

Run:

```bash
for file in \
  testing/junit5/README.md testing/junit5/README.ko.md \
  docs/manual/en/modules/bluetape4k-junit5/http-idempotency-conformance.md \
  docs/manual/ko/modules/bluetape4k-junit5/http-idempotency-conformance.md; do
  for term in assertBoundedWaitHttpIdempotencyConformance idempotency_in_flight \
    idempotency_waiters_exceeded idempotency_key_reused Retry-After Authorization Cookie \
    maxWaitersPerKey status-resource exactly-once rollback; do
    rg -q "$term" "$file" || { echo "missing $term in $file"; exit 1; }
  done
done
for file in \
  testing/junit5/README.md testing/junit5/README.ko.md \
  docs/manual/en/modules/bluetape4k-junit5/http-idempotency-conformance.md \
  docs/manual/ko/modules/bluetape4k-junit5/http-idempotency-conformance.md; do
  for source in KtorHttpIdempotencyConformanceTest SpringHttpIdempotencyConformanceTest; do
    rg -q "$source" "$file" || { echo "missing $source link in $file"; exit 1; }
  done
done
for landing in docs/manual/en/modules/bluetape4k-junit5.md \
  docs/manual/ko/modules/bluetape4k-junit5.md; do
  rg -q 'http-idempotency-conformance' "$landing" || { echo "missing chapter link in $landing"; exit 1; }
done
```

Expected: first loop output 0, 각 file의 required term 11/11. 두 source test link는 README/manual 네 파일에 각각 존재해 nested loop 8/8이 통과하며, landing page 양쪽은 새 chapter link 1개씩을 가진다. reviewer는 caller action 5행, support 4행, signals/actions 6행의 EN/KO 순서와 의미 parity를 확인한다.

- [ ] **Step 7: Lore commit**

```bash
git add testing/junit5/README.md testing/junit5/README.ko.md docs/manual/en/modules/bluetape4k-junit5 \
  docs/manual/ko/modules/bluetape4k-junit5 docs/manual/en/modules/bluetape4k-junit5.md \
  docs/manual/ko/modules/bluetape4k-junit5.md CHANGELOG.md
git commit -m "Make bounded idempotency adoption limits explicit" \
  -m "Constraint: Documentation must not imply a production store or exactly-once external effects" \
  -m "Confidence: high" -m "Scope-risk: moderate" \
  -m "Tested: English and Korean source links and contract terms were checked"
```

## Task 9: Cross-module verification와 delivery readiness

**Complexity:** Medium **Depends on:** Tasks 1-8 **Pattern
skills:** `verification-before-completion`, `requesting-code-review`, `bluetape-kotlin-patterns`
**Rollback/rerun:** 실패한 가장 작은 module/task로 돌아가 수정하고 해당 targeted test부터 순서대로 재실행한다.

**Files:**

- Modify only if verification exposes a defect in the files owned by Tasks 1-8.

- [ ] **Step 1: exact changed-file scope 확인**

Run: `repo-status && git diff --name-status origin/develop...HEAD`
Expected: 승인된 spec/plan과 plan에 열거한 junit5/Ktor/Spring/docs/changelog 파일만 변경되고 build/dependency/production adapter 파일은 없다.

Run: `git diff --name-status origin/develop...HEAD -- testing/junit5/src/main/kotlin | awk '$1 != "A" { print; bad=1 } END { exit bad }'`
Expected: output 0, exit 0. 기존 `bluetape4k-junit5` main source를 수정/삭제하지 않고 새 package/file만 추가했으므로 existing JVM ABI는 exact unchanged이고 새 surface만 additive다. 저장소에는 module-wide baseline API validator가 없으므로 이 additive-only diff gate와 아래 compiled JVM surface를 authority로 사용하며 generic `apiCheck` task가 있다고 가정하지 않는다.

- [ ] **Step 2: 세 module targeted test를 순차 실행**

Run: `repo-test-summary -- ./gradlew :bluetape4k-junit5:test --no-configuration-cache`
Expected: PASS. Run: `repo-test-summary -- ./gradlew :bluetape4k-ktor-testing:test --no-configuration-cache`
Expected: PASS. Run: `repo-test-summary -- ./gradlew :bluetape4k-spring-boot-core:test --no-configuration-cache`
Expected: PASS.

- [ ] **Step 3: affected modules broader build**

Run: `repo-test-summary -- ./gradlew :bluetape4k-junit5:build :bluetape4k-ktor-testing:build :bluetape4k-spring-boot-core:build --no-configuration-cache`
Expected: BUILD SUCCESSFUL; compile/test/jar tasks pass.

- [ ] **Step 4: detekt/static analysis**

Run: `./gradlew :bluetape4k-junit5:detekt :bluetape4k-junit5:detektTest :bluetape4k-ktor-testing:detektTest :bluetape4k-spring-boot-core:detektTest --no-configuration-cache`
Expected: 0 detekt violations. Task name이 존재하지 않으면 `./gradlew tasks --all | rg 'detekt(Test)?'`로 실제 affected task를 확인하고 같은 module scope로 실행한다.

- [ ] **Step 5: assertion 전수조사**

Run: `rg -n '(assertTrue|assertFalse|assertEquals|kotlin\.test\.assert|org\.junit\.jupiter\.api\.Assertions|\.shouldBe(True|False)\(\))' testing/junit5/src/{main,test}/kotlin/io/bluetape4k/junit5/http/idempotency ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/idempotency spring-boot/core/src/test/kotlin/io/bluetape4k/spring/idempotency`
Expected: no output. Exact matcher가 없는 fallback이 있으면 code comment와 review evidence에 이유가 있어야 한다.

- [ ] **Step 6: serialization/KDoc/compiled public API audit**

Run:

```bash
rg -n '^(data )?class |^interface |^suspend fun |^fun ' \
  testing/junit5/src/main/kotlin/io/bluetape4k/junit5/http/idempotency
issue1055_jar=$(find testing/junit5/build/libs -maxdepth 1 -type f -name '*.jar' \
  ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print -quit)
test -n "$issue1055_jar"
for class_name in \
  io.bluetape4k.junit5.http.idempotency.HttpIdempotencyRequest \
  io.bluetape4k.junit5.http.idempotency.HttpIdempotencyResponse \
  io.bluetape4k.junit5.http.idempotency.BoundedWaitHttpIdempotencyConformanceConfig \
  io.bluetape4k.junit5.http.idempotency.HttpIdempotencyQuiescence \
  io.bluetape4k.junit5.http.idempotency.BoundedWaitHttpIdempotencyAdapter \
  io.bluetape4k.junit5.http.idempotency.BoundedWaitHttpIdempotencyConformanceKt; do
  javap -classpath "$issue1055_jar" -public -s "$class_name"
done
```

Expected: 모든 public value는 `java.io.Serializable`, private `serialVersionUID`는
`HttpIdempotencyValuesTest`의 `ObjectStreamClass.lookup(...).serialVersionUID shouldBeEqualTo 1L`로 검증된다. `javap`에는 승인된 constructor/copy, adapter suspend contract와 runner entrypoint만 있고 Spring/Ktor/store/dispatcher/executor type이 없다. 모든 public declaration 앞 English KDoc을 source review로 확인한다. Task 2 lifecycle test와 Tasks 6-7의 실제 compile된 test가 config 생성, adapter override, runner 호출, caller-owned cleanup을 사용하므로 별도 복사된 문서 sample 대신 동일 source link를 README/KDoc에 연결한다.

- [ ] **Step 7: final hygiene**

Run: `git diff --check && git status --short --branch`
Expected: whitespace error 없음; branch는 commit 후 clean.

- [ ] **Step 8: Type A code review와 PR gate 준비**

6개 code-review lens와 main integration을 실행해 P0=0/P1=0을 만든다. concurrency/security task이므로 performance/stability scan을 생략하지 않는다. PR body에는 issue #1055, 17 scenario, in-memory HTTP proof boundary, three module test evidence, assertion audit, docs parity와 `## DoD Status`를 기록한다. PR 생성은 승인된 delivery scope에서 exact head를 push한 뒤 수행하고, merge는 CI/review 완료 후 별도 승인을 기다린다.

## Acceptance criteria traceability

| Spec/issue criterion                             | Plan task         | Proof command                                           |
|--------------------------------------------------|-------------------|---------------------------------------------------------|
| same-key replay / payload conflict               | Task 3            | `*HttpIdempotencyTerminalScenariosTest`                 |
| tenant scope / auth-before-lookup                | Task 3, Tasks 6-7 | shared terminal test + Ktor/Spring module tests         |
| bounded wait / timeout / overflow / cancellation | Task 4            | 20-run in-flight command                                |
| abandon/retry / owner disconnect                 | Task 4            | in-flight scenario test                                 |
| expiry / failed terminal / unsafe replay         | Tasks 3, 5        | terminal + boundary tests                               |
| malformed/oversized ingress                      | Task 5, Tasks 6-7 | boundary test through real `exchange` + framework tests |
| repeated fan-in and cleanup                      | Tasks 2, 4, 5     | lifecycle test + barrier-aligned exact-count stress     |
| same contract in two frameworks                  | Tasks 6-7         | Ktor and Spring module tests                            |
| serializable/KDoc/assertions patterns            | Tasks 1-2, 9      | value tests + KDoc/assertion audit                      |
| bilingual docs and policy guide                  | Task 8            | locale/source grep                                      |
| no production adapter/dependency                 | Tasks 1-9         | changed-file scope audit                                |

## Step 3-P risk prediction

| Risk                                                       | Signal                                              | Mitigation                                                                                      | Rollback/rerun point  |
|------------------------------------------------------------|-----------------------------------------------------|-------------------------------------------------------------------------------------------------|-----------------------|
| virtual time가 watchdog도 멈춤                             | stalled test가 scenario timeout을 넘김              | runner-owned monotonic scheduler와 outer-finally shutdown                                       | Task 2 lifecycle test |
| timeout/completion double response                         | exact boundary에서 결과/slot count 불일치           | single atomic transition, deadline instant는 timeout                                            | Task 4 20-run loop    |
| cancellation이 owner나 waiter slot을 누수                  | quiescence non-zero, lingering job/thread           | `finally`, `NonCancellable` cleanup only, cancellation rethrow                                  | Task 4 targeted test  |
| config보다 큰 body가 runner에서 먼저 막혀 HTTP를 못 검증   | adapter exchange count 0                            | intrinsic ceiling 이내 `configured + 1` negative vector를 실제 exchange                         | Task 5 boundary test  |
| framework fake 공유로 독립 proof 약화                      | Ktor/Spring가 같은 engine class import              | framework test source에 독립 state machine 유지                                                 | Tasks 6-7 diff review |
| header/body snapshot이 secret 또는 mutable collection 노출 | sentinel가 diagnostics에 등장, equality drift       | deep copy, redacted rendering, serialization/error sentinel tests                               | Task 1 value tests    |
| MockMvc blocking이 caller scope를 고갈                     | timeout, owned executor thread leak                 | caller-owned bounded dispatcher, `runInterruptible`, blocked-call interrupt/thread cleanup test | Task 7 module test    |
| fixture PASS가 durable production proof로 오해             | docs에 crash/transaction integration checklist 부재 | README/manual/changelog에 proof boundary와 adoption checklist                                   | Task 8 locale audit   |

## 계획 self-review

- Spec 17개 scenario는 Tasks 3-5에 빠짐없이 배정했다.
- Public API, `Serializable`, `serialVersionUID`, English KDoc, assertions audit는 Tasks 1-2와 9에 배정했다.
- Spring/Ktor는 later artifact에 의존하지 않으며 shared runner 완료 뒤 순서대로 구현한다.
- README locale parity, manual chapters/landing, changelog와 rollback은 Task 8에 배정했다.
- 신규 module, dependency, database, Testcontainers, production store/filter/plugin, diagram, AGENTS 변경은 scope evidence로 N/A다.
- Plan에는 미정 구현, scenario skip, framework-specific expectation override가 없다.

## Step 3-R 독립 계획 검토 결과

2026-07-22 최신 계획을 서로 독립적인 세 reviewer lane과 main integration에서 재검토했다.

| Lens          | P0 | P1 | P2 | 핵심 확인                                                                                              |
|---------------|---:|---:|---:|--------------------------------------------------------------------------------------------------------|
| Security      |  0 |  0 |  0 | nested collection/view/entry redaction, HTTP malformed-vector transport 경계, raw-key diagnostics 차단 |
| User/Caller   |  0 |  0 |  0 | test-only workload cap과 production capacity 분리, 지원/비지원 및 rollback 안내                        |
| Performance   |  0 |  0 |  0 | 5 rounds, 최대 510 attempts + 15 owners, round당 105 requests, retained response 96                    |
| Operator/Ops  |  0 |  0 |  0 | adapter-before-call preflight, interruptible MockMvc timeout, per-file docs/source-link gate           |
| Stability     |  0 |  0 |  0 | 1ns의 0ns/1ns/2ns behavioral ordering, cleanup/quiescence, blocking-call interruption                  |
| Developer/API |  0 |  0 |  0 | named copy forwarding, additive JVM surface, serializable/KDoc/assertion conventions                   |

Main integration은 spec의 17개 scenario가 Tasks 3-5에 정확히 6/7/4로 배정되고 Ktor/Spring가 같은 public runner를 skip이나 expectation override 없이 호출하며, implementation 전 verification sequence와 rollback boundary가 모든 task에 존재함을 확인했다. 최종 gate는
**P0=0/P1=0**이다.
