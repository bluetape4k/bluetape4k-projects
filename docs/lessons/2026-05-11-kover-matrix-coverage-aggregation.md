# Kover Matrix Coverage Aggregation Needs Artifact Boundaries

## Context

The bluetape4k-projects Nightly workflow aggregates Kover coverage across a
large multi-module build with integration-heavy test groups.

## Decision or Finding

Coverage aggregation should operate on explicit Kover XML artifacts from each
matrix group. A missing artifact is either an allowed empty group or a workflow
failure; it should not silently lower aggregate coverage.

## Outcome

The Nightly workflow can separate daily smoke coverage from weekly full coverage
while keeping aggregation behavior explicit.

## Verification

- `bluetape4k-projects` Nightly workflow now has separate smoke/full scope
  handling.
- The aggregation script is tracked under `.github/scripts/`.
- The org Nightly dispatcher dry-run sends `scope=smoke` or `scope=full` to
  `bluetape4k-projects` explicitly.

## Future Guidance

- Keep Kover input artifacts named by test group.
- Document intentionally excluded integration groups.
- When coverage looks too low, inspect artifact production before changing
  thresholds.
