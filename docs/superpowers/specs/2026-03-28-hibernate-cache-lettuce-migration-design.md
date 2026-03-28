# hibernate-cache-lettuce 이관 설계 Spec

**날짜**: 2026-03-28
**소스**: `bluetape4k-experimental/infra/hibernate-cache-lettuce/`
**대상**: `bluetape4k-projects/data/hibernate-cache-lettuce/`
**모듈명**: `bluetape4k-hibernate-cache-lettuce`

---

## 1. 개요 및 목적

Hibernate 2nd Level Cache를 Lettuce NearCache(Caffeine L1 + Redis L2)로 구현한 모듈을 experimental에서 projects로 이관한다.
기존 `bluetape4k-cache-lettuce`의 `LettuceNearCache`를 Hibernate `RegionFactoryTemplate`에 브릿지하는 구조이며, 소스 코드 및 패키지명 변경 없이 그대로 이관한다.

## 2. 이관 범위

### 2.1 main 소스 (3개 파일, 356 LOC)

| 파일 | 역할 | LOC |
|------|------|-----|
| `LettuceNearCacheRegionFactory.kt` | `RegionFactoryTemplate` 상속, Redis 클라이언트/코덱 초기화, region별 StorageAccess 생성 | 103 |
| `LettuceNearCacheStorageAccess.kt` | `DomainDataStorageAccess` 구현, L1/L2 캐시 브릿지, 캐시 키 정규화 (복합키/NaturalId) | 98 |
| `LettuceNearCacheProperties.kt` | Hibernate properties 파싱/검증, 15가지 코덱 지원, region별 TTL 오버라이드 | 155 |

### 2.2 테스트 소스 (14개 테스트 클래스 + 1개 인프라 + 5개 모델, 2,626 LOC)

**테스트 클래스**:

| 파일 | 역할 |
|------|------|
| `AbstractHibernateNearCacheTest.kt` | 테스트 기반 클래스 (Testcontainers Redis + H2 + SessionFactory) |
| `HibernateEntityCacheTest.kt` | 엔티티 2차 캐시 CRUD 테스트 |
| `HibernateQueryCacheTest.kt` | 쿼리 캐시 테스트 |
| `HibernateQueryCacheAdvancedTest.kt` | 쿼리 캐시 고급 시나리오 |
| `HibernateRelationCacheTest.kt` | 연관관계 캐시 테스트 |
| `HibernateFirstLevelCacheTest.kt` | 1차 캐시 동작 확인 |
| `HibernateElementCollectionCacheTest.kt` | ElementCollection 캐시 |
| `HibernateAdvancedKeyCacheTest.kt` | 복합키/NaturalId 캐시 키 테스트 |
| `HibernateCacheStatisticsTest.kt` | 캐시 통계 (hit/miss/put) |
| `HibernateTransactionRollbackTest.kt` | 트랜잭션 롤백 시 캐시 일관성 |
| `HibernateReadWriteStrategyTest.kt` | READ_WRITE 전략 동시성 |
| `HibernateConcurrentWriteTest.kt` | 동시 쓰기 테스트 |
| `HibernateCacheContainmentTest.kt` | 캐시 포함 관계 검증 |
| `LettuceNearCacheRegionFactoryTest.kt` | RegionFactory 단위 테스트 |

**인프라**:

| 파일 | 역할 |
|------|------|
| `RedisServers.kt` | Testcontainers Redis 싱글턴 |

**엔티티 모델** (`model/`):

| 파일 | 엔티티 |
|------|--------|
| `Person.kt` | `Person` |
| `AdvancedEntities.kt` | `CompositePerson`, `CompositePersonId`, `NaturalUser` |
| `RelationEntities.kt` | `Department`, `Employee`, `Project` |
| `ElementCollectionEntities.kt` | `Article` |
| `VersionedEntities.kt` | `VersionedItem`, `VersionedCategory`, `VersionedCategoryItem` |

### 2.3 기타 파일

| 파일 | 처리 |
|------|------|
| `build.gradle.kts` | 신규 작성 (의존성 수정) |
| `README.md` | 그대로 복사 (200줄) |
| `src/test/resources/junit-platform.properties` | 그대로 복사 |
| `src/test/resources/logback-test.xml` | 그대로 복사 |

### 2.4 이관하지 않는 것

- experimental 저장소의 다른 모듈
- experimental의 settings.gradle.kts / buildSrc 변경
- `build/` 디렉토리 (빌드 산출물)
- `CODE_REVEW.md` (오타 파일, 이관 불필요)

## 3. 패키지/디렉토리 구조

```
data/hibernate-cache-lettuce/
├── build.gradle.kts
├── README.md
└── src/
    ├── main/kotlin/io/bluetape4k/hibernate/cache/lettuce/
    │   ├── LettuceNearCacheProperties.kt
    │   ├── LettuceNearCacheRegionFactory.kt
    │   └── LettuceNearCacheStorageAccess.kt
    └── test/kotlin/io/bluetape4k/hibernate/cache/lettuce/
        ├── AbstractHibernateNearCacheTest.kt
        ├── HibernateAdvancedKeyCacheTest.kt
        ├── HibernateCacheContainmentTest.kt
        ├── HibernateCacheStatisticsTest.kt
        ├── HibernateConcurrentWriteTest.kt
        ├── HibernateElementCollectionCacheTest.kt
        ├── HibernateEntityCacheTest.kt
        ├── HibernateFirstLevelCacheTest.kt
        ├── HibernateQueryCacheAdvancedTest.kt
        ├── HibernateQueryCacheTest.kt
        ├── HibernateReadWriteStrategyTest.kt
        ├── HibernateRelationCacheTest.kt
        ├── HibernateTransactionRollbackTest.kt
        ├── LettuceNearCacheRegionFactoryTest.kt
        ├── RedisServers.kt
        ├── model/
        │   ├── AdvancedEntities.kt
        │   ├── ElementCollectionEntities.kt
        │   ├── Person.kt
        │   ├── RelationEntities.kt
        │   └── VersionedEntities.kt
        └── resources/
            ├── junit-platform.properties
            └── logback-test.xml
```

패키지명 변경 없음: `io.bluetape4k.hibernate.cache.lettuce`

## 4. build.gradle.kts 설계

```kotlin
plugins {
    kotlin("plugin.jpa")
    kotlin("plugin.allopen")
}

// JPA 엔티티 클래스 open 처리
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    // 내부 모듈 참조 (project 형식)
    api(project(":bluetape4k-cache-lettuce"))
    api(project(":bluetape4k-io"))
    api(project(":bluetape4k-lettuce"))

    // Serializer/Compressor runtime dependencies
    implementation(Libs.fory_kotlin)
    implementation(Libs.lz4_java)
    implementation(Libs.snappy_java)
    implementation(Libs.zstd_jni)

    // Hibernate
    api(Libs.hibernate_core)

    // Test
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(project(":bluetape4k-testcontainers"))
    testImplementation(Libs.testcontainers)
    testImplementation(Libs.h2_v2)
    testImplementation(Libs.hikaricp)
}
```

### 4.1 의존성 변환 목록

| experimental (Libs.xxx) | projects | 변경 |
|------------------------|----------|------|
| `Libs.bluetape4k_cache_lettuce` | `project(":bluetape4k-cache-lettuce")` | 외부 -> project |
| `Libs.bluetape4k_io` | `project(":bluetape4k-io")` | 외부 -> project |
| `Libs.bluetape4k_lettuce` | `project(":bluetape4k-lettuce")` | 외부 -> project |
| `Libs.bluetape4k_junit5` | `project(":bluetape4k-junit5")` | 외부 -> project |
| `Libs.bluetape4k_testcontainers` | `project(":bluetape4k-testcontainers")` | 외부 -> project |
| `Libs.fory_kotlin` | `Libs.fory_kotlin` | 동일 |
| `Libs.lz4_java` | `Libs.lz4_java` | 동일 |
| `Libs.snappy_java` | `Libs.snappy_java` | 동일 |
| `Libs.zstd_jni` | `Libs.zstd_jni` | 동일 |
| `Libs.hibernate_core` | `Libs.hibernate_core` | 동일 |
| `Libs.testcontainers` | `Libs.testcontainers` | 동일 |
| `Libs.h2_v2` | `Libs.h2_v2` | 동일 |
| `Libs.hikaricp` | `Libs.hikaricp` | 동일 |

## 5. 의존성 분석

### 5.1 main 소스 내부 모듈 import

| import | 모듈 |
|--------|------|
| `io.bluetape4k.cache.nearcache.LettuceNearCache` | `bluetape4k-cache-lettuce` |
| `io.bluetape4k.cache.nearcache.LettuceNearCacheConfig` | `bluetape4k-cache-lettuce` |
| `io.bluetape4k.io.serializer.BinarySerializers` | `bluetape4k-io` |
| `io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec` | `bluetape4k-lettuce` |
| `io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs` | `bluetape4k-lettuce` |
| `io.bluetape4k.support.requireNotBlank` | `bluetape4k-core` (transitive) |
| `io.bluetape4k.support.requirePositiveNumber` | `bluetape4k-core` (transitive) |
| `io.bluetape4k.utils.ShutdownQueue` | `bluetape4k-core` (transitive) |

### 5.2 테스트 소스 내부 모듈 import

| import | 모듈 |
|--------|------|
| `io.bluetape4k.testcontainers.storage.RedisServer` | `bluetape4k-testcontainers` |
| `io.bluetape4k.logging.KLogging` | `bluetape4k-logging` (transitive) |
| `io.bluetape4k.junit5.concurrency.MultithreadingTester` | `bluetape4k-junit5` |
| `io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester` | `bluetape4k-junit5` |

### 5.3 Libs.kt 추가 필요 여부

추가 필요 없음. 모든 외부 의존성 상수가 projects Libs.kt에 이미 존재.

## 6. 테스트 전략

### 6.1 테스트 환경

- **Redis**: Testcontainers Redis 7+ (`RedisServers.kt` 싱글턴)
- **DB**: H2 in-memory (`AbstractHibernateNearCacheTest`에서 Hibernate properties로 설정)
- **SessionFactory**: 순수 Hibernate (Spring 미사용), `StandardServiceRegistryBuilder`로 구성

### 6.2 검증 순서

1. `./gradlew :bluetape4k-hibernate-cache-lettuce:compileKotlin` — 컴파일 성공 확인
2. `./gradlew :bluetape4k-hibernate-cache-lettuce:test` — 14개 테스트 클래스 전체 통과 확인
3. `./gradlew :bluetape4k-hibernate-cache-lettuce:detekt` — 정적 분석 통과

### 6.3 주의사항

- 테스트 실행 시 Docker 환경 필요 (Testcontainers Redis)
- `AbstractHibernateNearCacheTest`의 `@BeforeAll`에서 `RedisServers.redis`를 먼저 기동
- Hibernate 2차 캐시 설정은 SessionFactory 빌드 시 properties로 주입

## 7. settings.gradle.kts 자동 등록

`data/` 디렉토리 하위 모듈은 `includeModules("data")` 함수에 의해 자동 등록됨.
디렉토리명 `hibernate-cache-lettuce` -> 모듈명 `bluetape4k-hibernate-cache-lettuce` 자동 매핑.
별도의 settings.gradle.kts 수정 불필요.

## 8. CLAUDE.md 업데이트 내용

`Architecture > Module Structure > Data Modules (data/)` 섹션에 추가:

```
- **hibernate-cache-lettuce**: Hibernate 2nd Level Cache + Lettuce NearCache (Caffeine L1 + Redis L2) — `LettuceNearCacheRegionFactory`, `LettuceNearCacheStorageAccess`, region별 TTL 오버라이드, 15가지 코덱 지원
```

위치: `hibernate-reactive` 항목 바로 아래.

## 9. 구현 시 주의사항

1. **패키지명 변경 없음**: `io.bluetape4k.hibernate.cache.lettuce` 그대로 유지
2. **소스 코드 수정 없음**: main/test 소스 파일은 그대로 복사 (import 경로 변경 불필요)
3. **build.gradle.kts만 수정**: `Libs.bluetape4k_xxx` -> `project(":bluetape4k-xxx")` 변환
4. **Hibernate 버전**: experimental 소스 코드는 Hibernate 6.6.x API 기준으로 작성됨. projects의 `Libs.hibernate_core`(6.6.x)와 호환 확인 완료
5. **lz4_java 중복 선언 제거**: experimental의 build.gradle.kts에서 `lz4_java`가 2번 선언되어 있음. 이관 시 1번으로 정리
6. **CODE_REVEW.md 이관 제외**: 오타 파일명이므로 이관하지 않음
7. **README.md 이관**: 200줄짜리 상세 문서 그대로 복사

---

## 10. 태스크 목록

| # | 태스크 | 비고 |
|---|--------|------|
| 1 | `data/hibernate-cache-lettuce/` 디렉토리 생성 | |
| 2 | `build.gradle.kts` 신규 작성 (의존성 project 참조 변환) | lz4_java 중복 제거 |
| 3 | main 소스 3개 파일 복사 | 수정 없음 |
| 4 | test 소스 19개 파일 복사 (테스트 14 + 인프라 1 + 모델 5) | 수정 없음 |
| 4-1 | test 리소스 2개 파일 복사 (`junit-platform.properties`, `logback-test.xml`) | |
| 5 | README.md 복사 | |
| 6 | `./gradlew :bluetape4k-hibernate-cache-lettuce:compileKotlin` 확인 | |
| 7 | `./gradlew :bluetape4k-hibernate-cache-lettuce:test` 확인 | Docker 필요 |
| 8 | CLAUDE.md Data Modules 섹션 업데이트 | |
| 9 | experimental 모듈 제거 (별도 PR 또는 동시 처리) | 선택 |
