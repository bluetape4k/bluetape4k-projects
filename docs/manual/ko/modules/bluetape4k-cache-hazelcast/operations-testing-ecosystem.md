---
title: 설정, 장애, 테스트와 생태계
description: L1과 IMap 설정 경계, 장애 뒤 상태, lifecycle 점검, 테스트와 cache backend 선택 기준을 정리합니다.
manualId: bluetape4k-cache-hazelcast
chapterId: operations-testing-ecosystem
---

# 설정, 장애, 테스트와 생태계

## L1과 cluster 설정을 나눈다

`HazelcastNearCacheConfig`는 Caffeine L1만 설정합니다. 이름, 최대 크기, 쓰기 후 만료, 선택적인 접근 후 만료, 통계 기록 여부가 전부입니다. 이름은 공백일 수 없고 size와 duration은 양수여야 합니다.

```kotlin
val config = hazelcastNearCacheConfig {
    cacheName = "catalog-v1"
    maxLocalSize = 20_000
    frontExpireAfterWrite = Duration.ofMinutes(5)
    frontExpireAfterAccess = Duration.ofMinutes(1)
    recordStats = true
}
```

IMap backup, cluster TTL·max-idle, eviction, in-memory format, split-brain protection과 serializer는 Hazelcast 설정에서 관리합니다. L1 만료를 IMap 만료로 착각하면 오래 남은 cluster 값이 다음 miss에서 다시 L1로 들어옵니다.

## 장애 뒤 어느 계층이 남는지 본다

- front-first `put`·`remove`: backend 실패 뒤 L1만 바뀔 수 있습니다.
- `replace`: backend 성공 뒤 L1이 바뀝니다.
- listener 지연·해제: 원격 변경 뒤 오래된 L1이 남을 수 있습니다.
- memoizer miss: 다른 JVM에서도 evaluator가 실행될 수 있습니다.
- `clearAll`: 현재 cache 이름의 공유 IMap 전체에 영향을 줍니다.

resilience decorator를 붙여도 이 순서가 자동으로 database transaction이 되지는 않습니다. retry 횟수보다 먼저 idempotency, stale 허용 시간과 원본 저장소 부하를 정합니다.

## 종료 순서를 명시한다

application shutdown에서는 새 요청을 막고, Near Cache를 닫아 listener를 제거한 뒤, cache manager/proxy를 정리하고, 마지막에 application-owned Hazelcast client/member를 종료합니다. module의 `close`는 Hazelcast instance까지 닫지 않습니다.

## 검증 경로

모듈 suite는 Testcontainers Hazelcast server와 client를 사용합니다. heavy suite이므로 다른 database·broker container test와 순차 실행합니다.

```bash
./gradlew :bluetape4k-cache-hazelcast:test --no-build-cache --no-configuration-cache
```

도입 전에는 두 client가 같은 map을 바꾸는 invalidation 시나리오, cluster disconnect 중 front-first write, serializer schema 전환, evaluator 실패·중복 실행과 shutdown 중 listener 제거를 애플리케이션 값 타입으로 추가합니다.

## backend 선택

- 이미 Hazelcast `IMap`과 cluster event를 운영한다면 이 모듈이 가장 직접적입니다.
- Redis 표준 운영과 명시적인 wire codec·RESP3 tracking이 필요하면 `cache-lettuce`를 봅니다.
- Redisson의 분산 객체와 local cached map이 필요하면 `cache-redisson`을 봅니다.
- 한 JVM에서만 쓰면 `cache-core`의 Caffeine helper로 시작합니다.

cache-aside는 애플리케이션이 miss 때 원본을 읽고 cache에 넣는 패턴입니다. 여기의 L1→IMap write-through는 cache 계층 사이의 동기화일 뿐 database persistence가 아닙니다.

## 1.12.1에서 제외할 설명

release source에는 README가 언급하는 독립 `ResilientHazelcastNearCache`, write-behind queue, tombstone 구현이 없습니다. 실제 factory test의 `withResilience` 결과는 `cache-core`의 `ResilientNearCacheDecorator`입니다. 매뉴얼과 운영 설계를 이 범위에 맞춥니다.

## Source와 tests

- [`HazelcastNearCacheConfig.kt`](../../../../../cache/cache-hazelcast/src/main/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCacheConfig.kt)
- [`HazelcastCachesTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastCachesTest.kt)
- [`HazelcastNearCacheConfigTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/HazelcastNearCacheConfigTest.kt)
- [`HazelcastServers.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/HazelcastServers.kt)
- [`ResilientHazelcastNearCacheOpsTest.kt`](../../../../../cache/cache-hazelcast/src/test/kotlin/io/bluetape4k/cache/nearcache/ResilientHazelcastNearCacheOpsTest.kt)
- [`build.gradle.kts`](../../../../../cache/cache-hazelcast/build.gradle.kts)
