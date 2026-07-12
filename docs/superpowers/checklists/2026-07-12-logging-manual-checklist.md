# Logging Manual 실행 체크리스트

## 범위

- Projects worktree: `feature/all-module-manuals`
- Site worktree: `feature/ecosystem-atlas-manual`
- 변경 유형: Type E documentation-only
- Production Kotlin API/runtime 변경: N/A — `docs/manual`, manifest, site snapshot과 blog link만 변경
- GitHub issue/PR/push/merge: N/A — 사용자 승인 범위가 local edit/test/verify까지임
- Chezmoi/agent guidance/workflow: N/A — 해당 surface를 변경하지 않음

## 공통 gate

- [x] **CG-01 — Re-read authority**
  - **Action:** repo `AGENTS.md`, workflow/writer/maintenance/diagram skill, status와 diff를 읽는다.
  - **Evidence:** `AGENTS.md`; worktree clean; selected skill/reference reads completed 2026-07-12.
  - **Failure:** STOP before editing.
- [x] **CG-02 — Query historical/current evidence**
  - **Action:** GNO와 현재 source/test/README/blog를 검색한다.
  - **Evidence:** GNO에 logging manual 선행 기록 없음; current Kotlin source/tests와 bilingual README/blog가 결정 근거.
  - **Failure:** STOP decisions that depend on missing evidence.
- [x] **CG-03 — Protect user work and boundaries**
  - **Action:** repo/worktree/branch/status를 확인한다.
  - **Evidence:** Projects `feature/all-module-manuals`, staged/unstaged/untracked/conflicted 모두 0; upstream 없음.
  - **Failure:** preserve or BLOCK.
- [x] **CG-04 — Apply audience language policy**
  - **Action:** Korean chat, bilingual public manual/blog parity를 유지한다.
  - **Evidence:** planned KO/EN landing/chapter and KO/EN blog pair.
  - **Failure:** repair locale drift.
- [x] **CG-05 — Prove public contract documentation**
  - **Action:** Kotlin API 변경 여부와 durable plan/spec 경로를 고정한다.
  - **Evidence:** runtime change N/A; design/plan under `docs/superpowers`.
  - **Failure:** block undocumented behavior.
- [x] **CG-06 — Reuse ecosystem patterns**
  - **Action:** Core/Coroutines chapter/asset/manifest pattern과 logging source/tests를 재사용한다.
  - **Evidence:** schema v2 manual inventory, existing logging README diagrams, source classes and representative tests mapped.
  - **Failure:** stop new abstraction/dependency work.
- [x] **CG-07 — Lock behavior and run targeted proof**
  - **Action:** manual validator/tests, logging tests, site tests/build를 실행한다.
  - **Evidence:** Ruby 14/41 + 2/7 PASS; validator aligned; Gradle rerun 51 passing, 20 tasks executed, BUILD SUCCESSFUL.
  - **Failure:** repair and rerun.
- [x] **CG-08 — Serialize heavyweight checks**
  - **Action:** heavyweight integration check applicability를 분류한다.
  - **Evidence:** N/A — docs-only; logging unit tests contain no Testcontainers/DB/native dependency.
  - **Failure:** rerun sequentially if scope changes.
- [x] **CG-09 — Verify issue/PR metadata live**
  - **Action:** GitHub metadata applicability를 분류한다.
  - **Evidence:** N/A — no issue/PR mutation requested.
  - **Failure:** verify live if GitHub scope is later approved.
- [x] **CG-10 — Verify PR body and reviews live**
  - **Action:** PR applicability를 분류한다.
  - **Evidence:** N/A — no PR exists in approved scope.
  - **Failure:** verify live before merge scope.
- [x] **CG-11 — Enforce side-effect authority**
  - **Action:** external/irreversible action boundary를 기록한다.
  - **Evidence:** no push/PR/merge/deploy; local commits only follow existing approved worktree flow.
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
  - **Evidence:** N/A — no Codex surfaces changed.
  - **Failure:** audit if scope changes.
- [x] **CG-15 — Preserve global policy boundaries**
  - **Action:** global policy and Claude surfaces를 제외한다.
  - **Evidence:** scoped file map contains only Projects/Site documentation.
  - **Failure:** revert unauthorized changes.
- [x] **CG-16 — Use authoritative tooling safely**
  - **Action:** raw reads, repo helpers, apply_patch, repo scripts를 사용한다.
  - **Evidence:** repo-status/repo-diff/rg/sed and selected skill references read.
  - **Failure:** rerun authoritative command.
- [ ] **CG-17 — Prove completion line by line**
  - **Action:** 모든 leaf/diagram/site proof와 clean diff/status를 재검증한다.
  - **Evidence:** final checked/total ledger.
  - **Failure:** remain PENDING.

## Logging manual leaf gate

- [x] **LOG-01 — Pin source-backed chapter model**
  - **Action:** public source, representative tests, README와 blog를 대조한다.
  - **Evidence:** six chapter topics map to KLogging/KotlinLogging, Slf4jExtensions, MdcSupport, MdcSupportCoroutines, KLoggingChannel, operations/tests.
  - **Failure:** remove unsupported claims.
- [x] **LOG-02 — Write bilingual chapter inventory**
  - **Action:** landing과 6개 KO/EN chapter를 작성한다.
  - **Evidence:** 12 chapter files; chapter IDs/order/source links validated in schema v2.
  - **Failure:** repair parity.
- [x] **LOG-03 — Create canonical diagrams**
  - **Action:** source-backed SVG/PNG 3쌍을 manual assets에 둔다.
  - **Evidence:** 3 SVG/PNG pairs rendered with CairoSVG scale 2 and inspected at full size; ledger below.
  - **Failure:** return to asset loop.
- [x] **LOG-04 — Register and validate manifest**
  - **Action:** chapters/assets inventory를 schema v2 manifest에 추가한다.
  - **Evidence:** 6 chapters + 6 assets registered; 14 validator tests/41 assertions and 2 export tests/7 assertions PASS; `Manuals are aligned.`
  - **Failure:** repair inventory/reference.
- [ ] **LOG-05 — Publish deterministic site snapshot**
  - **Action:** Projects commit 기준으로 site sync/check를 실행한다.
  - **Evidence:** source revision, document/asset counts, digest check.
  - **Failure:** repair snapshot.
- [ ] **LOG-06 — Align derived blog**
  - **Action:** bilingual Part 2 blog의 logging section을 manual route로 연결한다.
  - **Evidence:** KO/EN links resolve; blog remains narrative, manual remains technical source.
  - **Failure:** repair ownership drift.
- [ ] **LOG-07 — Browser and build proof**
  - **Action:** KO/EN landing/chapter/assets/blog links와 Astro build를 검증한다.
  - **Evidence:** HTTP/browser console plus build summary.
  - **Failure:** repair and rerun.

## 종료 집계

## Diagram evidence ledger

| Asset | Kind/source | XML/render | Audits | PNG inspection |
| --- | --- | --- | --- | --- |
| `logger-api-map` | Architecture; KLogging/KotlinLogging/Slf4jExtensions/MDC source | `xmllint` PASS; CairoSVG 3200×1960 | markers=1, connectors=4, intrusions=0, crossings=0, geometry/endpoints/mixed-corner failures=0 | text readable, role cards aligned, provider boundary and arrows clear |
| `mdc-scope-lifecycle` | Architecture; MdcSupport + tests | `xmllint` PASS; CairoSVG 3200×1960 | markers=1, connectors=2, intrusions=0, crossings=0, geometry/endpoints/mixed-corner failures=0 | outer/inner/restore sequence readable, no clipping or crowding |
| `async-channel-sequence` | Sequence; KLoggingChannel + tests; wiki best-practice and existing logging sequence references | `xmllint` PASS; CairoSVG 3600×2240 | markers=3, connectors=8, crossings=0, sequence-style PASS, numbered labels=7 | participants/lifelines/activations/close frame/post-close drop readable; marker color and direction consistent |

Text-only technology cards make infrastructure icons N/A. No separate review page exists; canonical files are linked directly from current manual chapters.

`Required checks: 21/26; N/A: 8; Blocked: 0` — site sync/browser/final proof pending.
