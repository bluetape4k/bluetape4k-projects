# Issue #1562 TenantContext production API 구현 계획

> **실행 agent 필수 skill:** `$test-driven-development`, `$bluetape-kotlin-patterns`,
> `$bluetape-full-feature`, `$verification-before-completion`을 task 시작 전에 다시 읽는다.
> 각 production 변경은 해당 task의 RED를 먼저 확인한 뒤 GREEN으로 진행한다.

**목표:** JDK 25 전용 공통 `TenantId`와 no-default `TenantContext`를 제공하고,
ThreadLocal·ScopedValue·Reactor·Ktor carrier를 framework 의존성 경계에 맞춘 세 artifact로
발행 가능한 상태까지 구현한다.

**구조:** `bluetape4k-tenant`가 값·예외·JDK carrier를 소유한다.
`bluetape4k-tenant-reactor`와 `bluetape4k-ktor-tenant`는 core를 `api`로 참조하고 각각
Reactor `Context`와 Ktor `ApplicationCall`만 adapter 공개 signature에 노출한다. header,
인증·인가, tenant 존재 확인, routing은 application consumer 소유로 남긴다.

**기술 기준:** Kotlin 2.4, Java/JVM 25, Gradle 9.7.0, Reactor Core, Ktor Server 3.x,
JUnit 5, bluetape4k assertions/JUnit5, Kover, Detekt, Maven Central SNAPSHOT

**설계:** `docs/superpowers/specs/2026-08-28-issue-1562-tenant-context-design.md`

**Stack:** `develop` (`18472064c594ab2dee835cff6695cd6ef9538ea5`) →
`feat/issue-1562-tenant-context`; 현재 spec commit
`9108d0e61e9edc5fce1c61ffb04b708a4d01f222`

---

## 실행 경계

- 세 artifact는 모두 JDK 25 전용이다. `virtualthread/api`, `virtualthread/jdk21`,
  Java 21 compatibility allowlist는 수정하지 않는다.
- Maven group은 실제 repository 계약인 `io.github.bluetape4k`다. Kotlin package
  `io.bluetape4k.*`와 혼동하지 않는다.
- `bluetape4k-tenant`에는 Reactor, Ktor, Servlet, Spring, coroutine 의존성을 추가하지 않는다.
- Reactor adapter에는 `kotlinx-coroutines-reactor`, global `Hooks`, automatic context
  propagation을 추가하지 않는다.
- Ktor adapter에는 plugin, header parser, status mapping, auth 또는 routing helper를 추가하지
  않는다.
- public mutable `set`/`clear`, default tenant, default parameter, global carrier singleton을
  제공하지 않는다.
- 새 외부 dependency를 도입하지 않는다. 현재 catalog의 Reactor/Ktor/JUnit dependency만
  사용한다.
- `bluetape4k-dependencies`와 두 workshop source는 이 계획에서 수정하지 않는다. 이 PR의
  산출물은 checksum을 가진 Projects SNAPSHOT handoff receipt까지다.
- push/PR은 구현·검증·독립 review·lesson 완료 뒤 수행한다. merge는 exact-head 상태를
  새로 읽고 별도 fresh 승인을 받기 전까지 실행하지 않는다.
- SNAPSHOT dispatch도 merge 뒤 workflow·target·credential·hold를 새로 확인한 후에만
  실행한다. stable release는 비범위다.

## 파일 구조와 책임

### Production과 test

| 파일 | 책임 |
| --- | --- |
| `bluetape4k/tenant/build.gradle.kts` | framework-free core, JUnit dependency, retention 전용 Test task |
| `bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/TenantId.kt` | blank 거부 value class |
| `bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/TenantContext.kt` | no-default 조회와 lexical binding 계약 |
| `bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/MissingTenantContextException.kt` | 공통 missing 예외와 exact message |
| `bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/ThreadLocalTenantContext.kt` | nested restore와 top-level remove |
| `bluetape4k/tenant/src/main/kotlin/io/bluetape4k/tenant/ScopedValueTenantContext.kt` | instance-private JDK 25 ScopedValue key |
| `bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/TenantIdTest.kt` | blank·non-normalization 계약 |
| `bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/TenantContextApiTest.kt` | public signature·missing 예외·default 인자 부재 계약 |
| `bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/ThreadLocalTenantContextTest.kt` | missing·nested·exception·same-thread cleanup·identity |
| `bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/ScopedValueTenantContextTest.kt` | dynamic scope·nested·exception·instance isolation |
| `bluetape4k/tenant/src/test/kotlin/io/bluetape4k/tenant/TenantContextRetentionStressTest.kt` | platform/virtual-thread overlap와 bounded retention |
| `bluetape4k/tenant-reactor/build.gradle.kts` | tenant core + Reactor public dependency, JUnit/Reactor Test test dependency |
| `bluetape4k/tenant-reactor/src/main/kotlin/io/bluetape4k/tenant/reactor/ReactorTenantContext.kt` | private key와 immutable Context 파생 |
| `bluetape4k/tenant-reactor/src/test/kotlin/io/bluetape4k/tenant/reactor/ReactorTenantContextTest.kt` | missing·interleave·nested·cancel·identity |
| `ktor/tenant/build.gradle.kts` | tenant core + Ktor server core public dependency, JUnit/Ktor Test Host test dependency |
| `ktor/tenant/src/main/kotlin/io/bluetape4k/ktor/tenant/TenantAlreadyBoundException.kt` | duplicate bind wiring 예외 |
| `ktor/tenant/src/main/kotlin/io/bluetape4k/ktor/tenant/KtorTenantContext.kt` | private holder/key와 atomic first bind |
| `ktor/tenant/src/test/kotlin/io/bluetape4k/ktor/tenant/KtorTenantContextTest.kt` | missing·collision·duplicate/concurrent·dispatcher·new/cancelled-call retention |
| 세 module의 `src/test/resources/junit-platform.properties`, `logback-test.xml` | repository test 실행·logging convention |

### 문서·CI·publication

| 파일 | 책임 |
| --- | --- |
| `bluetape4k/tenant/README.md`, `README.ko.md` | core/JDK carrier dependency와 lifecycle |
| `bluetape4k/tenant-reactor/README.md`, `README.ko.md` | Reactor subscription boundary와 unsupported bridge |
| `ktor/tenant/README.md`, `README.ko.md` | one-call/one-tenant와 application-owned plugin/auth |
| `README.md`, `README.ko.md` | Core/Ktor module inventory에 세 artifact 등록 |
| `.github/workflows/ci.yml` | tenant/tenant-reactor를 Core, ktor/tenant를 Ktor test·Kover에 포함 |
| `.github/workflows/nightly-tests.yml` | Nightly Core/Ktor test·Kover inventory 동기화 |
| `scripts/test_release_workflow_policy.py` | manual dispatch exact-head 입력과 receipt upload RED/GREEN |
| `.github/workflows/publish-snapshot.yml` | validated Nightly SHA checkout, post-publish receipt 생성·업로드 |
| `scripts/publication/create_snapshot_handoff.py` | Maven metadata/resource checksum을 fail-closed receipt로 생성 |
| `scripts/test_create_snapshot_handoff.py` | schema, checksum, stale/mutating metadata, receipt lifecycle, last-good manifest test |
| `docs/release-evidence/issue-1562/last-good-manifest.json` | base SHA와 rollback 가능한 직전 상태를 고정하는 train 중단 증거 |
| `docs/reviews/2026-08-28-issue-1562-tenant-context.md` | 구현 6관점 review와 finding disposition |
| `docs/lessons/2026-08-28-issue-1562-tenant-context.md` | 실패한 가정·교정·예방 rule과 검증 증거 |

`settings.gradle.kts`와 `bluetape4k/bom/build.gradle.kts`는 기존 자동 등록·동적 constraint를
사용하므로 원칙적으로 수정하지 않는다. `./gradlew projects` 또는 generated BOM 검증이
실패할 때만 원인을 확인하고 최소 변경을 별도 review한다.

## dependency order와 commit 경계

```text
Task 1 baseline/module routing
  -> Task 2 core value/API
  -> Task 3 ThreadLocal carrier
  -> Task 4 ScopedValue + stress
  -> Task 5 Reactor adapter
  -> Task 6 Ktor adapter
  -> Task 7 bilingual docs
  -> Task 8 CI + exact-head SNAPSHOT receipt
  -> Task 9 integration verification/review/lesson/PR
```

Task 2~6은 각 RED/GREEN 뒤 작은 Korean Lore commit으로 분리할 수 있다. Task 8은 workflow와
정책 test를 한 commit으로 유지한다. cross-repo consumer는 public SNAPSHOT receipt가 생긴
뒤에만 시작한다.

---

## Task 1: 기준선과 module routing을 고정한다

**Complexity:** 중간

**Files:** 세 `build.gradle.kts`, `.github/workflows/ci.yml`,
`.github/workflows/nightly-tests.yml`

- [ ] `git status --short`, `git rev-parse HEAD`, `./gradlew :bluetape4k-core:test` 결과를
      baseline evidence에 기록한다.
- [ ] 빈 module directory와 최소 build file을 만든다. core는
      `testImplementation(project(":bluetape4k-junit5"))`를 가진다. Reactor는
      `api(project(":bluetape4k-tenant"))`, `api(libs.reactor.core)`,
      `testImplementation(project(":bluetape4k-junit5"))`,
      `testImplementation(libs.reactor.test)`를 가진다. Ktor는
      `api(project(":bluetape4k-tenant"))`, `api(libs.ktor.server.core)`,
      `testImplementation(project(":bluetape4k-junit5"))`,
      `testImplementation(libs.ktor.server.test.host)`를 가진다.
- [ ] CI path filter에 `bluetape4k/tenant/**`, `bluetape4k/tenant-reactor/**`를 Core로
      포함하고 `ktor/tenant/**`가 기존 Ktor glob에 포함되는지 test/readback한다.
- [ ] Core/Ktor test와 Kover command에 세 project task를 추가하고 Nightly inventory도
      같은 project set으로 맞춘다. Core CI와 Nightly는 `tenantRetentionStress`를 required
      step으로 실행하고 해당 JUnit XML·실패 log·class histogram을 artifact에 포함한다.
      `Test / Core`가 실패하면 SNAPSHOT eligibility도 실패하는 연결을 policy test로 고정한다.

Run:

```bash
./gradlew projects --no-daemon --no-configuration-cache --no-build-cache
./gradlew :bluetape4k-tenant:dependencies \
  :bluetape4k-tenant-reactor:dependencies \
  :bluetape4k-ktor-tenant:dependencies \
  --configuration runtimeClasspath --no-daemon --no-configuration-cache
./gradlew :bluetape4k-tenant-reactor:dependencies \
  :bluetape4k-ktor-tenant:dependencies \
  --configuration testRuntimeClasspath --no-daemon --no-configuration-cache
python3 .github/scripts/test-ci-domain-parallelization.py -v
```

Expected: 세 project가 정확한 이름으로 나타나고 core runtime graph에는 Reactor/Ktor/Spring이
없다. CI domain graph와 YAML parse가 통과한다.

## Task 2: `TenantId`와 no-default API를 TDD로 고정한다

**Complexity:** 중간

**Depends on:** Task 1

- [ ] `TenantIdTest`에 `""`, whitespace-only가 정확한 `IllegalArgumentException`과
      `TenantId must not be blank`를 내는 RED를 작성한다.
- [ ] `TenantId(" clinic-a ")`가 원문을 보존해 application normalization을 대신하지
      않는 RED를 작성한다.
- [ ] `TenantContextApiTest` compile/behavior test에서 `TenantContext`의 `currentOrNull`,
      `requireCurrent`, `withTenant(tenantId) {}` signature, `MissingTenantContextException`의 exact
      type/message와 default 인자 부재를 고정한다. concrete carrier constructor는 해당 carrier의
      RED를 먼저 쓰는 Tasks 3~4에서 추가한다.
- [ ] 최소 production type을 구현하고 targeted test를 GREEN으로 만든다.

Run:

```bash
./gradlew :bluetape4k-tenant:test \
  --tests 'io.bluetape4k.tenant.TenantIdTest' \
  --tests 'io.bluetape4k.tenant.TenantContextApiTest' \
  --rerun-tasks --no-daemon --no-configuration-cache --no-build-cache
```

## Task 3: ThreadLocal lifecycle을 TDD로 구현한다

**Complexity:** 높음

**Depends on:** Task 2

- [ ] unbound `currentOrNull == null`, `requireCurrent`의 exact 공통 예외 RED를 작성한다.
- [ ] outer/inner success, inner exception, top-level exception 뒤 outer 복원 또는 remove를
      검증한다.
- [ ] 고정 8-thread executor에서 100 tenant의 순차·overlap 실행을 barrier로 섞고 각 block
      안의 값과 종료 뒤 unbound를 검증한다.
- [ ] 서로 다른 `ThreadLocalTenantContext` instance가 값을 공유하지 않는 identity RED를
      작성한다.
- [ ] public zero-argument `ThreadLocalTenantContext()` construction을 compile test로 고정한다.
- [ ] private `ThreadLocal<TenantId>`와 `try/finally` restore/remove만으로 GREEN을 만든다.
      public `set`/`clear`는 만들지 않는다.
- [ ] barrier test에 hard timeout을 걸고 executor shutdown/awaitTermination과 barrier abort를
      `finally`에서 수행한다. carrier/key construction count와 source inspection으로 application
      singleton당 한 instance이며 request마다 새 carrier/key를 만들지 않음을 고정한다.

Run:

```bash
./gradlew :bluetape4k-tenant:test \
  --tests 'io.bluetape4k.tenant.ThreadLocalTenantContextTest' \
  --rerun-tasks --no-daemon --no-configuration-cache --no-build-cache
```

## Task 4: ScopedValue와 retention stress를 TDD로 구현한다

**Complexity:** 높음

**Depends on:** Task 2

- [ ] unbound, nested success/failure, block 종료 비오염, instance-private key RED를 작성한다.
- [ ] public zero-argument `ScopedValueTenantContext()` construction을 compile test로 고정한다.
- [ ] `ScopedValue.where(key, tenantId)`의 JDK 25 lexical scope로 구현한다. raw `get()` 전에는
      반드시 `isBound`를 확인한다.
- [ ] JDK 25 `StructuredTaskScope` fork 안에서는 lexical binding이 상속되고, 단순히 새로
      시작한 unrelated virtual thread에는 자동 전파를 약속하지 않는 positive/negative
      fixture를 둔다. root의 기존 `--enable-preview` 계약 안에서만 사용한다.
- [ ] 일반 coroutine dispatcher hop 자동 전파를 API/test 이름으로 약속하지 않는다.
- [ ] 8 platform thread, 100 tenant, 10,000 virtual-thread task를 60초 안에 완료하고
      expected/observed tenant가 모두 같으며 종료 뒤 unbound인지 확인한다.
- [ ] unique non-interned sentinel의 `WeakReference`/`ReferenceQueue`를 최대 10초 확인한다.
      불안정하거나 sentinel이 남으면 통과 처리하지 않고 log와 class histogram을 blocker
      evidence로 남긴다.
- [ ] `TenantContextRetentionStressTest`를 `tenant-retention-stress` JUnit tag로 분리하고
      `bluetape4k/tenant/build.gradle.kts`에 같은 test source set/classpath를 사용하는
      `tenantRetentionStress` `Test` task를 등록한다. 일반 `test`에는
      `excludeTags("tenant-retention-stress")`, custom task에는
      `includeTags("tenant-retention-stress")`를 명시하며 CI, Nightly, release verification이
      custom task를 required gate로 실행한다.
- [ ] 모든 barrier, `StructuredTaskScope`, executor와 virtual thread 실행에 hard timeout을
      적용하고 `close`/`shutdown`/`join`과 strong reference 해제를 `finally`에서 수행한다.

Run:

```bash
./gradlew :bluetape4k-tenant:test \
  --tests 'io.bluetape4k.tenant.ScopedValueTenantContextTest' \
  --rerun-tasks --no-daemon --no-configuration-cache --no-build-cache
./gradlew :bluetape4k-tenant:tenantRetentionStress \
  --rerun-tasks --no-daemon --no-configuration-cache --no-build-cache
```

## Task 5: Reactor Context adapter를 TDD로 구현한다

**Complexity:** 높음

**Depends on:** Task 2

- [ ] private object key collision, 원본 `Context` identity/비오염, nested derived context,
      missing 공통 예외 RED를 작성한다.
- [ ] single-thread scheduler에서 두 subscription을 barrier로 interleave하고 각 subscriber가
      자기 tenant만 읽는지 검증한다.
- [ ] cancellation 뒤 외부 context가 unbound이고 global hook이 설치되지 않았음을 검증한다.
- [ ] `withTenant(Context, TenantId)`가 subscription boundary당 한 번 호출되는 fixture를
      두고 signal당 `put` 구현을 금지한다.
- [ ] `ReactorTenantContext` object와 private key만으로 GREEN을 만든다.
- [ ] barrier에 hard timeout을 적용하고 scheduler/disposable/cancellation을 `finally`에서
      정리한다. private key가 object 초기화 때 한 번만 만들어지고 request/signal마다 key를
      만들지 않는지 source와 behavior test로 확인한다.

Run:

```bash
./gradlew :bluetape4k-tenant-reactor:test \
  --tests 'io.bluetape4k.tenant.reactor.ReactorTenantContextTest' \
  --rerun-tasks --no-daemon --no-configuration-cache --no-build-cache
```

## Task 6: Ktor one-call/one-tenant adapter를 TDD로 구현한다

**Complexity:** 높음

**Depends on:** Task 2

- [ ] unbound 조회가 core `MissingTenantContextException`을 그대로 던지는 RED를 작성한다.
- [ ] private `TenantBinding` holder와 versioned key name을 사용해 외부
      `AttributeKey<TenantId>` collision이 값을 읽지 못하는 RED를 작성한다.
- [ ] 같은 call의 두 번째 bind가 exact `TenantAlreadyBoundException`을 던지고 기존 값을
      덮어쓰지 않는 RED를 작성한다.
- [ ] start barrier로 서로 다른 tenant를 같은 call에 동시에 bind해 정확히 한 성공,
      N-1 exact failure, 최종 winner value를 100회 반복 검증한다.
- [ ] `ApplicationCall.attributes` monitor 안의 check-and-put으로 최초 binding을 선형화한다.
- [ ] `testApplication`에서 dispatcher hop, exception, cancellation, 새 call 격리를 검증한다.
      plugin/header/status API는 추가하지 않는다.
- [ ] cancelled call의 `WeakReference`/`ReferenceQueue`를 bounded timeout으로 확인하고 adapter가
      call/global registry를 retain하지 않음을 검증한다. concurrent coroutine은 hard timeout,
      cancel/join, barrier abort를 `finally`에서 수행하며 lock scope가 attributes의 짧은
      check-and-put만 포함하는지 inspection한다.

Run:

```bash
./gradlew :bluetape4k-ktor-tenant:test \
  --tests 'io.bluetape4k.ktor.tenant.KtorTenantContextTest' \
  --rerun-tasks --no-daemon --no-configuration-cache --no-build-cache
```

## Task 7: 양언어 문서와 dependency 좌표를 맞춘다

**Complexity:** 중간

**Depends on:** Tasks 3~6

- [ ] 세 README 쌍에 `io.github.bluetape4k`, `2.0.0-SNAPSHOT`, JDK 25, no-default,
      carrier lifecycle, 전체 unsupported boundary, 최소 사용 예제를 각각 쓴다. BOM은 exact
      `platform("io.github.bluetape4k:bluetape4k-bom:2.0.0-SNAPSHOT")`, 개별 artifact는
      Gradle Kotlin DSL/Maven 예시를 제공한다.
- [ ] SNAPSHOT 예시는 Central snapshots repository
      `https://central.sonatype.com/repository/maven-snapshots/`, changing module 선언과
      `cacheChangingModulesFor(0, "seconds")` 조건을 포함하고 실제 sample dependency resolution로
      검증한다.
- [ ] ThreadLocal singleton + lexical block, ScopedValue coroutine 비보장, Reactor immutable
      derived context/global hook 미설치, Ktor application-owned plugin/auth를 명시한다.
- [ ] raw header를 직접 `TenantId`로 만들지 않는 application enum/domain mapping 예시를
      포함한다. raw header/token/tenant를 log, exception, MDC, metric tag에 기록하지 않고
      synthetic fixture만 expected/observed 값을 쓸 수 있음을 명시한다. library에는 logging/
      metric backend를 추가하지 않는다. consumer의 optional metric은 enum-only `carrier`/`stage`,
      기존 correlation ID, 5분 구간 alert와 workshop maintainer/release coordinator owner를 쓴다.
- [ ] root README의 Core section에 tenant/tenant-reactor, Ktor section에 tenant를 추가하고
      EN/KO 의미를 맞춘다.
- [ ] reader-facing KDoc은 한국어로 작성하고 identifier·coordinate는 원문을 유지한다.

Run:

```bash
node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  --series clinic-appointment \
  bluetape4k/tenant/README.ko.md \
  bluetape4k/tenant-reactor/README.ko.md \
  ktor/tenant/README.ko.md README.ko.md
git diff --check
```

## Task 8: exact-head SNAPSHOT workflow와 handoff receipt를 fail-closed로 만든다

**Complexity:** 높음

**Depends on:** Tasks 1~7

- [ ] `publish-snapshot.yml`의 자동 `workflow_run` publish trigger를 제거하고 승인된
      `workflow_dispatch`만 남긴다. `scripts/test_release_workflow_policy.py`는 manual dispatch가
      `verified_ci_run_id`, `expected_head_sha`, `handoff_issue_number`를 모두 요구하고, 승인 없는
      자동 trigger/fallback이 있으면 실패하는 RED를 가진다.
- [ ] verified run의 workflow path, completed/success conclusion, `head_sha`와 현재
      `develop` ref가 모두 `expected_head_sha`와 같은지 검증한다. path/conclusion/head/current-ref
      mismatch fixture를 각각 둔다. receipt의 `verified_ci_run_id`에는 이 exact run ID만 기록한다.
- [ ] checkout이 validation output SHA만 사용하고 branch 이름/current checkout에 fallback하지
      않는 RED를 추가한다.
- [ ] receipt schema `bluetape.snapshot-handoff/v1`의 `repository`, `merge_sha`,
      `verified_ci_run_id`, `publication_run_id`, `handoff_issue_number`, `group`, `artifact`, `base_version`, `timestamp`,
      `build_number`, `last_updated`, `resources`, `catalog_commit_sha=null`, `created_at`, `status`,
      `supersedes`를 test로 고정한다. 최초 receipt는 `verified`, 실패 receipt는 기존 파일을
      수정하지 않고 digest로 연결한 append-only `rejected`다.
- [ ] immutable artifact name, 90일 retention, `actions/upload-artifact@v7`, artifact ID/digest와
      Korean linked-issue comment를 policy test로 고정한다. validation은 `contents: read`와
      `actions: read`, publish는 `contents: read`, 별도 issue 기록 job만 `issues: write`를 가지며
      signing/Central secret은 publish step에만 scope한다.
- [ ] `create_snapshot_handoff.py`는 Maven metadata를 두 번 읽고 timestamp/build number/
      `lastUpdated`가 호출 전후 같을 때만 BOM과 세 artifact POM/JAR SHA-256을 계산한다.
- [ ] fixture HTTP server로 success, missing metadata/resource, checksum mismatch, 호출 중 metadata
      mutation, wrong group/artifact를 검증한다. network를 사용하는 test는 만들지 않는다.
- [ ] workflow가 publish 후 bounded retry로 public Central metadata/resource를 확인하고 receipt를
      생성한다. publish job과 retry에 명시적 hard timeout을 두고 timeout이면 job 자체를
      실패시키며 downstream issue를 시작하거나 issue에 success를 기록하지 않는다.
- [ ] immutable artifact name에 merge SHA, timestamp, build number를 넣고 job summary에 run ID,
      artifact ID/digest, resource checksum을 기록한다. 같은 증거를 linked issue에 Korean
      comment로 기록하며 raw tenant/secret은 출력하지 않는다.
- [ ] `last-good-manifest.json`은 base SHA, dependency coordinate, resolved timestamp/build number,
      catalog commit SHA, resource checksum과 통과한 command/run ID를 기록한다. schema test와
      read-back을 통과하지 않으면 train을 시작하거나 재개하지 않는다.

Run:

```bash
python3 -m unittest scripts/test_release_workflow_policy.py -v
python3 -m unittest scripts/test_create_snapshot_handoff.py -v
ruby scripts/publication/publication_inventory_audit_test.rb
ruby scripts/publication/publication_pom_integration_test.rb
ruby scripts/publication/publication_module_metadata_audit_test.rb
```

## Task 9: 통합 검증, review, lesson과 PR evidence를 만든다

**Complexity:** 높음

**Depends on:** Tasks 1~8

- [ ] 세 module targeted test에서 실제 JUnit test count와 skipped=0을 기록한다.
- [ ] module registration, dependency graph, Detekt, Kover, generated POM/module metadata와 BOM
      constraints를 fresh command로 확인한다.
- [ ] 6관점 독립 code review를 수행하고 P0/P1=0, P2/P3 fixed 또는 rationale-deferred를
      `docs/reviews/...`에 기록한다.
- [ ] lesson을 `실패한 가정/판단 → 발견 증거 또는 교정 → 수정 결정 → 향후 예방 확인`
      형식으로 기록한다. 최소한 Maven group 오기와 review lane deadline 회수를 포함한다.
- [ ] exact branch/base, commit list, test evidence, changed-path inventory를 읽고 Korean PR을 만든다.
      PR은 `Refs #1562`, #1320/#1552 추적 링크와 cross-repo #213/#255/#215 dependency order를
      포함한다.
- [ ] production source에 logger/MDC/metric/global hook이 없고 workflow permission이 최소이며
      publish secret이 step-local인지 source/policy test로 검증한다.
- [ ] noisy benchmark를 release gate로 추가하지 않는다. per-signal `Context.put`, request별
      carrier/key 생성 또는 넓은 Ktor lock을 inspection에서 발견하면 구현을 고치고, 안정적인
      baseline이 필요한 후속 benchmark는 duplicate 확인 뒤 별도 Korean issue로 등록한다.

Run:

```bash
repo-test-summary -- ./gradlew :bluetape4k-tenant:test \
  :bluetape4k-tenant-reactor:test \
  :bluetape4k-ktor-tenant:test \
  --rerun-tasks --no-daemon --no-configuration-cache --no-build-cache

repo-test-summary -- ./gradlew :bluetape4k-tenant:tenantRetentionStress \
  --rerun-tasks --no-daemon --no-configuration-cache --no-build-cache

./gradlew :bluetape4k-tenant:check \
  :bluetape4k-tenant-reactor:check \
  :bluetape4k-ktor-tenant:check \
  :bluetape4k-tenant:koverXmlReport \
  :bluetape4k-tenant-reactor:koverXmlReport \
  :bluetape4k-ktor-tenant:koverXmlReport \
  --no-daemon --no-configuration-cache --no-build-cache

./gradlew detekt checkDisabledTests \
  --no-daemon --no-configuration-cache --no-build-cache

./gradlew projects --no-daemon --no-configuration-cache --no-build-cache
./gradlew exportManualModuleInventory --no-configuration-cache
./gradlew -p buildSrc test --no-daemon --no-configuration-cache --no-build-cache

./gradlew generatePomFileForBluetape4kPublication \
  generateMetadataFileForBluetape4kPublication \
  -PsnapshotVersion=-SNAPSHOT \
  --no-daemon --no-configuration-cache --no-build-cache
ruby scripts/publication/validate_poms.rb
ruby scripts/publication/validate_module_metadata.rb

python3 -m unittest scripts/test_release_workflow_policy.py \
  scripts/test_create_snapshot_handoff.py -v
find bluetape4k/tenant bluetape4k/tenant-reactor ktor/tenant \
  -path '*/build/reports/kover/*.xml' -type f -size +0c
if rg -n 'LoggerFactory|KotlinLogging|MDC|MeterRegistry|Hooks\.' \
  bluetape4k/tenant/src/main bluetape4k/tenant-reactor/src/main ktor/tenant/src/main; then
  exit 1
fi
git diff --check
```

Expected:

- 세 artifact의 targeted test와 check가 fresh 실행되고 JUnit XML의 실제 count가 0보다 크며
  skipped=0이다. retention custom task도 같은 machine-check를 통과한다.
- core outgoing/runtime dependency에 Reactor/Ktor/Servlet/Spring이 없다.
- generated BOM에 `bluetape4k-tenant`, `bluetape4k-tenant-reactor`,
  `bluetape4k-ktor-tenant` constraint가 정확히 한 번씩 있다.
- Detekt source coverage에 빈 신규 module이 없고 세 Kover XML이 존재하며 비어 있지 않다.
- workflow policy와 offline receipt fixture가 fail-closed 경로를 모두 검증한다.
- PR exact head에서 required CI가 모두 terminal success이기 전에는 merge-ready로 보고하지
  않는다.

## PR 이후 release train handoff

1. PR exact head, CI, review thread, mergeability를 다시 읽는다.
2. fresh merge 승인을 받은 뒤에만 rebase merge하고 실제 merge SHA를 확인한다.
3. merge SHA에서 README/KDoc의 dependency, lifecycle, unsupported boundary, 비노출 계약과
   `last-good-manifest.json`을 다시 읽는다. manifest 검증이 없으면 train을 중단한다.
4. merge SHA의 full Nightly exact-head 성공과 manual-only dispatch hold를 확인한다.
5. 별도 fresh 승인 범위 안에서 `verified_ci_run_id`, `expected_head_sha`,
   `handoff_issue_number=1562`로 SNAPSHOT workflow를 dispatch한다. immutable artifact를
   repository/run/artifact ID로 다운로드하고 GitHub artifact digest, schema, merge/CI SHA,
   public resource checksum을 검증한다.
6. public BOM POM과 세 artifact의 timestamp/build number/`lastUpdated`/checksum이 receipt와
   일치할 때만 Dependencies #213을 시작한다. 90일이 지났거나 mutable metadata가 바뀌면 같은
   coordinate를 재사용하지 않고 새 exact-head CI/publish receipt를 만든다.
7. Dependencies #213은 별도 최신 base/head, PR, exact-head CI, fresh merge 승인, SNAPSHOT publish,
   public POM/catalog alias 해석과 append-only handoff receipt를 모두 통과해야 한다. 그 전에는
   workshop을 시작하지 않는다.
8. exposed-workshop #255와 exposed-r2dbc-workshop #215는 같은 verified Dependencies build로
   시작하고 missing/blank/conflicting/unknown/unauthorized tenant가 binding 전에 실패하는
   consumer fixture를 포함한다. 두 consumer lane은 서로 독립이므로 이 gate 뒤 병렬화한다.
9. 어느 downstream이 실패하든 기존 receipt를 수정하지 않고 `supersedes`가 원 receipt digest를
   가리키는 `rejected` receipt를 추가한다. merge 전에는 dependency 변경을 되돌리고 merge 후에는
   revert PR을 사용하며, last-good manifest 기준 복원 test 증거 전에는 train을 재개하지 않는다.

## 독립 검토 결과

| 관점 | P0 | P1 | P2 | P3 | disposition |
| --- | ---: | ---: | ---: | ---: | --- |
| Developer/API | 0 | 1 | 2 | 0 | adapter test dependency, API test inventory, fresh 실행을 계획에 반영 |
| Operations/release | 0 | 3 | 2 | 0 | manual-only 승인 hold, exact CI identity, rollback/receipt/Dependencies gate 반영 |
| User/caller docs | 0 | 2 | 2 | 0 | SNAPSHOT repository/cache, observability, README/merged read-back 반영 |
| Security | 0 | 1 | 2 | 0 | retention release gate, pre-binding negative, permission/non-disclosure check 반영 |
| Performance | 0 | 1 | 1 | 1 | key lifetime, per-signal 금지, benchmark deferral와 후속 issue 조건 반영 |
| Stability | 0 | 2 | 2 | 0 | hard timeout/cleanup, tag wiring, CI artifact와 machine count 반영 |
| 합계 | 0 | 10 | 11 | 1 | 중복 finding을 통합해 모두 fixed; unresolved P0/P1=0 |

초기 전체-file 검토가 5분 command deadline을 넘긴 세 lane은 결과를 사용하지 않고 회수했다.
같은 agent를 더 좁은 write-scope 없는 계약 검토로 재배정해 위 표의 결과를 얻었다. leader는
모든 finding을 design line과 대조해 반영했으며, P2/P3도 defer하지 않고 실행 항목으로 승격했다.

## 계획 DoD

- [x] 승인된 spec의 모든 수용 기준이 Task와 fresh 검증 command에 연결됐다.
- [x] production/test/doc/CI/publication 파일과 write scope가 명시됐다.
- [x] 각 production task가 RED → GREEN 순서를 가진다.
- [x] module registration, JDK25, dependency leakage, Ktor concurrency, ThreadLocal retention,
      Reactor cancellation, SNAPSHOT mutability hazard가 fail-closed test로 배정됐다.
- [x] 독립 6관점 plan review에서 unresolved P0=0/P1=0이다.
- [x] 계획과 spec coordinate 교정이 같은 commit으로 고정되고 plan 작성 범위가 clean하다.
