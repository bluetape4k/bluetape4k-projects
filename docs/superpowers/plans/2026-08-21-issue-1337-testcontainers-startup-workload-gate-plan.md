# Testcontainers 이미지 family startup·workload gate 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Issue #1337의 변경 Docker image family에 대해 startup, 애플리케이션 readiness, 대표 workload를 동일한 manifest와 실행기로 검증하고, PR·Nightly·stable release가 `52/52` 성공 증거 없이는 통과하지 않도록 고정한다.

**Architecture:** 현재 `testing/testcontainers`의 52개 `*Server.kt`/`*Test.kt` 계약을 단일 JSON manifest로 선언한다. Python 표준 라이브러리 기반 정적 검증기는 manifest와 Kotlin source, EN/KO README, 테스트 클래스의 parity를 검증한다. 별도 실행기는 manifest 항목을 `max-parallel: 1`로 순차 실행하고, JUnit/Gradle 결과와 Docker·K3s 진단을 secret-free JSON/Markdown artifact로 남긴다. CI와 Nightly는 changed/full scope를 각각 호출하고, release workflow는 tag checkout 뒤 full scope의 성공 요약을 `publish`의 선행 조건으로 사용한다. public Kotlin API와 기존 readiness 구현은 변경하지 않는다.

**Tech Stack:** Python 3.12 표준 라이브러리, GitHub Actions, Gradle Wrapper 9.7, JDK 25, Docker CLI, 기존 JUnit 5/Testcontainers/Kover 실행 경로

---

## 사전 조건과 불변 계약

- [ ] 작업 대상은 `.worktrees/feat/1418-04-testcontainers-startup-workload-gate`의 `feat/1418-04-testcontainers-startup-workload-gate`이며 base는 `origin/develop`의 착수 SHA로 고정한다. canonical worktree와 다른 stacked worktree를 수정하지 않는다.
- [ ] 현재 52개 `IMAGE/TAG` 선언, EN/KO README 52행, 대응 `*Test.kt` 52개를 baseline으로 기록한다. 기존 `scripts/test_testcontainers_contract.py`의 성공을 보존하고, `git diff --check`를 모든 커밋 전후에 실행한다.
- [ ] 실행기는 Docker Hub 인증·mirror·retry를 환경변수/Actions secret으로만 받고 값 자체를 log/artifact에 기록하지 않는다. rate-limit은 `infrastructure_failure`로 분류하며 성공으로 재분류하지 않는다.
- [ ] 테스트containers 계열 실행은 shared Docker daemon 경합을 피하도록 하나의 gate job 내부에서 순차 실행한다. 재시도는 횟수와 원인을 artifact에 남기되 제품 실패를 숨기지 않는다.
- [ ] stable publish는 gate summary의 모든 `releaseRequired=true` 항목이 `success`이고 `coverage`가 `52/52`일 때만 진행한다. `product_failure`, 미분류, timeout, missing result, blocked는 fail-closed다.

## Task 1: 이미지 family manifest와 정적 계약 검증

**Files:**
- Create: `scripts/testcontainers_image_gate_manifest.json`
- Create: `scripts/test_testcontainers_image_gate.py`
- Modify: `scripts/test_testcontainers_contract.py`

- [ ] `testcontainers_image_gate_manifest.json`에 52개 family를 정확히 한 번씩 선언한다. 각 항목은 `id`, `server`, `source`, `image`, `tag`, `testPattern`, `readiness`, `workload`, `diagnostics`, `releaseRequired`를 갖고 source 경로와 기존 테스트 클래스명을 그대로 가리킨다.
- [ ] `readiness`는 HTTP endpoint, TCP/Curator/API, 기존 Testcontainers wait strategy 등 현재 구현의 observable contract를 사용한다. readiness를 새로 구현하거나 포트 open만으로 애플리케이션 준비를 주장하지 않는다.
- [ ] `workload`는 해당 `*Test.kt`의 최소 대표 성공 경로를 식별하고, K3s는 API readiness 뒤 pod/event/log 수집과 대표 workload를 포함한다. K3s가 없는 로컬 환경에서 임의의 성공값을 만들지 않는다.
- [ ] 정적 테스트로 schema/type/중복, 52개 수, Kotlin `IMAGE/TAG`와 manifest image/tag, EN/KO README parity, source/testPattern 존재, 빈 readiness/workload 금지, `releaseRequired` boolean, changed-only selection 키를 검증한다.
- [ ] 기존 `test_testcontainers_contract.py`에 manifest와의 source/tag count/parity assertion을 추가하되 기존 4개 contract test의 의미와 출력은 유지한다.
- [ ] 먼저 `python3 -m unittest scripts/test_testcontainers_image_gate.py scripts/test_testcontainers_contract.py -v`를 실행해 검증기가 의도한 drift를 red/green으로 확인하고, 테스트 파일 자체에 Docker 호출을 넣지 않는다.

## Task 2: 순차 실행기와 실패 진단 artifact

**Files:**
- Create: `scripts/run_testcontainers_image_gate.py`
- Create: `scripts/test_run_testcontainers_image_gate.py`
- Create: `scripts/testcontainers_image_gate_lib.py` (실행·분류·redaction이 재사용될 때만)

- [ ] CLI를 `--manifest`, `--scope {changed,full}`, `--report-dir`, `--gradle-task`, `--max-attempts`, `--timeout-minutes`로 고정하고 기본 task는 `:bluetape4k-testcontainers:test`로 둔다. 변경 scope는 base/head diff의 실제 `testing/testcontainers` 및 manifest 관련 변경 family만 선택하고, release는 항상 `full`을 사용한다.
- [ ] family를 manifest 순서대로 하나씩 실행한다. 각 호출은 `--tests <testPattern>`을 사용하며 exit code, elapsed time, attempts, JUnit result 경로, image/tag, commit SHA를 기록한다. 프로세스 환경과 command line에서 secret-looking 값은 `<redacted>`로 치환한다.
- [ ] 성공은 startup/readiness/workload와 JUnit 결과가 모두 확인된 경우에만 `success`로 기록한다. non-zero Gradle/JUnit assertion은 `product_failure`, Docker pull/auth/daemon/timeout/rate-limit은 `infrastructure_failure`, prerequisite 누락·manifest 오류·결과 누락은 `blocked`로 기록한다.
- [ ] 실패 시 `docker inspect`, container logs, bounded `docker events`를 수집하고, K3s family는 bounded `kubectl get pods`, `describe`, events, logs를 수집한다. 진단 실패가 원래 제품 실패를 덮어쓰지 않으며 artifact 크기와 시간 제한을 둔다.
- [ ] `summary.json`, `summary.md`, family별 JSON, raw Gradle/JUnit 경로 목록을 생성한다. summary는 `selected`, `success`, `product_failure`, `infrastructure_failure`, `blocked`, `coverage`, `release_gate`를 포함하고, 항목이 하나라도 누락되면 `release_gate=false`다.
- [ ] subprocess를 fake runner로 주입해 success/product/infrastructure/timeout/secret-redaction/diagnostic-failure을 단위 테스트한다. 실제 Docker daemon은 이 테스트에서 요구하지 않는다.
- [ ] 실행기는 `max-parallel=1`을 내부 불변값으로 유지하고, `--max-attempts`를 무제한으로 허용하지 않는다. 재시도 후에도 원인이 달라지지 않으면 원래 분류와 모든 시도 결과를 보존한다.

## Task 3: Pull Request CI gate 연결

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `scripts/test_release_workflow_policy.py` (release job set 검증은 Task 5와 함께 갱신)

- [ ] `testing/testcontainers/**`, manifest/runner/관련 workflow 변경을 감지하는 filter를 추가하고, 변경 family를 계산해 `run_testcontainers_image_gate.py --scope changed`에 전달한다.
- [ ] `testcontainers-image-gate` job은 JDK 25, Python 3.12, Gradle wrapper를 설정하고 registry auth/mirror는 secret/vars에서만 주입한다. 하나의 job에서 gate를 순차 실행하고 `if: always()` artifact upload를 보장한다.
- [ ] summary가 `releaseRequired` family 전부 성공했을 때만 job을 성공시킨다. 변경 범위가 없을 때는 `skipped`를 명시하고 stable release의 full gate와 혼동하지 않는다.
- [ ] 기존 `testcontainers-spring`, release-policy, required check의 dependency를 점검해 image gate 실패가 green CI로 우회되지 않게 한다. 기존 일반 Testcontainers 테스트와 중복되는 Docker 실행은 제외 플래그로 명시한다.
- [ ] PR 경로에서 `python3 -m unittest scripts/test_testcontainers_image_gate.py scripts/test_run_testcontainers_image_gate.py scripts/test_testcontainers_contract.py scripts/test_release_workflow_policy.py -v`와 `actionlint .github/workflows/ci.yml`을 실행한다.

## Task 4: Nightly full gate와 진단 보존

**Files:**
- Modify: `.github/workflows/nightly-tests.yml`

- [ ] 기존 broad Testcontainers matrix는 유지하되, 새 `test-testcontainers-image-gate` job을 별도 `max-parallel: 1` lane으로 추가해 동일 runner의 `--scope full`을 호출한다. 기존 matrix의 8-way 병렬이 새 gate를 병렬화하지 않도록 dependency와 job 이름을 분리한다.
- [ ] mock-web-server/mock-webflux-server image build와 K3s prerequisite를 gate가 재사용하되, 실패 시 어떤 prerequisite가 막혔는지 summary에 남긴다. ZooKeeper readiness는 현재 `ZookeeperServerSupport.kt`의 Curator session contract를 그대로 사용한다.
- [ ] Nightly artifact에는 summary, family 진단, JUnit XML, Kover 결과를 retention 14일 이상으로 업로드하고, `52/52`와 분류별 count를 job summary에 출력한다.
- [ ] `test-testcontainers-spring`와 downstream nightly 집계가 image gate 결과를 읽도록 연결하되, 기존 nightly 전용 테스트 범위와 coverage 계산을 임의로 확장하지 않는다.
- [ ] YAML syntax/actionlint와 fake runner unit test를 먼저 실행한 뒤, Docker-backed full gate는 하나의 sequential lane에서만 실행한다. rate-limit 또는 daemon 장애가 발생하면 artifact를 남기고 성공으로 강제하지 않는다.

## Task 5: Stable release fail-closed gate

**Files:**
- Modify: `.github/workflows/release.yml`
- Modify: `scripts/test_release_workflow_policy.py`
- Modify: `scripts/test_testcontainers_image_gate.py`

- [ ] `resolve-version` 뒤 `testcontainers-image-gate` job을 추가한다. tag ref를 checkout하고 JDK 25/Python 3.12/Gradle을 설정한 뒤 동일 runner로 `--scope full`을 호출한다. `summary.json`에서 `release_gate=true`, `coverage=52/52`, 모든 `releaseRequired=true` 성공을 재검증한다.
- [ ] `publish.needs`에 `testcontainers-image-gate`를 포함시켜 gate가 실패·누락·취소되면 Maven Central publish가 시작되지 않도록 한다. publish job의 기존 Maven-only 권한과 release machinery 금지 계약은 보존한다.
- [ ] release artifact에는 tag, exact commit SHA, manifest digest, runner version, per-family result, failure classification, rollback identity를 기록한다. signing secret과 registry credential은 진단에 포함하지 않는다.
- [ ] `scripts/test_release_workflow_policy.py`의 release job set을 `resolve-version`, `testcontainers-image-gate`, `publish`로 갱신하고 개발 버전 배포 workflow는 여전히 `publish` 하나만 허용한다. 정확한 Maven release task가 한 번만 호출되는 검증을 보존한다.
- [ ] semantic negative test로 gate dependency 제거, `coverage != 52/52`, 개발 버전 배포 task 삽입, GitHub Release machinery, `contents: write`를 각각 거부하는지 검증한다.
- [ ] 정상 rollback은 이전에 검증된 bounded manifest/tag로 gate를 다시 통과한 뒤 publish를 재시도하는 경로로 한정한다. rate-limit 우회를 위한 무제한 retry나 gate skip을 정상 rollback으로 문서화하지 않는다.

## Task 6: 운영 문서와 README 계약

**Files:**
- Create: `docs/operations/issue-1337-testcontainers-image-gate.md`
- Modify: `testing/testcontainers/README.md`
- Modify: `testing/testcontainers/README.ko.md`
- Create: `docs/lessons/2026-08-21-issue-1337-testcontainers-startup-workload-gate.md`

- [ ] 운영 문서에 local/PR/Nightly/release 명령, manifest 갱신 절차, auth/mirror/retry 설정, artifact schema, 분류별 triage, rate-limit 대응, K3s timeout·pod/event/log 확인, rollback/forward-fix를 한국어로 기록한다.
- [ ] EN/KO README에 “52개 image family gate”의 목적, startup/readiness/workload 의미, stable publish prerequisite, 로컬 실행 예를 추가하고 현재 52행 default-tag 표와 모순되지 않게 한다. copyable command marker와 EN/KO marker parity를 고정한다.
- [ ] lesson에는 원인-결정-검증-잔여 위험을 기록하고 실제 실행 결과가 없는 항목은 `PENDING`으로 명시한다. benchmark나 chart를 만들지 않으며 이번 slot의 증거 범위를 image gate로 한정한다.
- [ ] 문서 산출물에 대해 SPW-01 source/evidence lock, SPW-02 contract completeness, SPW-03 Korean technical register/naturalness, SPW-04 traceability, SPW-05 final Markdown read-back을 각각 기록한다. `audit-korean-terms.mjs`, placeholder scan, `git diff --check`가 모두 통과해야 한다.

## Task 7: 검증 순서와 DoD 증거 수집

- [ ] 정적/단위 검증: `python3 -m unittest scripts/testcontainers_image_gate.py scripts/test_run_testcontainers_image_gate.py scripts/test_testcontainers_contract.py scripts/test_release_workflow_policy.py -v`, `python3 -m py_compile scripts/run_testcontainers_image_gate.py`, `git diff --check`.
- [ ] workflow 검증: `actionlint .github/workflows/ci.yml .github/workflows/nightly-tests.yml .github/workflows/release.yml`, release policy 재실행, 변경된 workflow의 job dependency와 artifact path read-back.
- [ ] Gradle 검증: `./gradlew projects --no-daemon --no-configuration-cache`, `./gradlew :bluetape4k-testcontainers:compileKotlin :bluetape4k-testcontainers:compileTestKotlin --no-daemon --no-configuration-cache`, 이후 `./gradlew :bluetape4k-testcontainers:test --rerun-tasks --no-configuration-cache`를 순차 실행한다.
- [ ] Docker-backed 검증은 CI/Nightly의 하나의 sequential lane에서 실행하고, 변경 범위와 full scope를 각각 `52/52` 선택/실행/결과 count로 read-back한다. Docker daemon, registry, K3s 장애는 통과가 아닌 `infrastructure_failure` evidence로 보고한다.
- [ ] 실패 시 `git diff --check`, static tests, targeted Gradle compile을 유지한 채 원인별 최소 수정 후 해당 단계부터 재검증한다. 전체 테스트가 장시간/환경 장애로 실행되지 않으면 정확한 command, elapsed time, 로그 경로, 미검증 범위를 DoD에 남긴다.
- [ ] 최종 DoD는 public API 변경 없음, manifest/source/README/test parity, sequential gate, secret-free diagnostics, PR/Nightly/release dependency, `52/52`, P/I/blocked 0, stable publish fail-closed, docs EN/KO parity를 각각 증거와 함께 보고한다.

## Task 8: stacked PR 생성과 다음 승인 경계

- [ ] 모든 구현·문서·workflow 변경을 이 branch에 하나의 reviewable child commit train으로 정리하고 commit마다 Lore 형식(`Constraint`, `Rejected`, `Confidence`, `Scope-risk`, `Directive`, `Tested`, `Not-tested`)을 사용한다. plan/spec 커밋은 이미 `a7e0ac7f6`로 보존한다.
- [ ] child PR은 base `develop`, head `feat/1418-04-testcontainers-startup-workload-gate`, linked Issue `#1337`, Epic `#1418`로 생성한다. 본문과 `## DoD Status`는 한국어로 작성하고 exact base/head, checks, artifacts, `52/52` 결과, one-person review N/A 사유, 남은 위험을 명시한다.
- [ ] PR 생성 후 live read-back으로 exact head/base, mergeability, checks/reviews/comments, changed files, linked issue, labels/milestone을 확인한다. CI가 green이어도 path-filtered/skipped 또는 infrastructure failure가 있으면 merge-ready로 표시하지 않는다.
- [ ] PR이 열려 있는 동안 Epic #1418은 `3/4`로 유지하고 #1337 PR 링크와 `PENDING` gate를 기록한다. merge 승인 전에는 branch 삭제, worktree cleanup, Epic close, default-branch sync를 수행하지 않는다.
- [ ] fresh exact-head CI/review/DoD 증거가 모두 확보된 뒤에만 사용자에게 merge 승인 경계를 보고한다. 이번 계획의 종료 상태는 구현 완료가 아니라 계획 승인 대기(`PENDING`)다.

## Rollback Plan

- [ ] 코드/문서 변경이 문제를 일으키면 해당 child commit을 되돌리고 기존 `test_testcontainers_contract.py`, 기존 Nightly matrix, 기존 release workflow를 복원한다. gate skip을 추가해 green으로 위장하지 않는다.
- [ ] workflow만 실패하면 이전 workflow commit으로 bounded revert한 뒤, 정적 contract와 release policy를 먼저 통과시키고 재작업한다. publish가 이미 시작된 경우에는 신규 publish를 중단하고 release owner가 tag/artifact identity를 확인한다.
- [ ] registry/K3s/Docker 장애는 retry 횟수 증가 대신 진단 artifact와 owner/time-bound를 남긴다. 정상 경로는 bounded manifest를 수정·검증하고 full gate를 다시 통과하는 것이며, break-glass는 별도 명시적 승인 없이는 사용하지 않는다.

## 완료 기준

- [ ] Issue #1337 요구사항 4개와 설계 acceptance criteria 8개가 코드·workflow·문서·테스트 증거로 연결된다.
- [ ] changed scope와 full scope 모두 선택/실행/분류 결과가 재현 가능하고, stable release는 `52/52` 성공이 아니면 publish하지 않는다.
- [ ] fresh CI/targeted tests/full module tests/actionlint/read-back이 확보되고, unchecked item·known failure·blocked gate가 DoD에 숨김없이 남는다.
