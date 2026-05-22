# 2026-05-22 1.10.0 Post-Release Snapshot

Context: The `1.9.0` tag, Full Nightly gate, Publish Release workflow, and
GitHub release all completed successfully. A duplicate Memgraph nightly issue
remained open after the release.

Decision: Close the duplicate issue and move the committed base version to
`1.10.0` for the next development cycle. Keep `snapshotVersion=` empty in
`gradle.properties`; SNAPSHOT publishing must pass `-PsnapshotVersion=-SNAPSHOT`.

Outcome: `develop` can resume backlog work against the next minor line without
changing the release/snapshot versioning contract.

Verification: Check Gradle project version with and without
`-PsnapshotVersion=-SNAPSHOT`, then confirm the post-release PR checks.

Future guard: Do not reopen release work for backlog IO HTTP issues; start from
`#586` after the snapshot bump reaches `develop`.
