---
manualId: bluetape4k-hibernate-cache-lettuce
title: "Lettuce 기반 Hibernate 2차 캐시"
description: "Hibernate 2차 캐시를 Caffeine L1과 Redis L2로 구성하고 Region, TTL, 무효화, 장애 경계를 운영하는 방법을 설명합니다."
kind: library
group: caching
learningOrder: 570
---

# Lettuce 기반 Hibernate 2차 캐시

## 제공하는 기능 {#problem}

`bluetape4k-hibernate-cache-lettuce`는 Hibernate ORM 7.2의 2차 캐시를 Lettuce Near Cache에 연결합니다. 각 Hibernate Region마다 Caffeine L1과 Redis L2를 만들고, entity·collection·natural-id·query result·update timestamps Region을 같은 저장 구조로 다룹니다.

이 모듈은 캐시 일관성을 데이터베이스 transaction처럼 보장하지 않습니다. 캐시 오류는 대체로 로그만 남기고 DB 조회로 돌아가며, RESP3 CLIENT TRACKING이 시작되지 않아도 캐시는 계속 동작합니다. hit rate뿐 아니라 무효화와 fallback 동작을 함께 검증해야 합니다.

## 사용하기 전에 결정할 것 {#when-to-use}

- 반복 조회 비용이 Redis 왕복과 직렬화 비용보다 큰지 측정합니다.
- entity와 collection 중 무엇을 캐시할지 Region별로 정합니다.
- `NONSTRICT_READ_WRITE`의 짧은 stale window를 허용할지 판단합니다.
- Caffeine 만료, Redis TTL과 Hibernate query timestamps의 관계를 정합니다.
- RESP3 CLIENT TRACKING을 쓸 수 있는 Redis 6+ 환경인지 확인합니다.
- Redis 데이터가 신뢰 경계 안에 있는지, 어떤 직렬화 codec을 허용할지 정합니다.

단일 프로세스의 단순 캐시라면 일반 Caffeine이 더 작습니다. Hibernate가 아닌 직접 캐시 API가 필요하면 [bluetape4k-cache-lettuce](./bluetape4k-cache-lettuce.md)를 사용합니다.

## 의존성 추가 {#coordinates}

사용자는 하위 cache, Lettuce, Hibernate 버전을 따로 맞추지 않고 중앙 BOM 버전만 관리합니다. 실제 Redis 서버와 database driver는 애플리케이션이 준비합니다.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-dependencies:<version>"))
    implementation("io.github.bluetape4k:bluetape4k-hibernate-cache-lettuce")

    runtimeOnly("org.postgresql:postgresql") // 사용하는 driver로 교체
}
```

1.12.1 artifact는 Fory와 LZ4 runtime을 포함하지만, 선택한 codec에 따라 Snappy·Zstd·Kryo/JDK 직렬화 특성과 신뢰 경계를 따로 검토합니다.

## 첫 2차 캐시 {#quick-start}

Hibernate 설정에 RegionFactory와 Redis 연결을 등록하고, 캐시할 entity에 Hibernate `@Cache`를 붙입니다.

```properties
hibernate.cache.use_second_level_cache=true
hibernate.cache.region.factory_class=io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
hibernate.cache.lettuce.redis_uri=redis://localhost:6379
hibernate.cache.lettuce.codec=lz4fory
hibernate.cache.lettuce.use_resp3=true
hibernate.cache.lettuce.local.max_size=10000
hibernate.cache.lettuce.local.expire_after_write=30m
hibernate.cache.lettuce.redis_ttl.default=120s
```

```kotlin
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Product(
    @Id @GeneratedValue
    var id: Long? = null,
    var name: String = "",
)
```

같은 entity를 새 Session에서 다시 읽어야 1차 캐시가 아니라 2차 캐시 동작을 확인할 수 있습니다.

## API 선택 지도 {#api-by-task}

| 필요한 작업 | 시작할 API·설정 | 기억할 경계 |
| --- | --- | --- |
| Hibernate RegionFactory 등록 | `LettuceNearCacheRegionFactory` | SessionFactory가 RedisClient와 Region cache 수명을 소유합니다. |
| 설정 파싱·검증 | `LettuceNearCacheProperties` | 잘못된 codec, boolean, 크기와 duration은 시작 중 즉시 실패합니다. |
| entity·collection cache | `@Cache`, `CacheConcurrencyStrategy` | 1차 캐시와 2차 캐시를 구분해 테스트합니다. |
| query cache | `hibernate.cache.use_query_cache`, `setCacheable(true)` | update timestamps Region은 Redis TTL을 사용하지 않습니다. |
| 특정 key·Region 제거 | `SessionFactory.cache.evict*` | key 제거와 Region 전체 제거 모두 L1·L2에 전달됩니다. |
| cache 통계 | Hibernate statistics, `getCaches()` | Caffeine 통계는 `local.record_stats=true`가 필요합니다. |
| Spring Boot 자동 설정 | `bluetape4k-spring-boot-hibernate-lettuce` | 별도 artifact가 properties·Metrics·Actuator를 연결합니다. |

## 학습 경로 {#concepts}

각 장은 설정 목록만 옮기지 않고 실제 1.12.1 코드와 테스트를 따라갑니다. 캐시를 처음 붙이는 과정부터 Region 격리, key digest, query invalidation, Redis 장애와 종료 순서까지 코드 예제와 실패 조건을 함께 설명합니다.

1. [Near Cache 구조와 Region](./bluetape4k-hibernate-cache-lettuce/architecture-regions.md) — Caffeine L1, Redis L2와 Hibernate Region의 관계를 잡습니다.
2. [설정, codec과 TTL](./bluetape4k-hibernate-cache-lettuce/configuration-codecs-ttl.md) — 모든 설정 키, duration, Region별 TTL과 직렬화 신뢰 경계를 확인합니다.
3. [Entity, collection과 query cache](./bluetape4k-hibernate-cache-lettuce/entity-query-collection-cache.md) — 캐시 annotation, 새 Session 검증, query timestamps를 다룹니다.
4. [Key, 동시성 전략과 무효화](./bluetape4k-hibernate-cache-lettuce/keys-concurrency-invalidation.md) — `hck2` digest, composite·natural id, RESP3와 `READ_WRITE` 선택 기준을 설명합니다.
5. [수명주기, 장애와 운영](./bluetape4k-hibernate-cache-lettuce/lifecycle-failures-operations.md) — 시작·종료 순서, fallback, eviction과 관측 항목을 정리합니다.
6. [Spring Boot와 생태계 경로](./bluetape4k-hibernate-cache-lettuce/spring-boot-ecosystem.md) — 자동 설정, demo, Hibernate·Lettuce·Exposed cache 학습 경로를 연결합니다.

처음 도입한다면 1→2→3→4 순서로 읽고, 운영 점검은 5장을 기준으로 만듭니다. Spring Boot 애플리케이션이라도 먼저 이 모듈의 계약을 이해한 뒤 6장의 자동 설정으로 넘어갑니다.

## 권장 패턴 {#patterns}

읽기 비중이 높고 stale window를 허용하는 데이터부터 작은 Region 단위로 도입합니다. entity와 collection은 필요한 곳에만 `@Cache`를 붙이고, query cache는 같은 조건의 반복 query가 실제로 많은 경우에만 켭니다. 쓰기 뒤에는 Hibernate가 수행하는 eviction을 통과시키며 Redis를 직접 수정하지 않습니다.

캐시가 비어도 요청이 정상 동작하도록 DB fallback을 기본 계약으로 둡니다. Redis 장애 때 DB 부하가 갑자기 커질 수 있으므로 pool, query latency와 cache miss를 같은 경보에서 봅니다.

## 연동 {#integrations}

모듈은 `bluetape4k-cache-lettuce`, `bluetape4k-lettuce`, `bluetape4k-io`와 Hibernate ORM을 묶습니다. Spring Boot 4에서는 [bluetape4k-spring-boot-hibernate-lettuce](./bluetape4k-spring-boot-hibernate-lettuce.md)가 `bluetape4k.cache.lettuce-near.*` 설정을 Hibernate property로 바꾸고 Metrics·Actuator를 연결합니다.

ORM helper와 entity lifecycle은 [bluetape4k-hibernate](./bluetape4k-hibernate.md), Redis를 직접 다루는 API는 [bluetape4k-lettuce](./bluetape4k-lettuce.md)에서 이어집니다.

## 설정 {#configuration}

기본값은 Redis `localhost:6379`, codec `lz4fory`, L1 최대 10,000개·쓰기 후 30분 만료, Redis TTL 120초, RESP3 사용입니다. 접미사가 없는 duration은 초로 읽고 `ms`, `s`, `m`, `h`를 지원합니다.

Region별 TTL은 `hibernate.cache.lettuce.redis_ttl.<regionName>`으로 기본 TTL을 덮어씁니다. `default-update-timestamps-region`은 query cache invalidation 계약 때문에 설정과 무관하게 TTL이 없습니다.

## 실패 동작 {#failures}

잘못된 codec, boolean, 0 이하 크기·duration과 빈 Redis URI는 시작 단계에서 `IllegalArgumentException`으로 실패합니다. 실행 중 `get`, `put`, `contains`, eviction의 Redis 오류는 `LettuceNearCacheStorageAccess`가 경고로 기록하고 각각 `null`, 무시, `false`, 무시로 바꿉니다. 이는 availability를 높이지만 Redis 장애 때 DB fallback과 stale L1 위험을 운영에서 감시해야 한다는 뜻입니다.

RESP3 tracking 시작 실패도 경고만 남깁니다. 캐시가 살아 있다는 사실만으로 프로세스 간 L1 무효화가 정상이라고 판단하지 않습니다.

## 운영 {#operations}

Hibernate의 second-level hit·miss·put, query cache hit, update timestamps, Region별 entry 수와 Caffeine 통계를 관찰합니다. Redis latency·error·connection 수, DB query latency와 pool saturation도 같은 화면에서 봅니다. `evictAllRegions`와 Region 전체 제거는 `SCAN`과 `UNLINK`를 사용하므로 Region key 수가 많을 때 완료 시간과 Redis 부하를 측정합니다.

## 테스트 {#testing}

1.12.1 테스트는 H2와 Testcontainers Redis 7+로 entity·collection·query·natural-id·composite key·rollback·동시 읽기와 통계를 검증합니다.

```bash
./gradlew :bluetape4k-hibernate-cache-lettuce:test --no-build-cache --no-configuration-cache
```

테스트에서 cache hit를 확인할 때는 Session을 닫고 새 Session에서 읽습니다. 같은 Session의 반복 조회는 Hibernate 1차 캐시 검증에 가깝습니다.

## 워크숍 {#workshops}

[Hibernate Lettuce demo](./bluetape4k-spring-boot-hibernate-lettuce-demo.md)는 `Product` entity, Spring Data repository, cache endpoint와 실제 `application.yml`을 갖춘 실행 예제입니다. 모듈 내부에서는 `HibernateEntityCacheTest`, `HibernateQueryCacheTest`, `HibernateAdvancedKeyCacheTest`가 작은 단계별 실습 역할을 합니다.

더 넓은 cache-aside·read-through·write-through 전략은 `bluetape4k-cache-lettuce`와 [exposed-workshop](https://github.com/bluetape4k/exposed-workshop)에서 비교합니다. Hibernate 2차 캐시의 `putIntoCache`를 애플리케이션 repository의 write-through 저장 패턴과 같은 개념으로 혼동하지 않습니다.

## 1.12.1 범위 {#limitations}

이 매뉴얼은 `bluetape4k-projects` 1.12.1 배포 소스를 기준으로 합니다. `LettuceNearCacheRegionFactory`는 기본 access type으로 `NONSTRICT_READ_WRITE`를 반환하지만 entity annotation은 `READ_WRITE`를 선택할 수도 있습니다. 분산 soft-lock의 비용과 eviction 동작을 측정하지 않았다면 기본 전략을 우선합니다.

CLIENT TRACKING 시작 실패는 factory 시작을 중단하지 않습니다. StorageAccess의 cache 연산 오류도 transaction을 실패시키지 않습니다. 캐시는 source of truth가 아니라 재생성 가능한 가속 계층으로만 사용해야 합니다.

## Source와 tests {#sources}

- [`LettuceNearCacheProperties.kt`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheProperties.kt)
- [`LettuceNearCacheRegionFactory.kt`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheRegionFactory.kt)
- [`LettuceNearCacheStorageAccess.kt`](../../../../cache/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCacheStorageAccess.kt)
- [`LettuceNearCache.kt`](../../../../cache/cache-lettuce/src/main/kotlin/io/bluetape4k/cache/nearcache/LettuceNearCache.kt)
- [`LettuceNearCachePropertiesTest.kt`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/LettuceNearCachePropertiesTest.kt)
- [`HibernateEntityCacheTest.kt`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateEntityCacheTest.kt)
- [`HibernateAdvancedKeyCacheTest.kt`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateAdvancedKeyCacheTest.kt)
- [`HibernateTransactionRollbackTest.kt`](../../../../cache/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/HibernateTransactionRollbackTest.kt)

<!-- release-readme-diagrams:start -->
## 배포본 다이어그램 {#release-diagrams}

아래 그림은 `1.12.1` 배포본의 README 자산을 해당 배포 커밋에서 직접 불러옵니다. 이후 SNAPSHOT이 아니라 이 매뉴얼 버전의 구조와 실행 흐름을 보여 줍니다. 미리보기를 누르면 같은 배포 커밋의 SVG 원본이 열립니다.

### Hibernate Lettuce Near Cache 2-Tier 구조도

[![Hibernate Lettuce Near Cache 2-Tier 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-01.svg)

_배포본 README: [`cache/hibernate-cache-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/hibernate-cache-lettuce/README.ko.md)_

### Hibernate Lettuce Cache Layer 구조도

[![Hibernate Lettuce Cache Layer 구조도](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-02.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-02.svg)

_배포본 README: [`cache/hibernate-cache-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/hibernate-cache-lettuce/README.ko.md)_

### getFromCache / putIntoCache 다이어그램

[![getFromCache / putIntoCache 다이어그램](https://raw.githubusercontent.com/bluetape4k/bluetape4k-projects/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-sequence-01.png)](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/docs/images/readme-diagrams/cache-hibernate-cache-lettuce-sequence-01.svg)

_배포본 README: [`cache/hibernate-cache-lettuce/README.ko.md`](https://github.com/bluetape4k/bluetape4k-projects/blob/7cf0b73646af05c0f8872cc4f6a16983949c4e3e/cache/hibernate-cache-lettuce/README.ko.md)_

<!-- release-readme-diagrams:end -->
