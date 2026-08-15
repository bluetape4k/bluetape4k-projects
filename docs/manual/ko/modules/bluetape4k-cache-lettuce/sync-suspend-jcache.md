---
title: 동기·suspend JCache
description: Lettuce JCache의 CRUD, listener, EntryProcessor, coroutine adapter와 close·destroy 경계를 다룹니다.
manualId: bluetape4k-cache-lettuce
chapterId: sync-suspend-jcache
---

# 동기·suspend JCache

## factory로 시작하기

`LettuceCaches`는 가장 자주 쓰는 JCache 생성 경로를 모읍니다. 동기 API는 `JCache<K, V>`, coroutine API는 String key를 쓰는 `LettuceSuspendJCache<V>`를 반환합니다.

```kotlin
val sessions = LettuceCaches.jcache<String, Session>(
    redisClient,
    cacheName = "sessions-v1",
    ttlSeconds = 1_800,
)

val suspended = LettuceCaches.suspendJCache<Session>(
    redisClient,
    cacheName = "sessions-suspend-v1",
    ttlSeconds = 1_800,
)
```

두 경로 모두 Redis hash 기반 `LettuceJCache`를 사용합니다. suspend wrapper는 별도 non-blocking Redis 구현이 아니라 동기 JCache 호출을 `Dispatchers.IO`에서 실행합니다.

## CRUD와 batch

`getAll`은 key를 100개씩 나누고 hash field를 batch 조회합니다. `putAll`은 listener가 있을 때만 기존 field를 한 번 읽어 CREATED와 UPDATED를 구분한 뒤 batch write합니다.

```kotlin
sessions.putAll(mapOf("a" to sessionA, "b" to sessionB))
val found = sessions.getAll(setOf("a", "b", "missing"))

check(found.keys == setOf("a", "b"))
check(sessions.putIfAbsent("a", another) == false)
```

`replace(key, old, new)`와 `remove(key, old)`는 읽고 비교한 뒤 별도 Redis 명령을 수행합니다. 여러 client가 같은 key를 동시에 바꾸는 상황에서 원자적 compare-and-set을 보장하지 않습니다. 원자적 조건 변경은 Near Cache의 Lua CAS나 별도 Redis script를 사용합니다.

## 동기 NearCache write-through

`isSynchronous=true`로 설정한 `NearJCache`의 `put`, `putAll`, `putIfAbsent`, `remove`,
`replace`는 500ms 이상으로 보정된 `syncRemoteTimeout` 안에서 Lettuce back write가
끝나기를 기다립니다. Lettuce는 write worker 또는 synchronous callback thread에서 해당
write의 listener를 호출할 수 있습니다. `NearJCache`는 operation-scoped key/type/value
상관관계가 맞는 self-event를 caller가 잡은 mutation gate를 다시 획득하지 않고 front에
직접 반영하므로 write가 자기 listener를 기다리는 교착을
막습니다. 매칭되지 않는 다른 wrapper나 외부 write의 event는 계속 mutation gate를 획득하며, 비동기
write-through도 기존 gate 경로를 유지합니다. JCache event에는 operation ID가 없으므로 동일 key/type/value의 외부 event는
활성 self-event와 구분할 수 없습니다. provider가 interrupt를 무시하면 호출자가
timeout을 관찰한 뒤 back completion이 늦게 도착할 수 있지만, back-write barrier가
후속 write와 순서를 보존합니다.

## listener와 EntryProcessor

cache entry listener는 이 `LettuceJCache` instance가 수행한 CREATED·UPDATED·REMOVED 이벤트를 process 안에서 호출합니다. Redis의 다른 client가 값을 바꿔도 JCache listener가 자동으로 알림을 받는 구조는 아닙니다.

1.12.1의 `invoke`와 `invokeAll`은 `MutableEntry`를 구현합니다. processor가 값을 읽고 `setValue` 또는 `remove`를 요청하면 처리 뒤 JCache write로 commit합니다.

```kotlin
val updated = sessions.invoke("a", EntryProcessor<String, Session, Int> { entry, _ ->
    val next = entry.value!!.copy(refreshCount = entry.value!!.refreshCount + 1)
    entry.setValue(next)
    next.refreshCount
})
```

이 과정도 Redis transaction이나 Lua script 하나로 묶이지 않습니다. 같은 key에 대한 외부 동시 변경과 충돌할 수 있으므로 분산 원자 연산으로 해석하지 않습니다.

## suspend API의 실행 경계

`LettuceSuspendJCache`는 `get`, `put`, `remove`, `replace`, `close`를 `Dispatchers.IO`에서 호출합니다. `getAll(keys)`와 `entries()`는 `Flow`를 반환하지만 내부 source는 동기 JCache 조회입니다.

```kotlin
coroutineScope {
    suspended.put("a", sessionA)
    suspended.getAll(setOf("a", "b")).collect { entry ->
        audit(entry.key, entry.value)
    }
}
```

`putAllFlow`는 입력 Flow를 하나씩 수집해 `put`합니다. 대량 입력을 Redis 한 번의 batch로 보내는 API가 아니므로 throughput이 중요하면 map을 모아 `putAll`을 사용합니다.

## manager와 종료

`LettuceSuspendCacheManager`는 이름별 wrapper를 재사용하고 기본 TTL·codec을 적용합니다. `closeCache`는 registry에서 wrapper를 제거하지만 Redis hash는 남깁니다. 새 wrapper를 열면 같은 데이터를 다시 읽을 수 있습니다.

```kotlin
val manager = LettuceSuspendCacheManager(redisClient, defaultTtlSeconds = 600)
val cache = manager.getOrCreate<User>("users-v1")

cache.put("42", user)
manager.closeCache(cache)
check(manager.getOrCreate<User>("users-v1").get("42") == user)
```

데이터까지 삭제하려면 `destroyCache` 또는 `clear`를 명시적으로 호출합니다. manager의 suspend `close()`는 등록 cache를 모두 닫고, cache close 중 `CancellationException`이 발생해도 나머지 정리를 시도한 뒤 취소를 다시 전파합니다.

## Source와 tests

- [`LettuceJCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceJCache.kt)
- [`LettuceSuspendJCache.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCache.kt)
- [`LettuceSuspendCacheManager.kt`](../../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendCacheManager.kt)
- [`LettuceJCacheTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceJCacheTest.kt)
- [`LettuceSuspendJCacheManagerTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/jcache/LettuceSuspendJCacheManagerTest.kt)
