# bluetape4k-nats 테스트 커버리지 개선 설계

**작성일**: 2026-04-27  
**이슈**: #177  
**목표**: `infra/nats` 모듈 라인 커버리지 49.08% → 70% (최소 +68 라인 커버)

---

## 1. 현황 분석

### 베이스라인 (koverXmlReport 실측)

| 파일                         | 커버 | 전체 | 비율  | 우선순위 |
|------------------------------|------|------|-------|----------|
| `Options.kt`                 | 1    | 13   | 7.7%  | HIGH     |
| `Service.kt`                 | 1    | 13   | 7.7%  | HIGH     |
| `JetStream.kt`               | 4    | 15   | 26.7% | HIGH     |
| `KeyValueConfiguration.kt`   | 5    | 18   | 27.8% | HIGH     |
| `ConnectionExtensions.kt`    | 23   | 50   | 46.0% | HIGH     |
| `NatsMessage.kt`             | 10   | 20   | 50.0% | MEDIUM   |
| `PullSubscriptionOptions.kt` | 3    | 6    | 50.0% | MEDIUM   |
| `JetStreamOptions.kt`        | 0    | 16   | 0%    | HIGH     |
| `KeyValueOptions.kt`         | 0    | 6    | 0%    | HIGH     |
| `PublishOptions.kt`          | 0    | 5    | 0%    | MEDIUM   |
| `PushSubscriptionOptions.kt` | 0    | 6    | 0%    | MEDIUM   |
| `Consumer.kt`                | 0    | 8    | 0%    | MEDIUM   |
| `StreamInfoOptions.kt`       | 0    | 4    | 0%    | LOW      |
| `ObjectLink.kt`              | 0    | 5    | 0%    | LOW      |

**전체**: 160/326 = 49.08%  
**목표**: 228/326 = 70.0%  
**필요 추가 커버**: +68 라인

---

## 2. 설계 원칙

### 2-1. 테스트 분류

**순수 단위 테스트 (서버 불필요)**  
DSL 빌더 함수, 파라미터 검증, 반환 타입 검증 → MockK 또는 단순 인스턴스 생성으로 검증

**MockK 기반 단위 테스트**  
`Connection`, `JetStream`, `Consumer` 인터페이스의 extension function → MockK 모킹 + 동작 검증

**`runTest` 기반 코루틴 단위 테스트**  
suspend extension function (`requestSuspending`, `drainSuspending`) → MockK + `runTest`  
`CompletableFuture.completedFuture(value)` 사용하여 실제 서버 없이 await 검증

### 2-2. 파라미터 검증 패턴

`requireNotBlank` 검증이 있는 함수는 blank 입력 케이스를 반드시 포함한다.

```kotlin
assertThrows(IllegalArgumentException::class.java) {
    someFunction("")
}
```

---

## 3. 신규 테스트 파일 목록

### 3-1. `OptionsTest.kt` (기대 커버: +12라인)

```
io.bluetape4k.nats.client.OptionsTest
```

- `natsOptions { }` → `Options` 인스턴스 반환
- `natsOptions(properties) { }` → Properties 기반 초기화
- `natsOptionsOf()` → 기본 URL/maxReconnects/bufferSize 반영
- `natsOptionsOf(url, maxReconnects, bufferSize)` → 파라미터 반영

### 3-2. `JetStreamOptionsTest.kt` (기대 커버: +15라인)

```
io.bluetape4k.nats.client.JetStreamOptionsTest
```

- `defaultJetStreamOptions` → `JetStreamOptions.DEFAULT_JS_OPTIONS`와 동일
- `jetStreamOptions { }` → 빈 빌더로 기본 인스턴스 생성
- `jetStreamOptionsOf()` → 기본값으로 생성
- `jetStreamOptionsOf(prefix, requestTimeout, publishNoAck)` → 파라미터 반영

### 3-3. `PublishOptionsTest.kt` (기대 커버: +5라인)

```
io.bluetape4k.nats.client.PublishOptionsTest
```

- `publishOptions { }` → 기본 빌더 호출
- `publishOptionsOf(properties)` → Properties 기반 초기화

### 3-4. `KeyValueOptionsTest.kt` (기대 커버: +6라인)

```
io.bluetape4k.nats.client.KeyValueOptionsTest
```

- `keyValueOptions { }` → 기본 빌더
- `keyValueOptions(kvo) { }` → 기존 옵션 복사
- `keyValueOptions(jso) { }` → JetStreamOptions 포함

### 3-5. `PullSubscriptionOptionsTest.kt` (기대 커버: +3라인)

```
io.bluetape4k.nats.client.PullSubscriptionOptionsTest
```

- `pullSubscriptionOptionsOf(stream, bind)` → PullSubscribeOptions.bind () 호출 확인
- `pullSubscriptionOptionsOf("", bind)` → IllegalArgumentException
- `pullSubscriptionOptionsOf(stream, "")` → IllegalArgumentException

### 3-6. `PushSubscriptionOptionsTest.kt` (기대 커버: +6라인)

```
io.bluetape4k.nats.client.PushSubscriptionOptionsTest
```

- `pushSubscriptionOf(stream)` → PushSubscribeOptions.stream () 결과
- `pushSubscriptionOf("") ` → IllegalArgumentException
- `pushSubscriptionOf(stream, durable)` → bind () 호출
- `pushSubscriptionOf(stream, "")` → IllegalArgumentException

### 3-7. `NatsMessageTest.kt` (기대 커버: +10라인)

```
io.bluetape4k.nats.client.NatsMessageTest
```

- `natsMessage { subject("foo") }` → NatsMessage 반환
- `natsMessageOf(ByteArray)` → subject/data 설정 확인
- `natsMessageOf(String)` → subject/data 설정 확인
- `natsMessageOf("", data)` → IllegalArgumentException (requireNotBlank)
- replyTo, headers 파라미터 전달 확인

### 3-8. `ConnectionExtensionsTest.kt` (기대 커버: +10라인)

```
io.bluetape4k.nats.client.ConnectionExtensionsTest
```

MockK `Connection` 사용. 아직 커버 안 된 경로 중심:

- `publish(subject, body)` → `Connection.publish(subject, null, body.toUtf8Bytes())` 호출
- `publish(subject, replyTo, body)` → 3-arg publish 호출
- `request(subject, body)` → 동기 호출 결과 반환
- `requestAsync(subject, body, timeout = null)` → `request(...)` 경로
- `requestAsync(subject, body, timeout = someTimeout)` → `requestWithTimeout(...)` 경로
- `flush(timeout)` → `Connection.flush(java.time.Duration)` 호출
- suspend: `requestSuspending(message)` → `CompletableFuture.completedFuture` 로 await 확인
- suspend: `drainSuspending(timeout)` → drain future await

### 3-9. `ServiceExtensionsTest.kt` (기대 커버: +10라인)

```
io.bluetape4k.nats.service.ServiceExtensionsTest
```

MockK `Connection` 사용:

- `natsService { connection(nc); name("svc"); version("1.0") }` → Service 반환
- `natsServiceOf(nc, name, version, endpoint)` → Service에 endpoint 등록 확인

### 3-10. `ConsumerExtensionsTest.kt` (기대 커버: +7라인)

```
io.bluetape4k.nats.client.ConsumerExtensionsTest
```

MockK `Consumer` 사용:

- `consumer.drain(100L)` → CompletableFuture<Boolean> 반환
- `consumer.drain(Duration.ofSeconds(1))` → 유효 Duration
- `consumer.drainSuspending(100L)` → runTest 내 await 결과
- `consumer.drainSuspending(Duration.ZERO)` → 0 이상 경계값

---

## 4. 리스크

1. **`ConnectionExtensions` suspend
   테스트**: `requestSuspending`은 `request(message).await()`를 호출하므로 MockK에서 `every { request(message) } returns CompletableFuture.completedFuture(response)` 형태로 모킹해야 한다. `Connection.request()` 반환 타입이 `CompletableFuture<Message>`인지 확인 필요.

2. **`Service` 빌더
   검증**: `natsService { }` 빌더에 필수 필드 (`connection`, `name`, `version`) 없이 build 시 예외 발생 여부 — 빈 빌더 케이스는 건너뛰고 최소 유효 파라미터만 사용한다.

3. **`Consumer` 인터페이스**: `Consumer.drain(java.time.Duration)` 반환 타입이 `CompletableFuture<Boolean>`인지 확인 후 MockK 설정.

---

## 5. 수락 기준 (DoD)

- [ ] `./gradlew :bluetape4k-nats:koverXmlReport` 결과 라인 커버리지 ≥ 70%
- [ ] 신규 테스트 파일 10개 모두 컴파일 + 통과
- [ ] 모든 신규 테스트는 Testcontainers (NATS 서버) 없이 실행 가능
- [ ] `README.md` + `README.ko.md` 테스트 섹션 업데이트
