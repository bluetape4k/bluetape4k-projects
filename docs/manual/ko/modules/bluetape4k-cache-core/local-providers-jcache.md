---
title: 로컬 provider와 JCache
description: Caffeine, Cache2k, Ehcache helper와 JCache manager, expiry, coroutine API의 실제 기본값을 비교합니다.
manualId: bluetape4k-cache-core
chapterId: local-providers-jcache
---

# 로컬 provider와 JCache

## 가장 작은 provider부터 고르기

Caffeine은 heap 안의 작은 캐시와 loading cache에 적합합니다. `caffeine {}`은 builder를 감쌀 뿐이라 최대 크기, 만료와 통계는 애플리케이션이 직접 정합니다.

```kotlin
val cache = caffeine {
    maximumSize(5_000)
    expireAfterAccess(Duration.ofMinutes(5))
    recordStats()
}.build<String, Product>()
```

Cache2k helper는 typed builder와 default manager를 제공하고, Ehcache helper는 heap·off-heap·disk resource pool 구성을 연결합니다. 1.12.1 build에서 두 provider는 `compileOnly`이므로 사용하는 애플리케이션이 runtime dependency를 제공해야 합니다.

## JCache manager와 이름

`JCaching.Caffeine`, `JCaching.Cache2k`, `JCaching.EhCache`는 provider별 `CacheManager`를 lazy하게 만들고 같은 이름의 cache를 가져오거나 생성합니다. `getOrCreate`는 내부 lock으로 같은 manager 안의 중복 생성을 막습니다.

```kotlin
val cache = JCaching.Caffeine.getOrCreate<String, User>(
    "users-by-id",
    jcacheConfiguration {
        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES))
        isStatisticsEnabled = true
    },
)
```

cache 이름은 manager 범위의 identity입니다. 서로 다른 value type에 같은 이름을 재사용하면 typed `getCache`가 실패할 수 있으므로 테스트와 애플리케이션에서 이름 규칙을 고정합니다.

## Eternal은 무제한 용량이 아니다

`getDefaultJCacheConfiguration()`은 `EternalExpiryPolicy`를 사용합니다. 시간 때문에 entry가 사라지지 않는다는 뜻이지 capacity가 무한하다는 뜻은 아닙니다. 실제 eviction은 provider의 용량 정책이 결정합니다.

`NearJCacheConfig`의 front cache는 이 기본값과 다르게 접근 후 30분 만료를 사용합니다. 같은 모듈의 API라도 생성 경로에 따라 expiry가 달라지므로 configuration을 조회하거나 테스트로 고정합니다.

## Coroutine 경계

`CaffeineSuspendJCache`는 Caffeine `AsyncCache`의 future를 `await()`하고 CRUD를 suspend API로 제공합니다.

```kotlin
val cache = CaffeineSuspendJCache<String, User> {
    maximumSize(1_000)
    expireAfterWrite(Duration.ofMinutes(10))
}

cache.put("42", user)
check(cache.get("42") == user)
cache.close()
```

`close()`는 한 번만 실행되며 entry invalidation과 cleanup을 시도합니다. local Caffeine 구현에서 JCache entry listener 등록은 실제 동작이 아니라 debug log만 남깁니다. listener 기반 invalidation이 필요하다면 해당 기능을 제공하는 remote provider를 선택합니다.

## Source와 tests

- [`CaffeineSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/caffeine/CaffeineSupport.kt)
- [`Cache2kSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/cache2k/Cache2kSupport.kt)
- [`EhcacheSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/ehcache/EhcacheSupport.kt)
- [`JCacheSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt)
- [`CaffeineSuspendJCacheTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/CaffeineSuspendJCacheTest.kt)
