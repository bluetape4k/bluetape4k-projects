# #1474 `SuspendJCacheEntryEventListener` bounded admission 설계

## 문제와 목표

선행 작업 #1360은 `SuspendJCacheEntryEventListener`의
`CancellationException` 전파, `close()` lifecycle, 불변 이벤트 사본,
provider registration contract를 고정했다. 현재 구현은 JCache callback마다
`scope.launch`를 하나씩 만들기 때문에 callback burst가 크면 in-flight job 수가
burst 크기만큼 증가한다.

이번 작업의 목표는 callback fan-out의 admission 상한과 overflow 정책을
명시하고, accepted callback의 처리 의미와 기존 cancellation/close 계약을
유지하는 것이다.

## 현재 근거

| 근거 | 확인 결과 |
| --- | --- |
| live issue | [#1474](https://github.com/bluetape4k/bluetape4k-projects/issues/1474) 는 `OPEN`, 담당 `debop`, milestone `2.0.0`, labels `test`, `performance`, `tech-debt`, `cache`다. |
| 선행 구현 | `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/SuspendJCacheEntryEventListener.kt`가 callback마다 child job을 만들고 `close()`에서 `scope.cancel()`만 호출한다. |
| 등록 경로 | `cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCache.kt`가 `MutableCacheEntryListenerConfiguration`으로 listener를 등록한다. |
| 기존 설계 | `docs/superpowers/specs/2026-08-22-issue-1419-coroutine-contract-train-design.md`는 callback별 fan-out과 unbounded burst follow-up을 명시한다. |
| baseline | `repo-test-summary -- ./gradlew :bluetape4k-cache-core:test --tests 'io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListenerTest' --rerun-tasks` 결과 `12 passing`, `BUILD SUCCESSFUL`이다. |

## 범위와 비범위

### 범위

- listener 내부 callback admission cap을 추가한다.
- accepted/overflow-rejected 경계를 코드와 KDoc에 문서화한다.
- finite burst, in-flight job 수, accepted 처리, overflow 로그, close 후
  cooperative cancellation을 결정적으로 검증한다.
- 기존 listener event 종류별 front-cache 반영과 provider registration contract를
  회귀 검증한다.

### 비범위

- #1360의 `CancellationException` propagation, 불변 이벤트 사본,
  public one-argument constructor ABI 변경
- per-key coalescing, global ordering, backend가 지원하지 않는 강제 중단
- 새 public configuration API, dependency, module, metric backend
- release, tag, publish, integration branch merge

## 대안 검토

### 대안 A — bounded non-blocking admission (선택)

listener별 `Semaphore` permit 수를 in-flight callback job 상한으로 사용한다.
JCache callback은 동기 API이므로 permit을 기다리지 않고 `tryAcquire()`만
호출한다. permit을 얻은 callback만 child job을 만들고, 포화 상태의 callback은
즉시 거부한다. 내부 queue를 두지 않으므로 queue depth는 항상 0이고 callback
job 수 자체가 상한이 된다.

장점은 기존 callback별 child job과 child 단위 cancellation 전파를 유지하면서
unbounded fan-out만 제거하는 점이다. 단점은 포화 시 callback을 거부하므로
accepted가 아닌 event는 이 listener가 보장하지 않는다는 점이다. 이 거부는
조용한 drop이 아니라 명시적인 overflow 정책과 sanitized debug log로 드러낸다.

### 대안 B — bounded per-key coalescing

대기 중인 key별 최신 event만 유지하고 같은 key의 이전 event를 대체한다.
unique key가 cap을 넘을 때의 overflow와 created/updated/removed/expired
선형화 지점을 추가로 정의해야 하며, event ordering 의미가 바뀐다. #1360의
event 종류별 반영 의미를 보존하는 것보다 복잡도가 크므로 이번 작업에서는
채택하지 않는다.

### 대안 C — bounded channel + worker pool

bounded `Channel`에 callback batch를 넣고 고정 worker가 처리한다. queue에
accepted된 event가 worker cancellation으로 남을 수 있고, worker가
`CancellationException`으로 종료되는 기존 child 계약을 다시 설계해야 한다.
callback별 cancellation을 유지하면서 queue semantics까지 검증해야 하므로
대안 A보다 위험하다.

## 선택 설계

### Admission과 data flow

1. `onCreated`/`onUpdated`는 callback 반환 전에 iterable을 `EventCopy` map으로
   복사한다. `onRemoved`/`onExpired`는 key를 `LinkedHashSet`으로 복사한다.
2. 기존과 같은 count/type trace log를 기록한다. raw key, value, source는
   기록하지 않는다.
3. `closed == false`이고 `targetCache.isClosed() == false`인 경우에만
   admission을 시도한다. 이미 닫힌 target은 overflow가 아니라 pre-admission
   rejection이다.
4. `Semaphore.tryAcquire()` 성공을 accepted 선형화 지점으로 삼는다. permit이
   없으면 callback을 동기적으로 거부하고 cap과 operation을 포함한
   low-cardinality debug log를 남긴다. callback thread는 절대 기다리지 않는다.
5. accepted callback은 기존 `scope.launch` child 하나에서 한 번만
   `putAll` 또는 `removeAll`을 시도한다. child의 `finally`에서 permit을
   반환한다.

기본 cap은 listener 인스턴스당 `64`개 in-flight callback job이다. cap은
callback batch 수에 적용하며 batch 내부 entry 수에는 새 상한을 추가하지
않는다. public one-argument constructor는 그대로 유지하고, 작은 cap을
주입하는 경로는 `@JvmSynthetic internal forTest`에만 둔다.

### Event 종류별 반영

- created/updated: 불변 이벤트 사본을 `Map<K, V>`로 만들고
  `targetCache.putAll(...)`을 호출한다.
- removed/expired: 불변 key 사본을 `Set<K>`로 만들고
  `targetCache.removeAll(...)`을 호출한다.
- 같은 batch 안의 중복 key와 기존 `associate`/`LinkedHashSet` 결과는
  선행 구현과 동일하게 유지한다.
- 전역 ordering은 보장하지 않는다. permit 획득 순서나 dispatcher scheduling은
  API 계약이 아니다.

### Error와 lifecycle

- `CancellationException`은 broad catch보다 먼저 재전파한다. permit은
  `finally`에서 반환한다.
- 일반 `Exception`은 기존처럼 operation과 sanitized cache id만 error log로
  남기고 sibling child의 처리를 계속한다.
- `close()`는 `closed.compareAndSet(false, true)` 성공 시 `scope.cancel()`을
  호출한다. join, drain, blocking wait를 하지 않는다.
- admission 이후 `close()`와 경쟁하는 callback은 cooperative cancellation
  대상이다. close가 callback 완료를 보장하지 않는다는 #1360 계약을 유지한다.
- 한 child의 `CancellationException`은 `SupervisorJob` 아래 sibling child를
  취소하지 않는다. listener scope 자체가 취소되면 남은 callback도 함께
  취소된다.

### 관측성과 로그

overflow log에는 `operation`, sanitized `cache id`, admission cap만 포함한다.
key, value, event source의 `toString()` 결과와 raw payload는 포함하지 않는다.
이번 작업에는 public metrics API를 추가하지 않는다. 테스트는 target operation
호출 수, child job 수, barrier 상태, log appender를 사용해 accepted/overflow와
상한을 측정한다.

## 호환성

- `SuspendJCacheEntryEventListener(SuspendJCache<K, V>)` public constructor와
  기존 JVM descriptor를 유지한다.
- `SuspendNearJCache`의 `MutableCacheEntryListenerConfiguration` factory와
  listener provider registration 코드는 변경하지 않는다.
- 새 dependency/module/catalog 등록은 없다.
- overflow는 best-effort listener의 명시적 admission 정책이다. 포화 시 모든
  외부 event를 수용해야 하는 durable delivery 계약은 이번 listener가
  제공하지 않으며, 그 요구는 별도 설계가 필요하다.

## 실패 모드와 완화

| 실패 모드 | 관찰 가능한 결과 | 완화/검증 |
| --- | --- | --- |
| callback burst가 cap을 초과 | permit 없는 callback이 즉시 거부되고 job 수가 cap을 넘지 않음 | `runTest` barrier로 finite burst와 accepted/overflow 호출 수를 검증 |
| admission과 `close()`가 경쟁 | accepted child가 cooperative cancellation을 받고 close는 반환을 기다리지 않음 | `CompletableDeferred`/`awaitCancellation`으로 cancellation과 non-join을 검증 |
| target operation이 `CancellationException`을 던짐 | 해당 child만 취소되고 permit이 반환됨 | child 상태와 sibling 처리, `finally` permit 반환을 검증 |
| target operation이 일반 예외를 던짐 | error log 후 sibling은 계속 처리 | MockK `coAnswers`와 `coVerify`로 예외 격리를 검증 |
| callback iterable이 반환 후 변경됨 | 이미 복사한 불변 이벤트 사본만 front cache에 반영됨 | 기존 불변 이벤트 사본 회귀 테스트 유지 |
| overflow 로그에 payload가 섞임 | 민감한 key/value/source 노출 위험 | `ListAppender`에서 raw token 부재를 검증 |

## 수용 기준과 검증 매핑

| 이슈 수용 기준 | 설계 증거 | 검증 |
| --- | --- | --- |
| admission 상한/선형화/overflow 문서화 | `tryAcquire()` 성공 지점, cap `64`, 즉시 거부 정책, 본 spec/KDoc | bounded burst test와 source/KDoc review |
| accepted event 손실·중복 없음 | accepted callback당 child 하나, `putAll`/`removeAll` 한 번, permit 반환 | barrier 기반 call count와 child bound |
| raw payload 로그 없음 | sanitized cache id와 operation/cap만 로깅 | 기존 redaction + overflow log test |
| close 후 cooperative cancellation | `scope.cancel()` only, no join/drain | `awaitCancellation` child test |
| #1360 targeted/module gate 회귀 없음 | constructor/exception/불변 사본/event semantics 유지 | targeted test, module test, detekt/build |
| provider registration contract 유지 | `SuspendNearJCache` registration path 비변경 | near-cache registration contract test |

## 구현 순서와 stacked train

현재 branch `feat/issue-1474-spec`에는 이 설계 문서만 커밋한다.

다음 train은 아래 순서로 구성한다.

1. `feat/issue-1474-spec`: 이 spec과 설계 검증 증거
2. `feat/issue-1474-bounded-admission`: spec을 base로 listener와 회귀 테스트
3. `feat/issue-1474-verification`: 독립 검증, 문서/KDoc 보완, final checklist

각 branch는 `develop`을 최종 base로 하는 누적 head를 유지한다. PR 생성은
approved plan의 정확한 repository/base/head 권한을 fresh-read한 뒤 수행하며,
merge는 별도의 exact-head approval 없이는 수행하지 않는다.

## 설계 DoD

- [x] 문제, 현재 근거, 범위와 비범위가 명시됨
- [x] bounded admission, coalescing, channel 대안을 비교하고 admission을 선택함
- [x] 선형화 지점, overflow, event 반영, cancellation/close 계약이 명시됨
- [x] 실패 모드 6개와 완화/검증 방법이 매핑됨
- [x] public ABI, provider registration, dependency 범위가 고정됨
- [x] 수용 기준과 deterministic test/validation 명령이 매핑됨
- [x] stacked train과 PR/merge side-effect 경계가 명시됨

중지 조건은 다음 중 하나다. public ABI 또는 provider registration을 바꿔야
하거나, accepted event의 의미를 coalescing/order 보장으로 바꿔야 하거나,
bounded test가 상한·cancellation·accepted call count를 결정적으로 증명하지
못하면 구현을 중지하고 설계 승인을 다시 받는다.
