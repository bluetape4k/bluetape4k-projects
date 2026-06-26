# Lessons Learned — R2DBC Indexed Binding (2026-06-26)

**Issue**: #826
**Module**: `:bluetape4k-r2dbc`

## L1: Documentation examples are API contract tests

### Problem

`bindIndexedMap` forwarded map keys directly to Spring R2DBC's indexed binding API, but KDoc and README examples showed `1` and `2` as the first two indexes.

### Lesson

When a helper mirrors a framework API without translating values, examples must use the framework's exact contract. For Spring R2DBC indexed parameters, that contract is zero-based.

### Future Guard

For parameter binding helpers, add a direct test that executes the README/KDoc-style example. Also validate out-of-contract inputs such as negative indexes before forwarding to the framework layer.
