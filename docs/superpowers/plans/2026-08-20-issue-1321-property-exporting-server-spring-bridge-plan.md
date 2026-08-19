# PropertyExportingServer의 Spring DynamicPropertyRegistry 연동 Implementation Plan

> **For agentic workers:** 이 계획을 순서대로 실행한다. 각 단계는 작은 검증 가능한 단위로 유지하고, stacked PR의 base/head를 임의로 바꾸지 않는다.

**Goal:** `PropertyExportingServer`를 Spring `DynamicPropertyRegistry`에 연결하는
선택 모듈을 추가하고, core의 Spring 무의존·lazy supplier·namespace 재사용 계약을
테스트·문서·CI로 고정한다.

**Architecture:** `testing/testcontainers-spring`이 core interface와 Spring Test를
공개 의존하고 top-level `registerDynamicProperties` extension을 제공한다. 등록 시
`propertyKeys()`만 읽고, 값은 `registry.add` supplier 안에서 `properties()[key]`로
조회한다. 새 모듈은 자동 settings 등록을 사용하며 기존 Docker-backed core suite와
분리된 JVM unit test를 실행한다.

**Tech Stack:** Kotlin 2.4, Gradle 9.7, Java 25 default target, Spring Boot 4
dependency platform, Spring Test `DynamicPropertyRegistry`, JUnit 5/Kluent.

---

## 파일 책임 지도

| 파일 | 책임 |
| --- | --- |
| `testing/testcontainers-spring/build.gradle.kts` | 선택 module의 core/Spring Test dependency와 test 설정 |
| `testing/testcontainers-spring/src/main/.../PropertyExportingServerDynamicPropertyRegistry.kt` | public lazy bridge API |
| `testing/testcontainers-spring/src/test/.../PropertyExportingServerDynamicPropertyRegistryTest.kt` | fake server/registry contract test |
| `testing/testcontainers-spring/src/test/resources/*` | JUnit platform와 test logging 설정 |
| `testing/testcontainers-spring/README.md`, `README.ko.md` | dependency, usage, lifecycle, collision 문서 |
| `README.md`, `README.ko.md` | root testing module catalog |
| `.github/workflows/ci.yml` | PR path filter 및 JVM module test coverage |
| `.github/workflows/nightly-tests.yml` | nightly bridge test task |
| `.github/workflows/codeql.yml` | 새 Kotlin source의 분석 scope 또는 명시적 제외 근거 |
| `docs/superpowers/specs/2026-08-20-issue-1321-property-exporting-server-spring-bridge-design.md` | 설계 source of truth |
| `docs/lessons/2026-08-20-issue-1321-property-exporting-server-spring-bridge.md` | 구현 후 재발 방지 lesson |

## Task 1: 승인된 설계와 module boundary를 먼저 고정한다

**Files:**
- Create: `docs/superpowers/specs/2026-08-20-issue-1321-property-exporting-server-spring-bridge-design.md`
- Create: `docs/superpowers/plans/2026-08-20-issue-1321-property-exporting-server-spring-bridge-plan.md`

- [ ] **Step 1: issue/Epic/선행 head read-back**

  `gh issue view 1321`, `gh issue view 1418`, `gh pr list --search '1321 in:title,body'`
  결과와 현재 branch base `846a804b9287c61bbf802d0573909005e1a66f8f`를 기록한다.

- [ ] **Step 2: SPW-01~05 spec/plan 검증**

  두 문서를 전체 read-back하고 미완성 placeholder가 0건인지 확인한다.
  `git diff --check`와 Korean term audit를 실행한다.

- [ ] **Step 3: 설계 문서만 Lore commit**

  다음 intent line과 trailers를 사용해 문서만 commit한다.

  ```text
  #1321의 Spring 연동 경계를 구현 전에 고정한다

  SDK-neutral core를 유지하면서 선택 모듈의 lazy DynamicPropertyRegistry bridge와
  stacked Slot 3의 검증·CI·후속 head 계약을 문서화한다.

  Constraint: bluetape4k-testcontainers core must not depend on Spring Test.
  Rejected: core compileOnly bridge and Workshop-only helper; boundary or reuse contract is wrong.
  Confidence: high
  Scope-risk: moderate
  Directive: Keep the Spring bridge optional and lazy; do not change system-property lifecycle.
  Tested: issue/Epic/head read-back, document read-back, git diff --check.
  Not-tested: implementation and Gradle tests pending.
  ```

## Task 2: module skeleton과 build contract를 추가한다

**Files:**
- Create: `testing/testcontainers-spring/build.gradle.kts`
- Create: `testing/testcontainers-spring/src/test/resources/junit-platform.properties`
- Create: `testing/testcontainers-spring/src/test/resources/logback-test.xml`

- [ ] **Step 1: auto-registration을 사용한 module skeleton 생성**

  `plugins { java-library; kotlin("jvm") }`와 저장소의 test configuration convention을
  따른다. `api(project(":bluetape4k-testcontainers"))`,
  `api(platform(bt4k.spring.boot4.dependencies))`, `api("org.springframework:spring-test")`
  를 사용하고 Spring dependency에 임의 version을 붙이지 않는다.

- [ ] **Step 2: test dependency를 최소화한다**

  `testImplementation(project(":bluetape4k-junit5"))`, Kotlin/JUnit 5 engine,
  `testRuntimeOnly(bt4k.logback)`를 추가한다. Docker/Jib, Testcontainers image build,
  Spring Boot starter를 새 module에 추가하지 않는다.

- [ ] **Step 3: settings/project path 검증**

  `./gradlew projects --no-daemon --no-configuration-cache --no-build-cache`로
  `bluetape4k-testcontainers-spring`이 auto-registration되는지 확인한다.

## Task 3: contract test를 먼저 작성해 RED를 증명한다

**Files:**
- Create: `testing/testcontainers-spring/src/test/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistryTest.kt`

- [ ] **Step 1: fake server와 recording registry를 정의한다**

  `DynamicPropertyRegistry.add(String, Supplier<Any>)` 호출을 map/list에 보관하는
  fake registry와 `PropertyExportingServer` fake를 만든다. fake server는 key set,
  map, properties 호출 횟수·예외를 제어할 수 있어야 한다.

- [ ] **Step 2: 다음 테스트를 구현한다**

  full key mapping, empty key, lazy registration, repeated supplier evaluation,
  key/map mismatch, supplier exception passthrough, no system-property mutation,
  collision delegation을 각각 독립 test로 고정한다.

- [ ] **Step 3: 구현 전 RED 실행**

  `./gradlew :bluetape4k-testcontainers-spring:test --no-daemon --no-configuration-cache --no-build-cache`
  를 실행한다. extension source가 없으므로 compile failure가 예상된다. 실패 로그와
  현재 test count를 기록하고, 실패가 dependency/configuration 오류라면 먼저 module
  skeleton을 보정한다.

## Task 4: lazy bridge API를 구현한다

**Files:**
- Create: `testing/testcontainers-spring/src/main/kotlin/io/bluetape4k/testcontainers/spring/PropertyExportingServerDynamicPropertyRegistry.kt`

- [ ] **Step 1: public extension signature을 고정한다**

  `PropertyExportingServer.registerDynamicProperties(registry: DynamicPropertyRegistry)`
  를 public top-level function으로 둔다. KDoc은 namespace format, lazy supplier,
  no lifecycle ownership, no system-property mutation, collision delegation을 한국어로
  명시한다.

- [ ] **Step 2: propertyKeys 기반 등록을 구현한다**

  `propertyKeys().forEach { key -> registry.add("testcontainers.$propertyNamespace.$key") { properties()[key] ?: error(...) } }`
  흐름을 사용한다. 등록 단계에서 `properties()`를 호출하거나 map을 캐시하지 않는다.
  core의 internal `SERVER_PREFIX`를 새 module에서 참조하지 말고 기존 공개 key 계약의
  문자열 형식을 bridge에 한정해 사용한다.

- [ ] **Step 3: 예외와 충돌 동작을 보존한다**

  missing key는 명확한 `IllegalStateException`으로 실패시키고, properties가 던지는
  원래 예외는 감싸지 않는다. registry entry를 사전 조회·삭제·덮어쓰지 않는다.

## Task 5: GREEN 및 Kotlin/Gradle 품질 검증

**Files:**
- Modify: `testing/testcontainers-spring/src/test/...` only if test diagnostics require it

- [ ] **Step 1: focused test GREEN**

  동일한 module test를 다시 실행해 모든 bridge contract test가 통과하는지 확인한다.

- [ ] **Step 2: compile/detekt/check**

  ```bash
  ./gradlew :bluetape4k-testcontainers-spring:compileKotlin \
    :bluetape4k-testcontainers-spring:detekt \
    :bluetape4k-testcontainers-spring:check \
    --no-daemon --no-configuration-cache --no-build-cache
  ```

- [ ] **Step 3: core regression boundary**

  `./gradlew :bluetape4k-testcontainers:compileKotlin :bluetape4k-testcontainers-spring:check`
  를 순차 실행하고 dependency insight로 core가 Spring을 transitively 받지 않는지
  확인한다.

## Task 6: reader-facing 문서와 repository integration을 추가한다

**Files:**
- Create: `testing/testcontainers-spring/README.md`
- Create: `testing/testcontainers-spring/README.ko.md`
- Modify: `README.md`
- Modify: `README.ko.md`
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/nightly-tests.yml`
- Modify: `.github/workflows/codeql.yml` only when source scope requires it

- [ ] **Step 1: module README EN/KO 작성**

  dependency coordinate, `@DynamicPropertySource` example, full key example,
  lazy supplier, lifecycle non-ownership, system property bridge distinction,
  collision guidance를 같은 marker로 작성한다. Spring Test를 선택 모듈이 제공한다는
  사실과 core만 쓰는 소비자는 기존 dependency만 유지한다는 점을 명시한다.

- [ ] **Step 2: root module catalog parity**

  EN/KO testing 목록에 `bluetape4k-testcontainers-spring` 링크를 같은 위치와 의미로
  추가한다. 기존 module link와 heading을 변경하지 않는다.

- [ ] **Step 3: CI/nightly/codeql path audit**

  `ci.yml` path filter가 `testing/testcontainers-spring/**`를 포함하고 module test를
  실행하는지 확인한다. nightly는 Docker-backed `bluetape4k-testcontainers`와 별도로
  pure JVM bridge test를 sequentially 실행한다. codeql은 새 Kotlin source를 기존
  testing scope로 분석하거나, scope 밖이면 그 이유를 workflow 주석과 read-back으로
  남긴다.

- [ ] **Step 4: module/coverage contract audit**

  generated project catalog, Kover aggregation, static contract scripts가 새 module을
  누락하지 않는지 `rg`와 Gradle task listing으로 확인한다. 기존 Docker image build나
  Kover exclusion을 새 module에 복사하지 않는다.

## Task 7: lesson과 최종 검증을 작성한다

**Files:**
- Create: `docs/lessons/2026-08-20-issue-1321-property-exporting-server-spring-bridge.md`

- [ ] **Step 1: lesson에 재발 방지 규칙 기록**

  core/adapter dependency boundary, `propertyKeys()` vs `properties()` lazy split,
  Spring registry collision ownership, CI path registration을 실제 diff와 test evidence로
  기록한다. reader-facing lesson은 한국어로 쓴다.

- [ ] **Step 2: 최종 validation sequence**

  ```bash
  ./gradlew :bluetape4k-testcontainers-spring:test \
    --no-daemon --no-configuration-cache --no-build-cache
  ./gradlew :bluetape4k-testcontainers-spring:compileKotlin \
    :bluetape4k-testcontainers-spring:detekt \
    :bluetape4k-testcontainers-spring:check \
    --no-daemon --no-configuration-cache --no-build-cache
  ./gradlew projects --no-daemon --no-configuration-cache --no-build-cache
  git diff --check
  node ~/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
    README.md README.ko.md \
    testing/testcontainers-spring/README.md \
    testing/testcontainers-spring/README.ko.md \
    docs/lessons/2026-08-20-issue-1321-property-exporting-server-spring-bridge.md
  ```

- [ ] **Step 3: diff and branch boundary review**

  `git diff --stat`, `git diff --name-only develop...HEAD`, and `git status --short`로
  Slot 3 파일만 포함되는지 확인한다. temporary workflow input JSON은 commit에서 제외하거나
  lane evidence를 별도 durable artifact로 정리한다.

## Task 8: stacked PR, CI, review, merge, cleanup

**Files:**
- Modify: PR body only through GitHub API/CLI

- [ ] **Step 1: commit implementation with Lore trailers**

  intent line은 독자가 변경 이유를 이해하도록 한국어로 작성한다. 모든 commit에
  Constraint/Rejected/Confidence/Scope-risk/Directive/Tested/Not-tested를 포함한다.

- [ ] **Step 2: PR create/read-back**

  `feat/1418-03-dynamic-property-registry`를 exact base
  `846a804b9287c61bbf802d0573909005e1a66f8f`에 대해 PR로 만든다. issue #1321,
  milestone `1.13.0`, labels/assignee `debop`를 mirror하고 한국어 body 마지막을
  정확히 `## DoD Status`로 둔다. DoD에는
  `Required checks: X/Y; N/A: N; Blocked: N`을 실제 상태로 기록한다.

- [ ] **Step 3: independent six-perspective review**

  performance, stability, security, operations, developer/API, user/caller 관점의
  독립 리뷰를 read-only로 수행한다. P0/P1은 merge 전 0이어야 하고, P2는 수정하거나
  수용 근거를 남긴다. exact head가 바뀌면 리뷰와 DoD를 다시 읽는다.

- [ ] **Step 4: CI and fresh approval gate**

  PR checks와 review thread를 live read-back한다. green CI는 merge 권한이 아니므로
  exact PR head·base·metadata·DoD·mergeability를 다시 확인한 뒤 사용자의 fresh
  merge approval을 기다린다. auto-merge는 사용하지 않는다.

- [ ] **Step 5: merge and successor hold**

  fresh approval 후에만 PR을 merge하고 merge SHA와 `origin/develop` sync를 확인한다.
  #1337 branch는 merge SHA read-back이 끝날 때까지 만들거나 구현하지 않는다.

- [ ] **Step 6: safe cleanup and Epic progress**

  merged exact head, clean worktree, no unique commits를 확인한 뒤에만 Slot 3 worktree와
  branch를 정리한다. Epic #1418 body/checklist를 live read-back하고 progress를 `3/4`로
  갱신하며, #1337을 next slot로 남긴다.

## 실행 규칙

- Testcontainers Docker suite와 bridge JVM unit test는 동시에 실행하지 않는다.
- core에 Spring dependency가 보이면 구현을 중지하고 module boundary를 수정한다.
- supplier를 eager하게 만들거나 `properties()`를 캐시하는 편의 리팩터링을 하지 않는다.
- public KDoc/README/lesson/PR/commit prose는 한국어로 유지하고 API·명령·URL은 보존한다.
- 모든 완료 주장은 fresh command output, GitHub read-back, exact SHA evidence로 뒷받침한다.
