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

동기 `NearJCache`의 표준 `get`, `containsKey`, `getAll`은 front miss를 back에서
읽고 back hit를 front에 채우는 논리적 2-tier read 계약을 따른다. `getAndPut`,
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
