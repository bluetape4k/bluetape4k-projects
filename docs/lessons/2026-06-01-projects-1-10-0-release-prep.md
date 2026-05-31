# projects 1.10.0 release prep

## Context

`bluetape4k-projects` needed the 1.10.0 stable release gate prepared after the
remaining milestone issue was confirmed to be completed and unrelated to the
repository release scope.

## Decision

Prepare release metadata only: set `baseVersion=1.10.0`, keep
`snapshotVersion=` empty, and add a curated `CHANGELOG.md` section before
creating the release-prep PR.

## Outcome

The release-prep branch records the 1.10.0 user-facing changes from the Ktor
module family, performance work, cancellation fixes, release guards, and Zip
Slip hardening.

## Verification

- Confirmed the worktree started from clean `origin/develop`.
- Confirmed `release.yml` requires `version`, optional `diagnoseSigning`, and
  optional `catalogRef`.
- Confirmed `snapshotVersion=` remains empty for stable release dispatch.

## Next time

Do not dispatch the stable release until the release-prep PR has passed CI,
Nightly/snapshot validation is current for the merged release state, and Maven
Central verification is ready for the exact `1.10.0` BOM and representative
module POMs.
