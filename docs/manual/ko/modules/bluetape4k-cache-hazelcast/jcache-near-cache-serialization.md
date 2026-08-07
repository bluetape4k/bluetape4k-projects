---
title: JCache Near Cache와 직렬화 제한
description: listener factory가 Hazelcast cluster로 직렬화될 때 생기는 실패와 listener-free factory의 capability를 설명합니다.
manualId: bluetape4k-cache-hazelcast
chapterId: jcache-near-cache-serialization
---

# JCache Near Cache와 직렬화 제한

## 직접 listener를 등록하면 왜 실패하는가

`HazelcastNearJCache`는 back JCache event를 받아 front cache를 갱신하려고 `MutableCacheEntryListenerConfiguration`을 등록합니다. listener factory가 Caffeine cache proxy를 캡처하면 Hazelcast가 이 구성을 cluster로 보내는 과정에서 직렬화하지 못합니다.

release tests는 이 경로가 `HazelcastSerializationException`과 내부 `NotSerializableException`으로 실패하는 것을 명시적으로 검증합니다. 즉, 이 factory는 문서상 가능한 조합처럼 보여도 1.12.1 client JCache 구성에서는 지원 경로가 아닙니다.

## HazelcastCaches factory는 listener를 빼고 만든다

`HazelcastCaches.nearJCache`는 Caffeine front JCache와 Hazelcast back JCache를 직접 조합하고 listener를 등록하지 않습니다. `suspendNearJCache`도 고정된 Caffeine front를 만들고 `SuspendNearJCache.withoutListener`를 사용합니다.

```kotlin
val cache = HazelcastCaches.nearJCache<String, User>(hazelcast) {
    cacheName = "users-v1"
}

cache.put("42", user)
cache.clear()             // front only
check(cache.getDeeply("42") == user)
```

read-through와 두 계층 write는 동작하지만 다른 process가 back cache를 바꿔도 이 front cache를 지울 listener가 없습니다. factory 성공을 peer invalidation 지원으로 해석하면 안 됩니다.

## native IMap Near Cache와 구분한다

peer 변경 무효화가 필요하면 JCache factory 대신 `HazelcastNearCache`의 `IMap.addEntryListener` 경로를 사용합니다. 이 listener는 client JVM에서 실행되므로 Caffeine L1을 캡처해도 JCache listener factory와 같은 cluster serialization 문제가 없습니다.

| 선택 | 장점 | 제한 |
| --- | --- | --- |
| factory JCache Near Cache | JCache front/back 계약 재사용 | listener 없음, peer L1 propagation 없음 |
| direct listener-backed JCache factory | 의도는 event propagation | 1.12.1에서 직렬화 실패 |
| native IMap Near Cache | client-side entry listener 무효화 | String key, JCache API가 아님 |

## suspend factory의 고정 front 설정

1.12.1의 `suspendNearJCache` factory는 Caffeine front를 최대 10,000개, 접근 후 30분 만료로 직접 만듭니다. 전달한 `NearJCacheConfig`의 cache 이름은 back cache에 쓰지만 front 용량과 만료는 이 고정 구성입니다. 다른 정책이 필요하면 지원 capability를 확인한 별도 구성을 사용합니다.

## Source와 tests

- [`HazelcastCaches.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt)
- [`HazelcastNearJCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCache.kt)
- [`HazelcastSuspendNearJCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastSuspendNearJCache.kt)
- [`HazelcastNearJCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCacheTest.kt)
- [`HazelcastSuspendNearJCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastSuspendNearJCacheTest.kt)
