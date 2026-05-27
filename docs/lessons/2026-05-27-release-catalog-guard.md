# Release Catalog Guard

## Context

The AWS 0.3.0 release exposed a shared release workflow risk: an operational
catalog override can make a stable release use a different
`bluetape4k-dependencies` catalog than the checked-in `settings.gradle.kts`
default.

## Decision

Stable tag releases use the checked-in catalog default. Manual dispatch can use
an explicit `catalogRef` override, then the repository variable as an
operational fallback.

## Outcome

The release workflow logs the selected catalog source and verifies required
catalog aliases before Maven Central publish.

## Verification

Run `actionlint`, validate catalog selection branches locally, and check the
current release catalog contains the required aliases.

## Future Guidance

Treat repository catalog variables as manual release overrides, not as the
release train source of truth.
