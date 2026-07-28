# Review — Issue 824 R2DBC PostgreSQL JSON Conversion (2026-06-26)

**Scope**: `:bluetape4k-r2dbc`
**Issue**: #824
**Branch**: `fix/r2dbc-json-conversion-fail-fast`

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

- `JsonToMapConverter` now throws `ConversionFailedException` for malformed PostgreSQL JSON instead of returning `emptyMap()`.
- `MapToJsonConverter` now throws `ConversionFailedException` for Jackson serialization failures instead of returning `Json.of("{}")`.
- Both converters preserve the original Jackson cause on the conversion failure.
- Regression proof before the fix: 2 new tests failed with `Expected ConversionFailedException but no exception was thrown`.
- Targeted validation after the fix: `./gradlew :bluetape4k-r2dbc:test --tests 'io.bluetape4k.r2dbc.convert.postgresql.PostgresJsonConvertersTest' --no-build-cache` passed with 4 tests.
- Module validation after the fix: `./gradlew :bluetape4k-r2dbc:cleanTest :bluetape4k-r2dbc:compileKotlin :bluetape4k-r2dbc:compileTestKotlin :bluetape4k-r2dbc:test --no-build-cache` passed with 175 tests.
- `git diff --check` passed.

## Notes

The converter still logs the Jackson error, but the persistence/read path now fails explicitly so invalid or unserializable data cannot be silently replaced with a valid empty JSON shape.
