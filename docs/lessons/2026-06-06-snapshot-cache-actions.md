# Snapshot Cache Actions

## Context

Nightly should not disable Gradle dependency caching while the repository relies
on mutable bluetape4k SNAPSHOT artifacts from Central snapshots.

## Decision

Remove `cache-disabled: true` from Nightly Gradle setup steps so scheduled jobs
can reuse dependency metadata according to Gradle's changing-module cache policy.

## Outcome

Nightly keeps its existing task structure, but Gradle cache write/read behavior
is no longer explicitly disabled in the workflow.

## Verification

- `actionlint .github/workflows/*.yml`
- `rg -n -- '--refresh-dependencies|cache-disabled: true' .github/workflows` -> no matches
- `./gradlew help --no-daemon`
- `git diff --check`

## Future Guidance

Use explicit dependency refresh only in dedicated post-publish freshness checks.
Ordinary CI, Nightly, and Examples workflows should rely on cached changing-module
metadata plus targeted warm-up when a test-only SNAPSHOT dependency needs it.
