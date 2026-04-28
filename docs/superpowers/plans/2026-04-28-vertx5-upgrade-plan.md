# Vert.x 4.5.26 → 5.0.11 업그레이드 구현 계획

- **작성일**: 2026-04-28
- **이슈**: GitHub #197 — `feat: Vert.x 4.x → 5.x 업그레이드`
- **작업 브랜치**: `feat/vertx5-upgrade` (worktree: `.worktrees/feat-vertx5-upgrade/`)
- **연계 Spec**: `docs/superpowers/specs/2026-04-28-vertx5-upgrade-design.md`
- **타겟 버전**: Vert.x **5.0.11** · Hibernate Reactive **4.3.3.Final** · Hibernate ORM **7.3.2.Final**
- **현재 버전**: Vert.x 4.5.26 · HR 3.2.0.Final · ORM 7.2.7.Final
- **baseVersion**: `1.8.0` (이미 적용 — semver 추가 범프 불필요)

---

## 0. 검증 완료 사실 (Plan 진입 전 고정)

Spec 작성 후 추가 검증으로 다음 항목이 확정되었으며, **본 Plan 의 모든 작업은 아래 사실을 위배하지 않는다**.

| # | 사실 | 근거 |
|---|------|------|
| F1 | Vert.x **5.0.11** 이 최신 stable GA | Maven Central 직접 확인 |
| F2 | `vertx-jdbc-client:5.0.11` GA **존재** — Vert.x 5 에서 제거되지 **않음** | Maven Central POM 확인 |
| F3 | `SqlResultSupport.kt` (production) 가 `JDBCPool.GENERATED_KEYS` 사용 | `io/vertx/src/main/kotlin/.../SqlResultSupport.kt` |
| F4 | `AbstractVertxSqlClientTest.kt` (test) 가 `JDBCPool.pool()` 사용 | `io/vertx/src/test/kotlin/.../AbstractVertxSqlClientTest.kt` |
| F5 | `Libs.vertx_jdbc_client` 정의 + `build.gradle.kts` 참조 → **유지** | F3, F4 |
| F6 | HR **4.3.3.Final** = exact target | Maven Central HR POM |
| F7 | HR 4.3.3.Final POM hard-dep `hibernate-core:7.3.2.Final` → ORM **7.3.2.Final** = exact target | HR POM |
| F8 | `Versions.hibernate` 변경은 `data/hibernate`, `spring-boot3/`, `spring-boot4/` 등 **프로젝트 전역** 적용 | `Libs.kt` 의존 트리 |
| F9 | `resilience4j_vertx` 는 `Libs.kt` 외 어떤 build.gradle.kts 에서도 미참조 | `rg resilience4j_vertx` 0건 |
| F10 | Spring Boot 3 BOM 은 Netty **4.1.x** 관리 / Vert.x 5 는 Netty **4.2.12.Final** 요구 → `data/hibernate-reactive` 에 명시 핀 필요 | Spring Boot 3.5.x BOM, Vert.x 5.0.11 POM |
| F11 | `succeeding(Handler<T>)` 람다형은 5.x 에서도 **유지** (zero-arg 형만 제거) | Vert.x 5 JUnit5 모듈 changelog |
| F12 | `x-obsoleted/vertx-coroutines/`, `vertx-sqlclient/`, `vertx-webclient/` 는 `settings.gradle.kts` 에 미등록 → **빌드 무관 14 파일 삭제 가능** | `settings.gradle.kts` 직접 확인 |
| F13 | `baseVersion=1.8.0` 이미 적용됨 — 이번 PR 에서 추가 범프 불필요 | `gradle.properties` |
| F14 | `.coAwait()` 마이그레이션 완료 — 4.x deprecated `xxxAwait()` 잔존 0건 | `rg "Await(\\(\\)\\.await)" --glob '*.kt'` |

---

## 1. 작업 범위 (Affected Modules)

| 영역 | 모듈 / 파일 | 결합 강도 |
|------|------------|----------|
| 버전 상수 | `buildSrc/src/main/kotlin/Libs.kt` | **owner** |
| 메인 모듈 | `io/vertx/` (~25 main + ~30 test = 55 파일) | **owner** |
| HR 의존 | `data/hibernate-reactive/` (build + src) | **direct API consumer** |
| Micrometer | `infra/micrometer/` (vertx_core implementation) | **direct dep** |
| 옵셔널 | `io/feign/`, `io/retrofit2/`, `io/http/` | **compileOnly** (자동 반영) |
| ORM 전역 영향 | `data/hibernate`, `spring-boot3/hibernate-*`, `spring-boot4/hibernate-*` | **transitive** (ORM 7.3) |
| 정리 | `x-obsoleted/vertx-coroutines/`, `vertx-sqlclient/`, `vertx-webclient/` | **delete only** |

---

## 2. Phase 구성 원칙

```
T0 (Sequential) ──► T1 (Sequential, scope unknown until T0) ──► T2 (Parallel per module)
   버전 범프 + 컴파일      API 마이그레이션 + 정리                   테스트 + 문서
```

- **T0** 는 **순차 실행** — 각 컴파일 결과가 다음 단계 작업 범위를 결정.
- **T1** 의 작업 *수* 와 *범위* 는 T0 결과에 의존 (사전에 식별된 것 + 실제 발견된 것).
- **T2** 는 모듈별 독립이라 **병렬 가능** — `--parallel` 또는 worker 분리.

---

## 3. Phase T0 — 버전 범프 + 컴파일 검증

> 목표: **의존성만** 바꿔서 깨지는 범위를 정량화. 코드 수정은 T1 에서.

### T0.1 · Libs.kt 4건 동시 변경

- **complexity**: low
- **effort**: S (single file, 4 라인)
- **dependencies**: 없음 (시작점)

| 변경 항목 | Before | After |
|----------|--------|-------|
| `Versions.vertx` | `"4.5.26"` | `"5.0.11"` |
| `Versions.hibernate_reactive` | `"3.2.0.Final"` | `"4.3.3.Final"` |
| `Versions.hibernate` | `"7.2.7.Final"` | `"7.3.2.Final"` |
| `Libs.resilience4j_vertx` val | (라인 존재) | (라인 삭제) |

**DoD**:
- `git diff buildSrc/src/main/kotlin/Libs.kt` 가 정확히 위 4건만 표시
- `rg resilience4j_vertx` 결과 0건

### T0.2 · Netty 핀 추가 (data/hibernate-reactive)

- **complexity**: medium
- **effort**: S (single file, 한 블록 추가)
- **dependencies**: T0.1 완료

**작업**: `data/hibernate-reactive/build.gradle.kts` 에 Netty 4.2.12.Final 강제 룰 추가.

```kotlin
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.netty" && requested.name != "netty-tcnative-boringssl-static") {
            useVersion("4.2.12.Final")
            because("Vert.x 5.0.11 requires Netty 4.2.x; Spring Boot 3 BOM pins 4.1.x")
        }
    }
}
```

**DoD**:
- `./gradlew :bluetape4k-hibernate-reactive:dependencies | rg "netty-common"` 결과가 모두 `4.2.12.Final` 로 해석

### T0.3 · `:bluetape4k-vertx:compileKotlin`

- **complexity**: medium (결과에 따라 T1 작업 결정)
- **effort**: M (실패 시 에러 분류 작업 포함)
- **dependencies**: T0.1

**명령**: `./gradlew :bluetape4k-vertx:compileKotlin --no-daemon 2>&1 | tee /tmp/t0.3.log`

**DoD (양자택일)**:
- (a) `BUILD SUCCESSFUL` → T1.1, T1.2 사전 식별 항목만 처리
- (b) 컴파일 에러 → `/tmp/t0.3.log` 에서 에러를 분류하여 T1.x 항목 추가

**예상 에러 후보** (Spec §4.3, §4.5 기반):
1. `succeedingThenComplete` 등 시그니처 변경 (F11 — `succeeding(Handler<T>)` 는 유지)
2. `JDBCPool` import / 메서드 시그니처
3. `WebClientOptions` / `HttpServerOptions` setter

### T0.4 · `:bluetape4k-hibernate-reactive:compileKotlin`

- **complexity**: medium
- **effort**: M
- **dependencies**: T0.1, T0.2

**명령**: `./gradlew :bluetape4k-hibernate-reactive:compileKotlin --no-daemon 2>&1 | tee /tmp/t0.4.log`

**DoD**: `BUILD SUCCESSFUL` 또는 에러 분류 → T1.3 작업 항목화

**예상 에러 후보**:
1. ORM 7.2 → 7.3 minor API (Internal SPI 변경)
2. HR 3.2 → 4.3 의 `Stage.SessionFactory` / `Mutiny.SessionFactory` deprecation

### T0.5 · `:bluetape4k-micrometer:compileKotlin`

- **complexity**: low
- **effort**: S
- **dependencies**: T0.1

**명령**: `./gradlew :bluetape4k-micrometer:compileKotlin --no-daemon`

**DoD**: 컴파일 통과. (vertx-core 의존만 — 영향 매우 좁음)

### T0.6 · ORM 7.3 전역 영향 컴파일

- **complexity**: high (영향 범위 광범)
- **effort**: L (다수 모듈 컴파일 + 잠재 fix)
- **dependencies**: T0.1

**검증 모듈** (F8 — `Versions.hibernate` 의존):
```bash
./gradlew \
  :bluetape4k-hibernate:compileKotlin \
  :bluetape4k-spring-boot3-hibernate-lettuce:compileKotlin \
  :bluetape4k-spring-boot4-hibernate-lettuce:compileKotlin \
  --no-daemon 2>&1 | tee /tmp/t0.6.log
```

**추가 후보 모듈** (Glob 으로 사전 탐색):
- `Glob "**/build.gradle.kts"` 에서 `Libs.hibernate` / `Libs.hibernate_core` 참조 모듈 모두

**DoD**:
- 모든 hibernate 의존 모듈이 컴파일 통과
- 실패 시 `/tmp/t0.6.log` 에서 깨진 모듈 별로 T1.x 항목화

### T0.7 · 옵셔널 모듈 (compileOnly) 컴파일

- **complexity**: low
- **effort**: S
- **dependencies**: T0.1

**명령**:
```bash
./gradlew \
  :bluetape4k-feign:compileKotlin \
  :bluetape4k-retrofit2:compileKotlin \
  :bluetape4k-http:compileKotlin \
  --no-daemon
```

**DoD**: 전부 통과 (compileOnly 이므로 영향 0 예상 — 회귀 방지 목적)

---

## Phase T0 종료 게이트

- [ ] T0.1 ~ T0.7 모두 `BUILD SUCCESSFUL` 또는 에러가 T1 항목으로 등록됨
- [ ] `./gradlew :bluetape4k-vertx:dependencies | rg "vertx-core"` → `5.0.11` 으로 해석 확인
- [ ] `./gradlew :bluetape4k-hibernate-reactive:dependencies | rg "hibernate-reactive-core"` → `4.3.3.Final`
- [ ] `./gradlew :bluetape4k-hibernate-reactive:dependencies | rg "hibernate-core"` → `7.3.2.Final`
- [ ] `./gradlew :bluetape4k-hibernate-reactive:dependencies | rg "netty-common"` → `4.2.12.Final`

---

## 4. Phase T1 — API 마이그레이션 + 정리

> T0 의 컴파일 결과가 **T1 작업 수와 범위를 결정**. 사전 식별 항목 + T0 발견 항목 모두 처리.

### T1.1 · vertx-jdbc-client API 호환성 (F2~F5 기반)

- **complexity**: medium (5.x 시그니처 미세 변경 가능)
- **effort**: S~M (변경 0~2 파일 예상)
- **dependencies**: T0.3 결과

**대상 코드**:
- `io/vertx/src/main/kotlin/.../SqlResultSupport.kt:3` — `import io.vertx.jdbcclient.JDBCPool` + `.property(JDBCPool.GENERATED_KEYS)`
- `io/vertx/src/test/kotlin/.../AbstractVertxSqlClientTest.kt:10-11,73` — `JDBCConnectOptions`, `JDBCPool.pool()`

**확인 사항**:
1. `JDBCPool.GENERATED_KEYS` 상수 존재 여부 (5.0.11)
2. `JDBCPool.pool(Vertx, JDBCConnectOptions, PoolOptions)` 시그니처 유지 여부
3. `JDBCConnectOptions` 의 setter chain 호환성

**DoD**:
- 두 파일 컴파일 통과
- `import io.vertx.jdbcclient.JDBCPool` 유지 (Vert.x 5 패키지 동일)

**중요**: F2 — `vertx-jdbc-client` 는 Vert.x 5 에서 **제거되지 않았다**. `Libs.vertx_jdbc_client` 와 `build.gradle.kts` 참조를 **유지**한다. (Spec §4.1 의 "제거" 표현은 본 Plan 의 F2~F5 사실로 **무효**)

### T1.2 · `succeeding(Handler<T>)` 시그니처 점검 (F11 기반)

- **complexity**: low
- **effort**: S
- **dependencies**: T0.3 결과

**대상 코드**:
- `io/vertx/src/test/kotlin/io/bluetape4k/vertx/examples/VertxJunit5Examples.kt:60, 94`
- `io/vertx/src/test/kotlin/io/bluetape4k/vertx/examples/LifecycleExamples.kt:63`

**확인 사항**: F11 — Vert.x 5 에서 **lambda form `succeeding(Handler<T>)` 는 유지** (zero-arg 형 `succeeding()` 만 제거됨). 따라서 본 프로젝트 코드는 변경 불필요할 가능성 높음.

**DoD**:
- T0.3 컴파일 결과로 판단:
  - 통과 시: 변경 없이 종료
  - 실패 시: `succeedingThenComplete()` 또는 `assertThat(...).also { testContext.completeNow() }` 패턴으로 변환

### T1.3 · ORM 7.3 호환 점검 (data/hibernate-reactive)

- **complexity**: medium
- **effort**: S~M (0~3 파일)
- **dependencies**: T0.4, T0.6 결과

**대상 후보**:
- `data/hibernate-reactive/src/main/kotlin/.../SessionFactorySupport.kt` (Mutiny / Stage)
- `data/hibernate-reactive/src/main/kotlin/.../*Repository.kt`
- ORM 7.3 deprecation 워닝이 있는 모든 파일

**DoD**:
- `./gradlew :bluetape4k-hibernate-reactive:compileKotlin` 워닝 0건 또는 의도적 `@Suppress` 표기
- T0.6 에 등록된 ORM 7.3 깨짐 모듈 모두 fix

### T1.4 · x-obsoleted/vertx-* 디렉토리 삭제 (F12 기반)

- **complexity**: low
- **effort**: S
- **dependencies**: 없음 (병렬 가능, 다만 T0 이후 권장)

**삭제 대상** (14 파일):
- `x-obsoleted/vertx-coroutines/` (전체)
- `x-obsoleted/vertx-sqlclient/` (전체)
- `x-obsoleted/vertx-webclient/` (전체)

**명령**: `git rm -r x-obsoleted/vertx-coroutines x-obsoleted/vertx-sqlclient x-obsoleted/vertx-webclient`

**DoD**:
- 3 디렉토리 부재 (`fd vertx x-obsoleted/` 결과 0건)
- `./gradlew projects` 출력에서 변화 없음 (settings.gradle.kts 등록되지 않았음 — F12)

### T1.5 · T0 발견 추가 컴파일 에러 처리

- **complexity**: unknown (T0 결과 의존)
- **effort**: S~L (발견된 에러 수에 비례)
- **dependencies**: T0.3 ~ T0.6 결과

**작업 방식**:
- `/tmp/t0.*.log` 의 에러를 그룹화 (모듈별 / API 종류별)
- 각 그룹마다 sub-task 등록 (T1.5.a, T1.5.b, ...)
- 각 sub-task DoD: 해당 모듈 `compileKotlin` 통과

**예상 후보** (T0 전 사전 식별 — Spec §4 참조):
- `WebClientOptions` / `HttpServerOptions` setter 시그니처 (대부분 deprecated 만)
- `Future` API: `CompositeFuture` (현재 0건 — 회귀 방지 grep 만)
- ORM 7.3 의 `SchemaExport` / `SessionImplementor` 내부 API

---

## Phase T1 종료 게이트

- [ ] 영향 모든 모듈 `compileKotlin` 통과
- [ ] `rg "xxxAwait\\(|CompositeFuture" --glob '*.kt'` 결과 0건 (회귀 방지)
- [ ] `x-obsoleted/vertx-*` 3 디렉토리 삭제 완료
- [ ] `Libs.vertx_jdbc_client` **유지 확인** (F5)

---

## 5. Phase T2 — 테스트 + 문서

> 모듈별 독립 → **병렬 실행 가능**. 단, T2.4~T2.5 (문서) 는 코드 fix 가 끝난 다음.

### T2.1 · `:bluetape4k-vertx:test`

- **complexity**: medium
- **effort**: M (테스트 실행 + 실패 디버깅)
- **dependencies**: T1 완료

**명령**: `./gradlew :bluetape4k-vertx:test --no-daemon`

**DoD**:
- 전 테스트 통과 (passing count + duration 기록)
- 실패 시 모듈 코드 측 fix → T1 으로 복귀

### T2.2 · `:bluetape4k-hibernate-reactive:test`

- **complexity**: high (Testcontainers MySQL + PG)
- **effort**: L (Docker 환경 + HR 4.3 회귀)
- **dependencies**: T1 완료

**명령**: `./gradlew :bluetape4k-hibernate-reactive:test --no-daemon`

**DoD**:
- Testcontainers MySQL 시나리오 통과
- Testcontainers PostgreSQL 시나리오 통과 (있는 경우)
- 실패 시 HR 4.3 호환 패치 → T1.3 으로 복귀

### T2.3 · `:bluetape4k-micrometer:test`

- **complexity**: low
- **effort**: S
- **dependencies**: T1 완료

**명령**: `./gradlew :bluetape4k-micrometer:test --no-daemon`

**DoD**: 통과 (vertx-core 회귀만 보면 됨)

### T2.3.b · 옵셔널 모듈 회귀 (병렬)

- **complexity**: low
- **effort**: S
- **dependencies**: T1 완료

**명령**:
```bash
./gradlew \
  :bluetape4k-feign:test \
  :bluetape4k-retrofit2:test \
  :bluetape4k-http:test \
  --no-daemon
```

**DoD**: regression 없음

### T2.4 · README 업데이트 (3 모듈 × 2 언어 = 6 파일)

- **complexity**: low
- **effort**: M (호환 매트릭스 + Mermaid 검토)
- **dependencies**: T1 완료

**대상**:
| 모듈 | 파일 |
|------|------|
| `io/vertx` | `README.md` + `README.ko.md` |
| `data/hibernate-reactive` | `README.md` + `README.ko.md` |
| `infra/micrometer` | `README.md` + `README.ko.md` |

**갱신 항목**:
- 버전 표기 (Vert.x 5.0.11 / HR 4.3.3.Final / ORM 7.3.2.Final)
- `io/vertx`: JDBC 클라이언트 **유지** 안내 (F2~F5) — Spec §4.1 의 "제거" 표현 정정
- `data/hibernate-reactive`: 호환 매트릭스 (Vert.x ↔ HR ↔ ORM)
- `infra/micrometer`: Vert.x 5 metrics 호환 명기
- 기존 Mermaid 다이어그램 — 변경 없으면 유지

**DoD**:
- 6 파일 모두 갱신 + 양 언어 동기 (한국어/영어 내용 일치)
- 새 KDoc 발생 시 작성 (CLAUDE.md 규칙)

### T2.5 · 루트 CLAUDE.md 업데이트

- **complexity**: low
- **effort**: S
- **dependencies**: T2.4 완료

**갱신 항목**:
- "Module Groups" 표의 `io/` 행 — 변경 없음 (vertx 모듈명 동일)
- 별도 "Vert.x 5.0.11" 명시 줄 추가 검토 (선택)
- baseVersion 1.8.0 확인 (이미 적용 — F13)

**DoD**:
- `git diff CLAUDE.md` 가 의도된 변경만 표시

### T2.6 · 통합 검증 (smoke + detekt)

- **complexity**: medium
- **effort**: M
- **dependencies**: T2.1 ~ T2.5

**명령**:
```bash
./gradlew detekt --no-daemon  # 영향 모듈만
./gradlew clean build -x test --no-daemon  # 전체 ABI 비파괴 확인
```

**DoD**:
- detekt 신규 위반 0건
- 전체 build (테스트 제외) 성공

### T2.7 · `/wiki-update` 스킬 실행

- **complexity**: low
- **effort**: S
- **dependencies**: 모든 T 단계 완료

**작업**: 본 Spec/Plan 을 wiki/qmd 색인에 반영

**DoD**:
- `wiki/pages/` 에 Vert.x 5 업그레이드 페이지 등록 또는 갱신
- `qmd index` 재실행 완료

---

## Phase T2 종료 게이트 (PR 머지 직전)

- [ ] T2.1 ~ T2.7 전수 통과
- [ ] PR 본문에 모듈별 passing count + duration 기재
- [ ] `oh-my-claudecode:code-reviewer` 실행 → HIGH/CRITICAL 0건
- [ ] CLAUDE.md "Before Creating a PR" 체크리스트 모두 충족

---

## 6. 작업 의존 그래프 (요약)

```
T0.1 (Libs.kt) ──┬──► T0.3 (vertx compile) ────► T1.1 (jdbc API) ─┐
                 ├──► T0.4 (HR compile) ───────► T1.3 (ORM 7.3) ──┤
                 ├──► T0.5 (micrometer) ────────────────────────  ├──► T2.1~T2.3.b (parallel test)
                 ├──► T0.6 (ORM global) ───────► T1.5 (extras) ───┤      │
                 ├──► T0.7 (optional) ─────────────────────────── │      │
                 │                                                 │      ▼
                 ▼                                                 │   T2.4~T2.5 (docs)
            T0.2 (Netty pin)                                       │      │
                 │                                                 │      ▼
                 │                                                 │   T2.6 (smoke + detekt)
                 │                                                 │      │
              T1.2 (succeeding) ◄───── T0.3 result               │      ▼
              T1.4 (x-obsoleted delete, 독립) ──────────────────► │   T2.7 (wiki-update)
```

- **Critical Path**: T0.1 → T0.3/0.4/0.6 → T1.1/1.3/1.5 → T2.1/2.2 → T2.6
- **병렬 가능**: T0.3 ↔ T0.4 ↔ T0.5 ↔ T0.6 ↔ T0.7 (T0.1 완료 후) · T2.1 ↔ T2.2 ↔ T2.3 ↔ T2.3.b · T1.4 (전 단계 독립)

---

## 7. 작업 요약 표

| Task | Phase | Complexity | Effort | Dependencies | Scope known? |
|------|-------|-----------|--------|--------------|--------------|
| T0.1 Libs.kt 4건 변경 | T0 | low | S | — | ✅ |
| T0.2 Netty 핀 추가 | T0 | medium | S | T0.1 | ✅ |
| T0.3 vertx compile | T0 | medium | M | T0.1 | ✅ |
| T0.4 HR compile | T0 | medium | M | T0.1, T0.2 | ✅ |
| T0.5 micrometer compile | T0 | low | S | T0.1 | ✅ |
| T0.6 ORM 전역 compile | T0 | high | L | T0.1 | ✅ |
| T0.7 옵셔널 compile | T0 | low | S | T0.1 | ✅ |
| T1.1 jdbc-client API fix | T1 | medium | S~M | T0.3 | ⚠️ T0 결과 의존 |
| T1.2 succeeding 점검 | T1 | low | S | T0.3 | ⚠️ T0 결과 의존 |
| T1.3 ORM 7.3 fix | T1 | medium | S~M | T0.4, T0.6 | ⚠️ T0 결과 의존 |
| T1.4 x-obsoleted 삭제 | T1 | low | S | — | ✅ |
| T1.5 추가 에러 fix | T1 | unknown | S~L | T0.3~T0.6 | ❌ T0 후 결정 |
| T2.1 vertx test | T2 | medium | M | T1 | ✅ |
| T2.2 HR test | T2 | high | L | T1 | ✅ |
| T2.3 micrometer test | T2 | low | S | T1 | ✅ |
| T2.3.b 옵셔널 test | T2 | low | S | T1 | ✅ |
| T2.4 README 6건 | T2 | low | M | T1 | ✅ |
| T2.5 CLAUDE.md | T2 | low | S | T2.4 | ✅ |
| T2.6 smoke + detekt | T2 | medium | M | T2.1~T2.5 | ✅ |
| T2.7 wiki-update | T2 | low | S | All | ✅ |

**Total**: 19 tasks (T0: 7 / T1: 5 / T2: 7) — 그 중 4건이 T0 결과에 따라 범위 확정.

---

## 8. 회귀 방지 grep 명령 (PR 직전 실행)

```bash
# 4.x deprecated API 잔존 확인 (모두 0건이어야 함)
rg "xxxAwait\\(|CompositeFuture" --glob '*.kt'

# resilience4j_vertx 미참조 확인
rg "resilience4j_vertx"

# vertx-jdbc-client import 유지 확인 (F2~F5)
rg "io.vertx.jdbcclient.JDBCPool" --glob '*.kt'

# x-obsoleted/vertx-* 부재 확인
fd vertx x-obsoleted/

# 핵심 의존 버전 확인
./gradlew :bluetape4k-vertx:dependencies | rg "vertx-core"
./gradlew :bluetape4k-hibernate-reactive:dependencies | rg -E "hibernate-(reactive|core)|netty-common"
```

---

## 9. 변경하지 않는 항목 (의도적 비변경)

- **`baseVersion`**: 이미 `1.8.0` 적용됨 (F13) — 추가 범프 없음
- **`Libs.vertx_jdbc_client`**: F2~F5 — 유지
- **`io/vertx/build.gradle.kts` 의 vertx-jdbc-client 의존**: 유지
- **`io/feign`, `io/retrofit2`, `io/http` 코드**: compileOnly 만 사용 → 자동 반영, 코드 수정 없음
- **새 Vert.x 5 신기능 (Virtual Thread Verticle, Future.all/any/join DSL, gRPC-web)**: 본 PR 범위 외 — Spec §10 의 별도 issue

---

## 10. PR 머지 전 최종 체크리스트 (DoD)

Spec §6 + CLAUDE.md "Before Creating a PR" 통합:

- [ ] T0.1 ~ T2.7 전수 통과
- [ ] 영향 모듈 6+ 곳 모두 컴파일 + 테스트 통과 (passing count + duration 기재)
- [ ] `Libs.kt` 4건 변경 (vertx, hibernate_reactive, hibernate, resilience4j_vertx 삭제) 정확
- [ ] `Libs.vertx_jdbc_client` **유지** (F5 — Spec 표현 정정)
- [ ] `data/hibernate-reactive/build.gradle.kts` 에 Netty 4.2.12.Final 핀 추가
- [ ] `x-obsoleted/vertx-*` 3 디렉토리 삭제
- [ ] 6 파일 README 갱신 (`io/vertx`, `data/hibernate-reactive`, `infra/micrometer` × ko/en)
- [ ] 새 공개 API 발생 시 KDoc 작성
- [ ] `/wiki-update` 실행 완료
- [ ] `oh-my-claudecode:code-reviewer` 실행 후 HIGH/CRITICAL 0건
- [ ] PR 본문에 테스트 결과, 변경 근거, 검증 명령 기재
- [ ] worktree 안에서 작업 완료 (`.worktrees/feat-vertx5-upgrade/`)

---

**Plan 종료. T0.1 부터 구현 진입 가능.**
