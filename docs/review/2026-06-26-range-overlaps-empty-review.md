# Review - Range Empty Overlap (2026-06-26)

Issue: #783
Branch: `fix/range-overlaps-empty`
Module: `:bluetape4k-core`

## Scope

- Added an empty-operand guard to `Range.overlaps()`.
- Added regression coverage for empty open/open, closed/open, and open/closed ranges in both receiver and argument
  positions.

## 7-Tier 검토

| Tier | Result | Evidence |
|---|---:|---|
| Correctness | PASS | `overlaps()` now returns `false` when either operand is empty before endpoint comparisons run. |
| Compatibility | PASS | Public API signatures are unchanged. |
| Boundary behavior | PASS | Existing same-type and cross-type non-empty overlap tests still pass. |
| Test coverage | PASS | New regression test failed before the fix and passes after the guard. |
| Simplicity | PASS | One guard added in the shared helper; no new abstraction. |
| Documentation | PASS | Lesson artifact records the empty-range comparison rule. |
| Regression risk | PASS | Core module test suite passes and CodeGraph reported low impact for the changed files. |

## 발견 사항

P0: 0
P1: 0

P2/P3: none requiring code changes before PR.

## 검증 Evidence

- Reproduced before fix:
  - `./gradlew :bluetape4k-core:test --tests 'io.bluetape4k.ranges.RangeSupportTest.empty ranges never overlap any range' --no-build-cache`
  - Result: FAIL with `Expected <true> to be <false>` at `RangeSupportTest.kt:135`.
- After fix:
  - `./gradlew :bluetape4k-core:test --tests 'io.bluetape4k.ranges.RangeSupportTest.empty ranges never overlap any range' --tests 'io.bluetape4k.ranges.RangeSupportTest.overlaps with same type ranges' --tests 'io.bluetape4k.ranges.RangeSupportTest.overlaps with cross-type ranges' --no-build-cache`
  - Result: PASS.
  - `./gradlew :bluetape4k-core:compileKotlin :bluetape4k-core:compileTestKotlin :bluetape4k-core:test --no-build-cache`
  - Result: PASS.
  - `git diff --check`
  - Result: PASS.
