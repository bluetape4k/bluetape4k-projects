# Epic #1420 데이터 계약 검증 stacked train 설계

## 1. 문서 상태와 결정

- 대상 Epic: [#1420 JDBC·Cassandra·Spring Boot 계약 검증](https://github.com/bluetape4k/bluetape4k-projects/issues/1420)
- 대상 milestone: `2.0.0`
- 기준 commit: 현재 fetch로 확인한 live `origin/develop@a907d144f39bfb94cba783cf65a5412e0714e9d5`로 갱신한다. 초안 기준은 `24a3eb9b580e1e9307df1b8d37e8424f82f978d4`였으며, 구현 직전 remote가 이동하면 rebase·range-diff 후 문서와 receipt를 함께 갱신한다.
- 분류: Type-A Full Feature, strict linear stacked PR train
- 현재 상태: 설계 승인 완료, spec/plan 및 구현 전 단계

Epic에는 다음 여섯 child가 native sub-issue로 연결되어 있다. #1340과 #1345는 각각 PR #1487과 PR #1513으로 이미 병합되었으므로, 현재 train은 그 결과를 포함한 `develop`에서 #1359부터 재개한다.

| 순서 | Issue | 현재 상태 | 역할 |
| --- | --- | --- | --- |
| 1 | #1340 | CLOSED | JDBC·Cassandra 위임/영속성 oracle 선행 결과 |
| 2 | #1345 | CLOSED | Cassandra 타입 round-trip 선행 결과 |
| 3 | #1359 | OPEN | Cassandra `WriteOptions` nullable 계약 |
| 4 | #1346 | OPEN | Hibernate QueryDSL Kotlin codegen 재평가 |
| 5 | #1357 | OPEN | hibernate-lettuce root 조건 전파 |
| 6 | #1358 | OPEN | MongoDB Spring Boot 4.1 경계 |

Epic 본문에는 `Epic·1.13.0`이라는 과거 문구가 남아 있으나 live milestone은 `2.0.0`이다. 구현 중에는 본문을 임의로 덮어쓰지 않고, 모든 child와 PR의 live 상태를 확인하는 closeout 단계에서 정합성을 갱신한다.

## 2. 문제와 목표

현재 네 개의 미착수 child는 서로 다른 모듈에 있지만 하나의 data contract 검증 train으로 묶여 있다.

1. Cassandra `WriteOptions` 확장은 nullable 값을 `!!`로 재사용하고 있어 null/음수/0 TTL과 timestamp 조합에서 안전한 계약이 없다.
2. Hibernate QueryDSL Kotlin codegen은 build 파일의 비활성화 주석과 fixture의 상충하는 주석만 있으며, data class·일반 Entity·association의 지원 범위가 재현 가능한 matrix로 고정되지 않았다.
3. hibernate-lettuce Metrics auto-configuration은 root `enabled`와 metrics `enabled`를 독립적으로 검사하므로 root를 끈 상태에서 binder가 살아날 수 있다.
4. MongoDB auto-configuration의 KDoc와 통합 테스트는 구 namespace와 수동 client 설정에 의존하여 Spring Boot 4.1 property binding/order 계약을 증명하지 못한다.

목표는 각 child를 독립적으로 리뷰·검증 가능한 PR로 만들고, 앞 PR의 green 결과와 생성물을 다음 PR이 상속하는 순차 train을 완성하는 것이다. 공개 API 호환성, Spring 조건/순서, 실제 provider 경계, 한국어 문서 parity를 모두 회귀 테스트로 고정한다.

## 3. 범위와 비범위

### 3.1 범위

- #1359의 `OptionsSupport` null-safe 구현과 TTL/timestamp/statement subtype matrix, Cassandra EN/KO README의 microseconds·TTL 경계 문서
- #1346의 QueryDSL Kotlin codegen 및 일반 Entity 지원성 재현 fixture, build 선택, 문서화
- #1357의 root/metrics property matrix와 Metrics/Actuator 조건 전파
- #1358의 Spring Boot 4.1 `spring.mongodb.*` binding, auto-configuration ordering, 테스트 경계 분리
- 각 slice의 Korean KDoc/README parity, CHANGELOG migration note, targeted test, module build와 정적 분석
- strict linear branch/base/head, restack, rollback, exact-head CI 증거
- Type-A spec/plan/lesson과 독립 리뷰 관점별 findings 수렴

### 3.2 비범위

- 새로운 Gradle module, 외부 dependency, QueryDSL fork 또는 전면 entity mapping 재작성
- Cassandra/Spring Data 공개 API 이름 변경
- hibernate-lettuce의 cache 구현 자체 변경
- MongoDB driver/server 버전 변경 또는 Testcontainers launcher 재설계
- release/tag/publish, 자동 merge, 승인 전 branch/issue/PR metadata 정리

## 4. 현재 근거와 영향 파일

### 4.1 #1359 Cassandra WriteOptions

- production: `spring-boot/cassandra/src/main/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupport.kt`
- test: `spring-boot/cassandra/src/test/kotlin/io/bluetape4k/spring/cassandra/cql/OptionsSupportTest.kt`
- `Insert`, `Update`, `UpdateStart`, `Delete` 경로와 `WriteOptions.isPositiveTtl`에 `ttl!!`/`timestamp!!`가 존재한다.
- baseline `OptionsSupportTest`는 11개가 모두 통과하지만 zero TTL을 positive로 취급하며 null/negative 및 subtype matrix를 포함하지 않는다.

### 4.2 #1346 Hibernate QueryDSL

- build: `data/hibernate/build.gradle.kts`
- fixture/test: `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/querydsl/simple/ExampleEntity.kt`, `ExampleDto.kt`, `SimpleQuerydslExamples.kt`
- association fixture: `data/hibernate/src/test/kotlin/io/bluetape4k/hibernate/mapping/associations/join/models.kt`
- `querydsl-kotlin-codegen`은 upstream `querydsl/querydsl#3454`와 tree entity 문제를 이유로 주석 처리되어 있다.
- baseline `SimpleQuerydslExamples`는 기존 Java-style QueryDSL 생성물로 5개가 통과한다.

### 4.3 #1357 hibernate-lettuce

- production: `spring-boot/hibernate-lettuce/src/main/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/`
- test: `spring-boot/hibernate-lettuce/src/test/kotlin/io/bluetape4k/spring/boot/autoconfigure/cache/lettuce/LettuceNearCacheAutoConfigurationTest.kt`
- Hibernate/Actuator 구성은 root property를 보지만 Metrics 구성은 `bluetape4k.cache.lettuce-near.metrics.enabled`만 검사한다.
- baseline auto-configuration test 16개는 통과하지만 root=false + metrics=true 조합은 없다.
- README 영어/한국어는 root false가 전체 기능을 끈다고 설명하므로 실제 조건과 parity가 필요하다.

### 4.4 #1358 MongoDB

- production: `spring-boot/mongodb/src/main/kotlin/io/bluetape4k/spring/mongodb/config/ReactiveMongoAutoConfiguration.kt`
- test helpers: `spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/AbstractReactiveMongoTest.kt`, `MongoTestApplication.kt`
- test resource: `spring-boot/mongodb/src/test/resources/application.yml` (bean overriding은 비활성화)
- integration: `spring-boot/mongodb/src/test/kotlin/io/bluetape4k/spring/mongodb/coroutines/ReactiveMongoOperationsCoroutinesTest.kt`
- KDoc와 `DynamicPropertySource`는 `spring.data.mongodb.*`를 사용하고, `MongoTestApplication`은 수동 `AbstractReactiveMongoConfiguration`으로 binding을 가린다.
- baseline `CriteriaExtensionsTest` 25개는 통과했다. MongoDB Testcontainers 통합 검증은 구현 후 별도로 한 번에 실행한다.

## 5. 설계 대안과 선택

### 대안 A — 하나의 Epic 통합 PR

모든 모듈을 한 branch/PR에 넣는다. cross-module diff가 한 번에 보이지만, QueryDSL build 실패나 MongoDB container failure가 다른 계약의 review와 rollback을 막고, PR이 독립적으로 검증되지 않는다. 선택하지 않는다.

### 대안 B — 모듈별 독립 PR을 병렬 진행

각 모듈은 빠르게 review할 수 있지만, Epic에 명시된 #1359 → #1346 → #1357 → #1358 의존성과 predecessor 생성물/검증 결과를 잃는다. 조건·문서·통합 테스트가 동시에 바뀌어 실패 원인도 섞인다. 선택하지 않는다.

### 대안 C — strict linear stacked PR train (선택)

첫 child는 현재 `develop`에서 시작하고 각 후속 PR은 직전 exact head를 base로 한다. 각 slice가 작고 independently testable하며, predecessor merge 뒤 downstream을 최신 `develop`에 restack하고 새 head에서 다시 검증할 수 있다. 명시된 issue dependency와 Testcontainers 순차 실행 규칙을 그대로 보존하므로 선택한다.

## 6. Train 구조와 branch 계약

| 순서 | Head branch | PR base | Issue | 핵심 경계 |
| --- | --- | --- | --- | --- |
| 1 | `fix/1359-cassandra-write-options-nullable` | `develop@a907d144f39bfb94cba783cf65a5412e0714e9d5` | #1359 | Cassandra production/test/KDoc/README |
| 2 | `build/1346-querydsl-kotlin-codegen` | #1359 exact head | #1346 | Hibernate build/fixture/test/docs |
| 3 | `fix/1357-hibernate-lettuce-root-condition` | #1346 exact head | #1357 | auto-configuration/test/README |
| 4 | `fix/1358-mongodb-boot41-boundary` | #1357 exact head | #1358 | auto-configuration/test helpers/docs |

각 PR의 body는 `Fixes #<issue>`와 stack predecessor를 명시하고, PR 생성 시점의 live issue milestone/labels/assignee를 다시 읽어 반영한다. predecessor가 merge되면 downstream을 `develop`에 restack하고 targeted/module/required CI를 새 head에서 다시 실행한다. 예상 remote head가 다르면 force push하지 않고 train을 `PENDING`으로 보존한다.

## 7. Slice별 계약

### 7.1 #1359 — nullable WriteOptions

- `isPositiveTtl`은 Spring Data Cassandra의 기존 `hasTtl` 의미인 `ttl != null && !ttl.isNegative`로 고정한다. null은 미적용, zero는 `USING TTL 0`, 음수는 builder 단계의 명시적 예외로 처리한다.
- `Insert`, `Update`, `UpdateStart`, `Delete`의 기존 statement builder 의미와 subtype guard를 보존한다. Delete는 TTL을 적용하지 않지만 timestamp는 계속 보존한다.
- TTL null/negative/zero/positive × timestamp null/present를 각 applicable subtype에 대해 assertion하고, `Int` 범위를 벗어난 초 값은 `Math.toIntExact` 기반의 명시적 예외로 고정한다.
- production `!!`는 제거하고, KDoc/EN·KO README의 non-negative/overflow 동작과 실제 query 문자열을 일치시킨다. Cassandra timestamp는 microseconds 단위이며 subsecond TTL은 whole seconds로 절삭된다. `isPositiveTtl`은 이름을 유지하되 zero 포함의 historical 의미를 공개한다.
- 먼저 failing test를 작성하고, 테스트가 실제 NPE 또는 잘못된 CQL을 재현하는지 확인한 뒤 최소 구현을 적용한다.

### 7.2 #1346 — QueryDSL codegen 재평가

- data class DTO, 일반 Entity, tree Entity, association/join fixture를 최소 matrix로 나눈다.
- 기존 `QExampleEntity`/`QExampleDto` Java-style 생성물과 Kotlin codegen 후보를 구분해 compile/runtime 결과를 기록한다.
- 지원 가능하면 KAPT 생성 source와 실제 query repository 실행을 검증한다. EN/KO README에는 DTO/일반 Entity/tree Entity/association별 지원 matrix, Java APT fallback Gradle snippet, generated source 위치와 repository path 사용법을 같이 둔다.
- 지원 불가하면 현재 `kapt` 설정을 억지로 켜지 않고, 재현 로그·upstream `#3454`·workaround·후속 추적 항목을 문서화한다.
- 현재 fixture의 stale KDoc/FIXME는 실험 결과와 무관하게 실제 상속·association·equals 제약만 설명하도록 고친다. QueryDSL fork나 전면 model rewrite는 하지 않는다.

### 7.3 #1357 — hibernate-lettuce root condition

- root `bluetape4k.cache.lettuce-near.enabled=false`가 customizer, metrics binder, actuator endpoint를 모두 차단한다.
- root/metrics enabled 2×2 조합과 기본값, optional class absence를 `ApplicationContextRunner`로 검증한다. Metrics KDoc와 EN/KO README는 `root enabled && metrics enabled` 조건 및 `spring-boot-starter-actuator` dependency를 동일하게 설명한다.
- Metrics phase에도 root 조건을 직접 적용하고, auto-configuration import/order를 유지한다.
- 영어/한국어 README의 root disable 설명과 테스트 matrix를 동기화한다.

### 7.4 #1358 — MongoDB Boot 4.1 boundary

- Spring Boot 4.1에서 사용하는 `spring.mongodb.*` property namespace를 binding contract로 고정한다.
- `ReactiveMongoAutoConfiguration`은 Spring Boot의 Mongo auto-configuration 뒤에 적용되도록 명시적 order를 갖는다.
- `ApplicationContextRunner`와 `FilteredClassLoader`로 property binding, `@ConditionalOnClass` absence, default template, 사용자 bean 우선, required bean 부재를 검증한다.
- legacy-only `spring.data.mongodb.uri`는 조용한 localhost fallback을 막는 migration fail-fast로 처리하고, 새 `spring.mongodb.uri`가 함께 있으면 새 key를 우선한다. 예외는 `IllegalStateException("Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+")`로 고정하고 EN/KO README에 before/after migration과 이전 artifact pin rollback을 기록한다. 지원 범위는 Boot 4.1+로 명시하고 저장소 Boot 4.x compatibility matrix를 검증한다.
- test resource와 context runner에서는 bean overriding을 끄고, user `ReactiveMongoOperations`가 fallback 및 Boot 기본 template과 충돌 없이 우선하는지 검증한다.
- `MongoTestApplication`의 수동 connection 설정을 binding test와 분리하고, Testcontainers integration은 `AbstractReactiveMongoTest`에서 순차 실행한다. 공유 서버는 `ShutdownQueue`가 소유하고 context close는 Spring-managed client/template만 닫는다.
- context/lesson/PR evidence에는 synthetic URI만 남기며 credential이 있는 URI와 properties의 `toString()`을 기록하지 않는다.
- KDoc/README와 test helper가 같은 namespace와 lifecycle을 설명한다.

## 8. 실패 모드와 대응

| 실패 모드 | 탐지 방법 | 대응 |
| --- | --- | --- |
| nullable TTL에서 NPE, overflow 또는 잘못된 `USING TTL` 생성 | red test의 query/exception assertion 및 `!!` 검색 | `WriteOptions` nullable 분기만 최소 수정하고 subtype matrix 재실행 |
| QueryDSL KAPT가 tree/일반 Entity 생성에서 실패 | clean compile, generated source 존재, 실제 query test | 지원하지 않는 결론이면 build를 되돌리고 재현/문서/workaround만 남김 |
| root=false인데 Lettuce metrics/endpoint가 등록됨 | 2×2 `ApplicationContextRunner` bean count | 모든 phase에 root condition 추가 후 optional-class test 재실행 |
| 수동 Mongo client가 잘못된 property namespace를 가림 | context runner의 bound environment와 manual config 부재 검사 | binding test와 container integration을 분리하고 order annotation을 직접 검증 |
| predecessor merge 후 downstream diff/CI가 stale | exact head/base SHA, `git range-diff`, live checks | downstream push/PR 업데이트를 멈추고 restack 후 새 head 검증 |

## 9. 검증·문서·운영 계약

- 각 slice는 RED → GREEN → refactor 순서로 진행한다. 테스트가 먼저 실패한 증거와 최소 구현 후 green 결과를 기록한다.
- Kotlin 규칙에 따라 JUnit 5, bluetape4k assertions, `ApplicationContextRunner`, 기존 `MongoDBServer`/공유 fixture를 재사용한다.
- Testcontainers와 실제 DB 검증은 worktree/모듈 사이에서도 동시에 실행하지 않는다.
- 각 slice에서 targeted test → affected module test → detekt/compile → `git diff --check` 순서로 수행한다. Kover는 module behavior가 green인 뒤 report-only로 실행한다.
- 공개 설정 동작이 바뀌는 slice는 `CHANGELOG.md` Unreleased에 호환성/버그 수정/마이그레이션 항목을 남긴다. EN/KO README 핵심 토큰과 설정 예제/표 행은 자동 token 검사와 수동 의미 diff로 parity를 확인한다.
- Type-A 산출물은 다음과 같다.
  - `docs/superpowers/specs/2026-08-26-epic-1420-data-contract-train-design.md`
  - `docs/superpowers/plans/2026-08-26-epic-1420-data-contract-train.md`
  - `docs/lessons/2026-08-26-epic-1420-data-contract-train.md`
- spec/plan에는 각 변경 파일, 테스트 명령, rollback/restack, 문서 parity, unresolved risk를 포함한다.
- PR 전에는 Type-A 여섯 review perspective와 독립 integration review를 수행하고 P0/P1을 0으로 수렴한다.
- PR body는 한국어로 작성하고 마지막에 `## DoD Status`를 둔다. PR 생성 후 exact head, required checks, reviews/threads, mergeability를 live-read한다.
- merge/tag/release/branch cleanup은 이 설계의 실행 승인으로 자동 허가되지 않는다. merge-ready DoD 뒤 fresh merge approval을 별도로 받는다.

## 10. Train 완료 조건

1. 네 미착수 child가 올바른 predecessor와 각각 독립 PR로 연결되고 모든 child가 `2.0.0`에 유지된다.
2. 각 PR의 exact head에서 targeted/module/static checks가 green이며, Testcontainers evidence가 순차 실행으로 재현된다.
3. 모든 public/internal KDoc와 README locale이 실제 behavior와 일치한다.
4. spec/plan/lesson과 review findings가 committed 상태이고 P0/P1=0, Blocked=0이다.
5. Epic 본문/checkbox와 child/PR live metadata가 최종 상태와 일치한다.
6. merge-ready report를 사용자에게 제시한 뒤에만 다음 merge approval gate로 이동한다.

## 11. 승인 게이트

- 이 문서와 후속 implementation plan이 승인되기 전에는 code, branch push, PR, issue 관계/본문을 변경하지 않는다.
- spec self-review에서 placeholder, 모순, 범위 누락을 제거한 뒤 문서 commit을 만든다.
- 사용자가 committed spec을 검토하고 승인한 뒤 implementation plan을 작성한다.
- plan 승인 후에만 slice 구현·PR 생성을 수행한다. PR 생성 권한은 이 train의 repository/base/head가 명시된 승인된 plan으로 충족한다.
- 모든 PR이 merge-ready가 된 뒤 정확한 PR/head에 대한 fresh merge approval을 다시 받는다.
