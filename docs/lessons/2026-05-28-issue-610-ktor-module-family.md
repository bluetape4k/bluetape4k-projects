# Issue #610 Ktor Module Family Design

## Context

Milestone 1.10.0 starts the reusable Ktor module family in `bluetape4k-projects`.
The first step was design-only work for #610 before scaffolding or
implementation.

## Decision

Keep the first Ktor module family inside `bluetape4k-projects` and split the
server foundation into `ktor/core`, `ktor/observability`, and `ktor/testing`.
Keep `ktor/client`, `ktor/resilience4j`, `ktor/openapi`, and `ktor/auth` in the
backlog milestone until the server-side extension points are proven.

## Outcome

The design and plan documents define the module boundaries, dependency rules,
API direction, and PR sequence for #611 through #616.

## Verification

- GNO checked existing #609-#616 issue context.
- Existing Ktor examples in sibling bluetape4k repositories were surveyed.
- Official Ktor docs were checked for plugin installation, `StatusPages`,
  `ContentNegotiation`, `CallLogging`, `MicrometerMetrics`, and
  `testApplication` APIs.

## Future Guard

Do not start Ktor implementation before #610 design has passed review. Keep
plugin installation explicit and do not promote backlog modules into the first
slice without reopening the design boundary.

