# Issue #1039 Serializer Allocation Benchmark 설계

- Issue: [#1039 Prove ByteBuffer serializer allocation reductions and document limits](https://github.com/bluetape4k/bluetape4k-projects/issues/1039)
- Parent: [#754 Add ByteBuffer-oriented serializer APIs to reduce allocation pressure](https://github.com/bluetape4k/bluetape4k-projects/issues/754)
- Milestone: `1.12.0`
- Branch: `feat/issue-754-allocation-proof`
- Baseline authority: `origin/develop@09402f87752412031266059547be7a2f6351268d`
- Primary decision metric: normalized allocation, `gc.alloc.rate.norm` in B/op
- Delivery stop: exact-head pull request is merge-ready; merge requires fresh approval

## 1. Problem

The serializer stack now exposes caller-owned `ByteBuffer` APIs and several backends implement lower-copy paths. Functional tests prove compatibility, buffer boundaries, wire behavior, and failure policy, but they do not prove that an optimized path reduces allocation relative to the existing `ByteArray`
API under equivalent conditions.

Issue #1039 must provide reproducible allocation and GC evidence without turning short local measurements into universal throughput claims. It must also document which buffer paths are optimized, which are compatibility fallbacks, and where no allocation claim is justified.

## 2. Authority And Constraints

The live issue body, the approved #754 serializer-stack design, and the merged backend implementations are the scope authorities. This design does not change the serializer API or authorize release, publication, tag, repository-setting, or credential operations.

### Included

- A standalone `benchmark/serializer-benchmark` module.
- `kotlinx-benchmark` with its JMH backend and JMH GC profiler evidence.
- Equivalent `ByteArray`, compatibility-default, and optimized `ByteBuffer`
  cells for representative binary, JSON, and Avro serializers.
- Separate serialization and deserialization measurements.
- Two fresh evidence runs with raw JSON, a compact derived CSV, environment metadata, and an allocation-focused report.
- Public KDoc, representative English/Korean README pairs, the benchmark report index, and `CHANGELOG.md`.

### Excluded

- Changes to existing `ByteArray` signatures, binary compatibility, wire formats, null policy, security configuration, registration, or exception behavior.
- Claims based only on throughput, a single run, or a compatibility fallback.
- Per-operation library-owned direct-buffer allocation or a recommendation that callers allocate a new direct buffer for every call.
- Compressor, Redis/cache, Protobuf, and Kafka work tracked by #755-#758.
- New external services, Testcontainers, profiler dumps, charts, or benchmark dependencies beyond versions already governed by the repository catalogs.

## 3. Considered Approaches

### 3.1 Chosen: standalone serializer benchmark module

Add `benchmark/serializer-benchmark` and give it benchmark-only project dependencies on the serializer modules. This keeps cross-backend measurement out of production modules, permits one shared payload and protocol, and follows the existing `benchmark/protobuf-codec-benchmark` layout.

### 3.2 Rejected: extend the `io/io` test benchmark

The existing `BinarySerializerBenchmark` is throughput-oriented and lives inside a production module. Adding Jackson, Fastjson2, and Avro dependencies there would reverse module boundaries and couple production `io/io` to higher serializer modules.

### 3.3 Rejected: one benchmark module per backend

Per-backend modules would preserve narrow dependencies but duplicate payload, JMH configuration, evidence extraction, and comparison rules. That duplication would make same-condition comparisons and documentation drift harder to audit.

## 4. Module Architecture

`benchmark/serializer-benchmark` follows the repository benchmark convention:

- Kotlin `plugin.allopen` opens JMH `@State` classes.
- `kotlinx-benchmark` owns the benchmark source set and generated JMH target.
- The benchmark compilation associates with `main` using the existing Gradle pattern.
- Benchmark-only dependencies point to `:bluetape4k-io`,
  `:bluetape4k-jackson2`, `:bluetape4k-jackson3`,
  `:bluetape4k-fastjson2`, and `:bluetape4k-avro`.
- The module is excluded from publication and production coverage by the repository's existing `-benchmark` rules.
- No production module depends on the benchmark module.

The module contains four focused units:

1. `SerializerBenchmarkPayload` defines one deterministic, benchmark-local object shape accepted by the selected backends. It uses stable scalar, string, collection, and byte payload fields, supports JDK serialization, and provides deterministic semantic validation.
2. Backend fixtures construct serializers once with their normal production configuration and precompute serialized inputs outside timed methods.
3. Serialization benchmarks compare returned `ByteArray` values, explicit compatibility defaults, and writes into reused caller-owned buffers.
4. Deserialization benchmarks compare precomputed `ByteArray` input, compatibility-default buffer input, and backend-optimized buffer input.

Compatibility-default controls are benchmark-only delegating implementations. They expose the same backend's existing `ByteArray` behavior while deliberately using the public interface default buffer method. They do not change production dispatch and are labeled `ergonomic-only` in code and reports.

## 5. Backend Matrix

The benchmark matrix is representative rather than exhaustive. Every cell uses the same logical payload and equivalent backend configuration.

| Family    | Representative path    | Serialize cell                | Deserialize cell                                                   | Claim eligibility           |
|-----------|------------------------|-------------------------------|--------------------------------------------------------------------|-----------------------------|
| JDK       | `JdkBinarySerializer`  | ByteArray, default, optimized | ByteArray, default, optimized                                      | Optimized cells only        |
| Kryo      | `KryoBinarySerializer` | ByteArray, default, optimized | ByteArray, default, optimized                                      | Optimized cells only        |
| Fory      | `ForyBinarySerializer` | ByteArray, fallback           | ByteArray, optimized bounded input                                 | Input optimized only        |
| Jackson 2 | `JacksonSerializer`    | ByteArray, default, optimized | ByteArray, default, optimized                                      | Optimized cells only        |
| Jackson 3 | `JacksonSerializer`    | ByteArray, default, optimized | ByteArray, default, optimized                                      | Optimized cells only        |
| Fastjson2 | JSONB serializer       | ByteArray, fallback           | ByteArray, optimized array-backed input, direct/read-only fallback | Array-backed input only     |
| Avro      | reflect serializer     | ByteArray, default, optimized | ByteArray, default, optimized                                      | Measured reflect cells only |

Unmeasured Jackson formats and Avro generic/specific/list variants retain their functional documentation but receive no allocation-reduction claim from this benchmark. A backend cell that executes a production fallback remains useful as an ergonomic control and is never relabeled as optimized based on its score.

## 6. Timed-Path Rules

Serialization and deserialization are separate benchmarks so a round trip cannot hide which side allocates.

- Serializer construction, payload construction, schema setup, wire generation, and correctness checks occur at trial setup.
- Output buffers are allocated once per thread state. Invocation setup restores only position and limit; it does not allocate, resize, or replace storage.
- Deserialization inputs are precomputed. Buffer APIs receive stable bounded inputs and must preserve caller position as defined by the production contract.
- Heap and direct buffers are both caller-owned and preallocated. The benchmark never recommends allocating a direct buffer per operation.
- Timed methods perform only the selected public API call and consume the result through JMH `Blackhole` or an equivalent observable return.
- Buffer capacity is fixed from setup evidence with explicit headroom. A cell that overflows is failed rather than resized inside the measurement.
- Semantic equality, decoded type, buffer position/limit behavior, and expected serialized length are asserted before evidence runs.

## 7. Measurement Protocol

### 7.1 Metrics

The primary metric is JMH GC profiler `gc.alloc.rate.norm` in B/op. It directly supports the issue's allocation claim.

Secondary evidence records:

- `gc.alloc.rate` in MB/s;
- `gc.count` when collection occurs;
- allocations/op only when the selected JDK/JMH profiler exposes a stable count;
- throughput score and error as diagnostic context only.

A zero `gc.count` is retained and explained; it is not converted into missing data. If allocations/op is unavailable, the report records `N/A` and the profiler limitation rather than deriving a synthetic count from bytes.

### 7.2 Execution Shape

The module first runs a wiring smoke test:

- one fork;
- one warmup iteration;
- one measurement iteration;
- one thread.

Each evidence run then uses:

- one thread;
- two forks;
- three warmup iterations;
- five measurement iterations;
- one second per warmup and measurement iteration;
- JMH GC profiler enabled with `-prof gc`;
- JSON result format.

The exact generated benchmark task and JMH jar path are discovered with
`./gradlew :serializer-benchmark:tasks --all` after module registration. The final report records the literal commands that were executed; task names and jar paths are not guessed in advance.

### 7.3 Fresh-Run Rule

Run IDs use UTC timestamps such as `run-20260718T120000Z` and are stored under the corresponding directory:

```text
docs/benchmarks/raw/issue-1039/run-20260718T120000Z/
```

Each directory contains the raw JMH JSON, a compact derived CSV, and environment metadata. Two independent runs execute sequentially from the same commit, JDK, machine, payload, serializer configuration, and JMH flags.

### 7.4 Claim Rule

For the same backend, direction, payload, and configuration, an optimized
`ByteBuffer` cell may be described as reducing allocation only when both fresh runs report at least 5% lower `gc.alloc.rate.norm` than the corresponding
`ByteArray` baseline.

- Both runs must agree on direction and independently meet the 5% threshold.
- Functional, compatibility, wire, security, and resource tests must remain green.
- Throughput and `gc.count` do not override the primary allocation result.
- A smaller, mixed-direction, missing, or invalid difference is reported as
  `inconclusive`; no reduction claim is made.
- Compatibility-default and production-fallback cells are always excluded from reduction claims, even if their observed number is lower.

## 8. Evidence And Documentation

The numeric source of truth is:

`docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md`

It records scope, literal commands, run conditions, both run IDs, raw artifact paths, B/op comparison tables, secondary metrics, diagnostic throughput, claim decisions, unmeasured cells, and limitations. `docs/benchmarks/README.md`
links the report. No chart is required because reviewable tables and raw JSON are the authoritative artifacts.

Documentation updates cover:

- the new benchmark module's `README.md` and `README.ko.md`;
- affected public ByteBuffer KDoc;
- representative English/Korean README pairs in `io/io`, `io/jackson2`,
  `io/jackson3`, `io/fastjson2`, and `io/avro`;
- `CHANGELOG.md`.

The report owns measured numbers. Module READMEs summarize the result and link to the report so English/Korean copies do not become competing numeric sources. Every public-facing document states:

- output writes from initial position to limit and advances position only on success;
- input consumes the initial remaining range while preserving caller state;
- overflow, read-only, and failure rollback behavior;
- Kotlin and Java method usage;
- optimized versus fallback cells;
- caller-owned buffer lifecycle guidance;
- the evidence environment and limits;
- explicit deferral of #755-#758.

## 9. Failure Handling

1. **Benchmark wiring or registration
   failure:** stop before evidence collection, repair module registration, and rerun `projects`, task discovery, compilation, and smoke proof.
2. **Payload or backend
   inequivalence:** fail the affected setup when decoded semantics, configuration, wire expectations, or bounded-buffer behavior do not match. Do not retain that cell as comparable evidence.
3. **Overflow or resize
   pressure:** fail the cell and correct setup capacity. Never resize or allocate replacement storage in timed code.
4. **Run disagreement or sub-threshold delta:** publish the raw result as
   `inconclusive` and remove any allocation-reduction wording for that cell.
5. **Profiler or environment
   loss:** record the run as invalid, preserve its diagnostic artifact separately, and execute a new fresh run ID. An invalid run cannot satisfy the two-run rule.
6. **Documentation
   drift:** treat a mismatch between code dispatch, KDoc, locale READMEs, and the benchmark report as a delivery blocker.

## 10. Compatibility And Migration

This issue adds a benchmark module and documentation only. Existing production serializer behavior and public signatures do not migrate. Callers may continue using `ByteArray` APIs unchanged. Callers that already own reusable buffers may adopt documented optimized cells, but the report does not prescribe a global default buffer type or lifecycle.

Adding the module triggers the complete registration chain: Gradle project discovery, benchmark publication/coverage exclusion, README locale set, module maps, CI and Nightly path handling, generated checks, and
`./gradlew projects`. Unaffected BOM/catalog constraints remain unchanged because the benchmark artifact is not published.

## 11. Verification Strategy

Verification proceeds in dependency order:

1. Prove shared payload semantic equality and benchmark-cell setup contracts.
2. Compile the benchmark source set and run the discovered smoke task/JMH jar.
3. Run the two sequential fresh evidence executions and derive compact tables.
4. Run affected serializer module tests and the serializer buffer ABI proof.
5. Check English/Korean documentation parity and benchmark report/raw links.
6. Run Detekt, `git diff --check`, module registration checks, and a proportional Gradle build.
7. Review the exact evidence and all claims with P0=0 and P1=0 before PR creation.

No Testcontainers, external service, native/JNI, emulator, chart, release, or publication check is applicable to this scope.

## 12. Acceptance Criteria

- `benchmark/serializer-benchmark` follows the repository
  `kotlinx-benchmark` pattern and is correctly auto-registered.
- Serialization and deserialization use separate, equivalent comparison cells.
- Timed optimized cells reuse caller-owned buffers and contain no setup, validation, resize, or per-call direct-buffer allocation.
- Raw allocation/GC evidence, environment metadata, and two fresh run IDs are committed under `docs/benchmarks/raw/issue-1039/`.
- Every positive allocation claim satisfies the two-run, same-direction, 5% normalized-allocation rule.
- Fallback cells are visibly labeled ergonomic-only and excluded from claims.
- Throughput is labeled diagnostic and never used as the acceptance decision.
- Public KDoc, representative English/Korean README pairs, benchmark docs, and
  `CHANGELOG.md` match the measured optimized/fallback matrix.
- Existing serializer tests, ABI proof, security/wire/resource contracts, Detekt, registration checks, and the proportional build pass.
- #755-#758 and all release, tag, publish, settings, and credential changes remain outside the diff.
- Final review reports P0=0 and P1=0 on the exact PR head.

## 13. Definition Of Done

- The benchmark protocol is repeatable from committed commands and artifacts.
- Both evidence runs are fresh, valid, and comparable.
- Claims are no broader than the measured backend cells and environment.
- Documentation explains buffer position, limit, overflow, rollback, Java and Kotlin usage, optimized/fallback behavior, evidence, and limitations.
- Module, benchmark, documentation, ABI, static-analysis, and proportional build gates pass with fresh evidence.
- The issue-linked pull request mirrors milestone, labels, and assignee, ends its body with `## DoD Status`, and reaches merge-ready exact-head state.
- Merge, release, tag, snapshot, publication, and cleanup remain pending their separate authority gates.
