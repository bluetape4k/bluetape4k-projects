# WIP - bluetape4k-projects

Snapshot: 2026-05-22 KST
Scope: open GitHub issues assigned to `debop`, created on or after 2026-01-01.
Open count: 6 issues.

## Refresh Notes

Verified with `gh issue list --state open --assignee debop` on 2026-05-22 KST
after closing duplicate issue `#603`. All remaining open assigned issues are in
the `backlog` milestone. The `1.9.0` milestone has no open issues.

## Recently Completed

- `#493` added the near-cache backend capability matrix and conformance coverage.
- `#474` removed deprecated Kafka, OpenTelemetry, cache, Redis, and Resilience4j aliases for the 1.9.0 breaking-change line.
- `#596` added a reusable `EtcdServer` Testcontainers launcher.
- `#595` fixed the Nightly failures in IO HTTP, Elasticsearch-backed search messaging, and Memgraph-backed graph tests.
- `#602` / PR #604 stabilized the Memgraph Bolt nightly test, Full Nightly
  passed, and the `1.9.0` release workflow completed successfully.
- `#580` marked Fory-backed Kafka/Kafka4 codecs as `@BluetapeDelicateApi` and documented the deserialization trust boundary.
- PR #600 prepared the source version for `1.9.0-SNAPSHOT`, and the snapshot publish workflow completed successfully.

## Current Direction

The repository is in the post-1.9.0 cleanup lane. Open the next development
cycle by moving the committed base version to `1.10.0`; keep
`snapshotVersion=` empty in `gradle.properties` and pass
`-PsnapshotVersion=-SNAPSHOT` only for SNAPSHOT publishing.

After the snapshot bump PR lands, resume the IO HTTP performance/design backlog.
Prioritize API and architecture decisions before benchmark polish so later
performance work does not lock in the wrong client surface.

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P1 | [#586](https://github.com/bluetape4k/bluetape4k-projects/issues/586) HC5-first HTTP client surface | M | Architecture/API decision with migration risk; settle before adding more tuning helpers. |
| P1 | [#582](https://github.com/bluetape4k/bluetape4k-projects/issues/582) production HTTP client tuning defaults | M | Feature work that can become the shared default surface for IO HTTP users. |
| P1 | [#583](https://github.com/bluetape4k/bluetape4k-projects/issues/583) cache configuration DSL and cache metrics helpers | M | User-facing feature that depends on the tuning/defaults shape. |
| P2 | [#589](https://github.com/bluetape4k/bluetape4k-projects/issues/589) benchmark-driven HTTP component performance epic | L | Umbrella for the IO HTTP performance lane; split execution through child issues. |
| P2 | [#584](https://github.com/bluetape4k/bluetape4k-projects/issues/584) HTTP benchmarks against WebFlux mock server | S | Benchmark realism; useful after the target client surface is clear. |
| P2 | [#585](https://github.com/bluetape4k/bluetape4k-projects/issues/585) CPU and GC profiling for HTTP benchmarks | S | Profiling depth; pair with benchmark scenarios to avoid isolated numbers. |

## Dependency Map

```text
#586 HC5-first HTTP client surface
  -> informs #582 production tuning defaults
  -> informs #583 cache DSL and metrics helper shape

#589 IO HTTP performance epic
  -> #584 WebFlux mock-server benchmark scenarios
  -> #585 CPU/GC profiling
  -> should consume decisions from #586/#582/#583 before broad optimization
```

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Release prep | 1 | Done; keep only post-release snapshot bump and Central propagation checks. |
| IO HTTP API/design | 1 | `#586` first. |
| IO HTTP feature | 1 | `#582`, then `#583`. |
| IO HTTP performance | 1 | `#589` umbrella; execute `#584/#585` after design decisions. |

## Cleanup Actions

| Candidate | Action |
|---|---|
| `1.9.0` milestone | Keep closed; duplicate `#603` was closed after PR #604 and the release workflow succeeded. |
| `backlog` milestone | Keep `#582/#583/#584/#585/#586/#589` open for the next development lane. |
| release worktrees | Remove release-prep worktrees after PR merge and final verification. |
