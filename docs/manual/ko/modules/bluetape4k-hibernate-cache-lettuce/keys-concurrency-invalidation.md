---
title: Key, 동시성 전략과 무효화
description: Hibernate key digest, composite·natural id, cache concurrency strategy와 RESP3 무효화를 설명합니다.
manualId: bluetape4k-hibernate-cache-lettuce
chapterId: keys-concurrency-invalidation
---

# Key, 동시성 전략과 무효화

## `toString()`을 key로 쓰지 않는다

Hibernate key에는 entity·role 이름, tenant, identifier 종류와 값이 들어갑니다. StorageAccess는 이를 canonical byte sequence로 직렬화한 뒤 SHA-256 digest를 만들고 `hck2:` version prefix를 붙입니다.

```text
kind + entityOrRoleName + tenant presence/value + typed identifier
  → SHA-256
  → hck2:<base64url digest>
```

scalar `"[1, 2]"`와 object array `[1, 2]`, 구분자가 들어간 단일 natural-id와 2개 값 natural-id는 문자열 표현이 비슷해도 다른 key가 됩니다. composite id, primitive array와 같은 `toString()`을 반환하는 custom identifier도 테스트합니다.

## 전략을 데이터 계약에 맞춘다

factory 기본값은 `NONSTRICT_READ_WRITE`입니다. update 때 엄격한 distributed soft lock을 조정하기보다 cache entry를 제거하고 다음 read에서 다시 채우는 모델입니다. 짧은 stale window를 허용하는 read-heavy 데이터에 맞습니다.

`READ_WRITE` entity도 테스트되지만, Redis 분산 환경의 soft-lock 비용과 장애 동작을 측정해야 합니다. 삭제 뒤 lock marker가 남을 수 있어 테스트도 명시적으로 `evictEntityData`한 뒤 containment를 확인합니다. `READ_ONLY`는 실제로 변경되지 않는 reference data에만 씁니다.

## RESP3 CLIENT TRACKING

`use_resp3=true`이면 RedisClient를 RESP3로 만들고 Near Cache tracking listener를 시작합니다. L2 key가 다른 connection에서 바뀌면 Redis push를 받아 해당 L1 key를 지우는 것이 목표입니다.

tracking 시작 실패는 예외를 전파하지 않고 경고만 남깁니다.

```text
CLIENT TRACKING start failed, cache will work without invalidation
```

이 상태에서는 TTL이나 Hibernate eviction 전까지 다른 프로세스의 L1 값이 남을 수 있습니다. 시작 로그, 다중 인스턴스 invalidation test와 stale read 지표가 필요합니다. Redis 6 미만에서 `use_resp3=false`로 실행하면 이 교차 프로세스 무효화를 포기한다는 뜻입니다.

## 직접 Redis 쓰기를 피한다

CLIENT TRACKING이 있다고 해서 애플리케이션이 Hibernate Region key를 직접 변경해도 된다는 뜻은 아닙니다. key 형식은 versioned internal contract이고, query timestamps와 transaction completion까지 함께 갱신해야 합니다. 모든 변경은 Hibernate Session과 `SessionFactory.cache.evict*`를 통과시킵니다.

## Source와 tests

- [`LettuceNearCacheStorageAccess.kt`](../../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt)
- [`HibernateAdvancedKeyCacheTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateAdvancedKeyCacheTest.kt)
- [`HibernateReadWriteStrategyTest.kt`](../../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateReadWriteStrategyTest.kt)
- [`LettuceNearCacheTrackingTest.kt`](../../../../../cache/cache-lettuce/src/test/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCacheTrackingTest.kt)
