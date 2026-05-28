# Issue 609 - Ktor module adoption and metadata

## Context

The Ktor module family was added in small slices, but the idgenerator Ktor
example and CI metadata still needed to prove that the modules are usable by a
real consumer.

## Decision

Migrate `idgenerator-ktor-demo` onto the shared modules before expanding the
Ktor surface:

- Use `bluetape4k-ktor-core` for JSON, standard API errors, and health/readiness.
- Use `bluetape4k-ktor-observability` for request ID propagation and call
  logging.
- Keep application routes explicit so the example remains useful as a consumer
  reference.
- Add `ktor/**` to example workflow triggers and run Ktor module tests in CI and
  Nightly.

Release metadata did not need a hand-edited module list because publication and
BOM constraints are derived from root subprojects while excluding examples and
demo modules.

## Outcome

The example now exercises core, observability, and testing helpers together.
CI and Nightly have explicit Ktor test and coverage artifacts, while the
examples workflow reruns when shared Ktor modules change.

## Verification

- `actionlint`
- `./gradlew :idgenerator-ktor-demo:compileKotlin :idgenerator-ktor-demo:compileTestKotlin`
- `./gradlew :bluetape4k-ktor-core:test :bluetape4k-ktor-observability:test :bluetape4k-ktor-testing:test :idgenerator-ktor-demo:test`
- `./gradlew :bluetape4k-ktor-core:koverXmlReport :bluetape4k-ktor-observability:koverXmlReport :bluetape4k-ktor-testing:koverXmlReport`
- `./gradlew -q projects | rg "bluetape4k-ktor-(core|observability|testing)|idgenerator-ktor-demo"`
- `git diff --check`

## Future guard

When a shared module is adopted by an example, update both directions: the
example dependency and the workflow triggers that should rerun the example when
the shared module changes.
