# Review — Issue 826 R2DBC Indexed Binding (2026-06-26)

**Scope**: `:bluetape4k-r2dbc`
**Issue**: #826
**Branch**: `fix/r2dbc-indexed-binding-docs`

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

- `bindIndexedMap` KDoc now documents Spring R2DBC's zero-based indexed binding contract.
- `bindIndexedMap` now rejects negative indexes before forwarding to Spring R2DBC.
- `bindNullable(index, ...)` KDoc and validation now match the same zero-based index contract.
- English and Korean R2DBC README examples now use `0` and `1` for two positional parameters.
- Regression coverage now directly exercises `bindIndexedMap` with `?` positional markers through `DatabaseClient.GenericExecuteSpec`.
- Guard coverage now asserts negative index rejection for `bindIndexedMap` and indexed `bindNullable`.
- Validation after the fix: `./gradlew :bluetape4k-r2dbc:cleanTest :bluetape4k-r2dbc:compileKotlin :bluetape4k-r2dbc:compileTestKotlin :bluetape4k-r2dbc:test --no-build-cache` passed with 177 tests.
- `git diff --check` passed.

## Notes

The change does not translate caller indexes. It documents and enforces the existing Spring R2DBC zero-based contract so caller code and documentation stay aligned with the underlying API.
