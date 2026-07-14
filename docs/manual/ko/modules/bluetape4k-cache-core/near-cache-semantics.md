---
title: Near Cache의 front·back 동작
description: Local front와 remote back cache의 읽기, 채우기, 쓰기, 무효화와 통계 경계를 따라갑니다.
manualId: bluetape4k-cache-core
chapterId: near-cache-semantics
---

# Near Cache의 front·back 동작

## 두 계층의 역할

Near Cache는 자주 읽는 값을 JVM 안의 front cache에 두고, 여러 process가 공유할 값은 back cache에 둡니다. 공통 `NearCacheOperations<V>`와 suspend 대응 인터페이스는 `String` key로 읽기·쓰기·삭제·관리·통계를 통일합니다.

```text
get(key)
  ├─ front hit  ───────────────> return
  └─ front miss ─> back lookup ─> front fill ─> return
```

이 흐름은 source data를 읽는 cache-aside가 아니라 이미 cache에 있는 두 계층 사이의 read path입니다. back에도 값이 없으면 `null`이고 database 조회는 caller가 맡습니다.

## `clearLocal`과 `clearAll`

`clearLocal()`은 현재 process의 front만 비웁니다. 다음 `get`은 back에서 다시 읽어 front를 채울 수 있습니다. `clearAll()`은 front와 back을 모두 비웁니다.

legacy `NearJCache.clear()`도 front만 비우고 `clearAllCache()`가 두 계층을 비웁니다. 하지만 `clear()`는 공유 back cache를 사용하는 다른 Near Cache에 event를 전파하지 않습니다. 다른 process의 local entry까지 없애야 한다면 provider가 보장하는 `removeAll` 또는 invalidation channel을 사용합니다.

## 쓰기는 persistence write-through가 아니다

공통 interface의 `put`은 provider 구현이 local과 back cache를 같은 연산 경로에서 갱신하도록 요구합니다. 이는 cache tier 간 동기화입니다. database, JDBC repository, Exposed table은 이 interface에 등장하지 않으므로 persistence write-through가 아닙니다.

front를 먼저 바꾸는 구현에서 back write가 실패하면 local 값만 남을 수 있습니다. 반대로 back을 먼저 쓰는 구현에서는 remote 성공 뒤 local fill이 실패할 수 있습니다. 실제 순서와 보상은 Lettuce·Redisson provider source와 failure test에서 확인합니다.

## Listener 기반 무효화

`NearJCache`는 back cache entry listener를 등록해 변경 event를 front에 반영합니다. `SuspendNearJCache.withoutListener`는 listener를 cluster에 직렬화할 수 없는 환경을 위한 degraded 경로입니다. 이 모드에서는 다른 process의 변경을 자동으로 반영한다고 가정하면 안 됩니다.

provider마다 event 보장이 다릅니다. 1.11.0 source는 Redisson bulk operation이 entry event를 내지 않는 경우를 고려해 key별 remove 경로를 사용합니다.

## 통계를 읽는 법

`NearCacheStatistics`는 local hit·miss·size·eviction과 back hit·miss를 나눕니다. 전체 hit rate만 보면 local cache가 실제로 네트워크 왕복을 줄였는지 알기 어렵습니다.

- local hit가 낮고 back hit가 높으면 capacity·expiry·invalidation 빈도를 확인합니다.
- local·back miss가 함께 급증하면 cache-aside loader와 원본 저장소 부하를 확인합니다.
- eviction과 load latency가 함께 오르면 hot set보다 capacity가 작은지 측정합니다.

## Source와 tests

- [`NearCacheOperations.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/NearCacheOperations.kt)
- [`SuspendNearCacheOperations.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/SuspendNearCacheOperations.kt)
- [`NearJCache.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/NearJCache.kt)
- [`SuspendNearJCache.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/nearcache/jcache/SuspendNearJCache.kt)
- [`AbstractNearCacheOperationsTest.kt`](../../../../../cache/cache-core/src/testFixtures/kotlin/io/bluetape4k/cache/nearcache/AbstractNearCacheOperationsTest.kt)
