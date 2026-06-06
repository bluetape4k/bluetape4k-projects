# WIP - bluetape4k-projects

Snapshot: 2026-06-06 KST
Scope: 1.11.0 observability and telemetry lane, plus backlog split.
Open count: 13 issues.

## Refresh Notes

The `1.10.0` stable line has been published and consumed by downstream
repositories. Development now moves to the `1.11.0` observability and
event-telemetry lane.

Live GitHub state:

- `1.11.0`: 11 open issues.
- `backlog`: 2 open issues.
- Total open issue count: 13.

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
release. Work selection should prioritize the observability Epic and its
explicit dependency order, while keeping independent test/docs cleanup small and
isolated.

## Priority Queue

| Priority | Issue | Difficulty | Notes |
|---|---|---:|---|
| P0 | [#697](https://github.com/bluetape4k/bluetape4k-projects/issues/697) WIP backlog refresh | S | Current task; close after this file reflects live milestone/backlog state. |
| P1 | [#696](https://github.com/bluetape4k/bluetape4k-projects/issues/696) shared telemetry contract | M | Required design gate before Spring Boot, Ktor, event, and example work. |
| P1 | [#702](https://github.com/bluetape4k/bluetape4k-projects/issues/702) NearJCache remove semantics | S | Independent regression-test win; safe to run before or after #696. |
| P1 | [#691](https://github.com/bluetape4k/bluetape4k-projects/issues/691) Ktor opt-in OpenTelemetry tracing | M | First implementation slice after #696; lower blast radius than Spring/event lanes. |
| P1 | [#694](https://github.com/bluetape4k/bluetape4k-projects/issues/694) Spring Boot 4 observability helpers | M | Follow #696; keep Actuator/Micrometer conventions application-owned. |
| P2 | [#695](https://github.com/bluetape4k/bluetape4k-projects/issues/695) event publish/consume telemetry helpers | M | Follow #696 and reuse decisions proven by HTTP telemetry work. |
| P2 | [#692](https://github.com/bluetape4k/bluetape4k-projects/issues/692) observability examples | M | Depends on Spring Boot, Ktor, and event helper issues. |
| P2 | [#699](https://github.com/bluetape4k/bluetape4k-projects/issues/699) Copilot instruction alignment | S | Independent docs/agent-guidance cleanup. |
| P2 | [#698](https://github.com/bluetape4k/bluetape4k-projects/issues/698) HC5 interceptor ordering examples | S | Independent test/docs cleanup; verify expected HC5 ordering before editing behavior. |
| P2 | [#701](https://github.com/bluetape4k/bluetape4k-projects/issues/701) FutureUtils virtual-thread TODO | M | API-boundary cleanup; preserve compatibility and public KDoc. |
| P3 | [#690](https://github.com/bluetape4k/bluetape4k-projects/issues/690) observability telemetry Epic | L | Umbrella issue; keep open until child issues complete. |

## Backlog Queue

| Issue | Notes |
|---|---|
| [#700](https://github.com/bluetape4k/bluetape4k-projects/issues/700) Central snapshot task naming audit | Release-documentation cleanup; keep outside 1.11.0 unless snapshot workflow drift becomes blocking. |
| [#706](https://github.com/bluetape4k/bluetape4k-projects/issues/706) data-r2dbc pool benchmark validation mode | Performance/benchmark lane; promote only when the benchmark workflow is the active focus. |

## Dependency Map

```text
#690 observability and event telemetry Epic
  -> #696 shared telemetry contract
    -> #691 Ktor opt-in OpenTelemetry tracing
    -> #694 Spring Boot 4 observability helpers
    -> #695 event publish/consume telemetry helpers
      -> #692 Prometheus and OTLP examples

#697 WIP refresh
  -> update this file before choosing the next 1.11.0 execution slice

#702 NearJCache remove semantics
#699 Copilot instruction alignment
#698 HC5 interceptor ordering examples
#701 FutureUtils virtual-thread TODO
  -> independent of the observability Epic
```

## WIP Limits

| Lane | Limit | Current next |
|---|---:|---|
| WIP / management | 1 | `#697`; close after this refresh is merged. |
| Observability design | 1 | `#696` first. |
| Observability implementation | 1 | `#691`, then `#694`, then `#695`. |
| Observability examples | 1 | `#692` only after helper APIs exist. |
| Independent cleanup | 1 | Prefer `#702`; otherwise `#699`, `#698`, or `#701`. |
| Backlog | 0 | Keep `#700` and `#706` parked until explicitly promoted. |

## Cleanup Actions

| Candidate | Action |
|---|---|
| `#697` | Close only after the WIP refresh is merged or otherwise accepted. |
| `#690` | Keep open as the 1.11.0 umbrella until child issues are complete. |
| `backlog` milestone | Keep `#700` and `#706` out of the 1.11.0 lane unless priority changes. |
| Old 1.9.x / 1.10.0 queue references | Treat as historical context; do not use them for current work ordering. |
