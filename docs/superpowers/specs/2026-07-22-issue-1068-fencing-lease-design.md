# Issue #1068 Redis fencing lease 설계

## 1. 배경

`infra/lettuce`의 기존 `LettuceLock`, `LettuceSemaphore`,
`LettuceMultiKeyLease`는 opaque owner token과 TTL로 Redis 안의 소유권 변경을
보호한다. 하지만 opaque token은 순서를 비교할 수 없으므로, 만료된 lease holder가
긴 pause 뒤에 깨어나 PostgreSQL이나 outbox 같은 외부 시스템에 늦은 write를 보내는
문제를 막지 못한다.

Issue #1068은 한 logical resource에 대해 ownership generation마다 증가하는
`FencingToken(epoch, sequence)`을 발급하는 Redis primitive를 `bluetape4k-lettuce`에
추가한다. downstream authority는 마지막으로 수락한 token을 저장하고 다음 조건을
원자적으로 적용해야 한다.

```text
accept only when incomingFence > lastAcceptedFence
```

이 primitive가 제공하는 것은 orderable ownership generation이다. exactly-once 실행,
분산 transaction, business idempotency를 대신하지 않는다.

## 2. 목표

- 한 resource의 lease 획득과 fencing sequence 증가를 하나의 Lua 실행으로 처리한다.
- 같은 counter history 안에서 새 ownership generation마다 더 큰 token을 발급한다.
- token을 `(epoch, sequence)` 사전식 순서로 비교해 Redis history recovery 뒤에도 새
  namespace generation이 이전 generation보다 크게 정렬되게 한다.
- active owner가 같은 `FencingOwnerId`로 acquire를 재시도하면 새 sequence를 만들지 않고
  기존 token을 돌려준다.
- renew는 기존 token을 유지하고 release와 함께 owner와 token을 모두 비교한다.
- bootstrap, acquire, inspect, renew, release가 모호한 boolean 대신 명시적인 sealed
  result를 반환한다.
- sync, `CompletableFuture`, suspend API가 같은 validation, Lua, decoder, result 의미를
  공유한다.
- Redis Cluster same-slot, sequence overflow, counter loss, script cache loss,
  cancellation, ambiguous completion의 경계를 문서와 test로 고정한다.
- 모든 public data class, sealed result, nested data object에 `Serializable`과 명시적인
  `serialVersionUID = 1L`을 적용한다. enum은 Java의 고정 enum serialization을 따른다.

## 3. 비목표

- PostgreSQL adapter, repository, outbox, scheduler, domain workflow를 구현하지 않는다.
- `bluetape4k-leader` API나 module dependency를 변경하지 않는다.
- 여러 resource를 한 transaction으로 묶거나 cross-slot fencing을 흉내 내지 않는다.
- Redis 내부 retry, `Retry`, `CircuitBreaker`, `Bulkhead` 정책을 포함하지 않는다.
- Redis counter history 손실 뒤 같은 epoch의 monotonicity를 복구했다고 주장하지 않는다.
- downstream conditional write나 마지막 accepted token 저장을 대신하지 않는다.
- 기존 opaque lock/lease를 fencing token으로 재해석하지 않는다.
- 새 module, 새 dependency, diagram을 추가하지 않는다.

## 4. 현재 근거와 제약

### 4.1 재사용할 저장소 구현

- `RedisScript`와 `RedisScriptRunner`: sync/async/suspend EVALSHA 우선 실행 및
  `NOSCRIPT` 발생 시 EVAL fallback.
- `RedisScriptRunner.runAsync()`가 만드는 chained future만으로는 upstream Lettuce future의
  cancellation 전파를 보장할 수 없다. 구현 plan은 upstream future를 보존하는 internal
  cancellation-propagating wrapper 또는 runner 확장을 먼저 추가하고, EVALSHA와
  `NOSCRIPT` EVAL fallback 양쪽에 취소를 best-effort로 전달한다.
- `LettuceMultiKeyLeaseSupport`: Lua 상태 코드, dispatch 전 validation, 공통 decoder,
  standalone/Cluster scripting command 추상화.
- `RedisFuture.awaitSuspending()`: suspend API cancellation 전파.
- `RedisServer.Launcher`와 `RedisClusterServer.Launcher`: 실제 standalone Redis와
  Cluster 검증 fixture.
- `MultithreadingTester`, `SuspendedJobTester`: 결정적인 동시성 test harness.
- `bluetape4k-assertions`: JUnit 5 test의 matcher와 failure assertion.

### 4.2 Redis가 제공하는 범위

- Redis `INCR`는 signed 64-bit integer 범위에서 동작하며 overflow를 error로 거절한다.
  다만 Lua number로 반환된 큰 정수를 Kotlin `Long`으로 직접 decode하면 IEEE-754 정밀도
  경계를 건드릴 수 있으므로, script는 counter를 decimal string으로 검증하고 mutation
  뒤 `GET` 결과를 반환한다.
- Redis script는 한 번 실행되는 동안 atomic하지만, 네트워크 timeout이나 connection
  failure가 caller에게 command 적용 여부를 알려주지는 않는다.
- Redis Cluster의 multi-key script는 모든 key가 같은 hash slot에 있어야 한다.
- Redis replication은 기본적으로 asynchronous이므로 acknowledged write loss, replica
  promotion, snapshot rollback, disaster restore 뒤에는 이전 counter history가 보존됐다고
  가정할 수 없다.

공식 근거:

- [Redis INCR](https://redis.io/docs/latest/commands/incr/)
- [Redis scripting](https://redis.io/docs/latest/develop/programmability/eval-intro/)
- [Redis EVAL_RO](https://redis.io/docs/latest/commands/eval_ro/) — Redis 7.0 이상 read-only script
- [Redis Cluster specification](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)
- [Redis replication](https://redis.io/docs/latest/operate/oss_and_stack/management/replication/)

### 4.3 채택과 거절

| 선택지 | 결정 | 이유 |
|---|---|---|
| `FencingToken(sequence)`만 사용 | 거절 | counter history loss 뒤 새 sequence를 이전 값보다 크게 보장할 수 없다. |
| `FencingToken(epoch, sequence)` 사전식 비교 | 채택 | application-controlled epoch를 올리면 새 history 전체를 이전 history 뒤에 정렬할 수 있다. |
| acquire가 없는 counter를 자동 생성 | 거절 | 삭제·rollback 뒤 같은 epoch에서 더 작은 sequence를 조용히 발급할 수 있다. |
| 명시적인 `bootstrap()` | 채택 | 최초 생성과 recovery를 operation boundary로 드러내고 acquire는 counter가 없으면 fail closed한다. |
| 외부 durable database가 sequence 발급 | 거절 | Redis primitive 범위를 벗어나며 새 persistence adapter가 필요하다. |
| instance마다 resource/epoch를 config로 고정 | 채택 | key derivation과 ordering domain을 constructor에서 검증하고 operation별 cross-slot 입력을 없앤다. |
| `FencingLeaseHandle(ownerId, token)` 반환 | 거절 | caller가 이미 가진 owner ID를 result에 중복해 노출하고 log/toString 유출 표면을 늘린다. |
| raw `Throwable`을 result에 포함 | 거절 | serialization, 정보 노출, backend coupling을 public API에 전파한다. |
| backend failure category만 반환 | 채택 | caller policy에 필요한 범위만 안정적으로 제공하고 실제 exception은 내부 log에 남긴다. |
| primitive 내부 resilience 정책 | 거절 | Redis operation과 서비스별 retry/CB/bulkhead 설정을 결합한다. |

## 5. Ordering domain과 config

새 public API의 canonical package는 기존 lease 배치와 같은
`io.bluetape4k.redis.lettuce.lease`다. 한 `LettuceFencingLease` instance는 정확히 하나의
`(namespace, resourceName, epoch)`를 담당한다.

```kotlin
data class LettuceFencingLeaseConfig(
    val namespace: String,
    val resourceName: String,
    val epoch: Long,
) : Serializable
```

- `namespace`와 `resourceName`은 각각 1..128자의
  `[A-Za-z0-9._-]+`만 허용한다. `{`, `}`, `:`와 whitespace를 금지해 key/hash-tag
  ambiguity를 제거한다.
- `epoch`는 1 이상이어야 한다. 최초 운영 epoch도 1부터 시작한다.
- Java deserialization은 constructor와 `init`을 우회하므로, config는 private
  `readResolve()`에서 public constructor로 새 instance를 만들어 모든 invariant를 다시
  검증한다. invalid payload는 raw value를 message에 포함하지 않은
  `InvalidObjectException`으로 거절한다.
- config는 application-owned trusted configuration이다. untrusted Java serialization
  payload를 입력으로 받는 것은 지원하지 않는다.
- token 비교는 같은 `namespace`와 `resourceName`에서만 의미가 있다. token 자체에는
  namespace/resource를 포함하지 않으므로 caller는 서로 다른 ordering domain의 token을
  비교하지 않아야 한다.
- config는 `Serializable`을 구현하고 private companion object에
  `serialVersionUID: Long = 1L`을 둔다.

`FencingOwnerId`는 acquire attempt를 식별하는 opaque capability value다.

```kotlin
class FencingOwnerId private constructor(
    internal val value: String,
) : Serializable {
    companion object {
        fun random(): FencingOwnerId
        fun from(value: String): FencingOwnerId
    }

    override fun toString(): String = "FencingOwnerId(<redacted>)"
}
```

- `value`는 blank일 수 없고 UTF-8 기준 1..256 byte여야 한다.
- `random()`은 새 encoder를 만들지 않고 기존 `Base58.randomString(22)`를 사용한다.
  이 helper는 `SecureRandom.getInstanceStrong()`과 58-symbol alphabet을 사용하며 22자는
  약 129-bit의 선택 공간을 제공한다.
- `from(value)`는 외부 attempt ID 연동용이다. caller가 전역 collision/guessing 방지와
  안전한 저장을 책임진다.
- caller는 logical acquisition attempt마다 새 owner ID를 만들고, ambiguous response를
  복구할 때만 같은 값을 재사용한다.
- owner ID를 아는 caller는 active token을 조회하고 같은-owner recovery를 수행할 수
  있으므로 신뢰되지 않은 입력으로 사용하면 안 된다. Redis authentication이나 application
  authorization을 대신하지 않는다.
- owner ID는 orderable token이 아니며 downstream fence로 저장하면 안 된다.
- public result에는 owner ID를 넣지 않는다.
- equality와 hash code는 raw value만 사용하고 `toString()`은 항상 redacted한다.
- `FencingOwnerId`도 `serialVersionUID: Long = 1L`과 constructor 재검증
  `readResolve()`를 정의한다.

## 6. Fencing token

```kotlin
data class FencingToken(
    val epoch: Long,
    val sequence: Long,
) : Comparable<FencingToken>, Serializable {
    override fun compareTo(other: FencingToken): Int {
        val epochComparison = epoch.compareTo(other.epoch)
        return if (epochComparison != 0) epochComparison else sequence.compareTo(other.sequence)
    }

    override fun toString(): String = "FencingToken(<redacted>)"
}
```

- `epoch >= 1`, `sequence >= 1`을 생성 시 검증한다.
- natural ordering은 `(epoch, sequence)`의 lexicographic ordering이다.
- `equals`, `hashCode`, `compareTo`가 같은 두 property만 사용한다.
- library 내부와 기본 operational log가 token 숫자를 우발적으로 기록하지 않도록
  `toString()`을 redacted한다. downstream이 명시적으로 property를 저장·비교하는 것은
  이 제한과 별개다.
- `serialVersionUID: Long = 1L`과 constructor 재검증 `readResolve()`를 정의한다.
- counter는 0으로 bootstrap되고 첫 acquire가 sequence 1을 발급한다.
- sequence가 `Long.MAX_VALUE`이면 새 generation을 만들지 않고
  `SequenceExhausted`를 반환한다. wraparound는 허용하지 않는다. `epoch < Long.MAX_VALUE`
  일 때만 더 높은 epoch로 rollover할 수 있다. 최대 epoch가 소진되면 현재 ordering
  domain은 terminal 상태이며 external schema를 포함한 별도 domain migration이 필요하다.

예를 들어 epoch 7의 마지막 token보다 epoch 8의 첫 token이 항상 크다.

```text
(7, 9223372036854775807) < (8, 1)
```

## 7. Redis key와 저장 상태

config로부터 두 key를 내부에서만 생성한다.

```text
fence:{namespace:resourceName}:<epoch>:lease
fence:{namespace:resourceName}:<epoch>:counter
```

실제 `epoch` 숫자가 `epoch` 자리에 들어간다. 예:

```text
fence:{orders:rebuild}:7:lease
fence:{orders:rebuild}:7:counter
```

- 두 key는 같은 `{namespace:resourceName}` hash tag를 사용하므로 같은 Cluster slot에
  배치된다.
- 모든 epoch가 같은 resource slot을 공유하지만 keyspace는 epoch별로 분리된다.
- 같은 resource의 서로 다른 epoch lease는 Redis key 수준에서는 동시에 존재할 수 있다.
  epoch rollover는 old epoch acquire 중지와 새 config 배포를 포함한 control-plane cutover다.
  동시 활성화가 발생해도 downstream tuple 비교는 낮은 epoch의 stale write를 거절하지만,
  Redis mutual exclusion 자체가 epoch 사이에 유지된다고 주장하지 않는다.
- key는 public API 입력으로 받지 않는다. 따라서 cross-slot validation은 생성된 두
  key의 실제 wire byte slot이 같은지 constructor에서 확인하는 invariant check다.
- connection의 `RedisCodec<String, String>`을 private constructor까지 보존하고
  `SlotHash.getSlot(codec.encodeKey(key))`로 두 key를 검증한다. Kotlin `String` 자체나
  기본 charset으로 slot을 추정하지 않는다.
- 이 keyspace는 fencing lease 전용이다. 다른 command나 application이 직접 수정하면
  integrity가 깨진 것으로 취급한다.

counter key:

- Redis type은 string이어야 하고 canonical non-negative decimal string을 저장한다.
- `STRLEN <= 19`를 `GET`보다 먼저 확인한다.
- TTL을 두지 않는다.
- `PTTL == -1`만 정상이다. `-2`는 missing, 0 이상은 expiring counter integrity
  failure다.
- 정상 operation에서 감소하거나 삭제하지 않는다.

lease key:

- Redis type은 hash여야 하며 `owner`, `epoch`, `sequence` field를 저장한다.
- `HLEN == 3`과 fixed-field `HMGET`만 사용한다. `HGETALL`, `KEYS`, `SCAN`, stored
  cardinality loop를 사용하지 않는다.
- `HSTRLEN`으로 owner 256 byte, epoch/sequence 19 byte 상한을 materialization 전에
  검증한다.
- lease TTL만 millisecond 단위로 둔다.
- `epoch`와 `sequence`는 canonical decimal string으로 저장한다.
- field 누락, 예상하지 않은 field, 잘못된 숫자, config와 다른 epoch, lease sequence보다
  작은 counter는 integrity failure다.
- lease `PTTL == -2`는 absent/expired로 다시 분류하고, `-1`은 TTL 없는 malformed
  lease다. `PTTL >= 0`만 active이며 public TTL은 negative가 될 수 없다.
- 모든 script와 reply는 fixed command count와 fixed field count를 유지한다. steady-state
  operation은 한 번의 EVALSHA network round trip이고 `NOSCRIPT` fallback만 EVAL dispatch를
  한 번 더 추가한다.

한 logical resource의 command는 한 Redis slot/event loop에서 직렬화된다. caller 수를
늘려도 하나의 hot resource가 수평 확장되지는 않는다. 독립 resource는 서로 다른
ordering domain을 사용해 Cluster slot에 분산할 수 있다.

## 8. Bootstrap과 recovery

### 8.1 `bootstrap()`

`bootstrap()`은 새 `(namespace, resourceName, epoch)` counter를 0으로 초기화하는
명시적 operation이다.

1. lease key가 있으면 구조를 검증한다.
2. lease가 있는데 counter가 없으면 초기화하지 않고 integrity failure를 반환한다.
3. counter가 있으면 canonical decimal과 범위를 검증하고 `AlreadyInitialized`를 반환한다.
4. lease가 없고 counter도 없을 때만 `SET counter 0 NX`와 동등한 원자적 초기화를 한다.
5. concurrent bootstrap 중 다른 caller가 먼저 초기화하면 다시 검증하고
   `AlreadyInitialized`를 반환한다.

`bootstrap()`의 존재는 same-epoch recovery가 안전하다는 뜻이 아니다. 최초 배포나 아직
사용되지 않은 더 높은 epoch의 초기화에만 사용한다.

### 8.2 History loss recovery

counter missing, rollback, restore가 의심되면 다음 절차를 따른다.

1. external incident detector가 replica promotion, acknowledged-write loss, snapshot restore,
   disaster restore, counter missing/reset 같은 신호를 감지하면 해당 ordering domain의 acquire와
   downstream write를 즉시 중지한다. Redis primitive가 이 사건을 완전히 탐지한다고 주장하지
   않는다.
2. downstream에 저장된 마지막 `(epoch, sequence)`와 Redis recovery 사건을 확인한다.
3. 기존 epoch에 `bootstrap()`하지 않는다. `CounterUnavailable` 결과만으로 bootstrap을
   허가하지 않는다.
4. old epoch acquire가 실제로 차단됐고 active lease가 없어졌음을 read-only inspection과
   downstream writer drain으로 확인한다. mixed-epoch writer가 발견되면 cutover를 중단한다.
5. drain이 확인된 뒤 durable epoch authority에서 single-writer transaction 또는
   compare-and-set으로 현재보다 큰 epoch를 정확히 한 번 할당한다. application instance의
   local config나 Redis counter를 epoch authority로 사용하지 않는다.
6. 새 config로 instance를 만들고 새 epoch를 `bootstrap()`한 뒤 counter type, non-expiring
   TTL, readiness와 downstream tuple guard를 검증한다.
7. 새 writer를 점진 배포하고 old epoch write 부재를 확인한 뒤 acquire와 downstream write를
   재개한다.

Redis만 보고 기존 counter가 과거보다 작아졌는지는 완전히 감지할 수 없다. lease와
counter가 함께 일관된 과거 snapshot으로 돌아가면 구조는 정상처럼 보일 수 있다.
최종 stale rejection은 downstream이 마지막 accepted token을 durable하게 저장하고 더
작거나 같은 token을 거절해야 성립한다.

### 8.3 Epoch cutover와 rollback

정상 rollover도 8.2와 같은 durable single-writer/CAS epoch authority를 사용한다. 두 배포가
서로 다른 higher epoch를 동시에 활성화하면 mutual exclusion이 깨질 수 있으므로
`stop old acquire -> drain active lease/writer -> allocate higher epoch -> bootstrap -> readiness
verify -> roll out -> confirm old absence -> resume` 순서를 runbook invariant로 둔다.

cutover 뒤 application binary를 rollback하더라도 epoch를 낮추지 않는다. rollback binary가
현재 higher epoch config와 tuple guard를 지원하면 그 epoch를 유지하고, 지원하지 않으면
traffic을 중지한 채 호환 binary로 roll-forward하거나 다시 더 높은 epoch로 새 cutover를
수행한다. lower epoch 재활성화와 같은-epoch counter 재초기화는 금지한다. deterministic
rollback simulation은 old writer 차단, mixed-epoch 탐지, higher-epoch 유지, stale write
거절을 검증한다. Redis topology promotion 자체는 환경 의존적이므로 별도 opt-in tagged
integration test로 두고 기본 CI에서는 runbook/fault-injection simulation으로 대체한다.

## 9. Public API

### 9.1 Sync와 `CompletableFuture`

```kotlin
class LettuceFencingLease private constructor(
    syncCommands: RedisScriptingCommands<String, String>,
    asyncCommands: RedisScriptingAsyncCommands<String, String>,
    codec: RedisCodec<String, String>,
    config: LettuceFencingLeaseConfig,
) {
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ) : this(connection.sync(), connection.async(), connection.codec, config)

    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ) : this(connection.sync(), connection.async(), connection.codec, config)

    fun bootstrap(): FencingBootstrapResult
    fun bootstrapAsync(): CompletableFuture<FencingBootstrapResult>

    fun acquire(
        ownerId: FencingOwnerId,
        leaseTime: Duration,
    ): FencingAcquireResult

    fun acquireAsync(
        ownerId: FencingOwnerId,
        leaseTime: Duration,
    ): CompletableFuture<FencingAcquireResult>

    fun inspect(ownerId: FencingOwnerId): FencingInspectResult
    fun inspectAsync(ownerId: FencingOwnerId): CompletableFuture<FencingInspectResult>

    fun renew(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): FencingRenewResult

    fun renewAsync(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): CompletableFuture<FencingRenewResult>

    fun release(
        ownerId: FencingOwnerId,
        token: FencingToken,
    ): FencingReleaseResult

    fun releaseAsync(
        ownerId: FencingOwnerId,
        token: FencingToken,
    ): CompletableFuture<FencingReleaseResult>
}
```

### 9.2 Suspend

```kotlin
class LettuceSuspendFencingLease private constructor(
    asyncCommands: RedisScriptingAsyncCommands<String, String>,
    codec: RedisCodec<String, String>,
    config: LettuceFencingLeaseConfig,
) {
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ) : this(connection.async(), connection.codec, config)

    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ) : this(connection.async(), connection.codec, config)

    suspend fun bootstrap(): FencingBootstrapResult
    suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult
    suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult
    suspend fun renew(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): FencingRenewResult
    suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult
}
```

두 execution class는 Redis connection을 보유하므로 `Serializable`을 구현하지 않는다.
모든 operation은 caller가 준 argument를 dispatch 전에 검증한다. 특히 TTL은 positive
millisecond로 정확히 변환 가능해야 하며 0, negative, sub-millisecond, overflow
duration을 거절한다. `Duration.toMillis()`의 `ArithmeticException`을 포함한 변환 실패는
raw exception을 노출하지 않고 `IllegalArgumentException`으로 정규화한다. 변환된 TTL은
Lua가 integer와 `PTTL` reply를 정확히 다룰 수 있도록 `2^53 - 1` milliseconds 이하여야 하며,
이 상한 초과도 Redis dispatch 전에 `IllegalArgumentException`으로 거절한다.
`renew`와 `release`는 `token.epoch == config.epoch`를 dispatch 전에 검증하며 다른 epoch는
`IllegalArgumentException`이다. 같은 tuple을 가진 다른 resource의 token은 token 자체에
domain identity가 없어 검출할 수 없으므로, caller가 resource-bound handle/storage로
혼용을 막아야 하며 이 한계를 KDoc에 명시한다.

## 10. Result model

### 10.1 공통 failure value

```kotlin
enum class FencingBackendFailureKind {
    CONNECTION,
    TIMEOUT,
    COMMAND,
}

data class FencingLeaseBackendFailure(
    val kind: FencingBackendFailureKind,
) : Serializable

enum class FencingIntegrityFailureKind {
    MALFORMED_LEASE,
    INVALID_COUNTER,
    COUNTER_BEHIND_LEASE,
}

data class FencingLeaseIntegrityFailure(
    val kind: FencingIntegrityFailureKind,
) : Serializable
```

- public failure value는 message, Redis key, owner ID, fencing token, raw reply,
  `Throwable`을 포함하지 않는다.
- actual `Throwable`, message, cause, Redis command text는 log에 전달하지 않는다. allowlist된
  exception class name, operation, failure kind만 structured field로 남기고
  key/owner/token/raw argument는 기록하지 않는다.
- decoder invariant 또는 programmer bug는 `BackendFailure`로 평탄화하지 않는다.
- `CancellationException`은 broad catch보다 먼저 다시 던진다.
- sync/future/suspend는 하나의 internal backend classifier를 공유한다. classifier는
  `CompletionException`과 `ExecutionException`의 cause chain만 순환 방지와 bounded depth로
  정규화하고, chain 어디에 있든 `CancellationException`은 다시 던진다. internal decoder
  exception과 validation exception도 분류 대상에서 제외한다. 정규화한 원인이
  `RedisConnectionException`이면 `CONNECTION`, `RedisCommandTimeoutException` 또는 command
  timeout `TimeoutException`이면 `TIMEOUT`, 그 밖의 Lettuce `RedisException`이면 fallback
  `COMMAND`다. 알려지지 않은 non-Lettuce exception은 programmer/internal failure로 다시
  던지며 raw message나 cause를 public result에 넣지 않는다.
- enum을 포함한 public failure type과 모든 nested result는 Java serialization round-trip
  test를 가진다. enum은 Java가 본래 제공하는 serialization을 사용하고, 나머지 sealed
  result/data variant는 `serialVersionUID = 1L`을 명시한다. constructor invariant가 있는
  config/value/result data class는 private
  `readResolve()`에서 canonical constructor를 다시 호출하고 invalid serialized payload를
  `InvalidObjectException`으로 거절한다.

### 10.2 Bootstrap

```kotlin
sealed interface FencingBootstrapResult : Serializable {
    data object Initialized : FencingBootstrapResult
    data object AlreadyInitialized : FencingBootstrapResult
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ) : FencingBootstrapResult
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ) : FencingBootstrapResult
}
```

### 10.3 Acquire

```kotlin
sealed interface FencingAcquireResult : Serializable {
    data class Acquired(val token: FencingToken) : FencingAcquireResult
    data class AlreadyOwned(
        val token: FencingToken,
        val remainingTtlMillis: Long,
    ) : FencingAcquireResult
    data class Contended(val remainingTtlMillis: Long) : FencingAcquireResult
    data object CounterUnavailable : FencingAcquireResult
    data object SequenceExhausted : FencingAcquireResult
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ) : FencingAcquireResult
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ) : FencingAcquireResult
}
```

### 10.4 Inspect

```kotlin
sealed interface FencingInspectResult : Serializable {
    data class Owned(
        val token: FencingToken,
        val remainingTtlMillis: Long,
    ) : FencingInspectResult
    data object Lost : FencingInspectResult
    data class Contended(val remainingTtlMillis: Long) : FencingInspectResult
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ) : FencingInspectResult
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ) : FencingInspectResult
}
```

### 10.5 Renew와 release

```kotlin
sealed interface FencingRenewResult : Serializable {
    data object Renewed : FencingRenewResult
    data object Lost : FencingRenewResult
    data object OwnershipMismatch : FencingRenewResult
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ) : FencingRenewResult
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ) : FencingRenewResult
}

sealed interface FencingReleaseResult : Serializable {
    data object Released : FencingReleaseResult
    data object Lost : FencingReleaseResult
    data object OwnershipMismatch : FencingReleaseResult
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ) : FencingReleaseResult
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ) : FencingReleaseResult
}
```

각 sealed interface, nested data class, `data object`에
`serialVersionUID: Long = 1L`을 명시한다. nested value까지 빠짐없이 적용하고
reflection/round-trip test로 누락을 막는다. TTL을 담는 result는
`remainingTtlMillis >= 0`을 constructor와 `readResolve()`에서 검증한다.
serialized `data object`는 각각 private `readResolve()`가 canonical singleton instance를
반환하게 해 Java deserialization 뒤 reference identity까지 보존한다.

### 10.6 Caller action 결정표

| 결과 | caller action |
|---|---|
| `Initialized`, `AlreadyInitialized` | readiness를 확인하고 승인된 epoch rollout을 계속한다. |
| `Acquired`, `AlreadyOwned`, `Owned`, `Renewed` | ownership이 유효한 정상 경로를 계속한다. token은 resource identity와 함께 저장한다. |
| `Released` | local ownership을 즉시 폐기하고 downstream write를 금지한다. |
| acquire `Contended` | TTL 이후 새 owner attempt를 시작하거나 bounded backoff한다. 같은 호출을 backend retry로 취급하지 않는다. |
| inspect `Contended` | 다른 owner가 active하므로 local ownership을 즉시 폐기하고 downstream write를 금지한다. |
| `Lost`, `OwnershipMismatch` | local ownership을 폐기하고 downstream write를 금지한다. |
| `CounterUnavailable` | acquire를 중지하고 최초 배포인지 history-loss incident인지 운영 절차로 판별한다. 이 결과만으로 bootstrap하지 않는다. |
| `SequenceExhausted` | retry하지 않고 acquire를 중지하며 higher-epoch cutover를 alert한다. epoch도 최대면 ordering domain/schema migration 전까지 domain을 freeze한다. |
| `IntegrityFailure` | retry와 mutation을 중지하고 read-only 진단 및 runbook을 수행한다. |
| `BackendFailure` | ambiguous completion으로 보고 operation별 reconcile을 수행한다. 정책상 retry할 때만 같은 owner/token을 사용한다. |

token tuple만으로 ordering domain을 식별할 수 없으므로 downstream record와 in-memory handle은
항상 stable resource/domain identity와 `(epoch, sequence)`를 함께 보관한다.

## 11. Lua operation semantics

모든 script는 반환 field 수와 decimal string을 고정한 internal wire protocol을 사용한다.
Kotlin decoder는 예상하지 않은 status, field 수 또는 정상 status에 딸린 malformed
TTL/token/numeric payload를 stable internal protocol exception으로 처리하며 raw reply를
exception message나 log에 넣지 않는다. Redis key에서 script가 직접 관찰한 malformed
state만 public `IntegrityFailure`로 분류한다.

lease가 존재하는 모든 operation은 lease field뿐 아니라 counter가 존재하고 canonical하며
`counter >= lease.sequence`인지 mutation 전에 확인한다. active lease에서 counter가
없거나 뒤처졌다면 inspect를 포함해 integrity failure로 fail closed한다. 64-bit 값을
Lua `tonumber`로 변환해 비교하지 않고 decimal string의 길이와 lexicographic ordering을
사용한다.

Redis scripting의 atomicity는 다른 command와 interleave되지 않는다는 뜻이며 runtime
error 이전 write를 rollback한다는 뜻이 아니다. 모든 type, field count, value length,
decimal, TTL, argument를 첫 write 전에 O(1) command로 preflight한다. acquire mutation은
`INCR -> HSET -> PEXPIRE` 순서다.

- `INCR` 뒤 lease write 전에 중단되면 sequence gap은 허용한다. token은 증가만 하므로
  safety를 약화하지 않는다.
- `HSET` 뒤 `PEXPIRE` 전에 예상하지 못한 runtime failure가 나면 TTL 없는 partial lease가
  남을 수 있다. 다음 operation은 `PTTL == -1`을 `MALFORMED_LEASE`로 fail closed한다.
- operator는 incident 확인 뒤 partial lease만 제거할 수 있지만 counter를 감소·삭제하거나
  같은 epoch를 bootstrap하면 안 된다.
- script는 expected validation/error branch에서 write를 시작하지 않으며 모든 정상 reply는
  fixed-size다.

### 11.1 Integrity/result 결정표

| Redis/wire 상태 | Public/internal 분류 |
|---|---|
| lease wrong type, `HLEN != 3`, field 누락/초과, oversized owner/epoch/sequence, invalid lease decimal, config와 다른 epoch, lease `PTTL == -1` | `MALFORMED_LEASE` |
| active lease의 counter missing/wrong type/oversized/invalid/expiring | `INVALID_COUNTER` |
| lease 없음 + counter missing | acquire `CounterUnavailable`; inspect/renew/release는 각 operation의 `Lost` |
| valid counter < valid lease sequence | `COUNTER_BEHIND_LEASE` |
| lease `PTTL == -2` | absent/expired branch로 재분류 |
| lease `PTTL >= 0` | active; non-negative TTL만 public result에 포함 |
| unknown script status, 정상 status의 잘못된 reply field count 또는 malformed TTL/token/numeric payload | stable internal protocol exception; `BackendFailure`나 `IntegrityFailure`로 평탄화하지 않음 |

### 11.2 Acquire

1. lease가 있으면 `owner`, `epoch`, `sequence`, TTL과 counter integrity를 검증한다.
2. 같은 owner면 sequence를 증가시키지 않고 `AlreadyOwned(existingToken, pttl)`을
   반환한다.
3. 다른 owner면 mutation 없이 `Contended(pttl)`을 반환한다.
4. lease가 없으면 counter 존재와 canonical decimal을 검증한다.
5. counter가 없으면 `CounterUnavailable`, 값이 `Long.MAX_VALUE`면
   `SequenceExhausted`를 반환한다.
6. string length와 lexicographic 비교로 counter 범위를 검사한 뒤 `INCR`한다.
7. 증가된 값을 다시 `GET`해 exact decimal string으로 받고 lease hash와 TTL을 설정한다.
8. `Acquired(FencingToken(config.epoch, newSequence))`을 반환한다.

lease 생성과 counter 증가는 한 script 안에서 처리한다. script가 정상 status를 반환하기
전후로 connection이 끊기면 caller는 적용 여부를 확정할 수 없다.

### 11.3 Inspect

- lease가 없으면 `Lost`다.
- owner가 같으면 stored token과 TTL을 검증해 `Owned`를 반환한다.
- owner가 다르면 token이나 owner를 노출하지 않고 `Contended`를 반환한다.
- inspect는 TTL을 연장하거나 state를 수정하지 않는다.

### 11.4 Renew

- lease가 없으면 `Lost`다.
- stored owner 또는 `(epoch, sequence)`가 argument와 다르면
  `OwnershipMismatch`다.
- 둘 다 같을 때만 TTL을 갱신하고 `Renewed`를 반환한다.
- renew는 counter를 변경하거나 새 token을 만들지 않는다.

### 11.5 Release

- lease가 없으면 `Lost`다.
- stored owner 또는 token이 다르면 `OwnershipMismatch`다.
- 둘 다 같을 때만 lease key를 삭제하고 `Released`를 반환한다.
- counter는 삭제하거나 감소시키지 않는다.

## 12. Failure, retry, cancellation

### 12.1 Ambiguous completion

command dispatch 뒤 timeout, connection loss, future cancellation, coroutine cancellation은
Redis mutation이 적용됐는지 확정하지 못하는 ambiguous completion이다.

- acquire: 같은 owner ID로 다시 acquire한다. active ownership이면
  `AlreadyOwned`와 같은 token을 받는다.
- bootstrap: 최초 배포 또는 아직 사용되지 않은 higher epoch라는 전제가 유지될 때 같은
  config로 다시 호출한다. 첫 command가 적용됐다면 `AlreadyInitialized`, 아니면
  `Initialized`다. history loss가 의심되는 old epoch에는 이 규칙을 적용하지 않는다.
- renew: `inspect(ownerId)`로 현재 ownership과 token을 확인한다. 같은 token이면 새 TTL로
  renew를 다시 시도할 수 있다.
- release: `Lost`는 이전 release 성공과 자연 만료를 구분하지 않는다. 두 경우 모두
  caller가 더는 ownership을 갖지 않는다는 사실만 사용한다.
- backend failure result를 받았다고 local 성공/실패를 단정하지 않는다.

implementation은 public API를 늘리지 않는 internal script-executor seam으로 mutating
command의 다음 두 지점을 결정적으로 fault-inject한다.

- server apply 전 failure/cancellation;
- apply 완료 뒤 reply decode 전 failure/cancellation.

bootstrap, acquire, renew, release 각각에 대해 network timing이나 sleep에 기대지 않고 위
두 지점과 operation-specific reconciliation 결과를 검증한다.

### 12.2 `CompletableFuture` cancellation

- returned future의 `cancel(...)`은 future를 cancelled 상태로 끝내며
  `FencingLeaseBackendFailure`로 변환하지 않는다.
- cancellation은 underlying Lettuce future에 best-effort로 전파하지만, 이미 dispatch된
  Redis command의 중단이나 rollback을 보장하지 않는다.
- 기존 `RedisScriptRunner.runAsync()`의 chained future를 그대로 반환하지 않는다. internal
  wrapper가 현재 upstream Lettuce future를 보존하고 returned future의 cancellation을
  EVALSHA에 전달하며, `NOSCRIPT` 뒤 EVAL로 교체된 경우에는 새 upstream future로 전달
  대상을 원자적으로 바꾼다. fallback 전/후 cancellation race를 contract test로 고정한다.
- caller는 acquire에는 같은 owner ID, renew/release에는 inspect와 같은 token을 사용해
  server state를 reconcile한다.
- controllable internal executor로 server apply 전 cancel과 apply 후 reply 전 cancel을
  각각 검증한다.

### 12.3 외부 resilience decorator

primitive 내부에는 retry를 넣지 않는다. caller는 같은 owner ID와 token을 캡처한
operation을 `bluetape4k-resilience4j`의 `Retry`, `CircuitBreaker`, `Bulkhead` decorator로
감쌀 수 있다. 문서 예제와 실제 Redis integration test에서 조합 가능성을 보여주되,
다음 원칙을 지킨다.

- validation failure와 `IntegrityFailure`, `CounterUnavailable`,
  `SequenceExhausted`, `OwnershipMismatch`는 retry하지 않는다.
- operation이 exception 대신 classified result를 반환하므로 `Retry`와
  `CircuitBreaker`는 `BackendFailure`만 실패로 보는 result predicate를 명시적으로
  구성한다.
- 권장 `SuspendDecorators` chain은
  `.withRetry(retry).withCircuitBreaker(circuitBreaker).withBulkhead(bulkhead)`다. 이 builder는
  호출 순서대로 기존 supplier를 감싸므로 마지막 `Bulkhead`가 최외곽, 그 안이
  `CircuitBreaker`, 가장 안쪽이 `Retry`와 supplier다. 한 bulkhead permit이 전체 retry
  attempt를 감싸고 circuit breaker는 retry가 끝난 최종 result를 관찰한다.
- `Retry`와 `CircuitBreaker`의 result predicate는 `BackendFailure`만 failure로 센다.
  `CallNotPermittedException`과 `BulkheadFullException`은 primitive의 sealed result로
  변환하지 않는 caller-layer exception이다. `CancellationException`도 decorator를
  통과해 그대로 전파한다.
- acquire retry는 반드시 같은 owner ID를 사용한다.
- cancellation은 retry 대상이 아니다.
- decorator가 ambiguous completion을 exactly-once로 바꾸지는 않는다.

### 12.4 Exception/result boundary

- dispatch 전 argument/config validation은 existing Bluetape validation helper로
  `IllegalArgumentException`을 던진다.
- `Duration.toMillis()` overflow의 `ArithmeticException`도 dispatch 전에
  `IllegalArgumentException`으로 정규화한다.
- Redis connection, timeout, command failure는 classified result로 반환한다.
- async method의 dispatch 전 validation failure는 method 호출 시 동기적으로 throw한다.
- suspend method는 `CancellationException`을 result로 바꾸지 않고 그대로 전파한다.
- public result를 만들 수 없는 decoder invariant는 stable internal exception으로 fail
  fast한다.
- sync/future/suspend는 10.1의 같은 cause normalizer와 classifier를 사용하므로 wrapper
  형태가 달라도 같은 backend failure kind를 반환한다.

## 13. 주요 failure mode

| Failure mode | 탐지 | 결과/복구 |
|---|---|---|
| 최초 bootstrap 누락 | lease 없음 + counter 없음 | acquire `CounterUnavailable`; 승인된 새 epoch만 bootstrap |
| active lease 중 counter 삭제 | lease 존재 + counter 없음 | integrity failure; old epoch 자동 복구 금지 |
| counter가 lease sequence보다 작음 | 두 decimal string 비교 | `COUNTER_BEHIND_LEASE`; acquire/renew/release mutation 금지 |
| counter `Long.MAX_VALUE` | exact string 비교 | `SequenceExhausted`; epoch 여유가 있을 때만 더 높은 epoch로 rollover |
| epoch와 sequence 모두 `Long.MAX_VALUE` | config/counter exact 비교 | ordering domain terminal; external schema/domain migration |
| lease hash 손상/oversized field | O(1) type/HLEN/HSTRLEN/fixed HMGET 검증 | `MALFORMED_LEASE`; fail closed |
| counter에 TTL 존재 | `PTTL >= 0` | `INVALID_COUNTER`; mutation 금지 |
| lease에 TTL 없음 | `PTTL == -1` | `MALFORMED_LEASE`; incident 확인 뒤 lease만 제거 가능 |
| `INCR` 뒤 script runtime failure | counter 증가, lease 없거나 partial | gap 허용; TTL 없는 partial lease는 다음 operation에서 fail closed |
| `SCRIPT FLUSH` | EVALSHA가 `NOSCRIPT` 반환 | `RedisScriptRunner`가 EVAL fallback 후 같은 contract 유지 |
| command 적용 뒤 response loss | client/backend failure | same-owner acquire 또는 inspect 기반 reconciliation |
| coroutine cancellation | `CancellationException` | 즉시 전파; same-owner reconciliation 가능 |
| future cancellation | cancelled future | backend result로 변환하지 않음; dispatch 중단은 보장하지 않음 |
| replica promotion/write loss | Redis만으로 완전 탐지 불가 | 중지, durable epoch 증가, downstream tuple 비교 |
| snapshot rollback | 구조가 정상처럼 보일 수 있음 | downstream이 이전 accepted token을 거절; 더 높은 epoch로 복구 |
| stale holder renew/release | owner+token mismatch | `OwnershipMismatch`; newer lease 불변 |

### 13.1 Observability와 repair runbook

library structured log는 raw key, namespace, resource, owner, token, reply, exception message를
기록하지 않는다. correlation이 필요할 때만 canonical
`namespace + "\u0000" + resourceName + "\u0000" + epoch`의 SHA-256 앞 12 byte를 24자리
lowercase hex `domainFingerprint`로 계산한다. 이 fingerprint는 bounded stable log correlation
field일 뿐 metric label로 사용하지 않는다.

새 Micrometer dependency를 추가하지 않는다. caller가 classified result를 관찰해 operation,
result variant, backend/integrity failure kind처럼 bounded low-cardinality dimension만 기존
metrics에 기록한다. namespace, resource, owner, token, fingerprint는 metric label에서
금지한다. alert와 runbook mapping은 다음과 같다.

| signal | alert/action |
|---|---|
| `CounterUnavailable`, `INVALID_COUNTER`, `COUNTER_BEHIND_LEASE` | domain traffic pause, read-only diagnosis, history-loss 여부 판정; 자동 bootstrap 금지 |
| `MALFORMED_LEASE` | mutation pause, fixed key inspection, lease-only repair 조건 검토 |
| `SequenceExhausted` | non-retry alert, higher-epoch cutover; max epoch면 domain freeze와 schema/domain migration |
| backend failure rate 또는 circuit open | operation별 reconcile, dependency incident runbook; lower epoch 전환 금지 |
| external promotion/restore/rollback signal | 즉시 pause, durable epoch CAS bump, 8.2/8.3 절차 후 resume |

operator diagnosis는 파생된 정확한 두 key만 받는 bounded read-only diagnostic Lua를
`EVAL_RO`로 실행한다. script 내부는 `TYPE`, `PTTL`, `STRLEN`, `HLEN`, fixed-field
`HSTRLEN`, `GET`, fixed-field `HMGET`만 O(1)로 사용해 canonical decimal, counter TTL,
lease structure, `counter >= lease.sequence`를 판정하고, raw key/owner/token/value 대신 stable
classification code와 boolean만 반환한다. recorded classification output이 repair evidence다.
`HGETALL`, `KEYS`, `SCAN`, raw owner/token 출력은 사용하지 않는다.
TTL 없는 partial lease는 다음 조건을 모두 만족할 때만 lease key 하나를 수동 삭제할 수 있다.

1. ordering domain의 acquire와 downstream writer가 중지됐다.
2. downstream tuple guard와 마지막 accepted token이 확인됐다.
3. 보안 경계를 지킨 incident evidence와 정확한 진단 `lease PTTL == -1`이 기록됐다.
4. diagnostic script의 기록된 결과가 counter를 valid canonical non-expiring string,
   `counter >= lease.sequence`, lease `PTTL == -1`로 판정한다.

counter는 어떤 repair에서도 삭제, 감소, TTL 설정, 같은 epoch bootstrap 대상이 아니다.

## 14. Test 전략

### 14.1 Unit test

- `FencingToken`의 epoch 우선/sequence 차순/동일 token ordering과 equality.
- config, owner ID, TTL, token 경계 validation.
- `Base58.randomString(22)` 기반 owner ID factory 형식과 최소 128-bit 선택 공간, custom owner ID 재사용 시 같은-owner
  capability가 공유됨을 보여주는 misuse fixture.
- connection codec의 `encodeKey` 결과를 사용한 key derivation과 `SlotHash.getSlot` same-slot
  invariant. non-default codec fixture로 String 기반 추정이 아님을 고정한다.
- decimal string validator: leading zero, sign, whitespace, non-digit, 범위 초과,
  `Long.MAX_VALUE`.
- wire decoder의 unknown status, field count, malformed TTL/token과 public/internal 분류표.
- wrapped/unwrapped Lettuce connection, timeout, command exception이 sync/future/suspend에서 같은
  backend kind가 되고 cancellation/decoder/non-Lettuce exception은 분류되지 않는 test.
- sub-millisecond와 `Duration.toMillis()` overflow가 세 API 모두 dispatch 전에
  `IllegalArgumentException`이 되는 parity test.
- `2^53 - 1` milliseconds TTL은 실제 Redis acquire/replay/inspect에서 canonical decimal
  reply를 유지하고, 그보다 큰 TTL은 세 API 모두 dispatch 전에 거절하는 test.
- 다른 epoch token의 renew/release가 dispatch 전에 거절되고, 같은 tuple의 다른 resource
  token은 primitive가 식별할 수 없어 resource-bound caller handle이 필요한 misuse test.
- 모든 public config/value/failure/result variant의 Java serialization round-trip과
  non-enum serializable type의 `serialVersionUID` 존재 검증.
- 모든 serialized `data object` round-trip이 equality뿐 아니라 canonical singleton과
  reference identity를 유지함.
- constructor를 우회한 crafted serialized payload가 invalid config/token/TTL/oversized
  owner를 만들지 못하고 `InvalidObjectException`으로 거절되는 test.
- sentinel key/owner/token과 raw command가 backend/integrity log에 포함되지 않는
  redaction capture test.
- test는 JUnit 5, `bluetape4k-assertions`,
  `io.bluetape4k.assertions.assertFailsWith`, intent-specific matcher를 사용한다.

### 14.2 Shared behavior contract

`FencingLeaseContract`를 만들고 sync, future, suspend adapter가 같은 fixture를 실행한다.

- sequential acquire/release의 strictly increasing token.
- concurrent acquire에서 한 owner와 한 fencing generation만 생성.
- 같은 owner acquire retry가 같은 token을 반환.
- renew가 token을 유지.
- wrong owner와 stale token renew/release가 newer lease를 바꾸지 않음.
- caller action 표의 terminal/non-retry/reconcile 분기가 sealed result별로 빠짐없이 대응됨.
- expiry 뒤 takeover가 더 큰 token을 발급.
- backend failure category parity.
- `MultithreadingTester`, `SuspendedJobTester`를 적합한 concurrency case에 사용.
- standalone은 16 caller × 25 generation, Cluster는 8 caller × 10 generation의 bounded
  contention fixture를 30초 test timeout 안에서 실행한다. duplicate generation,
  ordering regression, unexpected failure, hang이 없어야 한다.

### 14.3 실제 standalone Redis

기존 singleton launcher를 사용하고 module 간/작업 간 Testcontainers 실행은
sequential하게 유지한다.

- lease key만 유실되고 counter가 남으면 다음 acquire가 더 큰 token을 발급.
- counter 삭제 시 acquire가 fail closed.
- active lease 상태에서 counter를 0으로 reset하면 integrity failure.
- wrong Redis type, oversized counter/owner/field, unexpected extra hash field, counter TTL,
  TTL 없는 partial lease가 O(1) preflight에서 fail closed.
- lease와 counter rollback을 Redis만으로 탐지할 수 없는 case와 downstream
  `incoming > lastAccepted` fixture의 stale rejection.
- resource identity와 last accepted tuple을 저장하는 PostgreSQL-style in-memory CAS fixture가
  `affectedRows == 1`만 수락하고 stale/same-token replay를 거절함.
- `Long.MAX_VALUE`에서 `SequenceExhausted`.
- `SCRIPT FLUSH` 뒤 EVALSHA/NOSCRIPT fallback.
- same-owner ambiguous acquire reconciliation.
- internal executor fault injection으로 bootstrap/acquire/renew/release의 apply 전과 apply 후
  reply 유실을 결정적으로 재현하고 각 recovery 결과를 검증.
- control-plane harness가 `pause -> old acquire 차단 -> active lease/downstream writer drain ->
  durable CAS epoch bump -> bootstrap -> readiness/tuple guard 확인 -> rollout -> old writer 부재
  확인 -> resume` 순서를 결정적으로 검증하고, mixed-epoch 탐지 시 abort 및 lower-epoch
  rollback을 거절함.
- tagged opt-in topology promotion/restore test는 external incident signal 뒤 traffic pause와
  higher-epoch recovery를 검증한다. 기본 CI는 동일 상태 전이를 fault-injection simulation으로
  검증한다.
- `Retry`, `CircuitBreaker`, `Bulkhead`를 외부 decorator로 적용한 실제 Redis 예제 test.
- decorator 순서가 Bulkhead 최외곽, CircuitBreaker, Retry, supplier이고 한 permit이 전체 retry를
  감싸며 circuit breaker가 최종 retry result만 관찰함을 검증한다. backend result만
  retry/CB failure로 세고 circuit-open/bulkhead-full exception과 cancellation이 library
  result로 변환되지 않음을 검증한다.

### 14.4 Redis Cluster

- derived lease/counter key가 같은 slot에 배치됨.
- test codec이 두 derived key를 다른 wire slot으로 encode하는 fixture는 constructor에서
  stable `IllegalArgumentException`으로 실패하고 어떤 node에도 dispatch하지 않음.
- standalone과 Cluster에서 sync/future/suspend result parity.
- invalid config/argument가 node dispatch 전에 실패함.
- script가 검증된 key 쌍으로 정상 routing됨.
- structural test/review로 hot path가 fixed command/reply와 steady-state one-script dispatch를
  유지함을 확인. 새 benchmark dependency나 CI latency SLA는 추가하지 않음.

### 14.5 Cancellation

- 실제 coroutine job을 cancel해 `CancellationException`이 다시 던져짐.
- cancellation을 backend failure result로 바꾸거나 retry하지 않음.
- local ownership cache가 없으므로 cancellation 뒤 client-side state가 오염되지 않음.
- 같은 owner acquire와 inspect를 이용해 server state를 복구 가능함.
- controllable executor에서 future를 server apply 전과 apply 후 reply 전 각각 cancel하고,
  cancelled 상태 유지와 operation-specific reconciliation을 검증.
- EVALSHA 단계와 `NOSCRIPT` EVAL fallback 전환 race 각각에서 returned future cancellation이
  현재 upstream future에 best-effort로 전달됨.
- 실제 async/suspend IO는 `runSuspendIO`를 사용한다.

## 15. Documentation과 KDoc

모든 public class, function, result, property에 English KDoc을 작성한다.

- ownership과 orderable fencing의 차이.
- token ordering domain과 `(epoch, sequence)` 비교.
- token이 domain identity를 포함하지 않아 resource-bound storage/handle이 필요한 점과
  renew/release의 cross-epoch pre-dispatch rejection.
- owner ID 재사용 범위와 ambiguous acquire retry.
- `FencingOwnerId.random()` 기본값, custom ID의 capability/collision 책임, redacted
  `toString()`과 serialization 재검증.
- expiry, renew, release, overflow, Cluster same-slot.
- 서로 다른 epoch의 동시 activation 금지와 control-plane cutover.
- 한 hot resource가 한 slot/event loop에서 직렬화되는 성능 경계.
- Redis history loss와 higher-epoch recovery.
- timeout/cancellation 뒤 ambiguous completion.
- downstream conditional write 책임.
- 내부 retry가 없고 외부 decorator를 사용할 수 있다는 점.
- 권장 Retry/CB/Bulkhead 순서, result predicate와 caller-layer exception 경계.
- public API의 canonical package `io.bluetape4k.redis.lettuce.lease`.

`infra/lettuce/README.md`와 `README.ko.md`는 같은 기술 내용을 각각 자연스럽게 제공한다.

- opaque multi-key lease가 충분한 경우와 fencing lease가 필요한 경우 비교.
- bootstrap과 epoch 운영 절차.
- 최대 epoch terminal 상태와 ordering-domain migration 경계.
- PostgreSQL-style tuple conditional update 예제.
- stale writer rejection 예제.
- `Retry`, `CircuitBreaker`, `Bulkhead` 외부 조합 예제.
- Redis가 exactly-once나 durable business correctness를 제공하지 않는다는 제한.

PostgreSQL 예제는 adapter를 만들지 않고 다음 의미만 보여준다.

`fence_epoch`와 `fence_sequence`는 `NOT NULL DEFAULT 0`으로 추가해 기존 row를 `(0, 0)`에서
시작하고, resource/domain identity와 함께 저장한다.

```sql
UPDATE guarded_resource
SET fence_epoch = :epoch,
    fence_sequence = :sequence,
    payload = :payload
WHERE id = :id
  AND (fence_epoch, fence_sequence) < (:epoch, :sequence);
```

caller는 `affectedRows == 1`일 때만 fencing guard가 write를 수락했다고 본다.
`affectedRows == 0`은 stale token 또는 같은-token replay이며 성공으로 간주하지 않는다.
business command의 idempotency key와 duplicate 처리 정책은 fencing tuple과 별도다.

## 16. Compatibility와 migration

- 기존 `LettuceLock`, `LettuceSuspendLock`, `LettuceSemaphore`,
  `LettuceMultiKeyLease` public API와 behavior를 변경하지 않는다.
- 기존 caller가 fencing lease로 자동 이동하지 않는다. orderable downstream guard가 필요한
  consumer만 새 primitive를 명시적으로 선택한다.
- opaque owner token을 `FencingToken`으로 cast/parse하는 migration은 제공하지 않는다.
- 안전한 migration은 먼저 downstream table에 resource-bound `NOT NULL` tuple column과
  strict `incoming > stored` guard를 배포한 뒤, old opaque lease writer의 신규 acquire를
  중지하고 active holder와 in-flight write를 drain한다. 그 다음 durable authority에서 새
  higher epoch를 할당·bootstrap하고 fencing writer를 활성화한다. old binary/opaque writer가
  다시 들어오지 못하도록 deployment/version gate를 유지하며, 재진입 가능성이 있으면
  migration을 완료로 보지 않는다.
- 새 API는 `bluetape4k-lettuce` 안에 추가되며 module registration, dependency catalog,
  publication topology를 바꾸지 않는다. public type은
  `io.bluetape4k.redis.lettuce.lease`에 둔다.
- `bluetape4k-leader` integration은 별도 issue에서 public leader contract와 함께 검토한다.

## 17. Acceptance criteria

- config가 namespace, resource, epoch를 instance 생성 시 고정하고 두 Redis key를 같은
  Cluster slot에 생성한다. slot은 connection codec이 encode한 실제 key byte로 검증한다.
- owner ID는 CSPRNG 기본 factory, custom capability 경계, redacted logging contract를
  제공한다.
- `FencingToken`이 `(epoch, sequence)` natural ordering과 serialization contract를
  제공한다.
- explicit bootstrap만 새 counter를 만들고 acquire는 missing counter에서 fail closed한다.
- acquire와 counter increment, lease 저장이 한 Lua execution에서 원자적이다.
- same-owner acquire retry가 active ownership의 같은 token을 돌려준다.
- renew는 token을 유지하고 renew/release는 owner와 token을 모두 비교한다.
- overflow, malformed state, counter loss/behind, backend failure를 명시적인 outcome으로
  구분한다.
- sync/future/suspend shared contract와 standalone/Cluster fixture가 같은 semantics를
  증명한다.
- 공통 cause classifier가 wrapper 차이와 무관하게 backend kind parity를 보장하고 validation,
  cancellation, decoder/internal exception을 public backend result로 평탄화하지 않는다.
- cancellation이 보존되고 ambiguous completion recovery가 test와 KDoc에 드러난다.
- upstream future cancellation이 EVALSHA와 NOSCRIPT fallback 양쪽에 best-effort로 전파된다.
- script preflight와 reply가 O(1)/fixed-size이고 runtime error의 non-rollback 및 safe gap,
  TTL 없는 partial lease 처리가 문서와 hostile-state test에 고정된다.
- 모든 public non-enum value/result 및 nested variant가 `Serializable`과
  `serialVersionUID = 1L`을 가지며 invalid deserialization을 canonical constructor
  재검증으로 거절한다. enum은 Java enum serialization round-trip을 검증한다.
- English KDoc과 영문/한글 README가 ordering, recovery, downstream responsibility를
  동등하게 설명한다.
- durable CAS epoch authority, pause/drain/bump/bootstrap/verify/resume cutover, lower-epoch
  rollback 금지, bounded observability와 lease-only repair 조건이 runbook에 고정된다.
- PostgreSQL 예제가 resource-bound `NOT NULL DEFAULT 0` tuple과 `affectedRows == 1` strict
  acceptance를 사용하고 business idempotency를 별도 책임으로 둔다.
- 기존 Lettuce lock/semaphore/multi-key lease test가 그대로 통과한다.

## 18. Definition of Done

- 설계와 구현 plan의 독립 관점 review에서 P0=0, P1=0이다.
- production code보다 먼저 failing test가 public contract와 hostile state를 고정한다.
- targeted unit/contract/standalone/Cluster/cancellation/resilience tests가 통과한다.
- `:bluetape4k-lettuce:test`, 관련 static analysis, `git diff --check`가 통과한다.
- public KDoc, `README.md`, `README.ko.md`가 구현과 일치한다.
- issue #1068 acceptance criteria가 PR DoD에 추적된다.
- PR은 `feature/issue-1068-fencing-lease`에서 `develop`으로 생성한다.
- CI와 최신 review/thread를 확인한 뒤 별도의 merge 승인을 받는다.
- merge 뒤 local `develop`을 fast-forward하고 merged worktree/branch를 정리한다.
