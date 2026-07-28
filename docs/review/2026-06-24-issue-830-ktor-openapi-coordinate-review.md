# Review - Issue #830 Ktor OpenAPI Maven Coordinate

Date: 2026-06-24
Branch: `fix/ktor-openapi-readme-coordinate-830`
Module: `:bluetape4k-ktor-openapi`

## Scope

- `ktor/openapi/README.md`
- `ktor/openapi/README.ko.md`

## Local 검토

- P0/P1 findings: 0
- The dependency snippets now match `gradle.properties`:
  `projectGroup=io.github.bluetape4k`.
- English and Korean README dependency sections remain in sync.
- No production code changed.
- No concurrency test helper was needed because this is a deterministic
  documentation coordinate correction.

## 검증 Evidence

- `rg "io\\.github\\.bluetape4k:bluetape4k-ktor-openapi" ktor/openapi/README.md ktor/openapi/README.ko.md`
  found the corrected coordinate in both README files.
- `rg "io\\.bluetape4k:bluetape4k-ktor-openapi" ktor/openapi/README.md ktor/openapi/README.ko.md`
  returned no stale openapi coordinate matches.
- `git diff --check` passed.
- `./gradlew :bluetape4k-ktor-openapi:compileKotlin :bluetape4k-ktor-openapi:compileTestKotlin :bluetape4k-ktor-openapi:test --no-build-cache`
  passed.

## Residual Risk

- A narrow sibling Ktor README audit found similar group-id drift in
  `ktor/core` and `ktor/observability`. This PR intentionally stays scoped to
  #830's `ktor/openapi` module; the sibling README drift should be handled in
  a follow-up docs issue or the corresponding module review.
