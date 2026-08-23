# Epic #1419 코루틴 계약 안정화 train 설계

## 1. 문서 상태

- 대상 Epic: [#1419 Flow·취소·종료 계약 안정화](https://github.com/bluetape4k/bluetape4k-projects/issues/1419)
- 대상 milestone: `2.0.0`
- 상태: 구현 전 설계 검토
- 기준 commit: `bd4f3c89bc1313e73691c934ce80fa625d1982e7` (`origin/develop`)
- 구현 방식: strict linear stacked PR train

이 문서는 네 child issue를 하나의 장기 branch에 섞지 않고, 앞 단계에서 확보한 회귀 oracle을 다음 단계가 상속하는 순차 train으로 전달하기 위한 계약을 정의한다.

## 2. 문제와 목표

현재 다음 네 영역은 각각 테스트가 통과하더라도 코루틴 경계의 핵심 계약을 충분히 증명하지 못한다.

1. `BufferedSuspendedSinkTest`는 모든 write overload의 실제 payload 대신 최종 buffer가 비어 있지 않다는 사실만 확인한다.
2. `BufferedResumableCollector`는 producer와 terminal signal의 동시 실행을 지원하지 않는다고 문서화하지만, 작은 buffer에서 suspend된 producer와 `complete`/`error`가 경합하는 동작을 고정하지 않는다.
3. `SuspendJCacheEntryEventListener`와 `SuspendNearJCache`는 `runCatching`으로 `CancellationException`까지 일반 실패처럼 처리하며, 테스트는 `Thread.sleep`에 의존한다.
4. NATS JetStream consumer에는 구조적 취소, 유한 backpressure, 리소스 정리를 함께 보장하는 `Flow<Message>` bridge가 없다.

목표는 payload 보존, terminal exactly-once, cancellation 전파, bounded backpressure를 각 단계의 결정적 회귀 테스트로 고정하고, 선행 단계가 green일 때만 다음 변경을 시작하는 것이다.

## 3. 범위와 비범위

### 3.1 범위

- #1341의 exact payload oracle과 flush/close 상태 검증
- #1349의 concurrent producer/terminal 상태 기계와 stress test
- #1360의 `CancellationException` 재전파 및 결정적 listener lifecycle test
- #1350의 JetStream push/pull cold Flow adapter, bounded backpressure, manual ack 및 cleanup 계약
- 각 단계의 KDoc, README, targeted test, 정적 분석과 module build
- Epic과 child issue의 `2.0.0` metadata 정합성

### 3.2 비범위

- 코루틴 라이브러리나 NATS Java client 버전 변경
- 새로운 Gradle module 또는 외부 dependency 추가
- JCache API의 공개 타입 재설계
- NATS consumer configuration의 생성·삭제 관리 API
- Flow source가 business handler 성공을 추론하는 자동 ack
- merge, tag, release, publish, branch 삭제

### 3.3 용어

- producer admission(생산자 수락): `next` 호출이 terminal과 경쟁해 queue 접근 권한을 얻는 atomic 전이
- terminal signal(종료 신호): 정상 완료 `complete` 또는 오류 완료 `error`
- idle timeout(유휴 제한 시간): message가 없어 polling receive가 `null`을 반환했지만 consumer는 계속 active인 상태
- normal completion(정상 완료): consumer가 stopped/finished/inactive여서 Flow가 예외 없이 끝나는 상태
- cancellation(취소): collector coroutine의 `CancellationException`이 primary cause가 되어 cleanup을 시작하는 상태

## 4. 확인된 현재 근거

### 4.1 저장소 근거

- `BufferedSuspendedSinkTest`의 overload test는 여러 write를 실행한 뒤 `buffer.size > 0L`만 확인한다.
- `BufferedResumableCollector`는 producer mutex와 별도의 `done`/`error` 상태를 사용하며 terminal과 in-flight producer를 하나의 직렬화 경계로 묶지 않는다.
- 같은 module의 `MulticastSubject`는 signal mutex로 in-flight emit과 terminal signal을 직렬화하고, 첫 terminal signal만 채택하는 선행 패턴을 제공한다.
- JCache listener의 네 callback은 독립 scope에서 `runCatching`을 사용하고, 테스트는 고정 sleep으로 완료를 추정한다.
- `infra/nats`의 coroutine 의존성은 이미 `compileOnly`로 노출되며, 현재 API는 drain과 단건 `nextMessage` 중심이다.
- Pulsar adapter는 caller가 consumer lifecycle과 ack를 소유하고 취소 시 대기 future를 중단하는 선행 계약을 제공한다.

### 4.2 NATS 2.26.1 source 근거

- `JetStream.subscribe(...)`는 동기 push 및 pull subscription을 생성한다.
- `Subscription.nextMessage(Duration)`는 dispatcher 소유가 아닌 active subscription에서 다음 message를 blocking 방식으로 반환한다.
- `ConsumerContext`는 pull consumer 전용이며 `next(...)`, `iterate(...)`, `consume(...)`를 제공한다.
- `MessageConsumer.stop()`은 새 pull 요청을 막지만 unsubscribe나 resource cleanup은 하지 않으며, `close()`가 underlying subject를 unsubscribe한다.
- `Message.ack()`, `nak()`, `term()`은 서로 다른 redelivery 결정을 표현한다.
- `PushSubscribeOptions.Builder.pendingMessageLimit(long)`과 `pendingByteLimit(long)`은 non-dispatched synchronous push queue의 상한을 설정하고, `SubscribeOptions.getPendingMessageLimit()`과 `getPendingByteLimit()`이 설정값을 제공한다.
- 생성된 `JetStreamSubscription`은 `Consumer`를 상속하므로 `getPendingMessageLimit()`, `getPendingByteLimit()`, `getDroppedCount()`로 실제 client queue 상한과 drop을 조회한다.

따라서 Flow adapter는 blocking receive를 `runInterruptible` 경계에서 실행하고, collector 취소 시 자신이 생성한 subscription 또는 consumer를 `finally`에서 닫아야 한다.

## 5. 선택한 train 구조

| 순서 | Issue | head branch | PR base | 분류 | 선행 gate |
| --- | --- | --- | --- | --- | --- |
| 1 | #1341 | `test/1341-buffered-sink-payload` | `develop` | Type B | 기준 테스트 green |
| 2 | #1349 | `fix/1349-buffered-collector-terminal-race` | `test/1341-buffered-sink-payload` | Type C | #1341 required CI green, blocker 0 |
| 3 | #1360 | `fix/1360-suspend-jcache-cancellation` | `fix/1349-buffered-collector-terminal-race` | Type C | #1349 required CI green, blocker 0 |
| 4 | #1350 | `feat/1350-nats-consumer-flow` | `fix/1360-suspend-jcache-cancellation` | Type B | #1360 required CI green, blocker 0 |

각 PR이 merge되면 다음 PR의 base를 `develop`로 바꾸고 최신 `origin/develop` 위에 restack한 뒤 required CI를 다시 실행한다. merge는 train 승인과 별개의 gate이며, exact head SHA, base, checks, review/thread, mergeability, metadata, DoD를 fresh-read한 뒤 별도 승인을 받는다.

### 5.1 Restack·rollback 운영 계약

- 단계별 PR 생성, merge, restack 시점에 head SHA, base SHA, CI run ID, predecessor merged SHA를 receipt로 남긴다.
- predecessor merge 뒤 downstream branch는 `--force-with-lease` 외의 강제 push를 금지한다. 예상 remote head가 다르거나 rebase conflict가 나면 push하지 않고 train을 `PENDING`으로 둔다.
- restack 뒤 targeted/module/required CI를 새 head에서 다시 실행한다. 이전 base의 green 결과는 재사용하지 않는다.
- predecessor merge 뒤 회귀가 발견되면 downstream 진행을 중단한다. 이미 merge한 commit의 revert는 별도 승인을 받아 처리하고, 승인 전에는 downstream branch와 worktree를 보존한다.
- worktree cleanup은 merge SHA가 `develop`에 포함되고 해당 worktree가 clean이며 unmerged commit이 없다는 사실을 증명한 대상에만 no-force로 수행한다. branch 삭제는 이 train 범위가 아니다.

## 6. 단계별 설계

### 6.1 #1341: BufferedSuspendedSink payload oracle

#### 변경 계약

- 각 write overload에 길이와 byte pattern이 서로 다른 sentinel을 사용한다.
- 호출 순서대로 만든 expected byte array와 delegate buffer의 전체 payload를 exact equality로 비교한다.
- `writeAll`은 source별 expected byte count와 실제 반환값을 각각 비교한다.
- `flush()` 전에는 buffered sink의 보류 상태를, `flush()` 뒤에는 delegate 반영 상태를 비교한다.
- `close()` 뒤에는 남은 payload가 모두 반영되고 delegate가 닫힌 상태임을 검증한다.

#### 변경 범위

- 주 변경 파일: `io/okio/src/test/kotlin/io/bluetape4k/okio/coroutines/BufferedSuspendedSinkTest.kt`
- production code는 새 regression test가 실제 결함을 드러낼 때만 최소 수정한다.

#### 회귀 oracle

- 임의 overload 한 개의 delegate write를 no-op으로 만든 mutation이 exact payload assertion을 실패시켜야 한다.
- size가 같고 내용만 다른 payload도 실패해야 한다.

### 6.2 #1349: BufferedResumableCollector terminal arbitration

#### 상태 계약

collector lifecycle을 다음 논리 상태로 고정한다.

```text
Open -> Completing -> Completed
Open -> Failing(error) -> Failed(error)
Open -> Cancelled(cause)
```

- 첫 terminal signal이 승자다. 이후 `complete`/`error`는 상태와 terminal cause를 바꾸지 않는 no-op이다.
- `complete`와 `error`의 기존 non-suspending signature를 유지한다. terminal 함수는 mutex를 기다리지 않고 atomic state에서 첫 terminal을 즉시 선점한다.
- atomic state는 terminal kind와 admission된 producer 수를 함께 보관한다. `next`는 `Open(n) -> Open(n + 1)` CAS에 성공해야 producer mutex에 진입하며, terminal 전 admission된 producer가 모두 반환하거나 명시적 실패로 끝날 때까지 drain은 종료하지 않는다.
- terminal은 value 대기와 capacity 대기 양쪽을 모두 깨운다. terminal을 관찰한 blocked producer는 아직 enqueue하지 않은 값을 `IllegalStateException`으로 거부하고 admission count를 감소시킨다.
- terminal 전 성공한 `queue.offer`를 value acceptance의 선형화 지점으로 정의한다. 이 시점부터 값은 enqueue 순서대로 drain되며, terminal과 교차해 offer가 성공한 경우에도 admission count가 0이 되기 전 drain이 종료하지 않으므로 값이 보존된다.
- `error`가 승리하면 buffered 값을 모두 전달한 뒤 동일한 error를 collector에 전파한다.
- terminal 상태 뒤 새 `next`와 terminal 때문에 enqueue하지 못한 blocked `next`는 `IllegalStateException`으로 즉시 거부한다. `queue.offer` 성공 뒤에는 cancellation을 다시 검사하지 않고 `next`를 반환하므로, accepted value가 enqueue됐지만 같은 호출이 cancellation으로 실패하는 ghost enqueue를 만들지 않는다.
- admission CAS가 성공한 producer는 mutex/capacity 대기, terminal wake-up, `queue.offer`, cancellation 중 어느 경로로 끝나도 `finally`에서 active admission count를 정확히 한 번 감소시킨다. mutex 또는 capacity 대기 중 cancellation이 먼저 관찰되면 값을 enqueue하지 않고 `CancellationException`을 그대로 전파한다.
- downstream collector가 취소되면 `CancellationException`을 유지하고 suspend된 producer를 깨운다.
- producer mutex를 잡은 채 downstream collector를 호출하지 않는다.
- `error(null)`은 first-terminal arbitration에는 참여하지만 downstream 관찰 결과는 정상 완료와 같다. 내부 terminal kind는 `complete`와 구분해 후속 terminal이 상태를 덮어쓰지 못하게 한다.

| 경합 순서 | 선형화와 관찰 결과 |
| --- | --- |
| terminal CAS가 producer admission CAS보다 먼저 성공 | producer는 admission에 실패하고 `IllegalStateException`을 받는다. |
| producer admission 뒤 `queue.offer`가 terminal CAS보다 먼저 성공 | `next`는 accepted value로 정상 반환한다. terminal 상태의 active count가 0이 될 때까지 drain은 종료하지 않고 해당 값을 전달한다. |
| producer admission 뒤 terminal CAS가 `queue.offer`보다 먼저 성공 | terminal이 capacity waiter를 깨우고 producer는 값을 enqueue하지 않은 채 `IllegalStateException`으로 끝난 뒤 active count를 감소시킨다. |
| producer cancellation이 mutex/capacity 대기 또는 terminal 거부와 경합하고 `queue.offer`는 아직 성공하지 않음 | 값을 enqueue하지 않고 `CancellationException`을 우선 전파하며 `finally`에서 active count를 감소시킨다. |
| `queue.offer` 성공과 producer cancellation이 경합 | `queue.offer` 성공이 먼저 선형화되면 값을 accepted로 보존하고 cancellation 재검사 없이 `next`를 반환한다. cancellation이 먼저 관찰되면 offer를 수행하지 않고 `CancellationException`으로 끝난다. 두 경로 모두 `finally`에서 active count를 정확히 한 번 감소시킨다. |

first terminal은 atomic state CAS에 성공한 호출이다. `error(null)`도 이 경쟁에서는 terminal이지만 caller가 정상 완료와 구분할 수 없으므로 새 코드에서는 `complete()` 사용을 권장한다.

#### 구현 방향

- 현재 `done` boolean과 별도 error field 대신 terminal kind, cause, active admission count를 포함한 단일 atomic 상태를 사용한다.
- `MulticastSubject`의 first-terminal-wins 패턴을 재사용하되 non-suspending terminal API와 buffered producer wake-up에 맞게 CAS 상태 기계로 구현한다.
- producer mutex는 queue의 single-producer 조건만 보호한다. terminal 함수는 이 mutex를 획득하지 않는다.
- admission 이후의 producer 본문은 active count 감소를 `finally` 한 곳에서 수행하고, 성공한 `queue.offer` 뒤에는 cancellation point나 `ensureActive`를 두지 않는다.
- busy wait, unbounded retry, timeout 기반 정확성은 사용하지 않는다.

#### 검증

- capacity 1과 다수 producer에서 `next` 대 `complete`, `next` 대 `error`, `complete` 대 `error(null)` 경합을 반복한다.
- drain이 없는 full-buffer 상태에서도 terminal 함수와 blocked producer가 bounded time 안에 반환 또는 명시적 실패하며 교착하지 않음을 검증한다.
- barrier와 virtual-time primitive로 경합 지점을 통제하며 `delay`나 `Thread.sleep`으로 순서를 추정하지 않는다.
- barrier로 admission 뒤 mutex/capacity 대기 cancellation과 `queue.offer` 직후 cancellation을 각각 재현해, 전자는 미전달과 `CancellationException`, 후자는 accepted value 전달과 정상 `next` 반환을 검증한다. 두 경우 모두 active count가 0이 되어 drain이 종료하는지도 확인한다.
- 수집 값, terminal count, terminal cause, producer 완료/취소를 모두 assertion한다.

### 6.3 #1360: JCache cancellation과 listener lifecycle

#### 실패 분류 계약

- `CancellationException`은 모든 callback, `clearAll`, `close` 경계에서 즉시 재전파한다.
- ordinary backend failure는 현재의 fire-and-forget listener 정책을 유지해 log로 관찰한다.
- `clearAll`/`close`의 ordinary failure를 삼키는 기존 caller contract는 이번 train에서 바꾸지 않는다. 다만 cancellation과 ordinary failure를 명시적 `try/catch`로 분리한다.

#### scope와 close 계약

- 공개 one-argument constructor의 source/API/ABI를 유지한다.
- production 기본 scope는 기존 `SupervisorJob + Dispatchers.IO` 의미를 유지한다. callback 하나당 job 하나를 만드는 기존 fan-out도 이번 cancellation-focused 변경에서는 보존하며, 무제한 burst admission 개선은 별도 후속 범위다.
- test 전용 internal seam으로 scope를 주입해 `runTest` scheduler가 callback 완료를 통제하게 한다.
- callback은 provider가 넘긴 mutable event iterable을 동기적으로 immutable key/value 이벤트 사본으로 바꾼 뒤 비동기 작업을 시작한다.
- listener-local atomic closed gate의 `false -> true` CAS를 close linearization point로 삼는다. callback은 이벤트 사본 생성 뒤 gate를 확인하고, close와 launch가 교차하면 cancelled scope에 생성된 job body가 target cache를 호출하지 않게 한다.
- `close()`는 idempotent이며 closed gate를 선점한 뒤 listener scope에 cancellation을 요청하고 즉시 반환한다. suspend 함수가 아니므로 active callback의 join 완료까지 기다리지 않는다.
- close 뒤 도착한 callback은 무시한다. close 전에 이미 target cache 호출을 시작한 callback은 cooperative cancellation 지점에서 종료하며, cancellation을 지원하지 않는 backend 호출의 완료까지 close가 기다린다고 보장하지 않는다.
- callback 사이의 전역 ordering은 기존 구현에서도 보장되지 않으므로 새 계약으로 추가하지 않는다. 한 callback batch 안의 이벤트 사본 순서와 각 event 종류의 front/back invalidation 의미만 유지한다.
- listener log에는 raw key/value/source를 기록하지 않고 event 종류, 건수, sanitized cache identifier만 기록한다.

#### 결정적 검증

- `Thread.sleep`을 제거하고 `TestScope`, `advanceUntilIdle`, barrier/deferred를 사용한다.
- created/updated/removed/expired callback별 batch 순서와 front/back invalidation 결과를 유지한다.
- cancellation, ordinary failure logging, close 전후 callback admission을 별도 case로 검증한다.
- close 재호출, close 직후 callback 무시, 이미 시작한 cooperative callback cancellation을 각각 검증한다.
- provider event iterable을 callback 반환 뒤 변경해도 동기 생성한 이벤트 사본이 유지됨을 검증한다.
- bounded burst test로 close latency와 job cancellation을 관찰한다. fan-out 상한 자체는 별도 follow-up으로 추적한다.
- #1360 merge 전 bounded admission/coalescing을 다루는 후속 issue를 등록하고, 이번 train에서는 기존 fan-out 보존과 측정 결과를 명시적 잔여 위험으로 남긴다.

### 6.4 #1350: NATS JetStream cold Flow

#### 공개 API 방향

두 source family를 명시적으로 분리한다.

1. `JetStream` 기반 push adapter는 collection 시점에 synchronous push subscription을 생성한다.
2. `ConsumerContext` 기반 pull adapter는 collection 시점에 `IterableConsumer`를 생성한다. callback 기반 `MessageConsumer`는 handler가 suspend할 수 없어 이번 API에서 사용하지 않는다.

공개 이름은 기존 Kotlin adapter 관례에 맞춰 `consumeAsFlow`를 사용한다. overload는 receiver가 달라 구분하며, NATS Java options 타입을 그대로 받아 새로운 configuration abstraction을 만들지 않는다.

```kotlin
fun JetStream.consumeAsFlow(
    subject: String,
    options: PushSubscribeOptions = defaultNatsFlowPushOptions,
    capacity: Int = 64,
    receiveTimeout: Duration = 1.seconds,
): Flow<Message>

fun ConsumerContext.consumeAsFlow(
    options: ConsumeOptions = ConsumeOptions.DEFAULT_CONSUME_OPTIONS,
    capacity: Int = 64,
    receiveTimeout: Duration = 1.seconds,
): Flow<Message>
```

- `capacity`는 `1..1024`, `receiveTimeout`은 `100.milliseconds` 이상만 허용한다. `defaultNatsFlowPushOptions`는 pending message 1024개와 pending payload 16 MiB를 상한으로 사용한다.
- push options의 pending 상한은 message `1..65_536`, byte `1..64 MiB`만 허용한다. 이 범위를 벗어난 oversized options는 subscription을 만들기 전에 거부한다.
- push options는 synchronous push subscription을 표현해야 한다. dispatcher/callback 소비와 pull options 조합은 이 overload에서 지원하지 않는다.
- `NatsConsumerFlowException`은 `val droppedMessages: Long`과 nullable `cause`를 보존하는 additive public exception이다. pure drop이면 cause는 `null`이고, status/read-back 실패와 drop이 함께 관찰되면 원래 실패를 cause로 둔다.
- cold는 “수집할 때 handle을 만든다”는 뜻이며 replay를 뜻하지 않는다. 같은 Flow instance의 동시 collect는 collection 시작 시 atomic gate로 검출해 두 번째 collector를 handle 생성 전에 `IllegalStateException`으로 거부한다. 같은 durable consumer에서 별도로 생성한 Flow instance 사이의 충돌은 전역 registry를 만들지 않고 jnats/server의 원래 예외를 전달한다. 한 durable consumer당 active collector 하나는 caller 책임이다.
- public API는 blocking dispatcher를 노출하지 않는다. internal implementation seam만 `Dispatchers.IO`를 test dispatcher로 교체할 수 있게 한다.

#### cold·backpressure 계약

- 각 collect는 독립 subscription/consumer를 생성하는 cold Flow다.
- blocking `nextMessage`와 `ConsumerContext`의 동기 receive는 `runInterruptible(Dispatchers.IO)` 안에서 실행한다.
- pull adapter는 channel의 `capacity`개와 receiver coroutine이 channel 전송을 기다리며 보유할 수 있는 1개를 합쳐 최대 `capacity + 1`개 message를 client-side에서 보유한다. byte 최악값은 `(capacity + 1) × server max payload`다.
- push adapter는 jnats pending queue의 message/byte limit에 pull과 같은 adapter channel·receiver 보유분을 더한다. 따라서 client-side 상한은 message 기준 `pendingMessageLimit + capacity + 1`, byte 기준 `pendingByteLimit + (capacity + 1) × server max payload`다.
- overflow 전략은 `BufferOverflow.SUSPEND`로 고정하고 drop 정책을 공개 parameter로 노출하지 않는다.
- channel이 가득 차면 receiver coroutine이 다음 message를 요청하지 않는다. pull consumer에는 직접 backpressure가 된다.
- push subscription은 adapter channel과 별도의 jnats pending queue를 가진다. adapter는 `PushSubscribeOptions`의 pending message/byte limit이 모두 양수·유한인지 검증하고, 생성된 subscription의 실제 limit을 read-back한다.
- push adapter는 subscription 생성 직후 dropped baseline을 읽고, 각 receive 전후와 cleanup의 unsubscribe 뒤에 final dropped count를 읽는다. 어느 검사에서든 증가하면 silent loss로 계속하지 않고 증가량을 포함한 `NatsConsumerFlowException`으로 Flow를 실패시킨다.
- adapter의 bounded 보장은 Flow channel, receiver가 보유한 1개 message, push client pending queue까지다. server `maxAckPending`은 전달됐지만 아직 ack되지 않은 message를 포함하는 별도 in-flight 상한이며 caller가 durable/ephemeral consumer에 설정해야 한다. README/KDoc는 adapter별 상한과 이 독립된 server 상한을 분리해 설명한다.
- message order는 단일 receiver coroutine의 receive 순서를 유지한다.

#### 취소와 종료 계약

- collector 취소는 대기 중인 blocking receive를 interrupt한다. push adapter가 만든 `JetStreamSubscription`은 unsubscribe하고, pull adapter가 만든 `IterableConsumer`는 close한다.
- cleanup 중 `CancellationException`은 원래 cancellation을 가리지 않는다.
- push의 `nextMessage(receiveTimeout)`이 `null`이면 `subscription.isActive`가 true일 때 idle timeout으로 계속하고 false일 때 정상 완료한다.
- pull의 `IterableConsumer.nextMessage(receiveTimeout)`이 `null`이면 `isStopped || isFinished`일 때 정상 완료하고 그 외에는 idle timeout으로 계속한다.
- 정상 stop/close는 Flow 완료로, connection/consumer status failure와 detected drop은 예외로 전달한다.
- cleanup ordinary failure는 수집 실패가 없을 때만 terminal error가 되며, 기존 수집 실패가 있으면 suppressed exception으로 보존한다.
- cleanup 순서는 receiver 취소, adapter-owned handle close/unsubscribe, cleanup error 결합 순이다. adapter가 생성하지 않은 `Connection`, `JetStream`, `ConsumerContext`, durable consumer configuration은 닫거나 삭제하지 않는다.

동시에 여러 종료 원인이 관찰되면 다음 우선순위를 적용한다.

| 우선순위 | 관찰 원인 | primary/suppressed 계약 |
| --- | --- | --- |
| 1 | collector cancellation 또는 receive interruption의 `CancellationException` | 항상 primary다. ordinary receive/collector, drop, cleanup 실패를 관찰 순서대로 suppressed로 붙인다. |
| 2 | ordinary receive 또는 downstream collector 실패 | cancellation이 없을 때 primary다. drop과 cleanup 실패를 suppressed로 붙인다. |
| 3 | dropped count 증가 또는 final drop read-back 실패 | 앞선 원인이 없을 때 `NatsConsumerFlowException`이 primary다. pure drop은 `droppedMessages > 0`, read-back 실패는 cause를 보존하며 cleanup 실패를 suppressed로 붙인다. |
| 4 | ordinary cleanup 실패 | 다른 원인이 없을 때만 primary terminal error다. |

동일 우선순위의 실패는 먼저 관찰한 실패를 primary로 유지한다. cleanup은 primary를 바꾸지 않고 suppressed 결합만 수행한다.

#### ack와 redelivery 계약

- 기본 및 유일한 source-level 정책은 manual ack다. Flow는 message를 그대로 전달하며 `ack`, `nak`, `term`을 호출하지 않는다.
- source-level auto ack는 제공하지 않는다. bounded channel에 성공적으로 넣었다는 사실은 business handler 성공을 뜻하지 않기 때문이다.
- caller가 처리 성공 뒤 `Message.ack()`, 재시도 가능한 실패에서 `nak()` 또는 무응답, 재전달 금지에서 `term()`을 선택하도록 README/KDoc 예제를 제공한다.
- 처리 실패 시 ack하지 않으면 server consumer policy에 따라 redelivery됨을 통합 테스트로 고정한다.
- ack 자체 실패는 adapter 검증이 아니라 caller-owned collect action 통합 테스트에서 원래 NATS exception으로 전달됨을 확인한다.

#### dependency 계약

- 공개 반환 타입에 `Flow<Message>`가 포함되므로 coroutine dependency가 public API signature에 나타난다.
- #1350에서 coroutine dependency를 `compileOnly`에서 `api`로 바꿔 published metadata가 `org.jetbrains.kotlinx:kotlinx-coroutines-core`를 전달하게 한다. 현재 resolved version은 `1.11.0`이며 중앙 catalog/BOM을 version authority로 유지한다.
- consumer compile/runtime fixture는 별도 coroutine 직접 선언 없이 published metadata만으로 두 overload를 compile하고 collect할 수 있음을 검증한다.
- `infra/nats/README.md`와 `README.ko.md`에는 one-active-collector, manual ack, `maxAckPending`, pending-limit/drop exception 예제를 함께 제공한다.

```kotlin
consumerContext.consumeAsFlow(capacity = 64)
    .collect { message ->
        try {
            process(message.data)
            message.ack()
        } catch (retryable: RetryableException) {
            message.nak()
            throw retryable
        }
}
```

README/KDoc에는 pull 예제 외에도 finite `PushSubscribeOptions`, server `maxAckPending`, `NatsConsumerFlowException.droppedMessages` 확인, retryable `nak()`와 non-retryable `term()`을 포함한 push 예제를 제공한다.

#### 검증

- local unit test: cold creation, 동일 Flow 동시 collect 거부, capacity/timeout/pending-limit 경계와 oversized rejection, push/pull별 보유 상한, order, idle timeout, cancellation cleanup, 우선순위별 primary/suppressed exception.
- NATS Testcontainers: push/pull order, 작은 capacity backpressure, 마지막 receive 뒤 overrun을 포함한 push drop 실패, caller-owned manual ack, ack 누락 redelivery, connection/consumer failure.
- Testcontainers-backed module은 다른 worktree/module과 병렬 실행하지 않는다.

## 7. 호환성과 migration

- #1341은 test-only이며 runtime/API 영향이 없다.
- #1349는 기존 문서상 지원하지 않던 concurrent terminal을 정의하므로 source compatibility는 유지하되 lifecycle behavior가 강화된다.
- #1360은 `CancellationException`을 더 이상 삼키지 않는다. 정상적인 structured concurrency 복원이며 ordinary failure의 기존 관찰 방식은 유지한다.
- #1350은 additive API다. 기존 drain/nextMessage API는 변경하지 않는다.
- NATS Flow 사용자는 collection scope가 subscription/consumer lifecycle을 소유함과 manual ack 책임을 명시적으로 받아들여야 한다.
- #1360의 cancellation 전파 변경과 #1350의 Flow API, manual ack, `api` coroutine dependency는 root `CHANGELOG.md`의 `Unreleased`와 양쪽 NATS README에 migration 예제로 기록한다.

## 8. 실패 모드와 방어

| 실패 모드 | 방어 | 증거 |
| --- | --- | --- |
| sink overload 하나가 잘못된 bytes를 기록하지만 다른 write 때문에 test가 통과 | 고유 sentinel과 전체 concatenated payload exact equality | mutation regression |
| producer가 buffer 대기 중 terminal이 먼저 닫혀 값 손실 또는 교착 | atomic admission count, terminal wake-up, 미수락 값의 명시적 실패 | barrier stress test |
| error가 buffered 값보다 먼저 전달 | drain 후 terminal cause 전파 | ordered collection assertion |
| listener가 cancellation을 ordinary failure로 삼킴 | 명시적 `catch (ce: CancellationException) { throw ce }` | cancellation test |
| close 뒤 listener callback이 새 작업 시작 | atomic closed gate, 동기 이벤트 사본, cancelled scope | deterministic close race test |
| NATS collector 취소 뒤 receive thread 또는 subscription 누수 | `runInterruptible`와 `finally` cleanup | job/thread/resource assertion |
| 느린 push collector에서 pending queue overrun이 message를 조용히 drop | finite channel/client limits, limit read-back, dropped count failure | small-limit overrun integration test |
| Flow가 처리 전에 auto ack하여 실패 message가 유실 | manual ack only | redelivery integration test |
| cleanup exception이 원래 수집 실패를 가림 | primary/suppressed exception 규칙 | dual-failure unit test |

## 9. 검증 행렬

| 단계 | targeted | module | 정적 검증 | 추가 gate |
| --- | --- | --- | --- | --- |
| #1341 | `BufferedSuspendedSinkTest` | `:bluetape4k-okio:test` | detekt 대상 확인, `git diff --check` | mutation oracle |
| #1349 | `BufferedResumableCollectorTest` | `:bluetape4k-coroutines:test` | detekt, public KDoc | repeated race test |
| #1360 | listener/NearJCache tests | `:bluetape4k-cache-core:test` | detekt, no `Thread.sleep` | virtual-time lifecycle |
| #1350 | NATS Flow unit/integration tests | `:bluetape4k-nats:test` | detekt, README/KDoc | sequential Testcontainers |

각 단계는 targeted test에서 시작해 module test와 detekt로 넓힌다. 전체 `clean build`는 train 마지막 단계에서 실행하되, 환경 또는 선행 baseline failure는 product failure와 분리해 기록한다. skipped/path-filtered CI는 검증 통과로 간주하지 않는다.

현재 PR CI path filter는 `infra/nats/**`를 `search-messaging` 검증에 연결하지 않으므로 #1350 변경에 포함해 `.github/workflows/ci.yml`의 path filter, 해당 test job, summary/coverage 연결을 함께 보강한다. #1350 exact head에서 NATS job이 실제 실행되어 green이고 summary가 이를 포함해야 train gate를 통과한다. skipped, pending, 다른 head의 Nightly 결과는 통과 증거가 아니다.

## 10. 대안과 기각 사유

### 10.1 네 child를 sibling PR로 병렬 진행

모듈 충돌은 적지만 Epic이 요구한 “이전 단계의 coroutine test oracle green 후 다음 단계 진행”을 증명하지 못한다. 테스트 자원을 병렬 점유하고 terminal/cancellation 계약이 서로 다른 시점에 분산되므로 기각한다.

### 10.2 하나의 aggregate PR

한 PR에서 모든 테스트를 볼 수 있으나 Okio, core coroutines, JCache, NATS 변경이 섞여 review와 rollback 경계가 넓어진다. child issue별 exact evidence와 독립 merge 판단이 약해져 기각한다.

### 10.3 NATS callbackFlow에서 dispatcher callback이 직접 send

Java callback은 suspend할 수 없어 `trySend`는 drop/failure를 만들고 `runBlocking`은 dispatcher thread를 막는다. synchronous receive를 interruptible producer coroutine에서 실행하는 방식보다 backpressure와 cancellation이 불명확해 기각한다.

### 10.4 NATS source-level auto ack

channel enqueue 또는 downstream `emit` 반환은 business transaction 성공과 동치가 아니다. 처리 전 ack로 redelivery를 잃을 수 있어 manual ack only로 결정한다.

## 11. Acceptance criteria

- [ ] #1341에서 모든 write overload의 exact payload, `writeAll` count, flush/close 상태가 검증된다.
- [ ] #1349에서 작은 buffer와 다수 producer의 terminal 경합이 accepted value 손실·교착 없이 first-terminal-wins로 종료된다.
- [ ] #1349에서 buffered 값 이후 error 전파와 collector cancellation이 결정적으로 검증된다.
- [ ] #1360에서 callback, `clearAll`, `close`가 `CancellationException`을 재전파한다.
- [ ] #1360에서 고정 sleep 없이 callback batch ordering, immutable 이벤트 사본, close lifecycle이 검증된다.
- [ ] #1350에서 push/pull cold Flow의 순서, bounded adapter/client queue, drop detection, 취소 cleanup이 검증된다.
- [ ] #1350에서 manual ack 성공, ack 누락 redelivery, ack failure가 검증된다.
- [ ] 공개 KDoc/README와 coroutine dependency 계약이 구현과 일치한다.
- [ ] #1350 exact head의 PR CI가 `infra/nats/**` 변경으로 NATS `search-messaging` 검증을 실제 실행하고 summary에 반영한다.
- [ ] `CHANGELOG.md`, `infra/nats/README.md`, `infra/nats/README.ko.md`가 cancellation 변경과 Flow migration 계약을 설명한다.
- [ ] 각 PR은 predecessor required CI green과 blocker 0을 확인한 뒤 생성한다.
- [ ] merge 전 exact-head fresh-read와 별도 승인을 받는다.

## 12. 독립 설계 리뷰 결과

| 관점 | 최종 P0 | 최종 P1 | 핵심 확인 |
| --- | ---: | ---: | --- |
| 성능 | 0 | 0 | bounded NATS queue와 drop 검출, JCache burst 측정 경계를 확인했다. |
| 안정성 | 0 | 0 | terminal/cancellation/close 상태 기계와 NATS source contract를 확인했다. |
| 보안 | 0 | 0 | raw key/value/source log 제거, pending 상한, cleanup 경계를 확인했다. |
| 운영 | 0 | 0 | restack receipt, exact-head CI, `infra/nats/**` path gate를 확인했다. |
| 개발자/API | 0 | 0 | 공개 signature, `api` coroutine dependency, producer cancellation 선형화를 확인했다. |
| 호출자/사용자 | 0 | 0 | 기본값, lifecycle, ack, drop exception, 실패 우선순위를 확인했다. |

main integration 결과 관점 사이의 P0/P1 충돌은 없다. 의도적으로 남긴 위험은 JCache callback fan-out 상한뿐이며, #1360 merge 전에 bounded admission/coalescing 후속 issue를 등록하는 gate로 분리했다.

## 13. DoD Status

- [x] Epic/child live state와 `2.0.0` milestone을 확인했다.
- [x] strict linear stacked train의 head/base와 선행 gate를 고정했다.
- [x] 기존 저장소 패턴과 NATS 2.26.1 source contract를 확인했다.
- [x] 네 단계의 lifecycle, failure, compatibility, verification contract를 작성했다.
- [x] 6개 독립 관점 설계 리뷰에서 P0/P1을 모두 해소했다.
- [ ] 사용자 설계 리뷰를 통과한다.
- [ ] child별 구현 계획을 작성한다.
- [ ] 네 PR을 구현·검증·생성한다.
- [ ] exact-head merge 승인과 merge 후 sync/cleanup을 단계별 수행한다.

현재 stop condition은 이 명세의 사용자 리뷰 완료다. 구현은 그 다음 gate에서 시작한다.
