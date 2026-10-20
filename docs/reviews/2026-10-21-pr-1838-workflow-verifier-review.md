# PR #1838 워크플로 검증기 독립 리뷰 후속

## 검토 범위와 판정

- 저장소: `bluetape4k/bluetape4k-projects`
- 검토한 PR head: `1a537ec61fd03e285652ab3f34ff16639507dcb4`
- base: `34c84a6115c80e3b3dfcde84be54106084259cfa`
- 리뷰 lane: `issue1832_postfix_review` (`code-reviewer`)
- 검토 파일: 18개
- 판정: **REQUEST CHANGES** — P0 0건, P1 5건, P2 1건

리뷰어는 actionlint가 허용하는 워크플로 변형을 검증기에 적용해 여섯 우회를 재현했다. 이 변경은 Python 워크플로 검증기와 테스트에 한정되며 Kotlin 제품 코드, 공개 API/ABI, 발행 모듈은 바꾸지 않는다.

## 발견과 대응

| 심각도 | Tier | 재현한 우회 | 대응 |
|---|---|---|---|
| P1 | T4, T7 | `uses: "actions/checkout\u0040v7"`로 checkout을 감추고 다른 job에 유효한 checkout을 추가 | escaped `uses` 값을 거부하고 보상 checkout을 포함한 회귀 테스트 추가 |
| P1 | T4, T7 | flow 형식 `defaults.run.shell`로 검증기 스크립트 실행을 바꿈 | CI 워크플로의 `defaults` 선언을 거부하는 회귀 테스트 추가 |
| P1 | T3, T4, T7 | `ci-status`의 governance 검사 셸 블록을 주석으로 바꿈 | 이름이 고정된 `Check all jobs` step의 실행 스크립트 전체를 고정하고 주석 변형 테스트 추가 |
| P1 | T4, T7 | `ci-status` job 조건을 `workflow_dispatch` 전용으로 바꿈 | job이 정확히 `if: always()`로 실행되는지 확인하는 회귀 테스트 추가 |
| P1 | T4, T7 | Dependency Submission을 SHA 검사 앞에 두고 검사 문자열은 주석에 남김 | 정확한 step 이름·순서, 검증 스크립트, `EXPECTED_SHA` 환경 변수, action SHA 고정과 실패 전파를 확인하는 회귀 테스트 추가 |
| P2 | T3, T4 | Dependabot parser contract 명령을 주석으로 바꿈 | 이름이 고정된 parser step의 실제 스크립트와 catalog 검증 step보다 먼저 실행되는지 확인하는 회귀 테스트 추가 |

모든 부정 테스트는 수정 전에 기존 검증기가 오류를 내지 않는 RED를 보였고, 수정 후 통과했다. 기존 `dependency-submission`의 checkout-head mismatch 테스트도 구체적인 검증 오류를 계속 요구한다.

## 로컬 검증

| 검사 | 결과 |
|---|---|
| `python3.13 -m unittest tests.test_catalog_workflows` | PASS — 테스트 59건, 실패/오류/건너뜀 0건 |
| `python3.13 scripts/verify-catalog-workflows.py` | PASS — catalog 워크플로 계약 |
| `ruff check scripts/verify-catalog-workflows.py tests/test_catalog_workflows.py` | PASS |
| `python3.13 -m py_compile scripts/verify-catalog-workflows.py tests/test_catalog_workflows.py` | PASS — Python 컴파일 |
| `actionlint .github/workflows/ci.yml .github/workflows/dependency-submission.yml` | PASS — 두 워크플로 lint |
| `git diff --check` | PASS |
| `ruff format --diff scripts/verify-catalog-workflows.py tests/test_catalog_workflows.py` | 기존 변경하지 않은 영역의 포맷 제안만 남음; 신규 변경 영역은 formatter diff 없음 |

Hosted CI run `39458442605`는 PR head `1a537ec...`에서 28/28 job이 성공했다. 다만 이 run은 위 여섯 리뷰 finding을 고치기 전의 head를 검증하므로 현재 로컬 후보의 CI 증거로 사용할 수 없다.

## 남은 DoD

- **로컬 코드 및 회귀 검증:** PASS.
- **새 후보 PR head의 GitHub CI:** PENDING — 수정과 이 기록을 push한 후 확인한다.
- **새 PR head의 독립 리뷰:** PENDING — push된 head를 다시 검토한다.
- **머지:** PENDING — 별도의 최신 승인 필요.
- **Dependency Submission과 hosted Dependabot 재실행:** PENDING — 승인된 merge 뒤 실행하고 alert #235 상태를 읽어 확인한다.

새 exact-head CI, 재리뷰와 merge 후 실제 보안 업데이트 결과가 확인될 때까지 Issue #1832와 alert #235를 해결된 것으로 처리하지 않는다.

## Inline follow-up: catalog 검증 스크립트의 조기 종료 우회

후속 정적 검토에서 verifier가 catalog 검증 step의 필수 명령들이 순서대로 들어 있는지만 확인하고, step의 전체 실행 본문은 고정하지 않는 점을 발견했다. `set -euo pipefail` 뒤에 `exit 0`을 추가하면 catalog 생성·검사·회귀 테스트를 건너뛰고도 검증기가 통과했다. 이는 P1 게이트 우회였다.

`test_rejects_exit_before_catalog_verification_commands` 회귀 테스트를 먼저 추가했다. 수정 전 `validate_workflows()`가 오류 없는 결과를 반환해 RED였고, 수정 후 기대한 오류가 나와 GREEN이 됐다. 검증기는 이제 catalog verification step의 전체 스크립트를 고정하며, 추가 명령이나 조기 종료를 허용하지 않는다. 전체 workflow contract 60개, verifier, Ruff, Python compile, actionlint, `git diff --check`가 로컬에서 통과했다.

이 inline follow-up은 verifier 변경 및 회귀 테스트의 T3/T4/T7 실행 계약만 검토한다. 이 patch는 Kotlin 제품 코드, 공개 API/ABI, 모듈 발행 계약을 바꾸지 않는다.

Inline verdict: PASS; P0=0, P1=0.

이 판정은 독립 리뷰가 아니다. 현재 최종 후보의 hosted exact-head CI와 새 독립 리뷰를 확인해야 merge 준비가 끝난다. 머지 승인 및 merge 후 Dependency Submission/hosted Dependabot 검증도 별도 DoD로 남아 있다.

## Writer DoD

| Gate | 상태 | 근거 |
|---|---|---|
| SPW-01 | PASS | 독자는 Projects 워크플로 검증기 유지보수자다. 한국어 리뷰의 대상은 PR #1838이며, 정확한 head/base, 독립 리뷰 결과, 회귀 테스트와 CI run을 원본 근거로 고정했다. |
| SPW-02 | PASS | 범위, 심각도, 여섯 finding, 각 대응, 검증 결과와 남은 DoD를 기록했다. |
| SPW-03 | PASS | 한국어 기술 문체 기준을 적용하고, 설명문은 구체적인 주어·동사로 정리했다. 식별자·명령·URL은 보존했다. |
| SPW-04 | PASS | 발견 사항 수와 7-Tier 분류를 독립 리뷰 결과에, 59개 테스트와 도구 결과를 현재 실행 출력에, 28/28 CI를 run `39458442605` live read-back에 대조했다. |
| SPW-05 | PASS | Markdown을 다시 읽고 링크·표·PENDING 상태를 확인했다. 아직 push 전인 새 head의 CI와 재리뷰는 미완료로 표시했다. |
| KO-01–KO-07 | PASS | 사실·식별자·불확실성을 보존하고, 문장과 표를 검토했으며, 변경 한국어 문서의 terminology audit 결과를 기록했다. |

**후속 inline-review writer DoD (2026-10-21):** SPW-01–05 PASS—검토 대상과 범위를 고정하고, 우회·근본 원인·회귀 테스트·수정·미완료 게이트를 근거와 함께 기록했다. KO-01–KO-07 PASS—수치와 식별자를 보존하고, 한국어 문장을 재검토했으며, 후속 terminology audit 결과를 확인했다. Inline verdict는 검토한 verifier patch의 로컬 근거에만 적용하고 hosted CI 및 독립 리뷰를 PENDING으로 분리했다.

## f648113 리뷰에서 찾은 세 P1 우회

독립 `code-reviewer` lane `issue1832_f648_exact_review`는 base `34c84a6115c80e3b3dfcde84be54106084259cfa`와 PR head `f648113d3e17b6bd38c8436d2bc917190ea9437a`를 비교해 **REQUEST CHANGES**를 반환했다. 판정은 P0 0건, P1 3건, 확인된 P2 0건이다. Kotlin/Java 공개 코드 변경은 없어 API/ABI finding은 없다.

| 심각도 | Tier | 재현한 우회 | 로컬 수정 |
|---|---|---|---|
| P1 | T4, T7 | parser, catalog, `ci-status`, Dependency Submission의 필수 `run` step에 `shell: bash -c 'exit 0' -- {0}`를 지정하면 verifier와 actionlint가 통과하고 step 본문을 건너뛴다 | 네 named step에서 `shell` override를 거부하고, Dependency Submission workflow의 `defaults.run.shell`도 거부 |
| P1 | T4, T7 | `catalog-governance`, `ci-status`, `dependency-submission` job에 `continue-on-error: true`를 추가해도 verifier가 통과한다 | 세 fail-closed job에서 job-level `continue-on-error`를 거부 |
| P1 | T3, T4, T7 | `ci-status`의 `RESULTS`, `CATALOG_GOVERNANCE_RESULT`, `EVENT_NAME`을 성공값으로 덮어쓰면 실패 gate와 PR merge-result 검사를 건너뛸 수 있다 | `Check all jobs`의 전체 step-level env mapping을 정확히 고정해 override, 누락, 추가 및 중복 변수를 거부 |

네 회귀 테스트를 작성한 뒤 verifier 수정 전 실행했다. 12개 변형은 모두 기존 검증을 통과해 테스트가 의도한 이유로 RED가 됐다. 수정 후에는 네 테스트가 GREEN이다. 기존 CI 검증은 명령 본문과 순서를 확인했지만 실행 shell, job 실패 전파, 실제 shell 입력을 고정하지 않았다.

## 수정 후보의 로컬 증거와 남은 게이트

f648 기반 로컬 수정 후보는 `scripts/verify-catalog-workflows.py`와 `tests/test_catalog_workflows.py`를 바꾼다. workflow YAML, Kotlin/Java 제품 코드, 공개 API/ABI, 발행 모듈은 수정하지 않았다.

| 검사 | 결과 |
|---|---|
| 네 새 회귀 테스트 | PASS — 12개 mutation 변형을 거부 |
| `python3.13 -m unittest tests.test_catalog_workflows -q` | PASS — 64 tests, failures/errors/skipped 0 |
| `python3.13 scripts/verify-catalog-workflows.py` | PASS |
| `ruff check scripts/verify-catalog-workflows.py tests/test_catalog_workflows.py` | PASS |
| `python3.13 -m py_compile scripts/verify-catalog-workflows.py tests/test_catalog_workflows.py` | PASS |
| `actionlint .github/workflows/ci.yml .github/workflows/dependency-submission.yml` | PASS |
| `git diff --check` | PASS |
| `ruff format --diff scripts/verify-catalog-workflows.py tests/test_catalog_workflows.py` | BASELINE GAP — 기존 변경 밖 구간에서만 포맷 제안이 남고, 새 회귀 테스트 구간에는 formatter diff가 없다 |

CI run `39465440177`은 f648 head에서 28/28 job 성공으로 완료됐다. 이 run은 이번 수정 전 head를 검증했으므로 새 수정의 hosted CI 증거가 아니다. 현재 회귀 수정과 이 문서 갱신은 로컬 worktree에만 있다.

- 새 수정 exact-head GitHub CI와 독립 재리뷰: PENDING — commit/push 후 재실행한다.
- PR #1838 merge: PENDING — 새 exact-head DoD 이후에도 별도의 최신 승인이 필요하다.
- Dependency Submission 및 hosted Dependabot security update 실행, alert #235 read-back: PENDING — 승인된 merge 후 진행한다.

Issue #1832와 alert #235는 merge 후 실제 dependency graph 제출 및 hosted 보안 업데이트 결과를 확인하기 전까지 해결 처리하지 않는다.

**후속 review writer DoD (2026-10-21):** SPW-01 PASS—독자는 PR #1838의 workflow verifier 유지보수자이며 head/base, lane, 범위와 판정을 고정했다. SPW-02 PASS—세 우회, 심각도, 수정, RED/GREEN, 검증과 미완료 게이트를 source 및 live run에 대조했다. SPW-03 PASS—한국어 기술 문장과 동일한 CI 용어를 유지하고 식별자·명령을 보존했다. SPW-04 PASS—독립 리뷰 출력, 테스트 출력, verifier 및 GitHub Actions 결과를 각 head에 연결했다. SPW-05 PASS—문서를 다시 읽고 PENDING 범위와 수치를 확인했다. KO-01–KO-07 PASS—사실·표·용어를 읽고 contextual terminology audit을 확인했다.

## 40533a0 exact-head 리뷰 후속

대체 독립 `code-reviewer` lane `issue1832_40533a0_code_reviewer_retry1`은 base `34c84a6115c80e3b3dfcde84be54106084259cfa`와 PR head `40533a0a1743327c13f22a513da03fffc5c2214d`를 검토해 **REQUEST CHANGES**를 반환했다(P0=0, P1=2, P2=0). 이 리뷰는 f648 head에서 발견한 세 P1 우회가 새 head에서 차단된 것도 확인했다.

| 심각도 | Tier | 재현한 우회 | 로컬 수정 후보 |
|---|---|---|---|
| P1 | T4, T7 | `.github/workflows/ci.yml`의 `Read pinned Dependabot Core commit` step에 사용자 정의 `shell`이나 추가 `run` 명령을 넣어도 verifier가 허용했다. 조작한 `commit` 출력을 후속 checkout이 사용한다. | step ID, 현재 명시된 `shell: bash`, 실행 스크립트 전체, 조건·오류 처리와 step 환경을 정확히 고정하고 mutation 회귀를 추가했다. |
| P1 | T3, T4, T7 | `catalog-governance` 및 `ci-status`의 job 환경과 CI/Dependency Submission의 workflow·job·step 환경에서 `BASH_ENV`, `PATH`, `ENV`, `GIT_*`, `PYTHONPATH`를 주입해 검증 전에 명령을 바꾸거나 우회할 수 있다. | 보호 workflow 범위의 모든 `env` mapping을 확인하고 위험 환경 키를 거부한다. 따옴표로 감싼 키와 인라인 flow mapping도 회귀 입력에 포함했다. |

처음 추가한 pin-step 4개와 environment 6개 변형은 수정 전 모두 검증기를 통과해 RED였다. 후속으로 pin-step 환경·중복 ID, `PYTHONPATH`, inline flow mapping 사례를 더해 총 14개 변형을 검증한다. 현재 로컬 후보는 66개 contract test, workflow verifier, Ruff lint, Python compile, 두 workflow의 `actionlint`, `git diff --check`를 통과했다. 새 코드 구간에는 `ruff format --diff` 제안이 없고, non-zero 출력은 기존 미변경 영역에서만 발생한다.

이 후보는 아직 commit/push하지 않았다. 따라서 위 로컬 검증은 PR head `40533a0...`의 hosted CI를 대체하지 않는다. 수정 후보를 push한 후 새 exact-head CI와 독립 리뷰를 확인한다. merge 승인, Dependency Submission, hosted Dependabot 실행과 alert #235 read-back은 계속 PENDING이다. Issue #1832와 CRITICAL alert #235를 해결된 것으로 처리하지 않는다.

**40533a0 리뷰 후속 writer DoD (2026-10-21):** SPW-01 PASS—독자·목적·base/head·review lane·소스 파일과 미확인 hosted 증거를 고정했다. SPW-02 PASS—7-Tier 심각도, 두 P1의 재현·대응, 이전 세 P1의 확인, 검증 범위와 다음 게이트를 기록했다. SPW-03 PASS—한국어 기술 자연스러움 점검을 적용하고 식별자·명령·불확실성을 보존했다. SPW-04 PASS—리뷰 결과, 소스, RED/GREEN 테스트, 로컬 검증 출력을 대조하고 이전 head의 CI를 새 후보 증거로 쓰지 않았다. SPW-05 PASS—최종 Markdown을 다시 읽고 표와 PENDING 상태를 확인했다. KO-01–KO-07 PASS—사실·용어·독자가 보는 내용을 확인하고 변경 문서별 terminology audit 결과를 검토했다.

## fe4b33b94 후속: pin step 작업 디렉터리 우회

PR base `34c84a6115c80e3b3dfcde84be54106084259cfa`와 당시 PR head `afa20f6ad1d9bcb9d2a56908a774d31f05fdfcaa`를 추가 확인했다. 메인 세션의 mutation probe에서 pin step에 `working-directory: injected`를 삽입한 변형을 verifier가 허용했다. 이 step의 `mapping_path`는 상대 경로이고 결과 commit은 이어지는 `dependabot/dependabot-core` checkout의 `ref`로 쓰인다. 따라서 PR 작성자가 같은 상대 경로를 갖는 catalog 파일을 추가하면 승인 catalog 바깥의 commit 값을 주입할 수 있는 P1이다.

회귀 변형은 수정 전 verifier를 통과했고(`errors=[]`), pin step에 `working-directory`가 있으면 거부하도록 수정한 뒤 targeted test가 통과했다. commit `fe4b33b94`에서 전체 66개 workflow contract test, `scripts/verify-catalog-workflows.py`, Ruff, Python compile, 두 workflow `actionlint`, `git diff --check`가 통과했다. `ruff format --diff`의 non-zero 출력은 이 변경 전부터 있던 미변경 구간에만 해당한다.

리뷰 lane `issue1832_afa20f6_review`는 제한 시간 안에 결과를 내지 않아 중단했다. 따라서 이 lane은 독립 판정으로 집계하지 않는다. 최종 수정 헤드의 독립 7-Tier 리뷰는 다시 수행해야 한다.

같은 exact head의 GitHub CI run `39475880048`은 `Test / IO`에서 실패했다. 로그는 Maven Central이 `reactor-test:3.8.7`, `kotlinx-benchmark-runtime:0.5.0`, `protoc:4.36.1` 다운로드에 HTTP 403을 반환한 것을 보였다. Gradle configuration-cache 오류와 coverage 생성 실패는 의존성 해석 실패에 뒤따랐다. 제품 테스트 assertion 실패 증거는 없으며, 이 원인은 PR 코드 결함으로 분류하지 않는다. 수정 commit과 문서 기록을 push한 뒤 exact-head CI를 다시 확인한다.

Issue #1832와 CRITICAL alert #235는 아직 열려 있다. PR merge, Dependency Submission, 실제 hosted Dependabot 실행과 alert 해결 read-back은 모두 남아 있다.

**fe4b33b94 review writer DoD (2026-10-21):** SPW-01 PASS—독자, PR base/head, 대상 step, 정확한 workflow·verifier·테스트 경로, CI run과 미확인 최종 헤드를 고정했다. SPW-02 PASS—P1 재현, 수정, 검증, 중단된 리뷰 lane, hosted CI 실패와 남은 gate를 기록했다. SPW-03 PASS—한국어 기술 문장, tier·상태 용어와 source identifier를 점검했다. SPW-04 PASS—mutation probe, RED/GREEN 출력, commit, GitHub job 로그와 issue/alert read-back을 대조했다. SPW-05 PASS—최종 Markdown과 표·코드·미완료 상태를 다시 읽었다. KO-01–KO-07 PASS—사실·불확실성, 용어, 문장과 독자가 보는 표면을 점검하고 contextual terminology audit을 확인했다.
