# Module bluetape4k-junit5

English | [한국어](./README.ko.md)

An extension library that reduces repetitive boilerplate in JUnit 5 tests.

## Architecture

### Extension Component Overview

![Extension Component Overview diagram](../../docs/images/readme-diagrams/testing-junit5-diagram-01.png)

### Class Diagram

![JUnit5 Class Structure diagram](../../docs/images/readme-diagrams/testing-junit5-diagram-02.png)

## Key Features

- `StopwatchExtension` — measure and log test execution time
- `TempFolderExtension` — provide temp directories/files, auto-deleted after the test
- Output capture helpers — capture `System.out`/`System.err` for assertion
- Random/Faker data injection — inject fake or randomized objects into test fields/parameters
- System property helpers — set properties before a test and restore them after
- Awaitility + coroutine helpers — `suspendUntil` / `awaitSuspending`
- Coroutine cancellation contracts — verify propagation, waiter cleanup, and resource cancellation
- HTTP observability conformance — verify stable routes, classifications, correlation, and sensitive-data exclusion
- Stress-testing utilities — `MultithreadingTester`, `SuspendedJobTester`, `StructuredTaskScopeTester`
- Parameter-source extensions — `FieldSource` for parameterized tests
- Mermaid-based reporting — Gantt timeline of test execution

## Usage Examples

### StopwatchExtension

```kotlin
@ExtendWith(StopwatchExtension::class)
class MyTest {
    @Test
    fun `measure execution time`() {
        // Logs: Starting test: [measure execution time]
        // ...
        // Logs: Completed test: [measure execution time] took 123 msecs.
    }
}

// Or use the annotation shortcut
@StopwatchTest
fun `annotated stopwatch test`() { }
```

### TempFolderExtension

`TempFolder.createFile(name)` and `createDirectory(name)` accept relative names under the temporary root only. Blank names,
absolute paths, `..` traversal, and symlink parents that resolve outside the temporary root are rejected.

```kotlin
@ExtendWith(TempFolderExtension::class)
class FileProcessingTest {
    lateinit var tempFolder: TempFolder

    @BeforeEach
    fun setup(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @Test
    fun `file processing test`() {
        val inputFile = tempFolder.createFile("input.txt")
        inputFile.writeText("test data")
        val outputDir = tempFolder.createDirectory("output")
        processFile(inputFile, outputDir)
    }
}
```

### OutputCapture

```kotlin
@OutputCapture
class OutputCaptureTest {
    @Test
    fun `capture stdout`(capturer: OutputCapturer) {
        println("Hello, Console!")
        capturer.capture() shouldContain "Hello, Console!"
    }
}
```

### FakeValue / Fakers

```kotlin
@ExtendWith(FakeValueExtension::class)
class FakeValueTest {
    @FakeValue(provider = "name.fullName")
    private lateinit var fullName: String

    @Test
    fun `injected fake value`() {
        println(fullName)  // e.g. "John Doe"
    }
}

// Fakers utility
val randomText = Fakers.randomString(10, 20)
val phone = Fakers.numberString("010-####-####")
```

### Stress Testing

```kotlin
// Platform threads
MultithreadingTester()
    .workers(Runtime.getRuntime().availableProcessors())
    .rounds(100)
    .add { counter.incrementAndGet() }
    .run()

// Coroutines
SuspendedJobTester()
    .workers(16)
    .rounds(100)
    .add { delay(10); results.add(1) }
    .run()

// Virtual Threads (Java 21+)
StructuredTaskScopeTester()
    .rounds(1000)
    .add { processRequest() }
    .run()

// With timeout — hangs are caught as TimeoutException
StructuredTaskScopeTester()
    .rounds(100)
    .withTimeout(5.seconds)   // run() throws TimeoutException if not done in 5s
    .add { processRequest() }
    .run()
```

### Coroutine Test Helpers

```kotlin
@Test
fun `basic suspend test`() = runSuspendTest {
    val result = someSuspendFunction()
    result shouldBe "expected"
}

@Test
fun `io dispatcher test`() = runSuspendIO {
    val data = readFromFile()
    processData(data)
}
```

### Coroutine Cancellation Contracts

Use the cancellation contract helpers when a suspend API wraps callbacks, futures, HTTP calls, or shared waiters.

```kotlin
@Test
fun `wrapper rethrows cancellation`() = runTest {
    assertCancellationPropagates {
        client.tryFetchSuspending {
            delay(Long.MAX_VALUE)
        }
    }
}

@Test
fun `cancelled waiter does not block the next waiter`() = runTest {
    assertCancellationClearsWaiter(
        awaiter = { gate.await() },
        releaser = { gate.resume() },
    )
}

@Test
fun `cancellation cancels the underlying call`() = runSuspendIO {
    assertResourceCancelledOnCoroutineCancellation(
        beforeCancel = { waitUntilRequestStarted() },
        resourceCancelled = { call.isCanceled },
    ) {
        call.suspendExecute()
    }
}
```

Do not wrap suspend APIs in plain `runCatching` when cancellation must propagate. `CancellationException` is the
structured concurrency signal and must be rethrown. Use `runCatchingNonCancellation` or `resultOfNonCancellation`
when an API intentionally returns `Result` for non-cancellation failures.

### HTTP Observability Conformance

Adapt a framework-owned test registry or tracer result into `HttpOperationObservation`, then verify the same contract
from Spring Boot, Ktor, or another server integration.

```kotlin
assertHttpOperationObservability(
    observation = HttpOperationObservation(
        operationName = "http.server.requests",
        routeTemplate = "/sales/{saleId}",
        statusCode = 200,
        classification = HttpOperationClassification.SUCCESS,
        correlation = HttpOperationCorrelation(
            inbound = requestId,
            outbound = responseRequestId,
            mode = HttpOperationCorrelationMode.PROPAGATED,
        ),
        metricAttributes = metricTags,
    ),
    expectation = HttpOperationExpectation(
        operationName = "http.server.requests",
        routeTemplate = "/sales/{saleId}",
        statusCode = 200,
        classification = HttpOperationClassification.SUCCESS,
        sensitiveValues = HttpOperationSensitiveValues(
            rawUrl = rawUrl,
            query = query,
            clientIp = clientIp,
            userId = userId,
            saleId = saleId,
            requestPayload = payload,
        ),
    ),
)
```

The fixture checks operation and route stability, status-compatible
success/client-error/timeout-or-cancellation/dependency-failure classification, explicit propagated/generated/absent
correlation semantics, and sensitive metric exclusion. All six representative sensitive inputs are required. A
successful check logs only the bounded classification, status code, and metric-attribute count; assertion failures
redact compared values and never log raw telemetry values.

The test remains the lifecycle owner. Create and close the fake registry, tracer, exporter, and OpenTelemetry SDK in
the Spring Boot or Ktor test; this fixture only validates the framework-neutral snapshot and does not install or close
telemetry infrastructure.

### Bounded-Wait HTTP Idempotency Conformance

Use the opt-in fixture to verify a framework adapter against the same observable HTTP contract. The configuration is
instance-scoped test input, not a set of production defaults.

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

| Observation | Stable result | Caller action |
| --- | --- | --- |
| First request owns execution | Terminal application response with `Idempotency-Replayed: false` | Continue normal response handling. |
| Same command reaches a terminal record | Same terminal response with `Idempotency-Replayed: true` | Treat it as terminal replay; do not repeat the business effect. |
| Same key has a different canonical payload | `409 idempotency_key_reused` | Stop automatic retries and investigate key reuse. |
| A bounded waiter reaches `waitTimeout` | `409 idempotency_in_flight` with `Retry-After` | Treat it as an ambiguous, retriable response within the retry horizon. |
| The per-key waiter budget is full | `429 idempotency_waiters_exceeded` with `Retry-After` | Back off and apply tenant/global admission controls. |
| Authentication or authorization fails | The application's normal `401` or `403`, independent of record presence | Authenticate and authorize before idempotency lookup. |

The caller key lifecycle is deliberately explicit:

| Situation | Caller action |
| --- | --- |
| Ambiguous/retriable response | Reuse the same key and canonical payload only while the documented retry horizon remains open. |
| Terminal replay | Accept the replayed terminal response and stop retrying that business command. |
| Changed-payload conflict | Treat `idempotency_key_reused` as a caller defect; never mutate the payload behind the same key. |
| Retention expiry | Expect the same key to become eligible for new ownership at the configured boundary. |
| New business intent | Generate a new key; do not recycle a previous command's key. |

Apply a suitability gate before adopting the fixture:

| Input or operation | Support decision |
| --- | --- |
| Bounded UTF-8 command with a canonical representation | Supported by the shared proof. |
| Binary, large, multipart, or streaming body | Unsupported by this fixture; use a domain-specific fingerprint and integration proof. |
| SSE, WebSocket, or long-running operation | Unsupported; prefer a `status-resource` policy. |
| External provider side effect | Observable HTTP behavior is covered, but provider idempotency and reconciliation need separate proof. |

Resolve authentication and authorization before lookup, derive tenant scope on the server, and never log raw keys,
payloads, or tenant identifiers. Persist only an explicit replay allowlist. `Authorization`, `Cookie`, credential-like,
and hop-by-hop headers remain non-overridable denylist entries even when configured in the allowlist.

`maxWaitersPerKey` bounds only duplicate fan-in for one key. It does not replace tenant/global connection limits, rate
limits, or admission control. The shared conformance workload accepts `maxWaitersPerKey <= 32` so tests stay bounded;
that ceiling is not a production recommendation. Validate a larger production limit with a representative test instance
and a separate load test.

The compile-checked references are
[`KtorHttpIdempotencyConformanceTest`](../../ktor/testing/src/test/kotlin/io/bluetape4k/ktor/testing/idempotency/KtorHttpIdempotencyConformanceTest.kt)
and
[`SpringHttpIdempotencyConformanceTest`](../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/idempotency/SpringHttpIdempotencyConformanceTest.kt).
The Ktor test owns `testApplication`; the Spring test owns and closes its blocking executor/dispatcher. The fixture owns
only its watchdog and calls adapter cleanup after each scenario.

Passing the fixture proves in-memory, observable HTTP behavior only. It does not prove atomic business-result and
idempotency-record commit, restart/crash recovery, or external `exactly-once` effects. Add durable integration tests for
those boundaries. Adoption is opt-in; rollback means removing the fixture call or pinning the previous library version.
Changing the public policy requires API versioning and client migration guidance.

### SystemProperty

```kotlin
@SystemProperty(name = "app.environment", value = "test")
class SystemPropertyTest {
    @Test
    fun `system property set`() {
        System.getProperty("app.environment") shouldBe "test"
    }
}
```

### FieldSource (Parameterized Test)

```kotlin
class FieldSourceTest {
    val isBlankArguments = listOf(
        argumentOf(null, true),
        argumentOf("", true),
        argumentOf("not blank", false)
    )

    @ParameterizedTest
    @FieldSource("isBlankArguments")
    fun `isBlank test`(input: String?, expected: Boolean) {
        input.isNullOrBlank() shouldBe expected
    }
}
```

### Mermaid Report

```bash
# Extract Mermaid Gantt timeline from test output
./gradlew :testing:junit5:test | awk 'f||/^gantt$/{f=1; print}' > gantt.mermaid
```

The listener sequence below shows how JUnit callbacks become a Mermaid Gantt report.

![Mermaid report sequence](../../docs/images/readme-diagrams/testing-junit5-diagram-03.png)

## Best Practices

- Use `TempFolderExtension` instead of ad hoc file paths in tests.
- Keep `TempFolder` names relative to the temporary root; path traversal and symlink escapes are rejected.
- Capture stdout/stderr when assertions depend on console output.
- Prefer `FakeValue` / `Fakers` providers for sample values instead of hardcoded data.
- Use the provided stress-testing helpers for concurrency-heavy tests — they maintain a stable worker pool regardless of round count.
- For suspend APIs, rethrow `CancellationException`, clear registered waiters/continuations on cancellation, and cancel wrapped futures or HTTP calls.
- Avoid plain `runCatching` around suspend APIs unless `CancellationException` is caught and rethrown before producing `Result.failure`.

## Adding the Dependency

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-junit5:${version}")
}
```

## References

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Awaitility](https://github.com/awaitility/awaitility)
- [Data Faker](https://www.datafaker.net/)
