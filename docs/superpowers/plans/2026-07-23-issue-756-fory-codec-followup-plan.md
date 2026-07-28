# #756 Fory/FastFory Raw Codec Allocation 구현 계획

> **agentic worker용:** 필수 sub-skill: 이 계획은 `superpowers:subagent-driven-development`(권장) 또는 `superpowers:executing-plans`로 task별 구현한다. 진행 상태는 checkbox(`- [ ]`) syntax로 추적한다.

**목표:** Fory 내부 reusable `MemoryBuffer`, 모든 serializer/codec fallback contract, caller-owned `ByteBuf` state를 보존하면서 raw Fory/FastFory codec path에서 warmed steady-state caller-return/handoff `ByteArray`만 제거한다.

**아키텍처:** `ForyBinarySerializer` gains the existing caller-owned `OutputStream` fast-path seam. Lettuce continues to own bounded absolute-index writing through `BoundedByteBufOutputStream`. Redisson decode receives a narrowly scoped read-only NIO view with exactly-one copied-primary fallback; Redisson encode is guarded by a benchmark-only feasibility probe and is implemented only when that probe accepts it. Evidence promotion, charts, and README claims are disposition-gated.

**기술 스택:** Kotlin 2.3, Java 21, Apache Fory/FastFory, Netty `ByteBuf`, Lettuce, Redisson, JUnit 5, `bluetape4k-assertions`, JMH, Python evidence validators.

**Required
pattern:** Follow `bluetape4k-kotlin-patterns`: immutable local state, JUnit 5 plus `bluetape4k-assertions` (`assertFailsWith` for failure contracts), explicit `finally` cleanup, no broad exception swallowing, KDoc at public behavior boundaries, and repository formatter/import conventions.

---

## Traceability and fixed decisions

| Requirement                                        | Planned proof                                                                                                                                                              |
|----------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No claim of Fory zero-copy/internal-buffer removal | Stream tests and KDoc state only the handoff array is removed.                                                                                                             |
| Lettuce raw Fory/FastFory only                     | Target-buffer tests for `fory()`/`fastFory()`; compressed codecs remain byte-array paths.                                                                                  |
| Redisson decode preserves fallback semantics       | Contract tests cover direct success, precondition/view fallback, direct failure, normalized control/fatal failures, caller indices/marks/refcount, and FastFory asymmetry. |
| Redisson encode remains conditional                | Feasibility writes `probeDisposition`; only a green production Task 7 may write `encodeDisposition=implemented`.                                                           |
| Benchmark integrity                                | Two independent fork outputs, manifest, preflight, allocation and throughput/error-bar gate.                                                                               |
| Documentation is evidence-backed                   | Only accepted canonical cells become README/chart claims; Korean/English report parity is checked.                                                                         |

## File map

| Area                               | Files                                                                                                                                                                                              |
|------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Fory stream seam                   | `io/io/src/main/kotlin/io/bluetape4k/io/serializer/ForyBinarySerializer.kt`, `CoreBinarySerializerByteBufferTest.kt`, `ForyBinarySerializerTest.kt`, `SecureForyBinarySerializerTest.kt`           |
| Lettuce target path                | `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt`, `LettuceBinaryCodecs.kt`, `LettuceBinaryCodecBufferContractTest.kt`, issue-756 JMH/preflight/runner files |
| Redisson decode/conditional encode | `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/ForyCodec.kt`, `FastForyCodec.kt`, codec contract tests, benchmark source/scripts                                               |
| Evidence and publication           | `docs/benchmarks/raw/issue-756-fory-followup/`, `io/io/README{,.ko}.md`, `infra/lettuce/README{,.ko}.md`, `infra/redisson/README{,.ko}.md`, report/assets, aggregate validator, release checklist  |

## Task 1: Lock Fory caller-owned stream behavior with failing tests

**파일:**

- 수정: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/CoreBinarySerializerByteBufferTest.kt`
- 수정: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/ForyBinarySerializerTest.kt`
- 수정: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/SecureForyBinarySerializerTest.kt`

- [ ] Extend the existing `RecordingForyHandler` proxy with a `serialize(OutputStream, value)` branch that records the stream overload, writes fixed bytes, and rejects the one-argument `serialize(value)` branch for this contract test. Serialize to `ByteArrayOutputStream` through `serializeBinaryToStream`, then assert the returned count, bytes, and exactly one stream-overload call.
- [ ] Add byte-for-byte stream-versus-`serialize()` parity tests for default, `fast()`, and secure/registration-required Fory; preserve existing registration configuration rather than adding a new policy.
- [ ] Add a failure table and tests that distinguish primary Fory serialization failure from borrowed-target failure: primary failure follows existing `serialize(graph)` normalization, while target `IOException`, runtime failure, and `Error` each propagate with their original identity unchanged as required by `BinarySerializer.serializeBinaryToStream`. Cover counter overflow, partial write (count advances only after a successful delegate write), and tracking-target no-`flush()`/no-`close()` behavior.

실행:

```bash
./gradlew :bluetape4k-io:test --tests '*CoreBinarySerializerByteBufferTest' --tests '*ForyBinarySerializerTest' --tests '*SecureForyBinarySerializerTest'
```

예상 결과: the stream-overload contract test fails before Task 2 because Fory uses the default allocating `BinarySerializer` implementation, which calls the proxy's deliberately rejected one-argument `serialize(value)` branch.

## Task 2: Implement the Fory stream seam without changing policy

**파일:**

- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/ForyBinarySerializer.kt`
- 수정: Task 1 tests as needed for exact exception assertions

- [ ] Override `serializeBinaryToStream(graph, target)` in `ForyBinarySerializer`.
- [ ] For `null`, return `0` and perform no write, matching existing serializer semantics.
- [ ] Otherwise call the pinned `ThreadSafeFory.serialize(countingTarget, graph)` overload, where `countingTarget` wraps the borrowed target; return its exact count.
- [ ] Put the reusable adapter in `io/io`, package-private/internal to the serializer package; it must delegate `write`, never close or flush the caller target, and guard integer count overflow. It must tag target-write failures so the outer implementation can rethrow their original cause unchanged rather than confusing them with Fory serialization failures.
- [ ] Normalize only primary Fory serialization failures with the existing `serialize(graph)` policy; rethrow borrowed-target failures unchanged. Do not change `BinarySerializer` public stream KDoc or `serializeTo(ByteBuffer)` compatibility behavior.
- [ ] Update public KDoc to say the stream path avoids the returned/handoff payload array but Fory may retain its own reusable internal buffer.

실행:

```bash
./gradlew :bluetape4k-io:test --tests '*CoreBinarySerializerByteBufferTest' --tests '*ForyBinarySerializerTest' --tests '*SecureForyBinarySerializerTest'
./gradlew :bluetape4k-io:check
git diff --check
```

예상 결과: tests pass; no serializer policy, registration mode, flush, or close side effect changes.

Commit after green (Lore protocol):

```text
Expose Fory caller-owned stream output without changing serializer policy

Constraint: Fory retains an internal reusable MemoryBuffer
Rejected: ByteBuffer direct serialization | compatibility path still allocates
Confidence: high
Scope-risk: narrow
Directive: Do not describe this as Fory zero-copy serialization
Tested: focused Fory and secure Fory serializer tests; detekt
Not-tested: codec integration and JMH evidence
```

## Task 3: Prove Lettuce raw codec dispatch and preserve compressed behavior

**파일:**

- 수정: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecTest.kt` (or the existing focused codec test)
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt` only if a failing contract proves a minimal adjustment is needed
- 수정: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecs.kt` only for KDoc/factory documentation

- [ ] Add `LettuceBinaryCodecBufferContractTest` cases for raw `fory()` and `fastFory()` on heap and direct pooled targets with nonzero `readerIndex`/`writerIndex`, marks, refcount checks, bounded max capacity, and sentinel bytes before/after the write range.
- [ ] Assert reported bytes equal committed bytes, bytes round-trip, prefix/suffix sentinels remain unchanged, and writer index advances exactly once on success.
- [ ] Force insufficient-capacity and destination-write failures for both raw codecs. Assert no commit on failure and exact preservation of reader index, writer index, both marks, refcount, prefix/suffix sentinels, and the existing Lettuce no-fallback behavior.
- [ ] Assert compressed Fory/FastFory constructors remain on their existing byte-array/compression route; do not add a compression streaming path.
- [ ] Keep `BoundedByteBufOutputStream` as the only Netty-aware writer. Do not introduce NIO views into Lettuce.

실행:

```bash
./gradlew :bluetape4k-lettuce:test --tests '*LettuceBinaryCodec*' --tests '*Fory*'
./gradlew :bluetape4k-lettuce:check
git diff --check
```

예상 결과: raw codecs use the already-established bounded stream dispatch; compressed codecs retain prior behavior.

## Task 4: Add Redisson decode contract tests before implementation

**파일:**

- 수정: `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/ForyCodecTest.kt`
- 수정: `infra/redisson/src/test/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCompatibilityTest.kt`
- 수정: package-local internal constructor/factory seams for Fory serializer, fallback codec, readable-view creation, and owned output buffer allocation; all public constructors keep their current ABI/default behavior

- [ ] Replace weak `runCatching(...).shouldNotBeNull()` assertions in touched Fory codec tests with value, type, and fallback assertions.
- [ ] Establish fixtures with nonzero `readerIndex`, unread suffix, marks, and reference-count checks.
- [ ] Use deterministic factory injection, not reflection, to force direct NIO-view success, view/precondition failure using copied primary once, direct primary failure without retry, Fory fallback, and FastFory runtime-only fallback asymmetry. Verify copy constructors preserve injected runtime configuration where applicable.
- [ ] Verify `readerIndex`, `writerIndex`, marked indices, and `refCnt` are unchanged after every decode attempt.
- [ ] Preserve trusted-Redis payload test coverage for default registration-off Fory and add KDoc-warning expectation coverage if the project convention supports it.
- [ ] Add a failure matrix for Fory and FastFory: ordinary semantic failure, raw and nested `CancellationException`, raw and nested `Error`, view/precondition failure, direct destination failure, fallback-terminal failure, fallback log count, and cleanup suppression. Establish the existing `deserialize(ByteArray)` result as the oracle for fallback invocation count, logger count, terminal type/identity/cause, and zero duplicate primary attempts.

실행:

```bash
./gradlew :bluetape4k-redisson:test --tests '*ForyCodecTest' --tests '*FastForyCompatibilityTest'
```

예상 결과: new direct-path cases fail before Task 5; old byte-array fallback behavior remains green.

## Task 5: Implement Redisson decode with exactly-one-copy fallback

**파일:**

- 수정: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/ForyCodec.kt`
- 수정: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCodec.kt`
- 수정: Task 4 tests

- [ ] Build a read-only duplicate/slice over exactly `readerIndex..writerIndex`; never mutate the supplied `ByteBuf` indices, marks, or refcount.
- [ ] Decode direct NIO only when the readable range can be represented safely. If view construction/preconditions fail, materialize copied bytes and perform the original primary decode exactly once.
- [ ] If direct primary decode throws, do not rerun direct primary. Materialize bytes once and use them only for the existing codec fallback path.
- [ ] Introduce only internal defaulted factory seams; retain public constructor signatures, lazy Fory injection, registration settings, `Exception` vs `RuntimeException` catch boundaries, logging, fallback codec selection, and copy-constructor behavior.
- [ ] Do not use public `ForyBinarySerializer.deserializeFrom` as a codec primary without an internal codec-only legacy normalizer. Immediately normalize every direct-primary `Throwable` to the exact `AbstractBinarySerializer.deserialize(ByteArray)` `BinarySerializationException` outcome
  **before** the existing codec catch boundary, so Fory's `Exception` and FastFory's `RuntimeException` paths retain old fallback reachability. View/precondition failures are not direct-primary failures: they take only the copied-primary route.
- [ ] The Task 4 matrix must prove the normalizer preserves byte-array oracle behavior for semantic, raw/nested cancellation, raw/nested `Error`, destination, fallback-terminal, and cleanup behavior; it must not duplicate direct primary attempts.
- [ ] Add KDoc that default registration-off decoding is for trusted Redis payloads only; do not claim it is a security hardening change.

실행:

```bash
./gradlew :bluetape4k-redisson:test --tests '*ForyCodecTest' --tests '*FastForyCompatibilityTest'
./gradlew :bluetape4k-redisson:check
git diff --check
```

예상 결과: direct decode succeeds for readable NIO buffers and every forced fallback follows the old externally visible codec contract.

Commit after green (Lore protocol):

```text
Avoid Redisson Fory decode handoff arrays without changing fallback behavior

Constraint: caller ByteBuf state and codec-specific fallback asymmetry are public behavior
Rejected: writable NIO views and repeated direct decode | unsafe state and duplicate work
Confidence: high
Scope-risk: narrow
Directive: Keep the copied fallback single-use and preserve catch boundaries
Tested: focused Fory/FastFory codec tests; detekt
Not-tested: conditional encode path and JMH evidence
```

## Task 6: Run the Redisson encode feasibility probe before production encode work

**파일:**

- Add: `infra/redisson/src/benchmark/.../Issue756ForyEncodeFeasibilityBenchmark.kt`
- Add: `infra/redisson/scripts/run-issue756-fory-feasibility.py`
- Add: `infra/redisson/scripts/validate-issue756-fory-feasibility.py`
- Add: `infra/redisson/scripts/test_validate_issue756_fory_feasibility.py`
- Add: `docs/benchmarks/raw/issue-756-fory-followup/feasibility/{probe-a,probe-b}/`

- [ ] Benchmark current `serialize() -> Unpooled.wrappedBuffer(bytes)` against candidate `Unpooled.buffer(256, Int.MAX_VALUE)` plus caller-owned stream write for both Fory and FastFory.
- [ ] Record `probe-a` and `probe-b` independently using `threads=1`, `forks=2`, `warmup=3x1s`, `measurement=5x1s`, `-prof gc`, throughput `ops/ms`, and `-Xms1g -Xmx1g -XX:+UseG1GC`.
- [ ] Make the runner own one clean `:bluetape4k-redisson:benchmarkBenchmarkJar` build. Both probe leaves must consume that exact JAR/classpath and record dependency/JVM/commit/command hashes; any rebuild or input drift rejects the probe.
- [ ] Use the fixed `Issue756BenchmarkData(756L, "lettuce-buffer-codec", "A".repeat(96))` payload. Each raw leaf contains `jmh.json`, sanitized `argv.json`, allowlisted `environment.json`, `metadata.json`, `summary.csv`, `comparison.json`, `validation.json`, and SHA-256 hashes. `environment.json` contains only OS/JVM/CPU/Gradle/JAR-hash/commit fields; it never serializes process environment.
- [ ] Fix allocation proof to JMH GC profiler `gc.alloc.rate.norm` in `B/op`. In both probes the candidate must be at most 95% of baseline
  **and** `candidate.score + candidate.scoreError < baseline.score - baseline.scoreError`; reject missing, non-finite, non-positive baseline, mismatched unit, NaN, error-bar overlap, preflight drift, or one-run failure. Throughput is a separate finite-positive `ops/ms` guard: each run's delta must be greater than -20%, with no throughput error-interval comparison.
- [ ] Add negative validator fixtures for missing/duplicate/unexpected methods, invalid allocation metric/unit, NaN, error-bar overlap, dirty tree/hash drift, malformed provenance, process-environment capture, and sensitive argv tokens/passwords/proxy URLs. The runner redacts or rejects sensitive argv; a repository scan gate runs before artifact or documentation promotion.
- [ ] Write only `probeDisposition=accepted|rejected` and its reason. A rejected probe is terminal for encode: record final `encodeDisposition=rejected` with reason, commit evidence, and skip Task 7 production edits.

실행:

```bash
python3 infra/redisson/scripts/run-issue756-fory-feasibility.py
python3 infra/redisson/scripts/validate-issue756-fory-feasibility.py
python3 infra/redisson/scripts/test_validate_issue756_fory_feasibility.py
```

예상 결과: a reproducible disposition artifact, not a presumed optimization result.

## Task 7: Conditional Redisson encode implementation

**Precondition:** Task 6 manifest says `probeDisposition=accepted`. If it says `rejected`, record the terminal `encodeDisposition=rejected` evidence and skip this task.

**Files when accepted:**

- 수정: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/ForyCodec.kt`
- 수정: `infra/redisson/src/main/kotlin/io/bluetape4k/redis/redisson/codec/FastForyCodec.kt`
- 수정: focused codec tests

- [ ] First write failing encode tests for a fresh owned output `ByteBuf`: round-trip values, encoder index correctness, release ownership, injected-factory copy preservation, and unchanged Fory/FastFory fallback behavior.
- [ ] Cover backend semantic failure, candidate-only setup/destination failure, baseline success/failure, terminal fallback failure (type, identity, cause, and log count), cleanup failure suppression, cancellation/`Error` behavior, and exactly-once release/refcount.
- [ ] Implement the smallest owned-output writer that uses the Fory stream seam, validates reported/actual byte count, and releases the newly allocated buffer on failure.
- [ ] Do not reuse the decode read-only view helper, caller-provided buffers, or Lettuce's bounded writer abstraction.
- [ ] Run the focused Redisson test/detekt commands from Task 5. Only after these are green write final `encodeDisposition=implemented`; Task 8 alone generates canonical evidence.

Commit accepted implementation or rejected feasibility evidence separately using Lore trailers; pin `probeDisposition` and final `encodeDisposition` in each commit body.

## Task 8: Generate canonical benchmark evidence and validate all disposition cells

**파일:**

- Modify/add: `infra/lettuce/src/benchmark/kotlin/io/bluetape4k/redis/lettuce/benchmark/LettuceCodecBenchmark.kt`, `LettuceCodecBenchmarkPreflight.kt`, `infra/lettuce/scripts/run-issue756-evidence.py`, `validate-issue756-jmh.py`, and their Python tests
- Add: Redisson issue-756 benchmark/preflight/runner/validator scripts and their Python tests
- Add: `docs/benchmarks/raw/issue-756-fory-followup/manifest.json`
- Add: `docs/benchmarks/raw/issue-756-fory-followup/validate-issue756-fory-followup.py`
- Add: `docs/benchmarks/raw/issue-756-fory-followup/test_validate_issue756_fory_followup.py`

- [ ] Define a checked method-to-cell matrix: Lettuce target
  **serialize** Fory/FastFory × heap/direct (4 pairs, 8 methods); Redisson decode Fory/FastFory × `{single-NIO heap ByteBuf, single-NIO direct ByteBuf, composite copied fallback}` (6 pairs, 12 methods); and, only when `encodeDisposition=implemented`, Redisson encode Fory/FastFory × baseline/candidate (2 pairs, 4 methods). Do not add a primary-exception fallback promotion pair. For every cell, pin the same logical payload, buffer state, equality/state preflight, baseline (`ByteBufUtil.getBytes` plus current byte-array decode/encode), candidate (streamed Lettuce target, direct NIO decode, copied-primary fallback, or owned-output encode), and `gc.alloc.rate.norm` unit.
- [ ] Apply the exact Task 6 settings and gate independently for each benchmark cell and each A/B run.
- [ ] Make `infra/lettuce/scripts/run-issue756-evidence.py` build one clean `:bluetape4k-lettuce:benchmarkBenchmarkJar`, run Lettuce canonical A/B, and validate its preflight; make the new Redisson runner do the equivalent. Run Lettuce first, Redisson second, with no Testcontainers overlap.
- [ ] Require every canonical leaf to contain the Task 6 raw files and hashes; the aggregate manifest records the one pinned JAR/classpath per module and rejects a rebuilt, divergent, or unlisted leaf.
- [ ] Mark every fallback cell non-promotable regardless of an allocation result; it can document only fallback compatibility and must never be charted as an optimization. The aggregate validator rejects a declared mode, baseline, candidate, preflight, metric unit, or disposition that conflicts with the checked matrix.
- [ ] Before result acceptance, run Python validator tests with negative fixtures for cardinality/disposition mismatch, missing/duplicate/unexpected methods, invalid allocation metric/unit, NaN/error overlap, dirty tree/hash drift, sensitive metadata, non-promotable fallback, ancestry, append-only raw authority, and changed-path allowlist violations.
- [ ] Validate 20 canonical methods when encode is rejected or 24 when implemented. Feasibility output remains raw supporting evidence and is never silently promoted to canonical results.
- [ ] Classify every cell `accepted`, `fallback`, `inconclusive`, or `rejected`; issue completion requires a terminal documented disposition for every in-scope cell.

실행:

```bash
python3 infra/lettuce/scripts/run-issue756-evidence.py
python3 infra/lettuce/scripts/test_validate_issue756_jmh.py
python3 infra/redisson/scripts/run-issue756-fory-evidence.py
python3 infra/redisson/scripts/test_validate_issue756_fory_evidence.py
python3 docs/benchmarks/raw/issue-756-fory-followup/test_validate_issue756_fory_followup.py
python3 docs/benchmarks/raw/issue-756-fory-followup/validate-issue756-fory-followup.py
git diff --check
```

예상 결과: aggregate validation rejects absent forks, inconsistent manifests, undocumented cells, and a count that conflicts with `encodeDisposition`.

## Task 9: Publish evidence-backed documentation, charts, and release handoff

**파일:**

- 수정: `io/io/README.md`, `io/io/README.ko.md`, `infra/lettuce/README.md`, `infra/lettuce/README.ko.md`, `infra/redisson/README.md`, and `infra/redisson/README.ko.md`
- Add: `docs/benchmarks/2026-07-23-issue-756-fory-codec-followup.md`; preserve `2026-07-22-issue-756-lettuce-buffer-codec-allocation.md` and its raw authority unchanged
- 수정: public KDoc for `ForyBinarySerializer`, `LettuceBinaryCodec`, `LettuceBinaryCodecs`, `ForyCodec`, and `FastForyCodec`
- Add: chart source and rendered assets under `docs/images/readme-charts/`
- Add: `docs/benchmarks/raw/issue-756-fory-followup/validate-chart-source.py`
- Add: `docs/superpowers/checklists/2026-07-23-issue-756-fory-followup-release.md`
- Add: compatibility fixtures/results under `docs/benchmarks/raw/issue-756-fory-followup/release/`
- Add: `infra/redisson/scripts/run-issue756-fory-compatibility.py`, `test_run_issue756_fory_compatibility.py`, and `run-issue756-fory-rollback-smoke.py`

- [ ] Before editing charts/diagrams, load and follow `bluetape-diagram`; generate reproducible assets from validated raw artifacts only.
- [ ] Add a transport-specific compatibility/status matrix covering raw-only scope, no compression migration, Fory internal-buffer/non-zero-copy caveat, trusted-payload registration-off warning, Lettuce no-fallback behavior, Redisson asymmetric fallback direction, and every accepted/rejected/inconclusive/fallback disposition. Non-accepted cells remain table/caption entries and are never plotted as zero.
- [ ] Keep Korean user-facing analysis and English README/public prose semantically aligned without translating code identifiers; inspect the six named README files and the five named public KDoc surfaces against the matrix.
- [ ] Bind chart source data to the aggregate manifest SHA, validate accepted-cell-only inputs, render the asset with the diagram skill's prescribed renderer, and visually inspect the rendered result.
- [ ] Release checklist must pin target coordinates/version, observed published version, exact JAR SHA-256, consumer scope, benchmark manifest SHA, and rollback owner/action. It must not assert publish authority.
- [ ] `run-issue756-fory-compatibility.py` resolves the checklist-pinned known-good JARs through an explicit repository URL, checksum-gates every loaded JAR against its coordinate/version/SHA-256 before execution, and stores coordinate/version/SHA/repository URL/classpath manifests. It records old-write/new-read plus new-write/old-read Fory/FastFory fixtures and has a checksum-mismatch negative test. `run-issue756-fory-rollback-smoke.py` executes and records the rollback Redis smoke procedure without publishing; either failure blocks documentation promotion.
- [ ] Validate links/assets, chart source data, Korean/English parity, explicit trusted-input/no-migration/raw-only/fallback/zero-copy wording, and markdown rendering conventions.

실행:

```bash
git diff --check
rg -n 'zero-copy|internal buffer removed|trusted.*payload|raw-only|no migration' io/io/README* infra/lettuce/README* infra/redisson/README* docs/benchmarks
python3 docs/benchmarks/raw/issue-756-fory-followup/validate-chart-source.py
python3 infra/redisson/scripts/test_run_issue756_fory_compatibility.py
python3 infra/redisson/scripts/run-issue756-fory-compatibility.py
python3 infra/redisson/scripts/run-issue756-fory-rollback-smoke.py
```

예상 결과: no unsupported zero-copy claim; only validated measurements appear in reader-facing materials.

## Task 10: Final verification, review, and handoff

- [ ] Run targeted tests for `io`, Lettuce, and Redisson sequentially with separate Gradle invocations; then sequential affected-module `check`/`build` validation. Detekt is root-only and has no subproject source task, so do not claim module-local Detekt coverage.
- [ ] Run every evidence validator and `git diff --check`; inspect generated artifacts and ensure no placeholder values remain.
- [ ] Verify the release cross-decode and rollback-smoke artifact from Task 9 before review or documentation promotion.
- [ ] Perform six independent reviews (performance, stability, security, operations, developer/API, caller/user). Resolve P0/P1, re-run affected checks, and record disposition evidence.
- [ ] Commit with Lore protocol, push the feature branch, verify local/remote/PR heads match, then report merge readiness. Do not merge without fresh user approval.

Final commands:

```bash
./gradlew :bluetape4k-io:test
./gradlew :bluetape4k-lettuce:test
./gradlew :bluetape4k-redisson:test
./gradlew :bluetape4k-io:check :bluetape4k-io:build
./gradlew :bluetape4k-lettuce:check :bluetape4k-lettuce:build
./gradlew :bluetape4k-redisson:check :bluetape4k-redisson:build
python3 docs/benchmarks/raw/issue-756-fory-followup/validate-issue756-fory-followup.py
python3 infra/redisson/scripts/run-issue756-fory-compatibility.py
python3 infra/redisson/scripts/run-issue756-fory-rollback-smoke.py
git diff --check
repo-status
```

**Stop
condition:** The plan is ready for implementation only after this plan itself is committed and six-lens plan review has zero unresolved P0/P1 findings. Production implementation begins only after the user approves the reviewed plan.
