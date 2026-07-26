# Jackson3 Review, Tests, and Docs Plan

## Plan

1. Add explicit logical EOF APIs to `AsyncJsonParser` and `SuspendJsonParser`.
2. Refactor token draining into private helpers so normal chunk feed and EOF drain use the same path.
3. Add edge tests for complete EOF and truncated EOF in callback and suspend parser variants.
4. Update Korean KDoc and add short code comments explaining the EOF contract.
5. Remove stale Jasypt `@JsonEncrypt` README content and add Jackson3 advantages, recommended scenarios, anti-patterns, and EOF usage notes to both READMEs.
6. Run targeted compile/tests and full module tests.
7. Run external Claude advisor review for P0/P1 convergence, integrate any P0/P1 findings, then commit and open a draft PR.

## Review Gate Tracking

| Iteration | P0 | P1 | Action                                                                                                                       |
|-----------|----|----|------------------------------------------------------------------------------------------------------------------------------|
| Baseline  | 0  | 1  | Implement EOF terminal APIs and EOF edge tests.                                                                              |
| Advisor 1 | 0  | 6  | Add feed readiness checks, length validation, cancellation checkpoints, broader EOF tests, and remove stale Jasypt mentions. |
| Advisor 2 | 0  | 0  | Gate closed; no remaining P0/P1 findings.                                                                                    |

## Verification Commands

```bash
./gradlew :bluetape4k-jackson3:compileTestKotlin --no-build-cache
./gradlew :bluetape4k-jackson3:test --tests "io.bluetape4k.jackson3.async.AsyncJsonParserTest" --tests "io.bluetape4k.jackson3.async.SuspendJsonParserTest" --no-build-cache
./gradlew :bluetape4k-jackson3:test --no-build-cache
git diff --check
```

## Rollback Notes

- The new EOF APIs are additive.
- Existing incremental `consume(...)` behavior is intentionally preserved.
- README cleanup is documentation-only and tracks current source files.
