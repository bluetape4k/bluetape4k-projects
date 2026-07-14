---
title: RESP3 무효화와 Lua CAS
description: CLIENT TRACKING의 key 등록과 push 처리, NOLOOP 의미, Lua EVALSHA와 NOSCRIPT fallback을 설명합니다.
manualId: bluetape4k-cache-lettuce
chapterId: resp3-invalidation-lua
---

# RESP3 무효화와 Lua CAS

## RESP3 client가 필요한 이유

Redis CLIENT TRACKING은 이 connection이 읽은 key가 다른 connection에서 바뀌면 invalidation push를 보냅니다. Lettuce가 push message를 받으려면 client protocol을 RESP3로 구성합니다.

```kotlin
val redisClient = RedisClient.create("redis://localhost:6379").also {
    it.options = ClientOptions.builder()
        .protocolVersion(ProtocolVersion.RESP3)
        .build()
}
```

`useRespProtocol3=true`는 Near Cache가 `CLIENT TRACKING ON NOLOOP`를 요청한다는 뜻입니다. 이 옵션이 RedisClient 자체의 protocol을 RESP3로 바꾸지는 않습니다.

## 읽은 key를 tracking에 등록하기

L1 miss 뒤 Redis `GET`을 수행하면 해당 key가 tracking table에 등록됩니다. `put`, `putAll`, 성공한 `putIfAbsent`와 `replace`는 Redis에 쓴 뒤 동기 `GET` 또는 `MGET`을 한 번 더 수행합니다.

이 추가 read는 값 확인보다 순서 보장이 목적입니다. fire-and-forget async GET을 쓰면 외부 update가 tracking GET보다 Redis에 먼저 도착해 invalidation을 놓칠 수 있습니다. 동기 command가 반환된 뒤에야 write API가 끝나도록 해 그 경쟁 구간을 닫습니다.

## NOLOOP와 자기 쓰기

`NOLOOP`는 같은 connection이 쓴 key에 invalidation push를 보내지 않습니다. Near Cache는 자기 write 성공 뒤 L1을 직접 갱신하므로 자기 push가 필요 없습니다. 다른 Near Cache instance나 외부 Redis connection이 같은 prefixed key를 바꾸면 push를 받고 L1을 비웁니다.

```text
Cache A GET users:42 -> tracking 등록, L1 fill
Cache B SET users:42 -> Redis push to A
Cache A listener -> L1 invalidate("42")
Cache A next GET -> Redis의 새 값으로 fill
```

invalidation은 비동기로 도착합니다. Cache B의 write가 반환된 순간 Cache A의 L1이 이미 비워졌다고 가정하지 않습니다.

## push payload 처리

`TrackingInvalidationListener`는 `invalidate` message의 key payload를 `ByteBuffer`, `ByteArray`, String과 list 형태로 처리합니다. `${cacheName}:` prefix가 맞는 key만 잘라 L1에서 제거하고 다른 cache 이름은 무시합니다.

key list가 `null`이면 Redis의 full invalidation 신호로 보고 해당 L1 전체를 비웁니다. decode할 수 없는 payload는 경고를 남기고 건너뜁니다. 이 경로는 `TrackingInvalidationListenerPayloadTest`에서 mixed payload, single key와 full flush로 검증됩니다.

## tracking 실패는 fail-open

Near Cache 생성 중 `trackingListener.start()`가 실패하면 경고를 남기고 cache 생성은 계속됩니다. Redis read/write와 L1 fill은 동작하지만 다른 connection의 update가 L1에 전파되지 않습니다.

health check에서 Redis ping만 확인하면 부족합니다. RESP3 protocol, `CLIENT TRACKING` 권한과 cross-instance invalidation 시나리오를 배포 환경에서 확인해야 합니다. tracking을 사용할 수 없다면 L1 TTL을 짧게 두거나 `useRespProtocol3=false`로 의도를 명시하고 별도 invalidation을 설계합니다.

## Lua compare-and-set

`replace(key, oldValue, newValue)`는 Redis에서 비교와 교체를 원자적으로 수행하는 Lua script를 사용합니다.

```lua
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
    redis.call('SET', KEYS[1], ARGV[2], 'XX', 'KEEPTTL')
    return 1
end
return 0
```

`KEEPTTL`은 성공한 교체가 기존 Redis TTL을 초기화하지 않게 합니다. client는 미리 계산한 SHA1으로 `EVALSHA`를 먼저 보내고 Redis가 `NOSCRIPT`를 반환할 때만 원문을 `EVAL`합니다. script cache flush나 failover 뒤에도 같은 연산을 복구할 수 있습니다.

fallback 대상은 `RedisNoScriptException`뿐입니다. 연결 실패, timeout, codec 오류를 raw `EVAL` 재시도로 가리지 않습니다.

## 검증 시나리오

- 같은 cache 이름의 두 instance에서 외부 update와 remove
- 다른 cache 이름의 동일 logical key가 무효화되지 않는지
- `putAll`, `putIfAbsent`, 두 `replace` overload 뒤 tracking 등록
- full flush와 mixed payload decode
- Redis script cache가 비워진 뒤 CAS fallback과 TTL 보존
- tracking start 실패 환경의 stale 허용 범위

## Source와 tests

- [`TrackingInvalidationListener.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/TrackingInvalidationListener.kt)
- [`NearCacheScripts.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheScripts.kt)
- [`LettuceNearCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt)
- [`LettuceNearCacheTrackingTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheTrackingTest.kt)
- [`TrackingInvalidationListenerPayloadTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/TrackingInvalidationListenerPayloadTest.kt)
