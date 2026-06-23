# Issue #849 Histogram Bin Progress Guards

Issue #849 found that histogram bin builders trusted the caller-provided bin
increment. A zero or negative bin size could make the shared comparable bin loop
append ranges forever, and non-finite `Double` bin sizes had no public guard.

## Decision

Reject invalid typed bin sizes before entering the shared bin loop:

- `Double` bin sizes must be finite and greater than zero.
- `BigDecimal` bin sizes must be greater than zero.
- Custom `binByComparable` incrementers must strictly increase the current range
  end on every loop iteration.

## Lessons

- Public numeric histogram APIs should validate progress at the typed boundary;
  callers should not be able to create non-terminating bins through a simple
  zero or negative size.
- Shared generic loops still need a defensive invariant because callers can
  provide custom incrementers outside the typed helpers.
- RED tests for non-progressing loops can exhaust the test JVM. Capture one
  failing reproduction, then add fail-fast guards before running the full suite.

## Verification

- RED: `./gradlew :bluetape4k-math:test --tests "io.bluetape4k.math.DoubleHistogramTest.binByDouble rejects non progressing bin sizes"` failed with test JVM `Java heap space` after the zero bin size kept growing ranges.
- GREEN targeted: `./gradlew :bluetape4k-math:test --tests "io.bluetape4k.math.DoubleHistogramTest.binByDouble rejects non progressing bin sizes" --tests "io.bluetape4k.math.BigDecimalHistogramTest.binByBigDecimal rejects non progressing bin sizes" --tests "io.bluetape4k.math.ComparableHistogramTest.binByComparable rejects non progressing incrementers"` passed with 3 tests.
- Module: `./gradlew :bluetape4k-math:test` passed with 573 tests and 1 pending.
- Build: `./gradlew :bluetape4k-math:build` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
