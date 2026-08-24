# #1486 Ignite2 ARM64 이미지와 실행 증거 게이트 설계

- **작성일**: 2026-08-24
- **저장소**: `bluetape4k/bluetape4k-projects`
- **이슈**: [#1486](https://github.com/bluetape4k/bluetape4k-projects/issues/1486)
- **기준 커밋**: `02b86211f2be6a0bf03a28d7c9723d822d09b56f`
- **작업 방식**: `fix/1486-image-gate-contract` → `fix/1486-ignite2-arm64-runtime` stacked PR train

## 1. 결정 요약

`Ignite2ServerTest`의 클래스 수준 `@Disabled`를 제거하고, Apache Ignite 2
thin client로 cache put/get을 수행하는 실제 workload를 추가한다. 테스트 전용
의존성은 중앙 `bt4k` catalog의 `ignite-core` alias를 사용하므로 public API나
의존성 버전 원본을 새로 만들지 않는다.

image gate 실행기는 Gradle 종료 코드와 `BUILD SUCCESSFUL` 문자열만으로 성공을
판정하지 않는다. 선택한 test pattern의 JUnit XML을 읽어 실행된 test가 하나
이상이고 전체가 skipped가 아님을 확인한다. XML이 없거나 `tests == skipped`이면
`blocked`로 기록하고 release gate를 닫는다.

Ignite2 항목에는 `amd64`/`arm64` platform metadata를 추가한다. 기존 전체
family gate는 기본 amd64 태그를 계속 검증하고, Nightly full과 Release에는
Ignite2 전용 native matrix를 추가한다.

| platform | runner | image tag | Docker architecture |
|---|---|---|---|
| `amd64` | `ubuntu-24.04` | `2.18.0` | `linux/amd64` |
| `arm64` | `ubuntu-24.04-arm` | `2.18.0-arm64` | `linux/arm64` |

각 실행은 runner architecture, Docker architecture, image digest, startup
readiness, workload, JUnit 실행 집계를 하나의 결과 artifact로 남긴다. PR CI에는
Docker runtime gate를 추가하지 않고 기존 정적 계약만 유지한다.

## 2. 현재 문제와 근거

- `Ignite2Server.DEFAULT_TAG`는 현재 `os.arch == "aarch64"`에서만
  `2.18.0-arm64`를 선택한다.
- `Ignite2ServerTest` 전체에 `@Disabled`가 붙어 있어 기준선 XML이
  `tests="3" skipped="3"`이다. 기준선 명령은 `BUILD SUCCESSFUL`이지만 실제
  test execution은 0건이었다.
- `scripts/run_testcontainers_image_gate.py`는 return code와 Gradle 성공 문자열만
  확인하며 JUnit XML, image digest, host architecture를 저장하지 않는다.
- `scripts/testcontainers_image_gate_manifest.json`의 Ignite2 항목은 단일
  `tag`만 표현하고 platform별 tag/runner 계약이 없다.
- EN/KO README에는 `2.18.0`과 `-arm64` 설명이 있지만 Linux platform과
  실행 증거의 연결이 명시되지 않았다.
- PR image gate는 #1484에서 30분 이상 걸리는 runtime job을 제거한 상태다.
  이 속도 경계는 유지해야 한다.

## 3. 범위와 제외

### 포함

1. Ignite2 test를 unskip하고 startup·thin-client cache workload를 실행한다.
2. `ignite-core` test dependency를 중앙 catalog alias로 연결한다.
3. manifest에 platform tag, architecture, runner metadata와 선택 API를 추가한다.
4. image gate가 JUnit XML 실행 수와 Docker image/architecture 증거를 fail-closed로
   판정하고 JSON/Markdown에 저장한다.
5. Nightly full과 Release에 native amd64/arm64 Ignite2 gate를 추가하고 publish와
   Spring bridge의 dependency를 갱신한다.
6. EN/KO README, Ignite2 KDoc, source/README/manifest static contract를 함께
   갱신한다.

### 제외

- `Ignite2Server` public constructor/operator 함수 시그니처 변경
- 기존 전체 52개 family를 ARM64에서 다시 실행하는 범위 확대
- PR workflow의 Docker pull/startup/workload runtime 실행
- x64 runner의 QEMU를 native ARM64 증거로 인정하는 우회
- 새 public abstraction, 새 registry dependency, 수동 digest pin 변경

## 4. 대안 비교

### A. x64 + QEMU로 ARM64 이미지를 실행 — 거부

실행 비용은 낮지만 실제 ARM64 kernel/runner에서의 startup과 workload를 증명하지
못한다. Issue #1486의 `aarch64` runtime 계약과 맞지 않는다.

### B. 전체 x64 gate + Ignite2 native platform matrix — 선택

기존 52-family amd64 gate는 유지하고, Ignite2만 두 native runner에서 별도
실행한다. ARM64 검증 비용을 문제 family로 제한하면서 두 tag의 실제 pull,
startup, workload 증거를 명확히 남긴다. PR CI 속도에는 영향을 주지 않는다.

### C. 52개 family 전체를 두 아키텍처에서 실행 — 거부

가장 넓은 증거를 만들지만 이번 이슈의 범위를 넘어 이미지 pull과 CI 시간을
두 배로 늘린다. 다른 family의 platform 계약은 별도 이슈에서 다룬다.

## 5. 구성 요소와 데이터 흐름

```text
Ignite2Server.kt + README EN/KO + manifest platform metadata
                         │ static contract
                         ▼
             native platform gate matrix
                         │ --family-id ignite2 --platform-id {amd64|arm64}
                         ▼
      Gradle test → JUnit XML → image/runner inspect → evidence result
                         │
                         ├─ tests=0 또는 all skipped → blocked
                         ├─ image/architecture 불일치 → blocked
                         ├─ startup/workload assertion 실패 → product_failure
                         └─ pull/daemon/timeout 실패 → infrastructure_failure
                         ▼
                 summary.json + summary.md + per-family JSON
                         │
             Nightly artifact / Release publish prerequisite
```

### 5.1 Manifest 계약

기존 Ignite2 entry의 기본 `image`/`tag`는 amd64 기준으로 유지하고 다음 필드를
추가한다.

```json
"platforms": [
  {
    "id": "amd64",
    "os": "linux",
    "architecture": "amd64",
    "tag": "2.18.0",
    "runner": "ubuntu-24.04"
  },
  {
    "id": "arm64",
    "os": "linux",
    "architecture": "arm64",
    "tag": "2.18.0-arm64",
    "runner": "ubuntu-24.04-arm"
  }
]
```

manifest validator는 platform id 중복, 빈 tag/runner, 지원하지 않는
architecture, EN/KO README의 두 tag 누락, `Ignite2Server.DEFAULT_TAG`의 source
계약을 모두 검사한다. 다른 family에는 기존 schema를 적용하며, platform matrix
선택은 `--family-id`와 `--platform-id` 조합으로만 허용한다.

### 5.2 Runner 결과 계약

`GateRunner`는 다음 순서로 한 family를 실행한다.

1. 선택된 entry의 platform override를 적용해 image tag와 test JVM property를
   결정한다.
2. 기존 Gradle `--tests` 명령을 실행한다. 실행 task의
   `testing/testcontainers/build/test-results/<task>/`에서 test pattern과
   일치하는 suite를 찾는다.
3. JUnit XML을 합산한다. `tests > 0`, `tests > skipped`, `failures == 0`,
   `errors == 0`이어야 실행 증거가 유효하다. XML이 없거나 pattern suite가
   없으면 `blocked`다.
4. `docker image inspect <image>:<tag>`, `docker info --format
   '{{.Architecture}}'`, runner `uname -m`을 수집한다. platform이 지정된
   경우 기대 architecture와 실제 Docker architecture가 다르면 `blocked`다.
5. test stdout/stderr, JUnit system-out, image inspect 결과, digest, startup과
   workload 집계를 per-family JSON과 summary에 기록한다.
6. 실패하면 기존 `docker_ps`, `docker_inspect`, `docker_events`, logs 진단을
   수집한다. secret redaction은 기존 규칙을 유지한다.

`0 tests`와 `all skipped`는 제품 실패가 아니라 실행 증거 부재이므로
`blocked`로 분류한다. `release_gate`는 선택 family가 모두 `success`이고
`releaseRequired`인 경우에만 true다.

### 5.3 Ignite2 runtime test

`Ignite2ServerTest`는 클래스 수준 `@Disabled`를 제거한다.

- 대표 test: `Ignite2Server`를 시작하고 `Ignition.startClient`로
  `host:port`에 연결한다. 중앙 `ignite-core`의 `ClientConfiguration`으로
  test cache를 생성해 `put`/`get`하고 값을 검증한 뒤 client를 닫는다.
- 기본 port test: `useDefaultPort = true`로 startup과 `PORT == 10800`을
  검증한다.
- 입력 검증 test: blank image/tag가 `IllegalArgumentException`을 발생시키는
  기존 계약을 유지한다.
- platform job은 runner가 선택한 tag를 test-only system property로 전달하고,
  test는 그 tag를 `Ignite2Server(tag = ...)`에 사용한다. public API는 변하지
  않는다.

container test는 `AbstractContainerTest`의 `SAME_THREAD` 실행 계약을 유지하고,
모든 client/container 자원을 `use`로 닫는다.

### 5.4 Workflow 경계

- **PR CI**: `test_testcontainers_contract.py`, manifest contract, Python runner
  unit/static tests만 실행한다. `run_testcontainers_image_gate.py` runtime 호출은
  계속 금지한다.
- **Nightly full**: 기존 52-family x64 gate와 별도로 Ignite2 platform matrix를
  실행한다. matrix는 `fail-fast: false`, `max-parallel: 1`로 두고 두 결과가 모두
  성공해야 Spring bridge가 시작된다. `testcontainers` 범위에서는 두 job 모두
  skipped로 남고 기존 조건대로 bridge를 실행한다.
- **Release**: manifest contract → 기존 full x64 gate → Ignite2 platform matrix
  순서로 실행한다. publish는 세 gate의 성공을 모두 `needs`로 요구하며
  `coverage`/platform evidence 없이 진행하지 않는다.
- **ARM runner**: GitHub public ARM64 label은 실행 시 실제 예약/architecture로
  검증한다. runner unavailable은 skipped나 N/A로 완화하지 않는다.

## 6. 실패 모드와 복구

| 실패 | 분류 | 처리 |
|---|---|---|
| XML 없음, pattern suite 없음, `tests=0`, all skipped | `blocked` | release gate 차단, 결과와 경로 기록 |
| 이미지 pull/daemon/rate limit/timeout | `infrastructure_failure` | bounded retry 후 Docker 진단, gate 실패 |
| container startup/readiness/workload assertion 실패 | `product_failure` | logs/inspect/events와 JUnit 결과 저장 |
| 기대 platform과 Docker architecture 불일치 | `blocked` | tag/runner 계약 재검토, 성공 완화 금지 |
| ARM runner label 예약 실패 | workflow failure | matrix 결과를 skipped로 대체하지 않음 |
| manifest/source/README drift | static contract failure | Docker 실행 전에 수정하도록 차단 |

롤백은 새 commit에서 platform job 또는 parser를 되돌리되, 기존 PR CI의 runtime
제외와 stable release의 full gate dependency는 유지한다. 이전 실행 artifact를
새 성공으로 재사용하지 않는다.

## 7. stacked PR train과 변경 파일

### Parent: `fix/1486-image-gate-contract` → `develop`

- `scripts/run_testcontainers_image_gate.py`
- `scripts/testcontainers_image_gate.py`
- `scripts/testcontainers_image_gate_manifest.json`의 schema/validation
- `scripts/test_run_testcontainers_image_gate.py`
- `scripts/test_testcontainers_image_gate.py`

Parent는 runtime gate가 실제 test execution과 image/architecture 증거를
검증하는 기반만 제공한다. Ignite2 source/README/workflow는 child에 둔다.

### Child: `fix/1486-ignite2-arm64-runtime` → parent

- `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/Ignite2Server.kt`
- `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/storage/Ignite2ServerTest.kt`
- `testing/testcontainers/build.gradle.kts`
- `testing/testcontainers/README.md`
- `testing/testcontainers/README.ko.md`
- `.github/workflows/nightly-tests.yml`
- `.github/workflows/release.yml`
- `scripts/test_testcontainers_contract.py`
- `docs/superpowers/specs/2026-08-24-issue-1486-ignite2-arm64-image-gate-design.md`

두 PR 모두 base/head와 exact cumulative SHA를 live read-back하고, child PR은
parent merge 전에는 독립 merge하지 않는다.

## 8. 검증 계획과 수용 기준

- [ ] 기준선 `Ignite2ServerTest`의 `3/3 skipped`가 RED 증거로 기록된다.
- [ ] runner unit test가 성공, XML 없음, `tests=0`, all skipped, pattern mismatch,
      architecture mismatch를 각각 검증한다.
- [ ] unskipped Ignite2 test가 startup과 thin-client cache put/get을 통과한다.
- [ ] manifest가 `amd64`/`arm64` tag·runner·architecture를 검증한다.
- [ ] EN/KO README와 KDoc이 `2.18.0`/`2.18.0-arm64` 및 platform mapping과
      일치한다.
- [ ] PR workflow에는 image runtime 호출이 없고, Nightly/Release만 native matrix를
      실행한다.
- [ ] `actionlint`, Python unit/static tests, targeted Gradle test, Kotlin compile,
      `git diff --check`가 통과한다.
- [ ] native amd64와 arm64 CI artifact에 image digest, Docker/runner architecture,
      startup/workload, JUnit execution count가 모두 존재한다.
- [ ] Release publish가 두 platform gate와 기존 52/52 full gate 없이는 진행되지
      않는다.

### 8.1 Writer DoD

- **SPW-01 PASS**: 독자는 bluetape4k contributor이며, Issue #1486, 기준 커밋,
  현재 source/workflow/manifest와 공식 GitHub·Ignite 문서를 근거로 고정했다.
- **SPW-02 PASS**: 대안, 범위, manifest/runner/runtime/workflow 계약, 실패 모드,
  호환성, stacked train, 수용 기준과 rollback을 포함했다.
- **SPW-03 PASS**: 한국어 기술 문체와 고정 용어를 적용하고 code token, command,
  URL, 숫자, status token을 보존했다. `korean-naturalness-checklist.md`를
  기준으로 문장과 표를 읽었다.
- **SPW-04 PASS**: 현재 worktree의 `Ignite2Server`, test XML, runner,
  manifest, workflows와 Issue #1486 acceptance를 대조했다. ARM runner와
  실제 digest는 구현 후 CI에서 확인해야 하는 미확인 항목으로 남겼다.
- **SPW-05 PASS**: 최종 Markdown을 다시 읽고 headings, tables, code fence,
  checklist, link를 확인했다. `git diff --check`와
  `audit-korean-terms.mjs`가 통과했다.

## 10. 공개 API·성능·안정성 검토

- public constructor/operator와 `Ignite2Server` property 계약은 유지한다.
- test-only `ignite-core`는 중앙 catalog의 기존 `ignite` version을 사용하며
  publish artifact에는 포함하지 않는다.
- PR CI runtime 비용은 증가하지 않는다. Nightly/Release는 Ignite2 두 번의
  native 실행 비용만 추가한다.
- cache/client/container는 모두 명시적으로 닫고, test runner는 family를
  순차 실행해 Docker pull 압력과 공유 daemon 경쟁을 제한한다.
- image inspect와 JUnit XML이 없으면 성공하지 않으므로 green-but-unverified
  결과를 방지한다.

## 11. 근거 원본

- Issue #1486 및 merged PR #1488, #1484
- `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/Ignite2Server.kt`
- `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/storage/Ignite2ServerTest.kt`
- `scripts/run_testcontainers_image_gate.py`
- `scripts/testcontainers_image_gate.py`
- `scripts/testcontainers_image_gate_manifest.json`
- `.github/workflows/ci.yml`, `.github/workflows/nightly-tests.yml`,
  `.github/workflows/release.yml`
- [Apache Ignite 2 Java Thin Client](https://ignite.apache.org/docs/ignite2/latest/thin-clients/java-thin-client)
- [GitHub-hosted runners](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)
