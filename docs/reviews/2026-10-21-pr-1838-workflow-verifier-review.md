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

## Writer DoD

| Gate | 상태 | 근거 |
|---|---|---|
| SPW-01 | PASS | 독자는 Projects 워크플로 검증기 유지보수자다. 한국어 리뷰의 대상은 PR #1838이며, 정확한 head/base, 독립 리뷰 결과, 회귀 테스트와 CI run을 원본 근거로 고정했다. |
| SPW-02 | PASS | 범위, 심각도, 여섯 finding, 각 대응, 검증 결과와 남은 DoD를 기록했다. |
| SPW-03 | PASS | 한국어 기술 문체 기준을 적용하고, 설명문은 구체적인 주어·동사로 정리했다. 식별자·명령·URL은 보존했다. |
| SPW-04 | PASS | 발견 사항 수와 7-Tier 분류를 독립 리뷰 결과에, 59개 테스트와 도구 결과를 현재 실행 출력에, 28/28 CI를 run `39458442605` live read-back에 대조했다. |
| SPW-05 | PASS | Markdown을 다시 읽고 링크·표·PENDING 상태를 확인했다. 아직 push 전인 새 head의 CI와 재리뷰는 미완료로 표시했다. |
| KO-01–KO-07 | PASS | 사실·식별자·불확실성을 보존하고, 문장과 표를 검토했으며, 변경 한국어 문서의 terminology audit 결과를 기록했다. |
