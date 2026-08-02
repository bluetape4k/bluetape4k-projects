# Issue #1297 Step 3-R Plan Review

## Reviewed scope

- Plan: `docs/superpowers/plans/2026-08-03-flow-operator-parity-plan.md`.
- Spec basis: `docs/superpowers/specs/2026-08-03-flow-operator-parity-design.md`.
- Repo: `bluetape4k-projects`, isolated branch
  `feat/issue-1297-flow-operators`.
- Baseline: `origin/develop` at `f47da3e0da0e98da86d3361be5743ea74679f09c`;
  `:bluetape4k-coroutines:test` baseline passed before implementation.
- Execution: six required plan lenses were run sequentially in the main
  session, with the workflow receipt's `main-review` fallback lane. No
  subagent or parallel implementation lane was used.

## Required plan checks

| # | Required check | Evidence | Result |
|---:|---|---|---|
| 1 | Every spec requirement and DoD maps to a concrete task | Tasks 1-5 cover inventory, three API families, docs, tests, benchmarks, and follow-up; Task 6 covers full verification, lesson, PR train, exact-head DoD. | PASS |
| 2 | Task ordering is implementable against current code | Task 1 fixes duplicate search/inventory before Task 2-4 code; Task 5 documents the completed APIs; Task 6 verifies the integrated branch. | PASS |
| 3 | No task depends on a later artifact | Each test is RED before its implementation; README examples are explicitly eventual signatures and are finalized after API tasks; final review follows all slices. | PASS |
| 4 | Tests cover success, failure, edge, concurrency, coroutine, lifecycle, backend capability | Count/time, partial completion, upstream/fallback errors, invalid arguments, same-instant race, `take(1)` cleanup, bounded concurrency/order, and child failures are named. Backend/Testcontainers is explicitly N/A for this module-local Flow change. | PASS |
| 5 | Verification commands are concrete and targeted | Every slice has an exact Gradle `--tests` command; Task 6 has module test/check/benchmark and `git diff --check`. | PASS |
| 6 | README coverage for public behavior | Task 1 and Task 5 modify both `README.md` and `README.ko.md` with matching examples and boundary semantics. | PASS |
| 7 | Korean KDoc and English public GitHub artifacts | Task 5 requires Korean-first KDoc; Task 1 uses an English follow-up issue body; Task 6 requires English PR metadata ending in `## DoD Status`. | PASS |
| 8 | New module registration/BOM/CI/resources | No module is added, moved, or published; the plan explicitly keeps settings/catalog/dependency scope unchanged. | N/A with scope evidence |
| 9 | Spring Boot auto-configuration guards | No Spring Boot module or auto-configuration is touched. | N/A |
| 10 | Exposed deprecated imports/receiver shadowing | No Exposed module or receiver API is touched. | N/A |
| 11 | Coroutine cancellation and dispatcher boundaries | Execution rules forbid dispatcher switching, blocking calls, `GlobalScope`, and detached children; tests use `runTest`, `finally` markers, and cancellation assertions. | PASS |
| 12 | Performance/stability: allocation, blocking, cleanup, backpressure, Testcontainers | Shared list collector, virtual-time timers, bounded per-inner channels, semaphore permits, `finally` cleanup, and benchmark methods are specified; no polling/blocking or Testcontainers path is introduced. | PASS; Testcontainers N/A |
| 13 | Cross-module duplication/reuse decision | Standard `flatMapLatest`, `buffer`, `conflate`, `combine`, `zip`, and `retryWhen` remain mappings/non-goals; one internal count-or-time collector serves both selected APIs. | PASS |
| 14 | Rollback/compatibility/migration risks | Existing `concatMapEager` remains; new APIs are additive; cold snapshot window and first-element timer semantics are documented; each slice is independently committed for stacked rollback. | PASS |

## Six-lens integration

| Lens | Finding after repair | Priority | Result |
|---|---|---:|---|
| Performance | Benchmark scope now distinguishes timer registration/list allocation from virtual-time timer firing. | P1 resolved | PASS |
| Stability | Same-instant receive/timeout precedence and producer/fallback cleanup have exact tests and implementation ordering. | P1 resolved | PASS |
| Security | No new trust boundary, credentials, persistence, deserialization, or network path. | N/A | PASS |
| Operator/Ops | No deployment/configuration surface; external runtime interoperability remains an explicit not-tested risk. | P2 deferred | PASS with documented gap |
| Developer/API | Task boundaries, overload compatibility, validation, imports, and exact commands are specified. | P1 resolved | PASS |
| User/caller | README/KDoc, migration notes, standard Flow mappings, and misuse tests are specified. | P1 resolved | PASS |

## Required repairs completed before closure

1. Replaced literal `\n` issue-body escaping with shell `$'...'` quoting and
   made post-create assignee/milestone verification explicit.
2. Added exact invalid-argument, same-instant tie, repeatable-window, and
   cancellation-marker test cases.
3. Added the `TimeoutException` import/type requirement and benchmark `days`
   import requirement.
4. Added explicit dispatcher/no-blocking/no-detached-child constraints.

## Verdict

- P0: **0**
- P1: **0** after the repairs above
- P2: **1 deferred** (external Rx/Reactor runtime interoperability; not part of
  this module-local slice)
- P3: **0 open**
- Step 3-R status: **PASS**.
- Implementation gate: **OPEN**; proceed to Task 1 only after recording this
  review and the Step 2-R evidence in the workflow receipt.
