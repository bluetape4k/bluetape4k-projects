# Issue #1351 NearJCache Management/Statistics 설계

## 1. 문서 상태

- 대상 이슈: [#1351 NearJCache JCache Management/Statistics MXBean을 실제 설정·운영 지표에 연결](https://github.com/bluetape4k/bluetape4k-projects/issues/1351)
- 상위 Epic: [#1408 NearJCache 표준 계약·보안·안정성 후속 작업](https://github.com/bluetape4k/bluetape4k-projects/issues/1408)
- 작업 유형: Type A Full Feature
- 작업 브랜치: `feat/1351-nearcache-management`
- 선행 브랜치: `fix/1348-lettuce-entryprocessor-atomicity`
- 기준 커밋: `513f70e785ea6975fc150844b6b8f23b9238031c`
- 설계 버전: v2.2
- 설계 승인: 2026-08-16 사용자 승인
- 구현 상태: 미시작
- 문서 상태: 작성본 검토 대기

이 문서는 `NearJCache`가 실제 front/back 동작을 관찰하면서도 JCache 표준 통계,
Near Cache tier 통계, provider capability를 서로 혼동하지 않도록 공개 계약과 lifecycle을
고정한다. 구현은 이 문서와 후속 구현 계획이 모두 승인되기 전에는 시작하지 않는다.

## 2. 문제와 목표

현재 `NearJCacheManagementMXBean`은 key/value type과 JCache flag를 고정된 기본값으로
반환한다. `NearJCacheStatisticsMXBean`은 독립 counter를 제공하지만 `NearJCache`의
연산과 연결되지 않는다. `EmptyNearJCacheStatisticsMXBean`도 부모 counter를 생성하며
`addRemovals`를 무력화하지 않아 disabled 계약을 만족하지 못한다.

목표는 다음과 같다.

1. front cache의 실제 JCache 설정을 immutable snapshot으로 노출한다.
2. caller가 관찰한 logical 결과와 front/back tier 결과를 분리해 계측한다.
3. statistics disabled 경로의 비용 상한을 명시하고 구현으로 고정한다.
4. credential이나 provider lifecycle을 노출하지 않는 explicit JMX 등록·해제를 제공한다.
5. 기존 public constructor와 직렬화/API shape를 보존한다.

## 3. 현재 근거와 제약

### 3.1 저장소 근거

- `NearJCache`는 `JCache<K, V> by backCache`로 선언되어 직접 override하지 않은
  `loadAll`, `invoke`, `invokeAll`, `getConfiguration`을 back cache에 위임한다.
- `NearJCache.clear()`는 front와 back의 데이터를 삭제한다. 통계 초기화 API가 아니다.
- `NearJCache.close()`는 listener와 front cache lifecycle을 정리하고 back cache와
  provider를 소유하지 않는다.
- `NearJCache`는 store-by-value front cache를 거부하므로 정상 인스턴스의
  `storeByValue`는 항상 `false`다.
- `NearJCacheConfig`의 primary constructor, legacy constructor, `copy`, custom
  serialization은 호환성 계약이다. observability를 위해 새 primary property를 추가하지 않는다.
- 현재 management package 전체가 Kover 대상에서 제외되어 있어 새 동작을 coverage로
  검증하려면 exclusion을 제거하거나 좁혀야 한다.

### 3.2 JCache 의미 제약

- `CacheStatisticsMXBean.clear()`는 통계 counter를 초기화한다. cache 데이터를 삭제하지 않는다.
- `CacheMXBean.isReadThrough`와 `isWriteThrough`는 front cache의
  `CacheLoader`/`CacheWriter` 설정을 뜻한다. Near Cache의 front-to-back 흐름을 뜻하지 않는다.
- 기본 `MutableConfiguration`은 key/value type을 `Object,Object`로 반환하며 명시 설정 여부를
  별도로 제공하지 않는다.
- 일반 JCache API는 `removeAll(Set)`과 `removeAll()`이 실제로 삭제한 entry 수를 반환하지 않는다.
- 일반 JCache provider에서는 capacity eviction을 정확히 관찰할 수 없다. back cache의
  `EXPIRED` event는 front capacity eviction이 아니다.

### 3.3 범위와 제외

이번 변경은 blocking `NearJCache`와 `cache-core`의 JCache management/statistics에 한정한다.

다음 항목은 제외한다.

- `SuspendNearJCache` 계측
- `loadAll`, `invoke`, `invokeAll` 계측
- provider별 capacity eviction adapter
- 새 `NearJCacheManager` abstraction
- runtime statistics/management enable/disable 전환
- provider가 소유한 `CacheManager`, `MBeanServer`, back cache lifecycle 변경

제외된 JCache 연산은 back cache로 직접 위임되는 현재 동작을 유지한다. 문서와 MXBean KDoc은
통계를 “모든 `Cache` 연산의 완전한 통계”가 아니라 “명시된 wrapper 연산의 logical 통계”로
설명한다.

## 4. 대안과 결정

### 4.1 선택: wrapper-owned snapshot/recorder와 explicit registrar

`NearJCache`가 construction-time snapshot과 recorder를 만들고, 별도 registrar가 필요한
MBean만 caller가 지정한 `MBeanServer`에 등록한다. 표준 logical 통계와 custom tier 통계를
하나의 recorder generation에서 읽는다.

이 방식은 provider 차이를 wrapper 경계에서 흡수하고, 등록 시점과 ObjectName을 caller가
통제하며, 기존 constructor shape를 유지할 수 있다.

### 4.2 거절: provider MXBean 재사용

provider MXBean은 front/back 중 어느 계층의 수치인지와 지원 범위가 다르다. provider
`CacheManager` URI를 ObjectName에 사용하면 Redis URI의 credential이 노출될 수도 있다.
따라서 provider MXBean을 NearJCache의 운영 계약으로 재노출하지 않는다.

### 4.3 보류: 새 NearJCacheManager와 자동 JMX 등록

새 manager는 cache 생성·조회·등록·종료 책임을 한 번에 바꾸므로 #1351보다 범위가 크다.
construction 시 자동 등록도 embedded/test 환경에 전역 side effect를 만든다. 이번 버전은
explicit registrar와 registration handle만 추가한다.

## 5. 구성 snapshot 계약

### 5.1 생성과 소유권

`NearJCacheConfigurationSnapshot`은 `NearJCache` construction 중 한 번 생성하는 immutable
internal value다. management bean은 이 snapshot만 보유하며 `NearJCache`, front cache,
back cache를 참조하지 않는다.

snapshot 생성은 공개 API 실행, reflection, provider-specific `unwrap`, loader/writer factory
실행을 하지 않는다.

### 5.2 type 결정

key/value type은 pair 단위로 다음 순서에서 결정한다.

1. 실제 front cache `Configuration`
2. caller가 제공한 front configuration
3. 실제 back cache `Configuration`
4. `java.lang.Object`, `java.lang.Object`

한 후보가 `Object,Object`이면 “타입 미확정”으로 보고 다음 후보로 이동한다. JCache API는
caller가 `Object,Object`를 의도적으로 지정했는지 구분하지 못하므로 이 경우에도 같은 규칙을
적용한다. key와 value를 서로 다른 후보에서 조합하지 않는다.

새 `NearJCacheConfigurationMXBean : CacheMXBean`은 `typeResolutionSource`와
`typeResolutionExact`를 추가한다. source는 `ACTUAL_FRONT`, `SUPPLIED_FRONT`, `ACTUAL_BACK`,
`UNRESOLVED_OBJECT` 중 하나다. actual front의 concrete pair만 exact이며 나머지는 inferred다.
따라서 back type fallback을 실제 front type으로 오해하지 않는다.

### 5.3 flag 의미

- `readThrough`, `writeThrough`: 실제 front cache가 `CompleteConfiguration`을 제공할 때의
  `CacheLoader`/`CacheWriter` flag다. 제공하지 않으면 `false`다.
- `storeByValue`: `NearJCache` construction invariant에 따라 항상 `false`다.
- `statisticsEnabled`, `managementEnabled`: construction-time front configuration snapshot이다.

`CacheManager.enableStatistics` 또는 `enableManagement`의 이후 호출은 v1 snapshot과 recorder를
바꾸지 않는다. runtime toggle은 지원하지 않으며 새 `NearJCache`를 생성해야 한다.

기존 instance의 flag를 전환하려면 기존 registration handle을 닫고 `NearJCache.close()`를 완료한
뒤, 변경한 front configuration으로 새 `NearJCache`를 만들고 새 handle을 등록한다. 기존과 새
instance가 같은 MBeanServer/ObjectName을 동시에 소유하는 rolling overlap은 허용하지 않는다.

## 6. 통계 모델

### 6.1 logical 통계와 tier 통계

기존 `NearJCacheStatisticsMXBean`은 `CacheStatisticsMXBean` 호환 surface를 유지한다.
새 `NearJCacheTierStatisticsMXBean : CacheStatisticsMXBean`은 다음 custom attribute를 추가한다.

- `frontHits`, `frontMisses`
- `backHits`, `backMisses`
- `frontEvictions`
- `frontEvictionObservationSupported`
- `bulkRemovalCountSupported`
- `statisticsScope`
- `supportedOperations`
- `backWriteCompletionIncluded`

`statisticsScope`의 안정적인 값은 `NEAR_JCACHE_WRAPPER_V1`이다. `supportedOperations`는
`get`, `getAll`, `put`, `putAll`, `putIfAbsent`, `replace`, `remove`, `getAndPut`,
`getAndReplace`, `getAndRemove`를 안정적인 순서의 문자열 배열로 반환한다.
`backWriteCompletionIncluded=false`는 표준 mutation 수치가 remote completion을 포함하지
않는다는 capability 신호다.

표준 수치는 caller가 wrapper에서 관찰한 logical 결과를 나타낸다. tier 수치는 같은 logical
operation이 각 계층에서 거친 결과를 나타낸다. 예를 들어 front miss 뒤 back hit이면
`cacheHits += 1`, `frontMisses += 1`, `backHits += 1`이다.

`NearJCacheTierStatisticsMXBean`을 `StandardMBean`의 MXBean interface로 사용해 JMX에 노출한다.
호환성을 위해 남기는 public `add*` method는 interface에 포함하지 않으며 JMX operation으로
노출하지 않는다.

기존 `NearJCacheStatisticsMXBean` class 하나가 `NearJCacheTierStatisticsMXBean`을 구현한다.
registrar는 이 instance를 `type=NearJCacheStatistics` ObjectName 하나로 등록한다. 별도의
standard-only statistics MBean/ObjectName을 만들지 않는다. inherited `CacheStatisticsMXBean`
attribute와 custom tier/capability attribute는 같은 `MBeanInfo`에 나타나며 `clear()`는 같은
recorder generation의 logical/tier counter를 함께 초기화한다.

configuration MBean도 `NearJCacheConfigurationMXBean`을 `StandardMBean` interface로 사용한다.
기존 `NearJCacheManagementMXBean` class는 이 interface를 구현해 standard `CacheMXBean` 속성과
type resolution metadata를 함께 제공한다.

### 6.2 operation matrix

| 연산 | logical count | tier count | 시간 경계 |
|---|---|---|---|
| `get(key)` | 반환값 기준 hit/miss 1회 | front 결과, 필요할 때 back 결과 | caller 반환까지 |
| `getAll(keys)` | key별 hit/miss | key별 front/back 결과 | 전체 caller 반환까지, key별 count에 동일 총시간을 중복 배분하지 않음 |
| `put` | 정상 반환 시 put 1회 | 없음 | caller 반환까지 |
| `putAll(entries)` | 전체 호출이 정상 반환하면 entry 수만큼 put | 없음 | caller 반환까지 |
| `putIfAbsent` | 실제 삽입 성공 시 put 1회, get은 기록하지 않음 | 없음 | caller 반환까지 |
| `replace*` | 교체 성공 시 put 1회, get은 기록하지 않음 | 없음 | caller 반환까지 |
| `remove`, conditional `remove` | 삭제 성공을 `true`로 확인할 때 removal 1회, get은 기록하지 않음 | 없음 | caller 반환까지 |
| `getAndPut` | 이전 값 기준 hit/miss 1회와 put 1회 | 구현이 관찰한 front/back read 결과 | caller 반환까지를 get/put 양쪽 total time에 1회씩 기록 |
| `getAndReplace` | 이전 값 기준 hit/miss 1회, 이전 값이 있을 때 put 1회 | 구현이 관찰한 front/back read 결과 | caller 반환까지를 get과 성공한 put total time에 기록 |
| `getAndRemove` | 이전 값 기준 hit/miss 1회, 이전 값이 있을 때 removal 1회 | 구현이 관찰한 front/back read 결과 | caller 반환까지를 get과 성공한 removal total time에 기록 |
| `removeAll(Set)`, `removeAll()` | 실제 삭제 수를 확인할 수 없어 removal을 추정하지 않음 | 없음 | 표준 average remove time에 포함하지 않음 |
| `clear()` | 통계 변화 없음 | 통계 변화 없음 | 계측하지 않음 |
| `containsKey` | JCache 표준에 따라 get으로 계산하지 않음 | 없음 | 계측하지 않음 |
| `loadAll`, `invoke`, `invokeAll` | v1 미지원 | v1 미지원 | 계측하지 않음 |

예외로 종료된 연산은 성공 count와 평균 시간을 증가시키지 않는다. recorder의
`totalGetTimeNanos`, `totalPutTimeNanos`, `totalRemoveTimeNanos`는 raw elapsed를 호출당 한 번만
누적한다. bulk 기록 시 elapsed를 entry 수로 나누지 않는다. getter만
`totalTimeNanos / logicalCount / 1_000.0`으로 계산해 JCache 계약 단위인 microsecond `Float`로
반환한다. count가 0이면 `0F`를 반환한다.

`getAll`은 key별 logical count를 기록하고 bulk elapsed를 total get time에 한 번만 더한다.
`putAll`도 전체 성공 시 entry 수를 logical put count에 더하고 bulk elapsed를 total put time에
한 번만 더한다. empty bulk operation은 count와 time을 모두 기록하지 않는다. compound 연산은
한 호출이 표준 get과 mutation을 함께 나타내므로 동일 elapsed time이 서로 다른 두 통계 범주에
각각 한 번 포함될 수 있다.

비동기 back write 설정에서는 표준 put/remove count와 시간은 caller가 wrapper operation을
성공적으로 반환받은 시점까지를 뜻한다. 이후 remote commit 실패, retry, 최종 completion은
`NearJCache.lastBackCacheWrite`의 `operationId`, `operation`, `completion` 또는
`addBackCacheWriteListener`가 제공하는 `BackCacheWriteCompletion`의 책임이다. MXBean 수치는
durable back commit 성공을 뜻하지 않는다. dashboard는 `completion`의 exceptional completion을
remote failure로 분류하고 동일 `operationId` 안에서만 caller-visible operation과 상관관계를
맞춘다. MXBean 자체에는 remote completion/failure counter를 추가하지 않는다.

```kotlin
val observation = nearCache.addBackCacheWriteListener { write ->
    write.completion.whenComplete { _, error ->
        if (error != null) {
            reportRemoteWriteFailure(write.operationId, write.operation, error)
        }
    }
}

nearCache.put("42", user)   // MXBean put은 caller-visible 성공
// application shutdown에서 observation.close()
```

동시에 여러 mutation을 실행하는 caller는 `lastBackCacheWrite`를 polling해 특정 호출과 연결하지
않고 listener가 전달한 atomic `BackCacheWriteCompletion`을 사용한다.

### 6.3 reset과 동시성

active recorder는 `AtomicReference<CountersGeneration>`을 보유한다. 각 operation은 current
generation을 한 번 읽고 그 generation에만 기록한다. statistics `clear()`는 새 generation을
`getAndSet`하는 시점에 선형화된다.

clear와 동시에 진행 중인 operation은 generation을 읽은 순서에 따라 이전 또는 새 generation
중 정확히 하나에 기록된다. getter는 current generation을 한 번 읽고 그 generation의 counter만
조회한다. percentage와 average getter도 한 generation reference에서 필요한 count/time을 함께
읽는다. `LongAdder` 기반 개별 값과 서로 다른 JMX attribute 호출 사이에는 atomic snapshot을
보장하지 않으며 속성별로 weakly consistent하다. 한 getter 계산 안에서는 서로 다른 generation을
섞지 않는다.

standard/custom statistics view는 같은 recorder를 사용한다. 어느 view에서 `clear()`를 호출해도
logical counter와 tier counter를 함께 초기화한다. `NearJCache.clear()`는 recorder를 초기화하지
않는다.

```kotlin
nearCache.put("42", user)
statistics.clear()          // 통계 초기화: logical/tier counter만 0으로 초기화
check(nearCache.get("42") == user)

nearCache.clear()           // 데이터 삭제: front/back 데이터를 삭제, counter는 유지
check(nearCache.get("42") == null)

EmptyNearJCacheStatisticsMXBean().clear() // 항상 안전한 no-op
```

README/KDoc 예제의 `statistics`는 registration의 statistics ObjectName으로 만든
`NearJCacheTierStatisticsMXBean` proxy다. data clear와 statistics clear를 같은 변수명이나
주석으로 축약하지 않는다.

### 6.4 disabled 비용 계약

statistics flag가 false이면 construction-time에 singleton NoOp recorder를 선택한다.
disabled operation path는 다음 비용을 만들지 않는다.

- atomic/adder counter 생성 또는 update
- `System.nanoTime()` 호출
- operation별 통계 객체나 lambda allocation
- JMX 등록 side effect

wrapper에는 recorder 선택을 위한 예측 가능한 final reference/NoOp 호출 경계만 남는다.
“오버헤드 없음”은 CPU instruction이 문자 그대로 0이라는 뜻이 아니라 위 비용이 없다는
구체적인 계약이다.

recorder API는 higher-order function을 받지 않는 direct method로 제한한다. active recorder의
`startTimeNanos()`만 package-internal `TimeSource`를 호출하고 NoOp recorder는 clock을 호출하지
않는 sentinel을 반환한다. counting fake `TimeSource`로 disabled 호출 수 0을 검증한다.

disabled 성능 회귀는 동일 benchmark 안의 계측 전 control path와 disabled recorder path를
비교한다. JMH/JFR 또는 저장소의 기존 JVM benchmark 도구로 steady-state allocation delta
`0 B/op`, median throughput regression `5%` 이하를 확인한다. 이 수치는 기능 테스트의 flaky
pass/fail 조건으로 사용하지 않고 동일 머신·JDK·fork/warmup 조건의 release evidence로 보존한다.

기존 `NearJCacheStatisticsMXBean()` no-arg constructor와 public `add*`는 active standalone
bean으로 유지한다. `EmptyNearJCacheStatisticsMXBean`은 NoOp recorder를 사용하고
`addRemovals`를 포함한 모든 mutator를 무력화한다.

### 6.5 eviction 계약

generic `cache-core` v1은 front capacity eviction을 관찰하지 않는다.
`frontEvictionObservationSupported=false`, `frontEvictions=0`, `cacheEvictions=0`을 반환한다.
0은 eviction이 없었다는 증거가 아니라 관찰 capability가 없다는 뜻이다. back cache의
`EXPIRED` event를 front eviction으로 변환하지 않는다.

provider별 정확한 eviction adapter는 후속 이슈에서 추가할 수 있다. 그때 capability가 true인
provider만 standard/custom eviction counter를 함께 증가시킨다.

## 7. JMX 등록과 lifecycle

### 7.1 explicit API

등록은 항상 explicit 호출로 수행한다. additive API는 caller가 다음 값을 제공하도록 한다.

- caller-owned `MBeanServer`
- 안정적이고 비밀이 아닌 opaque `managerId`
- 안정적이고 비밀이 아닌 opaque `cacheId`

공개 API shape는 다음과 같다.

```kotlin
fun NearJCache<*, *>.registerMBeans(
    mBeanServer: MBeanServer,
    managerId: String,
    cacheId: String,
): NearJCacheMBeanRegistration

interface NearJCacheMBeanRegistration: AutoCloseable {
    val managerId: String
    val cacheId: String
    val state: NearJCacheMBeanRegistrationState
    val activeObjectNames: Set<ObjectName>
    val isClosed: Boolean
    override fun close()
}

enum class NearJCacheMBeanRegistrationState {
    REGISTERED,
    RECOVERY_REQUIRED,
    CLOSING,
    CLOSED,
}

class NearJCacheMBeanRegistrationException(
    @Transient val recoveryRegistration: NearJCacheMBeanRegistration?,
    val remainingObjectNames: Set<ObjectName>,
    cause: Throwable,
): RuntimeException(cause)
```

exception은 명시적인 `serialVersionUID`를 제공한다. `recoveryRegistration`은 current process에서
즉시 retry할 때만 사용하는 transient handle이다. 직렬화된 exception은 defensive immutable
`remainingObjectNames`만 진단 정보로 보존하며 deserialization 뒤 handle은 `null`이다.

세 인자는 모두 필수다. platform `MBeanServer`나 cache name을 암묵적으로 선택하는 overload와
default parameter는 제공하지 않는다. blank ID는 등록 전에 `IllegalArgumentException`으로
거부한다.

library는 manager URI, provider properties, cache key/value, credential에서 ID를 자동 생성하지
않는다. random default도 제공하지 않는다. 재시작 뒤 dashboard가 같은 ObjectName을 찾으려면
caller가 동일한 stable ID를 다시 제공해야 한다.

ObjectName은 custom domain을 사용한다.

```text
io.bluetape4k.cache:type=NearJCacheConfiguration,manager=<quoted>,cache=<quoted>
io.bluetape4k.cache:type=NearJCacheStatistics,manager=<quoted>,cache=<quoted>
```

`manager`와 `cache` property는 `ObjectName.quote`로 인코딩한다. 표준 interface를 유지하되
provider가 소유한 `javax.cache` namespace와 충돌하지 않는다.

```kotlin
val server = ManagementFactory.getPlatformMBeanServer()
val registration = nearCache.registerMBeans(
    mBeanServer = server,
    managerId = "prod-orders-app",
    cacheId = "user-profile",
)

val statisticsName = registration.activeObjectNames.single {
    it.getKeyProperty("type") == "NearJCacheStatistics"
}
val statistics = JMX.newMXBeanProxy(
    server,
    statisticsName,
    NearJCacheTierStatisticsMXBean::class.java,
)

// application shutdown: registration.close() 후 nearCache.close()
```

configuration-only 또는 statistics-only flag에서는 존재하는 ObjectName만 조회한다. 예제는
두 bean이 모두 활성화된 경우이며 platform server, ID, 등록·해제 시점을 모두 caller가 결정한다.

### 7.2 flag matrix

| `managementEnabled` | `statisticsEnabled` | explicit 등록 결과 |
|---|---|---|
| `true` | `false` | configuration MXBean만 등록 |
| `false` | `true` | statistics MXBean만 등록 |
| `true` | `true` | configuration, statistics MXBean을 한 transaction으로 등록 |
| `false` | `false` | `IllegalStateException`으로 조기 거부 |

flag는 자동 등록을 뜻하지 않고 explicit 등록에서 허용할 bean 집합만 결정한다. configured flag와
live registration state는 별개다. 반환 handle의 `activeObjectNames`는 현재 MBeanServer에
등록된 것으로 handle이 추적하는 이름의 defensive unmodifiable snapshot이다. Kotlin
`Set` view뿐 아니라 Java caller도 원본 collection을 변경할 수 없다. `state`는 정상 등록
`REGISTERED`, 부분 rollback/unregister 실패 `RECOVERY_REQUIRED`, 해제 진행 `CLOSING`, 완전 해제
`CLOSED`를 구분한다. handle이 없는 `DISABLED`와 `NOT_REGISTERED`는 application이 configured
flag와 handle inventory로 구분한다.

### 7.3 원자성, 충돌, 해제

- `NearJCache` lifecycle은 논리적으로 `OPEN -> CLOSING -> CLOSED`로만 전이한다. 기존
  `closeStarted`/`closeCompleted` 상태와 `close()`가 사용하는 동일 lifecycle lock 안에서
  전이를 구현하며 별도 경쟁 lock을 만들지 않는다.
- `registerMBeans()`는 lifecycle lock 안에서 `OPEN` 확인, MBean 등록, handle의 cache registry
  삽입까지 수행한다. 등록/rollback이 진행 중인 동안 `close()`는 같은 lock에서 대기한다.
- `close()`는 lock 안에서 먼저 `CLOSING`으로 전이해 이후 등록을 거부하고 registry의 handle을
  모두 drain한다. rollback failure의 recovery handle도 exception을 던지기 전에 같은 lock 안에서
  registry에 삽입한다.
- 등록할 ObjectName이 이미 있으면 기존 MBean을 교체하거나 해제하지 않고 실패한다.
- collision의 `InstanceAlreadyExistsException`을 포함한 `JMException`은 원인을 보존해 caller에게 전달한다.
- registrar는 `registerMBean`이 `ObjectInstance`를 정상 반환한 ObjectName만 current transaction의
  owned set에 넣는다. 두 bean 등록 중 두 번째가 실패하면 owned set만 rollback한다.
- `InstanceAlreadyExistsException`이 발생한 collision 이름과 정상 반환 전에 실패한 이름은 다른
  owner의 MBean일 수 있으므로 절대 unregister하지 않는다.
- rollback/unregister 중 `InstanceNotFoundException`은 이미 해제된 성공 상태로 처리해 remaining
  set에서 제거한다.
- rollback 실패는 primary registration failure에 suppressed exception으로 보존한다. 해제하지 못한
  ObjectName이 있으면 `NearJCacheMBeanRegistrationException`이 남은 이름만 가진
  `recoveryRegistration`을 제공한다. 이 handle은 이미 cache registry에도 들어 있어 caller의
  explicit retry와 이후 cache close 양쪽에서 정리할 수 있다.
- registration handle은 `activeObjectNames`를 원자적으로 관리한다. 성공적으로 해제한 이름은
  집합에서 제거하며, 모든 이름을 해제한 뒤 `CLOSED`가 된다. `CLOSED` 뒤 `close()`는 no-op이다.
- 일부 unregister가 실패하면 handle은 실패한 ObjectName을 남긴 `RECOVERY_REQUIRED` 상태로
  돌아가며 다음 `close()`가 남은 이름만 재시도한다. concurrent `close()`는 직렬화한다.
- `NearJCache`는 자신을 통해 만든 handle을 추적하고 `NearJCache.close()`에서 해제한다.
- handle이 모든 ObjectName을 해제해 `CLOSED`가 되면 자신을 cache registry에서 제거해
  `MBeanServer`와 registration state를 불필요하게 보유하지 않는다.
- explicit handle close와 cache close가 경합해도 각 ObjectName은 최대 한 번의 성공 unregister만
  필요하며 최종 handle 상태는 closed다.
- `NearJCache.close()`는 JMX handle, back listener, front cache 순서로 모두 정리를 시도한다.
  unregister가 실패하면 기존 primary/suppressed failure 정책을 따르고 `closeCompleted=false`를
  유지해 다음 cache close에서도 남은 ObjectName을 재시도한다.
- JMX handle, listener, front cache는 각자 completion 상태를 가진다. close가 실패해도
  `CLOSING`을 유지하고 다음 `close()`는 미완료 resource만 재시도한다. 이미 성공한 cleanup은
  다시 실행하지 않으며 모든 resource cleanup이 성공한 경우에만 `CLOSED`/`closeCompleted=true`로
  전이한다.
- `MBeanServer`, back cache, provider는 caller 소유이며 registrar/cache close가 닫지 않는다.

configuration bean은 immutable snapshot만, statistics bean은 recorder만 보유한다. 어느 bean도
`NearJCache`, front/back cache, provider에 대한 strong reference를 보유하지 않는다.

## 8. 호환성

다음 JVM/source contract를 보존한다.

- `NearJCache(frontCache, backCache, config)` public constructor descriptor
- `NearJCacheConfig` primary/legacy constructor, `copy`, component 순서, custom serialization
- `NearJCacheManagementMXBean(NearJCache)` constructor
- `NearJCacheStatisticsMXBean()` constructor
- `NearJCacheStatisticsMXBean.addHits/addMisses/addPuts/addRemovals/addEvictions`와 time mutator
- `EmptyNearJCacheStatisticsMXBean` type

기존 management bean constructor는 전달받은 cache에서 snapshot을 만든 뒤 cache reference를
보관하지 않는다. 새 registrar, configuration/tier MXBean interface, registration state와
exception은 additive public API다. 공개 type에는 한국어 KDoc과 실제 factory를 사용하는 예제를
제공한다.

## 9. 실패 모드와 처리

| 실패 모드 | 처리 |
|---|---|
| front/back type이 모두 미확정 | pair를 `Object,Object`로 snapshot하고 문서화 |
| `CompleteConfiguration`을 제공하지 않는 front | loader/writer와 enable flag를 안전한 `false`로 snapshot |
| duplicate ObjectName | 기존 MBean을 보존하고 등록 실패 반환 |
| 두 번째 MBean 등록 실패 | 첫 등록 rollback, rollback 실패는 suppressed와 recovery handle로 보존 |
| explicit close와 cache close 경합 | idempotent handle state로 단일 해제 효과 보장 |
| unregister 실패 | primary/suppressed 정책으로 전달하고 retry 가능한 registration 상태 보존 |
| reset과 update 경합 | generation read/getAndSet 순서로 이전/새 generation 중 하나에만 기록 |
| async back write가 caller 반환 뒤 실패 | 표준 count는 caller-visible 성공, remote 실패는 back-write observation에서 확인 |
| bulk remove의 실제 삭제 수 미확정 | 표준 removal 수를 추정하지 않고 capability false 노출 |
| generic provider의 eviction 미관찰 | eviction 0과 support false를 함께 노출 |
| close 시작 뒤 등록 요청 | `IllegalStateException`으로 조기 거부 |
| ID에 `:`, `,`, `=`, `*`, `?`, quote 포함 | `ObjectName.quote`를 적용하고 pattern ObjectName을 만들지 않음 |

## 10. 테스트 전략

### 10.1 configuration

- actual front type/flag snapshot
- `Object,Object` front에서 supplied front, back, 최종 Object pair fallback
- `typeResolutionSource`와 `typeResolutionExact`가 fallback source를 구분
- key/value pair가 서로 다른 configuration source에서 섞이지 않음
- `storeByValue=false` invariant
- construction 이후 manager flag 변경이 snapshot/recorder를 바꾸지 않음

### 10.2 statistics

- front hit, front miss/back hit, front miss/back miss
- mixed `getAll`의 key별 logical/tier count와 bulk latency aggregate
- fixed `TimeSource`에서 bulk size 0, 1, N의 raw elapsed 단일 누적과 getter-only division
- nanosecond 누적에서 microsecond `Float` 변환, zero-count `0F`
- `getAnd*`, conditional mutation success/failure
- `putAll` full success와 exceptional partial-unknown 계약
- `removeAll(Set)`/`removeAll()` count unsupported 계약
- cache `clear()`와 statistics `clear()` 독립성
- concurrent update/reset generation 경계
- async caller-return count와 back-write observation 분리
- exceptional `BackCacheWriteCompletion`을 동일 operation ID의 remote failure로 분류
- `statisticsScope`, stable `supportedOperations`, `backWriteCompletionIncluded=false`
- unsupported eviction의 `0 + support=false`

### 10.3 disabled path

- NoOp recorder가 atomic/adder state를 만들지 않음
- counting fake `TimeSource`에서 disabled operation의 clock 호출이 0
- recorder direct method 경계에서 operation별 lambda/통계 allocation/update가 없음
- 동일 조건 benchmark에서 disabled allocation delta `0 B/op`, throughput regression `5%` 이하
- `EmptyNearJCacheStatisticsMXBean.addRemovals` 포함 모든 mutator가 no-op

### 10.4 JMX와 lifecycle

- management-only, statistics-only, both, neither flag matrix
- `REGISTERED`, `RECOVERY_REQUIRED`, `CLOSING`, `CLOSED`와 active ObjectName snapshot
- `MBeanInfo`에 custom/standard getter와 `clear`만 노출되고 legacy `add*`가 노출되지 않음
- statistics ObjectName 하나가 inherited standard/custom attribute와 shared `clear()`를 제공
- special-character ID quoting과 non-pattern ObjectName
- URI/credential/key/value가 ObjectName, exception, log에 포함되지 않음
- duplicate collision에서 기존 MBean 보존
- 두 번째 이름 collision에서도 first owned MBean만 rollback하고 collision MBean은 보존
- 두 번째 등록 실패 rollback과 rollback failure suppression
- rollback 실패 뒤 recovery handle explicit/cache-close retry
- recovery exception serialization은 immutable remaining names를 보존하고 transient handle을 제외
- registration/close 경합에서 `OPEN -> CLOSING` 선형화와 registry drain
- confirmed-owned set만 rollback, collision MBean 비삭제, `InstanceNotFoundException` 성공 처리
- handle close, cache close, concurrent close의 idempotency
- unregister 실패 뒤 retry와 최종 상태
- cache close 시작 뒤 등록 거부
- caller-owned `MBeanServer`, back cache, provider가 닫히지 않음
- runbook의 configured/live inventory 분류와 MBeanServer query 예제

### 10.5 호환성과 coverage

- `NearJCache`, `NearJCacheConfig`, management/statistics bean의 JVM descriptor 회귀 검사
- 기존 `NearJCacheConfig` serialization round-trip
- management package Kover exclusion 제거 또는 최소 범위 축소
- `cache-core` targeted test와 `detekt`

Testcontainers는 이 설계의 필수 proof가 아니다. configuration, operation matrix, JMX lifecycle은
fake JCache와 platform/test `MBeanServer`로 재현한다. provider별 integration은 기존 동작을
변경하는 경우에만 해당 module에서 순차 실행한다.

## 11. 문서 계약

- public interface/class/extension과 비자명한 internal recorder/snapshot에 한국어 KDoc을 작성한다.
- `cache/cache-core/README.md`와 `README.ko.md`에 source-equivalent configuration,
  statistics 범위, explicit JMX 등록, lifecycle 예제를 추가한다.
- EN/KO manual의 NearJCache management/statistics 설명을 함께 갱신한다.
- `readThrough/writeThrough`와 Near Cache tier 동작, cache `clear()`와 statistics `clear()`,
  caller-visible async 성공과 durable back commit을 각각 분리해 설명한다.
- unsupported 연산, bulk removal count, generic eviction capability를 숨기지 않는다.
- raw manager URI나 credential을 ObjectName으로 사용하는 예제를 작성하지 않는다.

### 11.1 운영 runbook

README/manual의 운영 절은 다음 절차를 제공한다.

1. `managerId`는 environment/cluster/application instance를 구분하는 안정적인 non-secret ID,
   `cacheId`는 application 안의 logical cache를 구분하는 안정적인 non-secret ID로 정한다.
   raw URI, hostname credential, tenant 개인정보는 사용하지 않는다. 사람이 읽는 cache name과
   opaque ID의 mapping은 application config와 dashboard inventory에 보관한다.
2. application 시작 시 configured flag와 registration handle inventory를 비교해
   `DISABLED`, `NOT_REGISTERED`, `REGISTERED`, `RECOVERY_REQUIRED`를 분류한다. 기대한
   `activeObjectNames`가 MBeanServer query 결과에 없으면 health/alert 실패로 처리한다.
3. collision에서는 기존 MBean을 교체하지 않는다. 동일 process에서 같은 ID를 사용하는 살아 있는
   owner를 먼저 확인한다. stale registration으로 확인된 이름만 해당 owner handle 또는
   `recoveryRegistration`으로 해제하고 재등록한다.
4. `RECOVERY_REQUIRED`는 즉시 alert하고 handle `close()`를 재시도한다. `CLOSING`은 bounded transient
   상태로만 허용하며 종료 뒤에도 남으면 cleanup failure로 처리한다.
5. `statisticsScope=NEAR_JCACHE_WRAPPER_V1`, `supportedOperations`,
   `frontEvictionObservationSupported`, `bulkRemovalCountSupported`,
   `backWriteCompletionIncluded`를 dashboard annotation과 alert 조건에 반영한다. 0인 unsupported
   counter를 “사건 없음”으로 해석하지 않는다.
6. 비동기 write에서는 MXBean put/remove 성공을 remote commit 성공으로 사용하지 않는다.
   `lastBackCacheWrite` 또는 `addBackCacheWriteListener`의 같은 `operationId` completion을 확인하고
   exceptional completion을 별도 장애 신호로 처리한다.
7. 종료 시 handle close와 MBeanServer query로 `activeObjectNames`가 모두 사라졌는지 확인한다.
   caller-owned `MBeanServer`, back cache, provider는 종료 대상에 포함하지 않는다.

JMX-only dashboard는 존재하지 않는 MBean만으로 `DISABLED`와 `NOT_REGISTERED`를 구분할 수 없다.
application health/inventory가 configured flag와 handle state를 함께 제공해야 한다. owner component가
사라졌지만 shared in-process MBeanServer에 stale MBean이 남고 소유 handle도 없으면 library는 이를
자동 해제하지 않는다. 다른 owner의 MBean을 삭제하지 않도록 host application을 재시작하거나,
운영자가 exact ObjectName과 inactive owner를 별도로 검증한 뒤 관리 절차로 해제한다.

runbook example은 configuration/statistics MBean query, configured/live state 분류, collision과
recovery retry, async completion 상관관계, unsupported metric 해석을 포함한다.

## 12. Issue 수용 기준 추적성

| Issue #1351 수용 기준 | 설계 대응 | 검증 |
|---|---|---|
| MXBean이 실제 설정값을 반환 | front-derived immutable snapshot과 pair fallback | configuration unit test |
| front/back 동작을 일관되게 통계에 반영 | 지원 operation matrix의 logical/tier 분리 | operation matrix test |
| 비활성화 시 기존 동작과 성능 계약 유지 | construction-time NoOp recorder와 명시적 비용 상한 | disabled regression test |
| JMX 등록·해제 및 counter reset | explicit registrar, transactional handle, generation reset | JMX/lifecycle/concurrency test |
| generic type fallback 문서화 | `Object,Object` sentinel과 pair-level fallback | fallback/KDoc/README test |

원문의 “front/back 동작”은 이번 버전에서 명시한 wrapper operation으로 제한한다.
`loadAll`, `invoke`, `invokeAll`, `SuspendNearJCache`, provider eviction adapter는 후속 범위다.
원문의 “clear() 이후 counter reset”은 `CacheStatisticsMXBean.clear()`를 뜻하며
`NearJCache.clear()`의 데이터 삭제와 결합하지 않는다.

## 13. 완료 조건

- [x] 이 설계 문서가 Step 2-R의 6개 관점에서 P0=0/P1=0을 만족한다.
- [ ] 모든 public/serialization ABI 보존 테스트가 통과한다.
- [ ] 지원 operation matrix와 excluded operation이 source/KDoc/README/manual에서 일치한다.
- [ ] disabled 비용 계약과 reset concurrency가 회귀 테스트로 고정된다.
- [ ] JMX flag matrix, 보안, collision, rollback, lifecycle 테스트가 통과한다.
- [ ] operator runbook의 ID mapping, inventory/health query, recovery, async/unsupported metric 예제가 검증된다.
- [ ] `cache-core` targeted tests, `detekt`, `git diff --check`가 통과한다.
- [ ] Epic stacked train의 선행 PR head를 base로 #1351 PR을 생성한다.
- [ ] Type A lesson과 최종 DoD를 커밋한다.

## 14. Stacked PR train

```text
develop
  └─ PR #1432 fix/1426-nearcache-observation
       └─ PR #1433 fix/1348-lettuce-entryprocessor-atomicity @ 513f70e
            └─ feat/1351-nearcache-management
```

#1351 PR의 base는 `fix/1348-lettuce-entryprocessor-atomicity`다. 선행 PR의 필수 CI와
review blocker가 해결되기 전에는 #1351을 merge하지 않는다. PR 생성, push, merge는 각각
해당 workflow gate의 별도 증거와 권한을 따른다.

rollout은 다음 순서를 따른다.

1. 기존 flag가 false인 상태에서 additive API와 disabled recorder를 배포해 기존 동작을 확인한다.
2. 한 cache에서 stable ID와 explicit registration을 활성화하고 MBean inventory, scope capability,
   async completion 관찰을 검증한다. runtime toggle을 지원하지 않으므로 flag 변경은 새
   `NearJCache`를 만드는 application restart/deployment와 함께 적용한다.
3. collision, `RECOVERY_REQUIRED`, stale registration, back-write failure alert가 없는 것을 확인한
   뒤 대상 cache를 단계적으로 늘린다.
4. rollback은 먼저 registration handle을 닫아 custom-domain MBean을 제거하고 이전 binary/config로
   되돌린다. persistent schema나 `NearJCacheConfig` serialization shape는 바뀌지 않는다.
   rollback한 dashboard는 새 capability attribute가 없거나 MBean 자체가 없는 상태를 허용해야 한다.
