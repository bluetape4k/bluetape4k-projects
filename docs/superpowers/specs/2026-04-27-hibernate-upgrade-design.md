# Hibernate 7.x 업그레이드 및 테스트 커버리지 70%+ 설계

- 작성일: 2026-04-27
- 이슈: [#179](https://github.com/debop/bluetape4k-projects/issues/179)
- 워크트리: `.worktrees/feat/hibernate-upgrade`
- 브랜치: `feat/hibernate-upgrade`
- 작성자: bluetape4k-design (brainstorming → spec)

---

## 1. 목적 및 배경

### 1.1 배경

`data/hibernate`, `data/hibernate-reactive`, `data/hibernate-cache-lettuce` 모듈은 현재 **Hibernate ORM 6.6.x / Hibernate Reactive 2.4.x** 라인을 기반으로 한다. 이 라인은 다음과 같은 한계를 가진다.

- **JPA 3.2 미지원**: Hibernate 7.x 부터 JPA 3.2 표준을 정식 지원 (Spring Boot 4 권장 라인)
- **Spring Boot 4 정합성 부재**: Spring Boot 4.0.x는 Hibernate 7.x를 BOM으로 채택. 현재 6.6.x는 강제 force resolution으로만 통과
- **장기 보안 패치 종료 임박**: Hibernate 6.x LTS는 2026년 말까지만 critical patch 제공
- **Reactive 3.x의 SQL 표준 / lazy fetch 지원**: `hibernate-reactive 3.x`는 H7 ORM과 묶여 있으며, JDK 21 Virtual Thread 친화 API 도입

또한 이슈 #179에서 두 가지 목표를 함께 제시한다.
1. Hibernate ORM/Reactive 버전을 최신 stable로 끌어올린다.
2. 테스트 커버리지를 **line 기준 70% 이상**으로 끌어올린다.

### 1.2 목적

- `data/hibernate` 패밀리를 Hibernate ORM **7.x** / Hibernate Reactive **3.x** 라인으로 이전한다.
- API breaking change에 대한 마이그레이션 가이드와 backward-compatible adapter를 정리한다.
- 테스트 커버리지를 70% 이상으로 끌어올리는 동시에, **회귀 안전망**으로 활용한다.
- 의존하는 spring-boot3 / spring-boot4 / cache-lettuce / examples 모듈이 영향받지 않도록 **business-as-usual**을 보장한다.

### 1.3 비목표

- Quarkus / Panache 통합 신규 추가 (별도 이슈)
- Hibernate Search / Envers 신규 도입 (별도 이슈)
- JPA 3.2 신규 기능 (예: `@SoftDelete`, `@TenantId`) 의 본격 활용 — 본 작업은 **버전 업그레이드 + 호환성 유지**가 우선
- Spring Boot 3 BOM 변경 — Spring Boot 3.5.x는 Hibernate 6.6 BOM을 유지하되, force resolution으로 H7 호환만 검증

---

## 2. 범위

### 2.1 직접 수정 모듈

| 모듈 | 경로 | 수정 강도 |
|------|------|-----------|
| `bluetape4k-hibernate` | `data/hibernate/` | HIGH (43 src, ORM API 변경 흡수) |
| `bluetape4k-hibernate-reactive` | `data/hibernate-reactive/` | HIGH (8 src, Reactive 3.x 마이그레이션) |
| `bluetape4k-hibernate-cache-lettuce` | `data/hibernate-cache-lettuce/` | MEDIUM (3 src, RegionFactory 시그니처 변경) |
| `buildSrc/src/main/kotlin/Libs.kt` | — | LOW (버전 상수만 갱신) |

### 2.2 의존성 검증 대상 모듈 (수정 가능성 있음)

| 모듈 | 경로 | 검증 수준 |
|------|------|-----------|
| `bluetape4k-spring-boot3-hibernate-lettuce` | `spring-boot3/hibernate-lettuce/` | 컴파일 + 테스트 통과 확인. H7 강제 force resolution 그대로 유지 |
| `bluetape4k-spring-boot4-hibernate-lettuce` | `spring-boot4/hibernate-lettuce/` | 이미 H7 force resolution 사용 중 — BOM 정합 후 force 제거 가능성 검토 |
| `bluetape4k-jpa-querydsl-demo` (examples) | `examples/jpa-querydsl-demo/` | QueryDSL 5.1.0 + H7 호환성 확인, 필요 시 6.0+ 업그레이드 |
| `bluetape4k-data-spring-boot3-jpa-*` examples | `examples/spring-data-jpa*` | 빌드 검증만 |

### 2.3 범위 밖

- `bluetape4k-hibernate-types` (구 라이브러리) — 이미 H6.6 기준 마이그레이션 완료
- 다른 ORM (Exposed, MyBatis) — 무관
- Hibernate Validator는 **별도 트랙**: 이미 9.1.0 사용 중이며 jakarta-bean-validation 3.1 호환. 본 spec에서는 검증만 수행

---

## 3. 현재 버전 vs 목표 버전

### 3.1 핵심 의존성

| 의존성 | 현재 | 목표 | 비고 |
|--------|------|------|------|
| `org.hibernate.orm:hibernate-core` | `6.6.44.Final` | `7.2.4.Final` | spring-boot4 BOM과 일치 |
| `org.hibernate.orm:hibernate-jcache` | `6.6.44.Final` | `7.2.4.Final` | second-level cache |
| `org.hibernate.reactive:hibernate-reactive-core` | `2.4.11.Final` | `3.2.x.Final` (latest stable) | H7 ORM 의존 |
| `org.hibernate.validator:hibernate-validator` | `9.1.0.Final` | `9.1.0.Final` (현행 유지) | jakarta validation 3.1 |
| `jakarta.persistence:jakarta.persistence-api` | `3.1.0` (transitive) | `3.2.0` | JPA 3.2 표준 |
| `jakarta.transaction:jakarta.transaction-api` | `2.0.1` | `2.0.1` (변경 없음) | — |
| `com.querydsl:querydsl-jpa` (examples) | `5.1.0:jakarta` | `5.1.0:jakarta` 또는 `6.0.x` | H7 호환성 검증 후 결정 |
| `org.assertj:assertj-core` (test) | 현행 | 현행 | — |
| Spring Boot 3 BOM (force resolution) | `6.6.44.Final` 강제 | `7.2.4.Final` 강제 | spring-boot3 모듈만 |
| Spring Boot 4 BOM | H7.2.4 (BOM) | H7.2.4 (BOM, force 제거 검토) | — |

### 3.2 Build / Test 도구

| 도구 | 현재 | 목표 |
|------|------|------|
| JDK Toolchain | 21 | 21 (변경 없음) |
| Kotlin | 2.3 | 2.3 (변경 없음) |
| JUnit 5 / MockK / Kluent | 현행 | 현행 |
| Testcontainers (PostgreSQL/MySQL) | 현행 | 현행 |
| Jacoco / Kover | 현행 | 현행 — 70% line coverage 게이트 추가 |

---

## 4. 업그레이드 전략

### 4.1 Brainstorming — 위험과 실패 모드

#### 위험 1: Hibernate 7.x SPI 변경으로 인한 컴파일 실패

`data/hibernate/`에는 다음과 같은 internal/SPI 사용처가 있다.
- `SessionFactoryImpl` 캐스팅 (factory holder 추출)
- `SessionImplementor` 사용
- `RegionFactoryTemplate` 추상 메서드 구현 (cache-lettuce)
- `CacheKeysFactory` 인터페이스 변경 가능성

H7에서는 다음과 같은 변경이 알려져 있다.
- `org.hibernate.cache.spi.RegionFactory` 의 `start(SessionFactoryOptions, Map)` → `start(SessionFactoryOptions, Properties)` 시그니처 일부 변경
- `org.hibernate.engine.spi.SessionImplementor` 의 일부 deprecated 메서드 제거
- `Type` 시스템: `org.hibernate.type.descriptor.java.JavaType` 패키지 이동/이름 변경

**완화**: `lsp_diagnostics` 로 컴파일 에러 한 번에 잡고, deprecated → quick-fix → 정합 패치. SPI 사용은 모두 adapter 함수로 격리해서 향후 변경 시 한 곳에서만 수정.

#### 위험 2: Hibernate Reactive 2.x → 3.x 행동 변경

H Reactive 3.x는 다음과 같은 변경이 있다.
- Mutiny `Uni`/`Multi` 시그니처 일부 변경
- `Stage.Session` 의 transactional callback signature 변경 가능
- `withSession` / `withTransaction` 의 reactive context propagation 강화

**완화**: 현재 `bluetape4k-hibernate-reactive` 의 모든 public surface 를 enumerate → 각 함수 단위로 H3 Reactive 매핑 표 작성 (plan 단계). 코루틴 어댑터 (`awaitSuspending`, `coAwait`) 는 v3에서도 동일하게 동작 — 변경 영향 적음.

#### 위험 3: 테스트 인프라 부조화

- Testcontainers PostgreSQL 16 / MySQL 8.4 와 H7 driver 호환성
- `HSQLDB` / `H2` 의 SQL dialect 정합 (H7은 H2 2.2.x 권장)
- `cache-lettuce` 의 second-level cache 키 직렬화 포맷 변경

**완화**:
- H2 의존성 명시적으로 `2.2.224` 이상으로 고정
- HSQLDB는 H7 신규 dialect 가 추가되었는지 확인하고 필요 시 dialect 명시
- cache-lettuce 는 통합 테스트로 hit/miss 시나리오 검증

#### 위험 4: QueryDSL `5.1.0:jakarta` 의 H7 비호환

QueryDSL 5.1.0 은 H6 까지 검증됨. H7의 `JpaSelector` / `Tuple` 변경에 따라 일부 metamodel 생성이 깨질 가능성.

**완화**: 사용 위치는 `examples/jpa-querydsl-demo` 한 곳뿐. 실패하면 QueryDSL 6.0.x (H7 호환) 또는 demo 모듈을 deprecate/skip 옵션 검토.

#### 위험 5: 테스트 커버리지 게이트가 빌드를 깨뜨리는 회귀

70% 게이트를 도입하면 후속 PR에서 갑자기 빌드 깨짐.

**완화**: 본 PR 에서 70% 도달 → 이후 게이트 활성화. 게이트는 module-level (data/hibernate, data/hibernate-reactive, data/hibernate-cache-lettuce) 만 적용.

### 4.2 접근 방식 비교

#### 접근 A: Big-bang (단일 PR로 H7 + 70% 커버리지 동시 진행)

| 항목 | 평가 |
|------|------|
| 장점 | 한 번의 의존성/CI 검증, 단순한 git 그래프, force resolution 한 번만 |
| 단점 | PR 사이즈 매우 큼 (43 src + 테스트), 리뷰 어려움, 회귀 발생 시 분리 어려움 |
| 적합도 | LOW — 변경 범위와 위험도 대비 부담 큼 |

#### 접근 B: 2-phase (Phase 1: H7 업그레이드 / Phase 2: 커버리지)

| 항목 | 평가 |
|------|------|
| 장점 | 업그레이드 회귀를 일찍 감지, 리뷰 부담 감소, 커버리지 추가는 빌드 영향 없음 |
| 단점 | 별도 PR/머지, 약간의 컨텍스트 스위칭 |
| 적합도 | HIGH — 변경 성격이 다르고, 1단계 안정화 후 2단계 추가 가능 |

#### 접근 C: 3-phase (Libs → ORM/Cache → Reactive 분리)

| 항목 | 평가 |
|------|------|
| 장점 | 각 PR 사이즈 최소, 회귀 위치 정확하게 식별 가능 |
| 단점 | Phase 1 단독 머지가 비현실적 (compile만 통과) — 의미 있는 단위가 아님 |
| 적합도 | MEDIUM — over-engineering 위험 |

### 4.3 권장 접근 — **접근 B (2-phase, 별도 PR)**

**2-phase 분리 진행** — 각 phase는 독립 PR로 머지한다.

#### Phase 1 — H7 마이그레이션 PR (이슈 #179-phase1)

목적: 기존 기능이 H7 위에서 정상 동작함을 보장. 커버리지 변경 최소화.

- `Libs.kt` 버전 갱신 (H7 pin, Reactive 버전 pin)
- `data/hibernate` ORM SPI 정합 (SPI adapter 격리 포함)
- `data/hibernate-reactive` Mutiny/Stage 3.x 정합
- `data/hibernate-cache-lettuce` RegionFactory SPI 정합
- 기존 테스트 전수 통과 (기존 테스트 수 유지, 신규 추가 최소)
- 의존 모듈 컴파일/테스트 통과 검증

> **Phase 1 에서 커버리지를 70%로 올리지 않는다.** H7 마이그레이션만 집중한다. 회귀가 발생했을 때 원인이 마이그레이션인지 테스트 추가인지 명확히 분리하기 위함이다.

#### Phase 2 — 테스트 커버리지 70%+ PR (이슈 #179-phase2)

목적: Phase 1이 안정화된 후 커버리지를 70% 이상으로 보강.

- 각 모듈 baseline coverage 실측 → 목표 70%와의 GAP 파악
- Reactive dispatcher 격리 패턴 도입 (`dispatcher` 파라미터 기본값 주입)
- 커버리지 GAP 영역 테스트 추가
- Kover 70% measure-only 게이트 설정

#### Phase 3 — 후속 작업 (별도 이슈, 본 spec의 Out of Scope)

- JPA 3.2 신기능 (`@SoftDelete`, `@TenantId`) 적극 활용 → 별도 spec
- QueryDSL 6.0.x 마이그레이션 → 별도 spec
- hibernate-reactive 코루틴 native suspend API 확장 → 별도 spec

### 4.4 단계별 체크포인트

| 체크포인트 | 검증 방법 |
|-----------|----------|
| CP1: `Libs.kt` 갱신 후 cold build | `./gradlew :bluetape4k-hibernate:compileKotlin` 통과 |
| CP2: hibernate 모듈 ORM SPI 컴파일 통과 | `./gradlew :bluetape4k-hibernate:build -x test` |
| CP3: hibernate-cache-lettuce RegionFactory 컴파일 통과 | `./gradlew :bluetape4k-hibernate-cache-lettuce:build -x test` |
| CP4: hibernate-reactive 컴파일 통과 | `./gradlew :bluetape4k-hibernate-reactive:build -x test` |
| CP5: 기존 테스트 전수 통과 | `./gradlew :bluetape4k-hibernate:test` 외 3개 |
| CP6: 70% 커버리지 도달 | `./gradlew :<module>:koverHtmlReport` 후 line coverage 확인 |
| CP7: 의존 모듈 (spring-boot3/4 + examples) 컴파일/테스트 | 각 모듈 `:test` |

---

## 5. 테스트 커버리지 목표 및 GAP 분석

### 5.1 목표

| 모듈 | 현재 line coverage (추정) | 목표 | 비고 |
|------|---------------------------|------|------|
| `bluetape4k-hibernate` | ~55% | **≥ 70%** | 43 src, Converter/Listener는 이미 잘 커버됨 |
| `bluetape4k-hibernate-reactive` | ~50% | **≥ 70%** | 8 src, suspend wrapper 위주 |
| `bluetape4k-hibernate-cache-lettuce` | ~60% | **≥ 70%** | 3 src, RegionFactory + Region |

> 정확한 현재 coverage는 plan 단계에서 `./gradlew :<module>:koverHtmlReport` 1회 실행 후 갱신.

### 5.2 우선 보강 영역

#### `data/hibernate`
- `JpaConsts` / `HibernateConsts` — 상수 클래스 검증 테스트 (낮은 가중치)
- `SessionFactorySupport` — 캐스팅 분기 / null 처리
- Converter 8종 (`*Converter.kt`) — 각각 round-trip 테스트
- Listener (`AbstractEntityLifecycleListener`) — life-cycle phase 별 호출 검증
- `HibernateExtensions.kt` — `withSession`, `withStatelessSession`, `inTransaction` 분기

#### `data/hibernate-reactive`
- `MutinySessionExtensions` — `awaitSuspending`, `coAwait` 의 success/error/cancel 경로
- `StageSessionExtensions` — `CompletionStage` 변환 분기
- `withSession` 코루틴 어댑터 — 트랜잭션 롤백 / 예외 propagation

#### `data/hibernate-cache-lettuce`
- `LettuceNearCacheRegionFactory` — start/stop 라이프사이클
- `LettuceNearCacheStorageAccess` — put/get/evict/contains 시나리오
- TTL / 만료 / 분산 동기화 (lettuce pub/sub) 통합 테스트

### 5.3 측정 도구

- **Jacoco** (현행) — `./gradlew jacocoTestReport`
- **Kover** (병행 가능) — `./gradlew koverHtmlReport`
- 게이트는 본 PR에서는 **measure-only**, 다음 PR에서 fail-on-violation 활성화 검토 (별도 이슈)

---

## 6. Breaking Changes 대응 계획

### 6.1 Hibernate ORM 7.x 주요 변경

| 변경 | 영향 영역 | 대응 |
|------|----------|------|
| JPA 3.2 (jakarta.persistence 3.2) | Annotation, Entity 정의 | 신규 어노테이션 미사용 — 즉시 영향 없음 |
| `org.hibernate.dialect.*` 일부 dialect deprecate / 제거 | Test config | H2/Postgres/MySQL dialect 명시 또는 자동 감지 의존 |
| `org.hibernate.cfg.AvailableSettings` 키 일부 변경 | `hibernate.properties` / Spring config | 신규 키 매핑, 기존 키 호환 |
| `Type` 시스템 패키지 정리 | Custom Converter/UserType | 컴파일 에러 발생 시 import 변경 |
| `SessionImplementor.getFactory()` 등 deprecated 제거 | SessionFactorySupport | adapter 함수에 모아둠 |
| `RegionFactoryTemplate` 추상 메서드 변경 가능 | cache-lettuce | 시그니처 매칭 |
| Bytecode enhancement 옵션 변경 | build.gradle.kts | 영향 없음 (해당 옵션 미사용) |

### 6.2 Hibernate Reactive 3.x 주요 변경

| 변경 | 영향 영역 | 대응 |
|------|----------|------|
| Hibernate ORM 7.x 의존 | 전 모듈 | ORM 업그레이드와 함께 진행 |
| Mutiny 2.7+ 의존 | hibernate-reactive 모듈 | Mutiny 버전 점검 |
| `Stage.Session#withTransaction` 시그니처 강화 | suspend wrapper | 어댑터 갱신 |
| Vert.x 4.5+ 권장 | Vertx test | Testcontainers 환경에서만 사용 — 영향 적음 |

### 6.3 호환성 어댑터 패턴

깨질 가능성이 큰 SPI 사용은 다음 위치에 격리한다.
- `data/hibernate/src/main/kotlin/io/bluetape4k/hibernate/internal/HibernateInternals.kt` — internal SPI 한정 사용. 향후 H8 마이그레이션 시 한 곳만 수정.
- 테스트에는 internal API 직접 사용 금지 (public API만 검증).

---

## 7. 의존 모듈 검증 계획

### 7.1 Spring Boot 3 (`spring-boot3/hibernate-lettuce`)

- 현재 Hibernate 6.6.x를 BOM으로 사용
- 본 PR 후에도 Spring Boot 3.5.x BOM은 6.6.x를 유지하지만, **force resolution**으로 H7.2.4를 강제
  ```kotlin
  configurations.all {
      resolutionStrategy {
          force("org.hibernate.orm:hibernate-core:7.2.4.Final")
      }
  }
  ```
- 검증: `./gradlew :bluetape4k-spring-boot3-hibernate-lettuce:test`
- Spring Data JPA 3.x 가 H7 위에서 동작함을 확인

### 7.2 Spring Boot 4 (`spring-boot4/hibernate-lettuce`)

- 이미 H7.2.4 force resolution 으로 테스트 중 → BOM이 H7로 정렬되면 force 제거 가능
- 검증: `./gradlew :bluetape4k-spring-boot4-hibernate-lettuce:test`

### 7.3 examples (`jpa-querydsl-demo`)

- QueryDSL 5.1.0:jakarta — H7 호환성 직접 확인
- 실패 시:
  - 단기: 모듈을 잠시 build에서 제외 (publishing 대상 아님)
  - 장기: QueryDSL 6.0.x 로 별도 PR

### 7.4 검증 체크리스트

```
[ ] :bluetape4k-hibernate:test
[ ] :bluetape4k-hibernate-reactive:test
[ ] :bluetape4k-hibernate-cache-lettuce:test
[ ] :bluetape4k-spring-boot3-hibernate-lettuce:test
[ ] :bluetape4k-spring-boot4-hibernate-lettuce:test
[ ] :jpa-querydsl-demo:build (examples)
[ ] data/hibernate kover line coverage ≥ 70%
[ ] data/hibernate-reactive kover line coverage ≥ 70%
[ ] data/hibernate-cache-lettuce kover line coverage ≥ 70%
```

---

## 8. 위험 요소 및 완화 방안

| # | 위험 | 가능성 | 영향 | 완화 |
|---|------|-------|------|------|
| R1 | H7 SPI 패키지 이동으로 컴파일 에러 다수 발생 | HIGH | HIGH | `HibernateInternals.kt` adapter 격리, H7 소스 jar 사전 추출 후 시그니처 diff |
| R2 | Hibernate Reactive 3.x 의 Mutiny API 시그니처 변경 | MEDIUM | HIGH | 함수 단위 매핑 표 (§6.2), 통합 테스트로 회귀 검증 |
| R3 | `LettuceNearCacheStorageAccess` — `org.hibernate.cache.internal.*` 임포트 H7에서 이동/제거 | HIGH | HIGH | CP3 사전 조건으로 H7 캐시 SPI 소스 jar 추출 + import 재매핑 |
| R4 | QueryDSL 5.1.0 의 H7 비호환 | MEDIUM | LOW | examples 한정 — 실패 시 모듈 build skip, 별도 QueryDSL 6.x spec |
| R5 | Spring Boot 3 BOM force resolution 의 transitive 충돌 | MEDIUM | MEDIUM | `./gradlew dependencies --configuration runtimeClasspath` 분석 후 필요 시 추가 force |
| R6 | Reactive `currentVertxDispatcher()` — `runTest` 비호환으로 커버리지 측정 불가 | HIGH | MEDIUM | Phase 2에서 `dispatcher` 파라미터 주입으로 해결 (Phase 1 범위 밖) |
| R7 | Rolling upgrade 중 H6/H7 노드 혼재 시 cache key 직렬화 충돌 | MEDIUM | MEDIUM | cache TTL 단축 + Blue/Green 배포 또는 cache 완전 플러시 후 재배포 |
| R8 | Kryo/Fory codec 역직렬화 공격 (Redis 접근 가능 시) | LOW | HIGH | 클래스 허용 목록(allowlist) 정책 + PII 엔티티 `@Cache` 감사 |
| R9 | Spring Data JPA 3.x + H7 호환성 (Spring Boot 3 force resolution) | MEDIUM | MEDIUM | SB3 + H7 force 환경에서 Spring Data JPA Repository 통합 테스트 필수 |
| R10 | Hibernate Micrometer/Statistics SPI 변경으로 silent metrics drop | MEDIUM | MEDIUM | `hibernate.generate_statistics=true` 활성화 후 테스트에서 counter 증가 검증 |

---

## 8-R. 롤백 절차

> ⛔ **CRITICAL — Phase 1 PR 머지 후 프로덕션 회귀 발생 시 반드시 이 절차를 따른다.**

### 롤백 트리거 기준

다음 중 하나라도 해당하면 즉시 롤백 절차를 시작한다:

- SPI 컴파일 에러가 hotfix PR에서도 수렴하지 않음 (≥ 3회 시도)
- 통합 테스트(PostgreSQL/MySQL Testcontainers) fail rate ≥ 10%
- `RegionFactory` 또는 cache codec 직렬화 incompatibility 발견
- Micrometer statistics silent drop 확인

### 롤백 순서

```bash
# Step 1: Libs.kt 버전 원복
# buildSrc/src/main/kotlin/Libs.kt
#   hibernate = "6.6.44.Final"
#   hibernate_reactive = "2.4.11.Final"

# Step 2: spring-boot3 force resolution 원복
# spring-boot3/hibernate-lettuce/build.gradle.kts 에서 force block 제거

# Step 3: spring-boot4 force resolution 원복 (필요 시)
# spring-boot4/hibernate-lettuce/build.gradle.kts 원복

# Step 4: 컴파일 + 테스트 통과 확인
./gradlew :bluetape4k-hibernate:build :bluetape4k-hibernate-reactive:build :bluetape4k-hibernate-cache-lettuce:build

# Step 5: revert PR 생성
gh pr create --title "revert: hibernate 7.x 업그레이드 롤백 (#179-phase1)" \
  --body "롤백 트리거: [원인 기술]. 원복 버전: H6.6.44.Final / Reactive 2.4.11.Final"
```

### 롤백 사전 준비 (Phase 1 머지 전)

- [ ] `Libs.kt` 변경 전 버전을 주석으로 보존하거나 git tag 생성
- [ ] force resolution 대상 모듈 목록 문서화
- [ ] 모든 변경을 단일 feature branch에 집중 (rollback이 `git revert <merge-commit>` 1회로 가능하도록)

---

## 9. Definition of Done

### 9-A. Phase 1 DoD — H7 마이그레이션 PR

#### 9-A.1 기능 / 호환

- [ ] `bluetape4k-hibernate`: ORM 7.2.4 기반 컴파일 + 모든 기존 테스트 통과
- [ ] `bluetape4k-hibernate-reactive`: Reactive 3.x (정확한 버전 핀 후 기재) 컴파일 + 기존 테스트 통과
- [ ] `bluetape4k-hibernate-cache-lettuce`: H7 RegionFactory/StorageAccess SPI 정합 + 통합 테스트 통과
- [ ] `bluetape4k-spring-boot3-hibernate-lettuce` 컴파일 + 테스트 통과 (force resolution H7로 갱신)
- [ ] `bluetape4k-spring-boot4-hibernate-lettuce` 컴파일 + 테스트 통과
- [ ] `examples/jpa-querydsl-demo` 컴파일 통과 (QueryDSL 비호환 시 skip 주석 추가)
- [ ] `data/hibernate` Micrometer Statistics 카운터 테스트에서 정상 증가 확인 (`hibernate.generate_statistics=true`)
- [ ] Rollback 절차(§8-R) 문서 존재 + 팀 공유

#### 9-A.2 보안

- [ ] `@Cache` 적용 엔티티 감사 — PII 필드 포함 엔티티가 Redis에 평문 캐시되지 않음 확인
- [ ] Kryo/Fory codec 클래스 허용 목록(allowlist) 정책 문서화 또는 기존 정책 재확인

#### 9-A.3 문서 (Phase 1)

- [ ] `data/hibernate/README.md` + `README.ko.md` — H7 업그레이드 사실 및 H6→H7 주요 변경 기재
- [ ] `data/hibernate-reactive/README.md` + `README.ko.md` 갱신
- [ ] `data/hibernate-cache-lettuce/README.md` + `README.ko.md` 갱신
- [ ] 변경된 public API에 `@Deprecated(replaceWith=...)` 또는 KDoc 갱신
- [ ] **BREAKING_CHANGES.md** 또는 README에 "H6→H7 공개 API 변경 목록" 섹션 추가 (외부 소비자 대상)
- [ ] 본 spec 파일을 wiki 인덱스에 등록 (`/wiki-update`)

#### 9-A.4 빌드 / CI

- [ ] `./gradlew :bluetape4k-hibernate:test` 통과 + 기간 보고
- [ ] `./gradlew :bluetape4k-hibernate-reactive:test` 통과 + 기간 보고
- [ ] `./gradlew :bluetape4k-hibernate-cache-lettuce:test` 통과 + 기간 보고
- [ ] `./gradlew detekt` 위반 0
- [ ] `ci.yml` 수정 시 `nightly-tests.yml` 동기화 확인
- [ ] GitHub Actions (CI) 통과
- [ ] `oh-my-claudecode:code-reviewer` 실행, HIGH/CRITICAL 0건

---

### 9-B. Phase 2 DoD — 커버리지 70%+ PR

#### 9-B.1 커버리지

- [ ] `data/hibernate` kover line coverage ≥ 70% (baseline 실측값 대비)
- [ ] `data/hibernate-reactive` kover line coverage ≥ 70%
- [ ] `data/hibernate-cache-lettuce` kover line coverage ≥ 70%
- [ ] 신규/수정 public API 100% covered
- [ ] Reactive 모듈 — `currentVertxDispatcher()` 격리 후 `runTest`로 suspend 경로 검증

#### 9-B.2 빌드 / CI

- [ ] 모든 신규 테스트 통과
- [ ] Kover 70% measure-only 설정 추가 (fail-on-violation은 별도 이슈에서 활성화)

### 9.5 리뷰 / 머지

- [ ] PR description: 변경 요약, 마이그레이션 노트, 테스트 결과, 검증 명령
- [ ] 작업은 워크트리 `.worktrees/feat/hibernate-upgrade` 안에서 수행
- [ ] PR 머지 후 `./bin/clean-branches` 실행

---

## 10. 작업 산출물 요약 (참고)

본 spec 이 채택되면 plan 단계에서 다음을 도출한다.
- Task 분해 (50개 내외 추정)
- 각 task 의 파일 단위 영향 범위
- 테스트 추가 항목 enum
- 위험 R1~R6 의 사전 트리거 / 회귀 시나리오

> 다음 단계: `/oh-my-claudecode:plan` 또는 `bluetape4k-design` plan 단계 진행.
