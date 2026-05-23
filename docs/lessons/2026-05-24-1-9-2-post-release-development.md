# 2026-05-24 1.9.2 Post-Release Development Line

## Context

`bluetape4k-projects` `1.9.1` has been released, but the repository still has
an active `1.9.2` milestone for the IO HTTP patch lane. A stale PR tried to move
`develop` directly to `1.10.0`, which would skip the active patch milestone.

## Decision

Move the committed base version from `1.9.1` to `1.9.2`. Keep
`snapshotVersion=` empty in `gradle.properties`; SNAPSHOT publishing must pass
`-PsnapshotVersion=-SNAPSHOT`.

## Outcome

`develop` stays aligned with the active `1.9.2` patch milestone while `1.10.0`
remains reserved for the Ktor module-family minor line.

## Verification

- `./gradlew properties --no-configuration-cache --no-daemon --quiet`
- `./gradlew properties -PsnapshotVersion=-SNAPSHOT --no-configuration-cache --no-daemon --quiet`
- `git diff --check`

## Future Guard

Do not open a new minor development line while an active patch milestone still
owns the next release unless that patch milestone is explicitly deferred or
closed.
