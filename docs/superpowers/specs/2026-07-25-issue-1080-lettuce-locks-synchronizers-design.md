# Issue #1080 Lettuce Lock 및 동기화 객체 설계 명세

## 1. 문서 상태

- 대상 이슈: [#1080 Add Redisson-style locks and synchronizers for Lettuce](https://github.com/bluetape4k/bluetape4k-projects/issues/1080)
- 대상 저장소: `bluetape4k-projects`
- 작업 유형: Type A Full Feature
- 작업 브랜치: `codex/issue-1080-lettuce-locks-design`
- 기준 브랜치: `origin/develop`
- 기준 커밋: `067ea7337b5864030067d41e68e68b30054fc0a3`
- 구현 상태: 미시작
- 승인된 전달 구조: Lock family → Synchronizer family → API convergence

이 문서는 #1080 전체의 아키텍처와 공통 계약을 고정한다. 구현은 세 delivery로
나누지만, 각 delivery가 임의의 로컬 API를 발명하지 않도록 전체 public surface와
통일 기준을 먼저 정의한다.

## 2. 문제와 목표

`infra/lettuce`에는 이미 다음 Redis coordination primitive가 있다.

- `lock/LettuceLock`, `LettuceSuspendLock`: token-checked single-key mutex
- `lease/LettuceFencingLease`, `LettuceSuspendFencingLease`: monotonic fencing token
- `lease/LettuceMultiKeyLease`, `LettuceSuspendMultiKeyLease`: same-slot multi-key lease
- `semaphore/LettuceSemaphore`, `LettuceSuspendSemaphore`: permit coordination

이 primitive들은 개별 안전 계약은 강하지만 caller가 목적에 맞는 lock 또는 synchronizer를
선택하는 하나의 public mental model은 제공하지 않는다. 반면 Redisson은 `Lock`,
`FairLock`, `FencedLock`, `ReadWriteLock`, `SpinLock`, `MultiLock`, `Semaphore`,
`PermitExpirableSemaphore`, `CountDownLatch`를 일관된 객체군으로 제공한다.

목표는 Redisson의 객체 분류와 capability를 참조해 Lettuce 기반 객체군을 제공하되,
Java thread identity나 Redisson 내부 runtime을 복제하지 않고 Bluetape의 명시적인
owner, result, failure, cancellation 계약을 유지하는 것이다.

## 3. 현재 근거

### 3.1 저장소 근거

- 기존 fencing lease는 operation별 sealed result, owner ID, token, epoch, backend failure
  classification을 제공한다.
- 기존 multi-key lease는 standalone/cluster connection, codec wire byte 기반 slot 검증,
  owner token replay, ambiguous completion reconciliation, EVALSHA/NOSCRIPT fallback을
  제공한다.
- 기존 semaphore는 permit counter, owner token, expiry sorted set과 sync/async/suspend
  API를 제공하지만 boolean/exception 중심 API와 local permit bookkeeping을 사용한다.
- 기존 single-key lock은 non-reentrant `SET NX PX`와 token-checked Lua unlock을 사용한다.
- 기존 테스트는 fencing, multi-key lease, cancellation, recovery, Cluster slot,
  script fallback의 재사용 가능한 fixture를 포함한다.

### 3.2 외부 근거

- [Redisson locks and synchronizers](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/)
  는 lock과 synchronizer의 객체 분류, lease/watchdog, fair lock, fencing token,
  expirable permit 및 latch mental model을 제공한다.
- [Redisson `RFencedLock`](https://github.com/redisson/redisson/blob/master/redisson/src/main/java/org/redisson/api/RFencedLock.java)
  은 lock capability에 fencing token 획득을 추가한다.
- [Redis distributed locks guidance](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/)
  는 lease 만료 뒤 오래된 holder가 작업을 계속할 수 있음을 경고하며 downstream fencing을
  별도 안전 경계로 다룬다.
- [Lettuce issue #150](https://github.com/redis/lettuce/issues/150)는 Lettuce core가
  higher-level distributed lock을 제공하지 않는다는 경계를 보여 준다.

### 3.3 채택, 변환, 거절

| 구분 | 결정 |
|---|---|
| 채택 | Redisson의 객체군, capability 이름, lock/synchronizer 분리, lease/watchdog 선택지 |
| 변환 | thread ownership을 logical owner 및 acquisition handle로 변환 |
| 변환 | boolean/exception 중심 결과를 명시적인 sealed result와 backend failure로 변환 |
| 재사용 | 기존 fencing lease, multi-key lease, semaphore script 및 failure fixture |
| 거절 | Redisson 내부 command executor, pub/sub/watchdog 구현의 기계적 복제 |
| 거절 | coroutine ownership을 Java thread ID로 모델링 |
| 거절 | 첫 delivery만 보고 공통 API를 임의로 확정하거나 이미 명세한 의미를 convergence에서 다시 설계 |

## 4. 승인된 전달 전략

### 4.1 Delivery 1: Lock family

다음 객체와 lock에 필요한 neutral coordination runtime을 제공한다.

- `LettuceDistributedLock`
- `LettuceFairLock`
- `LettuceFencedLock`
- `LettuceReadWriteLock`
- `LettuceSpinLock`
- `LettuceMultiLock`

이 delivery는 reentrancy, logical owner, acquisition generation, lease/watchdog,
fairness, fencing, read/write compatibility, spin backoff 및 same-slot atomicity를
완결한다.

### 4.2 Delivery 2: Synchronizer family

다음 객체를 제공한다.

- `LettuceDistributedSemaphore`
- `LettucePermitExpirableSemaphore`
- `LettuceCountDownLatch`

이 delivery는 lock의 reentrancy나 lock ownership hierarchy를 상속하지 않는다. Permit ID,
permit lease, permit count 및 latch generation을 자체 public contract로 정의하고,
script execution, slot validation, failure classification 같은 neutral runtime만 재사용한다.

### 4.3 Delivery 3: API convergence

새 기능을 추가하지 않는다. 두 객체군의 다음 항목만 교정한다.

- 클래스와 메서드 명명
- `waitTime`, `leaseTime`, renewal parameter 규칙
- blocking/async/suspend capability 대칭성
- owner ID, acquisition/permit handle 표현
- acquire/release/renew result taxonomy
- validation 및 backend failure taxonomy
- cancellation과 ambiguous completion reconciliation
- Redis Cluster slot 및 NOSCRIPT 정책
- configuration/default 구조
- KDoc, README, usage example, capability matrix
- 중복 internal abstraction 제거

Delivery 1과 2에서 발견된 차이는 convergence matrix에 기록한다. 이 명세에 정의한 public
operation, identity, handle, result/failure 경계는 두 구현의 선행 계약이다. Delivery 3은
그 의미를 깨지 않는 source-compatible naming/configuration 교정과 matrix 불일치만 수정하며
새 coordination algorithm 또는 caller contract의 breaking rewrite를 추가하지 않는다.

## 5. 패키지 및 의존 방향

```text
io.bluetape4k.redis.lettuce
├── coordination/
│   └── internal/       # neutral internal runtime; public type 노출 금지
├── lock/               # public lock value/result/config와 Lock family
├── lease/              # 기존 fencing/multi-key low-level primitive
└── synchronizer/       # public synchronizer value/result/config와 객체군
```

`coordination.internal`은 lock 또는 synchronizer public interface/value/result를 소유하지
않으며 public signature에 노출되지 않는다. 다음 재사용 기능만 소유한다.

- script execution 및 EVALSHA/NOSCRIPT fallback
- codec wire byte 기반 Redis Cluster slot 검증
- logical owner와 generation validation
- bounded deadline/backoff utilities
- cancellation propagation과 unknown-completion marker
- backend failure 및 integrity failure classification
- sensitive key/owner/token redaction

의존 방향은 `lock|lease|synchronizer -> coordination.internal`이다.
`coordination.internal`에서 세 public package로 역참조하지 않는다. 공개 lock identity,
handle, result, config는 `lock`에, permit/latch identity, handle, result, config는
`synchronizer`에 둔다. 두 객체군에서 공통으로 보이는 타입도 convergence 전에 public
`coordination` package로 끌어올리지 않는다.

기존 `lease` primitive는 새 facade의 구현 기반으로 유지한다. 새 facade를 만들기 위해
기존 primitive를 복제하거나 기존 public class를 즉시 제거하지 않는다.

## 6. 공통 public contract

### 6.1 Redisson-shaped, Bluetape-native API

객체 이름과 capability는 Redisson mental model을 따른다. 그러나 새 객체는
`java.util.concurrent.locks.Lock`을 구현하지 않는다.

그 interface는 blocking thread ownership, `void unlock()` 예외 중심 계약 및 Redis lease
상실을 표현하지 못한다. Bluetape API는 blocking, `CompletableFuture`, suspend variant가
같은 semantic result를 노출하도록 설계한다.

공개 객체는 Kotlin caller를 위한 config object와 Java caller가 찾기 쉬운
`@JvmStatic create(...)` factory를 제공한다. 같은 타입의 duration/identity 인자를 길게
나열하거나 Kotlin default argument만으로 Java API를 구성하지 않는다. Blocking class는
각 operation과 동일 이름의 `*Async`를 함께 제공하고, suspend API는
`LettuceSuspend*` adapter가 같은 config/result type을 공유한다. 공개 sealed result의
variant는 Java `instanceof`/switch 사용을 고려해 고유하고 안정적인 이름을 사용한다.

#### 6.1.1 Public API surface

| 객체 | Factory/config | Blocking 및 async operation | Suspend surface | 결과/handle | Lifecycle |
|---|---|---|---|---|---|
| `LettuceDistributedLock` | `create(connection, name, LockConfig)` | `tryAcquire`, `acquire`, `inspect`, `reconcile`, `renew`, `release` 및 동일한 `*Async` | `LettuceSuspendDistributedLock`의 동일 operation | `LockAcquireResult`, `LockInspectResult`, `LockMutationResult`, `LockHandle` | object `close`; connection/runtime은 별도 소유 |
| `LettuceFairLock` | `create(..., FairLockConfig)` | distributed lock operation + queue reconciliation | `LettuceSuspendFairLock` | 공통 lock result + `FairWaiterState` | waiter 등록과 object lifecycle |
| `LettuceFencedLock` | `create(..., FencedLockConfig)` | distributed lock operation + fencing bootstrap | `LettuceSuspendFencedLock` | `FencedLockHandle`, fencing result | lock lifecycle과 동일 |
| `LettuceReadWriteLock` | `create(..., ReadWriteLockConfig)` | `readLock()`, `writeLock()`, `downgrade(handle)`; 각 view가 lock operation/`*Async` 제공 | `LettuceSuspendReadWriteLock`의 read/write view | `ReadLockHandle`, `WriteLockHandle`, `DowngradeResult` | parent close가 두 view 등록을 종료 |
| `LettuceSpinLock` | `create(..., SpinLockConfig)` | distributed lock operation | `LettuceSuspendSpinLock` | 공통 lock result/handle | scheduled retry 등록 종료 |
| `LettuceMultiLock` | `create(connection, names, MultiLockConfig)` | distributed lock operation | `LettuceSuspendMultiLock` | `MultiLockHandle`, 공통 lock result | constituent key set 불변 |
| `LettuceDistributedSemaphore` | `create(..., SemaphoreConfig)` | `availablePermits`, `trySetPermits`, `tryAcquire`, `acquire`, `inspect`, `reconcile`, `release` 및 `*Async` | `LettuceSuspendDistributedSemaphore` | `PermitAcquireResult`, `PermitMutationResult`, `PermitHandle` | object close는 caller permit을 release하지 않음 |
| `LettucePermitExpirableSemaphore` | `create(..., ExpirableSemaphoreConfig)` | semaphore operation + `renew` | `LettuceSuspendPermitExpirableSemaphore` | `ExpirablePermitHandle`과 permit result | expired permit cleanup은 Redis authority |
| `LettuceCountDownLatch` | `create(..., LatchConfig)` | `trySetCount`, `getCount`, `await`, `inspect`, `countDown`, `delete` 및 `*Async` | `LettuceSuspendCountDownLatch` | `LatchGeneration`, operation별 latch result | close는 local waiter만 종료 |

`tryAcquire`는 zero wait의 immediate attempt이고 `acquire`는 명시적인 positive bounded
wait를 요구한다. Async/suspend variant는 blocking wrapper가 아니라 같은 script,
decoder 및 result taxonomy를 공유한다.

Operation shape는 다음 규칙을 따른다.

- Lock `tryAcquire(ownerId, requestId, leasePolicy)`와
  `acquire(ownerId, requestId, waitTime, leasePolicy)`가 `LockAcquireResult`를 반환한다.
- Lock `inspect(handle)`/`reconcile(ownerId, requestId)`는 Redis authority state를,
  `renew(handle, extension)`/`release(handle)`는 `LockMutationResult`를 반환한다.
- Read/write view는 같은 owner/request/lease shape를 사용하고
  `downgrade(WriteLockHandle)`만 atomic `DowngradeResult`를 반환한다. Upgrade operation은
  제공하지 않는다.
- Semaphore `acquire(ownerId, requestId, permits, waitTime)`는 성공 시 한
  `PermitHandle`을 포함한 `PermitAcquireResult`를, expirable variant는 성공 시 lease가
  포함된 `ExpirablePermitHandle`을 같은 result에 담아 반환한다.
- Latch mutation은 `LatchGeneration`과 `requestId`를 받고, `await`는 관찰할 generation과
  bounded wait를 명시한다.
- 각 blocking method `op(...) : R`에 `opAsync(...) : CompletableFuture<R>`가 있고,
  suspend adapter의 `suspend fun op(...) : R`도 같은 public value/result를 사용한다.

#### 6.1.2 Public value invariants

- `LockOwnerId`: reentrancy domain을 식별하는 caller-visible value다. `random()`은 기존
  Base58 generator에 CSPRNG source를 사용해 최소 128 bit의 예측 불가능성을 제공한다.
  외부 값은 UTF-8 1..256 byte이며 identifier일 뿐 credential이 아니다.
- `LockRequestId`와 synchronizer request ID: command retry/reconcile를 묶는
  최소 128-bit CSPRNG value다. 한 logical operation의 최초 dispatch부터 terminal
  reconciliation까지 재사용하고 다음 operation에는 새 ID를 사용한다.
- `LockHandle`: object identity digest, owner ID, Redis가 발급한 monotonic generation,
  request ID, lease policy 및 acquisition kind를 가진 immutable value다. hold count는
  Redis inspect 결과일 뿐 handle equality에 포함하지 않는다. 같은 owner의 reentrant
  acquire는 동일 generation을 가리키는 동등한 handle view를 반환한다.
- `FencedLockHandle`: `LockHandle` invariant와 fencing epoch/token을 추가한다.
  reentrant acquire는 같은 epoch/token을 반환한다.
- `PermitHandle`: 한 atomic acquisition에서 받은 정확히 `N`개의 permit, owner/request ID,
  monotonic generation을 표현한다. 최초 버전은 전량 release만 지원하며 partial release는
  거절한다. 성공한 release 뒤 single-use terminal state이고 재사용은 `AlreadyReleased`,
  만료는 `Expired`, generation 불일치는 `StaleGeneration`을 반환한다.
- `ExpirablePermitHandle`: `PermitHandle` invariant와 permit별 opaque ID/lease deadline을
  가진다. Permit ID는 최소 128-bit CSPRNG value다.
- `LatchGeneration`: Redis가 발급한 monotonic non-reused generation이다. 완료/삭제 뒤에도
  generation counter key는 제거하거나 0으로 reset하지 않는다.
- 공개 value/config/result는 repo 규칙에 따라 `Serializable`과 `serialVersionUID`를
  제공하되, acquisition/permit handle은 bearer-like 값이므로 신뢰 경계 밖 전송/저장을
  권장하지 않는다. 필요 시 명시적으로 보호하고 version을 검증한다.
- handle/identity의 `toString`, 예외, equality diagnostic, 로그에는 raw 값 대신
  stable redacted digest만 사용한다.

### 6.2 Logical owner

- Redis ownership은 Java thread가 아니라 stable logical owner로 식별한다.
- blocking caller도 thread ID를 Redis owner 값으로 저장하지 않는다.
- async/suspend caller는 suspension 또는 callback thread 변경 뒤에도 동일 owner를 유지한다.
- acquisition이 성공하면 owner와 generation을 포함한 opaque handle을 반환한다.
- release와 renew는 handle 또는 그 handle에서 파생된 stable owner/generation을 요구한다.
- owner ID, permit ID, key 및 token은 로그나 metric label에 기록하지 않는다.
- owner ID는 인증 credential이 아니며 Redis ACL/TLS를 대체하지 않는다.
- 기본 factory는 owner를 자동 생성하지 않는다. Caller는 application operation,
  actor/session 또는 명시적인 critical-section scope에서 `LockOwnerId`를 생성해
  reentrant call 전체에 전달한다.
- 편의 `newOwner()`/`newRequestId()`는 제공하되 request ID는 operation마다 새로 만들고,
  dispatch 후 timeout/cancellation/transport ambiguity에서는 같은 ID를 보존한다.
- handle을 받은 뒤에는 caller가 owner/generation을 별도로 재구성하지 않고 handle을
  `renew`, `release`, `inspect`, `reconcile`에 전달한다.

### 6.3 Reentrancy

Lock family는 동일 logical owner에 대해 reentrant하다.

- 첫 acquisition이 Redis generation과 lease를 생성한다.
- 같은 owner의 재획득은 hold count를 증가시키고 같은 generation을 유지한다.
- fencing lock의 재획득은 새 fencing token을 발급하지 않는다.
- 마지막 release만 Redis ownership을 제거한다.
- 다른 owner 또는 오래된 generation의 release/renew는 현재 ownership을 변경하지 않는다.
- MultiLock의 reentrancy는 모든 constituent key가 동일 owner/generation으로 일치할 때만
  허용한다.

Synchronizer family에는 reentrancy 개념을 적용하지 않는다.

### 6.4 Result와 failure

공통 결과 taxonomy는 다음 상태를 표현할 수 있어야 한다.

- acquired
- already owned/reentrant acquisition
- contended
- timed out
- released
- ownership lost 또는 stale generation/token
- invalid lifecycle state
- backend failure
- integrity failure
- ambiguous completion

각 객체는 관찰 불가능한 상태를 억지로 노출하지 않는다. Result 이름과 payload는 객체별로
좁힐 수 있지만, blocking/async/suspend variant 사이에서 의미가 달라지면 안 된다.

Failure channel은 첫 delivery부터 다음 한 규칙으로 고정한다.

- null, 형식, 범위, same-slot 같은 caller validation은 dispatch 전에
  `IllegalArgumentException` 또는 해당 subtype을 동기적으로 던진다. `*Async`도 invalid
  input으로 실패한 future를 만들기 전에 같은 synchronous validation을 수행한다.
- contention, timeout, stale owner/generation, expired/released state, backend/transport,
  malformed script/integrity 및 ambiguous completion처럼 Redis dispatch 이후 예상 가능한
  결과는 operation-specific sealed result로 반환한다.
- async exceptional completion과 suspend throw는 caller cancellation의 원래 identity,
  programmer error 및 decoder contract 밖 JVM fatal/unchecked invariant에만 사용한다.
- cancellation이 dispatch 이후 발생했을 가능성이 있으면 cancellation identity를 그대로
  전달하면서 recovery metadata를 보존하고, caller는 같은 request/handle로 `reconcile`한다.
- Blocking/async/suspend는 같은 result variant와 payload를 사용한다. 한 semantic state를
  result와 exception 두 채널로 동시에 제공하지 않는다.

### 6.5 Lease와 watchdog

Lock acquisition은 두 policy를 명시적으로 지원한다.

- fixed lease: 정해진 TTL 뒤 자동 만료하며 자동 renewal을 시작하지 않는다.
- watchdog lease: bounded renewal task가 ownership을 확인하며 TTL을 연장한다.

watchdog는 무기한 소유권 보장이 아니다.

- renewal interval은 lease TTL보다 짧아야 하며 bounded validation을 통과해야 한다.
- renewal failure 또는 ownership loss는 handle state에 반영한다.
- process pause, network partition 또는 scheduler starvation은 lease 상실을 일으킬 수 있다.
- caller는 lease 상실 뒤 작업이 중단됐다고 가정할 수 없다.
- fencing이 필요한 작업은 downstream authority가 최고 token을 저장하고 더 작은 token을
  거부해야 한다.

Synchronizer의 permit lease는 permit별 fixed lease를 기본으로 하며, expirable permit의
explicit renewal을 지원한다. CountDownLatch에는 watchdog를 적용하지 않는다.

공개 policy는 `LeasePolicy.Fixed(leaseTime)`과
`LeasePolicy.Watchdog(ttl, renewalInterval)` sealed value로 고정한다. Watchdog task는
connection/runtime당 shared scheduler에서 실행하며 handle마다 executor를 만들지 않는다.
Caller가 scheduler를 주입하면 runtime은 닫지 않고, runtime이 생성하면 connection/runtime
close에서 idempotently 닫는다. Object close는 해당 object의 신규 operation과 task
registration을 막고 자신이 등록한 task만 취소한다. Ownership loss는 handle inspect 결과와
`onOwnershipLost` observation hook으로 관찰하며 자동으로 caller 작업을 중단시키지 않는다.

### 6.6 Waiting과 cancellation

- 모든 wait는 absolute deadline으로 변환해 반복 계산 drift를 줄인다.
- blocking wait는 virtual-thread pinning을 피하는 park 또는 bounded blocking primitive를
  사용한다.
- async wait는 scheduler 또는 Lettuce future chain을 사용하며 caller thread를 block하지 않는다.
- suspend wait는 cancellable suspension을 사용한다.
- cancellation은 caller wait의 종료이지 Redis mutation 미실행의 증명이 아니다.
- acquisition/release/renew가 dispatch된 뒤 cancellation되면 result는 ambiguous completion으로
  간주하고 같은 owner/handle로 inspect 또는 reconcile할 수 있어야 한다.
- timeout/cancellation된 fair-lock waiter는 queue에서 제거한다. 제거 결과가 불명확하면
  bounded cleanup/reconciliation 경로를 제공한다.

#### 6.6.1 Inspect/reconcile decision table

| 관찰 시점 | Redis authority 결과 | Caller action |
|---|---|---|
| dispatch 전 cancellation | command 미전송 | 같은 business operation을 새 request ID로 재시도 가능 |
| acquire wait cancellation/timeout | `Queued` | 같은 waiter/request로 remove 후 terminal 결과 확인 |
| acquire ambiguity | `Admitted`/`Owned` | 회수된 handle로 작업을 계속하거나 즉시 release |
| acquire ambiguity | `NotFound` | 같은 request ID로 acquire/reconcile를 재호출; 새 identity blind retry 금지 |
| fair cleanup | `Removed` | terminal cancellation/timeout 처리 |
| fair cleanup | `StaleGeneration` | 현재 object generation을 inspect하고 이전 요청 종료 |
| release/renew ambiguity | `OwnedByHandle` | 같은 handle로 동일 operation을 idempotent retry |
| release/renew ambiguity | `Released`/`Lost`/`StaleGeneration` | local 작업을 종료하고 새 owner state를 변경하지 않음 |
| expirable permit ambiguity | `Active` | 동일 handle/request로 renew 또는 release |
| expirable permit ambiguity | `Expired`/`Released` | terminal 처리; capacity를 다시 더하지 않음 |
| latch await cancellation | 같은 generation count `0` | `Completed`로 처리 |
| latch await cancellation | 같은 generation count `>0` | count를 바꾸지 않고 중단하거나 같은 generation으로 재-await |

Fair waiter reconcile은 `Queued`, `Removed`, `Admitted`, `NotFound`,
`StaleGeneration`을 구분한다. Queue 삭제는 waiter ID, sequence, generation을 모두 비교한
뒤 수행해 늦은 cleanup이 재사용된 entry를 지우지 못하게 한다.

### 6.7 Input, time 및 capacity bounds

| 항목 | 초기 기본/최대 계약 |
|---|---|
| object namespace/name/hash-tag component | `[A-Za-z0-9._-]{1,128}`; encoded derived key 전체 512 byte 이하 |
| external owner/request ID | UTF-8 1..256 byte; generated ID는 최소 128-bit CSPRNG |
| wait | zero는 immediate attempt; positive wait 최대 24시간; indefinite wait 미지원 |
| fixed lease | 100ms..24시간 |
| watchdog | TTL 3초..24시간, 기본 30초; interval 기본 TTL/3; jitter ±10%; lifetime 기본 24시간, 최대 7일 |
| active watchdog registrations | runtime당 hard cap 10,000; tick당 dispatch 최대 256 |
| fair queue | object당 최대 10,000; stale cleanup 기본 64, 최대 256 entry/script |
| spin retry | 초기 10ms, multiplier 2, 최대 1초, jitter 0..25%, waiter당 최대 100 Redis attempt/초 |
| permit/count | object capacity 최대 1,000,000; 한 acquire 최대 10,000 |
| script response | tag 포함 최대 16 item, bulk item 최대 256 byte |

Duration은 nanosecond 입력을 Redis millisecond로 올림 변환해 positive 값이 0으로 잘리지 않게
하고 overflow를 validation failure로 거절한다. Wait deadline은 local monotonic clock으로
계산하고 wall clock은 Redis TTL authority를 대체하지 않는다. Queue/capacity/hard cap
초과는 dispatch 전 validation 또는 stable `CapacityExceeded` result로 fail closed한다.

## 7. Lock family

### 7.1 `LettuceDistributedLock`

- reentrant exclusive lock이다.
- 첫 acquisition은 owner/generation/hold count/lease를 원자적으로 생성한다.
- 다른 owner가 보유하면 contended 또는 timed out을 반환한다.
- renew와 release는 owner/generation을 모두 검증한다.
- 기존 `LettuceLock`의 token-checked unlock behavior를 재사용하거나 migration adapter로
  감싼다.

### 7.2 `LettuceFairLock`

- 살아 있는 queued waiter 사이에서 Redis enqueue sequence 기준 FIFO admission을 보장한다.
- waiter는 queue sequence와 unique waiter ID를 가진다.
- 모든 waiter는 Redis-side absolute expiry/deadline을 가지며 indefinite wait를 허용하지 않는다.
- timeout/cancellation waiter는 소유권 획득 대상에서 제거한다.
- acquire script는 head부터 bounded batch만 정리한다. Batch가 소진되면
  `CleanupPending`을 반환하고 뒤 waiter를 FIFO 밖으로 admit하지 않는다.
- stale waiter cleanup은 caller heartbeat나 죽은 process의 협조 없이 진행되고 active
  waiter를 삭제하지 않는다.
- fairness는 Redis가 관찰한 enqueue 순서에 대한 보장이지 caller wall-clock invocation
  순서 보장이 아니다.
- notification을 사용하더라도 queue state가 authority이며 notification 유실은 polling으로
  회복 가능해야 한다.

### 7.3 `LettuceFencedLock`

- `LettuceFencingLease`를 facade 구현 기반으로 사용한다.
- fresh generation의 첫 acquisition만 monotonic fencing token을 증가시킨다.
- 같은 owner의 reentrant acquisition은 동일 token을 반환한다.
- downstream 저장소는 `incomingFence > lastAcceptedFence`를 검사해야 한다.
- Redis token만으로 exactly-once, durable transaction 또는 stale work 중단을 보장한다고
  문서화하지 않는다.

### 7.4 `LettuceReadWriteLock`

- 여러 reader는 동시에 보유할 수 있다.
- writer는 모든 reader 및 다른 writer와 배타적이다.
- reader와 writer 모두의 starvation을 막기 위해 phase-fair queue를 사용한다.
- 연속 queued reader는 writer boundary 전까지 한 reader phase로 admit한다. Boundary에
  도달하면 reader phase 종료 뒤 writer 한 명을 admit하며, 이후 도착한 reader/writer는
  기존 boundary를 추월하지 않는다.
- read-to-write upgrade는 지원하지 않는다. caller는 read lock을 release한 뒤 write lock을
  새로 획득해야 한다.
- write-to-read downgrade는 한 atomic script 안에서만 지원하며 별도 operation으로 노출한다.
- expired reader/writer cleanup은 다른 generation의 valid holder를 삭제하지 않는다.

### 7.5 `LettuceSpinLock`

- exclusive, reentrant, non-fair lock이다.
- waiter queue와 pub/sub을 사용하지 않는다.
- bounded exponential backoff와 configurable jitter를 사용하며 jitter `0`으로 비활성화할
  수 있다.
- initial delay, multiplier, max delay, max wait는 configuration boundary에서 검증한다.
- cancellation/deadline은 다음 retry 전에 확인한다.
- spin은 CPU busy loop가 아니라 Redis retry scheduling을 뜻한다.

### 7.6 `LettuceMultiLock`

- `LettuceMultiKeyLease`를 facade 구현 기반으로 사용한다.
- 모든 key는 codec wire byte 기준으로 같은 Redis Cluster slot이어야 한다.
- acquisition/reentrancy/renew/release는 한 Lua script에서 all-or-nothing으로 수행한다.
- partial ownership 또는 persistent same-token key는 integrity failure이다.
- 서로 다른 slot을 순차적으로 잠그는 best-effort multi-lock은 지원하지 않는다.

## 8. Synchronizer family

### 8.1 `LettuceDistributedSemaphore`

- permit count와 active permit ownership을 Redis가 authority로 관리한다.
- acquisition은 permit handle을 반환한다.
- release는 local stack이나 thread identity가 아니라 permit handle을 요구한다.
- 한 handle은 한 atomic acquisition에서 얻은 정확히 `N` permit 전체를 표현한다. 초기
  public contract는 partial release를 지원하지 않고 전량 release만 허용한다.
- handle release는 single-use이며 duplicate release가 capacity를 두 번 복구하지 않는다.
- `availablePermits`, initialize/trySetPermits, acquire, release를 제공한다.
- permit count shrink는 active permit을 음수로 만들 수 없으며 정책 위반을 명시적인
  lifecycle result로 반환한다.
- 기존 `LettuceSemaphore`는 compatibility surface로 유지하고 새 handle contract로 이동할
  migration documentation을 제공한다. 기존 local permit stack과 새 handle state를 공유하는
  adapter는 이 delivery에 추가하지 않는다.

### 8.2 `LettucePermitExpirableSemaphore`

- 획득한 각 permit는 unique opaque permit ID와 자체 lease deadline을 가진다.
- release와 renew는 permit ID를 검증한다.
- expired permit cleanup은 capacity를 한 번만 복구한다.
- cancellation/unknown completion 뒤 caller는 동일 permit request identity 또는 inspect
  operation으로 reconciliation할 수 있다.
- permit ID는 bearer-like ownership token으로 취급하며 로그/metric에 노출하지 않는다.
- permanent permit은 explicit opt-in일 때만 허용하며 기본값으로 사용하지 않는다.

### 8.3 `LettuceCountDownLatch`

- latch는 name과 generation으로 식별되는 one-shot coordination object다.
- `trySetCount`는 active generation이 없을 때만 새 generation을 생성한다.
- count는 zero 아래로 내려가지 않는다.
- zero에 도달하면 해당 generation의 waiter를 release한다.
- 완료된 generation은 inspect할 수 있지만 같은 generation을 reset하지 않는다.
- 새 count는 명시적인 delete/cleanup 뒤 새 generation으로만 생성한다.
- generation은 별도 Redis monotonic counter에서 발급하고 cleanup 시 counter를 삭제하지 않는다.
- waiter는 자신이 관찰한 generation에 bind되어 이후 generation의 signal을 잘못 소비하지 않는다.
- await timeout/cancellation은 count를 변경하지 않는다.
- 같은 generation의 count가 이미 0이면 늦게 도착한 await도 즉시 `Completed`를 반환한다.

### 8.4 Synchronizer operation/result matrix

| 객체/operation | 성공 | 예상 terminal/negative 결과 | Reconcile identity |
|---|---|---|---|
| semaphore `trySetPermits` | `Initialized(generation)` | `AlreadyInitialized`, `InvalidCapacity`, `BackendFailure` | object generation |
| semaphore `acquire(N)` | `Acquired(PermitHandle(N))` | `Unavailable`, `TimedOut`, `CapacityExceeded`, `Ambiguous` | request ID |
| semaphore `release(handle)` | `Released(N)` | `AlreadyReleased`, `Expired`, `StaleGeneration`, `BackendFailure`, `Ambiguous` | handle |
| expirable `renew(handle)` | `Renewed(deadline)` | `Expired`, `Released`, `StaleGeneration`, `OwnershipLost`, `Ambiguous` | expirable handle |
| latch `trySetCount` | `Created(LatchGeneration)` | `ActiveGeneration`, `InvalidCount`, `BackendFailure` | object identity |
| latch `await(generation)` | `Completed` | `TimedOut`, `StaleGeneration`, `Deleted`, `Ambiguous` | latch generation/request ID |
| latch `countDown(generation)` | `Decremented(count)`/`Completed` | `AlreadyCompleted`, `StaleGeneration`, `Deleted`, `Ambiguous` | latch generation/request ID |
| latch `delete(generation)` | `Deleted` | `ActiveWaiters`, `StaleGeneration`, `NotFound`, `Ambiguous` | latch generation/request ID |

## 9. Redis key와 Cluster contract

- 한 object가 사용하는 모든 mutation key는 codec wire byte 기준으로 한 slot이어야 한다.
- object name에서 공통 hash tag를 파생하거나 caller가 명시한 hash tag를 보존한다.
- 예를 들어 logical name `orders`와 hash tag `coord-orders`는
  `bt4k:coord:v1:{coord-orders}:lock:orders:state`,
  `...:queue`, `...:generation`처럼 모든 보조 key가 같은 `{coord-orders}`를 사용한다.
- fair queue, reader/writer state, fencing counter, permit expiry index, latch generation key를
  포함해 multi-key script의 모든 key를 dispatch 전에 검증한다.
- raw object name, owner ID, waiter ID, permit ID를 exception message에 포함하지 않는다.
- key schema는 object kind와 schema version을 포함해 다른 객체군과 충돌하지 않게 한다.
- key schema 변경이 필요하면 in-place interpretation을 시도하지 않고 versioned namespace와
  migration/cleanup guidance를 제공한다.
- Lettuce의 `MOVED`/`ASK` topology handling을 사용한다. 그러나 mutation dispatch 후 redirect,
  timeout 또는 connection loss는 완료 여부가 불명확할 수 있으므로 새 identity로 자동
  재실행하지 않고 같은 request/handle의 reconcile 대상으로 분류한다.

## 10. Script와 runtime contract

- 모든 mutation은 Lua에서 atomic하게 수행한다.
- 기존 `RedisScriptRunner`의 EVALSHA-first/NOSCRIPT fallback을 재사용한다.
- script result는 bounded tagged response로 decode하고 unknown tag/arity를 integrity failure로
  분류한다.
- blocking/async/suspend는 같은 script와 decoder를 공유한다.
- retry는 idempotency가 증명된 operation 또는 같은 logical request identity를 사용한
  reconciliation에만 적용한다.
- ambiguous mutation을 새 owner/permit ID로 blind retry하지 않는다.
- watcher, scheduler, waiter registry는 closeable lifecycle을 갖고 connection 종료 뒤 task를
  남기지 않는다.
- script key/ARGV는 validated bounded value만 구성한다. Caller string을 Lua source에
  interpolation하지 않고 KEYS/ARGV로 전달하며 numeric parsing은 범위와 overflow를 검사한다.
- decoder는 최대 item count, arity 및 byte length를 먼저 검사해 oversized/malformed reply를
  integrity failure로 거절한다.

### 10.1 Redis command budgets

| 경로 | Warm script cache | Cold `NOSCRIPT` | 추가 제한 |
|---|---:|---:|---|
| immediate acquire/inspect/renew/release/countDown/permit mutation | 최대 1 command | 해당 node/script에서 최대 2 (`EVALSHA` + `EVAL`) | fallback은 `NOSCRIPT`에만 |
| fair enqueue+attempt | 최대 1 | 최대 2 | stale cleanup 최대 configured batch 포함 |
| fair wait retry/poll | retry당 1 | cold 시 최초 retry만 최대 2 | `CleanupPending`이면 FIFO를 건너뛰지 않음 |
| spin retry | attempt당 1 | cold 시 최초 attempt만 최대 2 | waiter당 초당 최대 100 attempt |
| watchdog renewal | due handle당 1 | cold 시 해당 script 최초 renewal만 최대 2 | shared scheduler, tick당 최대 256 |
| latch/semaphore await poll | retry당 1 | cold 시 최초 retry만 최대 2 | notification은 wakeup hint일 뿐 state authority 아님 |

Pub/sub을 선택하면 subscribe/unsubscribe command와 connection 비용은 script budget과 별도
측정한다. Notification 유실은 correctness를 바꾸지 않으며 bounded polling으로 회복한다.

### 10.2 Runtime lifecycle

- Connection/runtime이 shared scheduler, script registry, waiter registry를 소유한다.
  Injected scheduler는 caller 소유이고 runtime이 닫지 않는다.
- Object close는 idempotent하며 신규 work를 거절하고 해당 object의 renewal/wait registration만
  취소한다. 이미 획득한 Redis ownership/permit을 암묵적으로 release하지 않는다.
- Runtime/connection close는 신규 dispatch를 차단하고 owned scheduler를 idempotently
  종료한다. Pending caller future/coroutine은 stable lifecycle result 또는 원래 cancellation로
  종료한다.
- 각 scheduled task는 task ID와 object/generation을 기록한다. Cancel/close 뒤 도착한
  completion은 registry의 현재 task ID/generation과 비교해 일치할 때만 다음 task를 등록한다.
- Pending/active task, watchdog registration 및 waiter 수는 hard cap을 적용하고 cap 초과 시
  `CapacityExceeded`로 fail closed한다.

### 10.3 Operational observability

허용 metric dimension은 `object_kind`, `operation`, `outcome`, `failure_kind`,
`lease_policy`뿐이다. Object name, Redis key/hash tag, owner/request/waiter/permit ID,
fencing token 및 generation raw value는 label/log에 넣지 않는다.

- Counter: operation outcome, timeout/cancellation, reconcile state, stale cleanup,
  ownership loss, `NOSCRIPT` fallback, integrity failure.
- Gauge: active watchdog registration, scheduled task, queued waiter, connection별 coordination
  object 수.
- Histogram: Redis command latency, caller wait latency, retry count, cleanup batch size.
- Structured event: acquire/release/renew, timeout/cancel, reconcile, cleanup,
  ownership/lease loss, close 및 capacity rejection.
- 진단 correlation은 raw identity가 아닌 process-local bounded redacted digest를 사용한다.
  값 cardinality가 높은 correlation은 metric label이 아니라 sampled debug event에만 둔다.

README/runbook은 Redis ACL 최소 command/script 권한, TLS/credential 관리, namespace 격리와
key prefix, metric alert 예시를 포함한다. Library는 ACL/TLS를 우회하거나 identity를 인증
수단으로 취급하지 않는다.

## 11. Compatibility와 migration

- 기존 `LettuceLock`, `LettuceSuspendLock`, fencing lease, multi-key lease,
  `LettuceSemaphore`, `LettuceSuspendSemaphore`는 Delivery 1과 2에서 제거하지 않는다.
- 새 facade는 additive하게 도입한다.
- 기존 public primitive의 source/binary compatibility를 유지한다.
- 기존 behavior와 새 contract가 충돌하면 facade가 adapter로 차이를 흡수하거나, migration
  guide를 제공한 뒤 별도 deprecation cycle을 사용한다.
- deprecation 여부는 convergence 결과와 사용처 검색을 근거로 결정한다.
- convergence 전 새 API는 이 명세의 의미를 유지하는 source-compatible 이름/config 교정만
  허용하며, Delivery 3 완료 시 이름과 결과 contract를 동결한다.

### 11.1 기존 API migration matrix

| 기존 surface | 새 surface | Caller-visible 변화 | Compatibility |
|---|---|---|---|
| `LettuceLock.tryLock/lock/unlock` | `LettuceDistributedLock.tryAcquire/acquire/release` | implicit instance token/Boolean·exception에서 explicit owner, request, handle, sealed result로 이동 | 기존 class 유지; 새 API opt-in |
| `LettuceSuspendLock` | `LettuceSuspendDistributedLock` | 동일 owner/result contract를 suspend로 공유 | 기존 class 유지 |
| `LettuceFencingLease` / suspend | `LettuceFencedLock` / suspend | durable epoch/bootstrap은 유지하고 reentrant handle facade 추가 | 기존 lease가 implementation authority |
| `LettuceMultiKeyLease` / suspend | `LettuceMultiLock` / suspend | same-slot atomic lease를 reentrant lock handle로 노출 | 기존 lease가 implementation authority |
| `LettuceSemaphore.acquire/release(N)` | `LettuceDistributedSemaphore.acquire/release(handle)` | local permit stack 제거; 한 handle이 atomic N permit 전량을 release | 기존 class 유지; adapter는 double release 방지 |
| `LettuceSuspendSemaphore` | `LettuceSuspendDistributedSemaphore` | Boolean/exception에서 permit result/handle로 이동 | 기존 class 유지 |

기존 class의 deprecation은 usage evidence와 별도 cycle 없이 시작하지 않는다. Compatibility
adapter가 raw token/owner를 exception 또는 `toString`에 노출하지 않고, old/new surface를
섞어 release해도 capacity/ownership을 두 번 변경하지 않는 fixture를 둔다.

### 11.2 의도적으로 지원하지 않는 Redisson parity

- `java.util.concurrent.locks.Lock` 또는 Java thread identity ownership
- indefinite wait와 unbounded watchdog
- 서로 다른 Cluster slot의 best-effort `MultiLock`
- read-to-write upgrade
- Redis lock만으로 stale work 자동 중단, exactly-once 또는 durable transaction 보장
- object close 시 caller가 획득한 lock/permit의 암묵적 release

### 11.3 Rollout, rollback 및 key cleanup

- 각 delivery는 additive opt-in으로 배포하며 기존 class의 default behavior를 바꾸지 않는다.
- Old/new client는 기본적으로 versioned distinct key namespace를 사용한다. Compatibility
  adapter가 같은 key를 의도적으로 공유할 때만 양쪽 script/schema version 호환 fixture를
  통과해야 한다.
- Rollback은 신규 runtime 생성을 중단하고 이미 생성된 new-schema key를 old client가
  해석하지 않게 한다. Active fixed lease/watchdog/permit/latch generation은 TTL 만료 또는
  명시적 drain 전 강제 삭제하지 않는다.
- Versioned key cleanup은 active owner/waiter/permit/count가 없고 최대 lease/wait TTL이 지난
  뒤 operator inspect가 empty state를 확인했을 때 bounded batch로 수행한다. Generation과
  fencing epoch/counter authority는 migration 계약 없이 삭제하지 않는다.
- Delivery마다 release note와 operator runbook을 갱신한다. Runbook은 lease loss,
  ambiguous completion, stale fair waiter, leaked watchdog task, cross-slot/redirect,
  `NOSCRIPT`/protocol drift, generation ABA의 metric/event, inspect/reconcile 및 safe cleanup
  절차를 제공한다.

## 12. 실패 모드와 복구

### 12.1 Lease 만료 뒤 stale holder가 계속 작업

신호: renew가 ownership lost를 반환하거나 holder pause 뒤 다른 owner가 획득한다.

대응:

- release/renew가 newer generation을 변경하지 못하게 한다.
- fenced operation은 downstream highest-token comparison을 요구한다.
- non-fenced lock 문서에 stale work 차단을 보장하지 않음을 명시한다.

### 12.2 Cancellation 뒤 acquisition 결과 불명확

신호: Redis command dispatch 뒤 future/coroutine이 취소되어 caller가 result를 받지 못한다.

대응:

- same logical owner/request identity를 보존한다.
- inspect/reconcile operation으로 authoritative state를 확인한다.
- 새 owner ID를 생성해 blind retry하지 않는다.

### 12.3 Fair queue의 stale waiter가 head를 막음

신호: head waiter deadline이 지났지만 뒤 waiter가 진입하지 못한다.

대응:

- queue entry에 bounded deadline/generation을 저장한다.
- acquisition script가 stale head를 bounded batch로 정리한다.
- timeout/cancellation cleanup과 recovery fixture를 제공한다.

### 12.4 Watchdog scheduler 또는 connection 종료 누수

신호: object/connection close 뒤 renewal 또는 polling task가 계속 실행된다.

대응:

- runtime task registry를 connection/object lifecycle에 bind한다.
- close는 신규 scheduling을 차단하고 existing task를 cancel한다.
- lifecycle/leak test에서 task count와 late command를 검증한다.

### 12.5 Redis Cluster cross-slot

신호: codec가 encode한 key가 서로 다른 slot에 속한다.

대응:

- command dispatch 전에 actual wire byte slot을 검증한다.
- key 값을 노출하지 않는 stable cross-slot failure를 반환한다.
- partial fallback이나 key-by-key acquisition을 수행하지 않는다.

### 12.6 Script cache miss 또는 backend protocol drift

신호: `NOSCRIPT`, unknown response tag/arity, transport timeout이 발생한다.

대응:

- `NOSCRIPT`만 bounded EVAL fallback한다.
- unknown response는 integrity failure로 fail closed한다.
- transport failure는 operation과 recovery guidance를 보존해 분류한다.

### 12.7 Permit 또는 latch generation ABA

신호: expired/deleted generation의 늦은 release/countDown이 새 generation을 변경한다.

대응:

- permit handle과 latch operation에 generation을 포함한다.
- Lua가 generation 일치 후에만 mutation한다.
- stale generation operation은 현재 state를 변경하지 않는다.

## 13. 검증 전략

### 13.1 공통 contract matrix

모든 지원 객체는 적용 가능한 blocking/async/suspend variant에 대해 같은 fixture를 실행한다.

- validation before dispatch
- successful acquisition/release
- contention/timeout
- wrong owner/stale generation
- TTL expiry와 takeover
- renew success/loss
- cancellation before/after dispatch
- ambiguous completion reconciliation
- EVALSHA/NOSCRIPT fallback
- malformed script result
- standalone 및 Redis Cluster same-slot
- custom codec wire-byte slot validation
- secret/key/token redaction
- generated identity CSPRNG bit length, external identity/name/key/ARGV bound
- async exceptional completion이 cancellation/programmer/JVM invariant로 제한되는지
- reconcile decision table의 모든 state와 same-identity retry

### 13.2 Lock-specific

- exclusive lock에서 concurrent one-winner
- logical-owner reentrancy와 hold count
- fair lock FIFO admission, dead-process 없이 timeout stale-head cleanup, cleanup batch 소진 시
  `CleanupPending`, queue cap
- fenced lock monotonic token과 reentrant token stability
- read/write compatibility, phase-fair reader/writer boundary, 양쪽 starvation 방지,
  upgrade rejection, downgrade atomicity
- spin backoff upper/lower bound, jitter bound, cancellation responsiveness
- multi-lock atomicity와 partial-state integrity failure
- watchdog renewal rate/lifetime/cap, lease loss, shared scheduler ownership, close cleanup 및
  close 뒤 late completion 무시

### 13.3 Synchronizer-specific

- permit capacity와 multi-permit acquisition
- multi-permit handle 전량 single-use release 및 partial/duplicate release rejection
- wrong/stale permit release
- expired permit capacity one-time recovery
- permit lease renew 및 unknown completion reconciliation
- count initialization, zero floor, timeout/cancellation
- latch generation isolation, completed generation, delete/new generation
- cleanup 뒤 Redis monotonic generation non-reuse와 완료 generation late await

### 13.4 Stability와 performance

- Testcontainers-backed Redis 테스트는 모듈/worktree 간 순차 실행한다.
- concurrency fixture는 deterministic barriers와 bounded deadlines를 사용한다.
- fair queue, watchdog, spin retry는 command count 및 upper-bound latency를 관찰한다.
- warm EVALSHA/cold NOSCRIPT command budget과 operation event/latency metric을 같은 fixture에서
  검증한다.
- hot lock contention에서 unbounded allocation, busy loop, unbounded Redis round trip을 허용하지
  않는다.
- timing-only assertion은 사용하지 않고 state transition과 bounded event를 함께 검증한다.
- metric label allow-list/cardinality, raw identity redaction, runtime task/queue/watchdog gauge
  cleanup을 검증한다.
- Redis Cluster slot migration의 `MOVED`/`ASK`와 post-dispatch ambiguity를 fault fixture로
  검증한다.
- old/new compatibility adapter의 double release, redaction 및 key schema isolation을
  회귀 테스트한다.

## 14. 문서화

- public type과 result에 English KDoc를 작성한다.
- `infra/lettuce/README.md`와 `README.ko.md`에 top-level `Coordination primitives` 절,
  capability matrix, “choose this object when...” 선택표 및 old/new naming note를 제공한다.
- lock reentrancy/release, fenced downstream CAS, semaphore handle release, expirable permit
  renew/release, latch generation await 및 cancellation reconcile의 compile-tested 대표 예제를
  제공한다.
- fencing 예제는 downstream compare-and-set을 포함한다.
- fixed lease/watchdog lifecycle과 close ownership, ownership loss 대응,
  cancellation/unknown completion, Cluster same-slot/hash-tag example을 명시한다.
- operator runbook에 Redis ACL/TLS/key namespace, rollout/rollback/drain/cleanup, alert와
  inspect/reconcile 절차를 포함한다.
- Redisson과의 비교는 객체 선택을 돕기 위한 capability 비교로 제한하고 semantic parity를
  과장하지 않는다.

## 15. 거절한 대안

### 15.1 기존 primitive 이름만 바꾸는 facade-first 접근

초기 변경은 작지만 fair/read-write/spin/permit/latch가 서로 다른 owner/result/runtime을
발명하게 된다. 전체 coordination foundation과 객체군 계약을 먼저 고정하는 현재 설계를
선택한다.

### 15.2 Redisson Java API의 기계적 복제

thread-bound `Lock`, exception 중심 unlock, Redisson command executor/watchdog/pub-sub 내부
구조는 coroutine/Lettuce 환경과 맞지 않는다. 객체군과 capability는 채택하되 logical owner,
sealed result 및 Lettuce script runtime으로 변환한다.

### 15.3 Lock과 synchronizer를 한 delivery에서 구현

공통점보다 lifecycle 차이가 크고 review/test surface가 과도해진다. 전체 설계는 함께 고정하되
구현과 PR은 분리한다.

### 15.4 각 delivery 종료 시 public API를 즉시 고정

첫 객체군만 보고 공통 이름과 result를 확정하면 두 번째 객체군에서 불필요한 adapter가 생긴다.
두 구현 결과를 비교한 뒤 별도 convergence delivery에서 API를 동결한다.

## 16. Acceptance Criteria

### 16.1 전체 이슈

- Lock, Synchronizer, Convergence 세 delivery가 독립적으로 검증된다.
- Redisson-shaped 객체군과 Bluetape-native owner/result contract가 문서화된다.
- 모든 Redis mutation은 atomic script와 owner/generation 검증을 사용한다.
- blocking/async/suspend variant는 지원되는 capability에서 의미가 일치한다.
- cancellation과 ambiguous completion에 inspect/reconcile 경로가 있다.
- Cluster same-slot, NOSCRIPT, malformed result 및 lifecycle failure fixture가 있다.
- convergence matrix의 모든 필수 불일치가 해소된 뒤 public API가 동결된다.

### 16.2 Lock delivery

- 여섯 lock type이 정의된 계약과 테스트를 충족한다.
- logical-owner reentrancy, stale-generation protection, lease/watchdog가 검증된다.
- fair/FIFO, fencing, read/write, spin backoff, multi-key atomicity가 각각 독립적으로 증명된다.

### 16.3 Synchronizer delivery

- 세 synchronizer type이 정의된 계약과 테스트를 충족한다.
- permit handle/lease와 latch generation이 stale operation을 차단한다.
- 기존 semaphore migration 또는 compatibility adapter가 문서화된다.

### 16.4 Convergence delivery

- capability matrix가 blocking/async/suspend 차이를 설명한다.
- 명명, parameter, result, failure, cancellation 및 documentation 규칙이 통일된다.
- convergence 범위에 새 coordination algorithm이 포함되지 않는다.
- 기존 public primitive compatibility 결과와 deprecation 결정을 기록한다.

## 17. Definition of Done

- 이 spec과 후속 implementation plan이 Type A review에서 P0=0/P1=0이다.
- 각 delivery가 test-first 구현, targeted test, module check 및 sequential Redis integration
  test를 통과한다.
- public KDoc와 English/Korean README가 source와 일치한다.
- performance, stability, security, operator, developer/API, user/caller review가 수렴한다.
- `git diff --check`와 affected Gradle verification이 통과한다.
- issue/PR metadata와 `## DoD Status`가 workflow contract를 충족한다.
- 각 PR은 live CI와 review가 완료된 뒤 별도의 fresh merge 승인을 받는다.
- 마지막 convergence PR 병합 뒤 root `develop`과 `origin/develop`이 일치하고 관련 worktree와
  local branch가 정리된다.
