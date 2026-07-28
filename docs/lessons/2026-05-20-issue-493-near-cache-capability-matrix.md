# 이슈 #493 Near-Cache Capability Matrix

## 배경

Issue #493은 local/distributed cache provider 전반의 near-cache behavior에 대해 documented backend
capability matrix와 inherited conformance test를 요구했다.

## 결정

Support boundary는 documentation-first와 fixture-based 방식으로 유지한다:

- Native Lettuce, Hazelcast IMap, Redisson near cache는 `NearCacheOperations` /
  `SuspendNearCacheOperations` fixture를 계속 공유한다.
- JCache-backed Lettuce와 Redisson near cache는 JCache fixture를 계속 공유한다.
- Hazelcast JCache listener-backed construction은 unsupported다. Listener configuration이 cluster
  distribution을 위해 serializable이어야 하기 때문이다.
- Hazelcast factory는 read-through/write-through behavior를 위한 listener-free degraded support로 둔다.
- Cache2k whole-cache JCache `removeAll()` propagation은 조용한 skip이 아니라 명시적인 unsupported
  conformance case다.

## 결과

`docs/cache/near-cache-capability-matrix.md`를 추가하고 cache module README pair에서 link했다.
Shared conformance test를 강화했으며 disabled Hazelcast JCache test를 active unsupported/degraded
behavior test로 교체했다.

## 검증

Targeted downstream conformance suite 실행:

```text
./gradlew :bluetape4k-cache-core:test ... :bluetape4k-cache-hazelcast:test ... :bluetape4k-cache-lettuce:test ... :bluetape4k-cache-redisson:test ... --console=plain --no-configuration-cache
BUILD SUCCESSFUL
cache-core: 134 tests
cache-hazelcast: 47 tests
cache-lettuce: 184 tests
cache-redisson: 244 tests
```

Near-cache test scope에 `@Disabled`가 없고 `git diff --check`가 통과함도 확인했다.

## 향후 가이드

Near-cache capability를 바꿀 때는 matrix를 먼저 갱신하고, supported backend가 같은 fixture를 상속하도록
요구한다. Unsupported combination은 active test나 explicit matrix exclusion이어야 하며 disabled abstract
test로 숨기지 않는다.
