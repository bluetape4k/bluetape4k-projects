# Cassandra 상세 매뉴얼 실행 체크리스트

## 범위

- Projects worktree: `feature/all-module-manuals`
- Site worktree: `feature/ecosystem-atlas-manual`
- 변경 유형: Type E documentation-only
- 기준 소스: `bluetape4k-projects` `1.11.0` tag
- Production Kotlin API/runtime 변경: N/A — `docs/manual`, manifest와 site snapshot만 변경
- GitHub issue/PR/push/merge/deploy: N/A — 승인 범위는 local spec, edit, test, browser verify까지
- Chezmoi/agent guidance/workflow: N/A — 해당 surface를 변경하지 않음

## Workflow router

- [x] **WF-01 — Classify**
  - **Action:** 작업을 Type E 문서 유지보수로 분류한다.
  - **Evidence:** Kotlin production source와 dependency는 변경하지 않고 Cassandra manual, manifest, site snapshot만 변경한다.
  - **Failure:** production behavior가 필요하면 STOP하고 Type A/B/C로 재분류한다.
- [x] **WF-02 — Write the first concrete plan**
  - **Action:** 근거 수집, 구조 비교, spec/plan, bilingual 작성, snapshot/build/browser 검증 순서를 제시한다.
  - **Evidence:** 2026-07-13 active thread의 5단계 계획과 B형 chapter inventory.
  - **Failure:** STOP before durable artifacts.
- [x] **WF-03 — Obtain first-plan approval**
  - **Action:** B형 hub + focused chapters 설계의 명시적 승인을 받는다.
  - **Evidence:** 사용자 응답 `승인` (2026-07-13).
  - **Failure:** remain read-only.
- [x] **WF-04 — Load execution contracts**
  - **Action:** workflow, maintenance, writer, brainstorming, checklist/common gate와 Korean naturalness 계약을 읽는다.
  - **Evidence:** `/Users/debop/.codex/skills/{bluetape-workflow,bluetape-maintenance,bluetape-writer,brainstorming}` 및 required references를 현재 turn에서 읽음.
  - **Failure:** STOP before editing.
- [ ] **WF-05 — Execute gates in dependency order**
  - **Action:** spec approval, plan, manual source, site snapshot, verification 순으로 진행한다.
  - **Evidence:** 아래 gate별 fresh result.
  - **Failure:** leave downstream items unchecked and repair.
- [x] **WF-06 — Repair any skipped or weak gate**
  - **Action:** 누락되거나 약한 증거를 복구하고 영향을 받은 downstream proof를 다시 실행한다.
  - **Evidence:** 최초 untracked-file self-review가 trailing whitespace를 보지 못한 점을 commit output에서 발견; 공백 4개를 제거하고 `git diff --cached --check`와 `git show --check HEAD`를 다시 통과함.
  - **Failure:** final status BLOCKED.

## Common gates

- [x] **CG-01 — Re-read authority**
  - **Action:** workspace/repo `AGENTS.md`, selected skills, status와 diff를 읽는다.
  - **Evidence:** `/Users/debop/work/bluetape4k/AGENTS.md`, repo `AGENTS.md`; branch `feature/all-module-manuals`; tracked diff 없음; unrelated untracked 3개 보존.
  - **Failure:** STOP before editing.
- [x] **CG-02 — Query historical/current evidence**
  - **Action:** GNO GitHub/docs와 1.11.0 source/test/README를 대조한다.
  - **Evidence:** PR #919 session cache collision은 1.11.0에 포함; PR #986 bootstrap builder는 1.11.0 이후 변경임을 ancestry와 tag source로 확인; issue/review #809/#810, Cassandra public source/tests와 README 확인.
  - **Failure:** STOP decisions that depend on missing evidence.
- [x] **CG-03 — Protect user work and boundaries**
  - **Action:** repo/worktree/base/status와 제외 파일을 기록한다.
  - **Evidence:** HEAD `2114031e6e2acdfccaa243f58b26de54ac8a349d`; upstream 없음; 기존 untracked lesson/checklist 3개는 staging과 수정에서 제외.
  - **Failure:** preserve or BLOCK.
- [x] **CG-04 — Apply audience language policy**
  - **Action:** Korean-first manual과 English parity를 유지한다.
  - **Evidence:** spec은 내부 문서라 한국어, public manual은 KO/EN 동일 chapter inventory로 설계.
  - **Failure:** repair locale drift.
- [x] **CG-05 — Prove public contract documentation**
  - **Action:** production API 변경 여부와 durable spec/plan 경로를 고정한다.
  - **Evidence:** API change N/A; spec/checklist under `docs/superpowers`.
  - **Failure:** block undocumented public behavior.
- [x] **CG-06 — Reuse ecosystem patterns**
  - **Action:** Core/Coroutines/Logging의 multi-chapter manual pattern을 재사용한다.
  - **Evidence:** manifest v2 chapter model, bilingual parity, deterministic site snapshot 구조 확인.
  - **Failure:** stop new abstraction/dependency work.
- [ ] **CG-07 — Lock behavior and run targeted proof**
  - **Action:** Projects manual validator/tests, Cassandra test anchors, site tests/snapshot/build를 실행한다.
  - **Evidence:** Projects PASS — Ruby manual tests 35 runs/108 assertions, failures=0/errors=0; `Manuals are aligned.`; manifest snapshot current; 1.11.0 release links 4,814 checked/0 missing; `:bluetape4k-cassandra:test` `BUILD SUCCESSFUL`. Site snapshot/build/browser는 Task 7 PENDING.
  - **Failure:** repair and rerun.
- [x] **CG-08 — Serialize heavyweight checks**
  - **Action:** Testcontainers-backed Cassandra 검증을 단독 순차 실행하도록 고정한다.
  - **Evidence:** manual implementation plan에서 Cassandra targeted test를 다른 heavy check와 병렬 실행하지 않음.
  - **Failure:** discard ambiguous parallel evidence and rerun sequentially.
- [x] **CG-09 — Verify issue/PR metadata live**
  - **Action:** GitHub issue/PR mutation applicability를 분류한다.
  - **Evidence:** N/A — issue/PR 생성 또는 수정이 승인 범위에 없음.
  - **Failure:** verify live if GitHub scope is later approved.
- [x] **CG-10 — Verify PR body and reviews live**
  - **Action:** PR applicability를 분류한다.
  - **Evidence:** N/A — no PR in approved scope.
  - **Failure:** verify live before any PR/merge scope.
- [x] **CG-11 — Enforce side-effect authority**
  - **Action:** external/irreversible action 경계를 기록한다.
  - **Evidence:** local commit은 spec workflow에 포함; push/PR/merge/deploy는 수행하지 않음.
  - **Failure:** STOP at external boundary.
- [x] **CG-12 — Synchronize after merge**
  - **Action:** merge applicability를 분류한다.
  - **Evidence:** N/A — merge not requested.
  - **Failure:** sync if later merged.
- [x] **CG-13 — Update managed source first**
  - **Action:** chezmoi applicability를 분류한다.
  - **Evidence:** N/A — no managed user-scope guidance.
  - **Failure:** resolve source chain if scope changes.
- [x] **CG-14 — Audit durable Codex changes**
  - **Action:** Codex guidance applicability를 분류한다.
  - **Evidence:** N/A — no Codex surface changes.
  - **Failure:** audit if scope changes.
- [x] **CG-15 — Preserve global policy boundaries**
  - **Action:** global policy와 Claude surfaces를 제외한다.
  - **Evidence:** scoped paths are Projects/Site documentation only.
  - **Failure:** revert unauthorized changes.
- [x] **CG-16 — Use authoritative tooling safely**
  - **Action:** repo helpers, raw reads, `apply_patch`, repository validators를 사용한다.
  - **Evidence:** `repo-status`, `repo-diff`, GNO, git tag reads and selected skill references.
  - **Failure:** rerun authoritative command.
- [ ] **CG-17 — Prove completion line by line**
  - **Action:** router/common/leaf/locale/site proof와 scoped status를 재검증한다.
  - **Evidence:** final counts, unchecked list and status.
  - **Failure:** remain PENDING.

## Type E gates

- [x] **E-01 — Route support skills**
  - **Action:** public bilingual manual writing 지원 skill을 선택한다.
  - **Evidence:** `bluetape-writer`와 Korean naturalness checklist 로드; diagram은 현재 spec에서 제외되어 N/A.
  - **Failure:** STOP before editing with a missing route.
- [x] **E-02 — Discover current guidance**
  - **Action:** current manual, README, source/test, reviews와 existing manual platform을 읽는다.
  - **Evidence:** Cassandra landing, 1.11.0 README/source/test inventory, #809/#810 reviews, Core/Coroutines/Logging specs 확인.
  - **Failure:** remain read-only until authority is known.
- [x] **E-03 — Preserve behavior and ownership**
  - **Action:** production source는 건드리지 않고 canonical `docs/manual`을 먼저 수정한다.
  - **Evidence:** approved scope and planned file map; site is generated downstream.
  - **Failure:** revert or reclassify.
- [x] **E-04 — Apply and prove parity**
  - **Action:** chezmoi parity applicability를 분류한다.
  - **Evidence:** N/A — no managed user-scope files.
  - **Failure:** repair source chain if scope changes.
- [ ] **E-05 — Run maintenance verification**
  - **Action:** diff check, targeted references, manual validation, tests, site snapshot/build/browser checks를 실행한다.
  - **Evidence:** fresh verification ledger.
  - **Failure:** repair before completion.
- [ ] **E-06 — Close out durable delivery**
  - **Action:** locale parity, source links, final diff/commits와 checklist totals를 확인한다.
  - **Evidence:** final commit/status and counts; push remains N/A unless separately approved.
  - **Failure:** final status PENDING/BLOCKED.

## Cassandra manual leaf gates

- [x] **CAS-01 — Pin the release evidence**
  - **Action:** 기술 사실을 `1.11.0` source, tests, README와 merged fix history에 고정한다.
  - **Evidence:** release commit `6187173b58e8b4c5c435c145e00e94708f31ef75`; PR #919은 포함, PR #986은 post-release limitation 근거; issue/review #809/#810.
  - **Failure:** remove or qualify unsupported claims.
- [x] **CAS-02 — Approve the chapter architecture**
  - **Action:** one-page, hub+chapters, recipes-first 구조를 비교하고 B를 선택한다.
  - **Evidence:** 사용자 `승인`; landing + 5 focused chapters, 총 6개 페이지.
  - **Failure:** revise design before spec.
- [x] **CAS-03 — Commit and review the written spec**
  - **Action:** approved design을 spec으로 작성하고 self-review 후 commit한다.
  - **Evidence:** `docs/superpowers/specs/2026-07-13-cassandra-detailed-manual-design.md`; commit `0216925cd014428cd4b4724a18e9d2a5250f14a4`; placeholders=0, focused chapters=5, 1.11.0/post-release boundary explicit, commit diff-check clean.
  - **Failure:** repair spec and recommit.
- [x] **CAS-04 — Write the implementation plan**
  - **Action:** user spec review 후 `writing-plans`로 file-level implementation plan을 작성한다.
  - **Evidence:** 사용자 written spec 승인; `docs/superpowers/plans/2026-07-13-cassandra-detailed-manual.md`; 7 tasks, 5 chapter IDs, release boundary, validation commands와 execution handoff를 self-review함.
  - **Failure:** do not edit manual content.
- [x] **CAS-05 — Write the Korean source manual**
  - **Action:** landing과 5개 chapter를 자연스러운 한국어와 complete examples로 작성한다.
  - **Evidence:** KO chapter inventory 5개(`session-lifecycle`, `coroutine-queries`, `rows-data-mapping`, `statements-query-builder`, `operations-testing`); 1.11.0 source/test claim review와 Korean naturalness 금칙어 검색 0건; 각 Task의 spec/quality review Critical/Important/Minor=0.
  - **Failure:** repair unsupported or translated prose.
- [x] **CAS-06 — Produce English parity**
  - **Action:** Korean source와 같은 inventory/contract를 영어로 제공한다.
  - **Evidence:** KO/EN basename 5개 일치, 각 chapter의 frontmatter/manualId/chapterId와 heading/code/source-link 구조 검수 통과; bilingual spec/quality reviews 승인.
  - **Failure:** repair locale drift.
- [x] **CAS-07 — Register and validate the manifest**
  - **Action:** 5개 bilingual chapter를 manifest에 등록하고 source links를 검증한다.
  - **Evidence:** `docs/manual/manifest.yaml`과 generated snapshot에 5개 chapter 등록; `Manuals are aligned.`; `Manual manifest snapshot is current.`; release validator `4,814 checked, 0 missing`; Ruby tests 35 runs/108 assertions, failures=0/errors=0.
  - **Failure:** repair inventory/reference.
- [ ] **CAS-08 — Publish the deterministic site snapshot**
  - **Action:** committed Projects source 기준으로 site snapshot을 갱신한다.
  - **Evidence:** sync/check digest, release provenance and stale cleanup proof.
  - **Failure:** repair snapshot.
- [ ] **CAS-09 — Verify tests, build and browser routes**
  - **Action:** relevant Cassandra tests, Projects docs checks, site tests/build와 KO/EN browser routes를 검증한다.
  - **Evidence:** Projects PASS — `:bluetape4k-cassandra:test` `BUILD SUCCESSFUL`; docs validators/tests PASS. Site tests/build와 KO/EN browser routes는 Task 7 PENDING.
  - **Failure:** repair and rerun.

## 종료 집계

현재 상태는 `Required checks: 25/32; N/A: 6; Blocked: 0`이다. 미완료 항목은 `WF-05`, `CG-07`, `CG-17`, `E-05`, `E-06`, `CAS-08`, `CAS-09`이며, Site snapshot/build/browser 검증에 맞춰 즉시 갱신한다.
