# Issue #846 DateAdd Backward Split Period Seek

Issue #846 found that `DateAdd.findPrevPeriod()` could choose an older
available period when a backward calculation started after multiple split
available periods.

## Decision

Compare previous-period distance as `Duration.between(period.end, start)` so
the nearest prior period has the smallest positive distance. This keeps the
existing forward seek logic unchanged and fixes backward seek selection for both
plain `DateAdd` include periods and `CalendarDateAdd` working-hour periods.

## Lessons

- Backward search should compare the distance from a candidate period end to the
  start moment. Comparing `start` to `period.end` produces negative durations
  and lets older periods look smaller than closer ones.
- A zero-offset regression is useful for this bug because it exposes the chosen
  start period directly before any duration arithmetic happens.
- `CalendarDateAdd` should be covered when a bug lives in `DateAdd.calculateEnd`
  because it rebuilds weekly working periods and delegates to the same path.

## Verification

- RED: split backward regressions failed with `2011-04-15T00:00Z` instead of `2011-04-24T00:00Z` and `2011-04-04T12:00Z` instead of `2011-04-04T18:00Z`.
- GREEN targeted: `./gradlew :bluetape4k-javatimes:test --tests "io.bluetape4k.javatimes.period.calendars.DateAddTest.backward seek after split include periods starts from nearest previous period" --tests "io.bluetape4k.javatimes.period.calendars.CalendarDateAddTest.backward seek after split working hours starts from nearest previous period" --no-build-cache` passed with 2 tests.
- Module: `./gradlew :bluetape4k-javatimes:test --no-build-cache` passed with 690 tests and 36 pending.
- Build: `./gradlew :bluetape4k-javatimes:build --no-build-cache` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
