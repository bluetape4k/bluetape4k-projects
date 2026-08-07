---
title: Redisson JCache와 suspend wrapper
description: JCache provider 생성, cache 재사용, coroutine bridge와 원자성 경계를 설명합니다.
manualId: bluetape4k-cache-redisson
chapterId: redisson-jcache-suspend
---

# Redisson JCache와 suspend wrapper

## 두 진입점

`RedissonJCaching`은 Redisson의 `JCachingProvider`를 사용해 표준 `javax.cache.Cache`를 만들고, `RedissonSuspendJCache`는 같은 cache를 `SuspendJCache`로 감쌉니다. `RedissonCaches` factory를 쓰면 두 경로를 한 곳에서 선택할 수 있습니다.

```kotlin
val cache = RedissonCaches.suspendJCache<String, User>(
    redisson = redissonClient,
    cacheName = "users",
    configuration = MutableConfiguration<String, User>().apply {
        setTypes(String::class.java, User::class.java)
    },
)

cache.put("u:1", User("u:1", "debop"))
val user = cache.get("u:1")
```

`RedissonClient`를 넘기면 애플리케이션이 client를 계속 소유합니다. `Config` overload는 provider가 client 생성에 관여하므로 manager와 client 종료 정책을 애플리케이션 lifecycle 문서에 남깁니다.

## 같은 이름 cache 재사용

`RedissonJCaching.cacheManager`는 lazy singleton입니다. `getOrCreate`는 같은 이름 cache가 있으면 재사용합니다. 따라서 기존 cache의 key/value type이나 configuration과 새 호출이 어긋나면 새 설정이 적용된다고 기대하면 안 됩니다.

테스트는 고유 cache 이름을 사용하고 종료할 때 clear 또는 close합니다. 서비스에서는 cache 이름을 데이터 계약으로 취급하고 환경·tenant 경계를 이름에 반영합니다.

## async API를 기다리는 방법

CRUD 대부분은 Redisson JCache의 `*Async()`를 호출하고 `await()`합니다. `putAllFlow`는 각 put을 deferred로 바꾼 뒤 `joinAll()`로 모두 끝날 때까지 기다립니다. `entries()`는 JCache iterator를 순회하므로 대량 scan을 가벼운 non-blocking stream으로 오해하지 않습니다.

`removeAll()`은 async overload 대신 `Dispatchers.IO`에서 blocking method를 호출합니다. API가 suspend라는 사실만으로 provider의 모든 내부 작업이 non-blocking이라는 뜻은 아닙니다.

## `getAndPut`은 원자적이지 않다

1.12.1의 `getAndPut`은 Redisson JCache가 `getAndPutAsync()`를 제공하지 않아 `get(key)` 다음 `put(key, value)`를 실행합니다.

```kotlin
val previous = cache.getAndPut("u:1", updated)
```

두 호출 사이에 다른 writer가 값을 바꾸면 `previous`와 최종 값이 하나의 원자 연산 결과가 아닐 수 있습니다. compare-and-set이나 엄격한 read-modify-write가 필요하면 Redisson `RMap`의 원자 API를 직접 사용합니다.

## close와 데이터

`close()`는 감싼 JCache를 닫지만 저장된 entry를 삭제하지 않습니다. entry 정리는 `clear()` 또는 `removeAll()`로 명시합니다. close 중 일반 예외는 warning으로 남기고, `CancellationException`은 호출자에게 다시 던집니다.

## Source와 tests

- [`RedissonJCaching.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonJCaching.kt)
- [`RedissonSuspendJCache.kt`](../../../../../cache/cache-redisson/src/main/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCache.kt)
- [`RedissonSuspendJCacheTest.kt`](../../../../../cache/cache-redisson/src/test/kotlin/io/bluetape4k/cache/jcache/RedissonSuspendJCacheTest.kt)
