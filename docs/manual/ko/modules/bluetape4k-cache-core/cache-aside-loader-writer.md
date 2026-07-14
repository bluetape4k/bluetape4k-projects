---
title: Cache-aside와 loader·writer 계약
description: Caller가 miss를 채우는 cache-aside와 JCache loader·writer가 맡는 read-through, write-through를 구분합니다.
manualId: bluetape4k-cache-core
chapterId: cache-aside-loader-writer
---

# Cache-aside와 loader·writer 계약

## Cache-aside는 caller의 책임이다

cache-aside에서는 cache miss를 본 service나 repository가 원본을 읽고 cache에 저장합니다.

```kotlin
fun findProduct(id: Long): Product {
    productCache.get(id)?.let { return it }
    return productRepository.find(id).also { productCache.put(id, it) }
}
```

첫 요청은 DB에서 읽고 다음 요청부터 cache를 사용합니다. cache API는 원본 저장소를 모르므로 쓰기 transaction 뒤 갱신·무효화 순서를 caller가 책임집니다.

## `getOrPut`의 동시 miss

JCache extension `getOrPut`은 먼저 `get`, miss이면 supplier를 실행한 뒤 `putIfAbsent`를 호출합니다. 여러 thread가 동시에 miss를 보면 supplier가 여러 번 실행될 수 있지만 최종 반환값은 cache에 먼저 들어간 값으로 맞춥니다.

```kotlin
val product = cache.getOrPut(id) { productRepository.find(id) }
```

이 helper는 간단한 cache-aside에는 편리하지만 중복 DB 조회를 막는 stampede 방지 장치는 아닙니다. 같은 key의 평가를 한 번으로 합쳐야 한다면 memoizer나 provider loading cache를 사용합니다.

## JCache read-through

JCache read-through는 `CacheLoader` factory와 `isReadThrough=true`를 configuration에 등록할 때 성립합니다. `cache-core`의 `cacheLoader()` extension은 다른 cache를 읽는 adapter일 뿐입니다. database loader를 원한다면 repository 호출을 구현한 `CacheLoader`를 직접 제공합니다.

```kotlin
val config = jcacheConfigurationOf<Long, Product>(
    cacheLoaderFactory = FactoryBuilder.factoryOf(ProductLoader(productRepository)),
    isReadThrough = true,
)
```

loader가 miss 경로를 소유하므로 caller는 `cache.get(id)`만 호출합니다. provider가 loader exception을 어떻게 감싸는지 테스트하고, null을 지원하지 않는 provider 계약도 확인합니다.

## Write-through의 정확한 범위

JCache write-through는 `CacheWriter` factory와 `isWriteThrough=true`가 설정되어 cache 변경이 backing store 변경을 호출할 때만 사용합니다. `cache.cacheWriter()` helper는 **그 cache 자체에** `put`과 `remove`를 위임하는 adapter입니다. database writer가 아니므로 이것만 연결해서 persistence write-through라고 설명하면 안 됩니다.

Near Cache의 `put`도 local front와 remote back cache를 함께 갱신한다는 의미에서 두 cache 사이의 동기 쓰기입니다. database나 Exposed table까지 저장하지 않습니다.

## 원본 저장 실패와 cache 실패

원본을 먼저 저장한 뒤 cache invalidation이 실패하면 stale entry가 남을 수 있습니다. cache를 먼저 바꾸고 원본 저장이 실패하면 존재하지 않는 값이 노출될 수 있습니다. transaction 안에서 두 시스템을 원자적으로 묶지 않는다면 다음 중 하나를 명시적으로 선택합니다.

- 원본 저장 뒤 cache를 제거하고, 실패 시 짧은 TTL로 stale window를 제한합니다.
- outbox/event로 무효화를 재시도하고 처리 지연을 측정합니다.
- `JdbcCacheRepository`처럼 loader/writer와 persistence 경계를 함께 설계한 구현을 사용합니다.

## Source와 tests

- [`JCacheSupport.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/jcache/JCacheSupport.kt)
- [`JCacheReadWriteThroughExample.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheReadWriteThroughExample.kt)
- [`JCacheSupportExtTest.kt`](../../../../../cache/cache-core/src/test/kotlin/io/bluetape4k/cache/jcache/JCacheSupportExtTest.kt)
- [`JCacheMemoizer.kt`](../../../../../cache/cache-core/src/main/kotlin/io/bluetape4k/cache/memoizer/jcache/JCacheMemoizer.kt)
