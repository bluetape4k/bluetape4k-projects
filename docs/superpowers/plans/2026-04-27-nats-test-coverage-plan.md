# bluetape4k-nats 테스트 커버리지 개선 구현 계획

**작성일**: 2026-04-27  
**이슈**: #177  
**목표**: 라인 커버리지 49.08% → 70% (+68라인)

---

## Task 목록

### T1 — OptionsTest.kt 작성 (complexity: low)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/OptionsTest.kt`

- `natsOptions { server(url) }` → Options 반환, server 설정 확인
- `natsOptions(Properties())` → Properties 기반 Options 생성
- `natsOptionsOf()` → 기본 URL 적용
- `natsOptionsOf(url, maxReconnects, bufferSize)` → 파라미터 반영

**기대 커버**: Options.kt +12라인

---

### T2 — JetStreamOptionsTest.kt 작성 (complexity: low)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/JetStreamOptionsTest.kt`

- `defaultJetStreamOptions` → JetStreamOptions.DEFAULT_JS_OPTIONS와 동일
- `jetStreamOptions { }` → 기본 인스턴스 생성
- `jetStreamOptionsOf()` → 기본값
- `jetStreamOptionsOf(prefix = "myprefix")` → prefix 설정 확인
- `jetStreamOptionsOf(requestTimeout = 5.seconds)` → requestTimeout 설정
- `jetStreamOptionsOf(publishNoAck = true)` → publishNoAck 설정

**기대 커버**: JetStreamOptions.kt +15라인

---

### T3 — PublishOptionsTest.kt 작성 (complexity: low)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/PublishOptionsTest.kt`

- `publishOptions { stream("orders") }` → stream 설정 확인
- `publishOptionsOf(Properties())` → Properties 기반 생성

**기대 커버**: PublishOptions.kt +5라인

---

### T4 — KeyValueOptionsTest.kt 작성 (complexity: low)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/KeyValueOptionsTest.kt`

- `keyValueOptions { }` → 기본 인스턴스
- `keyValueOptions(existingKvo) { }` → 기존 옵션 기반 빌더
- `keyValueOptions(jso) { }` → JetStreamOptions 포함 빌더

**기대 커버**: KeyValueOptions.kt +6라인

---

### T5 — PullSubscriptionOptionsTest.kt 작성 (complexity: low)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/PullSubscriptionOptionsTest.kt`

- `pullSubscriptionOptions { stream("orders") }` → 빌더 경로
- `pullSubscriptionOptionsOf("orders", "consumer-a")` → bind () 경로
- `pullSubscriptionOptionsOf("", "consumer-a")` → IllegalArgumentException
- `pullSubscriptionOptionsOf("orders", "")` → IllegalArgumentException

**기대 커버**: PullSubscriptionOptions.kt +3라인 (현재 3/6 → 6/6)

---

### T6 — PushSubscriptionOptionsTest.kt 작성 (complexity: low)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/PushSubscriptionOptionsTest.kt`

- `pushSubscriptionOptions { stream("orders") }` → 빌더 경로
- `pushSubscriptionOf("orders")` → stream () 경로
- `pushSubscriptionOf("")` → IllegalArgumentException
- `pushSubscriptionOf("orders", "consumer-a")` → bind () 경로
- `pushSubscriptionOf("orders", "")` → IllegalArgumentException

**기대 커버**: PushSubscriptionOptions.kt +6라인

---

### T7 — NatsMessageTest.kt 확장 (complexity: low)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/NatsMessageTest.kt`

현재 10/20 커버. 추가 케이스:

- `natsMessage { subject("foo"); data("hello") }` → 빌더 경로
- `natsMessageOf(message: Message)` → MockK Message 래핑
- `natsMessageOf("foo", "hello".toByteArray())` → ByteArray 경로
- `natsMessageOf("foo", "hello")` → String 경로
- `natsMessageOf("foo", "hello", replyTo = "reply.topic")` → replyTo 설정
- `natsMessageOf("", "data")` → IllegalArgumentException

**기대 커버**: NatsMessage.kt +10라인 (10/20 → 20/20)

---

### T8 — ConnectionExtensionsTest.kt 작성 (complexity: medium)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/ConnectionExtensionsTest.kt`

MockK `Connection` 사용. 현재 23/50, 미커버 경로 집중:

**동기 메서드**:

- `publish(subject, body)` → Connection.publish (subject, null, bytes) 호출 verify
- `publish(subject, replyTo, body)` → 3-arg publish 호출 verify
- `request(subject, body)` → 응답 Message 반환
- `requestAsync(subject, body, timeout = null)` → request () 경로
- `requestAsync(subject, body, timeout = 1.seconds)` → requestWithTimeout () 경로
- `flush(1.seconds)` → flush (java.time.Duration) 호출 verify

**suspend 메서드** (`runTest`):

- `requestSuspending(message, null)` → request (message).await ()
- `requestSuspending(message, 1.seconds)` → requestWithTimeout (message, ...).await ()
- `requestSuspending(subject, bytes)` → request (subject, null, bytes).await ()
- `requestWithTimeoutSuspending(subject, bytes, timeout = null)` → null timeout 경로
- `requestWithTimeoutSuspending(subject, bytes, timeout = 1.seconds)` → timeout 경로
- `drainSuspending(1.seconds)` → drain (Duration) future await

**기대 커버**: ConnectionExtensions.kt +12라인 (23/50 → 35/50+)

---

### T9 — ServiceExtensionsTest.kt 작성 (complexity: medium)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/service/ServiceExtensionsTest.kt`

MockK `Connection` 사용. `ServiceBuilder`는 실제 객체 사용:

- `natsService { connection(nc); name("svc"); version("1.0") }` → Service 비null 반환
- `natsServiceOf(nc, "svc", "1.0")` → 최소 구성 Service 생성
- `natsServiceOf(nc, "svc", "1.0", endpoint1, endpoint2)` → 엔드포인트 2개 등록
- `natsServiceOf(nc, "svc", "1.0") { description("desc") }` → builder 블록 적용

**기대 커버**: Service.kt +10라인 (1/13 → 11/13)

---

### T10 — ConsumerExtensionsTest.kt 작성 (complexity: medium)

**파일**: `infra/nats/src/test/kotlin/io/bluetape4k/nats/client/ConsumerExtensionsTest.kt`

MockK `Consumer` 사용:

- `consumer.drain(100L)` → drain (Duration.ofMillis (100)) 호출 verify
- `consumer.drain(0L)` → 경계값 0 허용
- `consumer.drain(-1L)` → IllegalArgumentException (requireZeroOrPositiveNumber)
- `consumer.drain(kotlin.time.Duration.ZERO)` → 경계값 0 허용
- `consumer.drainSuspending(100L)` → runTest, await () 결과 true
- `consumer.drainSuspending(kotlin.time.Duration.ZERO)` → runTest, 경계값 허용

**기대 커버**: Consumer.kt +7라인 (0/8 → 7/8+)

---

### T11 — 테스트 실행 및 커버리지 검증 (complexity: low)

```bash
cd .worktrees/test-nats-coverage
./gradlew :bluetape4k-nats:test
./gradlew :bluetape4k-nats:koverXmlReport
```

- 목표: ≥ 70% 확인
- 실패 시 미커버 경로 추가 테스트 작성

---

### T12 — README.md + README.ko.md 업데이트 (complexity: low)

**파일**:

- `infra/nats/README.md`
- `infra/nats/README.ko.md`

테스트 섹션에 신규 테스트 파일 목록 + 커버리지 수치 업데이트

---

## 예상 커버리지 개선 요약

| Task     | 파일                       | 기대 추가 라인                  |
|----------|----------------------------|---------------------------------|
| T1       | Options.kt                 | +12                             |
| T2       | JetStreamOptions.kt        | +15                             |
| T3       | PublishOptions.kt          | +5                              |
| T4       | KeyValueOptions.kt         | +6                              |
| T5       | PullSubscriptionOptions.kt | +3                              |
| T6       | PushSubscriptionOptions.kt | +6                              |
| T7       | NatsMessage.kt             | +10                             |
| T8       | ConnectionExtensions.kt    | +12                             |
| T9       | Service.kt                 | +10                             |
| T10      | Consumer.kt                | +7                              |
| **합계** |                            | **+86라인** (목표 +68라인 초과) |

현재 160 + 86 = 246/326 = **75.5%** (예상)
