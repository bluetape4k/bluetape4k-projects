# Top-level docs sync

## Context

The root README still mixed older monorepo-era module descriptions with the current split-repository and module layout.

## Decision

Use `settings.gradle.kts`, module build files, and module README files as the source of truth for top-level README, agent guidance, and WIP updates. Use merged PR history, not planned work, as the source of truth for CHANGELOG entries.

## Outcome

The root README pair now points compression to `bluetape4k-io`, lists current infra/testing/utility/example modules, removes `nats` from the removed-module list, and keeps agent-facing module guidance in English. The CHANGELOG now covers merged PRs after #347, including WIP-completed idgenerator examples and workflow fixes.

## Verification

Checked module references against current module directories, searched for stale README tokens, and compared CHANGELOG PR references against merged PRs after #347.

## Future guidance

When modules are added, moved, removed, or split out, update `README.md`, `README.ko.md`, `CLAUDE.md`, `AGENTS.md`, `WIP.md`, and `CHANGELOG.md` in the same documentation pass.
