---
title: Suspend JCache와 async 경계
description: Hazelcast ICache async API와 Dispatchers.IO fallback, bulk 처리와 복합 연산의 원자성 경계를 설명합니다.
manualId: bluetape4k-cache-hazelcast
chapterId: suspend-jcache-async-boundaries
---

# Suspend JCache와 async 경계

## ICache를 unwrap할 수 있으면 async API를 쓴다

`HazelcastSuspendJCache`는 전달받은 JCache를 `ICache`로 unwrap해 둡니다. 성공하면 `getAsync`, `putAsync`, `removeAsync`, `replaceAsync`를 coroutine `await`로 기다립니다. unwrap할 수 없으면 같은 표준 JCache 호출을 `Dispatchers.IO`에서 실행합니다.

```kotlin
val cache = HazelcastCaches.suspendJCache<String, User>(hazelcast, "users-v1")

cache.put("42", user)       // ICache.putAsync(...).await()
val loaded = cache.get("42") // ICache.getAsync(...).await()
```

API가 `suspend`라고 해서 모든 작업이 Hazelcast native async 명령인 것은 아닙니다. `containsKey`, `clear`, `putAll`, key-set `getAll`, listener 등록 같은 표준 JCache 경로는 blocking 호출이거나 직접 호출입니다.

## bulk 경로는 명령마다 다르다

`putAll`은 표준 JCache bulk 호출을 IO dispatcher에서 한 번 실행합니다. 반면 `putAllFlow`는 `ICache`가 있으면 각 entry의 `putAsync`를 모아 `joinAll`로 기다립니다. 매우 큰 Flow를 그대로 넘기면 완료 전까지 deferred 목록도 커집니다.

```kotlin
cache.putAllFlow(
    users.asFlow().map { it.id to it }
)
```

입력량을 batch로 나누고 cluster operation queue와 coroutine 취소 시점을 함께 시험합니다.

## getAndPut은 단일 Hazelcast 연산이 아니다

1.12.1의 `getAndPut`은 `get(key).also { put(key, value) }`로 구현됩니다. 다른 node가 두 호출 사이에 값을 바꾸면 반환한 이전 값과 실제 교체 대상이 달라질 수 있습니다.

```kotlin
val previous = cache.getAndPut("42", replacement)
```

원자성이 필요하면 이 wrapper의 `getAndPut`을 사용해 해결됐다고 가정하지 말고 Hazelcast의 원자적 server-side operation이나 entry processor를 별도로 검토합니다. `getAndRemoveAsync`와 `getAndReplaceAsync`는 `ICache`가 지원될 때 native async 복합 연산을 사용합니다.

## close와 취소

`close`는 JCache proxy를 IO dispatcher에서 닫습니다. module은 `HazelcastInstance`를 종료하지 않습니다. release test는 공통 `AbstractSuspendJCacheTest` 계약을 실행하지만 모든 cluster disconnect·취소 timing을 고정하지는 않습니다. 장시간 bulk 작업 중 취소와 client reconnect는 애플리케이션 통합 테스트로 보완합니다.

## Source와 tests

- [`HazelcastSuspendJCache.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/jcache/HazelcastSuspendJCache.kt)
- [`HazelcastSuspendJCacheTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/jcache/HazelcastSuspendJCacheTest.kt)
- [`HazelcastCachesTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastCachesTest.kt)
