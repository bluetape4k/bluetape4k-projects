# hibernate-cache-lettuce 이관 구현 계획

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hibernate 2nd Level Cache + Lettuce NearCache 모듈을 experimental에서 projects로 이관

**Architecture:** `bluetape4k-cache-lettuce`의 `LettuceNearCache`(Caffeine L1 + Redis L2)를 Hibernate `RegionFactoryTemplate`에 브릿지. 소스 코드 및 패키지명 변경 없이 그대로 이관. `build.gradle.kts`만 의존성을 `Libs.bluetape4k_xxx` -> `project(":bluetape4k-xxx")`로 변환.

**Tech Stack:** Kotlin 2.3, Hibernate 6.6.x, Lettuce 6.8.2, Caffeine, Testcontainers Redis, H2, JUnit 5, Kluent

**Spec:** `docs/superpowers/specs/2026-03-28-hibernate-cache-lettuce-migration-design.md`

---

## 주요 구현 주의사항

1. **패키지명 변경 없음**: `io.bluetape4k.hibernate.cache.lettuce` 그대로 유지. import 경로 수정 불필요.
2. **소스 코드 수정 없음**: main/test 소스 파일은 1바이트도 수정하지 않고 그대로 복사.
3. **lz4_java 중복 제거**: experimental build.gradle.kts에서 `Libs.lz4_java`가 2번 선언됨. 이관 시 1번으로 정리.
4. **settings.gradle.kts 수정 불필요**: `data/` 하위 모듈은 `includeModules("data")`로 자동 등록. 디렉토리명 `hibernate-cache-lettuce` -> 모듈명 `bluetape4k-hibernate-cache-lettuce` 자동 매핑.
5. **Libs.kt 추가 불필요**: 모든 외부 의존성 상수가 projects Libs.kt에 이미 존재.
6. **Docker 필요**: 테스트 실행 시 Testcontainers Redis 사용. Docker Desktop 기동 필수.
7. **CODE_REVEW.md 이관 제외**: 오타 파일명이므로 이관하지 않음.

---

## 파일 구조

### 신규 생성 (build.gradle.kts)

| 파일 | 역할 |
|------|------|
| `data/hibernate-cache-lettuce/build.gradle.kts` | 의존성 project 참조 변환, lz4_java 중복 제거, allOpen/jpa 플러그인 |

### 소스 복사 (수정 없음)

| 소스 | 파일 수 | 비고 |
|------|---------|------|
| main 소스 | 3 | Properties, RegionFactory, StorageAccess |
| test 클래스 | 14 | 엔티티/쿼리/관계/동시성/통계 테스트 |
| test 인프라 | 1 | RedisServers.kt |
| test 모델 | 5 | Person, Advanced, Relation, ElementCollection, Versioned |
| test 리소스 | 2 | junit-platform.properties, logback-test.xml |
| README.md | 1 | 200줄 상세 문서 |

---

## 구현 태스크

### Task 1: 디렉토리 생성 [complexity: low]

- [ ] `data/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/` 디렉토리 생성
- [ ] `data/hibernate-cache-lettuce/src/test/kotlin/io/bluetape4k/hibernate/cache/lettuce/model/` 디렉토리 생성
- [ ] `data/hibernate-cache-lettuce/src/test/resources/` 디렉토리 생성

**검증**: 디렉토리 구조 확인

---

### Task 2: build.gradle.kts 작성 [complexity: medium]

- [ ] `data/hibernate-cache-lettuce/build.gradle.kts` 신규 작성
- [ ] `kotlin("plugin.jpa")` + `kotlin("plugin.allopen")` 플러그인 포함
- [ ] `allOpen` 블록: Entity, MappedSuperclass, Embeddable 어노테이션 open 처리
- [ ] 의존성 변환: `Libs.bluetape4k_cache_lettuce` -> `project(":bluetape4k-cache-lettuce")`
- [ ] 의존성 변환: `Libs.bluetape4k_io` -> `project(":bluetape4k-io")`
- [ ] 의존성 변환: `Libs.bluetape4k_lettuce` -> `project(":bluetape4k-lettuce")`
- [ ] 의존성 변환: `Libs.bluetape4k_junit5` -> `project(":bluetape4k-junit5")`
- [ ] 의존성 변환: `Libs.bluetape4k_testcontainers` -> `project(":bluetape4k-testcontainers")`
- [ ] `Libs.lz4_java` 중복 선언 제거 (2번 -> 1번)
- [ ] 외부 의존성 유지: fory_kotlin, snappy_java, zstd_jni, hibernate_core, testcontainers, h2_v2, hikaricp

**의존성 변환 전체 목록**:

| experimental | projects | scope |
|-------------|----------|-------|
| `Libs.bluetape4k_cache_lettuce` | `project(":bluetape4k-cache-lettuce")` | api |
| `Libs.bluetape4k_io` | `project(":bluetape4k-io")` | api |
| `Libs.bluetape4k_lettuce` | `project(":bluetape4k-lettuce")` | api |
| `Libs.fory_kotlin` | `Libs.fory_kotlin` | implementation |
| `Libs.lz4_java` | `Libs.lz4_java` | implementation (1회만) |
| `Libs.snappy_java` | `Libs.snappy_java` | implementation |
| `Libs.zstd_jni` | `Libs.zstd_jni` | implementation |
| `Libs.hibernate_core` | `Libs.hibernate_core` | api |
| `Libs.bluetape4k_junit5` | `project(":bluetape4k-junit5")` | testImplementation |
| `Libs.bluetape4k_testcontainers` | `project(":bluetape4k-testcontainers")` | testImplementation |
| `Libs.testcontainers` | `Libs.testcontainers` | testImplementation |
| `Libs.h2_v2` | `Libs.h2_v2` | testImplementation |
| `Libs.hikaricp` | `Libs.hikaricp` | testImplementation |

**검증**: Gradle sync 성공 확인

---

### Task 3: main 소스 복사 [complexity: low]

- [ ] `LettuceNearCacheProperties.kt` 복사 (155 LOC)
- [ ] `LettuceNearCacheRegionFactory.kt` 복사 (103 LOC)
- [ ] `LettuceNearCacheStorageAccess.kt` 복사 (98 LOC)

**소스 경로**: `bluetape4k-experimental/infra/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/`
**대상 경로**: `data/hibernate-cache-lettuce/src/main/kotlin/io/bluetape4k/hibernate/cache/lettuce/`
**검증**: 파일 3개 존재 확인

---

### Task 4: test 소스 복사 [complexity: low]

**테스트 클래스 14개**:

- [ ] `AbstractHibernateNearCacheTest.kt`
- [ ] `HibernateAdvancedKeyCacheTest.kt`
- [ ] `HibernateCacheContainmentTest.kt`
- [ ] `HibernateCacheStatisticsTest.kt`
- [ ] `HibernateConcurrentWriteTest.kt`
- [ ] `HibernateElementCollectionCacheTest.kt`
- [ ] `HibernateEntityCacheTest.kt`
- [ ] `HibernateFirstLevelCacheTest.kt`
- [ ] `HibernateQueryCacheAdvancedTest.kt`
- [ ] `HibernateQueryCacheTest.kt`
- [ ] `HibernateReadWriteStrategyTest.kt`
- [ ] `HibernateRelationCacheTest.kt`
- [ ] `HibernateTransactionRollbackTest.kt`
- [ ] `LettuceNearCacheRegionFactoryTest.kt`

**인프라 1개**:

- [ ] `RedisServers.kt`

**모델 5개** (`model/` 디렉토리):

- [ ] `AdvancedEntities.kt`
- [ ] `ElementCollectionEntities.kt`
- [ ] `Person.kt`
- [ ] `RelationEntities.kt`
- [ ] `VersionedEntities.kt`

**리소스 2개** (`src/test/resources/`):

- [ ] `junit-platform.properties`
- [ ] `logback-test.xml`

**소스 경로**: `bluetape4k-experimental/infra/hibernate-cache-lettuce/src/test/`
**대상 경로**: `data/hibernate-cache-lettuce/src/test/`
**검증**: 파일 22개(14+1+5+2) 존재 확인

---

### Task 5: README.md 복사 [complexity: low]

- [ ] `README.md` 복사 (200줄)

**소스 경로**: `bluetape4k-experimental/infra/hibernate-cache-lettuce/README.md`
**대상 경로**: `data/hibernate-cache-lettuce/README.md`
**검증**: 파일 존재 확인

---

### Task 6: 컴파일 검증 [complexity: medium]

- [ ] `./gradlew :bluetape4k-hibernate-cache-lettuce:compileKotlin` 성공
- [ ] `./gradlew :bluetape4k-hibernate-cache-lettuce:compileTestKotlin` 성공
- [ ] 컴파일 오류 발생 시 build.gradle.kts 의존성 수정

**명령어**:
```bash
./gradlew :bluetape4k-hibernate-cache-lettuce:compileKotlin :bluetape4k-hibernate-cache-lettuce:compileTestKotlin
```

**실패 시 대응**:
- import 미해결 -> 누락된 의존성 추가
- Hibernate API 호환 -> 버전 차이 확인 (6.6.x 기준)

---

### Task 7: 테스트 실행 [complexity: medium]

- [ ] Docker Desktop 기동 확인
- [ ] `./gradlew :bluetape4k-hibernate-cache-lettuce:test` 실행
- [ ] 14개 테스트 클래스 전체 통과 확인

**명령어**:
```bash
./gradlew :bluetape4k-hibernate-cache-lettuce:test
```

**실패 시 대응**:
- Testcontainers Redis 미기동 -> Docker 환경 확인
- 캐시 타이밍 이슈 -> TTL 관련 테스트 격리 확인
- Hibernate SessionFactory 구성 오류 -> AbstractHibernateNearCacheTest properties 확인

---

### Task 7.5: Detekt 정적 분석 [complexity: low]

- [ ] `./gradlew :bluetape4k-hibernate-cache-lettuce:detekt` 실행
- [ ] detekt 위반 없음 확인 (소스 복사이므로 통과 기대)

**명령어**:
```bash
./gradlew :bluetape4k-hibernate-cache-lettuce:detekt
```

**실패 시 대응**:
- projects와 experimental의 detekt 규칙 차이로 위반 발생 시 -> `@Suppress` 어노테이션 최소 적용

---

### Task 8: CLAUDE.md 업데이트 [complexity: low]

- [ ] `CLAUDE.md` > Architecture > Module Structure > Data Modules 섹션에 항목 추가
- [ ] `hibernate-reactive` 항목 바로 아래에 배치

**추가 내용**:
```
- **hibernate-cache-lettuce**: Hibernate 2nd Level Cache + Lettuce NearCache (Caffeine L1 + Redis L2) — `LettuceNearCacheRegionFactory`, `LettuceNearCacheStorageAccess`, region별 TTL 오버라이드, 15가지 코덱 지원
```

**검증**: CLAUDE.md diff 확인

---

## 실행 순서 및 병렬화

```
Task 1 (디렉토리)
    |
    v
Task 2 (build.gradle.kts) + Task 3 (main 소스) + Task 4 (test 소스) + Task 5 (README)  [병렬]
    |
    v
Task 6 (컴파일 검증)
    |
    v
Task 7 (테스트 실행)
    |
    v
Task 7.5 (detekt 정적 분석)
    |
    v
Task 8 (CLAUDE.md 업데이트)
```

**예상 소요**: Task 1~5 (5분), Task 6 (2분), Task 7 (5분, Docker 기동 시간 포함), Task 8 (1분)
**총 예상**: 약 13분

---

## 완료 조건

1. `data/hibernate-cache-lettuce/` 디렉토리에 26개 파일 존재 (build.gradle.kts + README + main 3 + test 22)
2. `./gradlew :bluetape4k-hibernate-cache-lettuce:compileKotlin :bluetape4k-hibernate-cache-lettuce:compileTestKotlin` 성공
3. `./gradlew :bluetape4k-hibernate-cache-lettuce:test` 전체 통과
4. `./gradlew :bluetape4k-hibernate-cache-lettuce:detekt` 통과
5. CLAUDE.md Data Modules 섹션에 `hibernate-cache-lettuce` 항목 존재

---

## 범위 외 (Deferred)

- **Experimental 모듈 제거**: `bluetape4k-experimental/infra/hibernate-cache-lettuce/` 삭제는 별도 PR에서 처리 (Spec Task 9 참조)
