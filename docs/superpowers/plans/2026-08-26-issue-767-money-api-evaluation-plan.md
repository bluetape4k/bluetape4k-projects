# Issue #767 Money API 평가 정리 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** owned Money API 구현을 보류한다는 승인 결정을 재현 가능한 평가 기록으로 남기고, Epic #1423과 child #767의 진행 상태를 현재 증거에 맞게 정리한 docs PR을 `develop`에 전달한다.

**Architecture:** production code와 dependency는 변경하지 않는다. 승인된 설계 명세를 단일 source of truth로 삼아 한국어 평가 기록을 작성하고, GitHub 이슈에는 결정 요약과 문서 링크만 반영한다. #767은 `2.0.0` milestone으로 이동하되 OPEN 상태와 native parent 관계를 유지하며, PR은 `docs/767-money-api-evaluation` head에서 `develop` base로 하나만 만든다.

**Tech Stack:** Markdown, Git, GitHub CLI, Gradle 9.7.0, Kotlin/JVM 25 baseline, bluetape-flow Type A receipt

---

## 1. 고정 범위와 실행 경계

- repository: `bluetape4k/bluetape4k-projects`
- base: `develop`
- head: `docs/767-money-api-evaluation`
- worktree: `.worktrees/docs/767-money-api-evaluation`
- 승인된 설계: `docs/superpowers/specs/2026-08-26-issue-767-money-api-evaluation-design.md`
- 생성할 평가 기록: `docs/superpowers/research/2026-08-26-issue-767-money-api-evaluation.md`
- 수정할 GitHub 대상: Epic #1423 본문, child #767 milestone
- 생성할 PR: head `docs/767-money-api-evaluation`, base `develop`, #767 연결
- 금지 범위: `utils/money/**`, `io/protobuf/**`, Gradle dependency, public API, deprecation, issue close, merge, auto-merge, branch/worktree 삭제
- 별도 승인 게이트: 이 계획 승인 후 GitHub metadata update와 PR 생성 가능. merge는 exact-head 재검증 후 fresh approval가 필요하다.

## 2. 파일 구조

| 경로 | 책임 |
| --- | --- |
| `docs/superpowers/specs/2026-08-26-issue-767-money-api-evaluation-design.md` | 승인된 대안, G1-G5, 미래 API/adapter/rollback 경계를 고정한다. 실행 중 의미를 변경하지 않는다. |
| `docs/superpowers/research/2026-08-26-issue-767-money-api-evaluation.md` | 현재 source와 consumer 증거, baseline, 최종 보류 결정, 재개 조건을 사용자·유지보수자에게 전달한다. |
| `.omx/epic-1423-767/issue-1423-body.md` | Epic #1423에 적용할 정확한 임시 본문. Git에 포함하지 않는다. |
| `.omx/epic-1423-767/issue-767-body.md` | #767에 적용할 정확한 임시 본문. Git에 포함하지 않는다. |
| `.omx/epic-1423-767/*.json` | live read-back과 Type A receipt 증거. Git에 포함하지 않는다. |

## Task 1: 실행 직전 live state와 exact base 재검증

**Files:**
- Read: `docs/superpowers/specs/2026-08-26-issue-767-money-api-evaluation-design.md`
- Read: `.bluetape/runs/20260826T124545Z-7bc806f9/receipt.jsonl`
- Evidence: `.omx/epic-1423-767/preflight.json`

- [ ] **Step 1: branch와 worktree 범위를 확인한다**

Run:

```bash
git status --short --branch
git rev-parse HEAD
git merge-base HEAD origin/develop
git diff --name-only origin/develop...HEAD
```

Expected: branch는 `docs/767-money-api-evaluation`, tracked diff는 승인된 spec/plan/research 문서뿐이며 production 경로가 없다.

- [ ] **Step 2: Type A receipt와 승인된 head를 결합한다**

Run:

```bash
(cd /Users/debop/work/bluetape4k/bluetape4k-projects && python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py resume-check --run-id 20260826T124545Z-7bc806f9)
git show --no-patch --format='%H%n%s' b9b62e0c87
```

Expected: run은 `running`, owner epoch는 `1`, topology gap은 0이고 latest checksum을 이후 mutation의 `--expected-head`로 사용한다. `lanes_requiring_observation`이 비어 있지 않으면 canonical repository root에서 `liveness-check --run-id 20260826T124545Z-7bc806f9 --lane-id epic-1423-767-design --at <현재 UTC> --command-status alive`를 먼저 실행하고, 반환된 bounded lifecycle command로 lane을 정상화한 뒤 `resume-check`가 mutation 가능 상태임을 다시 확인한다. `b9b62e0c87`은 승인된 설계 commit이며 현재 branch의 ancestor다. plan 승인 시각·사용자 승인 문구·현재 plan blob SHA-256을 `.omx/epic-1423-767/plan-approval-evidence.json`에 기록한다. terminal `complete`는 Task 6의 모든 check와 main verification 뒤에만 허용한다.

- [ ] **Step 3: remote base와 PR 중복을 확인한다**

Run:

```bash
git fetch origin develop
git ls-remote origin refs/heads/develop refs/heads/docs/767-money-api-evaluation
gh pr list --repo bluetape4k/bluetape4k-projects --state all --head docs/767-money-api-evaluation --json number,state,title,headRefName,baseRefName,url
```

Expected: `origin/develop` exact SHA가 확인되고 동일 head PR은 없다. base가 진전했으면 uncommitted 변경이 없는지 확인한 뒤 rebase하고 `git range-diff`로 문서 commit 의도를 검증한다.

- [ ] **Step 4: 두 issue와 native child 관계를 live로 다시 읽는다**

Run:

```bash
gh issue view 1423 --repo bluetape4k/bluetape4k-projects --json number,title,state,body,labels,assignees,milestone,url
gh issue view 767 --repo bluetape4k/bluetape4k-projects --json number,title,state,body,labels,assignees,milestone,url
gh api graphql -f query='query { repository(owner:"bluetape4k", name:"bluetape4k-projects") { issue(number:1423) { subIssues(first:20) { nodes { number state title } } } } }'
```

Expected: #1423과 #767은 OPEN, #1423 milestone은 `2.0.0`, #767 milestone은 변경 전 `backlog`, assignee `debop`, #767은 #1423의 native child다. 예상과 다르면 mutation 전에 계획과 live state를 재조정한다.

- [ ] **Step 5: baseline 증거를 보존한다**

Run:

```bash
./gradlew :bluetape4k-money:test :bluetape4k-protobuf:test --no-configuration-cache --console=plain
```

Expected: `257 tests completed`, 실패 0, `BUILD SUCCESSFUL`. 환경 또는 upstream 실패이면 exact error와 failed task를 기록하고 문서-only 판단과 runtime proof를 구분하며, GitHub metadata mutation·push·PR 생성으로 진행하지 않고 `PENDING`으로 중단한다.

## Task 2: 한국어 평가 기록 작성

**Files:**
- Create: `docs/superpowers/research/2026-08-26-issue-767-money-api-evaluation.md`
- Read: `docs/superpowers/specs/2026-08-26-issue-767-money-api-evaluation-design.md`

- [ ] **Step 1: 문서의 결정과 audience를 고정한다**

평가 기록 첫 문단은 다음 결정을 그대로 전달한다.

```markdown
## 결정

**owned Money API 구현을 보류하고, 현재 JSR-354/Moneta public API를 유지한다.**

현재 저장소에는 서로 독립된 production consumer가 없고, semantic contract,
source/binary compatibility, persistence/serialization, 성능·provider 운영 근거가
모두 충족되지 않았다. 따라서 이번 변경은 production code, dependency, public API,
deprecation을 추가하지 않는다.
```

- [ ] **Step 2: source/consumer evidence 표를 작성한다**

표에는 다음 행을 정확히 포함한다.

- `utils/money/build.gradle.kts`: JSR-354와 Moneta가 `api`로 export됨.
- `utils/money/src/main/kotlin/io/bluetape4k/money/MoneySupport.kt`: Moneta `Money` public factory/return type.
- `utils/money/src/main/kotlin/io/bluetape4k/money/FastMoneySupport.kt`: Moneta `FastMoney`, `Number`, minor-unit/fraction-digit public factory.
- `utils/money/src/main/kotlin/io/bluetape4k/money/MoneyAmountSupport.kt`: JSR-354 `MonetaryAmount` arithmetic/rounding/conversion extension.
- `utils/money/src/main/kotlin/io/bluetape4k/money/CurrencyConverter.kt`: `ECB`, `IMF` provider coupling.
- `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/MoneySupport.kt`: production source bridge이지만 `io/protobuf/build.gradle.kts`의 Moneta dependency가 `compileOnly`이고 독립 business consumer가 아님.
- `exposed-workshop/06-advanced/05-exposed-money/src/test/kotlin/exposed/examples/money/Ex02_Money.kt`와 `MoneyData.kt`: JDBC test source.
- `exposed-r2dbc-workshop/06-advanced/05-exposed-r2dbc-money/src/test/kotlin/exposed/r2dbc/examples/money/Ex01_MoneyDefaults.kt`, `Ex02_Money.kt`, `MoneyData.kt`: R2DBC test source.
- dedicated ABI fixture와 Money benchmark: 없음.

각 행은 repository-relative source path와 판정을 함께 적고, 독립 production consumer 수를 `0`으로 명시한다.

- [ ] **Step 3: G1-G5 판정과 재개 조건을 복제한다**

다음 판정을 표로 적는다.

| Gate | 현재 판정 |
| --- | --- |
| G1 independent consumers | FAIL — 0개 |
| G2 semantic contract | FAIL |
| G3 compatibility/migration | FAIL |
| G4 adapter/persistence | PARTIAL |
| G5 performance/stability | FAIL |

재개는 G1부터 G5까지 모두 PASS이고 별도 implementation issue/spec/plan이 승인된 경우에만 허용한다. 미래 후보 API, Protobuf exact fixture, persistence matrix, JMH 조건, provider timeout/cache/stale/telemetry 계약은 설계 명세 링크로 연결하되 현재 구현된 것처럼 표현하지 않는다.

- [ ] **Step 4: baseline과 검증 한계를 기록한다**

`Money`와 `Protobuf` 합산 `257` test PASS, 여섯 spec lens `P0=0/P1=0`, terminology audit와 `git diff --check` 결과를 기록한다. 별도 ABI fixture, downstream migration, 실제 provider network, owned API benchmark는 `미검증`으로 분리한다.

- [ ] **Step 5: DoD와 후속 조건을 체크리스트로 끝낸다**

완료 항목은 live issue/source inventory, 대안 비교, 보류 결정, G1-G5, baseline, review/writer gate다. 미완료 항목은 독립 consumer 2곳, ABI/serialization/persistence suite, JMH/provider soak, production implementation이며 “모든 gate 통과 전 생성하지 않음”을 명시한다.

## Task 3: 문서 writer gate와 commit

**Files:**
- Modify: `docs/superpowers/research/2026-08-26-issue-767-money-api-evaluation.md`
- Verify: `docs/superpowers/specs/2026-08-26-issue-767-money-api-evaluation-design.md`
- Verify: `docs/superpowers/plans/2026-08-26-issue-767-money-api-evaluation-plan.md`

- [ ] **Step 1: spec-to-research coverage를 검증한다**

설계의 source ledger, 대안, G1-G5, 호환성, adapter 방향, provider 경계, migration/rollback, review verdict가 평가 기록에서 누락되거나 구현 완료로 오해되지 않는지 문단별로 대조한다.

- [ ] **Step 2: 미완성 표식과 stale token을 검사한다**

Run:

```bash
rg -n 'T[B]D|T[O]DO|F[I]XME|place[holder]|1\.1[3]\.0|구현 완료' docs/superpowers/specs/2026-08-26-issue-767-money-api-evaluation-design.md docs/superpowers/plans/2026-08-26-issue-767-money-api-evaluation-plan.md docs/superpowers/research/2026-08-26-issue-767-money-api-evaluation.md
```

Expected: 계획의 금지어 설명 외에 findings 0. `1.13.0`은 stale milestone이므로 어떤 산출물에도 남지 않는다.

- [ ] **Step 3: Korean writer gate와 Markdown read-back을 실행한다**

Run:

```bash
node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs --series clinic-appointment docs/superpowers/specs/2026-08-26-issue-767-money-api-evaluation-design.md docs/superpowers/plans/2026-08-26-issue-767-money-api-evaluation-plan.md docs/superpowers/research/2026-08-26-issue-767-money-api-evaluation.md
```

세 파일의 표·목록·code fence를 처음부터 끝까지 읽는다. Expected: terminology findings 0, 끊긴 표/목록/code fence 0.

- [ ] **Step 4: diff 품질을 검사한다**

Run:

```bash
git diff --check
git diff --stat origin/develop...HEAD
git diff --name-only origin/develop...HEAD
```

Expected: whitespace error 0, production/build/dependency 파일 0.

- [ ] **Step 5: 평가 기록을 Lore commit으로 기록한다**

Run:

```bash
git add docs/superpowers/research/2026-08-26-issue-767-money-api-evaluation.md docs/superpowers/plans/2026-08-26-issue-767-money-api-evaluation-plan.md
git commit -m 'Money API 보류 결정을 Epic의 재평가 계약으로 고정한다' \
  -m 'Constraint: 독립 production consumer가 0개이고 현재 public API가 JSR-354와 Moneta를 노출한다.
Rejected: consumer proof 없는 additive facade 또는 즉시 owned type 전환 | source/binary/dependency 위험을 검증할 근거가 없다
Confidence: high
Scope-risk: narrow
Directive: G1부터 G5까지 모두 통과하기 전에는 production Money API를 구현하지 않는다.
Tested: Money와 Protobuf baseline 257개, 여섯 plan review lens, Korean terminology audit, git diff --check
Not-tested: owned API production 구현, downstream migration, 실제 provider network'
```

Commit decision record:

- intent: consumer 근거가 생길 때까지 Money API 전환을 보류하고 재개 조건을 공개한다.
- Constraint: 독립 production consumer 0개와 현재 JSR-354/Moneta public surface.
- Rejected: evidence 없는 additive facade 또는 즉시 owned type 전환.
- Confidence: high.
- Scope-risk: narrow.
- Directive: G1-G5가 모두 PASS하기 전 production 구현을 시작하지 않는다.
- Tested: 257 baseline tests, six-lens review, writer audit, diff check.
- Not-tested: owned API, downstream migration, provider network.

Expected: spec commit 뒤에 plan/research commit 하나가 추가되고 working tree는 clean이다.

## Task 4: Epic #1423과 child #767 진행 상태 정리

**Files:**
- Create: `.omx/epic-1423-767/issue-1423-body.md`
- Create: `.omx/epic-1423-767/issue-767-body.md`
- Create: `.omx/epic-1423-767/issue-1423-preimage-body.md`
- Create: `.omx/epic-1423-767/issue-767-preimage-body.md`
- Create: `.omx/epic-1423-767/issue-preimage-metadata.json`
- Evidence: `.omx/epic-1423-767/github-readback.json`

- [ ] **Step 1: #1423 본문을 정확한 진행 상태로 준비한다**

본문은 목적과 native child 목록을 유지하면서 다음 상태를 포함한다.

```markdown
## Child issues
- [x] #1070 — 재사용 가능한 Event Sourcing/projection primitive 평가: 공통 추출 보류, application-local 유지 (#1509)
- [x] #1320 — TenantContext bridge 평가: consumer proof 전 공통 승격 보류 (#1510)
- [ ] #767 — owned Money API 평가: G1-G5 미충족으로 구현 보류, 증거 gate 유지

## 진행 결정
세 child는 incidental linear stack이 아니라 독립적으로 검증 가능한 architecture evidence train이다. #1070과 #1320의 평가는 완료되었고, #767은 public API를 변경하지 않은 채 재개 조건을 고정한다.

## DoD Status
- 상태: 2/3 평가 완료, #767 결정 기록 전달 중
- 현재 결정: JSR-354/Moneta public API 유지, owned Money API 구현 보류
- 다음 게이트: #767 G1 independent production consumer 2곳 확보
- 전체 재개 조건: G1-G5 모두 PASS하고 별도 Type A implementation spec/plan 승인
```

Epic milestone은 `2.0.0`, assignee와 기존 labels는 그대로 둔다. 완료된 두 child의 OPEN/CLOSED 표시는 live state를 따르되 평가 완료 근거인 merged PR #1509/#1510을 명시한다.

- [ ] **Step 2: #767 본문에 승인된 결정 블록을 추가한다**

기존 맥락·제안·수용 기준을 삭제하지 않고 맨 위에 다음 블록을 추가한다.

```markdown
## 2026-08-26 평가 결정

owned Money API 구현을 보류하고 현재 JSR-354/Moneta public API를 유지한다.
독립 production consumer가 0개이고 G1-G5가 모두 통과하지 않았으므로 production code,
dependency, deprecation은 변경하지 않는다.

- 설계: `docs/superpowers/specs/2026-08-26-issue-767-money-api-evaluation-design.md`
- 평가: `docs/superpowers/research/2026-08-26-issue-767-money-api-evaluation.md`
- 재개 조건: 독립 consumer 2곳, semantic/ABI/adapter/performance/provider gate 모두 PASS
- 상태: OPEN 유지
```

- [ ] **Step 3: mutation 직전 metadata 기준 데이터와 approval scope를 확인한다**

`gh issue view` 결과를 읽고 두 기존 body를 preimage 파일에 `apply_patch`로 그대로 저장한다. 같은 시점의 state, milestone, labels, assignee, native parent, body SHA-256을 `issue-preimage-metadata.json`의 `{ "issues": { "1423": {...}, "767": { "milestone": "backlog", ... } } }` 구조로 기록한다.

Run:

```bash
gh issue view 1423 --repo bluetape4k/bluetape4k-projects --json state,body,labels,assignees,milestone
gh issue view 767 --repo bluetape4k/bluetape4k-projects --json state,body,labels,assignees,milestone
shasum -a 256 .omx/epic-1423-767/issue-1423-preimage-body.md .omx/epic-1423-767/issue-767-preimage-body.md
```

Expected: 이 계획 승인 이후이고, 두 issue 모두 OPEN이며 native parent 관계가 유지된다. body가 계획 작성 뒤 바뀌었으면 최신 내용을 보존해 patch를 재생성한다. 각 mutation 직전에 live body digest를 다시 계산해 preimage digest와 다르면 중단한다.

- [ ] **Step 4: 두 issue를 제한적으로 갱신한다**

Run:

```bash
set -euo pipefail
expected_1423_digest="$(shasum -a 256 .omx/epic-1423-767/issue-1423-preimage-body.md | awk '{print $1}')"
live_1423_digest="$(gh issue view 1423 --repo bluetape4k/bluetape4k-projects --json body --jq .body | shasum -a 256 | awk '{print $1}')"
expected_767_digest="$(shasum -a 256 .omx/epic-1423-767/issue-767-preimage-body.md | awk '{print $1}')"
live_767_digest="$(gh issue view 767 --repo bluetape4k/bluetape4k-projects --json body --jq .body | shasum -a 256 | awk '{print $1}')"
pre_767_milestone="$(jq -r '.issues["767"].milestone' .omx/epic-1423-767/issue-preimage-metadata.json)"
if [[ "$expected_1423_digest" != "$live_1423_digest" || "$expected_767_digest" != "$live_767_digest" ]]; then
  exit 1
fi
gh issue edit 1423 --repo bluetape4k/bluetape4k-projects --body-file .omx/epic-1423-767/issue-1423-body.md
updated_1423_digest="$(shasum -a 256 .omx/epic-1423-767/issue-1423-body.md | awk '{print $1}')"
readback_1423_digest="$(gh issue view 1423 --repo bluetape4k/bluetape4k-projects --json body --jq .body | shasum -a 256 | awk '{print $1}')"
if [[ "$updated_1423_digest" != "$readback_1423_digest" ]] || ! gh issue view 1423 --repo bluetape4k/bluetape4k-projects --json state,labels,assignees,milestone --jq '(.state == "OPEN") and (.milestone.title == "2.0.0") and ([.assignees[].login] == ["debop"]) and ([.labels[].name] | sort == ["design","enhancement","epic","management","tech-debt"])'; then
  gh issue edit 1423 --repo bluetape4k/bluetape4k-projects --body-file .omx/epic-1423-767/issue-1423-preimage-body.md || exit 1
  exit 1
fi

live_767_digest="$(gh issue view 767 --repo bluetape4k/bluetape4k-projects --json body --jq .body | shasum -a 256 | awk '{print $1}')"
live_767_milestone="$(gh issue view 767 --repo bluetape4k/bluetape4k-projects --json milestone --jq .milestone.title)"
if [[ "$expected_767_digest" != "$live_767_digest" || "$pre_767_milestone" != "$live_767_milestone" ]]; then
  gh issue edit 1423 --repo bluetape4k/bluetape4k-projects --body-file .omx/epic-1423-767/issue-1423-preimage-body.md || exit 1
  exit 1
fi
if ! gh issue edit 767 --repo bluetape4k/bluetape4k-projects --body-file .omx/epic-1423-767/issue-767-body.md --milestone '2.0.0'; then
  gh issue edit 767 --repo bluetape4k/bluetape4k-projects --body-file .omx/epic-1423-767/issue-767-preimage-body.md --milestone "$pre_767_milestone" || exit 1
  gh issue edit 1423 --repo bluetape4k/bluetape4k-projects --body-file .omx/epic-1423-767/issue-1423-preimage-body.md || exit 1
  exit 1
fi
updated_767_digest="$(shasum -a 256 .omx/epic-1423-767/issue-767-body.md | awk '{print $1}')"
readback_767_digest="$(gh issue view 767 --repo bluetape4k/bluetape4k-projects --json body --jq .body | shasum -a 256 | awk '{print $1}')"
if [[ "$updated_767_digest" != "$readback_767_digest" ]] || ! gh issue view 767 --repo bluetape4k/bluetape4k-projects --json state,labels,assignees,milestone --jq '(.state == "OPEN") and (.milestone.title == "2.0.0") and ([.assignees[].login] == ["debop"]) and ([.labels[].name] | sort == ["enhancement","refactor","tech-debt"])'; then
  gh issue edit 767 --repo bluetape4k/bluetape4k-projects --body-file .omx/epic-1423-767/issue-767-preimage-body.md --milestone "$pre_767_milestone" || exit 1
  gh issue edit 1423 --repo bluetape4k/bluetape4k-projects --body-file .omx/epic-1423-767/issue-1423-preimage-body.md || exit 1
  exit 1
fi
```

Expected: body와 #767 milestone만 변경된다. labels, assignee, state, native parent는 변경하지 않는다. 첫 read-back이 예상과 다르면 #767을 변경하지 않고 #1423 preimage를 복원한다. 최종 read-back이 다르면 두 issue를 preimage body와 이전 milestone으로 복원하고 `BLOCKED` evidence를 남긴다.

- [ ] **Step 5: mutation을 live read-back한다**

Run:

```bash
gh issue view 1423 --repo bluetape4k/bluetape4k-projects --json number,state,body,labels,assignees,milestone,url
gh issue view 767 --repo bluetape4k/bluetape4k-projects --json number,state,body,labels,assignees,milestone,url
gh api graphql -f query='query { repository(owner:"bluetape4k", name:"bluetape4k-projects") { issue(number:1423) { subIssues(first:20) { nodes { number state title } } } } }'
```

Expected: #1423/#767 OPEN, milestone `2.0.0`, assignee `debop`, 기존 labels 보존, children `1070,1320,767`, stale `1.13.0` 없음. 결과를 `metadata-readback`과 `native-linkage` receipt check로 기록한다.

## Task 5: branch push와 docs PR 생성

**Files:**
- Read: `.github/pull_request_template.md` 또는 repository PR template
- Evidence: `.omx/epic-1423-767/pr-body.md`

- [ ] **Step 1: push 직전 exact head와 범위를 고정한다**

Run:

```bash
git status --short --branch
git rev-parse HEAD
git log --oneline --decorate origin/develop..HEAD
git diff --name-status origin/develop...HEAD
git diff --check origin/develop...HEAD
```

Expected: clean, docs 파일 3개만 변경, production 경로 0, Lore commit 2개.

- [ ] **Step 2: semantic branch를 push한다**

Run:

```bash
git push -u origin docs/767-money-api-evaluation
```

Expected: remote head SHA가 local `git rev-parse HEAD`와 일치한다.

- [ ] **Step 3: 한국어 PR 본문을 준비한다**

PR title:

```text
[money] owned API 전환을 consumer evidence까지 보류한다
```

Task 1 Step 5의 fresh baseline이 PASS인 경우에만 PR body를 준비한다. PR body는 요약, 결정 이유, 검증, 영향/비영향, `Relates to #767`, merge가 별도 승인임을 포함하고 반드시 다음 절로 끝낸다.

```markdown
## DoD Status

- [x] 독립 production consumer 0개와 현재 JSR-354/Moneta 결합을 확인했다.
- [x] 대안과 G1-G5 재개 조건을 설계·평가 문서에 기록했다.
- [x] Money/Protobuf baseline 257개와 문서 검증을 통과했다.
- [x] production API, dependency, deprecation을 변경하지 않았다.
- [ ] merge — exact-head 검증 후 fresh approval 필요.
```

- [ ] **Step 4: `develop` 대상 PR을 하나만 생성한다**

Run:

```bash
set -euo pipefail
pr_count="$(gh pr list --repo bluetape4k/bluetape4k-projects --state all --head docs/767-money-api-evaluation --json number --jq 'length')"
if [[ ! "$pr_count" =~ ^[0-9]+$ || "$pr_count" -ne 0 ]]; then
  exit 1
fi
gh pr create --repo bluetape4k/bluetape4k-projects --base develop --head docs/767-money-api-evaluation --title '[money] owned API 전환을 consumer evidence까지 보류한다' --body-file .omx/epic-1423-767/pr-body.md
```

Expected: 생성 직전 중복 조회가 0건일 때만 진행한다. OPEN PR 1개, base `develop`, head `docs/767-money-api-evaluation`, `Relates to #767`. `Closes #767`은 사용하지 않는다.

## Task 6: exact-head CI·review·DoD 검증

**Files:**
- Evidence: `.omx/epic-1423-767/pr-readback.json`
- Evidence: `.omx/epic-1423-767/ci-readback.json`

- [ ] **Step 1: PR metadata와 exact head를 읽는다**

Run:

```bash
pr_number="$(gh pr view docs/767-money-api-evaluation --repo bluetape4k/bluetape4k-projects --json number --jq .number)"
gh pr view "$pr_number" --repo bluetape4k/bluetape4k-projects --json number,state,title,body,baseRefName,headRefName,headRefOid,mergeable,mergeStateStatus,reviewDecision,statusCheckRollup,url
git ls-remote origin refs/heads/docs/767-money-api-evaluation
```

Expected: remote SHA와 `headRefOid`와 local HEAD가 동일하고 PR은 OPEN이다.

- [ ] **Step 2: terminal check conclusion을 확인한다**

Run:

```bash
set -uo pipefail
pr_number="$(gh pr view docs/767-money-api-evaluation --repo bluetape4k/bluetape4k-projects --json number --jq .number)"
for attempt in {1..40}; do
  if perl -e 'alarm shift; exec @ARGV' 30 gh pr checks "$pr_number" --repo bluetape4k/bluetape4k-projects; then
    check_exit=0
  else
    check_exit=$?
  fi
  [[ $check_exit -ne 8 ]] && break
  sleep 15
done
[[ $check_exit -eq 8 ]] && exit 1
if [[ $check_exit -ne 0 ]]; then
  exit 1
fi
head_sha="$(git rev-parse HEAD)"
pr_head_sha="$(gh pr view "$pr_number" --repo bluetape4k/bluetape4k-projects --json headRefOid --jq .headRefOid)"
remote_head_sha="$(git ls-remote origin refs/heads/docs/767-money-api-evaluation | awk '{print $1}')"
if [[ "$pr_head_sha" != "$head_sha" || "$remote_head_sha" != "$head_sha" ]]; then
  exit 1
fi
run_count="$(gh run list --repo bluetape4k/bluetape4k-projects --commit "$head_sha" --limit 20 --json databaseId --jq 'length')"
if [[ ! "$run_count" =~ ^[0-9]+$ || "$run_count" -eq 0 ]]; then
  exit 1
fi
gh run list --repo bluetape4k/bluetape4k-projects --commit "$head_sha" --limit 20 --json databaseId,headSha,status,conclusion,workflowName,url
gh run list --repo bluetape4k/bluetape4k-projects --commit "$head_sha" --limit 20 --json databaseId --jq '.[].databaseId' | while read -r workflow_run_id; do
  gh run view "$workflow_run_id" --repo bluetape4k/bluetape4k-projects --json headSha,status,conclusion,jobs,url
done
```

Expected: 최대 10분 안에 exact `headRefOid`의 required jobs가 terminal `success`. 10분이 지나도 pending이면 `PENDING`으로 중단한다. exact-head run이 0개이면 fail-closed하고, skipped/path-filtered job은 실행 증거로 과장하지 않고 별도 표기한다.

- [ ] **Step 3: 독립 review와 thread 상태를 확인한다**

문서 scope, 설계-평가 일치, 링크/명령 정확성, Epic/child metadata, PR DoD를 독립 review한다. GitHub review와 unresolved thread를 다음 명령으로 읽는다.

```bash
gh api graphql -f owner=bluetape4k -f name=bluetape4k-projects -F number="$pr_number" -f query='query($owner:String!, $name:String!, $number:Int!) { repository(owner:$owner, name:$name) { pullRequest(number:$number) { reviewThreads(first:100) { nodes { isResolved isOutdated comments(first:20) { nodes { author { login } body url } } } } } } }'
```

P0/P1 또는 unresolved blocking thread가 있으면 수정 후 새 exact head로 Steps 1-3을 반복한다.

- [ ] **Step 4: Type A receipt를 현재 상태까지만 완결한다**

owner `codex-root`가 `/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py`를 사용한다. 각 check input은 `{ "component_id", "check_id", "passed", "reason" }`, evidence는 `[{ "kind", "summary" }]` JSON으로 `.omx/epic-1423-767/`에 `apply_patch`로 작성한다. `github-epic-progress`의 `metadata-readback`, `native-linkage`와 `pr-delivery`의 `exact-head`, `pr-metadata`, `ci-review`를 현재 receipt checksum의 `--expected-head`와 함께 순차 기록한다. lane completion과 main verification은 모든 필수 check가 PASS인 뒤에만 수행한다.

- [ ] **Step 5: merge 승인 게이트에서 중단한다**

보고에는 plan item status, commit SHA, PR URL, exact head, CI/review 결과, #1423/#767 read-back, 변경 파일, known gaps를 포함한다. merge, issue close, branch/worktree cleanup은 실행하지 않고 fresh approval를 요청한다.

## 3. 계획 독립 검토 통합

| 관점 | 최초 핵심 지적 | 반영 결과 | 최종 P0/P1 | 판정 |
| --- | --- | --- | --- | --- |
| 성능·CI | 무기한 wait, exact-head run 0건 처리 부재 | 10분 상한, 호출당 30초 상한, local/remote/PR SHA와 run count fail-closed | 0/0 | PASS |
| 보안·GitHub | preimage·부분 실패 rollback·PR race 차단 부재 | digest/milestone drift guard, 양 issue 보상 rollback, 생성 직전 중복 count guard | 0/0 | PASS |
| 개발자/API | source shorthand, plan 자체 미완성 표식 검사 누락 | exact repository/source-set path와 세 문서 scan으로 보강 | 0/0 | PASS |
| 안정성·재현성 | receipt/head/plan 승인 binding 부재 | run id, owner epoch, checksum, spec commit, plan blob SHA-256 결합 | 0/0 | PASS |
| 운영·rollback | #1423 성공 뒤 #767 실패 시 복구 계약 부재 | immutable preimage, 단계별 read-back, 이전 milestone 복구, rollback failure fail-closed | 0/0 | PASS |
| 사용자·호출자 | baseline 실패여도 PR DoD가 PASS로 보일 수 있음 | fresh baseline 실패 시 metadata/push/PR을 `PENDING`으로 차단하고 G1-G5 전체 재개 조건 반복 | 0/0 | PASS |

최종 재검토는 실제 갱신 파일을 다시 읽어 수행했으며 여섯 관점 모두 `P0=0`, `P1=0`이다.

## 4. Self-review checklist

- [x] Spec coverage: source ledger, 대안 A, G1-G5, future boundary, compatibility, provider/rollback, DoD가 Tasks 1-6에 매핑된다.
- [x] 미완성 표식 검사: 세 문서의 미완성 표식은 0건이고 PR/run 번호는 live CLI 조회 결과에 binding된다.
- [x] Type consistency: repo/base/head, issue 번호, file path, gate 이름, receipt component/check ID가 전 구간에서 동일하다.
- [x] Scope: production code/dependency/API 변경 0, issue close/merge/cleanup 0.
- [x] Approval: GitHub metadata와 PR 생성은 이 계획 승인 후, merge는 별도 fresh approval 후에만 실행한다.

## 5. 완료 정의

- [ ] 승인된 보류 결정이 spec과 research 문서에 일관되게 남는다.
- [ ] #1423은 #1070/#1320 완료 결정과 #767의 남은 gate를 정확히 표시한다.
- [ ] #767은 `2.0.0`, OPEN, `debop`, 기존 labels, native parent #1423을 유지한다.
- [ ] docs-only PR이 `docs/767-money-api-evaluation`에서 `develop`로 생성된다.
- [ ] local/remote/PR exact head가 같고 terminal CI와 review evidence가 있다.
- [ ] PR body가 `## DoD Status`로 끝난다.
- [ ] production API/dependency/deprecation, merge, close, cleanup은 수행되지 않는다.
