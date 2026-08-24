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
판정하지 않는다. 다만 기존 52-family gate에는 의도적인 class-level `@Disabled`
family가 있으므로 strict 실행 증거는 manifest의 `executionEvidenceRequired: true`
항목에만 적용한다. Ignite2는 선택한 workload testcase의 JUnit XML과 실제 image
pull·architecture·startup·workload 증거를 모두 검증하고, 증거가 없거나 전체가
skipped이면 `blocked`로 기록해 release gate를 닫는다.

Ignite2 항목에는 `amd64`/`arm64` platform metadata를 추가한다. 기존 52-family
full x64 gate가 amd64의 단일 authoritative 실행과 증거를 제공하고, Nightly
full과 Release에는 Ignite2 arm64 전용 native gate를 추가한다. 일반 Nightly
`storage-cache` matrix에서는 Ignite2를 제거해 같은 workload를 반복하지 않는다.

| platform | runner | image tag | Docker architecture |
|---|---|---|---|
| `amd64` | `ubuntu-24.04` | `2.18.0` | `linux/amd64` |
| `arm64` | `ubuntu-24.04-arm` | `2.18.0-arm64` | `linux/arm64` |

각 authoritative 실행은 runner label/architecture, Docker daemon과 image의
OS/architecture, 선택 tag의 pull 결과·digest, startup readiness, workload
testcase, JUnit 실행 집계를 schema v2 결과 artifact로 남긴다. PR CI에는 Docker
runtime gate를 추가하지 않고 기존 정적 계약만 유지한다. manifest의 `runner`는
설명과 정적 검증에만 사용하고 workflow의 `runs-on`을 결정하지 않는다.

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
- 기존 full gate와 Nightly `storage-cache` matrix가 같은 Ignite2 test를 중복
  실행할 경로가 있다.
- 현재 52-family 중 ChromaDB, Ollama, Redpanda, Ignite3 등은 의도적으로
  disabled 상태이므로 모든 family에 strict `tests > skipped`를 적용할 수 없다.
- EN/KO README에는 `2.18.0`과 `-arm64` 설명이 있지만 Linux platform과
  실행 증거의 연결이 명시되지 않았다.
- PR image gate는 #1484에서 30분 이상 걸리는 runtime job을 제거한 상태다.
  이 속도 경계는 유지해야 한다.

## 3. 범위와 제외

### 포함

1. Ignite2 test를 unskip하고 startup·thin-client cache workload를 실행한다.
2. `ignite-core` test dependency를 중앙 catalog alias로 연결한다.
3. manifest에 platform tag, architecture, runner metadata와 선택 API를 추가한다.
4. `executionEvidenceRequired` family에 한해 JUnit workload 실행 수와 Docker
   pull/image/architecture 증거를 fail-closed로 판정하고 schema v2 JSON/Markdown에
   저장한다.
5. 기존 full x64 gate를 amd64 권위 실행으로 유지하고 Nightly full과 Release에
   arm64 전용 Ignite2 gate를 추가하며 publish와 Spring bridge의 dependency를
   갱신한다.
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

### B. 전체 x64 gate + Ignite2 arm64 전용 gate — 선택

기존 52-family full x64 gate가 `2.18.0`의 amd64 pull, startup, workload와
strict evidence를 담당한다. Ignite2만 `ubuntu-24.04-arm`에서
`2.18.0-arm64`를 별도 실행해 ARM64 증거를 추가한다. 일반 Nightly matrix에서
Ignite2를 제거하므로 동일 workload와 pull을 반복하지 않으며, PR CI 속도에는
영향을 주지 않는다.

### C. 52개 family 전체를 두 아키텍처에서 실행 — 거부

가장 넓은 증거를 만들지만 이번 이슈의 범위를 넘어 이미지 pull과 CI 시간을
두 배로 늘린다. 다른 family의 platform 계약은 별도 이슈에서 다룬다.

## 5. 구성 요소와 데이터 흐름

```text
Ignite2Server.kt + README EN/KO + manifest platform metadata
                         │ static contract
                         ▼
             full x64 gate (amd64) + arm64 gate
                         │ --family-id ignite2 --platform-id arm64
                         ▼
      pull → Gradle test → JUnit XML → image/runner inspect → evidence result
                         │
                         ├─ tests=0 또는 all skipped → blocked
                         ├─ image/architecture 불일치 → blocked
                         ├─ pull/digest/workload evidence 부재 → blocked
                         ├─ startup/workload assertion 실패 → product_failure
                         └─ pull/daemon/timeout 실패 → infrastructure_failure
                         ▼
                 summary.json + summary.md + per-family JSON
                         │
             Nightly artifact / Release publish prerequisite
```

### 5.1 Manifest 계약

기존 Ignite2 entry의 기본 `image`/`tag`는 amd64 기준으로 유지하고 다음 필드를
추가한다. `runner`는 workflow 선택값이 아니라 문서·정적 계약의 설명값이다.

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
],
"defaultPlatformId": "amd64",
"executionEvidenceRequired": true,
"workloadTestPattern": "io.bluetape4k.testcontainers.storage.Ignite2ServerTest.representativeStartupAndWorkload",
"platformTimeouts": {
  "amd64": {"testMinutes": 6, "clientConnectSeconds": 30, "clientRequestSeconds": 30},
  "arm64": {"testMinutes": 30, "clientConnectSeconds": 30, "clientRequestSeconds": 30}
},
"pullEvidenceRequired": true
```

manifest validator는 platform id 중복, 빈 tag/runner, 지원하지 않는
architecture, 허용 runner 외의 값, EN/KO README의 두 tag 누락,
`Ignite2Server.DEFAULT_TAG`의 source 계약, workload pattern과
`executionEvidenceRequired`의 일관성을 모두 검사한다. image reference는
허용된 lowercase registry/repository grammar, tag는 control character·leading
dash 없는 bounded grammar, workload pattern은 `classname.method` grammar만
허용한다. 다른 family에는 기존
schema와 disabled 회귀 계약을 적용한다. 기존 `--scope full` x64 실행에서
strict Ignite2 resolver는 `platform_id=amd64`, `tag=platforms[amd64].tag`,
`architecture=amd64`, `runner=ubuntu-24.04`를 결과·command·artifact에
명시하고 top-level `tag`와 `platforms[amd64].tag`가 일치해야 한다.
`defaultPlatformId=amd64`가 없는 기존 entry는 full x64 경로에서만 이 기본값을
사용하고, strict entry의 platform별 `testMinutes`를 generic family 기본 timeout
보다 우선한다. 따라서 x64 full의 Ignite2는 6분(5분 container startup + 1분
Gradle/workload 여유), arm64 전용 gate는 30분이며
두 경로 모두 client connect/request timeout은 30초다.
platform 선택은 exact
`--family-id ignite2 --platform-id arm64` 조합만 허용하고, workflow `runs-on`은
정적으로 `ubuntu-24.04` 또는 `ubuntu-24.04-arm`만 사용한다.

### 5.2 Runner 결과 계약

`GateRunner`는 다음 순서로 한 family를 실행한다. `executionEvidenceRequired`가
없는 기존 family는 기존 성공 판정을 유지하고, Ignite2에만 아래 strict 단계를
적용한다. manifest에 이 필드를 지정하지 않은 기존 family는 `false`로
해석하며, validator regression test가 이 기본값과 `Ignite2=true` opt-in을
고정한다.

1. selector가 정확히 하나의 family/platform을 반환하는지 확인한다. 0개·복수개·
   unknown ID는 non-zero와 `blocked`로 처리하고, 결과·command·diagnostic에
   `platform_id`와 선택 image ref를 전달한다.
2. `pullEvidenceRequired` entry는 선택된 `<image>:<tag>`를 pull하고 pull
   결과/event와 cache 여부를 기록한다. pull 전 bounded event observer를 시작해
   `run_id`, `family_id`, `platform_id`, `attempt`, requested ref로 correlation하고,
   observer가 확인한 pull event id를 schema에 남긴다. authoritative 실행에서
   pull은 첫 attempt에 최대 1회만 수행하고, pull 단계가 registry transport
   failure였을 때만 두 번째 attempt에서 1회 재시도한다(실행당 최대 2회).
   pull 성공 후에는 verified image ID/digest를 다음 test retry에서 재사용하고
   tag를 다시 해석하지 않는다. pull 직후 image ID와 `RepoDigests`를 고정하고,
   container inspect의 image ID/digest가 그 값과 일치하는지 확인한다.
   `RepoDigests`가 비어 있거나 event correlation이 없으면 성공하지 않는다. registry
   credential은 dedicated pull subprocess의 ephemeral `DOCKER_CONFIG`에만
   주입한다. Gradle/test/diagnostic subprocess에는 sanitized environment만
   전달하고, pull 직후 temporary config와 helper credential을 삭제한다.
   `DOCKER_AUTH_CONFIG` raw/decoded/base64 값, basic-auth URL과 known secret을
   redaction registry에 등록해 print·exception·JSON·Markdown·diagnostic 전에
   동일 redactor를 통과시키며 결과에는 저장하지 않는다.
3. 기존 Gradle `--tests` 명령을 실행한다. arm64 전용 실행은 무관한 mock Jib
   task를 다음 두 인자로 제외한다: `-x
   :bluetape4k-mock-web-server:jibDockerBuild -x
   :bluetape4k-mock-webflux-server:jibDockerBuild`. 실행 전 기존 XML 목록을
   기록·정리하고 실행 시작
   이후 생성·수정된 regular non-symlink XML만 허용한다. native gate는 attempt별
   evidence root를 고정 allowlist의
   `-Dtestcontainers.image-gate.evidence-dir=<attempt-root>` JVM property로
   전달하고, 대표 test writer가 그 root의 bounded marker 파일
   `Ignite node started OK`를 server readiness 직후 workload 전에 한 번 기록한다.
   runner는 이 property 외의 arbitrary JVM property/Gradle option을 받지 않으며,
   최종 argv와 static fixture가 property와 writer 경로를 함께 고정한다.
   marker 파일은 root containment·mtime·1 KiB 상한을 검증한 뒤 schema의
   `marker_source=bounded_attempt_marker`로만 요약하며 raw log는 저장하지 않는다.
4. XML은 예상 결과 root 밖으로 나갈 수 없고 파일당 최대 4 MiB, family당 최대
   8개 regular XML, XML당 최대 2,048 testcase와 10,000 counter를 허용한다.
   DTD/ENTITY, malformed XML, exact suite/classname 불일치, bounded counter 위반은
   `blocked`다. parser는 bounded bytes를 먼저 읽고 external entity를 비활성화하며,
   `O_NOFOLLOW`/inode 재검증으로 parse 전후 symlink·TOCTOU 교체를 차단한다.
   XML·stdout·stderr·diagnostic이 상한을 넘으면 raw 값을 저장하지 않고 즉시
   `blocked`다.
   `tests > 0`, `tests > skipped`, `failures == 0`, `errors == 0`과 정확한
   `workloadTestPattern` testcase의 non-skipped 성공을 모두 확인한다.
   pattern은 `classname + name` grammar로 해석하고 Kotlin/JUnit name의 선택적
   `()` suffix만 canonicalize한다. suite 전체 `tests`와 workload testcase의
   `workload_tests=1`을 별도 필드로 기록한다.
5. `docker image inspect`의 allowlist(`Id`, `RepoDigests`, `Os`, `Architecture`),
   `docker info`, runner `uname -m`, Docker context를 수집한다. `DOCKER_HOST`와
   `DOCKER_CONTEXT`는 비워 두고 `docker context show == default` 및 local Unix
   socket endpoint만 허용하며, remote context/endpoint는 `blocked`다.
   strict Ignite2 gate에서는 `TESTCONTAINERS_REGISTRY_MIRROR`도 비워 두고
   registry host는 `docker.io` allowlist만 사용한다. 허용되지 않은 mirror나
   endpoint는 image digest가 있어도 성공하지 않는다.
   `x86_64`/`amd64`,
   `aarch64`/`arm64`를 canonical 값으로 정규화하고 image·daemon·runner의 OS와
   architecture가 기대값과 모두 일치해야 한다.
6. schema v2 artifact에는 `platform_id`, runner label, commit/ref/run,
   expected/observed platform, image ref/digest, pull result, JUnit counts,
   workload testcase와 startup marker만 저장한다. raw env/config, JUnit
   `system-out`, unbounded inspect/log는 저장하지 않고 allowlist와 bounded
   redaction을 적용한다. 성공·실패 공통으로 stdout/stderr는 각각 64 KiB/1,000
   lines, Docker logs/events/inspect는 각각 64 KiB/500 lines, per-family JSON은
   128 KiB, summary 전체는 1 MiB, artifact 전체는 8 MiB로 제한한다. Docker
   auth JSON, basic-auth URL, known secret 값과 multiline token은 회귀 테스트로
   차단한다. report ID/image/tag/workload/selector는 grammar·control character·
   leading dash를 거부하고, release/nightly runner는 arbitrary `--gradle-task`,
   JVM property, Gradle option을 받지 않는 고정 argv만 사용한다.
7. attempt state machine은 `pull → test → evidence → cleanup` 순서를 지킨다.
   각 attempt는 고유한 `run_id/attempt` root·container label·XML 기준 목록을
   만들고, retry 전 해당 attempt root만 제거한다. pull transport failure는 pull
   단계에서만 재시도하고, daemon reset/refused 또는 transient test timeout은
   `max_attempts > 1`인 arm64 경로에서만 verified digest를 재사용하는 test 단계
   재시도를 허용한다. x64 `max_attempts=1`은 in-job retry를 하지 않고 transient
   failure를 새 workflow run에서 재실행하는 정책이다. auth/manifest/digest
   오류, product assertion, architecture mismatch, XML/evidence 부재는 재시도하지
   않는다. 모든 경로는 `finally` cleanup으로 process group·container·marker를
   bounded 정리하고, stale XML/marker를 다음 attempt가 읽지 않게 한다.
8. 실패하면 선택 image ref와 관련 container ID를 사용해 bounded
   `docker logs`, `docker inspect`, `docker events`를 병렬 수집한다. 각 diagnostic
   command는 30초/64 KiB/500 lines를 넘지 않으며, x64 attempt 전체 diagnostics
   budget은 30초, arm64 attempt 전체는 120초다. retry 여부는 7번 state machine이
   결정하며 diagnostics 수집은 원래 failure 분류를 바꾸지 않는다. auth/manifest/
   digest 오류, product assertion, architecture mismatch, XML/evidence 부재는
   retry하지 않고 즉시 종료한다. attempt cleanup과
   `--max-attempts`/pull·test·diagnostic timeout/job timeout의 worst-case budget을
   §5.4의 공식으로 검증한다.
9. Release verifier는 `platform_id`, `coverage`, `release_gate`, expected/actual
   tag·architecture와 필수 evidence 필드를 직접 확인한다. `selected == 0`,
   `skipped`, `continue-on-error`, 이전 artifact 재사용은 성공으로 완화하지
   않는다.
10. 모든 subprocess는 unbounded `capture_output`을 사용하지 않고 capped
   streaming 또는 capped temporary file로 UTF-8 byte/line 한도를 적용한다.
   pull·Gradle·diagnostic·전체 job에 독립 deadline을 두고 timeout 시 process
   group을 종료한 뒤 bounded tail만 redaction한다. job budget guard가 실제
   subprocess wall-clock과 attempt 누계를 비교해 초과를 `blocked`로 만든다.
11. 결과는 매 run fresh staging root에서만 만든다. family/platform ID는
    `^[a-z0-9][a-z0-9._-]{0,63}$`, image/tag/workload는 허용 grammar와 control
    character/leading dash 검사를 통과해야 한다. root containment, regular
    non-symlink, exact allowlist(`summary.json`, `summary.md`, family JSON,
    bounded diagnostics JSON)만 staging하고 extra file·symlink·path escape는
    `blocked`다. upload action은 staging 파일 목록만 대상으로 한다.

schema v2의 per-family 결과는 다음 필드를 최소 계약으로 고정한다.

```json
{
  "schema_version": 2,
  "family_id": "ignite2",
  "platform_id": "arm64",
  "image_ref": "apacheignite/ignite:2.18.0-arm64",
  "expected": {"os": "linux", "tag": "2.18.0-arm64", "architecture": "arm64", "runner": "ubuntu-24.04-arm"},
  "observed": {"runner_os": "linux", "runner_architecture": "arm64", "daemon_os": "linux", "daemon_architecture": "arm64", "image_os": "linux", "image_tag": "2.18.0-arm64", "image_architecture": "arm64"},
  "pull": {"requested_ref": "docker.io/apacheignite/ignite:2.18.0-arm64", "attempts": 1, "status": "success", "cache": "up_to_date", "event_id": "<pull-event-id>", "image_id": "sha256:<image-id>", "digest": "sha256:<digest>", "elapsed_seconds": 12.3},
  "junit": {"suite": "<exact-suite>", "tests": 3, "skipped": 0, "failures": 0, "errors": 0, "workload_testcase": "<exact-testcase>", "workload_tests": 1},
  "startup": {"ready": true, "marker": "Ignite node started OK", "marker_source": "bounded_attempt_marker"},
  "provenance": {"commit": "<sha>", "ref": "<ref>", "workflow_run_id": "<run-id>"}
}
```

artifact 이름은 `testcontainers-image-gate-<workflow_run_id>-<platform_id>`
형식으로 고정하고, 고정 job의 실제 이름은 Nightly x64
`nightly-testcontainers-image-gate-${{ github.run_id }}-amd64`, Nightly arm64
`nightly-testcontainers-image-gate-${{ github.run_id }}-arm64`, Release x64
`release-testcontainers-image-gate-${{ github.run_id }}-amd64`, Release arm64
`release-testcontainers-image-gate-${{ github.run_id }}-arm64`로 사용한다.
`if: always()`와 `if-no-files-found: error`를 적용하며, artifact 내부 provenance의
`workflow_run_id`와 이름의 `run_id`가 일치해야
한다. 초과 시 artifact에는 해당 항목의 `blocked` 상태와 제한 이름만 남긴다.
업로드는 aggregate counts와 per-family artifact 참조만 담은 `summary.json`,
`summary.md`, family별 schema v2 JSON과 bounded diagnostics JSON만 허용하고
raw XML·stdout·stderr·Docker log 파일은 업로드하지 않는다. summary에는 상세
stdout/stderr를 중복 저장하지 않아 1 MiB 상한을 지키며, nightly artifact
retention은 14일, release artifact retention은 30일이다.
성공 artifact에는 위 allowlist 외의 환경변수, registry 설정, raw XML
`system-out`, 전체 Docker inspect/log를 포함하지 않는다. 실패 진단도 동일한
bounded/redacted 규칙을 따른다.

summary schema v2는 `schema_version`, `workflow_run_id`, `commit`, `ref`,
`scope`, `selected`, `success`, `product_failure`, `infrastructure_failure`,
`blocked`, `coverage`, `release_gate`, `platforms[]`를 필수로 하며,
`platforms[]` 각 항목은 `platform_id`, expected/observed tag·OS·architecture,
`pull.event_id`, digest, workload testcase/count와 family artifact reference를
포함한다. schema v1, partial platform entry, missing artifact reference는
verifier에서 거부한다.

`0 tests`와 `all skipped`는 제품 실패가 아니라 실행 증거 부재이므로
`blocked`로 분류한다. `release_gate`는 선택 family가 모두 `success`이고
`releaseRequired`인 경우에만 true다.

Digest pin은 이번 범위에서 수동으로 추가하지 않지만, strict gate의 Docker Hub
allowlist·mirror 차단과 pull 직후 digest 기록으로 관찰 provenance를 고정한다.
mutable tag의 공급망 잔여 위험은 `WATCH`로 기록하고, Release verifier는 허용된
registry host·expected tag·actual digest가 모두 있는 경우에만 통과시킨다.

### 5.3 Ignite2 runtime test

`Ignite2ServerTest`는 클래스 수준 `@Disabled`를 제거한다.

- 대표 test `representativeStartupAndWorkload`: 명시적 tag 없이 기본 생성자로
  `Ignite2Server`를 시작하고 `Ignition.startClient`로
  `host:port`에 연결한다. 중앙 `ignite-core`의 `ClientConfiguration`으로
  test cache를 생성해 `put`/`get`하고 값을 검증한 뒤 client를 닫는다.
  readiness marker를 기록한 뒤 workload를 실행하고, marker가 없으면 native
  gate가 `blocked`가 되도록 test fixture를 둔다. `ClientConfiguration`의
  connect/request timeout은 각각 30초로 설정하고 대표 test에는 6분 JUnit
  timeout(5분 container startup + 1분 workload 여유)을 둔다. timeout·assertion·예외 모든 경로에서 client/container의
  `use`/`finally` cleanup이 실행되는지 fixture로 검증한다.
- 기본 port test: `useDefaultPort = true`로 startup과 `PORT == 10800`을
  검증한다.
- 입력 검증 test: blank image/tag가 `IllegalArgumentException`을 발생시키는
  기존 계약을 유지한다.
- explicit tag override test는 `Ignite2Server(tag = ...)` 경로를 별도로 검증한다.
  native gate의 대표 test는 tag를 직접 주입하지 않아 `DEFAULT_TAG`와 실제
  runner architecture의 계약을 검증한다. public API signature는 변하지 않는다.
- `DEFAULT_TAG`는 `x86_64`/`amd64`를 `2.18.0`, `aarch64`/`arm64`를
  `2.18.0-arm64`로 매핑하고 unknown architecture는 fail-fast한다. 이 매핑은
  eager companion 초기화가 아니라 lazy getter/helper 또는 동등한 sentinel 경로로
  계산한다. 따라서 explicit `Ignite2Server(tag = ...)`와 명시적 custom image/tag
  경로는 default resolver를 호출하지 않고 unknown architecture에서도 그대로
  동작한다. canonical `apacheignite/ignite`의 no-tag만 supported architecture
  mapping을 적용하고, custom image에 tag가 없거나 canonical image가 unknown/
  Rosetta이면 fail-fast 및 native gate `blocked`로 기록한다. unknown-arch JVM
  fixture가 explicit/custom 경로와 canonical no-tag 경로를 구분해 검증한다. 이
  정책과 explicit/custom tag fixture를 EN/KO README와 KDoc에 동일하게 문서화한다.

`testing/testcontainers/build.gradle.kts`에는 중앙 catalog의
`testImplementation(bt4k.ignite.core)`만 추가하고, dependency resolution fixture가
`org.apache.ignite:ignite-core:2.18.0`을 확인한다. 이 test-only dependency가
published POM에 유입되지 않는지도 확인한다.

container test는 `AbstractContainerTest`의 `SAME_THREAD` 실행 계약을 유지하고,
모든 client/container 자원을 `use`로 닫는다. EN/KO README thin-client 예제도
동일한 `use`/`close` lifecycle과 `Ignition.startClient` 흐름을 사용해 실행 코드와
문서 계약이 어긋나지 않게 한다.

### 5.4 Workflow 경계

- **PR CI**: `test_testcontainers_contract.py`, manifest contract, Python runner
  unit/static tests만 실행한다. `run_testcontainers_image_gate.py` runtime 호출은
  계속 금지한다.
- Nightly/Release workflow는 `DOCKER_AUTH_CONFIG`를 job-level `env`에 두지 않고,
  runner의 dedicated pull 단계에만 secret input으로 전달한다. Gradle/test,
  diagnostics, summary/upload 단계의 env는 sanitized allowlist로 고정한다.
- **Nightly full**: 기존 52-family x64 gate가 amd64 evidence를 생성한다. 일반
  `test-testcontainers`의 `storage-cache` group에서는 Ignite2를 제외한다.
  x64 image-gate `runs-on`은 `ubuntu-24.04`로 고정하고 job timeout은 360분,
  `max_attempts=1`, generic family의 pull/test/diagnostic timeout은
  60초/4분/30초(aggregate), strict Ignite2의 test timeout은 manifest의
  6분(5분 container startup + 1분 Gradle/workload 여유)으로 고정하며
  `--job-budget-minutes 360`을 전달한다. x64 `max_attempts=1`은 in-job retry를
  하지 않고 transient failure를 새 workflow run에서 재실행한다. 최악 예산은
  `51 × 1 × (1 + 4 + 0.5) + 1 × 1 × (1 + 6 + 0.5) + 30 = 318분 < 360분`이다.
  job id
  `test-testcontainers-ignite2-arm64-image-gate`는 `timeout-minutes: 90`과 고정
  `ubuntu-24.04-arm`에서 `--family-id ignite2 --platform-id arm64`를 정확히
  한 번 실행하며 `max_attempts=2`, pull/test/diagnostic timeout은
  5분/30분/2분, `--job-budget-minutes 90`으로 고정한다. arm64 최악 예산은
  `1 × 2 × (5 + 30 + 2) + 10 = 84분 < 90분`이다. 두 job의 static contract는
  x64 full 선택 1회, arm64 exact selector 1회, targeted scope 0회를 검증한다.
  arm64 job은 mock Jib task를 빌드하지 않으며 완전한 Gradle invocation에 두
  `-x ...:jibDockerBuild` 인자를 포함한다. full에서 arm64 gate가 성공해야
  `test-testcontainers-spring`, `coverage-report`, `nightly-status`의 `needs`가
  성공한다. 각 upload artifact 이름은 위의 고정 literal run id/platform 조합을
  사용하고
  `if: always()`/`if-no-files-found: error`/14일 retention을 사용한다.
  `full`이 아닌 scope에서는 이 job과 x64 image gate를 skipped로 허용하되
  Spring bridge 조건에서만 허용한다.

  x64 full invocation은
  `python3 scripts/run_testcontainers_image_gate.py --scope full --report-dir
  build/reports/testcontainers-image-gate --default-platform-id amd64
  --max-attempts 1 --pull-timeout-seconds 60 --timeout-minutes 4
  --diagnostic-timeout-seconds 30 --job-budget-minutes 360`이고, arm64 invocation은
  `python3 scripts/run_testcontainers_image_gate.py --scope family --family-id
  ignite2 --platform-id arm64 --require-selection --report-dir
  build/reports/testcontainers-image-gate --max-attempts 2
  --pull-timeout-seconds 300 --timeout-minutes 30
  --diagnostic-timeout-seconds 120 --job-budget-minutes 90`이다. arm64 runner가
  생성하는 최종 Gradle argv는
  `./gradlew -Dtestcontainers.image-gate.evidence-dir=$RUN_EVIDENCE_DIR
  :bluetape4k-testcontainers:test --tests
  io.bluetape4k.testcontainers.storage.Ignite2ServerTest.representativeStartupAndWorkload
  --no-configuration-cache -x :bluetape4k-mock-web-server:jibDockerBuild -x
  :bluetape4k-mock-webflux-server:jibDockerBuild`이며, 두 exclusion이 사라지거나
  mock Jib pre-step가 생기거나 고정 evidence-dir property가 빠지면 static
  contract가 실패한다. `$RUN_EVIDENCE_DIR`는 runner가 attempt별로 생성한
  containment 검증 대상 root이며, 이 최종 argv에는 위 property 외의 JVM
  property가 허용되지 않는다.
- **Nightly targeted**: `testcontainers` scope에서는 x64 full gate와 arm64 gate를
  skipped로 유지하고, Spring bridge도 기존 skipped 허용 조건으로 실행한다.
- **Release**: manifest contract가 통과한 뒤 기존 full x64 gate(`coverage=52/52`)와
  job id `testcontainers-ignite2-arm64-image-gate`(`coverage=1/1`)를 병렬로
  실행한다. 두 job은 각각 360분/90분 budget과 Nightly와 동일한
  max-attempts·pull/test/diagnostic 수치를 사용한다. arm64 job과 summary
  verifier는 각 gate job의 마지막 required step으로 실행하며, `publish.needs`에는
  step 결과가 아니라 `testcontainers-image-gate`와
  `testcontainers-ignite2-arm64-image-gate` job ID를 추가한다. 각 verifier는
  `coverage`, `release_gate`, `platform_id`, expected/actual tag·architecture,
  digest, workload evidence를 직접 검증하고 실패하면 job 자체를 실패시킨다.
  publish는 manifest contract, x64 gate, arm64 gate 세 job 결과의 성공을 모두 요구하며
  `platform_id`,
   expected/actual tag·architecture, digest와 workload evidence 없이 진행하지
  않는다. 각 release artifact 이름은 run id와 platform을 포함하고
  `if: always()`/`if-no-files-found: error`/30일 retention을 사용한다.
  `continue-on-error`와 skipped 결과는 verifier에서 거부한다.
- **ARM runner**: GitHub public ARM64 label은 workflow에 고정하고 실행 시 실제
  예약/architecture로 검증한다. runner unavailable은 skipped나 N/A로 완화하지
  않는다.
- **Aggregation guard**: `full` scope의 Spring bridge, `coverage-report`,
  `nightly-status`는 x64 image-gate와 arm64 Ignite2 gate가 모두 `success`일
  때만 통과한다. `coverage-report`는 `if: always()`여도 full scope에서 두
  결과 중 하나가 `skipped`/`failure`이면 집계를 실패시키고, `nightly-status`는
  `failure|cancelled|skipped`를 full scope에서 거부한다. `testcontainers` targeted
  scope에서만 두 image gate의 `skipped`를 허용한다.

## 6. 실패 모드와 복구

| 실패 | 분류 | 처리 |
|---|---|---|
| XML 없음, pattern suite 없음, `tests=0`, all skipped | `blocked` | release gate 차단, 결과와 경로 기록 |
| 실제 pull/event/digest evidence 없음 | `blocked` | 선택 ref와 결과 기록, 성공 완화 금지 |
| 이미지 pull/daemon/rate limit/timeout | `infrastructure_failure` | transient에만 bounded retry 후 Docker 진단, gate 실패 |
| container startup/readiness/workload assertion 실패 | `product_failure` | logs/inspect/events와 JUnit 결과 저장 |
| 기대 platform과 image/daemon/runner architecture 불일치 | `blocked` | canonical mapping과 tag/runner 계약 재검토 |
| XML path escape, DTD/ENTITY, malformed 또는 secret 노출 | `blocked` | raw artifact 저장 금지, parser/redaction 수정 |
| credential이 test/diagnostic env로 전파되거나 staging allowlist 위반 | `blocked` | pull subprocess 격리·fresh staging 재생성, raw 파일 업로드 금지 |
| remote Docker endpoint, 비허용 registry/mirror, image provenance 불일치 | `blocked` | local/default Docker와 Docker Hub allowlist 확인 후 새 run |
| subprocess/output/job budget 초과 또는 process group 잔류 | `blocked` | bounded kill/cleanup 후 초과 원인 수정, 이전 artifact 재사용 금지 |
| ARM runner label 예약 실패 | workflow failure | matrix 결과를 skipped로 대체하지 않음 |
| manifest/source/README drift | static contract failure | Docker 실행 전에 수정하도록 차단 |

롤백은 새 commit에서 arm64 job과 parser/manifest 계약을 함께 되돌리되, PR CI의
runtime 제외와 stable release의 full x64 gate dependency는 유지한다. ARM job은
queue에서 10분 안에 runner가 할당되지 않으면 owner `debop`이
`gh run cancel <run-id>`로 run을 취소하고 release를
차단한 뒤, runner 가용성을 확인한 새 run에서만 재실행한다. queue 대기는 job
`timeout-minutes`와 별도의 관찰 대상이며 skipped/N/A로 완화하지 않는다.
runner 예약 실패는 재시도 가능한 workflow failure로 분류하고, product/infrastructure/
evidence failure는 새 run에서만 재실행한다. 이전 실행 artifact를 새 성공으로
재사용하지 않으며, rollback이 release 대상 tag에 이미 반영된 경우에도 새
release tag/candidate를 만들어야 한다. rollback verifier는 required artifact가
없거나 `release_gate=false`이면 실패 상태를 유지하고, 새 tag의 x64·arm64
verifier가 모두 성공한 뒤에만 publish를 재개한다.

## 7. stacked PR train과 변경 파일

### Parent: `fix/1486-image-gate-contract` → `develop`

- `scripts/run_testcontainers_image_gate.py`
- `scripts/testcontainers_image_gate.py`
- `scripts/testcontainers_image_gate_manifest.json`의 schema/validation
- `scripts/test_run_testcontainers_image_gate.py`
- `scripts/test_testcontainers_image_gate.py`

Parent는 runtime gate가 실제 test execution과 image/architecture 증거를
검증하는 기반만 제공한다. Parent는 `executionEvidenceRequired`, selector, XML
safety, architecture/security parser와 fixture를 소유한다. 실제 Ignite2
`platforms`/workload metadata는 child가 소유해 parent contract가 먼저 merge되어도
다른 family의 disabled 계약을 깨지 않도록 한다.

### Child: `fix/1486-ignite2-arm64-runtime` → parent

- `testing/testcontainers/src/main/kotlin/io/bluetape4k/testcontainers/storage/Ignite2Server.kt`
- `testing/testcontainers/src/test/kotlin/io/bluetape4k/testcontainers/storage/Ignite2ServerTest.kt`
- `testing/testcontainers/build.gradle.kts`
- `testing/testcontainers/README.md`
- `testing/testcontainers/README.ko.md`
- `.github/workflows/nightly-tests.yml`
- `.github/workflows/release.yml`
- `scripts/test_testcontainers_contract.py`
- `scripts/testcontainers_image_gate_manifest.json`의 Ignite2 `platforms`와
  `workloadTestPattern`
- `docs/superpowers/specs/2026-08-24-issue-1486-ignite2-arm64-image-gate-design.md`

두 PR 모두 base/head와 exact cumulative SHA를 live read-back하고, child PR은
parent merge 전에는 독립 merge하지 않는다.

## 8. 검증 계획과 수용 기준

- [ ] 기준선 `Ignite2ServerTest`의 `3/3 skipped`가 RED 증거로 기록된다.
- [ ] runner unit test가 성공, XML 없음, `tests=0`, all skipped, pattern mismatch,
      stale/malformed XML, selector 0/복수개, architecture mismatch, pull/digest
      부재, secret redaction, local/remote Docker context를 각각 검증한다.
- [ ] pull 전 event observer가 run/family/platform/attempt/ref와 correlation되고,
      pull cache/event/digest 및 verified-digest 재사용이 schema와 fixture로
      검증된다.
- [ ] unskipped Ignite2 test가 startup과 thin-client cache put/get을 통과한다.
- [ ] thin-client connect/request 30초, 대표 JUnit 6분 timeout(5분 startup + 1분
      workload 여유)과 timeout 경로의 `use`/cleanup, attempt별 XML/marker 격리가
      검증된다.
- [ ] 대표 test가 명시적 tag 없이 `DEFAULT_TAG`를 검증하고 explicit override와
      custom image tag 정책을 별도 검증하며, readiness marker가 workload 전에
      bounded attempt marker로 생성되는지 검증한다.
- [ ] manifest가 `amd64`/`arm64` tag·runner·architecture를 검증한다.
- [ ] 기존 disabled family는 `executionEvidenceRequired=false` 계약으로
      52/52 full gate의 의도된 상태를 유지하고, 필드 미지정은 `false`로
      해석한다.
- [ ] EN/KO README와 KDoc이 `2.18.0`/`2.18.0-arm64` 및 platform mapping과
      일치한다.
- [ ] PR workflow에는 image runtime 호출이 없고, Nightly/Release만 arm64 native
      gate를 실행하며 일반 `storage-cache`에서 Ignite2를 중복 실행하지 않는다.
      static contract가 x64 1회 + arm64 1회, targeted 0회를 세고 arm64 명령의
      두 mock Jib exclusion과 artifact run/platform 이름을 검증한다.
- [ ] runner unit/static test가 x64/arm64의 수치 예산 공식, retry 가능 오류 분류,
      pull 최대 횟수와 image ID/digest 재사용, XML·log·artifact byte/line 상한과
      retention/upload 파일 allowlist를 검증한다. capped streaming, process-group
      kill, fresh staging, safe ID/path grammar와 report-root containment도
      검증한다.
- [ ] credential이 pull subprocess에만 존재하고 Gradle/diagnostic/artifact로
      전파되지 않으며, raw/decoded/base64 auth, basic-auth URL, multiline/known
      secret redaction 회귀가 통과한다.
- [ ] release/nightly 고정 argv가 arbitrary Gradle task/JVM option을 거부하고,
      `DOCKER_HOST`/`DOCKER_CONTEXT`/registry mirror 및 remote context가
      fail-closed가 된다.
- [ ] `actionlint`, Python unit/static tests, targeted Gradle test, Kotlin compile,
      `git diff --check`가 통과한다.
- [ ] full x64 amd64 artifact와 arm64 CI artifact에 pull result, image digest,
      Docker/runner/image architecture, startup/workload, JUnit execution count와
      provenance가 모두 존재한다.
- [ ] x64 resolver가 `defaultPlatformId=amd64`와 `platform_id=amd64`를 기록하고,
      workload XML fixture가 `classname + name` 및 선택적 `()` canonicalization,
      suite `tests`와 `workload_tests=1`을 구분한다.
- [ ] summary/per-family schema v2가 expected/observed tag·OS·architecture,
      pull event/digest, workload evidence, provenance와 artifact reference를
      필수화하고 v1/partial/skipped 결과를 거부한다.
- [ ] x64/arm64의 완전한 runner CLI와 arm64 Gradle argv가 static fixture로
      보존되고, `job-budget-minutes` 공식·실제 subprocess wall-clock guard가
      검증된다.
- [ ] Release publish가 기존 52/52 full gate와 arm64 `coverage=1/1` summary
      verifier 없이는 진행되지 않는다.
- [ ] full scope 집계가 x64·arm64 `success`를 모두 요구하고, targeted scope에서만
      두 image gate의 `skipped`를 허용한다.

### 8.1 Writer DoD

- **SPW-01 PASS**: 독자는 bluetape4k contributor이며, Issue #1486, 기준 커밋,
  현재 source/workflow/manifest와 공식 GitHub·Ignite 문서를 근거로 고정했다.
- **SPW-02 PASS**: 대안, 범위, manifest/runner/runtime/workflow 계약, 실패 모드,
  호환성, stacked train, 증거 schema, 수용 기준과 rollback을 포함했다.
- **SPW-03 PASS**: 한국어 기술 문체와 고정 용어를 적용하고 code token, command,
  URL, 숫자, status token을 보존했다. `korean-naturalness-checklist.md`를
  기준으로 문장과 표를 읽었다.
- **SPW-04 PASS**: 현재 worktree의 `Ignite2Server`, test XML, runner,
  manifest, workflows와 Issue #1486 acceptance를 대조했다. ARM runner 예약,
  실제 pull/digest와 runtime workload는 구현 후 CI에서 확인해야 하는 미확인
  항목으로 남겼다.
- **SPW-05 PASS**: 최종 Markdown을 다시 읽고 headings, tables, code fence,
  checklist, link를 확인했다. `git diff --check`와
  `audit-korean-terms.mjs`가 통과했다.

## 10. 공개 API·성능·안정성 검토

- public constructor/operator와 `Ignite2Server` property 계약은 유지한다.
- test-only `ignite-core`는 중앙 catalog의 기존 `ignite` version을 사용하며
  publish artifact에는 포함하지 않는다.
- PR CI runtime 비용은 증가하지 않는다. Nightly/Release는 기존 x64 full gate에
  더해 Ignite2 arm64 한 번의 native 실행 비용만 추가한다. x64/arm64는 Release에서
  manifest 이후 병렬 실행하고, 각각 318분/84분의 worst-case budget이 job timeout
  안에 들어가는지 static contract로 검증한다.
- cache/client/container는 모두 명시적으로 닫고, test runner는 family를
  순차 실행해 Docker pull 압력과 공유 daemon 경쟁을 제한하되, Release의 두
  platform job은 서로 병렬로 시작해 불필요한 gate 직렬 대기를 피한다.
- image inspect와 JUnit XML이 없으면 성공하지 않으므로 green-but-unverified
  결과를 방지한다. credential은 pull 단계에만 격리하고, process/output/job
  budget과 fresh staging allowlist를 강제해 결과·진단 경로의 누출과 무제한
  대기를 방지한다.

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
