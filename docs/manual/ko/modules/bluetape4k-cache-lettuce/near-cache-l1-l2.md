---
title: Near Cache L1·L2 동작
description: Caffeine L1과 Redis L2의 read fill, write 순서, key 격리, TTL, bulk 연산과 통계를 설명합니다.
manualId: bluetape4k-cache-lettuce
chapterId: near-cache-l1-l2
---

# Near Cache L1·L2 동작

## 두 계층의 역할

`LettuceNearCache`와 `LettuceSuspendNearCache`는 Caffeine을 L1, Redis를 L2로 사용합니다. key type은 항상 `String`이고 Redis에는 `${cacheName}:${key}` 형태로 저장합니다.

```text
get("42")
  -> Caffeine L1 hit: 바로 반환
  -> L1 miss: GET users:42
       -> Redis hit: L1에 채우고 반환
       -> Redis miss: null
```

cache 이름 prefix 덕분에 같은 Redis database에서 `users:42`와 `sessions:42`는 독립적입니다. `clearAll()`도 `cacheName:*`만 찾아 지우며 `FLUSHDB`를 호출하지 않습니다.

## 설정과 기본값

```kotlin
val config = lettuceNearCacheConfig<String, User> {
    cacheName = "users"
    maxLocalSize = 10_000
    frontExpireAfterWrite = Duration.ofMinutes(10)
    frontExpireAfterAccess = null
    redisTtl = Duration.ofHours(1)
    useRespProtocol3 = true
    recordStats = true
}
```

L1 기본 만료는 쓰기 후 30분이고 최대 10,000개입니다. L2 TTL 기본값은 없으며, 지정하면 각 `${cacheName}:${key}` Redis key에 PX TTL을 설정합니다. 이는 JCache의 hash 전체 TTL과 다른 계약입니다.

L1 TTL을 L2보다 길게 두면 Redis에서 이미 사라진 값을 L1이 더 오래 반환할 수 있습니다. 일반적으로 L1 만료를 더 짧게 두고, 원본 데이터 변경 시 RESP3 invalidation도 사용합니다.

## write 순서

`put`은 Redis `SET`이 성공한 뒤 L1을 갱신하고 tracking용 `GET`을 등록합니다. Redis write가 실패하면 새 값이 L1에만 남지 않습니다.

```kotlin
users.put("42", user)              // Redis -> L1
users.replace("42", updated)      // Redis에 있을 때만 교체
users.remove("42")                 // L1 제거 -> Redis UNLINK
```

이 동작은 L1/L2 사이의 write-through입니다. database repository를 호출하지 않으므로 애플리케이션의 원본 데이터까지 쓰는 write-through는 아닙니다.

`remove`와 `removeAll`은 Redis 메인 thread의 큰 값 해제 부담을 줄이려고 `DEL` 대신 `UNLINK`를 사용합니다. Redis에서 실제 메모리 해제가 끝나는 시점과 command 성공 시점은 다를 수 있습니다.

## 조건부 쓰기

`putIfAbsent`는 먼저 `get`으로 기존 값을 확인한 뒤 `SET NX`를 시도합니다. 다른 client가 먼저 저장하면 그 값을 Redis에서 읽어 반환합니다.

`replace(key, value)`는 존재 확인과 `SET XX`가 별도 command입니다. `replace(key, oldValue, newValue)`만 Lua script로 비교와 교체를 한 번에 수행합니다. 원자성 요구에 맞는 overload를 선택합니다.

## bulk와 clear

`getAll`은 L1 hit를 먼저 모으고 miss만 async Redis GET으로 발행한 뒤 `flushCommands()`합니다. 결과가 있는 key만 L1에 채웁니다.

`putAll`은 TTL이 없으면 `MSET`, TTL이 있으면 Redis transaction block에서 key별 `SET PX`를 수행합니다. 두 경로 모두 Redis 성공 뒤 L1을 갱신합니다.

`clearLocal()`은 L1만 비워 다음 조회가 Redis로 가게 합니다. `clearAll()`은 L1을 비운 뒤 `SCAN MATCH cacheName:* COUNT 100`과 `UNLINK`로 L2도 지웁니다. 이 작업은 전체 key 수에 비례하므로 request hot path에서 호출하지 않습니다.

## 통계 해석

```kotlin
val snapshot = users.stats()
println(snapshot.localHits)
println(snapshot.localMisses)
println(snapshot.backHits)
println(snapshot.backMisses)
```

`recordStats=true`일 때만 Caffeine local hit·miss·eviction이 유효합니다. Redis hit·miss counter는 L1 miss 뒤 단일 `get` 경로에서 갱신되며 1.12.1의 `getAll` miss 결과는 같은 counter에 반영되지 않습니다. 통계를 전체 요청 수와 정확히 같다고 가정하지 말고 추세와 별도 Redis metric을 함께 봅니다.

## Source와 tests

- [`LettuceNearCacheConfig.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheConfig.kt)
- [`LettuceCaffeineLocalCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceCaffeineLocalCache.kt)
- [`LettuceNearCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt)
- [`LettuceSuspendNearCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceSuspendNearCache.kt)
- [`LettuceNearCacheIsolationTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheIsolationTest.kt)
