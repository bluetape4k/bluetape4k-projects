# bluetape4k-annotations module

## Context

Kotlin opt-in annotations need to be reused across Bluetape modules without
pulling in `bluetape4k-core`. A standalone annotations artifact keeps API
maturity contracts available to low-level modules and future ecosystem
artifacts.

## Decision

Add `:bluetape4k-annotations` under `bluetape4k/annotations` with six marker
annotations:

- `BluetapeExperimentalApi`
- `BluetapeBetaApi`
- `BluetapeInternalApi`
- `BluetapeDelicateApi`
- `BluetapeObsoleteApi`
- `BluetapeImplementationApi`

`BluetapeImplementationApi` is intentionally class-oriented and documented for
`@SubclassOptInRequired`, not as a generic function or property marker.

## Outcome

The module is auto-registered by `includeModules("bluetape4k", true, false)`,
covered by CI and nightly core jobs, and documented in the root README and BOM
README pairs.

## Verification

- `./gradlew projects --console=plain --no-configuration-cache`
- `./gradlew :bluetape4k-annotations:compileKotlin :bluetape4k-annotations:test --console=plain --no-configuration-cache`
- `actionlint`
- `rg -n -F "\\'" .github/workflows`
- `git diff --check`
- IntelliJ MCP diagnostics returned zero problems/build errors, but file
  analysis was not fresh because the worktree files were not open in the IDE.

## Future Guidance

Keep marker annotation classes available after individual APIs graduate to
stable, unless a major version intentionally removes the marker artifact.
