---
title: JCache Near Cache와 직렬화 제한
description: listener factory가 Hazelcast cluster로 직렬화될 때 생기는 실패와 listener-free factory의 capability를 설명합니다.
manualId: bluetape4k-cache-hazelcast
chapterId: jcache-near-cache-serialization
---

# JCache Near Cache와 직렬화 제한

## 직접 listener를 등록하면 왜 실패하는가

직접 `NearJCache(config, backCache)`를 생성하면 back JCache event로 front cache를 갱신하려고 `MutableCacheEntryListenerConfiguration`을 등록합니다. listener factory가 Caffeine cache proxy를 캡처하면 Hazelcast가 이 구성을 cluster로 보내는 과정에서 직렬화하지 못합니다. 공개 `HazelcastNearJCache(...)` factory는 더 이상 이 listener-backed 경로를 사용하지 않습니다.

release tests는 직접 listener-backed 구성이 `HazelcastSerializationException`과 내부 `NotSerializableException`으로 실패하는 것을 명시적으로 검증합니다. 따라서 이 생성자는 Hazelcast client JCache back cache에서 계속 지원하지 않습니다.

## Hazelcast factory는 listener를 빼고 만든다

`HazelcastCaches.nearJCache`는 Caffeine front JCache와 Hazelcast back JCache를 직접 조합하고 listener를 등록하지 않습니다. 공개 `HazelcastNearJCache(...)` factory는 호출자가 제공한 front JCache와 Hazelcast back JCache를 같은 listener-free 경로로 조합합니다. `suspendNearJCache`도 고정된 Caffeine front를 만들고 `SuspendNearJCache.withoutListener`를 사용합니다.

직렬화 가능한 `NearJCacheConfig`와 destructive clear 권한은 별도 계약입니다. 기존
Hazelcast factory 호출의 기본값은 `NearJCacheClearAuthority.DENY`이므로 공유 namespace에는
key-scoped `removeAll(keys)`를 사용합니다. `clear()` 또는 `clearAllCache()`를 호출하려면
back namespace 전체를 caller가 소유한다고 확인한 뒤
`NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE`를 전달합니다. 이 권한은 runtime-only이며
Hazelcast configuration 직렬화로 전송되지 않습니다. wrapper `close()`는 전달받은 front만
닫고 Hazelcast back cache나 provider는 닫지 않습니다.

```kotlin
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority

val cache = HazelcastCaches.nearJCache<String, User>(
    hazelcast,
    NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
) {
    cacheName = "users-v1"
}

cache.put("42", user)
check(cache.getDeeply("42") == user)
cache.clear()             // front and back
check(cache.getDeeply("42") == null)
```

read-through와 두 계층 write는 동작하지만 다른 process가 back cache를 바꿔도 이 front cache를 지울 listener가 없습니다. factory 성공을 peer invalidation 지원으로 해석하면 안 됩니다.

## native IMap Near Cache와 구분한다

peer 변경 무효화가 필요하면 JCache factory 대신 `HazelcastNearCache`의 `IMap.addEntryListener` 경로를 사용합니다. 이 listener는 client JVM에서 실행되므로 Caffeine L1을 캡처해도 JCache listener factory와 같은 cluster serialization 문제가 없습니다.

| 선택 | 장점 | 제한 |
| --- | --- | --- |
| factory JCache Near Cache | JCache front/back 계약 재사용 | listener 없음, peer L1 propagation 없음 |
| direct listener-backed JCache construction | 의도는 event propagation | 1.12.1에서 직렬화 실패 |
| native IMap Near Cache | client-side entry listener 무효화 | String key, JCache API가 아님 |

## suspend factory의 고정 front 설정

1.12.1의 `suspendNearJCache` factory는 Caffeine front를 최대 10,000개, 접근 후 30분 만료로 직접 만듭니다. 전달한 `NearJCacheConfig`의 cache 이름은 back cache에 쓰지만 front 용량과 만료는 이 고정 구성입니다. 다른 정책이 필요하면 지원 capability를 확인한 별도 구성을 사용합니다.

<!-- nearjcache-clear-authority-contract -->
### #1368 shared-back clear authority

Listener-free factory도 기본값은 `DENY`입니다. 공유 namespace에서는 key-scoped
operation을 사용하고, 독점 owner만 namespace clear를 opt-in합니다. runtime-only
권한은 직렬화되는 configuration에 포함되지 않습니다.

```kotlin
val shared = HazelcastCaches.nearJCache<String, User>(hazelcast)
shared.removeAll(setOf("tenant-a:key-1"))
val owner = HazelcastCaches.nearJCache<String, User>(
    hazelcast,
    NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
) { cacheName = "users-owner" }
owner.clear()
```
<!-- /nearjcache-clear-authority-contract -->

## Source와 tests

- [`HazelcastCaches.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/HazelcastCaches.kt)
- [`HazelcastNearJCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCache.kt)
- [`HazelcastSuspendNearJCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastSuspendNearJCache.kt)
- [`HazelcastNearJCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastNearJCacheTest.kt)
- [`HazelcastSuspendNearJCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/jcache/HazelcastSuspendNearJCacheTest.kt)
