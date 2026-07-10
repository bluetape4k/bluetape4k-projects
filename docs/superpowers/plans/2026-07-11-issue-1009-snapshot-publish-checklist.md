# Issue #1009 Snapshot Publish Checklist

## Release Identity

| Field | Value |
| --- | --- |
| Flow | Routine snapshot followed by one AWS consumer validation |
| Producer | `bluetape4k-projects` at `f38c8aef6d4dd2b07d148ec97265f4244ae9add4` |
| Branch | `test/issue-1009-floci-non-reusable` |
| Version | `1.11.1-SNAPSHOT` |
| Changed artifact | `io.github.debop:bluetape4k-testcontainers` |
| Publication surface | Root `nmcpPublishAggregationToCentralPortalSnapshots` workflow task |
| Consumer | `bluetape4k-aws` issue #369; its catalog already selects `1.11.1-SNAPSHOT` |
| Authority | User instruction on 2026-07-11: use the current snapshot and proceed |

## Topology

`bluetape4k-projects` (stable-library repository, snapshot producer)
`->` `bluetape4k-aws` (consumer validation only)

The edge is a snapshot dependency. No catalog ref or stable release is part of
this change. AWS mutex removal is explicitly deferred until this central
Launcher change is publicly consumable and validated.

## Preflight

- [x] PRE-01: The selected flow is a routine snapshot, not a stable release.
- [x] PRE-02: Issue #1009 is open, assigned to `debop`, and remains the active
  implementation issue; milestone closeout is outside this snapshot scope.
- [x] PRE-04: Focused `FlociServerTest` passes at `f38c8aef6`; full module
  execution reached 422 passing tests and has one unrelated
  `OpenSearchServer$Launcher` class-loading failure.
- [x] PRE-05: `gradle.properties` has `baseVersion=1.11.1` and an empty
  `snapshotVersion`; the workflow appends `-SNAPSHOT`.
- [ ] PRE-07: Generate and audit the changed artifact POM before dispatch.
- [ ] PRE-08: Run the declared publishing/signing diagnostic.
- [ ] PRE-09: Capture the complete workflow publication artifact matrix and
  representative `bluetape4k-testcontainers` coordinates.
- [x] PRE-10: `.github/workflows/publish-snapshot.yml` at `f38c8aef6` exposes
  only optional `diagnoseSigning`; the dispatch will omit it.

## Dispatch Hold

- [ ] Branch containing `f38c8aef6` is pushed to `origin`.
- [ ] All unchecked preflight rows pass.
- [ ] Workflow schema, branch SHA, issue state, and absent snapshot metadata
  are refreshed immediately before dispatch.
- [ ] Exact workflow run reaches success.
- [ ] Central Snapshots metadata and the changed artifact POM resolve publicly.
- [ ] AWS resolves the published snapshot and passes its repository-level
  Floci-backed validation.

## Out Of Scope

- Stable release, tag, GitHub Release, and milestone closure.
- Changing the public `FlociServer` direct-construction reuse default.
- Removing the AWS root Test mutex before the consumer validation succeeds.
