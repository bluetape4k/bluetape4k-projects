# Issue #846 DateAdd Backward Split Period 검토

## Scope

- `utils/javatimes/src/main/kotlin/io/bluetape4k/javatimes/period/calendars/DateAdd.kt`
- `utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/period/calendars/DateAddTest.kt`
- `utils/javatimes/src/test/kotlin/io/bluetape4k/javatimes/period/calendars/CalendarDateAddTest.kt`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| Backward seek correctness | Previous-period selection compared negative durations from `start` to each period end. | P1 | The comparison now measures positive distance from `period.end` to `start`. |
| Split include periods | Backward `subtract()` and negative `add()` could start from the first older include period. | P1 | Added split include period regression coverage from after the final period. |
| Calendar delegation | `CalendarDateAdd` delegates weekly working periods through `DateAdd.calculateEnd`. | P1 | Added split working-hour regression coverage for the same backward path. |
| Boundary behavior | The wrong period is visible even with zero offset. | P2 | Tests assert zero-offset selection before checking one-day/hour movement. |
| Scope control | Forward seek behavior should not change. | P2 | The implementation changes only `findPrevPeriod()` distance calculation. |

## Verification

- RED: DateAdd and CalendarDateAdd split backward regressions failed against the old selection logic.
- GREEN targeted: both new regressions passed.
- Module: `./gradlew :bluetape4k-javatimes:test --no-build-cache` passed with 690 tests and 36 pending.
- Build: `./gradlew :bluetape4k-javatimes:build --no-build-cache` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
