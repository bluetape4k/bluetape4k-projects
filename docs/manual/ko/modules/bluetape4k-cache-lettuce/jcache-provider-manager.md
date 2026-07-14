---
title: JCache provider, manager와 설정
description: JCache SPI 탐색, manager identity, Redis hash 저장 구조와 TTL·codec·소유권을 설명합니다.
manualId: bluetape4k-cache-lettuce
chapterId: jcache-provider-manager
---

# JCache provider, manager와 설정

## SPI로 provider 찾기

`LettuceCachingProvider`는 `META-INF/services/javax.cache.spi.CachingProvider`에 등록됩니다. JCache `Caching` API로 provider를 찾을 수 있고, provider는 `(ClassLoader, URI)` 조합마다 하나의 `LettuceCacheManager`를 재사용합니다.

```kotlin
val provider = Caching.getCachingProvider(
    "io.bluetape4k.cache.jcache.LettuceCachingProvider"
)
val manager = provider.getCacheManager(
    URI("redis://redis.example:6379/2"),
    Thread.currentThread().contextClassLoader,
)
```

URI를 생략하면 provider의 논리 URI `lettuce-jcache-default`를 쓰고 실제 Redis 주소는 `redis://localhost:6379`입니다. 같은 URI라도 ClassLoader가 다르면 manager도 다릅니다. plugin이나 application server 환경에서 cache identity를 URI 하나로만 판단하면 안 됩니다.

## manager와 RedisClient 소유권

provider가 manager를 만들면 내부에서 `RedisClient`도 만들고 manager 종료 시 함께 shutdown합니다. 반대로 이미 관리 중인 client를 `LettuceJCaching.cacheManagerOf(redisClient)`에 넘기면 manager는 client를 닫지 않습니다.

```kotlin
val redisClient = RedisClient.create("redis://localhost:6379")
val cache = LettuceJCaching.getOrCreate<String, User>(
    redisClient = redisClient,
    cacheName = "users-v1",
    ttlSeconds = 600,
)

try {
    cache.put("42", user)
} finally {
    cache.close()          // cache connection만 닫음
    redisClient.shutdown() // 외부 소유자가 client를 닫음
}
```

`LettuceJCaching`은 RedisClient별 manager를 내부 map에 보관합니다. manager를 닫은 뒤 같은 client로 다시 `cacheManagerOf`를 호출하면 닫힌 manager가 남을 수 있으므로 애플리케이션 수명주기에서 manager와 client를 함께 한 번만 닫는 구조가 안전합니다.

## Redis hash 저장 구조

JCache 하나는 cache 이름과 같은 Redis hash 하나를 사용합니다. JCache key는 hash field 문자열로, value는 선택한 `LettuceBinaryCodec`의 byte array로 저장됩니다.

```text
Redis hash key: users-v1
  field "42" -> <lz4+fory bytes>
  field "84" -> <lz4+fory bytes>
```

기본 key encoder는 `key.toString()`입니다. String이 아닌 key를 순회하려면 문자열 field를 원래 key로 되돌릴 `keyDecoder`가 필요합니다.

```kotlin
val config = lettuceCacheConfigOf<Int, User>(
    ttlSeconds = 600,
    keyCodec = Int::toString,
    keyDecoder = String::toInt,
)
val users = manager.createCache("users-by-number", config)
```

`get`, `put`만 쓸 때는 decoder 없이도 동작할 수 있지만 iterator가 field를 key로 복원할 때 `CacheException`이 발생합니다. 사용 경로가 바뀌어도 안전하도록 encoder와 decoder를 한 쌍으로 둡니다.

## TTL은 hash 전체에 적용된다

`LettuceCacheConfig.ttlSeconds`는 entry별 만료가 아닙니다. write가 성공할 때 Redis hash key의 TTL을 다시 설정하므로 같은 cache 이름의 모든 entry가 함께 만료됩니다.

- Redis 8+에서는 capability 확인 후 `HSETEX` 경로를 사용합니다.
- Redis 7 이하에서는 `HSET`/`HMSET` 뒤 `EXPIRE`로 fallback합니다.
- `ttlSeconds=null`이면 만료를 설정하지 않습니다.
- write가 이어지면 hash 전체의 만료 시점도 계속 뒤로 이동합니다.

서로 다른 만료 시간을 가진 entry가 필요하면 cache 이름을 분리하거나 Near Cache의 per-key Redis 저장 방식을 선택합니다.

## manager가 검증하는 계약

`createCache`는 공백 이름과 같은 이름의 중복 생성을 거부합니다. typed `getCache(name, keyType, valueType)`은 설정에 기록된 타입과 요청 타입을 비교하고 다르면 `ClassCastException`을 던집니다. manager가 닫힌 뒤에는 생성과 조회, 삭제가 `IllegalStateException`으로 실패합니다.

`destroyCache`는 Redis hash를 비운 뒤 cache connection을 닫습니다. `cache.close()`는 connection과 registry만 정리하고 Redis 데이터는 보존합니다. 배포 종료와 데이터 폐기를 같은 동작으로 묶지 않습니다.

## Source와 tests

- [`LettuceCachingProvider.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCachingProvider.kt)
- [`LettuceCacheManager.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheManager.kt)
- [`LettuceCacheConfig.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceCacheConfig.kt)
- [`LettuceCachingProviderTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceCachingProviderTest.kt)
- [`LettuceJCacheManagerTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceJCacheManagerTest.kt)
