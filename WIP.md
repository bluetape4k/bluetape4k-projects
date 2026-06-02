# WIP - bluetape4k-projects

Snapshot: 2026-06-02 KST
Scope: post-1.10.0 release train version alignment.
Open count: 12 issues.

## Refresh Notes

The `1.10.0` stable line has been published and consumed by
`bluetape4k-dependencies` `1.2.0`. Development now moves to the next minor
line, `1.11.0`, with `snapshotVersion=` kept empty for workflow-injected
snapshot publication.

## Recently Completed

- `#493` added the near-cache backend capability matrix and conformance coverage.
- `#474` removed deprecated Kafka, OpenTelemetry, cache, Redis, and Resilience4j aliases for the 1.9.0 breaking-change line.
- `#596` added a reusable `EtcdServer` Testcontainers launcher.
- `#607` / `#608` shipped the 1.9.1 UTF-8 truncation and catalog-governance fixes.
- `#595` fixed the Nightly failures in IO HTTP, Elasticsearch-backed search messaging, and Memgraph-backed graph tests.
- `#620` coordinated the downstream BOM/catalog handoff after `bluetape4k-projects` `1.9.1`.
- `#580` marked Fory-backed Kafka/Kafka4 codecs as `@BluetapeDelicateApi` and documented the deserialization trust boundary.
- PR #600 prepared the source version for `1.9.0-SNAPSHOT`, and the snapshot publish workflow completed successfully.

## Current Direction

The repository is in the `1.11.0` development lane after the `1.10.0` stable
release. Keep `snapshotVersion=` empty in `gradle.properties` and pass
`-PsnapshotVersion=-SNAPSHOT` only for SNAPSHOT publishing.

Work selection should use the next minor line for new feature work unless a
specific patch milestone is opened for release fallout.

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P1 | [#586](https://github.com/bluetape4k/bluetape4k-projects/issues/586) HC5-first HTTP client surface | M | Architecture/API decision with migration risk; settle before adding more tuning helpers. |
| P1 | [#582](https://github.com/bluetape4k/bluetape4k-projects/issues/582) production HTTP client tuning defaults | M | Feature work that can become the shared default surface for IO HTTP users. |
| P1 | [#583](https://github.com/bluetape4k/bluetape4k-projects/issues/583) cache configuration DSL and cache metrics helpers | M | User-facing feature that depends on the tuning/defaults shape. |
| P1 | [#610](https://github.com/bluetape4k/bluetape4k-projects/issues/610) Ktor module family boundaries | L | Architecture/API decision for the `1.10.0` Ktor module family; settle before scaffolding. |
| P1 | [#611](https://github.com/bluetape4k/bluetape4k-projects/issues/611) Scaffold Ktor module family in Gradle | M | Depends on #610 and sets the build/module shape for later Ktor work. |
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

#609 Ktor module family epic
  -> #610 design boundaries
  -> #611 Gradle scaffold
  -> #612/#613/#614 implementation modules
  -> #615 example migration
  -> #616 CI/documentation/release metadata
```

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| Release prep | 1 | Done for 1.9.1; keep only post-release version-line cleanup. |
| IO HTTP API/design | 1 | `#586` first. |
| IO HTTP feature | 1 | `#582`, then `#583`. |
| IO HTTP performance | 1 | `#589` umbrella; execute `#584/#585` after design decisions. |
| Ktor module family | 1 | `#610` first. |

## Cleanup Actions

| Candidate | Action |
|---|---|
| `1.9.0` and `1.9.1` milestones | Keep closed; do not add backlog work to completed releases. |
| `1.9.2` milestone | Keep `#582/#583/#584/#585/#586/#589` open for the IO HTTP patch lane. |
| `1.10.0` milestone | Keep `#609/#610/#611/#612/#613/#614/#615/#616` open for the Ktor module-family lane. |
| release worktrees | Remove release-prep worktrees after PR merge and final verification. |
