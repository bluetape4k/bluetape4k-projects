# 2026-06-04 Issue 707 Nightly Gradle Cache

## Context

Nightly builds across bluetape4k repositories intermittently resolved managed dependencies as `group:artifact:.` on GitHub runners.

## Decision

Disable `gradle/actions/setup-gradle` cache restore/write for Nightly jobs so scheduled runs do not reuse stale dependency-management state.

## Outcome

Every Nightly `setup-gradle` block now sets `cache-disabled: true` while keeping explicit Gradle dependency refresh.

## Verification

- Audited `.github/workflows/nightly-tests.yml`: setup-gradle blocks match cache-disabled blocks.
- Planned validation: `actionlint`, `git diff --check`.

## Future Rule

When a Nightly workflow uses snapshot or BOM-managed bluetape4k dependencies, keep Gradle action cache disabled unless a fresh CI proof shows cache restore cannot replay stale metadata.
