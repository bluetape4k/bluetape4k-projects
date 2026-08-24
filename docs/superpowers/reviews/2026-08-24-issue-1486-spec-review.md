# Issue #1486 설계 명세 독립 리뷰 기록

## 1. 범위와 현재 결론

- **대상 명세**:
  `docs/superpowers/specs/2026-08-24-issue-1486-ignite2-arm64-image-gate-design.md`
- **대상 단계**: Type A Full Feature Step 2-R
- **검토 기준**: Issue #1486 acceptance, 기준선 `Ignite2ServerTest` XML, 현재
  runner/manifest/workflow, 중앙 `bluetape4k-dependencies` catalog, 공식
  GitHub Actions·Apache Ignite 문서
- **검토 관점**: performance, stability, security, operator/ops,
  developer/API, user/caller
- **현재 판정**: `BLOCK` — P0는 없지만 교정 전 P1이 남아 있어 설계 수정과
  영향을 받는 관점의 재검토가 필요하다.
- **구현 상태**: production code, Gradle build, Docker runtime gate는 아직
  실행 대상이 아니다. 기준선 읽기와 설계 검토만 수행했다.

사용자/caller lane은 native thread 한도로 별도 writer lane을 시작하지 못했다.
해당 lane은 helper에 `lane-block`으로 기록했고, 같은 설계에 대해 완료된 native
fallback reviewer의 caller 결과를 독립 증거로 보존해 통합 표에 반영했다.

## 2. 독립 검토 결과

| 관점 | P0 | P1 | P2/P3 | 최초 판정 | 핵심 근거 |
|---|---:|---:|---:|---|---|
| Performance | 0 | 5 | 3 | BLOCK | x64/amd64 중복 실행·pull, matrix 직렬화, mock Jib 재실행, workload 선택, output/stale XML/timeout budget |
| Stability | 0 | 7 | 2 | BLOCK | 빈 platform 선택, stale XML, architecture 정규화, platform 진단, client timeout, retry/cleanup, workflow needs |
| Security | 0 | 2 | 3 | BLOCK | credential artifact 노출, manifest runner trust boundary, XML/JSON·path·명령 검증, mutable tag provenance |
| Operator/Ops | 0 | 4 | 2 | BLOCK | 실제 pull 증거, native architecture, Release summary, artifact provenance/schema |
| Developer/API | 0 | 4 | 3 | BLOCK | 기존 disabled family와 strict parser 충돌, workload testcase, selector/tag 전달, architecture 정규화 |
| User/Caller | 0 | 2 | 4 | BLOCK | 기본 tag 자체 검증, 일반 matrix 중복, architecture alias/custom image 정책, README/KDoc lifecycle |
| **통합 최초 결과** | **0** | **24** | **17** | **BLOCK** | 아래 교정안을 설계에 반영하고 affected lane을 재검토한다. |

### 2.1 교정 대상과 중복 제거

리뷰 결과를 중복 제거하면 다음 P1 계약이 남는다.

| 통합 ID | 관련 관점 | 설계 결함 | 필수 교정 |
|---|---|---|---|
| S-01 | performance, user, developer | Ignite2가 일반 Nightly matrix, 기존 52-family x64 gate, 새 x64/arm64 matrix에서 반복 실행된다. | 기존 full x64 image gate를 **amd64의 단일 authoritative 실행**으로 사용하고, 일반 `storage-cache` matrix에서 Ignite2를 제외하며, 새 native job은 `arm64` 한 leg만 실행한다. 실행 횟수와 local Docker 요구사항을 DoD에 고정한다. |
| S-02 | performance, developer | 기존 52-family에는 의도적인 class-level `@Disabled` family가 있어 모든 suite에 strict JUnit execution proof를 적용하면 full gate가 회귀한다. | manifest에 `executionEvidenceRequired`를 도입하고 Ignite2에만 `true`를 준다. 일반 family는 기존 `BUILD SUCCESSFUL` 계약을 유지하며 disabled-family regression fixture를 추가한다. |
| S-03 | developer, stability | class suite 집계만으로는 thin-client put/get workload가 실제 실행됐는지 보장하지 않는다. | Ignite2에 정확한 `workloadTestPattern`을 추가하고 XML에서 해당 testcase가 non-skipped·failure/error 0인지 별도 검증한다. full x64와 arm64 모두 같은 workload 계약을 사용한다. |
| S-04 | stability, developer | `--family-id`/`--platform-id`가 0개·복수·unknown 선택을 fail-open으로 만들 수 있다. | exact selector와 `--require-selection`을 추가하고, 필수 platform invocation은 정확히 하나의 family/platform만 허용한다. 결과·command·diagnostic·report에 선택 platform을 전달한다. |
| S-05 | performance, stability, ops, developer | `x86_64`/`amd64`, `aarch64`/`arm64`와 image/daemon/runner 값 비교 규칙이 없다. | canonical mapping을 고정하고 runner `uname`, Docker daemon, image `.Os`/`.Architecture`를 모두 비교한다. unknown·빈 digest·원격 context 불일치는 `blocked`다. |
| S-06 | security, ops | raw stdout/system-out/inspect/env와 Docker credential이 artifact로 유출될 수 있고, manifest runner가 self-hosted label을 선택할 수 있다. | allowlist 필드만 저장하고 XML/log/output에 bounded redaction을 적용한다. runner는 workflow의 고정 allowlist(`ubuntu-24.04`, `ubuntu-24.04-arm`)로만 선택하며 manifest 값은 설명·정적 검증에만 사용한다. secret 값·Docker auth JSON·basic-auth URL·multiline token 회귀 테스트를 추가한다. |
| S-07 | stability, performance, security | stale XML, malformed/unsafe XML, path escape, unbounded output이 현재 실행 증거를 오염시킬 수 있다. | attempt 전 기존 XML 기준 파일 목록/정리와 시작 이후 freshness를 적용하고, regular non-symlink 파일·root containment·size limit·DTD/ENTITY 금지·bounded counters·exact suite match를 검증한다. parse error/suite 부재/all skipped는 `blocked`다. |
| S-08 | ops, stability | image inspect/digest만으로는 실제 pull과 선택 tag provenance를 증명하지 못하고 Release platform summary 검증이 없다. | Ignite2 platform entry는 pull evidence를 요구한다. 선택 image ref의 pull result/event, digest, cache 상태, expected/observed tag·architecture를 schema에 포함하고, Release arm64 job이 `coverage=1/1`, `release_gate=true`, expected platform을 직접 검증한다. `RepoDigests`가 비면 `blocked`다. |
| S-09 | stability, ops | retry가 product/blocked failure까지 반복되고 client timeout, cleanup, workflow `needs`가 추상적이다. | transient infrastructure만 bounded retry하고 product/blocked는 즉시 종료한다. client/request·test·job timeout, attempt cleanup, Gradle daemon, worst-case budget을 고정하며 Nightly Spring bridge·coverage·status와 Release publish의 `needs`/skipped 조건을 명시한다. |
| S-10 | performance, user | arm64 전용 실행이 무관한 mock Jib image를 다시 빌드하고 문서 예제가 client lifecycle을 누락한다. | arm64 job은 mock Jib task를 `-x`로 제외하고, thin-client README EN/KO 예제는 `Ignition.startClient`와 `use`/`close`를 사용한다. |
| S-11 | user | `DEFAULT_TAG`가 실제 native default를 검증하지 않고 custom image에 ARM tag를 조용히 적용할 수 있다. | 기본 생성자 경로와 명시적 tag override를 별도 검증한다. canonical `apacheignite/ignite`에만 default tag를 적용하고 custom image는 명시적 tag를 요구하거나 문서화된 정책을 테스트한다. 지원 alias와 unknown/Rosetta 정책을 EN/KO README·KDoc·manifest에 맞춘다. |

### 2.2 검토 중 정정된 사실

Performance lane은 `ignite-core` alias가 없다고 보고했으나, live immutable
catalog ref `91f9ea9336b5ea991f5675323a1cf25ccfd6f5ed`의
`libs.versions.toml`에 `ignite-core = { module = "org.apache.ignite:ignite-core", version.ref = "ignite" }`가 존재함을 재확인했다. 따라서 alias 부재는 현재 P1이 아니다. 계획에는 catalog resolution/compile 검증을 남긴다.

## 3. 승인 후 설계 교정안

위 S-01, S-02, S-08은 기존 “x64 + Ignite2 native amd64/arm64 matrix” 구조를
바꾼다. 승인 후 설계 문서에 다음을 반영한다.

1. 기존 full x64 gate가 모든 family를 순차 실행하면서 Ignite2의 amd64 실행
   증거를 생성한다. 일반 Nightly `storage-cache` matrix에서는 Ignite2를
   제거해 동일 workload를 반복하지 않는다.
2. Nightly/Release에는 고정 `ubuntu-24.04-arm`의 Ignite2 arm64 전용 gate만
   추가한다. `runs-on`은 manifest에서 읽지 않으며, arm64 job은 mock Jib
   image build를 하지 않는다.
3. strict JUnit/workload/image evidence는 `executionEvidenceRequired`가 있는
   Ignite2에만 적용한다. 기존 disabled family의 full 52/52 의미는 별도
   회귀 fixture로 보존한다.
4. `summary.json` schema v2와 per-family evidence 필수 필드를 고정한다.
   성공 경로에는 counts, workload testcase, pull result, digest, expected와
   observed platform, commit/ref/run provenance를 남기고 raw credential/env와
   JUnit `system-out`은 남기지 않는다.
5. Release는 기존 `coverage=52/52` x64 verifier와 별도로 arm64
   `coverage=1/1` verifier를 통과해야 publish한다. 이전 artifact 재사용과
   `skipped` 완화를 금지한다.

이는 초기 승인 설계의 단순한 문구 보완이 아니라 실행 비용·release gate·증거
소유권을 바꾸는 material repair다. 따라서 spec 수정 전에 사용자의 fresh
approval을 받고, 수정 후 performance/stability/security/ops/developer/user
관점 affected lane을 재검토한다. 재검토 종료 조건은 `P0=0`, `P1=0`이다.

## 4. 재검토 계획

- 위 S-01~S-11을 design spec에 반영한다.
- 수정된 spec에 `SPW-01`~`SPW-05`를 다시 적용한다.
- 기존 six-lane 결과와 수정 diff를 대조해 affected lane을 다시 실행한다.
- `testcontainers_image_gate.py`의 기존 disabled-family fixture와 새 Ignite2
  selector/XML/architecture/security fixture를 읽기 전용으로 확인한다.
- 재검토 결과가 `P0=0`, `P1=0`일 때만 Step 3 implementation plan으로 진행한다.

## 5. 현재 DoD

| 항목 | 상태 | 증거 |
|---|---|---|
| 기준선과 Issue acceptance 대조 | PASS | `Ignite2ServerTest` 기준선 `tests=3, skipped=3`, Issue #1486 live read |
| six perspective review 수집 | PASS* | performance/stability/security/ops/developer 결과와 user fallback 결과 보존 |
| review integration artifact | PASS | 본 문서, 통합 ID S-01~S-11 |
| P0/P1 convergence | BLOCKED | 최초 통합 P0=0, P1=24; material repair과 fresh approval 필요 |
| production code 변경 없음 | PASS | worktree diff는 설계/review 산출물과 Python `__pycache__` 생성물만 포함 |
| plan/implementation 진행 | PENDING | spec repair 승인 후 수행 |

`*` user lane은 native thread limit으로 helper에 `lane-block`을 기록했으며,
동일 설계에 대한 native fallback caller 결과와 main-session 통합으로 증거를
보존했다. 이 lane 상태는 리뷰 결과를 숨기지 않기 위한 운영 기록이다.

## 6. 최종 판정

현재 Step 2-R은 `BLOCK`이다. 설계 교정안에 대한 fresh approval 전에는
production code나 implementation plan을 시작하지 않는다.

## 7. 수정 후 재검토 기록

사용자는 material repair 후 재검토를 승인했다. 다음 재검토는 수정된 설계와
초기 BLOCK 증거를 함께 대조했으며, 구현·실제 ARM runner 예약·Docker pull·Ignite
workload 실행은 아직 수행하지 않았다.

| 관점 | P0 | P1 | P2/P3 | 수정 후 판정 | 증거와 남은 WATCH |
|---|---:|---:|---:|---|---|
| Performance | 0 | 0 | 2 | WATCH | 정량 timeout/retry/output/pull/count 계약은 통과. checkout/setup 고정 overhead와 52-family 최악 8 MiB artifact fixture는 구현 검증 WATCH. 초기 rerun BLOCK 증거는 `evidence-spec-rerun-performance-1486-result.json`에 보존했다. |
| Stability | 0 | 0 | 1 | WATCH | attempt 격리, cleanup, digest 재사용, full success-only 집계는 통과. ARM queue 10분 관찰·취소 runbook은 운영 WATCH이며 native lane timeout 후 main fallback으로 판정했다. |
| Security | 0 | 0 | 1 | WATCH | pull 전용 credential, redaction, local Docker endpoint, XML/path/output 차단은 통과. mutable tag와 digest pin 제외는 범위상 WATCH다. 이전 317분 수치 지적은 318분으로 정정되어 해소됐다. |
| Operator/Ops | 0 | 0 | 3 | CLEAR | selector, runner/job topology, verifier needs, artifact literal·retention, x64/arm64 budget과 queue/rollback 절차를 최신 설계에서 확인했다. 실제 ARM 예약과 pull/workload 증거는 구현 후 CI에서 확인한다. |
| Developer/API | 0 | 0 | 1 | WATCH | fixed `-Dtestcontainers.image-gate.evidence-dir=$RUN_EVIDENCE_DIR` argv, x64 no in-job retry, 6분 test/JUnit timeout, lazy default resolver와 unknown-arch fixture가 명시됐다. custom-tag/unknown-arch 동작은 구현 테스트에서 확인한다. |
| User/Caller | 0 | 0 | 0 | CLEAR | default constructor/no-tag, duplicate matrix 제거, explicit/custom policy, lifecycle 문서 parity를 확인했다. native thread 제한으로 lane-block된 fallback 증거는 초기 기록과 함께 보존했다. |
| **수정 후 통합** | **0** | **0** | **8** | **PASS with WATCH** | material repair의 종료 조건 P0=0/P1=0을 충족했다. P2 WATCH는 구현·첫 Nightly/Release에서 해소하거나 후속 이슈로 분리한다. |

### 7.1 수정된 핵심 계약

1. x64 strict Ignite2 test timeout과 JUnit timeout은 6분으로 통일하고, 5분
   container startup과 1분 workload 여유를 분리한다. x64 `max_attempts=1`은
   in-job retry를 하지 않으며 transient failure는 새 workflow run에서만 다시
   실행한다. 최악 예산은 318분이고 job timeout 360분보다 작다.
2. arm64 최종 Gradle argv는 fixed
   `-Dtestcontainers.image-gate.evidence-dir=$RUN_EVIDENCE_DIR`를 전달하고,
   runner/test writer/static fixture가 같은 attempt root를 사용한다. arbitrary
   JVM property와 mock Jib pre-step는 금지한다.
3. `DEFAULT_TAG` 매핑은 lazy getter/helper 또는 sentinel 경계에서 평가한다.
   explicit custom tag는 unknown architecture에서도 default resolver를 호출하지
   않으며, canonical no-tag만 supported mapping과 fail-fast 정책을 적용한다.
4. manifest의 `defaultPlatformId=amd64`, platform별 tag/runner/architecture,
   schema v2, pull event/digest, startup marker, workload testcase와 Release
   verifier/aggregation guard를 유지한다.

## 8. Step 2-R 통합 DoD

| 항목 | 상태 | 증거 |
|---|---|---|
| 최초 six-perspective review 보존 | PASS | 본 문서 §2 및 S-01~S-11 |
| material repair 후 fresh approval | PASS | 사용자 최신 `승인` 메시지 |
| affected perspective rereview | PASS* | `.bluetape/evidence-spec-rerun-*-1486-result.json`와 main fallback evidence |
| P0/P1 convergence | PASS | 수정 후 통합 P0=0, P1=0 |
| SPW-01~SPW-05 | PASS | 설계 문서 Writer DoD 및 `git diff --check`/용어 감사 |
| production code/Gradle/workflow 구현 | PENDING | Step 3 plan gate 이후 수행 |
| 실제 CI ARM/pull/workload 증거 | PENDING | 구현·Nightly/Release 실행 필요 |

`*` stability native lane은 5분 deadline으로 terminal `lane-block`되어 main-session
fallback을 사용했다. 이는 결과를 숨기지 않고 evidence와 함께 기록한 운영 상태다.

## 9. 최종 Step 2-R 판정

**PASS with WATCH — implementation plan으로 진행 가능.** 초기 설계의 BLOCK은
수정 후 P0/P1이 모두 해소되어 종료한다. mutable tag, ARM queue, pre-step/artifact
worst-case와 실제 runtime evidence는 구현 및 첫 CI 실행에서 확인할 후속 WATCH이며,
현재 production code 변경은 없다.
