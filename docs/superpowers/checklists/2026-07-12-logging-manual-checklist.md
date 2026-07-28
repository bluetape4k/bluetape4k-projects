# Logging manual 실행 체크리스트

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
    - **실패 시:** 수정 전에 중단한다.
- [x] **CG-02 — Query historical/current evidence**
    - **조치:** GNO와 현재 source/test/README/blog를 검색한다.
    - **증거:** GNO에 logging manual 선행 기록 없음; current Kotlin source/tests와 bilingual README/blog가 결정 근거.
    - **실패 시:** 누락된 증거에 의존하는 결정을 중단한다.
- [x] **CG-03 — Protect user work and boundaries**
    - **조치:** repo/worktree/branch/status를 확인한다.
    - **증거:** Projects `feature/all-module-manuals`, staged/unstaged/untracked/conflicted 모두 0; upstream 없음.
    - **실패 시:** 보존하거나 BLOCKED로 둔다.
- [x] **CG-04 — Apply audience language policy**
    - **조치:** Korean chat, bilingual public manual/blog parity를 유지한다.
    - **증거:** 계획된 KO/EN landing/chapter와 KO/EN blog pair.
    - **실패 시:** locale drift를 수정한다.
- [x] **CG-05 — Prove public contract documentation**
    - **조치:** Kotlin API 변경 여부와 durable plan/spec 경로를 고정한다.
    - **증거:** runtime change N/A; design/plan under `docs/superpowers`.
    - **실패 시:** 문서화되지 않은 동작을 blocked로 둔다.
- [x] **CG-06 — Reuse ecosystem patterns**
    - **조치:** Core/Coroutines chapter/asset/manifest pattern과 logging source/tests를 재사용한다.
    -
  **증거:** schema v2 manual inventory, 기존 logging README diagram, source class, 대표 test를 mapping했다.
    - **실패 시:** 새 abstraction/dependency 작업을 중단한다.
- [x] **CG-07 — Lock behavior and run targeted proof**
    - **조치:** manual validator/tests, logging tests, site tests/build를 실행한다.
    -
  **증거:** Ruby 14/41 + 2/7 PASS; validator aligned; Gradle rerun 51 passing, 20 tasks executed, BUILD SUCCESSFUL.
    - **실패 시:** 수정하고 다시 실행한다.
- [x] **CG-08 — Serialize heavyweight checks**
    - **조치:** heavyweight integration check applicability를 분류한다.
    - **증거:** N/A — docs-only이며 logging unit test에는 Testcontainers/DB/native dependency가 없다.
    - **실패 시:** scope가 바뀌면 순차 재실행한다.
- [x] **CG-09 — Verify issue/PR metadata live**
    - **조치:** GitHub metadata applicability를 분류한다.
    - **증거:** N/A — issue/PR mutation은 요청되지 않았다.
    - **실패 시:** 나중에 GitHub scope가 승인되면 live로 검증한다.
- [x] **CG-10 — Verify PR body and reviews live**
    - **조치:** PR applicability를 분류한다.
    - **증거:** N/A — 승인 범위에 PR이 없다.
    - **실패 시:** merge scope 전에 live로 검증한다.
- [x] **CG-11 — Enforce side-effect authority**
    - **조치:** external/irreversible action boundary를 기록한다.
    - **증거:** push/PR/merge/deploy 없음; local commit만 기존 승인된 worktree flow를 따른다.
    - **실패 시:** external boundary에서 중단한다.
- [x] **CG-12 — Synchronize after merge**
    - **조치:** merge applicability를 분류한다.
    - **증거:** N/A — merge는 요청되지 않았다.
    - **실패 시:** 나중에 merge되면 sync한다.
- [x] **CG-13 — Update managed source first**
    - **조치:** chezmoi applicability를 분류한다.
    - **증거:** N/A — managed user-scope guidance가 없다.
    - **실패 시:** scope가 바뀌면 source chain을 해결한다.
- [x] **CG-14 — Audit durable Codex changes**
    - **조치:** Codex guidance applicability를 분류한다.
    - **증거:** N/A — Codex surface 변경이 없다.
    - **실패 시:** scope가 바뀌면 audit한다.
- [x] **CG-15 — Preserve global policy boundaries**
    - **조치:** global policy and Claude surfaces를 제외한다.
    - **증거:** scoped file map contains only Projects/Site documentation.
    - **실패 시:** revert unauthorized changes.
- [x] **CG-16 — Use authoritative tooling safely**
    - **조치:** raw reads, repo helpers, apply_patch, repo scripts를 사용한다.
    - **증거:** repo-status/repo-diff/rg/sed and selected skill references read.
    - **실패 시:** authoritative command를 다시 실행한다.
- [x] **CG-17 — Prove completion line by line**
    - **조치:** 모든 leaf/diagram/site proof와 clean diff/status를 재검증한다.
    -
  **증거:** 모든 logging leaf gate가 닫혔고, Projects validator/test 및 Site snapshot/test/build/browser evidence가 아래에 기록되었다. Commit 뒤 최종 clean status를 확인했다.
    - **실패 시:** PENDING으로 남긴다.

## Logging manual leaf 게이트

- [x] **LOG-01 — Pin source-backed chapter model**
    - **조치:** public source, representative tests, README와 blog를 대조한다.
    -
  **증거:** 여섯 chapter topic이 KLogging/KotlinLogging, Slf4jExtensions, MdcSupport, MdcSupportCoroutines, KLoggingChannel, operations/tests에 대응된다.
    - **실패 시:** 지원되지 않는 claim을 제거한다.
- [x] **LOG-02 — Write bilingual chapter inventory**
    - **조치:** landing과 6개 KO/EN chapter를 작성한다.
    - **증거:** 12개 chapter file; chapter ID/order/source link는 schema v2에서 검증했다.
    - **실패 시:** parity를 수정한다.
- [x] **LOG-03 — Create canonical diagrams**
    - **조치:** source-backed SVG/PNG 3쌍을 manual assets에 둔다.
    - **증거:** 3개 SVG/PNG pair를 CairoSVG scale 2로 render하고 full size로 검사했다. 아래 ledger에 기록한다.
    - **실패 시:** asset loop로 돌아간다.
- [x] **LOG-04 — Register and validate manifest**
    - **조치:** chapters/assets inventory를 schema v2 manifest에 추가한다.
    -
  **증거:** 6개 chapter와 6개 asset을 등록했다. 14 validator tests/41 assertions와 2 export tests/7 assertions PASS; `Manuals are aligned.`
    - **실패 시:** repair inventory/reference.
- [x] **LOG-05 — Publish deterministic site snapshot**
    - **조치:** Projects commit 기준으로 site sync/check를 실행한다.
    -
  **증거:** closing Projects revision에서 deterministic sync/check가 통과했다. Snapshot inventory는 90 modules, 224 localized documents, 28 assets를 보고했다.
    - **실패 시:** snapshot을 수정한다.
- [x] **LOG-06 — Align derived blog**
    - **조치:** bilingual Part 2 blog의 logging section을 manual route로 연결한다.
    -
  **증거:** KO/EN Part 2 article은 repository manual이 source of truth라고 명시하고 logging landing 및 다섯 decision chapter를 link한다. 대표 KO link는 browser에서 resolve되었다.
    - **실패 시:** ownership drift를 수정한다.
- [x] **LOG-07 — Browser and build proof**
    - **조치:** KO/EN landing/chapter/assets/blog links와 Astro build를 검증한다.
    -
  **증거:** Astro check는 0 errors/warnings/hints를 보고했고, Astro build는 373 pages를 생성했다. KO/EN landing, scoped MDC, async channel, blog links, 세 SVG asset은 browser console warning/error 없이 render되었다.
    - **실패 시:** 수정하고 다시 실행한다.

## 종료 집계

## Diagram 증거 원장

| Asset | Kind/source | XML/render | Audits | PNG inspection |
|--------------------------|------------------------------------------------------------------------------------------------|------------------------------------|------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| `logger-api-map`         | Architecture; KLogging/KotlinLogging/Slf4jExtensions/MDC source                                | `xmllint` PASS; CairoSVG 3200×1960 | markers=1, connectors=4, intrusions=0, crossings=0, geometry/endpoints/mixed-corner failures=0 | text readable, role cards aligned, provider boundary and arrows clear                                          |
| `mdc-scope-lifecycle`    | Architecture; MdcSupport + tests                                                               | `xmllint` PASS; CairoSVG 3200×1960 | markers=1, connectors=2, intrusions=0, crossings=0, geometry/endpoints/mixed-corner failures=0 | outer/inner/restore sequence readable, no clipping or crowding                                                 |
| `async-channel-sequence` | Sequence; KLoggingChannel + tests; wiki best-practice and existing logging sequence references | `xmllint` PASS; CairoSVG 3600×2240 | markers=3, connectors=8, crossings=0, sequence-style PASS, numbered labels=7                   | participants/lifelines/activations/close frame/post-close drop readable; marker color and direction consistent |

Text-only technology card이므로 infrastructure icon은 N/A다. 별도 review page는 없고,
canonical file은 current manual chapter에서 직접 link된다.

`Required checks: 26/26; N/A: 8; Blocked: 0` — logging manual, deterministic site
snapshot, derived blog link, build, browser proof가 완료되었다.
