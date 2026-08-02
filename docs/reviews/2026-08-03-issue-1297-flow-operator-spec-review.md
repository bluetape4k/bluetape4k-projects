# Issue #1297 Step 2-R Spec Review

## Review scope and method

- Issue: #1297, `bluetape4k-coroutines` Flow operator parity.
- Artifact: `docs/superpowers/specs/2026-08-03-flow-operator-parity-design.md`.
- Repository evidence: existing `windowed.kt`, `bufferingDebounce.kt`,
  `concatMapEager.kt`, `onBackpressureDrop.kt`, coroutine test dependencies,
  and benchmark registration.
- Review date: 2026-08-03.
- Execution: six required lenses were run sequentially in the main session as
  the user requested inline execution. The workflow receipt records this as
  the `main-review` fallback lane; no independent implementation lane was
  delegated.

The review was initially blocked by three contract ambiguities: same-instant
timer precedence, the lifecycle of returned windows, and what the benchmark
could prove. Those findings were repaired in the design before integration.

## Six-lens findings and rerun

| Priority | Lens | Evidence | Required edit | Rerun result |
|---|---|---|---|---|
| P1 | Performance | The first plan wording could be read as proving timer firing with a one-day benchmark timeout. | Separate timer registration/list-allocation evidence from virtual-time timer-firing tests. | PASS; spec lines 197-199 and plan lines 528-533 make the boundary explicit. |
| P1 | Stability | Kotlin `select` can have both receive and timeout clauses ready at the same virtual instant. | Register receive first, document biased-select precedence, and add a deterministic tie test. | PASS; spec lines 28-31 and 155-159, plan lines 171-194. |
| P1 | Developer/API | `windowTimeout` returned a `Flow` without stating whether it was live or replayable. | Define completed cold snapshots and document the migration difference from live windows. | PASS; spec lines 100-104 and 179-184, plan lines 159-163. |
| P1 | User/caller | Invalid arguments and upstream cancellation were only described, not shown as exact tests. | Add concrete invalid-size/duration and `take(1)`/`finally` tests. | PASS; plan lines 165-194. |
| P2 | Operator/Ops | External Rx/Reactor runtime interoperability is not exercised by this module-only change. | Keep it explicitly outside the slice and retain a `Not-tested` release risk. | DEFERRED with explicit scope in spec lines 197-202 and plan Task 6. |
| P3 | Security | No auth, secrets, external input trust boundary, persistence, or network side effect is introduced by the design. | Record N/A with scope evidence; do not add security machinery. | N/A; no open finding. |

## Integrated contract checks

| Check | Evidence | Result |
|---|---|---|
| Boundary and validation | `bufferTimeout`/`windowTimeout` require positive size and duration; timeout requires a positive duration; no empty batches/windows. | PASS |
| Completion and failure | Completion emits one non-empty partial snapshot; upstream failure drops the in-flight snapshot and preserves the cause. | PASS |
| Cancellation | Producer/timer are cancelled together; `CancellationException` is never converted to a data error; fallback follows upstream cleanup. | PASS |
| Timer determinism | All new timer contracts use coroutine suspension and `runTest`; wall-clock `System.nanoTime` in `bufferingDebounce.kt` is not reused. | PASS |
| Ordered bounded mapping | Existing overload remains source-compatible; bounded overload uses explicit concurrency and per-inner capacity with `finally` cleanup. | PASS |
| Scope and compatibility | Additive APIs only; standard Flow operators remain mappings/non-goals; deferred families have one linked follow-up issue. | PASS pending Task 1 live duplicate check |
| Public documentation | Korean-first KDoc plus both README locales are required; GitHub artifacts remain English. | PASS as a plan gate; implementation documentation remains pending |

## Verdict

- P0: **0**
- P1: **0** after the repairs above
- P2: **1 deferred** (external runtime interoperability; not required for this
  module-local slice)
- P3: **0 open**
- Step 2-R status: **PASS** for the design artifact.
- Implementation status: **PENDING**; this review does not claim code or
  runtime verification.

The next gate is Step 3-R plan review. Implementation may start only after the
plan review closes with P0=0 and P1=0 and the workflow receipt has the matching
evidence.
