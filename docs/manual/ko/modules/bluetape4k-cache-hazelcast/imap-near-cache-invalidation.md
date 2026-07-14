---
title: IMap Near Cache와 무효화
description: Caffeine L1과 Hazelcast IMap L2의 조회·쓰기 순서, entry listener 무효화와 통계를 설명합니다.
manualId: bluetape4k-cache-hazelcast
chapterId: imap-near-cache-invalidation
---

# IMap Near Cache와 무효화

## L1 miss만 IMap으로 간다

`HazelcastNearCache`와 suspend 구현은 String key 전용입니다. 먼저 Caffeine L1을 읽고 miss면 같은 `cacheName`의 `IMap`을 조회해 성공한 값을 L1에 채웁니다. `getAll`은 L1 miss key만 `IMap.getAll`로 묶습니다.

```kotlin
val products = HazelcastCaches.nearCache<Product>(hazelcast) {
    cacheName = "products-v1"
    maxLocalSize = 5_000
    frontExpireAfterWrite = Duration.ofMinutes(5)
    recordStats = true
}
```

`frontExpireAfterWrite`와 `frontExpireAfterAccess`는 Caffeine L1에만 적용됩니다. IMap TTL과 eviction은 Hazelcast map config에서 별도로 정합니다.

## entry event가 다른 L1을 지운다

cache 생성 시 `IMap.addEntryListener(listener, true)`를 등록합니다. update·remove event가 원격 member에서 왔다고 판단되면 해당 key를 L1에서 지우고, expire event는 항상 지웁니다. add event는 무시합니다.

event 전달은 write 응답과 별도의 비동기 흐름입니다. listener는 짧은 stale window를 줄이지만 linearizable read를 만들지는 않습니다. client/member topology에 따라 `event.member.localMember()` 판단이 기대와 같은지 실제 배포 구성으로 확인합니다.

## write는 L1부터 바꾼다

`put`, `putAll`, `remove`, `removeAll`은 L1을 먼저 수정하고 IMap을 호출합니다. IMap operation이 실패하면 L1만 바뀐 상태가 남을 수 있습니다.

```text
put(key, value)
  1. Caffeine L1 put
  2. IMap set
```

반대로 `replace`는 IMap 성공을 확인한 뒤 L1을 갱신합니다. `putIfAbsent`는 먼저 일반 `get`을 수행한 뒤 IMap의 원자적 `putIfAbsent` winner를 확인합니다. API마다 실패 뒤 상태가 같다고 가정하지 않습니다.

## clear와 close의 범위

`clearLocal`은 현재 JVM의 L1만 비웁니다. `clearAll`은 L1과 공유 IMap 전체를 비웁니다. `close`는 listener를 제거하고 L1을 닫지만 IMap 데이터와 Hazelcast instance는 유지합니다.

`isClosed` flag는 중복 close를 막지만 각 operation이 flag를 검사해 실패하도록 강제하지는 않습니다. close 뒤 객체를 재사용하지 않고 application scope에서 참조를 제거합니다.

## 통계 읽기

`stats()`는 Caffeine local hit·miss·eviction·size와 코드가 센 IMap hit·miss를 합칩니다. `recordStats=false`면 local hit·miss·eviction이 0으로 보일 수 있습니다. 숫자 0을 실제 트래픽 없음으로 오해하지 않습니다.

## Source와 tests

- [`HazelcastNearCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCache.kt)
- [`HazelcastSuspendNearCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastSuspendNearCache.kt)
- [`HazelcastEntryEventListener.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastEntryEventListener.kt)
- [`HazelcastLocalCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastLocalCache.kt)
- [`HazelcastNearCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCacheTest.kt)
