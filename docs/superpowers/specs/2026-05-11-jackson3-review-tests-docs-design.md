# Jackson3 Review, Tests, and Docs Design

## Scope

- Target module: `io/jackson3` (`bluetape4k-jackson3`).
- Apply the bluetape4k 6-Tier review gate until no P0/P1 findings remain.
- Add missing edge tests, Korean KDoc examples for public API changes, explanatory code comments for non-obvious behavior, and synchronized README updates.
- Keep changes scoped to Jackson3 async parsing and documentation drift found during review.

## 6-Tier Review Summary

| Tier | Result | Notes |
|------|--------|-------|
| API contract | P1 found | Non-blocking parsers had no explicit logical EOF path, so truncated final JSON could remain undetected. |
| Correctness | P1 found | EOF is required by Jackson `NonBlockingInputFeeder.endOfInput()` to convert incomplete input into a parse failure. |
| Concurrency/coroutines | No P0/P1 | `SuspendJsonParser` does not swallow coroutine cancellation in `onNodeDone`; no shared-thread use is introduced. |
| Security/data safety | No P0/P1 | Tink encryption implementation was not changed; README drift around legacy Jasypt docs is documentation risk only. |
| Tests | P1 coverage gap | Existing tests covered malformed tokens but not truncated-at-EOF streams. |
| Documentation | P2 found | README references nonexistent legacy `@JsonEncrypt` Jasypt files/modules in this module. |

## P0/P1 Findings

| Priority | Finding | Resolution |
|----------|---------|------------|
| P0 | None | N/A |
| P1 | `AsyncJsonParser`/`SuspendJsonParser` cannot signal logical stream end, so callers cannot force Jackson to report truncated final JSON. | Add explicit `endOfInput()` APIs, a suspend `consumeComplete()` convenience, KDoc examples, and EOF edge tests. |
| P1 | Parser feed could silently skip a new chunk if Jackson was not ready for more input. | Drain pending tokens first, then fail fast with `check` instead of ignoring data. |
| P1 | `consume(bytes, length)` accepted invalid length values. | Validate `length` with `requireInRange(0, bytes.size, "length")` and add range tests. |
| P1 | Suspend parser drain loop had no explicit cancellation checkpoint. | Add `currentCoroutineContext().ensureActive()` while draining available tokens. |
| P1 | EOF tests covered only one truncation shape. | Add number, unterminated string, and dangling escape truncation cases for both parsers. |

## P2/P3 Findings

| Priority | Finding | Resolution |
|----------|---------|------------|
| P2 | README documents legacy Jasypt `@JsonEncrypt` classes that do not exist under `io/jackson3/src/main/kotlin`. | Remove stale Jasypt guidance and document only `@JsonTinkEncrypt` for Jackson3. |
| P2 | README lacks a compact decision guide for Jackson3 advantages, recommended scenarios, and anti-patterns. | Add English/Korean sections and keep both READMEs synchronized. |

## Design Decisions

- Preserve `consume(...)` as an incremental feed API. Existing tests and expected usage call `consume` multiple times for partial arrays and repeated roots.
- Add EOF as an explicit terminal operation rather than treating every `Flow` completion as EOF. This avoids breaking callers that use several finite flows to model one longer logical stream.
- Add `consumeComplete(flow)` for finite suspend streams where Flow completion is the logical input boundary.
- Keep code comments short and focused on why EOF is required and why `consume` remains incremental.

## Acceptance Criteria

- `AsyncJsonParser.endOfInput()` throws on truncated final JSON and does not emit extra roots for complete input.
- `SuspendJsonParser.consumeComplete(...)` throws on truncated final JSON and succeeds for complete input.
- Public API KDoc explains incremental feed vs completed stream usage in Korean with examples.
- README.md and README.ko.md describe Jackson3 advantages, recommended scenarios, anti-patterns, and the EOF contract.
- Latest integrated 6-Tier gate shows `P0 = 0` and `P1 = 0`.

## Review Gate Closure

| Iteration | Reviewer | P0 | P1 | Notes |
|-----------|----------|----|----|-------|
| Baseline | Codex 6-Tier | 0 | 1 | Missing logical EOF contract found. |
| Advisor 1 | Claude Opus 4.7 | 0 | 6 | Feed readiness, length validation, coroutine cancellation, README drift, and EOF coverage gaps found. |
| Advisor 2 | Claude Opus 4.7 | 0 | 0 | All prior P1 findings resolved. Artifact: `.omx/artifacts/claude-jackson3-rereview-20260511.md`. |
