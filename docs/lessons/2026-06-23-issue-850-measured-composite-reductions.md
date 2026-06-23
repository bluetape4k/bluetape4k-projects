# Issue #850 Measured Composite Reduction Conversions

Issue #850 found that generic composite-unit reductions used the raw right-hand
amount instead of converting it into the unit embedded in the composite
measure. Mixed-scale reductions therefore produced values that were off by the
scale ratio.

## Decision

Convert the operand into the composite unit component before reducing:

- `(A/B) * B -> A` converts `B` into the ratio denominator.
- `(A*B) / A -> B` converts `A` into the product first unit.

This preserves the existing generic unit algebra while making mixed-scale
reductions consistent with `Measure.in`.

## Lessons

- Composite measure reductions must honor both operands' unit ratios, not just
  their raw amounts.
- Domain-specific operators such as `Length * Length -> Area` can hide bugs in
  the lower-level generic algebra; regression tests should exercise the generic
  `UnitsProduct` and `UnitsRatio` paths directly.
- RED cases should include asymmetric scales in the reduced-away dimension,
  such as `km/hr * minutes` and `km*km / meters`.

## Verification

- RED: `./gradlew :bluetape4k-measured:test --tests "io.bluetape4k.measured.MotionTest.속도와 다른 시간 단위로 거리를 계산한다" --tests "io.bluetape4k.measured.AreaTest.면적을 다른 길이 단위로 나누어 길이를 계산한다"` failed with `1080.0` vs `18.0` and `1.0` vs `1000.0`.
- GREEN targeted: the same two-test command passed with 2 tests.
- Module: `./gradlew :bluetape4k-measured:test` passed with 181 tests.
- Build: `./gradlew :bluetape4k-measured:build` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
