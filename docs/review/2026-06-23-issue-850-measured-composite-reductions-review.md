# Issue #850 Measured Composite Reductions 검토

## Scope

- `utils/measured/src/main/kotlin/io/bluetape4k/measured/Units.kt`
- `utils/measured/src/test/kotlin/io/bluetape4k/measured/MotionTest.kt`
- `utils/measured/src/test/kotlin/io/bluetape4k/measured/AreaTest.kt`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| Correctness | Generic `(A/B) * B` reduction multiplied by the raw `B` amount. | P1 | Convert `other` into `units.denominator` before multiplying. |
| Correctness | Generic `(A*B) / A` reduction divided by the raw `A` amount. | P1 | Convert `other` into `units.first` before dividing. |
| Regression coverage | Existing matching-unit tests did not expose scale-ratio errors. | P1 | Added mixed-scale `km/hr * minutes`, `m/s * hours`, `km*km / meters`, and `m*m / centimeters` cases. |
| API compatibility | The fix should preserve current generic operator signatures. | P2 | Implementation changes only internal calculation formulas and KDoc. |

## Verification

- RED: targeted measured tests failed with the expected mixed-scale regression values.
- GREEN targeted: targeted measured tests passed with 2 tests.
- Module: `./gradlew :bluetape4k-measured:test` passed with 181 tests.
- Build: `./gradlew :bluetape4k-measured:build` passed.
- Hygiene: `git diff --check` passed.
- Static analysis: `./gradlew detekt` passed with `:detekt NO-SOURCE`.
