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
    - **조치:** repo `AGENTS.md`, workflow/writer/maintenance/diagram skill, status와 diff를 읽는다.
    - **증거:** `AGENTS.md`; worktree clean; selected skill/reference reads completed 2026-07-12.
    - **실패 시:** STOP before editing.
- [x] **CG-02 — Query historical/current evidence**
    - **조치:** GNO와 현재 source/test/README/blog를 검색한다.
    - **증거:** GNO에 logging manual 선행 기록 없음; current Kotlin source/tests와 bilingual README/blog가 결정 근거.
    - **실패 시:** STOP decisions that depend on missing evidence.
- [x] **CG-03 — Protect user work and boundaries**
    - **조치:** repo/worktree/branch/status를 확인한다.
    - **증거:** Projects `feature/all-module-manuals`, staged/unstaged/untracked/conflicted 모두 0; upstream 없음.
    - **실패 시:** preserve or BLOCK.
- [x] **CG-04 — Apply audience language policy**
    - **조치:** Korean chat, bilingual public manual/blog parity를 유지한다.
    - **증거:** planned KO/EN landing/chapter and KO/EN blog pair.
    - **실패 시:** repair locale drift.
- [x] **CG-05 — Prove public contract documentation**
    - **조치:** Kotlin API 변경 여부와 durable plan/spec 경로를 고정한다.
    - **증거:** runtime change N/A; design/plan under `docs/superpowers`.
    - **실패 시:** block undocumented behavior.
- [x] **CG-06 — Reuse ecosystem patterns**
    - **조치:** Core/Coroutines chapter/asset/manifest pattern과 logging source/tests를 재사용한다.
    -
  **증거:** schema v2 manual inventory, existing logging README diagrams, source classes and representative tests mapped.
    - **실패 시:** stop new abstraction/dependency work.
- [x] **CG-07 — Lock behavior and run targeted proof**
    - **조치:** manual validator/tests, logging tests, site tests/build를 실행한다.
    -
  **증거:** Ruby 14/41 + 2/7 PASS; validator aligned; Gradle rerun 51 passing, 20 tasks executed, BUILD SUCCESSFUL.
    - **실패 시:** repair and rerun.
- [x] **CG-08 — Serialize heavyweight checks**
    - **조치:** heavyweight integration check applicability를 분류한다.
    - **증거:** N/A — docs-only; logging unit tests contain no Testcontainers/DB/native dependency.
    - **실패 시:** rerun sequentially if scope changes.
- [x] **CG-09 — Verify issue/PR metadata live**
    - **조치:** GitHub metadata applicability를 분류한다.
    - **증거:** N/A — no issue/PR mutation requested.
    - **실패 시:** verify live if GitHub scope is later approved.
- [x] **CG-10 — Verify PR body and reviews live**
    - **조치:** PR applicability를 분류한다.
    - **증거:** N/A — no PR exists in approved scope.
    - **실패 시:** verify live before merge scope.
- [x] **CG-11 — Enforce side-effect authority**
    - **조치:** external/irreversible action boundary를 기록한다.
    - **증거:** no push/PR/merge/deploy; local commits only follow existing approved worktree flow.
    - **실패 시:** STOP at external boundary.
- [x] **CG-12 — Synchronize after merge**
    - **조치:** merge applicability를 분류한다.
    - **증거:** N/A — merge not requested.
    - **실패 시:** sync if later merged.
- [x] **CG-13 — Update managed source first**
    - **조치:** chezmoi applicability를 분류한다.
    - **증거:** N/A — no managed user-scope guidance.
    - **실패 시:** resolve source chain if scope changes.
- [x] **CG-14 — Audit durable Codex changes**
    - **조치:** Codex guidance applicability를 분류한다.
    - **증거:** N/A — no Codex surfaces changed.
    - **실패 시:** audit if scope changes.
- [x] **CG-15 — Preserve global policy boundaries**
    - **조치:** global policy and Claude surfaces를 제외한다.
    - **증거:** scoped file map contains only Projects/Site documentation.
    - **실패 시:** revert unauthorized changes.
- [x] **CG-16 — Use authoritative tooling safely**
    - **조치:** raw reads, repo helpers, apply_patch, repo scripts를 사용한다.
    - **증거:** repo-status/repo-diff/rg/sed and selected skill references read.
    - **실패 시:** rerun authoritative command.
- [x] **CG-17 — Prove completion line by line**
    - **조치:** 모든 leaf/diagram/site proof와 clean diff/status를 재검증한다.
    -
  **증거:** all logging leaf gates closed; Projects validators/tests and Site snapshot/tests/build/browser evidence recorded below; final clean status is verified after commits.
    - **실패 시:** remain PENDING.

## Logging manual leaf gate

- [x] **LOG-01 — Pin source-backed chapter model**
    - **조치:** public source, representative tests, README와 blog를 대조한다.
    -
  **증거:** six chapter topics map to KLogging/KotlinLogging, Slf4jExtensions, MdcSupport, MdcSupportCoroutines, KLoggingChannel, operations/tests.
    - **실패 시:** remove unsupported claims.
- [x] **LOG-02 — Write bilingual chapter inventory**
    - **조치:** landing과 6개 KO/EN chapter를 작성한다.
    - **증거:** 12 chapter files; chapter IDs/order/source links validated in schema v2.
    - **실패 시:** repair parity.
- [x] **LOG-03 — Create canonical diagrams**
    - **조치:** source-backed SVG/PNG 3쌍을 manual assets에 둔다.
    - **증거:** 3 SVG/PNG pairs rendered with CairoSVG scale 2 and inspected at full size; ledger below.
    - **실패 시:** return to asset loop.
- [x] **LOG-04 — Register and validate manifest**
    - **조치:** chapters/assets inventory를 schema v2 manifest에 추가한다.
    -
  **증거:** 6 chapters + 6 assets registered; 14 validator tests/41 assertions and 2 export tests/7 assertions PASS; `Manuals are aligned.`
    - **실패 시:** repair inventory/reference.
- [x] **LOG-05 — Publish deterministic site snapshot**
    - **조치:** Projects commit 기준으로 site sync/check를 실행한다.
    -
  **증거:** deterministic sync/check passed at the closing Projects revision; snapshot inventory reports 90 modules, 224 localized documents, and 28 assets.
    - **실패 시:** repair snapshot.
- [x] **LOG-06 — Align derived blog**
    - **조치:** bilingual Part 2 blog의 logging section을 manual route로 연결한다.
    -
  **증거:** KO/EN Part 2 articles state that the repository manual is the source of truth and link the logging landing plus five decision chapters; representative KO links resolved in the browser.
    - **실패 시:** repair ownership drift.
- [x] **LOG-07 — Browser and build proof**
    - **조치:** KO/EN landing/chapter/assets/blog links와 Astro build를 검증한다.
    -
  **증거:** Astro check reports 0 errors/warnings/hints; Astro build emits 373 pages; KO/EN landing, scoped MDC, async channel, blog links, and all three SVG assets rendered with no browser console warnings/errors.
    - **실패 시:** repair and rerun.

## 종료 집계

## Diagram evidence ledger

| Asset                    | Kind/source                                                                                    | XML/render                         | Audits                                                                                         | PNG inspection                                                                                                 |
|--------------------------|------------------------------------------------------------------------------------------------|------------------------------------|------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| `logger-api-map`         | Architecture; KLogging/KotlinLogging/Slf4jExtensions/MDC source                                | `xmllint` PASS; CairoSVG 3200×1960 | markers=1, connectors=4, intrusions=0, crossings=0, geometry/endpoints/mixed-corner failures=0 | text readable, role cards aligned, provider boundary and arrows clear                                          |
| `mdc-scope-lifecycle`    | Architecture; MdcSupport + tests                                                               | `xmllint` PASS; CairoSVG 3200×1960 | markers=1, connectors=2, intrusions=0, crossings=0, geometry/endpoints/mixed-corner failures=0 | outer/inner/restore sequence readable, no clipping or crowding                                                 |
| `async-channel-sequence` | Sequence; KLoggingChannel + tests; wiki best-practice and existing logging sequence references | `xmllint` PASS; CairoSVG 3600×2240 | markers=3, connectors=8, crossings=0, sequence-style PASS, numbered labels=7                   | participants/lifelines/activations/close frame/post-close drop readable; marker color and direction consistent |

Text-only technology cards make infrastructure icons N/A. No separate review page exists; canonical files are linked directly from current manual chapters.

`Required checks: 26/26; N/A: 8; Blocked: 0` — logging manual, deterministic site snapshot, derived blog links, build, and browser proof complete.
