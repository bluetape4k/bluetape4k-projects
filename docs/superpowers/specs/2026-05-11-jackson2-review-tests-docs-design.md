# Jackson2 Review, Tests, and Docs Design

## Scope

- Target module: `io/jackson2` (`bluetape4k-jackson2`).
- Apply bluetape4k 6-Tier review until no P0/P1 findings remain.
- Add missing edge tests, Korean KDoc examples for public API changes, explanatory comments for non-obvious behavior, and synchronized README updates.
- Keep changes scoped to async parser stream safety and documentation drift found during review.

## 6-Tier Review Summary

| Tier | Result | Notes |
|------|--------|-------|
| API contract | P1 found | Non-blocking parsers expose `consume` but no logical EOF operation, despite Jackson requiring `endOfInput()` to validate final truncated input. |
| Correctness | P1 found | `consume` only feeds when `needMoreInput()` is true and otherwise silently skips a new chunk. |
| Coroutines | P1 found | `SuspendJsonParser` can drain many tokens without an explicit cancellation checkpoint. |
| Security/data safety | No P0/P1 in code | Tink encryption code remains unchanged; README has stale Jasypt `JsonEncrypt` references to nonexistent source files. |
| Tests | P1 gap | Existing async tests do not cover truncated-at-EOF, multiple truncation shapes, or invalid `length`. |
| Documentation | P2 found | README lacks advantages/scenarios/anti-patterns and still documents legacy Jasypt `JsonEncrypt` classes that are not present in `io/jackson2`. |

## P0/P1 Findings

| Priority | Finding | Resolution |
|----------|---------|------------|
| P0 | None | N/A |
| P1 | Async parsers cannot signal logical EOF, so truncated final JSON can remain in "waiting for more input" state. | Add `endOfInput()` APIs, `SuspendJsonParser.consumeComplete(...)`, KDoc examples, and EOF edge tests. |
| P1 | New chunks can be silently skipped when Jackson feeder is not ready. | Drain pending tokens first, then fail fast with `check` before feeding. |
| P1 | `consume(bytes, length)` accepts invalid length values. | Validate `length` with `requireInRange(0, bytes.size, "length")`. |
| P1 | Suspend token drain lacks an explicit cancellation checkpoint. | Add `currentCoroutineContext().ensureActive()` in the drain loop. |
| P1 | Advisor iteration found public lifecycle/throws docs, post-EOF tests, and cancellation regression coverage still incomplete. | Added lifecycle KDoc, `JsonParseException` throws docs, post-EOF/double EOF tests, empty Flow test, and cancellation propagation test. |

## P2/P3 Findings

| Priority | Finding | Resolution |
|----------|---------|------------|
| P2 | README documents legacy `JsonEncrypt`/Jasypt files that do not exist in this module. | Remove stale Jasypt guidance and document Tink-only field encryption for Jackson2. |
| P2 | README lacks a practical decision guide. | Add advantages, recommended scenarios, and anti-patterns to English/Korean READMEs. |
| P2 | `JacksonSerializer` catches `Throwable`. | Defer unless advisor escalates; this module is synchronous and current serializer tests cover public failure wrapping. |
| P2 | Class diagram implied `AsyncJsonParser` depends on `SuspendJsonParser`. | Removed the misleading dependency edge from both READMEs. |
| P2 | EOF KDoc did not mention Jackson `JsonParseException`. | Added `JsonParseException` throws docs to public consume/EOF APIs. |

## Design Decisions

- Preserve `consume(...)` as incremental feed API because existing callers/tests supply partial arrays and repeated roots across multiple calls.
- Add EOF as an explicit terminal operation. Flow completion remains incremental by default for backward compatibility.
- Add `consumeComplete(flow)` for finite suspend streams where Flow completion is the logical input boundary.
- Keep comments short and targeted to why EOF and drain-before-feed are necessary.

## Acceptance Criteria

- `AsyncJsonParser.endOfInput()` and `SuspendJsonParser.consumeComplete(...)` throw on truncated final JSON.
- Multiple truncation states are covered: cut object value, cut number, unterminated string, dangling escape.
- `consume(bytes, length)` rejects invalid length.
- Public API KDoc is Korean and includes examples.
- README.md and README.ko.md are synchronized and no longer reference nonexistent Jasypt files/classes.
- Latest integrated review gate shows `P0 = 0` and `P1 = 0`.

## Final Gate Evidence

| Review | P0 | P1 | Notes |
|--------|----|----|-------|
| Claude advisor initial review | 0 | 8 | Lifecycle docs/tests, cancellation coverage, README anti-pattern, and length-boundary gaps found. |
| Claude advisor re-review | 0 | 0 | Remaining items were P2/P3 only; P2 docs drift and cheap P3 tests/simplifications were also addressed. |
