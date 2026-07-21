# Issue #1065 Redis 다중 키 소유권 리스 설계

## 1. 배경

`infra/lettuce`에는 단일 key mutex인 `LettuceLock`과 permit 기반
`LettuceSemaphore`가 있다. 두 primitive 모두 owner token과 TTL을 사용하지만,
임의의 여러 Redis key를 하나의 advisory ownership 단위로 묶지는 않는다.

Concert Ticket Flash Sale 소비자는 결제 workflow를 시작하기 전에 다음 두 key를
동시에 점유해야 한다.

```text
ticket:{saleId}:inflight:ip:{ipDigest}
ticket:{saleId}:inflight:user:{userDigest}
```

두 key를 독립적으로 생성하거나 삭제하면 일부 key만 남는 partial ownership과
일관되지 않은 admission 판단이 발생할 수 있다. Issue #1065는 이 문제를
재사용 가능한 Lettuce primitive로 해결한다.

## 2. 목표

- 같은 Redis hash slot에 속한 여러 key를 한 Lua 실행으로 전부 획득하거나 전혀
  획득하지 않는다.
- 모든 key에 같은 caller-supplied owner token과 millisecond TTL을 적용한다.
- acquire의 같은-token 재호출을 결정적으로 처리해 모호한 획득 응답을 복구할 수 있게 한다.
- inspect, renew, release에서 전체 소유, 일부 유실, 전체 유실, 다른 owner 점유를
  구분한다.
- sync, `CompletableFuture`, suspend API가 같은 입력에 같은 의미를 제공한다.
- 기존 `RedisScriptRunner`의 EVALSHA 우선/NOSCRIPT fallback을 재사용한다.
- `Retry`, `CircuitBreaker`, `Bulkhead`는 `bluetape4k-resilience4j`에서 외부
  decorator로 조합할 수 있음을 실제 Redis 통합 테스트로 증명한다.
- public KDoc, 영문/한글 README, primitive-family 다이어그램을 함께 갱신한다.

## 3. 비목표

- durable transaction, fencing service, queue, semaphore, reentrant lock을 만들지 않는다.
- inventory, payment, purchase, idempotency ledger를 Redis lease로 대체하지 않는다.
- Redis failover나 성공한 lease를 business transition 승인으로 간주하지 않는다.
- cross-slot atomicity를 흉내 내거나 key를 slot별로 나누어 실행하지 않는다.
- 내부 retry loop, 자동 owner token 생성, 자동 fallback, 새 token 재시도를 제공하지 않는다.
- workshop의 도메인 모델이나 application-owned script를 이 저장소에서 구현하지 않는다.

## 4. 현재 근거

### 4.1 재사용할 구현

- `RedisScript`와 `RedisScriptRunner`: sync/async/suspend EVALSHA 우선 실행 및
  `RedisNoScriptException` 발생 시 EVAL fallback.
- `LettuceLock`: UUID token, positive millisecond TTL 검증, token compare-and-delete,
  실패한 release 이후 token 보존 원칙.
- `LettuceSemaphore`: owner lease, 만료 정리, stale/wrong-owner release 보호,
  sync/async/suspend 동작 정렬.
- `RedisFuture.awaitSuspending()`: coroutine cancellation을 `RedisFuture`에 전파한다.
- Lettuce 7.6.0 `SlotHash.getSlot(String)`: Redis Cluster hash tag 규칙을 포함한
  client-side slot 계산을 제공한다.
- `SuspendDecorators`: 호출 순서대로 기존 함수를 감싸며 `Retry`에서
  `CancellationException`을 재시도하지 않는다.

### 4.2 관련 이력

- projects #944: semaphore permit을 owner token과 TTL에 결합했다.
- projects #949: lock duration 검증과 실패한 release의 token 보존을 확립했다.
- workshop #521: sale-scoped IP/user dual in-flight filtering이 첫 소비자다.

### 4.3 채택과 거절

| 선택지 | 결정 | 이유 |
|---|---|---|
| 전용 result 기반 primitive | 채택 | 단일 lock과 다른 다중-key/partial-loss 의미를 명시적으로 표현한다. |
| `LettuceLock`에 collection overload 추가 | 거절 | 기존 인스턴스 token 상태와 caller token 계약이 충돌한다. |
| Lua helper만 공개 | 거절 | 숫자 상태 코드와 복구 규칙이 모든 caller에 누출된다. |
| lease 내부 Resilience4j 의존/설정 | 거절 | Redis primitive와 resilience 정책을 결합하고 소비자별 구성을 막는다. |
| caller-side decorator | 채택 | 같은 token을 캡처한 채 선택적으로 retry/CB/bulkhead를 조합할 수 있다. |

## 5. 패키지와 구성요소

```text
infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/lease/
├── LettuceMultiKeyLease.kt
├── LettuceSuspendMultiKeyLease.kt
├── LettuceMultiKeyLeaseConfig.kt
├── MultiKeyLeaseResult.kt
└── LettuceMultiKeyLeaseSupport.kt
```

- `LettuceMultiKeyLease`: connection과 config만 보유하며 sync 및
  `CompletableFuture` API를 제공한다.
- `LettuceSuspendMultiKeyLease`: connection과 같은 config를 보유하고 suspend API를
  제공한다.
- `LettuceMultiKeyLeaseConfig`: 인스턴스 정책인 `maxKeys`만 보유한다.
- `MultiKeyLeaseResult`: operation별 sealed result와 count value를 정의한다.
- `LettuceMultiKeyLeaseSupport`: 입력 정규화, slot 검증, Lua script, 반환 decoder를
  내부 구현으로 보유한다.
- `MultiKeyLeaseCrossSlotException`, `MultiKeyLeaseIntegrityException`: key/token을
  노출하지 않고 입력 slot 위반과 lease namespace 손상을 안정된 타입으로 구분한다.
- 기존 `RedisScriptRunner`에는 `RedisScriptingCommands<String, String>`과
  `RedisScriptingAsyncCommands<String, String>` overload를 추가한다. 기존
  `RedisCommands`/`RedisAsyncCommands` overload는 그대로 유지하고 generalized overload에
  위임해 binary compatibility를 보존한다.

두 public class는 로컬 ownership token이나 획득 상태를 저장하지 않는다.

## 6. Public API

### 6.1 Config

```kotlin
data class LettuceMultiKeyLeaseConfig(
    val maxKeys: Int = 32,
) : Serializable
```

- `maxKeys`는 1 이상이어야 한다.
- config는 인스턴스 생성 시 전달한다.
- same-slot, duplicate, token, TTL 검증은 비활성화할 수 없는 안전 계약이다.
- public data class는 `Serializable`과 `serialVersionUID`를 제공한다.

config는 application-owned trusted configuration이다. deserialization 결과를 포함해
primitive 생성자는 `maxKeys >= 1`을 다시 검증하므로 조작된 `Serializable` 값이 생성자
검증을 우회할 수 없다. 신뢰되지 않은 serialized config를 입력으로 받는 것은 지원하지
않는다.

### 6.2 Stable exceptions

```kotlin
enum class MultiKeyLeaseOperation { ACQUIRE, INSPECT, RENEW, RELEASE }

class MultiKeyLeaseCrossSlotException(
    val distinctSlotCount: Int,
): IllegalArgumentException()

class MultiKeyLeaseIntegrityException(
    val operation: MultiKeyLeaseOperation,
    val requestedKeyCount: Int,
    val invalidLeaseKeyCount: Int,
): IllegalStateException()
```

두 exception의 message와 property에는 key와 owner token을 포함하지 않는다.

### 6.3 Sync와 async

```kotlin
class LettuceMultiKeyLease private constructor(
    syncCommands: RedisScriptingCommands<String, String>,
    asyncCommands: RedisScriptingAsyncCommands<String, String>,
    config: LettuceMultiKeyLeaseConfig,
) {
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig(),
    ) : this(connection.sync(), connection.async(), config)

    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig(),
    ) : this(connection.sync(), connection.async(), config)

    fun acquire(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): MultiKeyAcquireResult

    fun acquireAsync(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): CompletableFuture<MultiKeyAcquireResult>

    fun inspect(
        keys: Collection<String>,
        ownerToken: String,
    ): MultiKeyInspectResult

    fun inspectAsync(
        keys: Collection<String>,
        ownerToken: String,
    ): CompletableFuture<MultiKeyInspectResult>

    fun renew(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): MultiKeyRenewResult

    fun renewAsync(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): CompletableFuture<MultiKeyRenewResult>

    fun release(
        keys: Collection<String>,
        ownerToken: String,
    ): MultiKeyReleaseResult

    fun releaseAsync(
        keys: Collection<String>,
        ownerToken: String,
    ): CompletableFuture<MultiKeyReleaseResult>
}
```

### 6.4 Suspend

```kotlin
class LettuceSuspendMultiKeyLease private constructor(
    asyncCommands: RedisScriptingAsyncCommands<String, String>,
    config: LettuceMultiKeyLeaseConfig,
) {
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig(),
    ) : this(connection.async(), config)

    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig(),
    ) : this(connection.async(), config)

    suspend fun acquire(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): MultiKeyAcquireResult

    suspend fun inspect(
        keys: Collection<String>,
        ownerToken: String,
    ): MultiKeyInspectResult

    suspend fun renew(
        keys: Collection<String>,
        ownerToken: String,
        leaseTime: Duration,
    ): MultiKeyRenewResult

    suspend fun release(
        keys: Collection<String>,
        ownerToken: String,
    ): MultiKeyReleaseResult
}
```

`keys`는 호출 시 immutable `List`로 복사하고 입력 iteration 순서를 유지한다.
token과 key 집합은 모든 재시도에서 caller가 동일하게 유지해야 한다.

standalone constructor는 `connection.sync()/async()`, cluster constructor는
`connection.sync()/async()`가 구현한 scripting interface를 내부 executor에 전달한다.
cluster command는 검증된 첫 key로 node routing되며 나머지 key는 같은 slot임이 dispatch
전에 보장된다. 실제 Redis Cluster fixture가 이 경로를 검증한다.

## 7. 결과 모델

operation별 sealed interface를 사용하고, caller가 전달한 key 값과 다른 owner token은
어떤 result에도 포함하지 않는다. count가 필요한 data class는 `Serializable`과
`serialVersionUID`를 제공한다.

각 상태 이름은 operation별 sealed interface 안에 중첩해 충돌을 피한다. 예를 들어
`MultiKeyAcquireResult.Acquired`, `MultiKeyInspectResult.Owned`,
`MultiKeyRenewResult.Renewed`, `MultiKeyReleaseResult.Released` 형태다. TTL을 제공하는
상태는 `minimumPttlMillis: Long`, 부분/충돌 상태는 아래 공통 count를 property로
제공한다. singleton 상태는 `data object`로 표현한다.

```kotlin
sealed interface MultiKeyAcquireResult: Serializable {
    data object Acquired: MultiKeyAcquireResult
    data class AlreadyOwned(val minimumPttlMillis: Long): MultiKeyAcquireResult
    data class PartialOwnership(val counts: MultiKeyLeaseCounts): MultiKeyAcquireResult
    data class Conflicted(val counts: MultiKeyLeaseCounts): MultiKeyAcquireResult
}

sealed interface MultiKeyInspectResult: Serializable {
    data class Owned(val minimumPttlMillis: Long): MultiKeyInspectResult
    data object Lost: MultiKeyInspectResult
    data class PartialOwnership(val counts: MultiKeyLeaseCounts): MultiKeyInspectResult
    data class Conflicted(val counts: MultiKeyLeaseCounts): MultiKeyInspectResult
}

sealed interface MultiKeyRenewResult: Serializable {
    data object Renewed: MultiKeyRenewResult
    data class PartialLoss(val counts: MultiKeyLeaseCounts): MultiKeyRenewResult
    data object Lost: MultiKeyRenewResult
    data class OwnershipMismatch(val counts: MultiKeyLeaseCounts): MultiKeyRenewResult
}

sealed interface MultiKeyReleaseResult: Serializable {
    data object Released: MultiKeyReleaseResult
    data class PartialRelease(val counts: MultiKeyLeaseCounts): MultiKeyReleaseResult
    data object Lost: MultiKeyReleaseResult
    data class OwnershipMismatch(val counts: MultiKeyLeaseCounts): MultiKeyReleaseResult
}
```

각 nested data class는 `serialVersionUID`를 정의한다. 모든 count는 script가 mutation하기
전에 관찰한 분류이며, `ownedKeys`는 호출 후 남아 있는 key 수가 아니라 입력 token과
일치했던 key 수다. `minimumPttlMillis`는 항상 1 이상이다.

caller는 sealed type을 exhaustive하게 처리한다.

```kotlin
when (val result = lease.acquire(keys, ownerToken, leaseTime)) {
    MultiKeyAcquireResult.Acquired -> startWorkflow()
    is MultiKeyAcquireResult.AlreadyOwned -> recoverExistingAttempt(result.minimumPttlMillis)
    is MultiKeyAcquireResult.PartialOwnership -> reconcile(result.counts)
    is MultiKeyAcquireResult.Conflicted -> reject(result.counts)
}
```

### 7.1 공통 count

```kotlin
data class MultiKeyLeaseCounts(
    val requestedKeys: Int,
    val ownedKeys: Int,
    val missingKeys: Int,
    val mismatchedKeys: Int,
) : Serializable
```

### 7.2 Acquire

- `Acquired`: 모든 key가 비어 있어 같은 token/TTL로 생성됐다.
- `AlreadyOwned`: 모든 key가 같은 token이다. replay는 TTL을 갱신하지 않는다.
- `PartialOwnership`: 같은 token key와 missing key가 섞였다. 아무것도 생성하지 않는다.
- `Conflicted`: 다른 token key가 하나 이상 있다. 아무것도 변경하지 않는다.

`AlreadyOwned`는 모든 key의 최소 `PTTL`을 millisecond로 제공한다.
상태 우선순위는 `mismatched > 0`이면 `Conflicted`, 그렇지 않고
`owned == requested`이면 `AlreadyOwned`, `missing == requested`이면 `Acquired`,
나머지는 `PartialOwnership`이다.

### 7.3 Inspect

- `Owned`: 모든 key가 같은 token이며 최소 잔여 TTL을 제공한다.
- `Lost`: 같은 token key가 하나도 없고 다른 token도 없다.
- `PartialOwnership`: 같은 token과 missing key가 섞였다.
- `Conflicted`: 다른 token key가 하나 이상 있다.

상태 우선순위는 `mismatched > 0`이면 `Conflicted`, 그렇지 않고
`owned == requested`이면 `Owned`, `missing == requested`이면 `Lost`, 나머지는
`PartialOwnership`이다.

### 7.4 Renew

- `Renewed`: 모든 key가 같은 token이며 전부 새 TTL로 갱신됐다.
- `PartialLoss`: 같은 token key만 갱신됐고 일부 key는 missing이다.
- `Lost`: 갱신할 같은 token key가 없다.
- `OwnershipMismatch`: 다른 token key는 변경하지 않고, 남아 있는 같은 token key만
  갱신했으며 count로 결과를 보고한다.

상태 우선순위는 `mismatched > 0`이면 `OwnershipMismatch`, 그렇지 않고
`owned == requested`이면 `Renewed`, `owned > 0 && missing > 0`이면 `PartialLoss`,
나머지 all-missing이면 `Lost`다.

### 7.5 Release

- `Released`: 모든 key가 같은 token이었고 전부 삭제됐다.
- `PartialRelease`: 같은 token key는 삭제됐고 일부 key는 이미 missing이다.
- `Lost`: 삭제할 같은 token key가 없다.
- `OwnershipMismatch`: 다른 token key는 삭제하지 않고, 남아 있는 같은 token key만
  compare-and-delete했으며 count로 결과를 보고한다.

상태 우선순위는 `mismatched > 0`이면 `OwnershipMismatch`, 그렇지 않고
`owned == requested`이면 `Released`, `owned > 0 && missing > 0`이면
`PartialRelease`, 나머지 all-missing이면 `Lost`다. renew/release count는 mutation 전
분류를 나타낸다.

## 8. 입력 검증과 Redis Cluster 계약

Redis를 호출하기 전에 다음 순서로 검증한다.

1. 빈 iterator는 거부한다. 원소를 추가하기 전에 현재 snapshot size가 `maxKeys`인지
   비교하고 초과 원소가 있으면 즉시 거부해 `maxKeys + 1` 정수 overflow 없이 bounded
   immutable snapshot을 만든다.
2. 모든 key는 non-blank다.
3. 중복 key가 없어야 한다.
4. `ownerToken`은 non-blank opaque 값이다.
5. acquire/renew TTL은 양수이고 `toMillis() >= 1`이다.
6. `SlotHash.getSlot(key)` 결과가 모두 동일하다.

TTL 변환은 Redis 호출 전에 수행한다. `Duration.toMillis()`가 overflow하면 그 예외를
그대로 전파하며, sub-millisecond 양수 duration은 lease가 즉시 만료되는 것을 막기 위해
거부한다. token 길이에 별도 product 정책을 두지 않으며 config에는 승인된 `maxKeys`만
둔다.

key와 token은 application이 생성한 신뢰된 Redis identifier다. 이 API는 임의 사용자
payload를 직접 받는 admission boundary가 아니므로 별도 byte-size 정책을 추가하지 않는다.
소비자는 Redis/Lettuce command-size 한도 안에서 bounded identifier를 사용해야 한다.
`maxKeys` 역시 trusted operational setting이지 hostile-input hard cap이 아니다. 기본 32를
권장하며 값을 높이면 §12.3 성능 검증과 Redis command timeout/메모리 영향 검토가
필수다. 구현은 `Int.MAX_VALUE` 설정에서도 count arithmetic overflow가 없어야 한다.
owner token은 인증 credential이 아니라 stale owner를 구분하는 caller capability다. 매
논리 획득마다 고엔트로피 임의 값을 생성하고 JWT, session token, 사용자 식별자, PII를
재사용하지 않는다. token은 Redis에 평문 값으로 저장되므로 Redis ACL과 TLS가 보안
경계이며, key/token을 log, metric label, exception에 기록하지 않는다.

cross-slot 입력은 public `MultiKeyLeaseCrossSlotException`으로 거부한다. 예외는
`IllegalArgumentException`을 상속하며 slot 개수만 제공하고 key 값은 메시지나
property에 노출하지 않는다.

API는 hash tag 자체를 강제하지 않는다. 자연스럽게 같은 slot인 key와
`ticket:{saleId}:...`처럼 shared hash tag를 가진 key를 모두 허용한다. slot이 다르면
분할 실행이나 best-effort rollback 없이 즉시 실패한다.

## 9. Lua 원자성

각 operation은 하나의 `RedisScript`를 사용한다. steady state에서는 한 번의
`EVALSHA` 왕복이며, `NOSCRIPT` 경로에서는 실패한 `EVALSHA`와 fallback `EVAL`로 두 번
왕복한다. `KEYS`에는 검증된 전체 key 목록을 전달하고, `ARGV`에는 owner token과 TTL만
전달한다.

모든 script의 시간 복잡도는 O(n), 추가 Lua 메모리는 O(1)이며 `n <= maxKeys`다.
최악의 내부 Redis command 수는 acquire/inspect/renew/release 모두 key당 값 조회 한 번과
상태별 `PTTL`, `SET`, `PEXPIRE`, `DEL` 중 최대 한 번, 즉 `2n`이다. 기본 `maxKeys = 32`는
한 번의 script에서 최대 64개 내부 command로 작업을 제한하는 보수적 상한이다.
`maxKeys`를 높이는 소비자는 아래 성능 characterization을 다시 실행해야 한다.

### 9.1 Acquire script

1. 전체 key의 값과 same-token key의 `PTTL`을 읽어 `owned`, `missing`, `mismatched`,
   `invalidTtl` count를 계산한다.
2. `invalidTtl > 0`이면 write 없이 integrity code를 반환한다.
3. 전부 missing이면 모든 key에 `SET key token PX ttl`을 실행한다.
4. 전부 같은 token이면 key를 변경하지 않고 최소 `PTTL`을 반환한다.
5. mismatched가 있으면 `Conflicted` code를 반환한다.
6. 그 외에는 `PartialOwnership` code를 반환한다.

Redis Lua의 단일-thread 실행 때문에 검사와 전체 생성 사이에 다른 command가 끼어들지
않는다. 모든 분류와 인자 검증을 첫 write 전에 완료하므로 정상 경쟁/충돌 실패 경로는
key를 하나도 변경하지 않는다. Redis server/runtime failure가 write loop 중 발생하면 Lua는
transaction rollback을 제공하지 않으므로 결과는 모호하며 same-token inspect/release와
durable authority 확인이 필요하다.

### 9.2 Inspect script

전체 값을 읽고 count와 same-token key의 `PTTL`을 검사하며 key를 변경하지 않는다.
모든 key가 same-token인 `Owned`에서만 최소 `PTTL`을 반환하고, partial/conflict/lost
상태의 TTL field는 `-1`이다. same-token key 중 `PTTL == -1`이 있으면 integrity code를
반환한다.

### 9.3 Renew script

write 전에 전체 key를 분류하고 same-token key의 TTL integrity를 확인한다.
`invalidTtl > 0`이면 아무것도 변경하지 않는다. 그렇지 않으면 같은 token key만
`PEXPIRE`하고 다른 token key는 절대 갱신하지 않는다.

### 9.4 Release script

각 key를 검사해 같은 token이면 `DEL`, missing이면 missing count, 다른 token이면
mismatched count를 증가시킨다. 다른 token key는 절대 삭제하지 않는다.
release는 same-token persistent key도 명시적 복구 수단으로 삭제할 수 있으며, 다른
operation과 달리 `PTTL == -1`만으로 integrity exception을 발생시키지 않는다.

### 9.5 반환 encoding

script는 `ScriptOutputType.MULTI`로 다음 숫자 vector를 반환한다.

```text
[statusCode, requested, owned, missing, mismatched, invalidTtl, minimumPttlMillis]
```

공통 decoder는 vector 길이와 숫자 범위를 내부 invariant로 검증한 뒤 operation별 sealed
result로 변환한다. raw status code는 public API에 노출하지 않는다.

primitive가 생성하거나 갱신한 key에는 항상 positive TTL이 있다. 같은 namespace를 다른
writer가 직접 수정해 token은 같지만 TTL이 없는 key(`PTTL == -1`)를 만들면 acquire,
inspect, renew는 public `MultiKeyLeaseIntegrityException`을 발생시킨다. 이 예외는
`IllegalStateException`을 상속하고 `operation: MultiKeyLeaseOperation`,
`requestedKeyCount`, `invalidLeaseKeyCount`만 제공하며 key/token은 노출하지 않는다.
release는 같은 token key를 삭제하는 recovery operation으로 유지한다. 이는 성공으로
오인할 수 없는 namespace-integrity 위반이며 README/KDoc에 해당 key namespace를 lease
primitive가 독점해야 한다고 명시한다.

decoder invariant는 `requested == owned + missing + mismatched`, 모든 count가 0 이상,
`invalidTtl <= owned`, TTL-bearing success의 `minimumPttlMillis >= 1`, 그 외 상태의
`minimumPttlMillis == -1`이다. 상태 우선순위와 맞지 않는 count 조합, 알 수 없는 code,
길이가 다른 vector는 `IllegalStateException`으로 거부한다.

## 10. Async와 cancellation

- sync는 `RedisScriptRunner.run`을 사용한다.
- async는 `RedisScriptRunner.runAsync` 결과를 `thenApply`로 decode한다.
- suspend는 `RedisScriptRunner.runSuspending`을 사용한다.
- 모든 public API는 같은 validation과 decoder를 공유한다.
- 예외를 광범위하게 감싸거나 다른 타입으로 변환하지 않는다.
- suspend 취소는 `CancellationException`을 그대로 전파하고 자동 재시도하지 않는다.
- 반환된 `CompletableFuture.cancel()`은 caller의 대기를 취소하지만 server-side script 실행
  취소를 보장하지 않는다. `thenApply` future 취소가 upstream `RedisFuture`를 중단한다는
  계약도 제공하지 않는다.
- async timeout/cancellation 또는 suspend 취소가 Redis dispatch 이후 발생하면 server-side
  실행 여부가 모호할 수 있다.
- primitive에 로컬 ownership 상태가 없으므로 취소가 인스턴스 상태를 손상시키지 않는다.

모든 mutating operation의 모호한 완료에서 새 token 재시도는 금지한다.

| Operation | 같은-token 복구 | 해석 한계 |
|---|---|---|
| acquire | `acquire` 또는 `inspect` 재호출 | `Acquired` 응답 유실은 `AlreadyOwned`/`Owned`로 결정적으로 확인한다. |
| renew | 먼저 `inspect`; 필요하면 같은 token으로 `renew` | 재호출은 TTL을 재호출 시점부터 다시 기산하므로 원 응답을 재현하지 않는다. |
| release | 같은 token으로 `inspect` 또는 `release` | `Lost`는 선행 release 성공과 자연 만료를 구분하지 못하므로 durable authority로 판단한다. |

KDoc과 README는 이 operation별 차이와 `CompletableFuture` cancellation 계약을 포함한다.

## 11. Resilience4j 외부 decorator

`bluetape4k-lettuce` production dependency에는 Resilience4j를 추가하지 않는다.
`infra/lettuce/build.gradle.kts`의 test scope에만 다음 dependency를 추가한다.

```kotlin
testImplementation(project(":bluetape4k-resilience4j"))
```

권장 suspend 조합은 다음과 같다.

```kotlin
val ownerToken = UUID.randomUUID().toString()
val retry = Retry.of(
    "multi-key-lease",
    RetryConfig.custom<MultiKeyAcquireResult>()
        .maxAttempts(2)
        .waitDuration(Duration.ZERO) // example/test; production은 bounded backoff 사용
        .retryOnException { error ->
            error is IOException ||
                error is RedisConnectionException ||
                error is RedisCommandTimeoutException
        }
        .build(),
)

val acquire = SuspendDecorators
    .ofSupplier {
        lease.acquire(keys, ownerToken, Duration.ofSeconds(10))
    }
    .withRetry(retry)
    .withCircuitBreaker(circuitBreaker)
    .withBulkhead(bulkhead)
    .decorate()

val result = acquire()
```

호출 순서는 다음 의미를 갖는다.

- `Retry`가 가장 안쪽에서 동일 token의 Redis/network 예외만 재시도한다.
- `CircuitBreaker`가 retry 전체를 하나의 논리 호출로 관찰한다.
- `Bulkhead`가 가장 바깥쪽에서 논리 호출 하나당 permit 하나를 유지한다.
- `Conflicted`, `PartialOwnership`, `OwnershipMismatch`는 정상 result이므로 retry나
  circuit breaker 실패로 바꾸지 않는다.
- `IllegalArgumentException`, `MultiKeyLeaseIntegrityException`,
  `CancellationException`은 retry predicate에 포함하지 않는다. coroutine decorator는
  cancellation을 즉시 다시 던진다.
- token은 decorator 밖에서 한 번만 생성한다.

README 예제는 실제 통합 테스트와 같은 순서를 사용한다.

## 12. 테스트 설계

### 12.1 공통 behavioral contract

sync, async, suspend adapter가 같은 시나리오 표를 공유하도록 test fixture를 구성한다.

- 전체 acquire 및 전체 release
- 같은 token replay와 TTL 비연장
- inspect/renew/release 결과 parity
- invalid key/token/TTL/config
- same-slot과 cross-slot
- `SCRIPT FLUSH` 이후 실패한 EVALSHA 1회와 fallback EVAL 1회로 끝나는 NOSCRIPT 복구
- `CompletableFuture.cancel()`이 server-side 미실행을 의미하지 않으며 같은-token 복구가
  partial ownership 없이 종료되는 async cancellation 계약
- 잘못된 길이/code/count/TTL sentinel을 가진 vector를 거부하는 decoder negative cases

NOSCRIPT fixture는 전용 Redis container 또는 JUnit resource lock으로 script cache를
격리한다. sync/async/suspend 각각 `EVALSHA -> NOSCRIPT -> EVAL` 성공을 검증하고 다른
테스트의 cache 상태에 의존하지 않는다.

### 12.2 적대적 Redis fixture

- 겹치는 key 집합으로 동시에 acquire한 두 caller 중 하나만 승리한다.
- loser key는 하나도 생성되지 않는다.
- wrong/stale token renew/release가 새 owner key를 변경하지 않는다.
- TTL 만료 후 새 owner가 전체 key를 획득한다.
- 한 key를 강제로 삭제한 뒤 renew는 `PartialLoss`, release는 `PartialRelease`를 반환한다.
- 한 key를 새 owner token으로 바꾼 뒤 mismatch count와 wrong-owner 보호를 검증한다.
- 같은 token을 가진 key의 TTL을 외부에서 제거하면 namespace-integrity 위반으로
  acquire/inspect/renew에서 `MultiKeyLeaseIntegrityException`이 발생하고 release로
  정리할 수 있다.
- cancellation 전파는 controllable pending `RedisFuture` test double로 결정적으로
  검증한다. 실제 Redis fixture는 dispatch barrier를 둔 뒤 취소하고 결과를 전체 획득 또는
  전체 미획득으로 제한하며 partial ownership은 허용하지 않는다.
- TTL 만료/비연장 검증은 exact sleep/equality 대신 bounded eventual polling과 허용 PTTL
  범위를 사용한다.
- `RedisClusterServer` 기반 실제 cluster에서 standalone과 같은 sync/async/suspend
  same-slot contract를 실행하고 cross-slot 입력이 dispatch 전에 거부되는지 검증한다.

Testcontainers-backed Redis 검증은 다른 module/worktree와 병렬 실행하지 않는다.

### 12.3 성능 characterization

기본 테스트와 분리된 performance-tag fixture에서 acquire/release cycle을 key count
`1/8/32`, caller concurrency `1/16` 조합으로 실행한다. 각 조합은 warm-up 후 같은 횟수의
측정 sample을 수집하고 다음을 기록한다.

- lease operation p50/p95 latency와 throughput
- timeout/error count
- 고경합 측정 중 lease workload와 분리된 전용 `StatefulRedisConnection`에서 고정 주기로
  실행한 `PING` probe의 p95/p99 latency

환경 간 절대 시간은 pass/fail 기준으로 사용하지 않는다. 같은 실행 안에서 8-key와
32-key의 key당 p95가 4배를 초과해 악화되지 않고, timeout/error가 0이며, probe가
connection command timeout을 넘지 않는 것을 회귀 기준으로 삼는다. 이 fixture의 fresh
결과를 PR evidence에 남기며 기본값 32나 내부 command 구조를 바꾸면 반드시 재실행한다.

### 12.4 Resilience4j 통합 fixture

`LettuceMultiKeyLeaseResilience4jTest`는 실제 Redis를 사용한다.

1. 첫 decorated 호출이 acquire를 실제로 성공시킨 뒤 테스트용 `IOException`을 던져
   응답 유실을 재현한다.
2. `Retry`가 같은 외부 token으로 두 번째 호출한다.
3. 최종 result가 `AlreadyOwned`인지 확인한다.
4. attempt 2회, Retry success-with-retry 1건, CircuitBreaker logical success 1건,
   Bulkhead permit 복구를 확인한다.
5. 별도 conflict fixture에서는 `Conflicted` result가 attempt 1회로 끝나며 breaker 실패나
   permit 누수가 없는지 확인한다.
6. argument, integrity, cancellation 예외가 attempt 1회로 끝나는 negative fixture를
   둔다. fixture의 Retry는 `maxAttempts=2`, zero test interval, 위 예제와 같은 exception
   predicate를 사용한다.

## 13. 문서와 contributor surface

- 새 public class, config, result, exception에 English KDoc을 작성한다.
- KDoc은 ownership, replay, expiry, partial loss, cross-slot, cancellation, advisory boundary와
  최소 예제를 포함한다.
- `infra/lettuce/README.md`와 `README.ko.md`를 source-equivalent하게 갱신한다.
- README에는 기본 사용, same-token 복구, `Retry + CircuitBreaker + Bulkhead` 조합,
  lease key namespace의 단일 writer 원칙, 운영 제한과 durable authority 경계를 포함한다.
- 기본 예제는 shared hash tag, 외부 token 수명, `Acquired`/`AlreadyOwned`, conflict,
  partial renew/release를 exhaustive `when`으로 처리한다.
- production dependency 추가 없이 caller telemetry 권고표를 제공한다. metric dimension은
  `operation`, sealed result type, exception type처럼 bounded 값만 사용하고 key/token은
  log와 metric label에서 금지한다. `Partial*`/`OwnershipMismatch`는 warning 및 durable
  상태 확인, integrity exception/cross-slot은 configuration alert, retry exhaustion/
  circuit-open/bulkhead rejection은 availability alert로 분류한다.
- integrity runbook은 외부 writer 중지, 손상된 same-token key의 명시적 release 또는
  안전한 drain, durable authority 확인, 단일 writer 재개 순서를 포함한다. token을
  보유하면 public release로 정리한다. token을 유실한 persistent key는 자연 drain되지
  않으므로 durable authority와 정확한 namespace/key 집합을 운영 승인으로 확인한 뒤 수동
  삭제하거나 namespace를 교체하고, 재검증 후 writer를 재개한다. 이 운영 절차에서도
  key/token을 log나 metric에 기록하지 않는다.
- `scripts/generate-infra-lettuce-diagram-01.mjs`와 생성된 SVG/PNG에
  `LettuceMultiKeyLease` family를 추가하고 English label을 사용한다.
- issue/PR, commit message, KDoc은 English로 유지한다.

## 14. 호환성과 migration

- 기존 `LettuceLock`, `LettuceSuspendLock`, `LettuceSemaphore`,
  `LettuceSuspendSemaphore`의 source/binary behavior를 변경하지 않는다.
- 새 package와 새 public API만 추가한다.
- 새 module이나 production dependency는 없다.
- `bluetape4k-resilience4j`는 test dependency로만 추가한다.
- workshop 소비자는 이 기능이 포함된 Bluetape version이 배포되기 전까지 기존
  application-owned script와 durable database guard를 유지한다.

cutover와 rollback 책임은 application owner에게 있다. 전환은 다음 순서로 수행한다.

1. production과 같은 key가 shared slot인지 사전 검증하고 durable database guard를 유지한다.
2. 기존 application-owned writer를 중지한다.
3. 기존 최대 TTL만큼 drain하거나 기존 token으로 안전하게 정리한다.
4. 기존 key/hash-tag와 token 재사용 계약을 유지한 채 새 primitive writer를 활성화한다.

rollback은 새 writer 중지, lease drain/정리, durable authority 확인, 기존 writer 재활성화의
역순이다. 같은 namespace에서 두 writer를 동시에 실행하는 dual-write는 금지한다.

## 15. 실패 모드와 대응

| 실패 모드 | 신호 | 대응 |
|---|---|---|
| 일부 key가 다른 slot | `MultiKeyLeaseCrossSlotException` | Redis 호출 전 거부하고 shared hash tag 사용을 안내한다. |
| acquire 응답 유실 | caller가 결과를 받지 못함 | 새 token을 만들지 않고 같은 token acquire/inspect로 복구한다. |
| renew/release 응답 유실 | timeout/cancellation/transport exception | 같은 token으로 inspect하고 §10의 operation별 한계에 따라 durable authority에서 판단한다. |
| 한 key 만료/eviction | `PartialOwnership`, `PartialLoss`, `PartialRelease` | 남은 own key만 안전하게 renew/release하고 durable authority에서 workflow를 재판단한다. |
| stale caller release | `OwnershipMismatch` 또는 `Lost` | 다른 token key를 삭제하지 않고 명시적 result를 반환한다. |
| persistent same-token key | `MultiKeyLeaseIntegrityException` | 외부 writer를 중지하고 같은 token release 또는 drain 후 durable authority를 확인한다. |
| `SCRIPT FLUSH` | 내부 EVALSHA miss | `RedisScriptRunner`가 EVAL로 fallback하며 EVAL도 실패할 때만 command failure를 전파한다. |
| script write 중 server/runtime failure | Redis command failure | rollback을 가정하지 않고 같은 token inspect/release와 durable authority로 확인한다. |
| async/coroutine cancellation | cancelled future 또는 `CancellationException` | 취소를 재시도하지 않고 같은 token으로 operation별 복구를 수행한다. |
| retry storm | Retry metric/latency 증가 | 예외만 bounded retry하고 outer bulkhead로 논리 호출 수를 제한한다. |

## 16. Acceptance criteria

- acquire는 정상 경쟁/충돌 경로에서 all-or-nothing이며 conflict 시 partial key를 남기지
  않는다. server/runtime failure는 rollback을 보장하지 않고 모호한 완료로 처리한다.
- 같은 token replay는 두 번째 ownership을 만들지 않고 TTL을 암묵적으로 연장하지 않는다.
- renew/release는 같은 token key만 변경하고 partial/lost/mismatch를 구분한다.
- 같은 slot은 허용하고 cross-slot은 Redis 실행 전에 안정된 exception type으로 거부한다.
- standalone과 실제 Redis Cluster connection이 같은 same-slot contract를 통과한다.
- sync, async, suspend가 같은 behavioral contract를 통과한다.
- cancellation 및 NOSCRIPT fallback fixture가 통과한다.
- Resilience4j 통합 fixture가 same-token retry와 domain-result 비재시도를 증명한다.
- `1/8/32` key와 concurrency `1/16` 성능 characterization이 timeout/error 0과
  normalized p95/probe 회귀 기준을 충족한다.
- public English KDoc, English/Korean README, diagram이 구현과 일치한다.
- 기존 lock/semaphore 회귀 테스트가 통과한다.

## 17. Definition of Done

- 승인된 API와 상태 전이가 source와 테스트에 그대로 반영됐다.
- targeted lease tests와 전체 `:bluetape4k-lettuce:test`가 순차적으로 통과했다.
- affected compile, detekt/static checks, `git diff --check`가 통과했다.
- generated publication POM 또는 production runtime dependency graph에서
  `bluetape4k-resilience4j`가 제외됨을 확인했다.
- spec/plan 검증과 성능·안정성·보안·Ops·API·caller 관점 리뷰에서 P0/P1이 0이다.
- bilingual README, KDoc, diagram, lesson이 최신이다.
- exact local/remote/PR head가 일치하고 CI와 live review가 통과했다.
- merge는 별도의 fresh approval 전에는 수행하지 않는다.
