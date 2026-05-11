# bluetape4k-core review, tests, KDoc, README design

Date: 2026-05-11
Module: `bluetape4k-core`
Branch/worktree: `codex/core-review-tests-docs` in `.worktrees/core-review-tests-docs`

## Problem

`bluetape4k-core` already has broad unit coverage and Korean KDoc examples, but the current review found several public contracts that still need stronger edge-case proof and documentation alignment:

- bounded collection APIs expose mutable operations with index/count edge cases;
- `SimplePaginatedList` accepts invalid pagination values until `totalPageCount` is evaluated;
- selected public iterator/array helpers need explicit KDoc examples;
- README should call out the documented edge contracts for core collections and pagination.

## Constraints

- Apply `bluetape4k-patterns`: use bluetape4k validation helpers for caller input, keep public API KDoc in Korean, keep diffs narrow, and verify with module tests.
- Do not change package layout or introduce dependencies.
- Keep compatibility-focused behavior unless current behavior is clearly invalid or corrupts object state.

## Approach

Use a focused module review rather than a full rewrite. Fix and test P1 correctness gaps, then document the same contracts in KDoc and README.

Rejected alternatives:

- Full public API KDoc rewrite across all 158 core files: too broad for one review pass and likely noisy.
- Documentation-only update: misses state-corruption and invalid-input gaps found in bounded collections and pagination.

## Acceptance Criteria

- P0 findings: 0.
- P1 findings: 0 after fixes and affected review reruns.
- New/changed public input contracts use existing `RequireSupport` helpers.
- Added edge tests cover negative/invalid inputs and bounded collection overflow insertion semantics.
- Public API KDoc touched by this change includes Korean examples.
- `README.md` and `README.ko.md` remain in sync for changed behavior.
- `:bluetape4k-core:test` and compile tasks pass or any verification gap is recorded with evidence.

## Initial Findings

| Priority | Finding | Target |
|---|---|---|
| P1 | `RingBuffer.drop(n)` accepts negative values, which can corrupt indices and size. | `RingBuffer.drop` |
| P1 | `BoundedStack.insert(index, elem)` has unclear/full-capacity insertion semantics and lacks edge tests. | `BoundedStack.insert` |
| P1 | `SimplePaginatedList` allows negative page numbers/counts and `pageSize <= 0`, causing invalid page metadata or divide-by-zero later. | `PaginatedList.kt` |
| P2 | `RingBuffer.toArray()` and iterators have limited public KDoc examples. | `RingBuffer`, `BoundedStack` |
| P2 | README does not explicitly describe bounded collection overflow and pagination validation contracts. | `README.md`, `README.ko.md` |

## Design Decision

- `RingBuffer.drop(n)` validates `n >= 0`; `drop(0)` is a no-op and `drop(size or more)` clears.
- `BoundedStack.insert(index, elem)` treats `index == 0` as push. For other valid indices, insertion preserves top-to-bottom order and discards the bottom element when capacity is exceeded.
- `SimplePaginatedList` validates `pageNo >= 0`, `pageSize > 0`, and `totalItemCount >= 0` at construction.
- KDoc examples document these contracts at the public API surface.
