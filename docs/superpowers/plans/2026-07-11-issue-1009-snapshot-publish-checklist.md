# Issue #1009 Snapshot Publish Checklist

## Release Identity

| Field | Value |
| --- | --- |
| Flow | Routine replacement snapshot followed by AWS consumer validation |
| Producer | `bluetape4k-projects` at `d8974d2688b543a35a0fc307cffb1b5e04257036` |
| Branch | `test/issue-1009-floci-non-reusable` |
| Version | `1.11.1-SNAPSHOT` |
| Changed artifact | `io.github.bluetape4k:bluetape4k-testcontainers` |
| Publication surface | Root `nmcpPublishAggregationToCentralPortalSnapshots` workflow task |
| Consumer | `bluetape4k-aws`; its catalog already selects `1.11.1-SNAPSHOT` |
| Authority | User instruction on 2026-07-11 to use the current snapshot and proceed |

## Topology

`bluetape4k-projects` (snapshot producer) -> `bluetape4k-aws` (consumer
validation). No catalog ref, stable release, tag, or Maven Central release is
part of this flow.

## Preflight

- [x] PRE-01: Routine snapshot selected at version `1.11.1-SNAPSHOT`.
- [x] PRE-02: Issue #1009 and PR #1010 are open, assigned to `debop`, and
  milestone `1.12.0` remains active; closeout is outside snapshot scope.
- [x] PRE-03: N/A for this routine snapshot; no stable changelog is published.
- [x] PRE-04: Local full module passed with 449 tests, 0 failures, 25 skipped;
  CI run `29125650184` passed at the exact producer SHA.
- [x] PRE-05: `baseVersion=1.11.1`, `snapshotVersion=` is empty, and the
  workflow supplies `-PsnapshotVersion=-SNAPSHOT`.
- [x] PRE-06: N/A; this change introduces no internal release references.
- [x] PRE-07: Generated `bluetape4k-testcontainers` POM has the expected
  coordinate, Apache-2.0 license, zero missing dependency versions, and zero
  dependency SNAPSHOT references. Kotlin's optional strict POM checker reports
  the pre-existing missing developer organization metadata; the declared
  snapshot workflow does not run that checker and Central accepted the same
  POM shape in run `29117088678`.
- [x] PRE-08: Snapshot run `29117088678` completed signing and publication
  successfully on the same branch; the replacement dispatch enables the
  workflow's declared `diagnoseSigning` input for a fresh secret-shape check.
- [x] PRE-09: Changed artifact matrix is
  `io.github.bluetape4k:bluetape4k-testcontainers:1.11.1-SNAPSHOT`; the root
  aggregation may republish unchanged modules, but consumer verification is
  pinned to this changed coordinate.
- [x] PRE-10: `.github/workflows/publish-snapshot.yml` at `d8974d268` declares
  only optional boolean input `diagnoseSigning`; dispatch uses only that input.

## Dispatch Hold

- [x] Branch SHA `d8974d2688b543a35a0fc307cffb1b5e04257036` is pushed to `origin`.
- [x] CI run `29125650184` completed successfully for the exact SHA.
- [x] Workflow schema, branch SHA, issue/PR state, and current Central metadata
  (`1.11.1-20260710.195152-13`) were refreshed before dispatch.
- [x] Replacement workflow run `29126253922` succeeded at `d8974d268` with
  signing diagnostics green.
- [x] Central metadata advanced to `1.11.1-20260710.220125-14`; its timestamped
  `bluetape4k-testcontainers` POM returned HTTP 200.
- [x] AWS resolved the exact `20260710.220125-14` snapshot and
  `S3KtorClientFlociTest` passed.

## Out Of Scope

- Stable release, tag, GitHub Release, and milestone closure.
- Removing repository test mutexes before consumer validation.
- Repairing the repository-wide developer organization POM metadata gap.
