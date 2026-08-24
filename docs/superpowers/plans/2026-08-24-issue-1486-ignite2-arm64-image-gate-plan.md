# Issue #1486 Ignite2 ARM64 image gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `test-driven-development`,
> `subagent-driven-development`, and the matching Bluetape Kotlin patterns.
> Execute the checked steps in order and preserve each RED/GREEN result as
> evidence.

## 목표와 완료 조건

Issue #1486의 `Ignite2ServerTest`를 실제 startup·thin-client cache workload로
전환하고, `apacheignite/ignite:2.18.0` amd64와
`apacheignite/ignite:2.18.0-arm64` arm64의 native pull·startup·workload·JUnit
증거를 fail-closed로 검증한다. PR CI에는 Docker runtime을 추가하지 않으며,
Nightly/Release에서만 x64 권위 실행과 arm64 전용 gate를 수행한다.

완료 조건은 다음과 같다.

- 기준선 `Ignite2ServerTest`의 `3/3 skipped`가 보존된 RED 증거 뒤에 대표 test가
  unskip되어 startup과 `Ignition.startClient` cache `put`/`get`을 통과한다.
- runner/manifest/parser의 schema v2가 pull event·digest·platform·startup
  marker·workload testcase·JUnit 실행 수를 필수화하고, XML/path/secret/output
  경계를 넘으면 `blocked`가 된다.
- x64는 full gate에서 정확히 한 번 amd64를 실행하고, arm64는
  `--family-id ignite2 --platform-id arm64 --require-selection` 전용 native job에서
  정확히 한 번 실행한다. 일반 `storage-cache` matrix와 PR runtime에는 중복 실행이
  없다.
- x64 strict/JUnit timeout은 6분, arm64 test timeout은 30분이며 worst-case budget은
  각각 318분/84분으로 job timeout 360분/90분 이내다.
- public `Ignite2Server` constructor/function signature와 기존 family의 disabled
  계약을 유지한다. EN/KO README와 KDoc의 tag/platform/lifecycle 설명이 source·test·
  manifest와 일치한다.
- `actionlint`, Python unit/static tests, targeted Gradle test/compile, `detekt`,
  `git diff --check`와 새 head의 CI evidence가 모두 통과한다. 실제 ARM runner/pull
  증거는 첫 Nightly/Release 실행에서 확인한다.

## 실행 경계와 stacked train

- 설계 기준 커밋: `b301aac98` (`fix/1486-ignite2-arm64`). 이 커밋의 spec/review는
  수정 후 Step 2-R `P0=0/P1=0`, P2 WATCH 상태를 보존한다.
- timeout authority는 설계 §5.1/§5.4와 이 계획의 목표를 함께 따른다. amd64
  `testMinutes=6`, 대표 JUnit 6분(5분 startup + 1분 workload 여유), x64 budget
  318분/360분을 단일 값으로 사용한다. 다른 timeout/budget 값이 발견되면 구현을
  중단하고 plan/spec drift로 기록한다.
- Parent PR은 `develop`을 base로 하는
  `fix/1486-image-gate-runner` worktree에서 runner/parser/schema/manifest
  validator와 독립 synthetic fixture만 소유한다. canonical
  `scripts/testcontainers_image_gate_manifest.json`은 read-only로 읽으며,
  parent는 그 파일의 family payload를 수정하지 않는다. JSON-pointer
  `/families/*`는 child hunk allowlist로만 변경할 수 있다.
- Child PR은 parent head를 base로 하는
  `fix/1486-ignite2-arm64-runtime` worktree에서 `Ignite2Server` source/test,
  test-only dependency, EN/KO README/KDoc, canonical manifest의
  `families[id=ignite2]` object와 Nightly/Release workflow를 소유한다. child는
  top-level schema/defaults나 다른 family object를 수정하지 않는다.
- Parent가 먼저 merge될 때까지 child PR을 독립 merge하지 않는다. 각 PR 생성 전
  exact `base/head/merge-base`와 required CI를 fresh-read하고, merge는 별도 fresh
  승인 없이는 수행하지 않는다. auto-merge, tag, release, publish, branch deletion,
  force push는 이 계획의 범위가 아니다.
- 두 worktree의 Testcontainers-backed test는 shared Docker daemon 경합을 피하도록
  병렬 실행하지 않는다. parent static/unit lane과 child Gradle compile은 독립적일
  때만 병렬화하고, Docker runtime/ARM CI는 순차적으로 관찰한다.

## 공통 준비와 기준선

### Task 0 — 기준선·worktree·authority 고정

**Files:** read-only except workflow evidence.

- [ ] `AGENTS.md`, workspace guide, `bluetape-workflow`,
      `bluetape-kotlin-patterns`, `test-driven-development`, `subagent-driven-development`
      및 이 계획을 다시 읽는다.
- [ ] live Issue #1486, milestone/assignee/labels, current `develop`, existing PR
      and duplicate paths를 `gh`로 재확인한다. Issue acceptance와 설계의 52-family
      count가 다르면 구현하지 않고 계획을 갱신한다.
- [ ] `git status --short --branch`, `git rev-parse HEAD`,
      `git rev-parse origin/develop`, `git merge-base --is-ancestor`를 parent/child
      worktree 각각에 기록한다. canonical develop worktree는 읽기 전용으로 둔다.
- [ ] baseline을 새 head에서 다시 실행한다.

```bash
repo-test-summary -- ./gradlew :bluetape4k-testcontainers:test \
  --tests 'io.bluetape4k.testcontainers.storage.Ignite2ServerTest' \
  --no-build-cache --no-configuration-cache
python3 -m unittest scripts.test_testcontainers_contract -v
```

Expected baseline은 XML `tests=3`, `skipped=3`, `failures=0`, process success이며,
runtime evidence가 없는 green 결과로 해석하지 않는다.

### Task 1 — Parent/child worktree 생성과 train receipt

- [ ] 현재 spec commit에서 parent를 만든다.

```bash
git worktree add -b fix/1486-image-gate-runner \
  .worktrees/fix/1486-image-gate-runner fix/1486-ignite2-arm64
```

- [ ] parent 구현이 안정화된 뒤에만 child를 parent head에서 만든다.

```bash
git worktree add -b fix/1486-ignite2-arm64-runtime \
  .worktrees/fix/1486-ignite2-arm64-runtime fix/1486-image-gate-runner
```

- [ ] 각 branch에 source ownership, allowed paths, base SHA, expected CI와 rollback
      조건을 workflow receipt에 남긴다. unrelated dirty path는 보존하고 되돌리지
      않는다.
- [ ] 이 plan을 spec branch에 먼저 commit하고 plan commit SHA를 receipt에 기록한
      뒤에만 parent/child worktree를 생성한다. untracked plan으로 implementation
      worktree를 만들지 않는다.

## Parent lane — runner/parser/schema

### Task 2 — RED: manifest·selector·XML·security fixture

**Parent files:**

- `scripts/testcontainers_image_gate.py`
- `scripts/test_testcontainers_contract.py`
- `scripts/test_run_testcontainers_image_gate.py` (신규)

- [ ] 현재 52-entry canonical manifest를 read-only baseline으로 보존한 뒤, synthetic
      fixture에서 `executionEvidenceRequired` 미지정 family가 `false`이고 Ignite2만
      strict opt-in인 RED fixture를 추가한다. Parent는 canonical manifest를
      수정하지 않는다.
- [ ] exact selector의 0개/복수개/unknown ID, `defaultPlatformId=amd64`, x64 full
      resolver와 arm64 `--require-selection`을 검증하는 테스트를 먼저 추가한다.
- [ ] XML fixture에 missing/stale/all-skipped/tests=0/malformed/DTD-ENTITY/
      symlink/path escape/suite mismatch/counter limit과 Kotlin `()` suffix
      canonicalization을 추가한다. workload testcase와 suite `tests`를 별도 집계한다.
- [ ] image/daemon/runner OS·architecture alias, empty digest, local-vs-remote
      Docker context, registry mirror, safe ID/reference grammar을 fail-closed로
      검증하는 fixture를 추가한다.
- [ ] raw/decoded/base64 Docker auth, basic-auth URL, multiline/known secret,
      arbitrary JVM property/Gradle option이 output·exception·JSON·Markdown에
      남지 않는 redaction/allowlist fixture를 추가한다.
- [ ] pull event correlation, requested ref, cache state, image ID/digest reuse,
      pull transport-only retry, attempt root/container/XML/marker isolation을
      fake subprocess로 검증한다.
- [ ] output cap(64 KiB/1,000 lines, diagnostic 64 KiB/500 lines, family 128 KiB,
      summary 1 MiB, artifact 8 MiB), per-command/job deadline, process-group kill,
      318/84 budget formula를 RED fixture로 고정한다.
- [ ] synthetic 52-family worst-case fixture가 bounded result 축약 후 전체 artifact
      8 MiB 이하임을 검증하고, checkout/setup/mock pre-step를 포함한 고정 30분
      setup slack과 실제 subprocess wall-clock 누계를 job-budget guard가 비교하게
      한다. 이 fixture 또는 wall-clock 증거가 없으면 최종 DoD를 `PENDING`으로
      유지한다.
- [ ] 이 단계에서는 production runner behavior를 수정하지 않고 의도한 실패와
      failure classification을 raw evidence로 보존한다.

### Task 3 — GREEN: runner/parser/schema implementation

**Parent files:**

- `scripts/run_testcontainers_image_gate.py`
- `scripts/testcontainers_image_gate.py`
- `scripts/test_run_testcontainers_image_gate.py`
- `scripts/test_testcontainers_contract.py`

- [ ] capped streaming/temp-file subprocess helper와 process-group deadline을
      구현한다. `capture_output=True` 같은 unbounded path를 사용하지 않는다.
- [ ] fresh staging root와 safe ID/path containment, regular non-symlink XML allowlist,
      bounded parser, DTD/ENTITY 차단, exact suite/workload matcher를 구현한다.
- [ ] pull 전 bounded Docker event observer와 dedicated ephemeral `DOCKER_CONFIG`
      pull subprocess를 구현한다. Gradle/test/diagnostic에는 sanitized env만
      전달하고 pull 성공 후 credential/temp config를 삭제한다.
- [ ] image inspect/docker info/context/uname allowlist와 canonical architecture,
      local Unix socket, Docker Hub/mirror 정책을 구현한다.
- [ ] per-family schema v2와 summary schema v2를 구현한다. v1/partial/skipped,
      missing pull/digest/workload/marker를 성공으로 완화하지 않는다.
- [ ] attempt state machine을 `pull → test → evidence → cleanup`으로 고정한다.
      x64 `max_attempts=1`은 in-job retry 없음, arm64만 `max_attempts>1`에서
      verified digest를 재사용하는 transient retry를 허용한다.
- [ ] generic family는 기존 process-success 계약을 유지하고 strict family만
      `executionEvidenceRequired=true` 검사로 분기한다. release verifier가
      `coverage`, `release_gate`, platform/tag/architecture/digest/workload를 직접
      검증하도록 static helper를 노출한다.
- [ ] parent RED fixture를 모두 GREEN으로 전환하고, fake runner가 product failure,
      infrastructure failure, blocked를 혼동하지 않음을 확인한다.

## Child lane — Ignite2 runtime/manifest/workflow

### Task 4 — RED: runtime·dependency·docs·workflow contract

**Child files:**

- `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/Ignite2Server.kt`
- `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/storage/Ignite2ServerTest.kt`
- `testing/testcontainers/build.gradle.kts`
- `scripts/testcontainers_image_gate_manifest.json`
- `.github/workflows/nightly-tests.yml`
- `.github/workflows/release.yml`
- `testing/testcontainers/README.md`, `testing/testcontainers/README.ko.md`,
  source KDoc

- [ ] `@Disabled` 제거, 기본 생성자 대표 workload, `Ignition.startClient`,
      `ClientConfiguration` connect/request 30초, `use`/`finally`, marker-before-
      workload 테스트를 작성한다. baseline `3/3 skipped`는 Task 0의 별도 기준선
      증거로 보존하고, 그 뒤 test-only dependency와 대표 test를 추가한 다음
      class-level `@Disabled`만 제거한다. production source를 수정하기 전에
      실제 startup/client assertion failure를 실행·기록해 RED를 만든다.
- [ ] `DEFAULT_TAG` x86_64/amd64와 aarch64/arm64 mapping, explicit override,
      canonical no-tag unknown fail-fast, custom image/tag unknown-arch lazy 경계를
      검증하는 JVM fixture를 작성한다. canonical no-tag, custom no-tag fail-fast,
      custom explicit-tag 허용, `DockerImageName` overload의 tagless 동작을
      분리해 테스트·KDoc·EN/KO README에 고정한다. public constructor/function
      descriptor는 변경하지 않는다.
- [ ] central catalog의 `bt4k.ignite.core` test-only dependency와
      `org.apache.ignite:ignite-core:2.18.0` resolution/published POM exclusion을
      검증하는 Gradle fixture를 작성한다.
- [ ] canonical manifest의 `families[id=ignite2]` object에만 `platforms`,
      `defaultPlatformId`, `executionEvidenceRequired`, workload pattern, platform
      timeout, pull evidence fields를 추가하는 RED contract를 만든다. top-level
      schema와 다른 family object는 parent head에서 그대로 보존한다.
- [ ] PR workflow에 runtime invocation이 없고, Nightly/Release x64/arm64 job
      selector·runner·needs·artifact literal·retention·timeout·budget·mock Jib
      exclusion이 정확한지 검증하는 static fixture를 먼저 작성한다.

### Task 5 — GREEN: Ignite2 source/test/dependency/docs

- [ ] `DEFAULT_TAG`를 eager companion property가 아닌 lazy getter/helper 또는
      sentinel 경계로 구현한다. explicit `tag`/custom image 경로는 unknown arch에서
      default resolver를 호출하지 않는다.
- [ ] `representativeStartupAndWorkload`에서 기본 `Ignite2Server()`로 container를
      시작하고 readiness marker를 evidence root에 기록한 뒤 thin-client cache
      `put`/`get`을 검증한다. client/container는 모든 예외·timeout 경로에서 닫힌다.
- [ ] 기존 port/blank image/tag/explicit override 계약을 유지하고 public API
      signature를 변경하지 않는다. JUnit timeout은 6분(5분 startup + 1분 workload
      여유)으로 둔다.
- [ ] `build.gradle.kts`에 중앙 catalog alias만 test scope로 추가하고 dependency
      graph 및 published POM에 production dependency가 유입되지 않음을 확인한다.
- [ ] EN/KO README와 KDoc에 `2.18.0`, `2.18.0-arm64`, OS/architecture mapping,
      canonical/custom/unknown policy, `Ignition.startClient` lifecycle을 동일하게
      문서화한다. reader-facing prose는 한국어 정책을 따른다.

### Task 6 — GREEN: manifest/workflow integration

- [ ] 일반 Nightly `storage-cache`에서 Ignite2를 제거하고 기존 x64 full gate의
      strict resolver를 amd64 authoritative 실행으로 고정한다.
- [ ] Nightly/Release에 `ubuntu-24.04-arm` Ignite2 전용 job을 추가한다. arm64
      final Gradle argv는 고정
      `-Dtestcontainers.image-gate.evidence-dir=$RUN_EVIDENCE_DIR`와 두 mock Jib
      `-x` exclusion을 포함하며 arbitrary task/JVM option을 받지 않는다.
- [ ] x64 job은 `max_attempts=1`, strict 6분, budget 360/timeout 360; arm64 job은
      `max_attempts=2`, 5/30/2분, budget 90/timeout 90을 사용한다. full aggregate와
      Release publish는 x64·arm64 `success`를 모두 요구하고 targeted scope에서만
      image-gate skipped를 허용한다.
- [ ] x64 runner CLI를 다음 고정 argv로 보존한다.

```text
python3 scripts/run_testcontainers_image_gate.py --scope full --report-dir build/reports/testcontainers-image-gate --default-platform-id amd64 --max-attempts 1 --pull-timeout-seconds 60 --timeout-minutes 4 --diagnostic-timeout-seconds 30 --job-budget-minutes 360
```

  arm64 runner CLI는 다음 고정 argv로 보존한다.

```text
python3 scripts/run_testcontainers_image_gate.py --scope family --family-id ignite2 --platform-id arm64 --require-selection --report-dir build/reports/testcontainers-image-gate --max-attempts 2 --pull-timeout-seconds 300 --timeout-minutes 30 --diagnostic-timeout-seconds 120 --job-budget-minutes 90
```

  arm64의 최종 Gradle argv는 다음 순서와 옵션을 그대로 static fixture로 검증한다.

```text
./gradlew -Dtestcontainers.image-gate.evidence-dir=$RUN_EVIDENCE_DIR :bluetape4k-testcontainers:test --tests io.bluetape4k.testcontainers.storage.Ignite2ServerTest.representativeStartupAndWorkload --no-configuration-cache -x :bluetape4k-mock-web-server:jibDockerBuild -x :bluetape4k-mock-webflux-server:jibDockerBuild
```

  위 command 외 arbitrary task/JVM property/Gradle option은 거부한다.
- [ ] Nightly full에서 x64 gate의 expected coverage는 `52/52`, arm64 gate는
      `1/1`이고, `test-testcontainers-spring`, `coverage-report`,
      `nightly-status`는 각각 x64와 arm64 gate job의 `success`를 exact `needs`로
      요구한다. targeted scope에서는 두 image gate와 해당 bridge만 명시적으로
      `skipped`를 허용한다.
- [ ] Release verifier fixture는 x64 `coverage == 52/52`, arm64 `coverage == 1/1`,
      `release_gate == true`, expected/actual `platform_id`, tag, OS/architecture,
      digest와 workload evidence를 직접 assert한다. x64 Release gate와 arm64
      Release gate 모두 `needs.resolve-version.outputs.ref`를 checkout ref로
      사용하고 `resolve-version`와 manifest contract를 선행 의존성으로 보존한다.
      `publish.needs`는 정확히 `[resolve-version,
      testcontainers-manifest-contract, testcontainers-image-gate,
      testcontainers-ignite2-arm64-image-gate]` 네 job ID만 포함한다.
- [ ] pull secret을 job-level `DOCKER_AUTH_CONFIG`에 두지 않고 dedicated pull
      subprocess의 0600 ephemeral `DOCKER_CONFIG` 파일로만 전달한다. argv·env·
      exception·artifact에 secret이 전파되지 않고, `finally`에서 config/helper가
      삭제되는 child-process environment fixture를 통과시킨다. artifact에는
      Nightly의 고정 x64/arm64 run-platform literal 이름과 `if: always()`,
      `if-no-files-found: error`, retention 14일을 assert한다. Release는 정확히
      `release-testcontainers-image-gate-${{ github.run_id }}-amd64`와
      `release-testcontainers-image-gate-${{ github.run_id }}-arm64` 두
      literal 이름을 사용하고, 두 artifact 모두 `if: always()`,
      `if-no-files-found: error`, retention 30일을 사용한다. raw logs/system-out은
      업로드하지 않는다.
- [ ] Release verifier를 각 gate job의 마지막 required step으로 실행하고
      `publish.needs`에는 job ID만 포함한다. skipped/continue-on-error/이전 artifact
      재사용을 거부한다. ARM queue 10분 초과 시 `gh run cancel <run-id>`, release
      block, 새 run-only retry와 새 release tag/candidate rollback을 문서화한다.

## 통합 검증

### Task 7 — Parent/child exact-head integration

- [ ] parent branch에서 Python unit/static tests, manifest parity, parser/security
      tests, `git diff --check`를 통과시킨다.
- [ ] parent commit 후 child를 parent exact head 위에 rebase/merge한다. `git
      merge-base --is-ancestor parent_head child_head`를 확인하고 parent 파일을
      child가 되돌리지 않았는지, canonical manifest는 Ignite2 object hunk만
      변경됐는지 diff ownership을 검사한다.
- [ ] child에서 Kotlin compile, targeted `Ignite2ServerTest`, testcontainers
      module test, workflow policy/static tests를 실행한다. Docker-backed test는
      local Colima context와 managed socket override를 확인한 뒤 sequentially
      실행하고, skip/failure를 성공으로 해석하지 않는다.
- [ ] `actionlint`, Python unittest, `detekt`, `./gradlew :bluetape4k-testcontainers:compileTestKotlin`
      및 `git diff --check`를 fresh child head에서 실행한다.
- [ ] 구현 후 parent/child 각각 Type-D read-only code review를 수행하고, P0/P1이
      남으면 해당 branch에서 수정·재검증한다. review artifact와 exact head를 PR에
      연결한다.

### Task 8 — CI/PR/merge 이후 동기화

- [ ] parent PR을 base `develop`/head `fix/1486-image-gate-runner`로 만들고,
      Korean body 끝에 `## DoD Status`와 현재 미검증 ARM/CI WATCH를 기록한다.
- [ ] parent required CI와 review/thread/mergeability를 fresh-read한 뒤 별도
      승인 후 merge한다. child branch를 parent merge SHA 위에 restack하고 exact
      cumulative head를 다시 검증한다.
- [ ] child PR을 parent branch base로 만들고 동일한 CI/review/DoD gate를 거친다.
      Release/Nightly CI가 실제 ARM runner/pull/workload evidence를 생성할 때까지
      green static CI만으로 runtime 완료를 주장하지 않는다.
- [ ] 두 merge 후 canonical `develop`을 fetch하고 local sync, `git worktree list`,
      `git branch --merged`, `git status`를 확인한다. stale/complete worktree만
      명시적 범위로 정리하고 remote ref는 보존한다.

## Verification command set

```bash
python3 -m unittest scripts.test_testcontainers_contract \
  scripts.test_testcontainers_image_gate \
  scripts.test_run_testcontainers_image_gate -v
actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml \
  .github/workflows/release.yml
./gradlew :bluetape4k-testcontainers:compileTestKotlin \
  :bluetape4k-testcontainers:test \
  --tests 'io.bluetape4k.testcontainers.storage.Ignite2ServerTest' \
  --no-build-cache --no-configuration-cache
./gradlew :bluetape4k-testcontainers:detekt --no-configuration-cache
git diff --check
```

실제 `docker pull`, ARM runner 예약, Nightly/Release dispatch와 merge는 구현 후
각 gate의 fresh 승인·CI evidence가 충족될 때까지 `PENDING`이다. Coveralls 수치는
이 이슈의 runtime 증거가 아니며, skipped/partial job을 global coverage proof로
사용하지 않는다.

## Rollback과 위험

- Parent parser/schema 변경이 기존 52-family를 깨면 parent branch에서 strict opt-in
  경계만 되돌리고 generic family 계약을 복구한다. child runtime/workflow는 parent
  head에 의존하므로 parent가 green이 아니면 child를 merge하지 않는다.
- ARM runner unavailable, pull event/digest 누락, architecture mismatch, XML/marker
  누락, credential/staging/output budget 초과는 `blocked`로 남기고 skipped로
  완화하지 않는다.
- mutable Docker tag는 이번 범위의 명시적 WATCH다. Release 이후 immutable digest
  allowlist 또는 tag-to-digest drift monitoring은 별도 이슈로 분리한다.
- known gaps: 실제 ARM/native runtime과 checkout/setup wall-clock은 구현/첫
  Nightly에서 fresh evidence가 필요하다. 52-family 최악 8 MiB fixture는 parent
  unit/static 단계의 Plan DoD 필수 항목이며, 통과 전에는 `PENDING`으로 보고한다.

## Plan DoD

- [ ] parent/child ownership과 exact-head train receipt가 보존된다.
- [ ] 모든 RED 단계가 의도한 결함으로 실패했고 GREEN 단계에서 동일 fixture가
      통과한다.
- [ ] public API/disabled-family/central catalog/README EN/KO parity가 유지된다.
- [ ] Python runner/security/schema/workflow static checks와 Kotlin targeted tests가
      fresh head에서 통과한다.
- [ ] CI/PR에는 runtime gate가 없고 Nightly/Release native x64+arm64 gate만 있다.
- [ ] final report가 changed files, test evidence, P2/P3 WATCH, PENDING runtime
      evidence, merge/sync/cleanup 상태를 DoD 순서로 보고한다.
