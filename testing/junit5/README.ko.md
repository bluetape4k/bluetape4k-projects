# Module bluetape4k-junit5

[English](./README.md) | 한국어

JUnit 5 테스트 작성 시 반복 코드를 줄여주는 확장 라이브러리입니다.

## 아키텍처

### 확장 기능 구성 다이어그램

![확장 기능 구성 다이어그램 1](../../docs/images/readme-diagrams/testing-junit5-diagram-01.png)

### 클래스 다이어그램

![JUnit5 Class Structure diagram](../../docs/images/readme-diagrams/testing-junit5-diagram-02.png)

## 주요 기능

- **Stopwatch Extension**: 테스트 실행 시간 측정
- **TempFolder Extension**: 테스트용 임시 디렉토리/파일 제공, 테스트 완료 후 자동 삭제
- **Output Capture**: System.out/err 및 로그 출력 캡처
- **Random/Faker 확장**: 랜덤/가짜 데이터 주입
- **System Property 확장**: 테스트 중 시스템 속성 설정/복원
- **Awaitility + Coroutines**: suspend 조건 대기 유틸
- **Coroutine Cancellation Contracts**: cancellation 전파, waiter 정리, resource cancellation 검증
- **HTTP Observability Conformance**: 안정적인 route, 결과 분류, correlation, 민감 정보 제외 검증
- **Stress Tester**: 멀티스레드/가상스레드/코루틴 기반 스트레스 테스트
- **Parameter Source 확장**: FieldSource 기반 인자 제공
- **Mermaid 리포트**: 테스트 실행 결과를 Mermaid Gantt 타임라인으로 출력

## 사용 예시

### 1. Stopwatch Extension

```kotlin
@ExtendWith(StopwatchExtension::class)
class MyTest {
    @Test
    fun `테스트 메소드`() {
        // 로그: Starting test: [테스트 메소드]
        // ...
        // 로그: Completed test: [테스트 메소드] took 123 msecs.
    }
}

// 어노테이션 단축형 사용
@StopwatchTest
fun `테스트 실행 시간 측정`() { }
```

### 2. TempFolder Extension

```kotlin
@ExtendWith(TempFolderExtension::class)
class FileProcessingTest {
    lateinit var tempFolder: TempFolder

    @BeforeEach
    fun setup(tempFolder: TempFolder) {
        this.tempFolder = tempFolder
    }

    @Test
    fun `파일 처리 테스트`() {
        val inputFile = tempFolder.createFile("input.txt")
        inputFile.writeText("test data")

        val outputDir = tempFolder.createDirectory("output")
        processFile(inputFile, outputDir)
        // 테스트 완료 후 자동 삭제
    }
}
```

#### TempFolder 주요 메소드

`TempFolder.createFile(name)`과 `createDirectory(name)`은 임시 루트 아래의 상대 이름만 허용합니다.
blank 이름, 절대 경로, `..` 경로 순회, 임시 루트 밖으로 해석되는 symlink 부모는 거부됩니다.

```kotlin
val tempFolder = TempFolder()

val autoNamedFile = tempFolder.createFile()
val namedFile = tempFolder.createFile("config.yml")
val dir = tempFolder.createDirectory("logs")
// tempFolder.createFile("../escape.txt") 는 IllegalArgumentException 발생

println(tempFolder.root)      // File 객체
println(tempFolder.rootPath)  // String

tempFolder.close()  // 수동 삭제 (Closeable 구현)
```

### 3. Output Capture

```kotlin
@OutputCapture
class OutputCaptureTest {

    @Test
    fun `stdout 캡처`(capturer: OutputCapturer) {
        println("Hello, Console!")
        System.err.println("Error message")

        val output = capturer.capture()
        output shouldContain "Hello, Console!"
        output shouldContain "Error message"
    }

    @Test
    fun `expect 블록으로 검증`(capturer: OutputCapturer) {
        println("Test output")
        capturer.expect { captured ->
            captured shouldContain "Test output"
        }
    }
}
```

### 4. Faker 확장

```kotlin
@ExtendWith(FakeValueExtension::class)
class FakeValueTest {

    @FakeValue(provider = "name.fullName")
    private lateinit var fullName: String

    @FakeValue(provider = "address.city", size = 5)
    private lateinit var cities: List<String>

    @Test
    fun `필드에 Fake 값 주입`() {
        println(fullName)   // "John Doe"
        println(cities)     // ["Seoul", "Tokyo", "New York", ...]
    }

    @Test
    fun `파라미터로 Fake 값 받기`(
        @FakeValue(provider = "name.firstName") firstName: String,
        @FakeValue(provider = "internet.emailAddress") email: String,
    ) {
        println(firstName)  // "John"
        println(email)      // "john@example.com"
    }
}
```

#### Fakers 유틸리티

```kotlin
val randomText = Fakers.randomString(10, 20)
val fixedText = Fakers.fixedString(16)
val phone = Fakers.numberString("010-####-####")  // "010-1234-5678"
val code = Fakers.letterString("???-###")         // "ABC-123"
val id = Fakers.alphaNumericString("?#?#?#")      // "A1B2C3"
val uuid = Fakers.randomUuid()
```

### 5. Random 확장

```kotlin
@RandomizedTest
class RandomizedTestExample {

    @RandomValue
    private lateinit var randomString: String

    @RandomValue(excludes = ["id", "password"])
    private lateinit var user: User

    @RandomValue(type = User::class, size = 10)
    private lateinit var users: List<User>

    @Test
    fun `필드에 랜덤 값 주입`() {
        println(randomString)
        println(user)
        users.forEach { println(it) }
    }
}
```

### 6. System Property 확장

```kotlin
@SystemProperty(name = "app.environment", value = "test")
class SystemPropertyTest {

    @Test
    fun `시스템 속성 사용`() {
        System.getProperty("app.environment") shouldBe "test"
        // 테스트 완료 후 자동 복원
    }
}

// 여러 속성 설정
@Test
@SystemProperties(
    SystemProperty(name = "cache.enabled", value = "false"),
    SystemProperty(name = "cache.ttl", value = "60")
)
fun `캐시 설정 테스트`() { }
```

### 7. Awaitility + Coroutines

```kotlin
@Test
fun `suspend 조건 대기`() = runSuspendTest {
    val state = MutableStateFlow(0)

    launch {
        delay(100)
        state.value = 42
    }

    await atMost 5.seconds untilSuspending {
        state.value == 42
    }
}
```

### 8. Coroutine Cancellation Contracts

callback, future, HTTP call, 공유 waiter를 감싸는 suspend API는 cancellation 계약을 명시적으로 테스트하세요.

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

suspend API에서 cancellation 전파가 필요하다면 plain `runCatching`으로 감싸면 안 됩니다.
`CancellationException`은 structured concurrency의 cancellation 신호이므로 반드시 다시 던져야 합니다.
non-cancellation 실패만 `Result`로 반환하려면 `runCatchingNonCancellation` 또는
`resultOfNonCancellation`을 사용하세요.

### HTTP Observability Conformance

Spring Boot, Ktor 같은 서버 통합 테스트에서 registry 또는 tracer 결과를 `HttpOperationObservation`으로
변환한 뒤 같은 계약으로 검증할 수 있습니다.

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

fixture는 operation과 route의 안정성, status와 일치하는
success/client-error/timeout-or-cancellation/dependency-failure 분류, propagated/generated/absent correlation
의미, 민감한 metric 값 제외 여부를 확인합니다. 여섯 종류의 대표 민감 입력은 모두 필수입니다. 검증에
성공하면 classification, status code, metric attribute 개수만 로그에 남기고, assertion 실패 메시지에서는
비교값을 redaction하여 raw telemetry 값을 기록하지 않습니다.

registry, tracer, exporter, OpenTelemetry SDK의 lifecycle은 테스트가 직접 소유합니다. 이 fixture는
framework-neutral snapshot만 검증하며 telemetry 인프라를 설치하거나 종료하지 않습니다.

### 9. Stress Tester

#### 실행 모델 요약

- `MultithreadingTester`: `workers * rounds` 실행 단위를 worker 고정 개수로 분배
- `SuspendedJobTester`: `rounds * 등록된 suspend 블록 수` 실행 단위를 worker 고정 개수로 분배
- 두 구현 모두 라운드 수가 커져도 worker 수만큼만 실행자(스레드/코루틴)를 유지해 메모리 사용량을 안정적으로 유지

```kotlin
// 플랫폼 스레드
MultithreadingTester()
    .workers(Runtime.getRuntime().availableProcessors())
    .rounds(100)
    .add { counter.incrementAndGet() }
    .run()

// 코루틴
SuspendedJobTester()
    .workers(16)
    .rounds(100)
    .add {
        delay(10)
        synchronized(results) { results.add(1) }
    }
    .run()

// Virtual Thread (Java 21+)
StructuredTaskScopeTester()
    .rounds(1000)
    .add { processRequest() }
    .add { handleResponse() }
    .run()

// withTimeout — 걸린 테스트를 TimeoutException으로 감지
StructuredTaskScopeTester()
    .rounds(100)
    .withTimeout(5.seconds)   // 5초 초과 시 TimeoutException
    .add { processRequest() }
    .run()
```

### 10. Coroutine Support

```kotlin
@Test
fun `기본 suspend 테스트`() = runSuspendTest {
    val result = someSuspendFunction()
    result shouldBe "expected"
}

@Test
fun `IO 작업 테스트`() = runSuspendIO {
    val data = readFromFile()
    processData(data)
}

@Test
fun `CPU 집약적 작업 테스트`() = runSuspendDefault {
    val result = heavyComputation()
    result shouldBe 42
}

@Test
fun `Virtual Thread 환경 테스트`() = runSuspendVT {
    val result = blockingOperation()
    result shouldBe "success"
}
```

### 11. FieldSource (Parameterized Test)

```kotlin
class FieldSourceTest {

    val isBlankArguments = listOf(
        argumentOf(null, true),
        argumentOf("", true),
        argumentOf("  ", true),
        argumentOf("not blank", false)
    )

    @ParameterizedTest
    @FieldSource("isBlankArguments")
    fun `isBlank 테스트`(input: String?, expected: Boolean) {
        input.isNullOrBlank() shouldBe expected
    }
}
```

### 12. Mermaid 리포트

```bash
# 테스트 실행 및 Mermaid 리포트 추출
./gradlew :testing:junit5:test | awk 'f||/^gantt$/{f=1; print}' > gantt.mermaid
```

동작 흐름:

![Mermaid 리포트 시퀀스](../../docs/images/readme-diagrams/testing-junit5-diagram-03.png)

- `active`: 성공한 테스트
- `crit`: 실패한 테스트
- `done`: 중단된 테스트

## 모범 사례

- 임시 파일이 필요한 테스트에는 ad hoc 경로 대신 `TempFolderExtension`을 사용하세요.
- `TempFolder` 이름은 임시 루트 기준 상대 경로로 유지하세요. 경로 순회와 symlink escape는 거부됩니다.
- 콘솔 출력을 검증해야 할 때는 `OutputCapture`를 사용하세요.
- 샘플 값에는 하드코딩 대신 `FakeValue`/`Fakers` 프로바이더를 활용하세요.
- 동시성 테스트에는 제공되는 Stress Tester 헬퍼를 재사용하세요 — 라운드 수가 증가해도 worker 수를 일정하게 유지합니다.
- suspend API는 `CancellationException`을 다시 던지고, cancellation 시 등록된 waiter/continuation을 정리하며, 감싼 future 또는 HTTP call을 취소해야 합니다.
- suspend API를 plain `runCatching`으로 감싸지 마세요. 필요하다면 cancellation을 다시 던지는 `runCatchingNonCancellation`을 사용하세요.

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-junit5:${version}")
}
```

## 참고

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Awaitility](https://github.com/awaitility/awaitility)
- [Data Faker](https://www.datafaker.net/)
- [Enhanced Random Beans](https://github.com/benas/random-beans)
