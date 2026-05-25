# 2026-05-26 projects 1.9.2 release prep

## Context

`bluetape4k-projects` 1.9.2 is the active patch release line for the IO HTTP
performance and documentation lane. The milestone was complete, but the release
metadata still needed a dated changelog section before tagging.

## Decision

Prepare release metadata on `release/1.9.2`, keep `baseVersion=1.9.2` and
`snapshotVersion=` unchanged, and clean release-range trailing whitespace so
`git diff --check` can be used as a clean preflight gate.

## Outcome

The release prep branch contains only changelog, lesson, and whitespace cleanup
changes. Stable publication should proceed only after the prep is merged and
snapshot validation is refreshed for the final `develop` commit.

## Verification

- `git diff --check`
- `./gradlew help --refresh-dependencies --no-daemon --no-configuration-cache --no-build-cache`

## Future Guard

Do not tag a stable release when the target changelog section is missing or
when the release range fails `git diff --check`; fix those before publishing.
