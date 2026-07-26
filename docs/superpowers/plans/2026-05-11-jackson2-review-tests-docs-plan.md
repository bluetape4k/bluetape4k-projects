# Jackson2 Review, Tests, and Docs Plan

## Plan

1. Add explicit logical EOF APIs to `AsyncJsonParser` and `SuspendJsonParser`.
2. Refactor token draining into private helpers used by normal consume and EOF.
3. Add feed readiness checks, input length validation, and coroutine cancellation checkpoints.
4. Add async parser edge tests for complete EOF, invalid length, and several truncated JSON states.
5. Update Korean KDoc and add explanatory comments around non-obvious stream safety behavior.
6. Update README.md and README.ko.md with Jackson2 advantages, recommended scenarios, anti-patterns, EOF guidance, and Tink-only encryption docs.
7. Run compile, targeted async tests, full module tests, `git diff --check`, and external advisor re-review until P0/P1 = 0.
8. Commit with Lore trailers, push branch, and open a draft PR.

## Review Gate Tracking

| Iteration           | P0 | P1 | Action                                                                                                                                   |
|---------------------|----|----|------------------------------------------------------------------------------------------------------------------------------------------|
| Baseline            | 0  | 4  | Implement EOF terminal APIs, feed validation, cancellation checkpoint, and edge tests.                                                   |
| Advisor iteration 1 | 0  | 8  | Added lifecycle/throws KDoc, post-EOF tests, cancellation propagation coverage, README anti-pattern fix, and empty/partial length tests. |
| Advisor iteration 2 | 0  | 0  | Gate closed; fixed P2 docs drift and low-risk P3 cleanup/tests before final validation.                                                  |

## Verification Commands

```bash
./gradlew :bluetape4k-jackson2:compileTestKotlin --no-build-cache --no-daemon
./gradlew :bluetape4k-jackson2:test --tests "io.bluetape4k.jackson.async.AsyncJsonParserTest" --tests "io.bluetape4k.jackson.async.SuspendJsonParserTest" --no-build-cache --no-daemon
./gradlew :bluetape4k-jackson2:test --no-build-cache --no-daemon
git diff --check
```

## Rollback Notes

- New EOF APIs are additive.
- Existing incremental `consume(...)` behavior is preserved.
- README cleanup tracks current source files and removes stale docs only.
