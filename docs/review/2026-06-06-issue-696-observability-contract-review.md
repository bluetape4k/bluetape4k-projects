# Issue #696 Observability Contract 검토

Date: 2026-06-06
Scope: `docs/superpowers/specs/2026-06-06-issue-696-observability-telemetry-contract-design.md`

## Review Method

Local Step 2-R review using the `bluetape4k-full-feature` spec review frame:
developer, security, Ops/SRE, user/caller, and 7-tier risk perspectives.

## 증거

- Issue body: GitHub issue #696.
- Repo evidence:
  - `infra/micrometer/README.md`
  - `infra/opentelemetry/README.md`
  - `ktor/observability/README.md`
  - `ktor/observability/src/main/kotlin/io/bluetape4k/ktor/observability/*`
  - `docs/lessons/2026-05-28-issue-613-ktor-observability.md`
- Official docs:
  - Spring Boot observability and metrics reference.
  - Micrometer Observation reference.
  - OpenTelemetry HTTP and messaging semantic conventions.
  - Ktor CallId, CallLogging, Micrometer metrics, and OpenTelemetry server docs.

## 발견 사항

| Perspective | P0 | P1 | P2 | P3 | Notes |
|---|---:|---:|---:|---:|---|
| Developer/API | 0 | 0 | 0 | 0 | Contract is implementable through existing Observation/OTel/Ktor boundaries. |
| Security | 0 | 0 | 0 | 0 | Raw headers, payloads, query strings, and secrets are forbidden by default. |
| Ops/SRE | 0 | 0 | 0 | 0 | Metric cardinality, exporter ownership, and Prometheus endpoint split are explicit. |
| User/caller | 0 | 0 | 0 | 0 | Spring Boot and Ktor integration ownership boundaries are clear. |
| Tier 1 Security | 0 | 0 | 0 | 0 | Sensitive data and correlation ID handling are covered. |
| Tier 2 Ops/SRE | 0 | 0 | 0 | 0 | Backend/exporter ownership remains application-owned. |
| Tier 3 Structural | 0 | 0 | 0 | 0 | No new modules or dependencies in this issue. |
| Tier 4 Kotlin/API | 0 | 0 | 0 | 0 | Follow-up API boundaries prefer `ObservationRegistry` or app-owned tracer inputs. |
| Tier 5 Testability | 0 | 0 | 0 | 0 | Follow-up issue acceptance checks include success/error/cancellation cases. |
| Tier 6 Performance | 0 | 0 | 0 | 0 | Cardinality explosion risk is mitigated by default-low-cardinality rules. |
| Tier 7 Docs/Evidence | 0 | 0 | 0 | 0 | Official and repo evidence are linked in the spec. |

## Applied Fixes

| Finding | Severity | Resolution |
|---|---|---|
| Ktor official OpenTelemetry example uses an alpha instrumentation artifact; #691 should not silently adopt it. | P1 | Spec now requires #691 to decide whether to adopt, optionally wrap, or avoid that dependency and to keep it opt-in if used. |

## Integrated Verdict

P0 = 0
P1 = 0

Step 2-R passes. P2/P3 follow-up items are not required for this design-only PR.
