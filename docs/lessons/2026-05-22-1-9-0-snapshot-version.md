# 2026-05-22 1.9.0 Snapshot Version

Context: The 1.9.0 milestone had no remaining open issues, and the next step was
to publish a Maven Central SNAPSHOT from `develop`.

Decision: Move `baseVersion` from `1.8.1` to `1.9.0` before publishing, keeping
`snapshotVersion=` empty in committed properties and passing
`-PsnapshotVersion=-SNAPSHOT` only for SNAPSHOT publishing.

Outcome: Snapshot publishing can use the normal
`publishAggregationToCentralSnapshots` workflow and produce `1.9.0-SNAPSHOT`
artifacts from a reproducible commit when the SNAPSHOT suffix property is set.

Verification: Check Gradle project version and GitHub Publish Snapshot workflow
after the version bump reaches `develop`.

Future guard: Do not publish a 1.9.0 release-candidate snapshot by overriding
`-PbaseVersion` only at command time; commit the version source first.
