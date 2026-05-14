# Issue 433 Infra Resilience Wrapper Triage

## Context

The JVM-only direction changes the Bucket4j/Resilience4j question from native porting to wrapper standardization. `infra/bucket4j` and `infra/resilience4j` already contain Kotlin facade code, coroutine wrappers, tests, and README pairs.

## Decision

Track the umbrella decision as issue `#433`: keep Bucket4j and Resilience4j as upstream JVM engines, then harden bluetape4k's Kotlin coroutine facade contracts. Do not port internals or introduce KMP/Kotlin Native scope. Split execution into module-specific issues `#434` for Bucket4j and `#435` for Resilience4j.

## Outcome

The WIP queue now has an infra resilience facade lane. `#433` stays as the decision record, while `#434` and `#435` carry acceptance criteria for implementation. The split keeps Bucket4j token-bucket provider ergonomics separate from Resilience4j coroutine resilience policy composition.

## Verification

- Searched open issues for an existing Bucket4j/Resilience4j wrapper item; no duplicate open issue was found.
- Counted current module footprint: `infra/bucket4j` has 19 main Kotlin files and 26 test Kotlin files; `infra/resilience4j` has 22 main Kotlin files and 40 test Kotlin files.
- Created GitHub issue `#433` assigned to `debop` with `enhancement`, `design`, and `refactor` labels.
- Split implementation into issues `#434` and `#435` after the umbrella proved too broad.
- Verified the current open assigned issue count with `gh issue list --assignee debop --state open --search 'created:>=2026-01-01'`; result was 12 issues.
- Updated `WIP.md` snapshot count from 10 to 12 active assigned issues and removed closed `#251` from the active queue.

## Future Guidance

When touching these modules, prefer contract hardening and documentation over new abstraction layers. Cancellation propagation tests should be the first implementation guardrail. Keep umbrella and execution issues separate so future PRs can close a focused module issue without reopening the broader Bucket4j versus Resilience4j positioning decision.
