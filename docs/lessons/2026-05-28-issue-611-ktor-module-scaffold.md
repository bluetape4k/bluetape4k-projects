# Issue #611 Ktor Module Scaffold

## Context

Issue #611 starts the 1.10.0 Ktor module family after #610 defined the
boundaries. This step should only register modules and keep production APIs for
#612 through #614.

## Decision

Add `ktor/core`, `ktor/observability`, and `ktor/testing` under the existing
`includeModules("ktor", withBaseDir = true)` pattern so Gradle project names
become `bluetape4k-ktor-core`, `bluetape4k-ktor-observability`, and
`bluetape4k-ktor-testing`.

## Outcome

The scaffold adds build files, README locale stubs, test resources, and root
module-list updates. CI/Nightly workflow changes remain assigned to #616.

## Verification

- `./gradlew projects` must list the three new modules.
- Targeted compile tasks must prove empty/minimal source sets do not break the
  build.

## Future Guard

Do not add production Ktor helper APIs in scaffold-only work. Start behavior in
#612, #613, and #614 after module registration is proven.

