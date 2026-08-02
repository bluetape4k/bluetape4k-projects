# Module bluetape4k-hibernate-cache-lettuce

[English](./README.md) | 한국어

Hibernate 7.2 **2nd Level Cache** 구현체 — Lettuce Near Cache (Caffeine L1 + Redis L2) 기반.
`hibernate.cache.lettuce.*` Hibernate properties 설정만으로 모든 Region에 Near Cache가 자동 적용된다.

> Near Cache 코어는 `bluetape4k-projects` 의 `bluetape4k-cache-lettuce` 모듈을 사용한다.
> Spring Boot 4와 통합하려면 [`spring-boot/hibernate-lettuce`](../../spring-boot/hibernate-lettuce/README.ko.md) 참조.

## 패키지 / import 안정성

cache 폴더 재편으로 소스 위치는 `cache/hibernate-cache-lettuce/`가 되었지만 Gradle 프로젝트 이름, Maven artifact, Kotlin package는 유지됩니다.

- Gradle project: `:bluetape4k-hibernate-cache-lettuce`
- Maven artifact: `io.github.bluetape4k:bluetape4k-hibernate-cache-lettuce`
- Kotlin package root: `io.bluetape4k.hibernate.cache.lettuce`

이번 재편으로 인한 사용자 import 변경은 필요하지 않습니다.

## 요구 사항

- **Hibernate ORM**: 7.2.7.Final 이상
- **Jakarta Persistence API**: 3.2.0 (Spring Boot 다운그레이드를 방지하기 위해 `configurations.all`로 강제)
- **Java**: JDK 21+ (bluetape4k 프로젝트 기본 정책)
- **Redis**: RESP3 Client Tracking 사용 시 6+ (구 버전은 `use_resp3=false` 설정)

## 아키텍처

### Near Cache 2-Tier 구조

![Hibernate Lettuce Near Cache 2-Tier Structure diagram](../../docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-01.png)

### 레이어 구조

![Hibernate Lettuce Cache Layer Structure diagram](../../docs/images/readme-diagrams/cache-hibernate-cache-lettuce-diagram-02.png)

- **Region 격리**: 각 Region은 독립된 `LettuceNearCache` 인스턴스를 가짐
- **키 prefix**: Redis key는 `{regionName}:{key}` prefix를 사용해 Region별 key space를 분리
- **Hibernate key encoding**: Hibernate key는 Region prefix 적용 전에 versioned collision-resistant digest로 정규화
- **AccessType**: `NONSTRICT_READ_WRITE` 권장 (분산 캐시에서 soft-lock 불필요)

### Cache Key 요구 사항

캐시 key에 사용하는 모든 Hibernate 식별자(entity ID, natural ID, composite key 구성 요소)는 지원되는 scalar/array 값이거나 전체 object graph가 `java.io.Serializable`이어야 합니다. Canonicalization은 fail-closed로 동작하므로, 지원되지 않는 값과 직렬화할 수 없는 nested member를 포함한 `Serializable` 값은 조회 시 cache miss, 저장 시 무시, keyed eviction 시 예외 전파로 처리됩니다. `toString()`이나 `hashCode()`를 fallback으로 사용하지 않습니다.

text fallback을 사용하던 이전 버전이 만든 기존 entry는 이 변경 후 다시 주소화할 수 없습니다. 해당 legacy entry를 제거해야 한다면 rollout 중 영향받는 region을 한 번 evict하세요. 직렬화 가능한 key는 기존 `hck2` digest 표현을 유지하므로 전체 cache migration은 필요하지 않습니다.

## 최근 변경

- `LettuceNearCacheStorageAccess.evictData()`를 region local-only clear에서 **local + Redis clearAll**로 변경
- key 및 region eviction 중 Redis 오류를 성공으로 처리하지 않고 호출자에게 전파
- correctness-critical text/hashCode cache-key fallback 제거: 지원되지 않는 식별자 graph는 fail-closed 처리
- 테스트에서 `session.get()`을 `session.find()`로 교체해 Hibernate deprecated API 제거
- One-To-Many / Many-To-One / Many-To-Many 관계 엔티티 캐시 시나리오 테스트 추가

## 의존성

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-hibernate-cache-lettuce:${bluetape4kVersion}")

    // 런타임 직렬화 (필수 - bluetape4k-io의 optional 의존성이므로 명시 필요)
    implementation(Libs.fory_kotlin)  // Apache Fory
    implementation(Libs.lz4_java)     // LZ4 압축
}
```

## 설정

### Hibernate Properties

```properties
# Region Factory 등록 (필수)
hibernate.cache.region.factory_class=io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
hibernate.cache.use_second_level_cache=true

# Redis 연결
hibernate.cache.lettuce.redis_uri=redis://localhost:6379

# 직렬화 코덱 (기본: lz4fory)
# 선택지: lz4fory | lz4fastfory | fory | fastfory | kryo | lz4kryo | lz4jdk | gzipfory | gzipfastfory | zstdfory | zstdfastfory | jdk
hibernate.cache.lettuce.codec=lz4fory

# RESP3 + CLIENT TRACKING 활성화 (Redis 6+ 필요)
hibernate.cache.lettuce.use_resp3=true

# L1 (Caffeine) 설정
hibernate.cache.lettuce.local.max_size=10000
hibernate.cache.lettuce.local.expire_after_write=30m

# Redis TTL (기본, ms/s/m/h 단위 지원)
hibernate.cache.lettuce.redis_ttl.default=120s

# Region별 TTL 오버라이드
hibernate.cache.lettuce.redis_ttl.io.example.Product=300s
hibernate.cache.lettuce.redis_ttl.io.example.Order=600s

# timestamps region은 query cache invalidation 정확성을 위해 TTL 비활성화

# Caffeine 통계 수집 (Metrics 연동 시 활성화)
hibernate.cache.lettuce.local.record_stats=false
```

### application.yml (Spring Boot 없이 직접 사용)

```yaml
spring:
  jpa:
    properties:
      hibernate:
        cache:
          region.factory_class: io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
          use_second_level_cache: true
          lettuce:
            redis_uri: redis://localhost:6379
            codec: lz4fory
            use_resp3: true
            local:
              max_size: 10000
              expire_after_write: 30m
            redis_ttl:
              default: 120s
```

지원 codec 값은 `jdk`, `kryo`, `fory`, `fastfory`, `gzip*`, `lz4*`, `snappy*`,
`zstd*` 계열이며, 오타나 미지원 codec 이름은 기본값으로 대체하지 않고 즉시 예외로 실패합니다.

FastFory 코덱은 `SCHEMA_CONSISTENT` 모드를 사용하며 기존 Fory 캐시 데이터와 wire format이
대칭 호환되지 않습니다. 모드를 바꾸기 전에 해당 region의 entry를 eviction 또는 migration할 수 있는
경우에만 사용하세요.

## Entity 설정

```kotlin
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class Product(
    @Id @GeneratedValue
    val id: Long = 0,
    val name: String = "",
    val price: BigDecimal = BigDecimal.ZERO,
)
```

### 컬렉션 캐싱

```kotlin
@OneToMany(mappedBy = "category")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
val products: MutableList<Product> = mutableListOf()
```

`NONSTRICT_READ_WRITE`를 권장한다. 분산 Redis 환경에서는 soft-lock 기반의 `READ_WRITE`가 추가 overhead를 발생시키기 때문이다.

## 동작 방식

#### getFromCache / putIntoCache 흐름

![getFromCache / putIntoCache diagram](../../docs/images/readme-diagrams/cache-hibernate-cache-lettuce-sequence-01.png)

| 연산                        | 동작                                                          |
|-----------------------------|---------------------------------------------------------------|
| `getFromCache`              | L1(Caffeine) hit → 즉시 반환 / miss → Redis GET → L1 populate |
| `putIntoCache`              | L1 + L2 동시 write-through                                    |
| `evictData(key)`            | L1 + L2 해당 key 삭제                                         |
| `evictData()` (region 전체) | L1 + L2 전체 제거 (`clearAll()`)                              |
| 외부 Redis 변경 감지        | RESP3 CLIENT TRACKING push → L1 자동 무효화                   |

### Eviction 실패 계약

`evictData(key)`와 `evictData()`는 L1과 L2를 함께 무효화하는 write-through 연산이다. Redis에서 제거를 완료하지 못하면 backend 예외를 로깅한 뒤 Hibernate에 전파하며, eviction을 성공으로 보고하지 않는다. 로컬 front cache가 먼저 비워진 상태에서 Redis stale entry가 남을 수 있으므로 호출자는 해당 연산을 실패로 처리하고 복구 정책을 적용해야 한다.

## 지원 코덱

| 코덱 이름  | 설명                           | 압축 |
|------------|--------------------------------|------|
| `lz4fory`  | LZ4 + Apache Fory **(기본값)** | LZ4  |
| `lz4fastfory` | LZ4 + Apache FastFory       | LZ4  |
| `fory`     | Apache Fory                    | -    |
| `fastfory` | Apache FastFory                | -    |
| `gzipfory` | GZip + Apache Fory             | GZip |
| `gzipfastfory` | GZip + Apache FastFory     | GZip |
| `snappyfory` | Snappy + Apache Fory         | Snappy |
| `snappyfastfory` | Snappy + Apache FastFory | Snappy |
| `zstdfory` | Zstd + Apache Fory             | Zstd |
| `zstdfastfory` | Zstd + Apache FastFory     | Zstd |
| `kryo`     | Kryo                           | -    |
| `lz4kryo`  | LZ4 + Kryo                     | LZ4  |
| `jdk`      | Java 직렬화                    | -    |
| `lz4jdk`   | LZ4 + Java 직렬화              | LZ4  |

## TTL 단위

`ms` (밀리초) · `s` (초) · `m` (분) · `h` (시간) · (없음 = 초)

## 테스트 실행

```bash
./gradlew :hibernate-cache-lettuce:test
```

Testcontainers로 Redis 7+를 자동 실행하며 H2 인메모리 DB를 사용한다.

## 주의 사항

- **timestamps region TTL 비활성화**:
  `default-update-timestamps-region`은 query cache invalidation 계약 때문에 Redis TTL을 적용하지 않는다.
- **H2 버전**: Hibernate 7.2는 H2 v2 (`com.h2database:h2:2.x`) 필요.
- **Jakarta Persistence 3.2.0
  강제**: `build.gradle.kts`의 `configurations.all` 블록으로 Jakarta Persistence 3.2.0을 강제 지정한다. Spring Boot BOM이 이전 버전으로 다운그레이드하려는 시도를 방지한다.
- **Redis 6+**: `use_resp3=true` (기본값) 사용 시 필요. 하위 버전은 `use_resp3=false` 설정.
