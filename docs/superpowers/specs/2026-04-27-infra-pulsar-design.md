# bluetape4k-pulsar 설계 스펙

- **Issue**: #147
- **브랜치**: feat/infra-pulsar
- **작성일**: 2026-04-27
- **상태**: v1.1 (2026-04-27 리뷰 반영)

---

## 1. 배경 및 목표

Apache Pulsar는 멀티테넌시, 토픽별 독립 스토리지, 지리적 복제를 내장한 분산 메시징 플랫폼. Kafka 대비 파티션 수 변경 없이 무한 스케일아웃 가능.

현재 bluetape4k에는 Pulsar Java Client를 Kotlin Coroutines / Flow로 감싸는 래퍼가 없음.

**목표**:
- `infra/pulsar` 단일 모듈로 Kotlin 코루틴 퍼스트 Pulsar 클라이언트 래퍼 제공
- Extension functions 우선 (object 메서드 금지)
- Jackson2 + Jackson3 양쪽 지원 (compileOnly)
- 네이티브 CompressionType DSL 노출
- Spring 통합은 별도 이슈로 분리

---

## 2. 아키텍처 결정

### 2-1. 모듈 구조 (A안 확정)

```
infra/pulsar/
├── build.gradle.kts
└── src/
    ├── main/kotlin/io/bluetape4k/pulsar/
    │   ├── PulsarClientSupport.kt          -- pulsarClient {} DSL, withPulsarClient {}
    │   ├── codec/
    │   │   ├── JacksonSchema.kt            -- Jackson2용 Schema<T> (compileOnly)
    │   │   └── Jackson3Schema.kt           -- Jackson3용 Schema<T> (compileOnly)
    │   ├── producer/
    │   │   ├── ProducerSupport.kt          -- producer {} DSL, withProducer {}
    │   │   └── ProducerExtensions.kt       -- suspend send, sendAsFlow
    │   ├── consumer/
    │   │   ├── ConsumerSupport.kt          -- consumer {} DSL, withConsumer {}
    │   │   └── ConsumerExtensions.kt       -- suspend receive, receiveAsFlow, suspend ack/nack
    │   └── reader/
    │       ├── ReaderSupport.kt            -- reader {} DSL, withReader {}
    │       └── ReaderExtensions.kt         -- suspend readNext, readAsFlow
    └── test/kotlin/io/bluetape4k/pulsar/
        ├── AbstractPulsarTest.kt
        ├── codec/
        │   ├── JacksonSchemaTest.kt
        │   └── Jackson3SchemaTest.kt
        ├── producer/
        │   └── ProducerExtensionsTest.kt
        ├── consumer/
        │   └── ConsumerExtensionsTest.kt
        └── reader/
            └── ReaderExtensionsTest.kt
```

### 2-2. 설계 원칙

1. **Extension functions 우선**: `suspend fun Producer<T>.sendSuspend(...)` 형태
2. **CompletableFuture → Coroutine**: `awaitSuspending()` (`bluetape4k-coroutines`)
3. **Flow 변환**: `flow { while (isActive) { emit(receiveAsync().awaitSuspending()) } }` 폴링 패턴. 취소 시 대기 중인 `CompletableFuture.cancel(true)` 명시적 호출
4. **DSL**: `pulsarClient {}`, `producer {}`, `consumer {}`, `reader {}` 빌더 DSL
5. **withXxx {}**: `suspend inline fun` + `try/finally { closeAsync().awaitSuspending() }` — 예외/취소 시 비동기 close 보장
6. **top-level 파일 로깅**: `companion object : KLogging()` 없는 파일은 `private val log = KotlinLogging.logger {}` 사용

---

## 3. API 설계

### 3-1. 클라이언트 DSL

```kotlin
// 생성 DSL
val client = pulsarClient {
    serviceUrl("pulsar://localhost:6650")
    connectionTimeout(5, TimeUnit.SECONDS)
    operationTimeout(30, TimeUnit.SECONDS)
}

// withPulsarClient {} — 블록 후 자동 close
withPulsarClient("pulsar://localhost:6650") {
    // PulsarClient scope
}
```

**구현 시그니처**:
```kotlin
fun pulsarClient(serviceUrl: String = "", setup: ClientBuilder.() -> Unit = {}): PulsarClient

// block은 suspend 람다 — 내부에서 sendSuspend/receiveSuspend 호출 가능
// finally에서 closeAsync().awaitSuspending() 보장
suspend inline fun <T> withPulsarClient(
    serviceUrl: String,
    setup: ClientBuilder.() -> Unit = {},
    crossinline block: suspend PulsarClient.() -> T
): T
```

### 3-2. Producer DSL + Extension

```kotlin
// Producer 생성 DSL (PulsarClient 확장)
val producer = client.producer(Schema.JSON(Order::class.java)) {
    topic("persistent://public/default/orders")
    producerName("order-producer")
    compressionType(CompressionType.LZ4)   // 네이티브 CompressionType 노출
    batchingEnabled(true)
}

// suspend send
val msgId: MessageId = producer.sendSuspend(order)

// TypedMessageBuilder DSL
val msgId = producer.sendSuspend<Order> {
    value(order)
    key("order-${order.id}")
    property("version", "1")
}

// Flow 기반 배치 발행
val results: Flow<MessageId> = producer.sendAsFlow(ordersFlow)
```

**구현 시그니처**:
```kotlin
// ProducerSupport.kt
fun <T> PulsarClient.producer(schema: Schema<T>, setup: ProducerBuilder<T>.() -> Unit): Producer<T>

// block은 suspend 람다, finally에서 closeAsync().awaitSuspending() 보장
suspend inline fun <T, R> PulsarClient.withProducer(
    schema: Schema<T>,
    setup: ProducerBuilder<T>.() -> Unit = {},
    crossinline block: suspend Producer<T>.() -> R
): R

// ProducerExtensions.kt
suspend fun <T> Producer<T>.sendSuspend(message: T): MessageId
// sendSuspend { value(order); key("...") } — <T> 명시 불필요, Producer<T>에서 추론
suspend fun <T> Producer<T>.sendSuspend(setup: TypedMessageBuilder<T>.() -> Unit): MessageId
// 에러 전파: 첫 메시지 실패 시 Flow 즉시 종료 (기본). retry는 호출자 책임
fun <T> Producer<T>.sendAsFlow(messages: Flow<T>): Flow<MessageId>
```

### 3-3. Consumer DSL + Extension

```kotlin
// Consumer 생성 DSL
val consumer = client.consumer(Schema.JSON(Order::class.java)) {
    topic("persistent://public/default/orders")
    subscriptionName("order-processor")
    subscriptionType(SubscriptionType.Failover)
    ackTimeout(30, TimeUnit.SECONDS)
}

// suspend receive
val msg: Message<Order> = consumer.receiveSuspend()

// Flow 기반 무한 소비
consumer.receiveAsFlow()
    .map { msg ->
        processOrder(msg.value)
        consumer.acknowledgeSuspend(msg)
    }
    .catch { e -> log.error("처리 실패", e) }
    .collect()

// suspend ack
consumer.acknowledgeSuspend(msg)
// negativeAcknowledge — Pulsar API가 void (non-blocking 큐 enqueue). suspend 불필요
// PulsarClientException 발생 시 전파하지 않음 (내부 큐잉)
consumer.negativeAcknowledge(msg)
```

**구현 시그니처**:
```kotlin
// ConsumerSupport.kt
fun <T> PulsarClient.consumer(schema: Schema<T>, setup: ConsumerBuilder<T>.() -> Unit): Consumer<T>

suspend inline fun <T, R> PulsarClient.withConsumer(
    schema: Schema<T>,
    setup: ConsumerBuilder<T>.() -> Unit = {},
    crossinline block: suspend Consumer<T>.() -> R
): R

// ConsumerExtensions.kt
suspend fun <T> Consumer<T>.receiveSuspend(): Message<T>

// receiveAsFlow() 생명주기 계약:
// - Flow 취소(coroutineContext.isActive == false) 시 루프 종료
// - CancellationException 발생 시 대기 중인 CompletableFuture.cancel(true) 후 예외 재전파
// - Flow는 Consumer를 소유하지 않음 — withConsumer {} 블록이나 호출자가 close 책임
// - Pulsar Java Client 내부 재연결(자동)은 receiveAsync() 수준에서 처리됨
// - 브로커 연결 끊김: Pulsar Client가 자동 재연결 시도, 연결 복구 전까지 receiveAsync() 블로킹
fun <T> Consumer<T>.receiveAsFlow(): Flow<Message<T>>

suspend fun <T> Consumer<T>.acknowledgeSuspend(message: Message<T>)
// cumulative ack: Exclusive/Failover subscription에서만 유효.
// Shared subscription에서 호출하면 PulsarClientException 발생 (KDoc에 명시)
suspend fun <T> Consumer<T>.acknowledgeCumulativeSuspend(message: Message<T>)
```

**테스트 subscriptionName**: 각 테스트마다 `subscriptionName("test-sub-${UUID.randomUUID()}")` 패턴 사용 → 테스트 간 충돌 방지.

### 3-4. Reader DSL + Extension

```kotlin
// Reader 생성 DSL
val reader = client.reader(Schema.JSON(Order::class.java)) {
    topic("persistent://public/default/orders")
    startMessageId(MessageId.earliest)
}

// suspend readNext
val msg: Message<Order> = reader.readNextSuspend()

// Flow 기반 읽기 (hasMessageAvailable 기반)
reader.readAsFlow()
    .map { it.value }
    .collect { order -> println(order) }
```

**구현 시그니처**:
```kotlin
// ReaderSupport.kt
fun <T> PulsarClient.reader(schema: Schema<T>, setup: ReaderBuilder<T>.() -> Unit): Reader<T>

suspend inline fun <T, R> PulsarClient.withReader(
    schema: Schema<T>,
    setup: ReaderBuilder<T>.() -> Unit = {},
    crossinline block: suspend Reader<T>.() -> R
): R

// ReaderExtensions.kt
suspend fun <T> Reader<T>.readNextSuspend(): Message<T>

// readAsFlow() 생명주기 계약:
// - hasMessageAvailable() == false 이면 Flow 정상 종료
// - 취소 시 대기 중 CompletableFuture.cancel(true) 후 종료
// - Reader는 소유하지 않음 — withReader {} 또는 호출자가 close 책임
fun <T> Reader<T>.readAsFlow(): Flow<Message<T>>
```

### 3-5. Jackson Schema (compileOnly)

**Jackson2 vs Jackson3 선택 기준**:
- 프로젝트에 `bluetape4k-jackson2` 의존 → `jacksonSchema<T>()` 사용
- 프로젝트에 `bluetape4k-jackson3` 의존 → `jackson3Schema<T>()` 사용
- Jackson2: `com.fasterxml.jackson.databind.ObjectMapper` / Jackson3: `tools.jackson.databind.ObjectMapper` (패키지 다름, 바이너리 비호환) → 런타임에 하나만 사용

> **⚠️ 주의**: `JacksonSchema.kt` / `Jackson3Schema.kt`는 `compileOnly` 의존. 사용 모듈의
> `build.gradle.kts`에서 `implementation(project(":bluetape4k-jackson2"))` (또는 jackson3)를
> 반드시 선언해야 함. 없으면 런타임에 `NoClassDefFoundError` 발생.

```kotlin
// JacksonSchema.kt (Jackson2용 — com.fasterxml.jackson.databind.ObjectMapper)
fun <T> jacksonSchema(type: Class<T>, mapper: ObjectMapper = Jackson.defaultJsonMapper): Schema<T>
inline fun <reified T> jacksonSchema(mapper: ObjectMapper = Jackson.defaultJsonMapper): Schema<T>

// Jackson3Schema.kt (Jackson3용 — tools.jackson.databind.ObjectMapper)
fun <T> jackson3Schema(type: Class<T>, mapper: tools.jackson.databind.ObjectMapper): Schema<T>
inline fun <reified T> jackson3Schema(mapper: tools.jackson.databind.ObjectMapper): Schema<T>
```

내부적으로 Pulsar `Schema<T>` 인터페이스 구현:
- `encode(T): ByteArray` — `mapper.writeValueAsBytes(value)`
- `decode(ByteArray): T` — `mapper.readValue(bytes, type)`
- `getSchemaInfo(): SchemaInfo` — 다음을 포함해야 함:
  ```kotlin
  SchemaInfo.builder()
      .name(type.simpleName)
      .type(SchemaType.JSON)
      .schema(ByteArray(0))   // 브로커 스키마 검증 없음 (호환성 우선)
      .build()
  ```

---

## 4. 압축 지원

Pulsar 네이티브 CompressionType을 DSL로 노출하는 방식 사용:

```kotlin
client.producer(Schema.STRING) {
    topic("...")
    compressionType(CompressionType.LZ4)    // LZ4 / ZSTD / SNAPPY / ZLIB / NONE
}
```

`io/io` Compressor 기반 별도 Schema 래퍼는 이번 스코프에서 제외 (네이티브 지원으로 충분).

---

## 5. 의존성

```kotlin
// build.gradle.kts
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-coroutines"))
    // bluetape4k-io 제외 — io/io Compressor 미사용 (네이티브 CompressionType 사용)

    // Pulsar
    api(Libs.pulsar_client)                   // 3.3.9

    // Jackson (compileOnly — 사용 모듈이 런타임에 하나를 implementation으로 선언해야 함)
    compileOnly(project(":bluetape4k-jackson2"))
    compileOnly(Libs.jackson_databind)         // Jackson2: com.fasterxml.jackson.databind
    compileOnly(project(":bluetape4k-jackson3"))
    compileOnly(Libs.jackson3_databind)        // Jackson3: tools.jackson.databind

    // Coroutines
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)

    // Testcontainers
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers_pulsar)
}
```

---

## 6. 테스트 전략

### 테스트 환경
- `PulsarServer.Launcher.pulsar` (기존 testcontainers) 재사용
- `AbstractPulsarTest` 기반 클래스로 client 생명주기 관리

### 테스트 케이스

| 테스트 | 검증 내용 |
|--------|-----------|
| `ProducerExtensionsTest` | sendSuspend, sendAsFlow (100건) |
| `ConsumerExtensionsTest` | receiveSuspend, receiveAsFlow (Exclusive/Shared/Failover) |
| `ReaderExtensionsTest` | readNextSuspend, readAsFlow (earliest~latest) |
| `JacksonSchemaTest` | Jackson2 encode/decode 라운드트립 |
| `Jackson3SchemaTest` | Jackson3 encode/decode 라운드트립 |

---

## 7. DoD (Definition of Done)

### 구현
- [ ] `infra/pulsar` 모듈 Gradle 등록 (`settings.gradle.kts` 자동)
- [ ] `pulsarClient {}` / `withPulsarClient {}` DSL 구현
- [ ] `producer {}` / `consumer {}` / `reader {}` DSL on PulsarClient 구현
- [ ] `withProducer {}` / `withConsumer {}` / `withReader {}` — `suspend inline fun` + `try/finally { closeAsync().awaitSuspending() }` 구현
- [ ] `Producer<T>` — `sendSuspend(T)`, `sendSuspend(TypedMessageBuilder DSL)`, `sendAsFlow` extension 구현
- [ ] `Consumer<T>` — `receiveSuspend`, `receiveAsFlow`, `acknowledgeSuspend`, `acknowledgeCumulativeSuspend` extension 구현
- [ ] `Reader<T>` — `readNextSuspend`, `readAsFlow` extension 구현
- [ ] Jackson2 `jacksonSchema<T>()` 구현 (SchemaInfo 포함)
- [ ] Jackson3 `jackson3Schema<T>()` 구현 (SchemaInfo 포함)

### 코드 품질
- [ ] 모든 public API 한국어 KDoc (예외 타입, compileOnly 사용 경고 포함)
- [ ] 클래스 파일: `companion object : KLogging()` 포함
- [ ] top-level 파일(`*Extensions.kt`, `*Support.kt`): `private val log = KotlinLogging.logger {}` 사용

### 테스트
- [ ] 테스트별 UUID subscriptionName 사용 (`subscriptionName("test-sub-${UUID.randomUUID()}")`)
- [ ] 테스트 전수 통과 (ProducerExtensionsTest / ConsumerExtensionsTest / ReaderExtensionsTest / JacksonSchemaTest / Jackson3SchemaTest)
- [ ] `src/test/resources/junit-platform.properties` + `logback-test.xml` 포함

### 문서
- [ ] `README.md` + `README.ko.md` 작성
- [ ] `CLAUDE.md` `infra/` 모듈 그룹 테이블에 `pulsar` 추가

---

## 8. 제외 범위

- Spring Integration (별도 이슈)
- Pulsar Functions / Pulsar IO Connectors
- Admin API suspend 래핑
- io/io Compressor 기반 CompressedSchema (네이티브 CompressionType으로 충분)
