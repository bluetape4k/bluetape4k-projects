# Vert.x 4.5.26 → 5.0.11 업그레이드 설계 문서

- **작성일**: 2026-04-28
- **이슈**: GitHub #197 — `feat: Vert.x 4.x → 5.x 업그레이드`
- **작업 브랜치**: `feat/vertx5-upgrade` (worktree: `.worktrees/feat-vertx5-upgrade`)
- **상태**: Design (Spec) — Plan 작성 대기
- **타겟 버전**: Vert.x **5.0.11** (latest stable GA, 2025-05 릴리즈)
- **현재 버전**: Vert.x 4.5.26 (`buildSrc/src/main/kotlin/Libs.kt:91`)

---

## 1. Background

### 1.1 현재 상태

`bluetape4k-projects` 는 `io/vertx` 모듈을 중심으로 Vert.x 기반 SQL Client / Web Client / JUnit5 통합을 제공합니다. 그러나 다음 모듈들이 Vert.x 의존성에 직간접적으로 결합되어 있습니다.

| 모듈 | 결합 방식 | Vert.x 의존성 종류 |
|------|----------|--------------------|
| `io/vertx` | **owner module** | `core`, `lang-kotlin`, `lang-kotlin-coroutines`, `web`, `web-client`, `junit5`, `sql-client(+templates)`, `mysql-client`, `pg-client`, `jdbc-client` |
| `data/hibernate-reactive` | `api(project(":bluetape4k-vertx"))` + `Libs.hibernate_reactive_core (3.2.0.Final)` | Hibernate Reactive 가 Vert.x SQL Client 위에서 동작 |
| `infra/micrometer` | `implementation(Libs.vertx_core)` | Micrometer Vert.x metrics |
| `io/feign`, `io/retrofit2`, `io/http` | `compileOnly` 로만 사용 | 옵셔널 통합 (사용자가 클래스패스에 vertx 추가 시 활성) |

또한 `x-obsoleted/` 하위에 **3개의 구식 vertx-* 디렉토리** (vertx-coroutines, vertx-sqlclient, vertx-webclient — 합계 14개 예제 파일) 가 남아 있습니다.

### 1.2 업그레이드 동기

1. **API 정리**: Vert.x 5 는 `xxxAwait()` 계열 deprecated 메서드를 완전히 제거하고 `coAwait()` 만 남겼습니다. 본 프로젝트 코드는 이미 `coAwait()` 로 마이그레이션된 상태이므로 4.5 의 deprecated 코드 경로를 유지할 이유가 사라졌습니다.
2. **모듈 슬림화**: Vert.x 5 에서 `vertx-jdbc-client` 모듈이 삭제되었습니다. Reactive 환경에서 JDBC 를 강제할 이유가 없어졌고, 본 프로젝트도 `compileOnly` 로만 노출하던 상태이므로 정리 적기입니다.
3. **Hibernate Reactive 4.3 호환**: HR 4.3.x 시리즈는 Vert.x 5 + ORM 7.3.x 를 요구합니다. 본 프로젝트는 이미 Hibernate ORM 7.2.7 을 사용 중이므로(`Libs.kt`) 업그레이드 폭이 적습니다.
4. **장기 유지보수**: Vert.x 4.x 는 LTS 종료 시점에 가까워졌으며, 5.x 가 2025-05 GA 이후 안정 트랙으로 자리잡았습니다.
5. **Java 21 / Loom 친화**: Vert.x 5 는 Virtual Thread 통합이 강화되었습니다. 본 프로젝트의 `virtualthread/*` 자산과 시너지가 있습니다.

### 1.3 검증 완료된 사실 (Research Findings)

| 항목 | 검증 결과 | 영향 |
|------|----------|------|
| Vert.x 5.0.11 GA 존재 | Maven Central 확인 | Libs.kt 버전 한 줄 교체 |
| `resilience4j_vertx` 가 build.gradle.kts 에서 미참조 | `rg "resilience4j_vertx"` 0건 | `Libs.kt`에서 라인 삭제만 하면 됨 |
| `vertx-jdbc-client` **5.0.11 GA 존재** | Maven Central 직접 확인 — `vertx-jdbc-client:5.0.11` 정상 릴리즈 | 버전 자동 반영만 (코드/빌드 변경 불필요) |
| `SqlResultSupport.kt` 가 `JDBCPool.GENERATED_KEYS` 를 production 코드에서 사용 | `rg JDBCPool io/vertx/src/main/kotlin` 확인 | vertx-jdbc-client 계속 유지 — 제거 불가 |
| Hibernate Reactive 3.2.0.Final 은 **Vert.x 4.5 전용** | HR 릴리즈 노트 | HR 을 **4.3.3.Final** 로 동반 업그레이드 필요 |
| Hibernate Reactive **4.3.3.Final** 이 Vert.x 5.0 + ORM **7.3.2.Final** 요구 | Maven Central HR 4.3.3.Final POM 직접 확인 | ORM 7.2.7 → **7.3.2.Final** 범프 (프로젝트 전역 적용됨) |
| 기존 코드는 이미 `.coAwait()` 사용 중 | `rg coAwait` ✅ | API 마이그레이션 비용 거의 없음 |
| `testContext.succeeding {}` 패턴 1곳 잔존 | `VertxJunit5Examples.kt` 라인 60, 94 | `succeedingThenComplete()` 변환 또는 람다 시그니처 점검 필요 |
| `CompositeFuture` 미사용 | `rg CompositeFuture` 0건 | 5.x 의 `Future.all()` 로 대체할 코드 없음 |
| Pool builder 패턴은 이미 5.x 호환 | `PoolSupport.kt` 검사 | 변경 불필요 |

---

## 2. Scope

### 2.1 In-Scope (변경 대상)

| 모듈 / 파일 | 변경 종류 | 예상 파일 수 |
|------------|----------|------------|
| `buildSrc/src/main/kotlin/Libs.kt` | 버전 상수 `vertx 4.5.26 → 5.0.11`, `hibernate_reactive 3.2.0.Final → 4.3.3.Final`, `hibernate 7.2.7.Final → 7.3.2.Final`, `resilience4j_vertx` 삭제 | 1 파일 |
| `io/vertx/src/**` | API 마이그레이션 점검(이미 90% 호환) — `succeeding`/`succeedingThenComplete` 1~2곳, deprecated 옵션 setter 점검 | ~3-5 파일 |
| `io/vertx/README.md` + `README.ko.md` | 버전 표기, Vert.x 5.0.11 호환성 명기 | 2 파일 |
| `data/hibernate-reactive/build.gradle.kts` | HR 4.3.3.Final 사용 + ORM 7.3.2.Final, Netty 해결 전략 추가 검토 | 1 파일 |
| `data/hibernate-reactive/src/**` | ORM 7.2 → 7.3 호환 점검 (minor; API 거의 동일) | ~0-3 파일 |
| `data/hibernate-reactive/README.md` + `README.ko.md` | 호환 매트릭스 갱신 | 2 파일 |
| `infra/micrometer/build.gradle.kts` | 버전만 자동 반영 (코드 수정 불필요 예상) | 의존성만 |
| `infra/micrometer/README.md` + `README.ko.md` | 버전 표기 갱신 (CLAUDE.md — 모든 변경 모듈 README 필수) | 2 파일 |
| `x-obsoleted/vertx-coroutines/`, `vertx-sqlclient/`, `vertx-webclient/` | **디렉토리 삭제** | 14 파일 + 디렉토리 |

### 2.2 Out-of-Scope (이번 PR에서 제외)

- **`io/feign`, `io/retrofit2`, `io/http`**: `compileOnly(Libs.vertx_core)` 만 사용. 버전 상수 변경으로 자동 반영되며 코드 수정 불필요. (의존성 그래프만 검증)
- **새로운 Vert.x 5 기능 도입** (예: HTTP/2 우선, gRPC-web, Virtual Thread Verticle): 이번 PR 은 *순수 업그레이드* 만 다루며, 신기능 활용은 **후속 issue 로 분리**합니다.
- **`infra/resilience4j` 와 vertx 통합**: `resilience4j_vertx` 라인은 *과거 미사용 잔여물* 이므로 **제거만** 하고 통합 자체는 도입하지 않음.
- **Vert.x 5 `EventBusOptions` / Cluster Manager API 재설계**: 본 프로젝트는 단일-JVM 사용만 가정. Cluster 관련 변경 없음.

---

## 3. Architecture Decision

### 3.1 결정: 단일 트랙 in-place 업그레이드

> Vert.x 5 로 **한 번에** 올린다. Vertx4 / Vertx5 병행 모듈을 두지 않는다.
> Hibernate Reactive 도 **같은 PR 내에서** 4.3.x 로 동반 업그레이드한다.

### 3.2 근거

1. **Spring Boot platform 과 독립적**: 본 프로젝트의 Vert.x 사용은 transport 계층이며 Spring Boot 3/4 platform 으로부터 버전을 강제받지 않습니다. (Spring Boot dependencies BOM 이 vertx 를 관리하지 않음 — `Libs.kt` 에서 직접 핀)
2. **소비자 영향 범위가 제한적**: Vert.x 를 직접 노출하는 모듈은 `io/vertx` + `data/hibernate-reactive` 둘뿐이고, 나머지는 `compileOnly` 또는 `implementation`. **API breaking 영향이 좁음**.
3. **HR 의 lock-step 의존성**: HR 3.2 ↔ Vert.x 4.5 / HR 4.3 ↔ Vert.x 5.0 매트릭스가 strict. 두 라이브러리를 분리해서 단계적으로 업그레이드하는 것은 *어느 한쪽에서 컴파일 불가* 를 만듭니다. 따라서 **원자적 PR** 이 정답.
4. **이미 코드는 5.x 호환에 가까움**: `.coAwait()` 마이그레이션이 이미 끝났고, Pool builder 패턴도 5.x 스타일을 따릅니다. 추가 리팩토링 비용이 매우 낮음.
5. **유지보수 단순성**: 두 트랙(`vertx4` vs `vertx5` 모듈) 분리는 향후 모든 이슈에 대해 양쪽을 모두 수정해야 하는 부담을 안깁니다. 사용자가 적은 옵셔널 모듈에서 이 비용은 비합리적.

### 3.3 모듈 구조 (변경 후)

```
io/vertx/                  -- Vert.x 5.0.11 only (single track)
  └── src/main/kotlin/io/bluetape4k/vertx/...

data/hibernate-reactive/   -- Hibernate Reactive 4.3.x (ORM 7.3.x) + Vertx 5
  └── src/main/kotlin/io/bluetape4k/hibernate/reactive/...

x-obsoleted/               -- vertx-* 3개 디렉토리 삭제
```

---

## 4. Breaking Changes to Handle

Vert.x 4.5 → 5.0 전환에서 본 프로젝트 코드에 영향이 있는 항목만 추립니다 (전체 changelog 가 아님).

### 4.1 `vertx-jdbc-client` 모듈 — 버전 자동 반영 (제거 불가)

| 항목 | 4.5.26 | 5.0.11 |
|------|--------|--------|
| 모듈 존재 여부 | `io.vertx:vertx-jdbc-client` 정상 GA | **5.0.11 GA 정상 존재** (Maven Central 확인) |
| 본 프로젝트 production 사용 | `io/vertx/src/main/kotlin/.../SqlResultSupport.kt:3` — `import io.vertx.jdbcclient.JDBCPool` + `.property(JDBCPool.GENERATED_KEYS)` | 계속 유지 |
| 본 프로젝트 test 사용 | `AbstractVertxSqlClientTest.kt:10-11,73` — `JDBCConnectOptions`, `JDBCPool.pool()` | 계속 유지 |
| 처리 방안 | **변경 없음** — `Libs.vertx_jdbc_client` 정의 유지, build.gradle.kts 유지. Vert.x 버전 범프로 자동 반영 |
| 호환성 위험 | `JDBCPool.GENERATED_KEYS` 상수 + `JDBCPool.pool()` API 가 5.x 에서 시그니처 변경 여부는 T0.5 컴파일로 검증 |

### 4.2 `xxxAwait()` deprecated 메서드 제거 → `coAwait()` 만 사용

| 항목 | 상태 |
|------|------|
| 현재 코드 | 이미 전부 `coAwait()` 사용 중 (`rg` 검증 완료) |
| 처리 | **추가 작업 없음** (regression 방지를 위해 grep 검증만) |

### 4.3 `VertxTestContext.succeeding {}` 람다 시그니처

| 항목 | 4.5 | 5.0 |
|------|-----|-----|
| `succeeding(Handler<T>)` | 존재 | **deprecated** (`succeedingThenComplete` 권장) |
| `succeedingThenComplete()` | 존재 | 권장 |
| 본 프로젝트 영향 | `io/vertx/src/test/kotlin/io/bluetape4k/vertx/examples/VertxJunit5Examples.kt:60, 94` 의 `testContext.succeeding { buffer -> ... }` 2곳 |
| 처리 방안 | 어설션 후 `testContext.completeNow()` 명시 호출 또는 `succeedingThenComplete()` 로 분기 정리. **deprecated 경고만 발생하므로 컴파일 영향은 없음** — 5.0 에서 메서드가 제거되었는지 릴리즈 노트로 재확인 후 확정 |
| `LifecycleExamples.kt:63` 의 `succeeding<Unit>` | 동일 정책 적용 |

### 4.4 `HttpClientOptions.setMaxPoolSize()` API 점검

| 항목 | 상태 |
|------|------|
| 본 프로젝트 사용 | `rg setMaxPoolSize` 결과 0건 (코드 검색에서 확인) |
| 처리 | **불필요** — 사용처 없음 |

### 4.5 `CompositeFuture` → `Future.all()` / `Future.any()` / `Future.join()`

| 항목 | 4.5 | 5.0 |
|------|-----|-----|
| `CompositeFuture.all(...)` | 정상 | deprecated, `Future.all(...)` 권장 |
| 본 프로젝트 영향 | `rg CompositeFuture` 0건 → **변경 없음** |

### 4.6 Hibernate Reactive 3.2 → 4.3 + ORM 7.2 → 7.3

| 항목 | 변경 내용 |
|------|----------|
| `Versions.hibernate_reactive` | `3.2.0.Final` → **`4.3.3.Final`** (Maven Central HR 4.3.3.Final POM 직접 확인) |
| `Versions.hibernate` (ORM) | `7.2.7.Final` → **`7.3.2.Final`** (HR 4.3.3.Final POM 의 hard dep: `hibernate-core:7.3.2.Final`) |
| **전역 영향 주의**: `Versions.hibernate` 변경은 `Libs.kt` 를 참조하는 **모든 hibernate 모듈**에 적용됨 — `data/hibernate`, `spring-boot3/`, `spring-boot4/` 등 포함. ORM 7.2→7.3 API 호환성 가정 전에 각 모듈 컴파일 검증 필수 | — |
| `jakarta.persistence` 강제 버전 | 현재 3.2.0 강제 (`build.gradle.kts:46`) — **HR 4.3 도 동일 요구**. 변경 불필요 |
| Netty 충돌 위험 | `data/hibernate-reactive` 가 `platform(Libs.spring_boot3_dependencies)` 를 임포트 — Spring Boot 3.5.x 는 Netty 4.1.x 를 관리함. Vert.x 5.0.11 은 Netty **4.2.12.Final** 요구. `resolutionStrategy.eachDependency { if (group == "io.netty") useVersion("4.2.12.Final") }` 추가 필요 검토 |
| Code 영향 | ORM 7.2 → 7.3 minor: API 호환 (Internal SPI 만 변경). 본 프로젝트의 `mutiny`/`stage` SessionFactorySupport 코드는 표준 API 만 사용 → 영향 없음 예상 |

### 4.7 `resilience4j_vertx` 정리

| 항목 | 처리 |
|------|------|
| `Libs.kt` 정의 | 라인 삭제 |
| 어떤 build.gradle.kts 도 참조 안 함 | `rg resilience4j_vertx` 결과 `Libs.kt` 외 0건 — 안전한 정리 |

### 4.8 `x-obsoleted/vertx-*` 디렉토리

| 디렉토리 | 파일 수 | 처리 |
|---------|--------|------|
| `x-obsoleted/vertx-coroutines/` | build.gradle.kts + README ko/en + src/ 예제 | **삭제** |
| `x-obsoleted/vertx-sqlclient/` | 동일 구조 | **삭제** |
| `x-obsoleted/vertx-webclient/` | 동일 구조 | **삭제** |
| 사유 | 이미 `x-obsoleted/` 는 publish 대상에서 제외된 examples-only 디렉토리. Vert.x 5 와 호환되지 않는 4.x 예제 코드를 유지할 가치 없음. `settings.gradle.kts` 에 등록되지 않으므로 빌드 영향 0 |

---

## 5. Implementation Phases

### Phase T0 — 버전 범프 + 컴파일 ("Big Bang Lift")

**목표**: 의존성만 바꿔서 *얼마나 깨지는지* 측정.

| 단계 | 작업 | DoD |
|------|------|-----|
| T0.1 | `buildSrc/Libs.kt` 수정: `vertx 4.5.26 → 5.0.11`, `hibernate_reactive 3.2.0.Final → 4.3.3.Final`, `hibernate 7.2.7.Final → 7.3.2.Final`, `resilience4j_vertx` val 삭제 | git diff 1 파일 |
| T0.2 | `data/hibernate-reactive/build.gradle.kts` — `dependencies { runtimeOnly("io.netty:netty-all:4.2.12.Final") }` 또는 `resolutionStrategy` Netty 핀 추가 (SB3 BOM이 Netty 4.1.x 을 끌어내릴 수 있음) | 의존성 트리 확인: `./gradlew :bluetape4k-hibernate-reactive:dependencies | rg netty` |
| T0.3 | `./gradlew :bluetape4k-vertx:compileKotlin` | 컴파일 통과 또는 에러 목록 수집 |
| T0.4 | `./gradlew :bluetape4k-hibernate-reactive:compileKotlin` | 동일 |
| T0.5 | `./gradlew :bluetape4k-micrometer:compileKotlin` | 동일 |
| T0.6 | ORM 7.3 전역 영향 확인: `./gradlew :bluetape4k-hibernate:compileKotlin :bluetape4k-spring-boot3-hibernate-lettuce:compileKotlin` | 에러 없음 확인 |
| T0.7 | `./gradlew :bluetape4k-feign:compileKotlin :bluetape4k-retrofit2:compileKotlin :bluetape4k-http:compileKotlin` | compileOnly 만 사용하므로 영향 0 예상 |

**T0 종료 조건**: 모든 영향 모듈이 *컴파일* 통과. (테스트는 T2)

### Phase T1 — API 마이그레이션 (필요 시)

T0 에서 발견된 컴파일 에러를 항목별로 처리합니다. 사전에 식별된 항목:

| 단계 | 작업 | 산출물 |
|------|------|--------|
| T1.1 | `io/vertx/src/test/.../VertxJunit5Examples.kt` 의 `succeeding { ... }` → `succeedingThenComplete()` 또는 `assertThat(...).also { testContext.completeNow() }` 변환 | 1 파일 |
| T1.2 | `io/vertx/src/test/.../LifecycleExamples.kt` 의 `succeeding<Unit>` 점검 | 1 파일 |
| T1.3 | `vertx-jdbc-client` API 호환성 점검 — `JDBCPool.GENERATED_KEYS`, `JDBCPool.pool()` 5.0.11 시그니처 변경 여부. T0.3 컴파일 시 발견된 에러 기준으로 패치 | 0~2 파일 |
| T1.4 | T0 에서 추가로 드러난 deprecation/API 변경 패치 (예: `HttpServerOptions`, `WebClientOptions` setter 시그니처 변동 시) | 발생 시 N 파일 |
| T1.5 | `data/hibernate-reactive` ORM 7.3 minor 호환 점검 (`SessionFactorySupport.kt` 등) | 0~3 파일 |
| T1.6 | `x-obsoleted/vertx-coroutines`, `vertx-sqlclient`, `vertx-webclient` 디렉토리 **삭제** | 3 디렉토리 |

### Phase T2 — 테스트 + 검증

| 단계 | 작업 | DoD |
|------|------|-----|
| T2.1 | `./gradlew :bluetape4k-vertx:test` | 전 테스트 통과 (현 baseline 과 동일 또는 향상) |
| T2.2 | `./gradlew :bluetape4k-hibernate-reactive:test` | Testcontainers MySQL 시나리오 포함 전수 통과 |
| T2.3 | `./gradlew :bluetape4k-micrometer:test :bluetape4k-feign:test :bluetape4k-retrofit2:test :bluetape4k-http:test` | regression 없음 |
| T2.4 | `./gradlew detekt` (영향 모듈) | 신규 위반 0건 |
| T2.5 | README.md / README.ko.md 갱신 (`io/vertx`, `data/hibernate-reactive`) | 버전 표 + JDBC 클라이언트 제거 안내 + 호환 매트릭스 |
| T2.6 | `./gradlew clean build -x test` 전체 (smoke 만 — 영향 없는 모듈 ABI 비파괴 확인) | 성공 |
| T2.7 | `./gradlew check` (영향 모듈) | 통과 |
| T2.8 | KDoc — 새로 노출되는 5.x API 가 있다면 KDoc 보강 (없으면 skip) | 필요 시 작성 |

---

## 6. Definition of Done (DoD)

PR 머지 전 다음 모두 만족해야 합니다.

- [ ] `Libs.kt` 의 `Versions.vertx = "5.0.11"` 적용
- [ ] `Libs.vertx_jdbc_client` val 삭제 + 모든 build.gradle.kts 에서 미참조
- [ ] `Libs.resilience4j_vertx` val 삭제 + 미참조 확인
- [ ] `Libs.hibernate_reactive` 4.3.x 적용 (Vert.x 5.0 호환 patch)
- [ ] `Versions.hibernate` 7.3.x 적용 (HR 4.3 매트릭스 충족)
- [ ] `io/vertx`, `data/hibernate-reactive`, `infra/micrometer`, `io/feign`, `io/retrofit2`, `io/http` 모두 Vert.x 5.0.11 클래스패스 위에서 컴파일 통과
- [ ] 위 모든 모듈에서 기존 테스트 전수 통과 (regression gate)
- [ ] `x-obsoleted/vertx-coroutines/`, `vertx-sqlclient/`, `vertx-webclient/` 디렉토리 삭제
- [ ] `io/vertx/README.md` + `README.ko.md` 갱신 (Vert.x 5 명기, JDBC 제거 안내)
- [ ] `data/hibernate-reactive/README.md` + `README.ko.md` 갱신 (HR 4.3 + Vert.x 5 + ORM 7.3 호환 매트릭스)
- [ ] 루트 `CLAUDE.md` 의 Vert.x 관련 언급(있다면) 갱신
- [ ] `/wiki-update` 스킬 실행으로 wiki/QMD 색인 동기화
- [ ] `oh-my-claudecode:code-reviewer` 실행 후 HIGH/CRITICAL 이슈 해소
- [ ] PR 본문에 테스트 결과(passing count + duration), 변경 근거, 검증 명령 기재

---

## 7. Risk Analysis

### 7.1 Risk: Hibernate Reactive 4.3 가 ORM 7.3 마이너 변경에서 본 프로젝트의 SessionFactory 패턴을 깸

- **확률**: 낮음 (현재 코드가 표준 API `Stage.SessionFactory` / `Mutiny.SessionFactory` 만 사용)
- **영향**: 중 (`hibernate-reactive` 모듈 컴파일/런타임 실패)
- **선행 시그널**: T0.6 단계의 컴파일 결과
- **완화책**:
  1. T0 단계에서 빠르게 컴파일 검증 → 실패 시 ORM 7.3 → 7.2 유지하고 HR 4.3 의 ORM 7.2 호환 patch 가 있는지 확인
  2. HR 릴리즈 노트의 deprecation 목록을 사전 점검 후 spec 부록에 첨부 (Plan 단계에서 수행)
  3. 최후의 보루: HR 만 별도 issue 로 분리하고 Vert.x 5 + HR 3.2 (Vertx 4.5 호환만 표방하지만 가끔 5.0 도 동작) 호환성 시도. **단, 공식 비호환이므로 우선순위 낮음**.

### 7.2 Risk: `vertx-jdbc-client` 제거로 다운스트림 사용자 빌드가 깨짐

- **확률**: 매우 낮음 (`io/vertx` 가 `compileOnly` 로만 노출했으므로 transitive 영향 없음)
- **영향**: 중 (외부 사용자가 본 모듈 + agroal-pool 조합으로 자체 wrapping 한 경우 빌드 실패)
- **완화책**:
  1. CHANGELOG / README 에 **Breaking** 표기와 마이그레이션 가이드 (`bluetape4k-jdbc` + agroal 사용법) 명시
  2. baseVersion 을 `1.7.0` → `1.8.0` 으로 minor 범프 (semver: 트랜지티브 의존성 제거 = breaking)

### 7.3 Risk: Vert.x 5 의 `EventBus` / `MessageCodec` 시그니처 미세 변경 누락

- **확률**: 낮음 (본 프로젝트 코드에서 EventBus 직접 사용 검색 결과 미미)
- **영향**: 저 (테스트로 잡히지 않으면 런타임 ClassNotFoundException 위험)
- **완화책**:
  1. T2.1 테스트 단계에서 EventBus 관련 테스트가 있는지 별도 체크 (`rg "eventBus\|EventBus"` 영향 모듈 한정)
  2. 통합 smoke 테스트 추가가 필요하다면 Plan 단계에서 식별

### 7.4 Risk: Vert.x 5 의 Netty 버전이 본 프로젝트의 `bluetape4k-netty` 또는 `infra/grpc` Netty 버전과 충돌

- **확률**: 중 (Vert.x 5 는 Netty 4.1.117+ 또는 4.2 라인 사용)
- **영향**: 중 (런타임 NoSuchMethodError 가능)
- **완화책**:
  1. `./gradlew :bluetape4k-vertx:dependencies | rg netty` 로 해석된 Netty 버전 확인
  2. 충돌 시 `Libs.kt` 의 `netty_*` 버전을 vertx 5 가 요구하는 라인으로 정렬
  3. T2.6 의 `clean build` 가 Netty 충돌을 잡아냄

### 7.5 Risk: Spring Boot 3/4 platform BOM 이 vertx 트랜지티브를 강제로 끌어 4.x 로 다운그레이드

- **확률**: 매우 낮음 (Spring Boot dependencies BOM 은 vertx 를 관리하지 않는 것이 일반적이지만 platform 정책에 따라 변동 가능)
- **영향**: 중 (`hibernate-reactive` 가 platform 사용 중이므로 검증 필수)
- **완화책**:
  1. T0.6 직후 `./gradlew :bluetape4k-hibernate-reactive:dependencies | rg vertx-core` 로 해석 버전 확인
  2. 필요 시 `data/hibernate-reactive/build.gradle.kts` 의 `configurations.all { resolutionStrategy.eachDependency { ... } }` 블록에 vertx 5.0.11 강제 룰 추가

---

## 8. Approach Comparison

세 가지 대안을 비교했습니다.

### 접근 A — 단일 PR in-place 업그레이드 (✅ 채택)

- **방법**: Vert.x 5.0.11 + HR 4.3.x + ORM 7.3.x 를 한 PR 에 묶어 모든 영향 모듈을 동시에 갱신
- **장점**:
  - HR ↔ Vertx lock-step 매트릭스를 한 번에 만족 → 중간 *broken state* 가 없음
  - 사용자/소비자 입장에서 release 1회로 끝남 (semver 1.8.0 한 번)
  - 기존 코드의 `.coAwait()` 마이그레이션이 끝나 있어 비용 낮음
  - x-obsoleted 정리, JDBC 클라이언트 정리 등 housekeeping 동시 처리
- **단점**:
  - PR 크기가 다소 큼 (모듈 6개 + Libs.kt + x-obsoleted 삭제) — 단, 코드 변경량은 의외로 적음 (의존성 위주)
  - 한 번에 깨지면 디버깅 표면적이 넓음 → Phase T0 단위 컴파일 검증으로 완화

### 접근 B — Vertx4 / Vertx5 병행 모듈 (vertx5-core 신규)

- **방법**: 기존 `io/vertx` 는 4.5 유지, `io/vertx5` 모듈 신규 생성. 점진적으로 사용자 이전.
- **장점**: 사용자가 자기 페이스로 마이그레이션 가능
- **단점**:
  - 모듈 2배 (`io/vertx5`, `data/hibernate-reactive5` 등) → 유지보수 폭증
  - HR 라이브러리 자체가 lock-step 이므로 Vertx4+HR3 / Vertx5+HR4 양쪽 모두 검증해야 함
  - 추후 모든 이슈/PR 에서 양쪽 모두 패치해야 함
  - 옵셔널/소수 사용자 모듈에 이 비용은 비합리적
- **결과**: ❌ 비채택. 비용 대비 효익 부재.

### 접근 C — 다단계 PR (vertx5 먼저, HR 4.3 별도 PR)

- **방법**: PR1) Vert.x 5 만 적용 (HR 3.2 그대로) → PR2) HR 4.3 만 별도 적용
- **장점**: PR 단위가 작아짐
- **단점**:
  - **lock-step 깨짐**: HR 3.2 는 Vertx 5 와 공식 비호환 → PR1 시점에서 `bluetape4k-hibernate-reactive` 가 컴파일/런타임 실패 → develop 브랜치가 일시적으로 broken
  - CI 파이프라인이 PR1 시점에서 빨간불 → 머지 불가
  - PR1 머지 못 하면 PR2 도 막힘
- **결과**: ❌ 비채택. 매트릭스 위반.

### 결론

> **접근 A (단일 PR in-place)** 가 본 프로젝트 규모와 의존성 매트릭스에 최적.
> Phase T0 의 빠른 컴파일 검증이 *디버깅 표면적 증가* 라는 단일 단점을 충분히 상쇄합니다.

---

## 9. 부록: 사전 검증 명령 (Plan 단계 첫 작업)

```bash
# Vert.x 5.0.11 GA artifact 존재 확인
httpie GET https://repo1.maven.org/maven2/io/vertx/vertx-core/5.0.11/ | rg "vertx-core-5.0.11.pom"

# vertx-jdbc-client 5.x 부재 확인
httpie GET https://repo1.maven.org/maven2/io/vertx/vertx-jdbc-client/ | rg "5\."

# HR 4.3 latest patch 확인
httpie GET https://repo1.maven.org/maven2/org/hibernate/reactive/hibernate-reactive-core/ | rg "4\.3\."

# 본 프로젝트의 잔여 vertx 4.x 패턴 검색
rg "xxxAwait\(|CompositeFuture|JDBCClient" --glob '*.kt'

# 영향 모듈 의존성 그래프 (T0 직후 실행)
./gradlew :bluetape4k-vertx:dependencies | rg -E "vertx-core|netty"
./gradlew :bluetape4k-hibernate-reactive:dependencies | rg -E "vertx-core|hibernate-(reactive|core)"
```

---

## 10. 후속 작업 (이번 PR 외부, 별도 issue)

이번 업그레이드 PR 머지 *이후* 별도 이슈로 다룰 항목:

1. **Vert.x 5 Virtual Thread Verticle 도입**: `virtualthread/jdk21`, `jdk25` 와 통합한 `VirtualThreadVerticle` 헬퍼
2. **`Future.all()/any()/join()` DSL 정비**: 본 프로젝트에 새 헬퍼 도입 가치 평가
3. **Vert.x 5 gRPC-web 통합**: `infra/grpc` 와의 시너지 검토
4. **HTTP/2 우선순위 / Hedged request 패턴**: Vert.x 5 신규 API 활용

이상 4개 항목은 **본 PR 의 DoD 와 무관** — 단순 업그레이드만 완료한다.

---

## 11. 변경 요약 (한 화면 요약)

| 영역 | Before | After |
|------|--------|-------|
| Vert.x | 4.5.26 | 5.0.11 |
| Hibernate Reactive | 3.2.0.Final | 4.3.x (latest patch) |
| Hibernate ORM | 7.2.7.Final | 7.3.x |
| `vertx-jdbc-client` | compileOnly 노출 | 제거 |
| `resilience4j_vertx` Libs.kt 정의 | 미사용 잔존 | 제거 |
| `x-obsoleted/vertx-*` | 3 디렉토리 14 파일 | 삭제 |
| `succeeding {}` 패턴 | 4.5 deprecated | `succeedingThenComplete()` 또는 명시적 `completeNow()` |
| `coAwait()` 사용 | 이미 적용됨 ✅ | 유지 |
| baseVersion | 1.7.0 | 1.8.0 (semver: breaking — JDBC 클라이언트 제거) |

---

**Spec 종료. Plan 작성으로 진행 가능.**
