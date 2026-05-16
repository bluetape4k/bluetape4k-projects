# Issue #472: BatchListenerFailedException DLT 라우팅 수정

**날짜**: 2026-05-16
**브랜치**: `fix/batch-listener-dlt-routing`
**관련 이슈**: #472

## 루트 원인

### 원인 1: Spring Kafka 3.3 DLT 토픽 suffix 변경

Spring Kafka 3.3에서 `DeadLetterPublishingRecoverer`의 기본 DLT 토픽 suffix가 변경되었다:

| 버전 | 기본 suffix | 예시 |
|------|------------|------|
| Spring Kafka 3.2 이전 | `.DLT` (대문자, 점) | `blc6.DLT` |
| Spring Kafka 3.3+ | `-dlt` (소문자, 하이픈) | `blc6-dlt` |

`DeadLetterPublishingRecoverer` 소스:
```java
DEFAULT_DESTINATION_RESOLVER = (cr, e) -> new TopicPartition(cr.topic() + "-dlt", cr.partition());
```

테스트는 `blc6.DLT` 토픽을 구독하고 있었지만, recoverer는 `blc6-dlt`로 발행했다.
→ DLT 메시지가 consume되지 않아 latch2가 countdown되지 않음.

### 원인 2: DefaultErrorHandler 재시도 횟수 초과

`DefaultErrorHandler` 기본값: `FixedBackOff(0, 9)` = 10번 시도.  
`FETCH_MAX_WAIT_MS=500ms` × 10번 = ~5초 > 테스트 타임아웃 3초.

DLT 라우팅이 3초 내에 완료되지 못해 `latch2.await(3s)`가 timeout 반환.

## 디버깅 방법

Spring Kafka 로그에서 DLT 발행 성공 메시지 확인:
```
DEBUG DeadLetterPublishingRecoverer: Successful dead-letter publication: blc6-0@1 to blc6-dlt-0@0
```

`blc6-dlt-0@0` 형식 = `<topic>-<partition>@<offset>` → DLT 토픽이 `blc6-dlt`임을 알 수 있다.

## 수정 사항

### 1. DLT 토픽 이름 업데이트

```diff
-@EmbeddedKafka(..., topics = ["blc6", "blc6.DLT"])
+@EmbeddedKafka(..., topics = ["blc6", "blc6-dlt"])

-@KafkaListener(topics = ["blc6.DLT"], groupId = "blc6.DLT", ...)
+@KafkaListener(topics = ["blc6-dlt"], groupId = "blc6-dlt", ...)
```

### 2. 즉시 DLT 라우팅 설정

```diff
-val errorHandler = DefaultErrorHandler(DeadLetterPublishingRecoverer(template))
+val errorHandler = DefaultErrorHandler(DeadLetterPublishingRecoverer(template), FixedBackOff(0L, 0L))
```

`FixedBackOff(0L, 0L)`: interval=0ms, maxAttempts=0 → 첫 실패 즉시 DLT 라우팅.

### 3. 테스트 실행 순서 보장

`Listener5`는 Spring 싱글톤 빈으로 두 테스트가 상태를 공유한다.  
`conversion error`가 `conversion error routes to DLT` 전에 실행되어야 한다.

```kotlin
@TestMethodOrder(MethodOrderer.MethodName::class)
```

JUnit 5 MethodName 정렬은 `Method.getName()`을 사용 →  
`"conversion error"` < `"conversion error routes to DLT"` (prefix ordering).

## 테스트 결과

- `bluetape4k-kafka`: 6/6 pass ✓
- `bluetape4k-kafka4`: 6/6 pass ✓

## 교훈

1. **Spring Kafka 버전 업그레이드 시 DLT suffix 확인 필수**: 3.3부터 `.DLT` → `-dlt`.
2. **`DeadLetterPublishingRecoverer` 로그 확인**: `to <topic>-<partition>@<offset>` 패턴으로 실제 DLT 토픽 이름 파악 가능.
3. **`DefaultErrorHandler` 재시도 횟수**: 테스트 타임아웃보다 (재시도 × poll 대기시간)이 크면 항상 타임아웃 → `FixedBackOff(0L, 0L)` 사용.
4. **싱글톤 Spring 빈 공유 테스트**: CountDownLatch 상태 공유 시 실행 순서를 명시적으로 제어해야 한다.
