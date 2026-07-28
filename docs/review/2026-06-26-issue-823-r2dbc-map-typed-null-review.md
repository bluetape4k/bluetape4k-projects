# Review — Issue 823 R2DBC Map Typed Null Binding (2026-06-26)

**Scope**: `:bluetape4k-r2dbc`
**Issue**: #823
**Branch**: `fix/r2dbc-map-typed-null-binding`

## Tiers

- Tier 1 Security: PASS
- Tier 4 Code correctness: PASS
- Tier 5 Tests: PASS
- Tier 7 Evidence: PASS

## 발견 사항

- P0: 0
- P1: 0
- P2/P3: 0

## 증거

- `typedNullParameter(type)` and `typedNullParameter<T>()` now provide a public helper for map-based typed NULL values.
- `bindMap` rejects raw null entries instead of silently binding them as `String` typed NULL values.
- `bindIndexedMap` rejects raw null entries and still validates zero-based indexes before binding.
- `UpdateValuesSpecImpl.set(parameters)` rejects raw null entries instead of converting them to `Any` typed NULL values.
- Explicit `Parameter` values are still preserved by map-based binding and update-map storage paths.
- Existing insert tests that need nullable values now use typed null binding through the explicit `nullValue(field, type)` overload.
- English and Korean R2DBC README examples now show typed NULL map binding.
- Regression proof before the fix: targeted `:bluetape4k-r2dbc:test` failed in `compileTestKotlin` because `typedNullParameter` did not exist.
- Validation after the fix: `./gradlew :bluetape4k-r2dbc:cleanTest :bluetape4k-r2dbc:compileKotlin :bluetape4k-r2dbc:compileTestKotlin :bluetape4k-r2dbc:test --no-build-cache` passed with 183 tests.
- `git diff --check` passed.

## Notes

The change intentionally treats raw map null values as ambiguous. Callers that
need nullable map entries must pass `typedNullParameter<T>()` or an explicit
R2DBC `Parameter` value so the driver receives usable type information.
