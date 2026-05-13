# Ignore Gradle Codex Cache

## Context

An untracked `.gradle-codex/` directory appeared after a local isolated Gradle
run. It contained Gradle wrapper/native caches, not source or build contract
changes.

## Decision

Ignore `.gradle-codex/` alongside `.gradle/` so future isolated Codex Gradle
runs do not dirty the repository.

## Outcome

The repository can stay clean after local Codex-scoped Gradle cache usage.

## Verification

- Confirmed the directory contained only Gradle cache/native files before deletion.
- Verified `git status --short --branch` was clean before adding the ignore rule.
