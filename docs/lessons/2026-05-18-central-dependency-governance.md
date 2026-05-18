# Central Dependency Governance Sync

## Context

Downstream Dependabot PRs were updating shared dependency versions one repository at a time, creating version drift across the bluetape4k organization.

## Decision

Shared dependency versions should be changed in `bluetape4k-dependencies` first, then materialized into this repository with `sync-shared-versions.py`. This repository also ignores centrally governed dependency names in Dependabot so future PRs route through the central source of truth.

## Outcome

The local version catalog and `.github/dependabot.yml` now follow the central dependency-governance policy.

## Verification

- `sync-shared-versions.py --write --check --summary` for this repository
- `sync-dependabot-ignores.py --write --check --summary` for this repository
- `git diff --check`

## Future Guard

Do not merge repo-local Dependabot PRs for centrally governed dependencies. Update `bluetape4k-dependencies`, then sync this repository.
