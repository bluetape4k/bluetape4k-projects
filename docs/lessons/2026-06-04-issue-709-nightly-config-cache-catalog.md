# 2026-06-04 Issue 709 Nightly Config Cache And Catalog

## Context

Nightly workflows use snapshot and BOM-managed dependencies, so stale Gradle/configuration state can surface versionless dependency coordinates.

## Decision

Keep Nightly Gradle commands on `--no-configuration-cache` and keep local bluetape4k aliases versioned through their BOM ref.

## Outcome

Nightly commands no longer rely on configuration cache during dependency refresh, and repo-local catalog aliases avoid `group:artifact:.` coordinates.

## Verification

- Planned: `actionlint`, `git diff --check`, command audit, catalog alias audit.

## Future Rule

For Nightly jobs that refresh snapshots, disable both Gradle action cache and configuration cache unless a repo-specific proof says otherwise.
