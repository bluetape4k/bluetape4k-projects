# Issue #1832 표준 카탈로그 구현 계획

> **실행 방식:** 이전에 승인된 inline 실행을 사용한다. 저장소 밖 subagent 리뷰 attestation은 주장하지 않는다.

**목표:** Projects의 표준 Gradle catalog에서 Issue #1832 대상 열 개 GAV를 Dependabot이 읽게 하고, 기존 중앙 버전 관리와 실제 dependency graph의 선택을 보존한다.

**설계:** bluetape4k-dependencies가 Projects 원본 카탈로그와 고정된 중앙 catalog를 받아 표준 gradle/libs.versions.toml을 생성하고, Projects CI가 원본·allowlist·생성물을 검증한다. repository map v2는 생성 catalog와 Projects 원본 authority catalog를 분리해 기존 v1 소비자와 호환성을 유지한다.

**기술 스택:** Python 3.11+ (tomllib), Python 3.13 CI, Gradle Wrapper 9.8.1, Kotlin Gradle Plugin 2.4.20, Kotlin language/API 2.4, Temurin JDK 21/25, GitHub Actions, pinned dependabot-core Gradle parser.

---

## 범위와 실행 기준

- Projects 저장소: bluetape4k/bluetape4k-projects, base develop, 작업 branch fix/issue-1832-dependabot-standard-catalog, 승인 기준 HEAD 34c84a6115c80e3b3dfcde84be54106084259cfa.
- 중앙 저장소: bluetape4k/bluetape4k-dependencies, base develop, 작업 branch feat/dependabot-standard-catalog, 승인 기준 HEAD e9946b18bdb0022f52cd5dc8994d859d470e7bab.
- 실행 worktree는 이미 두 저장소에 각각 존재한다. 통합 branch와 기존 worktree의 변경은 건드리지 않는다.
- 로컬 경로는 Projects `/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/fix/issue-1832-dependabot-standard-catalog`, 중앙 `/Users/debop/work/bluetape4k/bluetape4k-dependencies/.worktrees/feat/dependabot-standard-catalog`, workspace `/Users/debop/work/bluetape4k`로 고정한다. map, receipt, lock은 Git이 무시하는 Projects `$EVIDENCE_DIR` 아래에 두고 승인된 closeout까지 보존한다.
- 구현 명령에서 재사용할 변수는 다음과 같이 한 번 설정한다.

```bash
export PROJECTS_WT=/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/fix/issue-1832-dependabot-standard-catalog
export DEPENDENCIES_WT=/Users/debop/work/bluetape4k/bluetape4k-dependencies/.worktrees/feat/dependabot-standard-catalog
export WORKSPACE=/Users/debop/work/bluetape4k
export EVIDENCE_DIR="$PROJECTS_WT/.gradle/issue-1832-evidence"
export REPOSITORY_MAP="$EVIDENCE_DIR/repository-map.json"
mkdir -p "$EVIDENCE_DIR"
```
- Issue #1832는 OPEN, milestone 2.1.0, labels security/ci, assignee debop이다. 이슈는 새로 만들지 않고 기존 이슈에 연결한다.
- 같은 중앙 커밋에서 Projects PR을 검증하려면 immutable signed catalog tag가 필요하다. 중앙 PR merge와 tag 생성/push에는 별도 승인이 필요하므로 현재 승인 범위에서는 그 경계 전까지 구현, 검증, 중앙 PR 및 CI를 완료한다. Tag 승인 전에는 Projects PR과 merge 후 Dependabot 재실행이 PENDING이다.
- Dependabot 보안 업데이트의 hosted Try again 재실행은 이전 요청에서 승인됐지만, Projects 변경이 기본 branch에 반영되고 exact-SHA dependency submission이 확인된 뒤에만 실행한다. 한 번의 실행 결과로 모든 alert가 해결됐다고 추정하지 않는다.
- 현재 Projects workflow receipt는 `verify` PASS, `completion-check` incomplete 상태다. `dependabot-execution`, `dependencies-catalog-promotion`, `projects-standard-catalog-view`와 main verification이 남아 있다. 구현 중 helper가 허용한 evidence만 기록하고, tag/Projects merge/Try again 전에는 완료로 처리하지 않는다. `.bluetape` 파일은 직접 수정하지 않는다.

## 변경 파일 지도

### 중앙 bluetape4k-dependencies

- 생성: scripts/project-dependabot-catalog.py, config/projects-dependabot-catalog.toml.
- 수정: scripts/catalog_candidate.py, scripts/sync-shared-versions.py, scripts/sync-dependabot-ignores.py, scripts/run-catalog-validation.py, scripts/sync-managed-catalog.py, scripts/sync-publishing-signing-support.py, scripts/migrate-catalog-authority.py, scripts/run-issues-242-243-validation.py, scripts/verify-issues-242-243-receipt.py, scripts/verify-publication-poms.py, .github/workflows/ci.yml.
- 수정/추가 테스트: tests/test_project_dependabot_catalog.py, tests/test_catalog_candidate.py, tests/test_ci_catalog_governance.py, tests/test_sync_shared_versions.py, tests/test_sync_dependabot_ignores.py, tests/test_run_catalog_validation.py, tests/test_sync_managed_catalog.py, tests/test_sync_publishing_signing_support.py, tests/test_migrate_catalog_authority.py, tests/test_run_issues_242_243_validation.py, tests/test_verify_issues_242_243_receipt.py, tests/test_verify_publication_poms.py; parser fixture는 tests/fixtures/project-dependabot-catalog/ 아래 둔다.

### Projects

- 이동: 기존 gradle/libs.versions.toml 원본을 gradle/libs.project.versions.toml로 옮긴다.
- 생성: 새 기본 gradle/libs.versions.toml, gradle/libs.project.versions.allowlist.json.
- 수정: .github/workflows/ci.yml의 catalog-governance와 ci-status, .github/workflows/dependency-submission.yml, build.gradle.kts, AGENTS.md, CLAUDE.md. 현재 경로 설명은 이 범위에서만 갱신한다. 이전 경로를 당시 상태로 기록한 `docs/review/2026-07-09-issue-1000-kafka4-compat-review.md`, `docs/review/2026-09-08-measured-infra-implementation-review.md`와 다른 모듈의 README는 역사적·비대상 자료로 둔다.
- .github/dependabot.yml의 weekly schedule, open-pull-requests-limit: 0, security-update opt-in은 그대로 둔다.
- 모듈을 추가/이동하지 않으며 CHANGELOG.md, 공개 Kotlin API, published source set은 변경하지 않는다. 검증 중 차이가 발견되면 계획을 다시 열고 별도 계약 판단을 한다.

## 구현 작업

### 구현 시작 전 필수 검토

- [x] 수정된 design과 이 plan을 main session에서 승인된 inline 경로로 performance, stability, security, operator/Ops, developer/API, user/caller, integration 관점에서 다시 검토했다. 독립 lane attestation은 주장하지 않는다.
- [x] Projects `docs/review/2026-10-19-issue-1832-catalog-design-review.md`에 설계 리뷰 표와 P0/P1/P2/P3 결과를 한국어로 남겼다. 리뷰 문서의 writer SPW-01..05는 모두 PASS다.
- [x] design, plan, review 파일의 `SPW-01`..`SPW-05` PASS와 acceptance mapping을 확인하고 세 파일을 Lore commit 형식으로 커밋한다. 이 commit 전에는 Task 1 구현을 시작하지 않는다.

**문서 게이트 증거 (2026-10-19):** 각 자료에 SPW-01..05를 적용한다. Korean terminology audit 명령은 `node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs --series clinic-appointment --json docs/superpowers/specs/2026-10-19-issue-1832-dependabot-standard-catalog-design.md docs/superpowers/plans/2026-10-19-issue-1832-dependabot-standard-catalog-plan.md docs/review/2026-10-19-issue-1832-catalog-design-review.md`이며 결과는 `files=3`, `findings=[]`다. 전체 readback 및 최종 staged diff 검사는 Lore commit 직전에 다시 수행한다.

| 자료 | SPW-01: 목적·근거·미확인 사항 | SPW-02: 구조 | SPW-03: 한국어 | SPW-04: 추적성 | SPW-05: 최종 readback |
|---|---|---|---|---|---|
| 설계 명세 | 목표·근거·미확인 사항을 기록 | 범위·결정·수용 기준·복구 순서를 구분 | 3개 자료 terminology audit 통과 | Dependabot/Python source와 16개 기준 기록 | PASS |
| 실행 계획 | 저장소·기준 HEAD·승인 경계 기록 | 순서형 Task 1–9와 위험 표 | 3개 자료 terminology audit 통과 | 명세 기준 1–16을 Task 1–9에 연결 | PASS |
| 설계 리뷰 | source ledger와 남은 검증 기록 | 7-Tier·초기 P1·게이트 상태를 분리 | 3개 자료 terminology audit 통과 | spec/plan SHA와 local call sites 연결 | PASS |

### Task 1: 생성기 계약 테스트를 먼저 작성한다

**대상 파일:**
- 생성: 중앙 tests/test_project_dependabot_catalog.py
- 생성: 중앙 tests/fixtures/project-dependabot-catalog/
- Test: 중앙 tests/test_project_dependabot_catalog.py

- [ ] test fixture에 Projects 원본 TOML, 303개 alias allowlist 샘플, 중앙 TOML, 열 개 좌표 매핑 샘플, repository map v1/v2 샘플을 둔다.
- [ ] 열 개 GAV가 설계 표의 중앙 원천과 정확히 대응하는지 검사한다. Jackson 2/3은 version key, Netty는 netty version key, RabbitMQ/Bouncy Castle/FreeMarker/jsoup은 중앙 library alias에서 버전을 해석한다.
- [ ] 원본의 non-target 선언 내용과 상대 순서, alias 집합을 유지하는지와 생성 결과의 반복 실행 바이트가 같은지 검사한다.
- [ ] 원본 library alias의 version/version.ref, plugin/bundle, 변조 GAV, 동적/rich version, 미해결 ref, 중복 좌표, alias 정규화 충돌을 각각 거부하는 테스트를 쓴다.
- [ ] 주입한 쓰기·atomic replace·read-back 실패 후 기존 출력 SHA-256이 유지되는지 검사한다.
- [ ] receipt/lock이 모든 경로, input/output SHA-256, Projects/central full HEAD, central peeled commit, Python 버전, map bytes를 결속하고 재사용·변조 receipt를 거부하는 테스트를 쓴다.
- [ ] `test_projection_preserves_unmapped_alias_bytes_and_order`와 `test_rejects_versioned_source_alias`부터 작성한다. 첫 실행은 생성기 모듈이 아직 없어 실패해야 하며, 실패 원인이 assertion 또는 import 경로임을 확인한다.
- [ ] 실행: `python3.13 -m unittest tests.test_project_dependabot_catalog`; 예상 결과: 구현 전 실패, 구현 후 전체 PASS.

### Task 2: Projects 표준 catalog 투영 생성기를 구현한다

**대상 파일:**
- 생성: 중앙 scripts/project-dependabot-catalog.py
- 생성: 중앙 config/projects-dependabot-catalog.toml
- 수정: Task 1의 fixture/test

- [ ] tomllib import 전에 sys.version_info >= (3, 11)을 확인하고 지원 하한보다 낮으면 입력 읽기와 쓰기 전에 명확히 실패한다.
- [ ] mapping을 명시적으로 정의한다. 별칭/GAV 추측을 금지하고 central alias의 module과 resolved fixed version을 확인한다.
- [ ] 프로젝트 원본의 library alias는 전부 versionless여야 하며, generated catalog에서 version이 있는 library alias는 매핑 열 개만 허용한다.
- [ ] TOML 전체를 다시 직렬화하지 않고 non-target 원본 구간을 보존한다. 생성은 고정 경로에서만 허용하고 같은 입력에 대해 결정적 바이트를 낸다.
- [ ] generator receipt와 candidate lock을 canonical JSON으로 쓰고, 입력 HEAD·해시 재확인, 임시 파일 fsync, atomic replace, directory fsync, read-back, 복구 동작을 구현한다.
- [ ] 작업은 fixture 기반 단위 테스트가 먼저 실패한 뒤 최소 구현으로 통과하도록 한다.
- [ ] 실행: python3.13 -m unittest tests.test_project_dependabot_catalog; 예상 결과: 전체 PASS, 음성 사례는 각기 실패 처리.

### Task 3: repository map v2와 모든 consumer를 함께 갱신한다

**대상 파일:**
- 수정: 중앙 scripts/catalog_candidate.py (repository-map v2와 project 전용 map builder `build-project-map`)
- 수정: 중앙 scripts/sync-shared-versions.py, scripts/sync-dependabot-ignores.py, scripts/run-catalog-validation.py, scripts/sync-managed-catalog.py, scripts/sync-publishing-signing-support.py, scripts/migrate-catalog-authority.py, scripts/run-issues-242-243-validation.py, scripts/verify-issues-242-243-receipt.py, scripts/verify-publication-poms.py, .github/workflows/ci.yml
- 수정: 대응하는 tests/test_catalog_candidate.py, tests/test_ci_catalog_governance.py, tests/test_run_catalog_validation.py 및 변경 consumer별 단위 테스트

- [ ] repository-map v2 정규화 API에 catalog와 authority_catalog을 추가한다. v1은 authority_catalog = catalog 기본값으로 기존 의미를 유지한다.
- [ ] Projects v2 entry는 gradle/libs.versions.toml 생성 catalog와 gradle/libs.project.versions.toml authority catalog를 각각 잠근다. 다른 저장소의 경로는 기존 경로를 유지한다.
- [ ] 기존 CI inline map builder는 full repository map v2를 만들도록 갱신한다. 별도 `build-project-map` 명령은 로컬의 두 승인 worktree만 관측해 generator 전용 canonical map을 쓰며, 다른 저장소 worktree의 상태에 의존하지 않는다.
- [ ] 직접/간접 consumer 열 곳 모두 같은 loader/API를 사용하도록 수정한다. `run-catalog-validation.py`도 manifest의 repository map을 다음 consumer에 전달하므로 v2 정규화 API를 사용해야 한다. map을 직접 읽지 않는 소비 경로는 기존 tests와 call graph로 확인하고 근거를 남긴다.
- [ ] v1/v2, 파일 경로 escaping, 심볼릭 링크, repository-root mismatch, stale HEAD, lock/receipt/map/catalog SHA 불일치를 거부하는 회귀 테스트를 추가한다.
- [ ] sync-shared-versions.py에 --catalog-filename libs.project.versions.toml을 추가하고 이 선택자는 --repo bluetape4k-projects 단독 지정에서만 허용한다. 공유 버전, compatibility, adoption-gap, authority inventory가 같은 source catalog를 읽게 한다.
- [ ] 실행: `python3.13 -m unittest tests.test_catalog_candidate tests.test_ci_catalog_governance tests.test_sync_shared_versions tests.test_sync_dependabot_ignores tests.test_run_catalog_validation tests.test_sync_managed_catalog tests.test_sync_publishing_signing_support tests.test_migrate_catalog_authority tests.test_run_issues_242_243_validation tests.test_verify_issues_242_243_receipt tests.test_verify_publication_poms`.

### Task 4: pinned Dependabot parser evidence와 중앙 CI를 연결한다

**대상 파일:**
- 수정: 중앙 config/projects-dependabot-catalog.toml, scripts/project-dependabot-catalog.py
- Create/Modify: 중앙 tests/fixtures/project-dependabot-catalog/ parser fixture 및 검증 코드
- 수정: 중앙 .github/workflows/ci.yml

- [ ] dependabot-core full commit을 설정에서 고정하고 Gradle parser fixture가 generated 기본 catalog에서 열 개 GAV와 version.ref를 모두 읽는지 검사한다.
- [ ] parser receipt에 upstream commit, fixture 경로/SHA-256, generated catalog 경로/SHA-256, GAV별 alias/version을 기록하고 candidate lock과 교차 검증한다.
- [ ] 중앙에 새 generator 전용 test job을 두고 `actions/setup-python@v7`의 Python `3.11`/`3.13` matrix에서 unit suite를 실행한다.
- [ ] Python을 실행하는 central `build`, `publication-pom-contract`, `supply-chain-report-only` jobs 각각에서 첫 Python invocation 전에 `actions/setup-python@v7`로 Python 3.13을 고정하고 `python3 --version`을 기록한다. generator 전용 test job은 위의 3.11/3.13 matrix를 쓴다.
- [ ] central CI는 고정된 Projects fixture로 generator/parser/candidate lock을 검증하고 기존 v1 repository map을 계속 검증한다.
- [ ] 실행: `python3.13 -m py_compile scripts/project-dependabot-catalog.py scripts/catalog_candidate.py tests/test_project_dependabot_catalog.py`; 이어 해당 consumer 8개를 대상으로 한 기존 unittest 모음과 `python3.13 -m unittest discover -s tests -p 'test_*.py'`.

> Python 3.11은 표준 `tomllib` 사용을 위한 호환성 하한이며, 중앙 CI의 일반 Python jobs는 3.13을 쓴다. generator 전용 CI matrix는 3.11/3.13에서 하한 호환성을 확인한다.

### Task 5: Projects source, allowlist, generated catalog, Gradle 제약을 연결한다

**대상 파일:**
- 이동: Projects gradle/libs.versions.toml -> gradle/libs.project.versions.toml
- 생성: Projects gradle/libs.project.versions.allowlist.json
- 생성: Projects generated gradle/libs.versions.toml
- 수정: Projects build.gradle.kts, .github/workflows/ci.yml, AGENTS.md, CLAUDE.md

- [ ] 원본의 303개 alias/GAV에서 정렬된 allowlist를 만든다. 원본 alias는 versionless이고 plugins/bundles는 비어 있어야 한다.
- [ ] `python3.13 scripts/project-dependabot-catalog.py --projects-root "$PROJECTS_WT" --central-root "$DEPENDENCIES_WT" --write`로 투영 파일을 생성한다. 이 준비 모드는 결정적 출력을 쓰되 provenance receipt를 발행하지 않으며, receipt와 candidate lock은 Task 7의 clean committed HEAD 검증에서만 발행한다.
- [ ] 결과의 10개 versioned alias, 기존 alias 전체, 결정적 바이트, allowlist 일치를 확인한다.
- [ ] root catalog accessor baseline과 candidate를 비교한다. 기존 accessor를 모두 보존하고 예상 추가 libs.amqp.client, libs.bouncycastle.bcprov, libs.bouncycastle.bcprov.lts8on, libs.freemarker, libs.jsoup만 허용한다.
- [ ] 모든 Dokka 구성의 jsoup 요청 버전을 bt4kVersion("jsoup")으로 바꾸고, 숫자 dotted-release 비교 및 최소 보안 버전 1.23.1 조건을 검사한다.
- [ ] catalog path를 직접 읽거나 원본을 수정하는 안내를 새 gradle/libs.project.versions.toml / generated gradle/libs.versions.toml 역할에 맞게 고친다.

### Task 6: Projects CI와 Dependency Submission을 exact SHA에 결속한다

**대상 파일:**
- 수정: Projects .github/workflows/ci.yml
- 수정: Projects .github/workflows/dependency-submission.yml
- 생성: Projects `tests/test_catalog_workflows.py` (Python stdlib unittest, workflow structural contract checks; YAML semantics are also validated by actionlint)
- 생성: Projects `scripts/verify-catalog-workflows.py`

- [ ] catalog-governance가 exact immutable central ref와 peeled commit을 확인하고 generator --check, repository map v2, allowlist, parser receipt, stale generated output, authority-source sync를 검증하게 한다.
- [ ] `catalog-governance` job에서 Python 3.13을 `actions/setup-python@v7`로 고정해 generator와 contract tests를 실행한다.
- [ ] ci-status가 catalog-governance가 success일 때만 통과하도록 고치고 skipped를 성공으로 취급하지 않는다.
- [ ] Dependency Submission은 github.sha를 checkout하고 checkout HEAD와 github.sha를 비교한다. run head_sha, 제출 graph의 commit SHA를 artifact/log에 남긴다.
- [ ] concurrency key는 SHA를 포함하고 cancel-in-progress: false로 설정해 다른 SHA 실행을 취소하지 않는다.
- [ ] workflow contract 테스트는 잘못된 ref, HEAD 불일치, skipped governance, 다른 SHA의 동시 실행 취소를 각각 거부한다.
- [ ] 실행: 변경한 workflow contract 테스트, actionlint .github/workflows/ci.yml .github/workflows/dependency-submission.yml, git diff --check.

### Task 7: baseline/candidate 그래프, API/ABI, POM/GMM, toolchain을 비교한다

**대상 파일:**
- 이 비교 단계에서는 Task 3에서 갱신한 중앙 `scripts/run-catalog-validation.py`와 map 소비자 테스트를 추가 수정 없이 사용한다. `scripts/verify-publication-poms.py`의 결과와 함께 Gradle `dependencies`/`dependencyInsight` 결과를 비교한다. 필요한 차이를 입증하지 못하면 계획을 다시 열고 대상 파일과 테스트를 구체적으로 추가한다.
- 증거: 두 저장소의 baseline/candidate 명령 결과 및 receipt

- [ ] Tasks 1–6의 구현과 생성 결과를 각 브랜치에 Lore 형식으로 커밋한다. 두 worktree가 clean임을 확인한 뒤, 중앙 worktree에서 `python3.13 scripts/catalog_candidate.py build-project-map --projects-root "$PROJECTS_WT" --central-root "$DEPENDENCIES_WT" --output "$REPOSITORY_MAP"`으로 두 저장소만 묶는 canonical map을 만든다. `python3.13 scripts/project-dependabot-catalog.py --projects-root "$PROJECTS_WT" --central-root "$DEPENDENCIES_WT" --repository-map "$REPOSITORY_MAP" --evidence-dir "$EVIDENCE_DIR" --check`로 receipt/lock을 발행하고 검증한다. map, receipt, lock, parser receipt는 서로 해시 기준을 공유하므로 `$EVIDENCE_DIR`에 승인된 closeout까지 보존한다. closeout 전에 map을 삭제하면 receipt 검증이 무효가 된다.
- [ ] Projects worktree에서 `./gradlew --no-daemon help` 및 실제 `bt4kVersion`/`libs` alias caller에서 확인한 영향 모듈의 compile/test를 실행한다. 대상 모듈과 명령은 이 Task의 baseline inventory에서 구체화하고 테스트 전에 임의로 생략하지 않는다.
- [ ] baseline Projects HEAD에서 영향 모듈의 Gradle resolution과 dependencyInsight를 순차로 수집한 뒤 candidate에서 반복한다. 열 개 GAV별 configuration, requested/selected version, path를 비교한다.
- [ ] resolved graph, API/runtime classpath, published POM 및 Gradle Module Metadata를 비교한다. dependency edge/version/scope/variant/capability/constraint 차이가 있으면 실패 처리하고 별도 public-contract 승인 전에는 PR을 진행하지 않는다.
- [ ] 공개 Kotlin/Java source fixture를 candidate로 compile한다. 별도 ABI fixture는 baseline artifact로 먼저 compile한 bytecode를 candidate artifact 및 runtime classpath에서 재컴파일 없이 실행한다. 영향 모듈의 API dump 또는 javap -public -s owner/descriptor도 비교한다.
- [ ] Gradle Wrapper 9.8.1/checksum, Kotlin Gradle Plugin 2.4.20, language/API 2.4, Temurin JDK 21/25를 기록한다. Java 21 island 및 최소 dependency closure를 Java 25 기본 target과 분리해 확인한다.
- [ ] 모든 Dokka configuration에서 org.jsoup:jsoup의 최종 선택 버전이 중앙 catalog와 같음을 dependencyInsight 또는 ResolutionResult로 검사한다. 1.16.1, 1.23.0, dynamic/range/rich/qualifier/malformed/empty 요청을 음성 사례로 검증한다.
- [ ] Testcontainers와 real DB 테스트가 필요하면 module/worktree 사이에서 순차 실행한다. 모듈 추가/이동이 없음을 diff와 settings auto-registration 검사로 확인한다.

### Task 8: pre-PR 리뷰, lesson, commit, PR 및 CI를 처리한다

**대상 파일:**
- 생성: Projects `docs/lessons/2026-10-19-issue-1832-dependabot-standard-catalog.md`
- 생성: Projects `docs/review/2026-10-19-issue-1832-catalog-implementation-review.md`
- 생성: central `docs/review/2026-10-19-issue-1832-catalog-implementation-review.md`
- 수정: 두 저장소의 승인된 구현 파일만

- [ ] 중앙과 Projects의 diff를 각 저장소의 Kotlin/Python/workflow 패턴에 따라 7-Tier로 inline review한다. 보안, 정확성/lifecycle, 구조/reuse, Kotlin API/ABI, 테스트/callers, 성능, docs/CI/release를 분리해 P0/P1=0을 증명한다.
- [ ] 승인된 inline review로 7-Tier 결과를 기록하고 독립 reviewer attestation은 포함하지 않는다. GitHub human-reviewer subgate는 단일 개발자 lane 근거로 N/A 처리하되, exact-head CI·review/thread read-back·P0/P1 처리는 유지한다. 현재 model registry/runtime에서 gpt-6-luna max provenance가 확인되지 않으면 이를 독립 증명으로 표현하지 않는다.
- [ ] 표준 catalog와 원본 catalog 분리, versionless alias 보존, immutable tag 승인 순서에서 재사용할 교훈을 한국어 lesson에 기록하고 writer SPW-01..05를 통과시킨다.
- [ ] Task 7에서 검증한 candidate commit 이후 리뷰 수정이 있으면 Lore 형식의 한국어 후속 커밋으로 두 저장소를 갱신하고, 해당 저장소의 검증과 map/receipt를 다시 만든다. Tested:에는 실제 결과, Not-tested:에는 보류된 tag/Projects exact-head 범위를 기록한다.
- [ ] 사용자 요청에 포함된 중앙 PR은 bluetape4k/bluetape4k-dependencies:develop <- feat/dependabot-standard-catalog로 만든다. 한국어 본문 끝에 ## DoD Status를 두고 Issue #1832 및 현재 승인 경계를 기록한다.
- [ ] 중앙 PR의 exact remote head SHA에서 required CI, review/thread read-back을 완료한다. CI가 실패하거나 P0/P1이 생기면 고치고 해당 검증을 다시 실행한다.
- [ ] 중앙 PR merge와 signed catalog/YYYY-MM-DD-NN tag 생성/push는 이 계획의 자동 실행 범위가 아니다. fresh approval이 올 때까지 기다린다.
- [ ] 중앙 tag가 별도 승인 후 발행되면 Projects pin/mapping/lock을 그 tag의 peeled SHA로 갱신하고 full Projects 검증과 exact-head CI를 마친 뒤 bluetape4k/bluetape4k-projects:develop <- fix/issue-1832-dependabot-standard-catalog PR을 연다.
- [ ] 각 PR merge는 merge-ready DoD 이후 fresh approval을 받아 수행한다. auto-merge는 설정하지 않는다.

### Task 9: 실제 hosted Dependabot 결과와 Issue #1832를 판정한다

**선행 조건:** 승인된 Projects merge, merge SHA, exact SHA dependency-submission 성공, 서명된 중앙 tag pin 확인.

- [ ] implementation 직전, Try again 직전, 재실행 후에 Dependabot open alert 목록을 다시 읽는다. alert ID/GAV/firstPatchedVersion을 세 시점으로 비교한다.
- [ ] 각 현재 대상 alert에서 승인된 hosted Try again 경로로 실행하고 alert마다 updated, already-patched, no-update-available, not-detected, updater-error, unknown 중 하나의 증거를 보존한다.
- [ ] updated는 실제 PR diff와 resolved patched version, already-patched는 패치 graph와 fixed, no-update-available은 계속 열린 alert와 PENDING을 확인한다.
- [ ] alert 누락, open alert, updater error, dependency_not_found, 또는 상태 불명 항목이 하나라도 있으면 Issue #1832를 open으로 유지한다. 모든 대상 alert가 종료되고 final graph가 패치 버전을 선택할 때만 해결 처리한다.
- [ ] Issue를 해결한 뒤 merge, canonical sync, 보존 대상 worktree, 정리, GNO indexing을 각 워크플로우 gate에 따라 확인한다.

## 수용 기준 추적

| 명세 기준 | 계획 작업 |
|---|---|
| 1–4: 열 개 중앙 매핑, 보존·결정성, allowlist, 실패 입력 | 1–2, 5 |
| 5: repository-map v1/v2와 전체 consumer | 3 |
| 6: 서명된 중앙 태그와 모든 pin/checkout 일치 | 3–6, 8의 승인 대기 |
| 7: provenance receipt, atomic write, lock/replay 거부 | 1–2 |
| 8: central/Projects CI와 exact SHA dependency submission | 4–6 |
| 9: pinned parser, accessor baseline, 10개 alias만 versioned | 1, 4–6 |
| 10: dependencyInsight 및 version-selected path | 7 |
| 11: graph/API/ABI/POM/GMM와 baseline consumer | 7 |
| 12: Gradle/Kotlin/JDK/Java 21 closure/Dokka jsoup | 5, 7 |
| 13: 두 PR exact-head CI/review | 8; tag 승인 전 Projects 작업은 PENDING |
| 14: alert baseline/실행 결과별 판정 | 9 |
| 15: retry/hold/revert recovery | 8–9 |
| 16: Python 3.11 minimum, Python 3.13 CI | 2, 4 |

## 위험 예측과 복구

| 위험 | 징후 | 완화 / 복구 |
|---|---|---|
| 중앙 카탈로그 투영으로 Gradle 해석 또는 게시 결과가 바뀜 | `dependencyInsight`, API/runtime classpath, POM/GMM, API dump 또는 ABI fixture가 다름 | PR 진행을 멈추고 좌표와 artifact 차이를 기록한다. 작업 브랜치에서 기준 pin과 생성 파일을 복구한다. 게시 동작을 바꾸려면 별도 공개 계약 결정을 연다. |
| repository-map v2가 기존 소비자를 깨뜨림 | v1 fixture/consumer 실패 또는 v2 경로·lock SHA 불일치 | v1 호환 검사를 먼저 유지하고 소비자 하나씩 복구한다. 전체 consumer suite가 통과하기 전에는 Projects 브랜치를 갱신하지 않는다. |
| receipt 재사용 또는 경로 대체 | exact field/hash/head/peeled/map 검증 실패 또는 심볼릭 링크·비정규 경로 허용 | 출력 교체 전에 거부하고 기존 생성 바이트를 보존한다. 해당 경로를 조사한다. |
| 새 기본 catalog가 의도하지 않은 Dependabot 후보를 노출함 | 매핑되지 않은 원본 alias에 version/version.ref가 있거나 생성된 버전 지정 alias 집합이 매핑 열 개와 다름 | 생성을 거부한다. 원본 303개 alias는 모두 versionless로 유지하고 버전은 중앙 매핑에서만 바꾼다. |
| signed tag 발행 전에 Projects가 중앙 pin을 검증할 수 없음 | CI가 후보 SHA의 승인된 immutable tag를 가져오거나 검증하지 못함 | Projects PR 생성과 merge 의존 실행을 보류한다. 별도 명시 승인이 오기 전에 태그를 만들거나 push하지 않는다. |
| 구현 중 hosted alert 목록이 바뀜 | alert ID/GAV가 시작 기준과 다르거나 새 alert가 나타남 | 새 ID/GAV를 작업 ledger에 추가하고 분류한 뒤 세 시점 목록을 대조한다. Issue #1832를 해결 처리하기 전에 결과를 조정한다. |
| Python 실행 버전이 지원 범위와 다름 | 버전이 3.11 미만이거나 `tomllib`을 사용할 수 없음 | 입력을 읽거나 쓰기 전에 실패시킨다. 중앙 검증은 Python 3.13을 쓰고 CI도 3.13으로 고정한다. |

## 계획 인라인 리뷰

검토 방식: 승인된 inline 경로. 독립 reviewer attestation은 사용하지 않는다. 첫 재검토에서 확인한 P1 세 건(consumer 목록 누락, map 삭제와 receipt 보존 계약 충돌, PR 리뷰 방식의 승인 범위 불일치)을 수정하고 관련 관점을 다시 확인했다.

| 관점 | 검토 결과 | 처리 |
|---|---|---|
| Performance | generator는 303개 작은 alias 목록을 선형 검증하며 런타임 Kotlin artifact에는 코드를 추가하지 않는다. | P2 없음; 결정성·불필요 직렬화 금지 유지 |
| Stability | v1 호환, input 재해시, clean HEAD map, atomic replace/read-back, versionless 원본 검증을 단계별로 둔다. | P0=0, P1=0 |
| Security | canonical non-symlink path, 고정 GAV mapping, immutable signed tag, alert 결과별 판정이 명시돼 있다. | P0=0, P1=0 |
| Operations | exact SHA dependency submission, cancel-in-progress 비활성화, helper-only receipt, map을 포함한 evidence 보존, merge/tag/retry 승인 경계를 분리했다. | P0=0, P1=0 |
| Developer/API | 기본 `libs` alias는 유지하고 예상 신규 accessor 다섯 개만 허용한다. 게시 API/소스 변경은 별도 계약 검토 전 차단한다. | P0=0, P1=0 |
| User/caller | 중앙 map consumer 열 곳 모두 Task 3에 포함하고 v1/v2 검증, Gradle graph/accessor/POM/GMM 비교를 수용 기준에 연결했다. | P0=0, P1=0 |
| Integration | dirty worktree에서 provenance를 만들 수 없는 기존 helper 제약을 반영해 출력 생성과 clean committed HEAD receipt 검증을 순서화했다. tag 승인 전에는 Projects PR/hosted retry를 차단한다. | P0=0, P1=0 |

계획 재검토 결과: 모호했던 실행 경로와 문서 범위, CI governance 테스트 누락, 조기 map 생성 순서를 앞선 수정에 반영했다. 이번 검토에서는 map 소비자와 map 증거 보존을 구현 파일 및 closeout 순서에 명시하고, PR 검토 범위를 승인된 inline 방식으로 고쳤다. 수용 기준 1–16을 Task 1–9에 대조했다. 요청 모델은 `gpt-6-luna max`이며 실제 session provenance는 검증되지 않았으므로 독립 attestation으로 주장하지 않는다. 중앙 tag, PR merge, Projects PR/merge, hosted retry, sync, cleanup 경계는 서로 분리되어 있다.
