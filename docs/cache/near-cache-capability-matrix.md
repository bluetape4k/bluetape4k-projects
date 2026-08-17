# Near-cache backend capability matrix

이 matrix는 cache module이 지원하는 near-cache 동작을 정의한다. 단순한 test
inventory가 아니라 runtime 지원 경계다.

Capability 상태:

- `Supported`: 구현되어 있으며 공용 conformance fixture가 검증한다.
- `Factory degraded`: factory method가 의도적으로 listener를 등록하지 않는다.
  read-through와 write-through는 동작하지만 peer front-cache 전파는 보장하지
  않는다.
- `Local only`: local/front cache 또는 local JCache provider로 사용할 수 있다.
  자체적으로 distributed back-cache invalidation을 제공하지 않는다.
- `Unsupported`: 직접 생성하면 실패하도록 설계했거나 public contract에 포함되지
  않는다.

## Native NearCache API

Native API는 `NearCacheOperations<V>` 또는 `SuspendNearCacheOperations<V>`를
구현한다.

| backend | 동기 near-cache | suspend near-cache | listener 또는 invalidation source | put/replace/remove 전파 | removeAll 의미 | back cache 범위 | conformance |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Lettuce | Supported | Supported | Redis RESP3 `CLIENT TRACKING` plus explicit write-through | Supported | Removes selected keys from front and Redis back cache | Distributed Redis | `AbstractNearCacheOperationsTest`, `AbstractSuspendNearCacheOperationsTest` |
| Hazelcast IMap | Supported | Supported | Hazelcast `IMap` entry listener | Supported | Removes selected keys from front and IMap back cache | Distributed Hazelcast map | `AbstractNearCacheOperationsTest`, `AbstractSuspendNearCacheOperationsTest` |
| Redisson `RLocalCachedMap` | Supported | Supported | Redisson local cached map invalidation | Supported | Removes selected keys from local and Redis-backed map | Distributed Redis via Redisson | `AbstractNearCacheOperationsTest`, `AbstractSuspendNearCacheOperationsTest` |
| Caffeine | Local only | Local only | Local cache events only | Local only | Local only | Local JVM | Covered as front/local cache, not native distributed near-cache |
| Cache2k | Local only | Not provided as native suspend near-cache | Local cache events only | Local only | Local only | Local JVM | Covered as local/JCache provider, not native distributed near-cache |

## JCache NearCache API

JCache API는 `NearJCache<K,V>` 또는 `SuspendNearJCache<K,V>`를 구현한다.

동기 `NearJCache`의 표준 `get`과 `containsKey`는 front miss를 back에서 읽어
front에 채운다. `getAll`은 front hit와 모든 back hit를 반환하고, bulk back hit의
front residency는 설정된 정책을 따른다. `getAndPut`,
`getAndRemove`, `getAndReplace`는 back provider의 원자 compound 연산을
수행한 뒤 front를 갱신한다. 표준 `clear`와
호환 alias `clearAllCache`는 현재 wrapper의 front와 back을 함께 지운다. 공유
back을 사용하는 peer wrapper의 기존 front까지 listener로 지우는 것은 보장하지
않는다. `getDeeply`는 표준 `get`의 alias이며, compound 원자성은 provider의
back atomic operation을 먼저 완료한 뒤 front를 reconciliation하는 계약으로 고정한다.

`NearJCacheConfig`의 기본 front는 store-by-reference이며, 안전한 filtered copier
계약이 없는 custom store-by-value 설정은 생성 단계에서 거부한다. read-through
populate는 mutation epoch로 stale 값을 차단하고, clear는 timeout-late backend
write가 끝난 뒤 back을 지우는 barrier를 사용한다.

<!-- issue-1369-bulk-policy:start -->
### Bulk `getAll` 결과의 front 저장 정책

<!-- contract: default-bypass; bounded-all-or-nothing; single-key-get-unchanged; repeated-back-read; legacy-safe-default -->

기본 `BulkFrontPopulationPolicy.BypassFront`는 결과를 모두 반환하되 bulk back hit를
front에 저장하지 않는다. `BulkFrontPopulationPolicy.PopulateIfAtMost(n)`은
`backValues.size <= n`일 때만 batch 전체를 저장하며 초과 batch의 일부는 저장하지
않는다. entry 수는 메모리에 상주하는 byte 크기나 back 조회 크기 제한이 아니고,
single-key `get()` 저장은 바뀌지 않는다.

Configuration MXBean의 고정 token은 `BYPASS_FRONT`와
`POPULATE_IF_AT_MOST`이며 `bulkFrontPopulationMaximumEntryCount`가 `0`이면 bypass에
상한이 적용되지 않는다는 뜻이다. 새 config와 복원한 legacy stream은 안전한 기본값을
사용한다. 따라서 반복 `getAll`에서 back 반복 조회가 발생할 수 있다. 이전 무제한
batch 저장을 복원하지 않고 front 용량과 로컬 heap 예산에 맞는 상한만
명시한다.
<!-- issue-1369-bulk-policy:end -->

### Blocking `NearJCache` management 범위

Blocking `NearJCache`는 생성 시점의 `immutable configuration snapshot`, wrapper
단위 `logical/tier statistics`, caller가 선택한 server에 대한
`explicit custom-domain JMX` 등록을 지원한다.

| 범위 | 지원 경계 |
| --- | --- |
| 통계 대상 연산 | `get`, `getAll`, `put`, `putAll`, `putIfAbsent`, `replace`, `remove`, `getAndPut`, `getAndReplace`, `getAndRemove` |
| 통계 비대상 연산 | `loadAll`, `invoke`, `invokeAll`, `SuspendNearJCache` |
| capability | `isFrontEvictionObservationSupported=false`, `isBulkRemovalCountSupported=false`, `isBackWriteCompletionIncluded=false` |
| 등록 | management/statistics flag에 따른 명시적 등록, exclusive ObjectName namespace, caller-owned `MBeanServer` |
| lifecycle | registration handle 정리 후 listener, front 순서로 wrapper 자원을 정리하며 back/provider는 소유하지 않음 |

| back provider | 동기 `NearJCache` | suspend `SuspendNearJCache` | listener 등록 | peer front 전파 | removeAll 의미 | back cache 범위 | conformance |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Lettuce JCache | Supported | Supported | Supported | Supported for per-entry mutations; clear peer-front invalidation is not promised | Supported through per-entry removal where needed | Distributed Redis | `AbstractNearJCacheTest`, `AbstractSuspendNearJCacheTest` |
| Redisson JCache | Supported | Supported | Supported | Supported for per-entry mutations; clear peer-front invalidation is not promised | Supported through per-entry removal where needed | Distributed Redis via Redisson | `AbstractNearJCacheTest`, `AbstractSuspendNearJCacheTest` |
| Hazelcast JCache direct listener construction | Unsupported | Unsupported | Unsupported: listener configuration must be serializable for cluster distribution | Unsupported | Unsupported in listener-backed direct construction | Distributed Hazelcast JCache | Explicit unsupported tests in `cache-hazelcast` |
| Hazelcast factory methods | Factory degraded | Factory degraded | Not registered intentionally | Not promised | `clearAllCache` / `clearAll` clear front and back for the local wrapper; listener propagation is not promised | Distributed Hazelcast JCache | Explicit factory behavior tests in `cache-hazelcast` |
| Cache2k JCache | Supported except whole-cache `removeAll()` propagation | Not provided | Local provider listener | Same-JVM only | Whole-cache `removeAll()` propagation is explicitly unsupported by conformance test | Local JVM | `AbstractNearJCacheTest` with explicit unsupported override |
| Caffeine JCache/front | Local only | Supported as front `SuspendJCache` | Local provider listener | Same-JVM only | Local/front only unless paired with a distributed back cache | Local JVM | Used as front cache in suspend JCache conformance |
| Ehcache JCache | Supported | Not provided in this suite | Local provider listener | Same-JVM only | Supported by the sync JCache fixture | Local JVM | `AbstractNearJCacheTest` |

## Test 소유권

- `cache-core/src/testFixtures/.../AbstractNearCacheOperationsTest.kt`와
  `AbstractSuspendNearCacheOperationsTest.kt`가 native near-cache conformance를
  정의한다.
- `cache-core/src/testFixtures/.../jcache/AbstractNearJCacheTest.kt`와
  `AbstractSuspendNearJCacheTest.kt`가 JCache near-cache conformance를 정의한다.
- backend module은 지원 조합에 이 fixture를 상속한다.
- 지원하지 않는 조합은 unsupported 또는 degraded 동작을 검증하는 active test를
  사용해야 한다. 비활성화한 test class는 지원 여부를 나타내는 표식으로 사용할
  수 없다.
