# Issue #754 ByteBuffer Serializer Stack Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve every existing `ByteArray` serializer API and wire/security contract while adding caller-owned `ByteBuffer` input/output paths, backend-specific lower-copy implementations, allocation evidence, and a non-bypassable 1.12.0 release hold across five stacked pull requests.

**Architecture:** Interface-owning modules add executable JVM defaults backed by module-local transaction helpers, while optimized serializers override only the paths their resolved dependency version can support safely. The stack lands contract, core backends, JSON backends, Avro, then benchmark/documentation evidence; each descendant inherits and reruns all predecessor proof before its own exact-head PR gate.

**Tech Stack:** Kotlin 2.3, Java 21, Gradle, JUnit 5, Kluent/bluetape4k assertions, JDK `ByteBuffer`/object streams, Kryo 5.6.2, Apache Fory 1.3.0, Jackson 2.22.1, Jackson 3.2.0, Fastjson2 2.0.62, Avro 1.12.1, kotlinx-benchmark 0.4.17, JMH 1.37, Python 3 standard library, GitHub Actions.

---

## Authority And Stop Conditions

- Approved design: `docs/superpowers/specs/2026-07-16-issue-754-bytebuffer-serializer-stack-design.md` at `f0b8bd22a61b3cf8c497377de46a090f7e0cccbe`.
- Initial base authority: `origin/develop@90b267871e9154f242e6de7ee9fd0539f83e509e`.
- Current branch/PR 1 head: `feat/issue-754-buffer-contract`.
- PR 2: `feat/issue-754-core-serializers`, initially based on PR 1.
- PR 3: `feat/issue-754-json-serializers`, initially based on PR 2.
- PR 4: `feat/issue-754-avro-serializers`, initially based on PR 3.
- PR 5: `feat/issue-754-allocation-proof`, initially based on PR 4.
- No production code starts until this plan is reviewed, approved, and committed.
- PR creation is authorized by the approved stacked delivery request once each slice reaches its pre-PR gate. Every merge still stops for a fresh exact-PR/head approval.
- Snapshot, Nightly, release, production tag creation, tag-ruleset mutation, GitHub App installation, environment mutation, branch deletion, and worktree deletion remain separate side effects. Execute them only at the matching explicit authority row.
- If a predecessor changes after a descendant was validated, invalidate the descendant evidence, rebase it on the merged predecessor, and rerun its complete inherited gate.
- Evidence manifests avoid impossible self-reference: committed manifests record the tested producer in `evidenceProducerSha` and never embed the final candidate as `releaseCandidateSha`. They also record a deterministic `testedCodeTreeSha256` over the checked allowlist of serializer/benchmark source, every JSON backend implementation, Gradle/build configuration, validation scripts, and workflows. README/manual/CHANGELOG, `.github/release-holds/**`, and `docs/evidence/issue-754/**` are excluded from that code digest but validated separately by full-head path/checksum contracts. The exact final PR head, PR tree, merge SHA/tree, and release-candidate SHA are supplied to and emitted by the mandatory live check after the commit exists. The validator recomputes the same code-tree digest from both the evidence producer and that exact candidate, rejects any covered implementation drift, and rejects every producer-to-candidate path except `.github/release-holds/**` and `docs/evidence/issue-754/**`; it separately validates hold authority and evidence checksums on the full exact head.

## File And Ownership Map

| Slice | Existing files to modify | Files to create | Owner boundary |
|---|---|---|---|
| PR 1 contract | `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializer.kt`, `BinarySerializerSupport.kt`, `io/io/src/main/kotlin/io/bluetape4k/io/ByteBufferOutputStream.kt`, `io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializer.kt`, all three Avro serializer interfaces, `.github/workflows/publish-snapshot.yml`, `.github/workflows/release.yml` | `.github/workflows/release-generic.yml`, module-local `BufferSerializationDefaults.kt`, contract/adapter/ABI tests, frozen pre-change fixtures, both release-hold JSON authorities, hold/GitHub-settings scripts and tests, contract evidence | All public APIs, default behavior, compatibility, workflow/credential hold, and external-setting gate only; no backend optimization |
| PR 2 core | `JdkBinarySerializer.kt`, `KryoBinarySerializer.kt`, `ForyBinarySerializer.kt`, core README pair | backend buffer tests and core compatibility evidence | JDK/Kryo output and input, Fory input; Fory output remains fallback |
| PR 3 JSON | Jackson 2/3 and Fastjson2 primary serializer classes plus README pairs | backend buffer/security tests and JSON compatibility evidence | Jackson 2/3 optimized paths; Fastjson2 JSONB fallback proof only |
| PR 4 Avro | three default Avro implementations and Avro README pair | implementation buffer tests, OCF semantic oracle, hostile fixtures, Avro evidence | Reflect/generic/specific/list overrides and bounded security parity; public defaults are inherited from PR 1 |
| PR 5 proof/docs | root/module READMEs, public KDoc, `CHANGELOG.md`, release-hold manifest | `benchmark/serializer-bytebuffer-benchmark/**`, benchmark/docs/candidate validators and tests, validation-only candidate workflow, final evidence and lesson | Allocation claims, bilingual docs, final hold decision; no new serializer behavior |

Shared files are owned by only one active implementation lane at a time. In particular, PR 1 owns the interface/default helper surfaces; backend PRs may override methods but do not rewrite the transaction contract.

## Acceptance Traceability

| Design acceptance criterion | Implementing tasks | Proof |
|---|---|---|
| Preserve public `ByteArray` APIs and compiled callers | 1-4 | frozen fixtures, Kotlin/Java source compatibility, `javap`, runtime linkage |
| Add heap/direct/sliced/read-only buffer APIs | 2-4, 6-12 | parameterized contract matrices and exact targeted module tests |
| Preserve position/limit/mark/order and failure policy | 2-12 | canary, mark/reset, overflow, backend/flush/close failure tests |
| True lower-copy only where supported | 6-12 | backend implementation tests plus Task 14/15 JMH allocation gate |
| Preserve security and wire behavior | 1, 6-12 | old/new cross-reading, registration/filter/polymorphism, OCF semantic oracle |
| Keep integrations/compression out | every task | diff allowlists exclude #755-#758 surfaces and compression decorators |
| Document exact caller contract and limits | 13, 16 | English/Korean parity check, examples, CHANGELOG, evidence manifest |
| Hold 1.12.0 until the complete exact stack is proved and close its tag immutably | 4, 5, 16, 17 | release-hold validator, credential isolation, final exact-head check, no-bypass tag policy |

## Risk Register

| Risk | Signal | Mitigation | Rollback/rerun point |
|---|---|---|---|
| Binary/source ABI break | fixture compilation/linkage or `javap` mismatch | executable interface defaults and distinct `deserializeFrom` JVM names | revert PR 1 before any backend PR merges; otherwise revert descendants first |
| Caller position corruption | contract matrix observes position/mark/order drift | duplicate/slice input and one transaction helper per interface module | return to the failing backend task and rerun inherited contract tests |
| Overflow hidden by backend wrappers | wrong exception type/cause/suppressed graph | cycle-safe throwable classifier with fatal-`Error` precedence | revert the backend override to the default path |
| Pool/reference retention | failed call poisons next call or weak reference remains reachable | call-scoped wrappers and `finally` release before target commit | disable optimized override and rerun lifecycle tests |
| Wire/security drift | byte/semantic parity or filter/registration test differs | reuse the configured serializer/mapper/reader and frozen old artifact | abandon optimization for that cell; retain ergonomic fallback |
| Benchmark false positive | missing GC metrics, reused run ID, pooled samples, inconsistent runs | dedicated non-up-to-date `gcProfile`, two UUID runs, deterministic validator | classify cell neutral; never select the better run |
| Partial 1.12.0 release | publish/tag workflow can bypass hold | mandatory job dependency plus protected release App/ruleset gate | freeze descendants and publication; revert newest merged slice first |
| External GitHub setting cannot be made non-bypassable | probe actor can create/update/delete protected patterns | stop after PR 1 merge and request explicit repository-setting authority | keep release hold red; do not start release side effects |

## PR 1 - Buffer Contract

### Task 1: Freeze The Pre-Change Compatibility Authority

**Complexity:** High
**Depends on:** approved spec and plan only
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Create: `io/io/src/test/resources/compat/issue-754/pre-change/manifest.json`
- Create: `io/io/src/test/resources/compat/issue-754/pre-change/binary/jdk-simple-data.bin`
- Create: `io/io/src/test/resources/compat/issue-754/pre-change/binary/kryo-default-simple-data.bin`
- Create: `io/io/src/test/resources/compat/issue-754/pre-change/binary/kryo-fast-simple-data.bin`
- Create: `io/io/src/test/resources/compat/issue-754/pre-change/binary/fory-default-simple-data.bin`
- Create: `io/io/src/test/resources/compat/issue-754/pre-change/binary/fory-fast-simple-data.bin`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/SerializerBufferAbiCompatibilityTest.kt`
- Create: `io/io/src/test/resources/compat/issue-754/src/java/LegacyBinaryCaller.java`
- Create: `io/io/src/test/resources/compat/issue-754/src/java/LegacyBinaryImplementation.java`
- Create: `io/io/src/test/resources/compat/issue-754/src/java/NewBinaryBufferCaller.java`
- Create: `io/io/src/test/resources/compat/issue-754/src/kotlin/LegacyBinaryCaller.kt`
- Create: `io/io/src/test/resources/compat/issue-754/src/kotlin/LegacyBinaryImplementation.kt`
- Create: `io/json/src/test/resources/compat/issue-754/src/java/LegacyJsonCaller.java`
- Create: `io/json/src/test/resources/compat/issue-754/src/java/LegacyJsonImplementation.java`
- Create: `io/json/src/test/resources/compat/issue-754/src/java/NewJsonBufferCaller.java`
- Create: `io/json/src/test/resources/compat/issue-754/src/kotlin/LegacyJsonCaller.kt`
- Create: `io/json/src/test/resources/compat/issue-754/src/kotlin/LegacyJsonImplementation.kt`
- Create: `io/avro/src/test/resources/compat/issue-754/src/java/LegacyAvroCaller.java`
- Create: `io/avro/src/test/resources/compat/issue-754/src/java/LegacyAvroReflectImplementation.java`
- Create: `io/avro/src/test/resources/compat/issue-754/src/java/LegacyAvroGenericRecordImplementation.java`
- Create: `io/avro/src/test/resources/compat/issue-754/src/java/LegacyAvroSpecificRecordImplementation.java`
- Create: `io/avro/src/test/resources/compat/issue-754/src/java/NewAvroBufferCaller.java`
- Create: `io/avro/src/test/resources/compat/issue-754/src/kotlin/LegacyAvroCaller.kt`
- Create: `io/avro/src/test/resources/compat/issue-754/src/kotlin/LegacyAvroReflectImplementation.kt`
- Create: `io/avro/src/test/resources/compat/issue-754/src/kotlin/LegacyAvroGenericRecordImplementation.kt`
- Create: `io/avro/src/test/resources/compat/issue-754/src/kotlin/LegacyAvroSpecificRecordImplementation.kt`
- Create: `scripts/check-serializer-buffer-abi.sh`
- Create: `docs/evidence/issue-754/contract/baseline-manifest.json`

- [ ] **Step 1: Build the authority artifact from the pinned base**

Run in a temporary clean worktree rooted at `90b267871e9154f242e6de7ee9fd0539f83e509e`:

```bash
./gradlew :bluetape4k-io:jar :bluetape4k-json:jar :bluetape4k-avro:jar --no-configuration-cache
```

Expected: exit 0; record Java/Kotlin/Gradle versions, producer commit, serializer configuration, fixture SHA-256, schema/codec where applicable, and all three jar paths/SHA-256 values in `manifest.json`. Do not infer this authority from a published Maven version. Build baseline jars into ignored `.codex/compat/issue-754/90b267871e9154f242e6de7ee9fd0539f83e509e/`; commit manifests and checksums, not jar binaries. `check-serializer-buffer-abi.sh` must compile and run every legacy Avro caller solely against this pinned Avro artifact before Task 3 starts.

- [ ] **Step 2: Add legacy callers that compile against the pre-change interfaces**

The Java fixtures must retain the null-literal calls that would become ambiguous if same-name `ByteBuffer` members were added:

```java
Object binaryNull = binary.deserialize(null);
Object jsonNull = json.deserialize(null, Object.class);
Object avroNull = reflect.deserialize(null, Object.class);
```

The Kotlin fixture must call the existing static extension symbol:

```kotlin
val restored: String? = serializer.deserialize(ByteBuffer.wrap(payload))
```

Compile the exact Java/Kotlin legacy implementation fixtures against only the pinned pre-change interfaces. Each implements the old abstract ByteArray members for Binary, JSON, Avro reflect, Avro generic, and Avro specific/list families. Against current jars, instantiate each fixture and invoke its inherited new buffer defaults in addition to the old methods; record compilation, linkage, default dispatch, and returned values in the ABI report.

- [ ] **Step 3: Run the ABI fixture before implementation and record RED**

```bash
bash scripts/check-serializer-buffer-abi.sh --build-current --expected-head "$(git rev-parse HEAD)"
```

Expected: FAIL because the new JVM default methods and new buffer runtime behavior do not yet exist; the legacy ByteArray and null-literal compilation portion must already pass.

- [ ] **Step 4: Commit the frozen authority separately**

```text
Freeze the exact pre-change serializer compatibility authority

Constraint: Use origin/develop@90b267871e9154f242e6de7ee9fd0539f83e509e as the producer
Confidence: high
Scope-risk: narrow
Directive: Regenerate only by recording a new producer commit and checksums
Tested: Pinned jars, legacy caller compilation, fixture checksums
Not-tested: New ByteBuffer defaults remain intentionally RED
```

### Task 2: Lock Fixed-Buffer Transaction Behavior

**Complexity:** High
**Depends on:** Task 1
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `io/io/src/main/kotlin/io/bluetape4k/io/ByteBufferOutputStream.kt`
- Create: `io/io/src/main/kotlin/io/bluetape4k/io/BufferFailurePolicy.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/FixedByteBufferOutputStreamTest.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/BufferFailurePolicyTest.kt`
- Modify: `io/io/src/test/kotlin/io/bluetape4k/io/ByteBufferStreamTest.kt`

- [ ] **Step 1: Add RED tests for the exact fixed adapter factory**

Test `ByteBufferOutputStream.fixed(buffer)` with heap, direct, sliced, non-zero-position, reduced-limit, and read-only targets. The central assertion shape is:

```kotlin
val start = target.position()
val stream = ByteBufferOutputStream.fixed(target)
stream.write(payload)
stream.close()
target.position() shouldBeEqualTo start + payload.size
target.limit() shouldBeEqualTo originalLimit
target.order() shouldBeEqualTo originalOrder
```

Also prove exact-capacity success, one-byte-short `BufferOverflowException`, no growth/detachment, no-op `close`, and `toByteArray()` returning only bytes written from the captured start without changing the target.

- [ ] **Step 2: Verify adapter RED**

```bash
./gradlew :bluetape4k-io:test --tests '*FixedByteBufferOutputStreamTest' --tests '*BufferFailurePolicyTest' --no-configuration-cache
```

Expected: FAIL because `fixed` and the shared failure classifier do not exist.

- [ ] **Step 3: Implement the fixed adapter without changing existing growing factories**

Add the exact factory:

```kotlin
@JvmStatic
fun fixed(buffer: ByteBuffer): ByteBufferOutputStream =
    ByteBufferOutputStream(buffer, growable = false, initialPosition = buffer.position())
```

Keep `invoke(...)` and `direct(...)` growable and source-compatible. In fixed mode `ensureCapacity` throws raw `BufferOverflowException`; `close()` does not close or detach caller storage.

Add public KDoc to `fixed` that specifies null/read-only validation order; aliasing of the exact supplied `ByteBuffer` view; the current limit as a hard bound; no growth, replacement, or detachment; position advancement; idempotent no-op close and post-close writes; `toByteArray()` over `[capturedStart, currentPosition)` without target mutation; and caller ownership plus thread confinement.

- [ ] **Step 4: Implement cycle-safe failure classification**

Use an identity set while traversing `cause` and `suppressed`; check any `Error` root before overflow translation. Preserve the exact primary/cause/suppressed precedence defined in design sections 4.5 and 6.

- [ ] **Step 5: Run GREEN and existing stream regressions**

```bash
./gradlew :bluetape4k-io:test --tests '*FixedByteBufferOutputStreamTest' --tests '*BufferFailurePolicyTest' --tests '*ByteBufferStreamTest' --no-configuration-cache
```

Expected: all selected tests pass; existing growing adapter behavior remains unchanged.

- [ ] **Step 6: Commit the adapter transaction**

```text
Provide fixed caller-buffer writes without silent storage replacement

Constraint: Preserve every existing growable ByteBufferOutputStream factory
Rejected: Preflight serialization | It duplicates backend work and allocation
Confidence: high
Scope-risk: moderate
Directive: Failed target content is unspecified even when position rolls back
Tested: Heap, direct, slice, overflow, close, fatal, and throwable-cycle tests
```

### Task 3: Add Binary, JSON, And Avro Interface Defaults

**Complexity:** High
**Depends on:** Task 2
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Create: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BufferSerializationDefaults.kt`
- Modify: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializer.kt`
- Modify: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/BinarySerializerSupport.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerByteBufferContractTest.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/SerializerBufferConcurrencyTestSupport.kt`
- Create: `io/json/src/main/kotlin/io/bluetape4k/json/BufferSerializationDefaults.kt`
- Modify: `io/json/src/main/kotlin/io/bluetape4k/json/JsonSerializer.kt`
- Create: `io/json/src/test/kotlin/io/bluetape4k/json/JsonSerializerByteBufferContractTest.kt`
- Create: `io/avro/src/main/kotlin/io/bluetape4k/avro/BufferSerializationDefaults.kt`
- Modify: `io/avro/src/main/kotlin/io/bluetape4k/avro/AvroReflectSerializer.kt`
- Modify: `io/avro/src/main/kotlin/io/bluetape4k/avro/AvroGenericRecordSerializer.kt`
- Modify: `io/avro/src/main/kotlin/io/bluetape4k/avro/AvroSpecificRecordSerializer.kt`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/AvroSerializerByteBufferContractTest.kt`
- Modify: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerSupportTest.kt`
- Modify: `io/json/src/test/kotlin/io/bluetape4k/json/JsonSerializerTest.kt`

- [ ] **Step 1: Add parameterized RED contract tests**

For Binary, JSON, and all Avro interface families, cover heap/direct/slice/read-only input, writable/read-only output, non-zero position, reduced limit, mark/reset, byte order, null/empty, exact capacity, one-byte-short overflow, backend failure, deterministic backend `Error`, and retry. The default-path test serializer records whether the ByteArray sibling was invoked, proving read-only target rejection happens first. `Error` rows assert same-instance propagation, no overflow translation, position-only rollback, and a clean subsequent success. Avro tests include reflect, generic, specific, and specific-list null/empty policies.

- [ ] **Step 2: Verify contract RED**

```bash
./gradlew :bluetape4k-io:test :bluetape4k-json:test :bluetape4k-avro:test --tests '*SerializerByteBufferContractTest' --no-configuration-cache
```

Expected: FAIL at missing `serializeTo`/`deserializeFrom` methods.

- [ ] **Step 3: Add module-local transaction helpers**

Each of `:bluetape4k-io`, `:bluetape4k-json`, and `:bluetape4k-avro` owns this observable behavior without adding a dependency edge:

```kotlin
internal inline fun serializeNullableTo(
    target: ByteBuffer,
    produce: () -> ByteArray?,
): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()
    val start = target.position()
    return try {
        val bytes = produce() ?: return 0
        if (bytes.size > target.remaining()) throw BufferOverflowException()
        target.put(bytes)
        bytes.size
    } catch (failure: Throwable) {
        target.position(start)
        throw failure
    }
}

internal fun copyRemaining(source: ByteBuffer): ByteArray =
    ByteArray(source.remaining()).also { source.duplicate().get(it) }
```

Use the reviewed fatal/overflow classifier where cleanup is involved; do not introduce a global shared dependency solely to deduplicate these small helpers.

The test support defines the shared reusable-instance concurrency contract: 8 workers, 50 mixed valid/invalid repetitions each, a 2-second start-barrier deadline, a 15-second completion deadline, and one caller-owned buffer per invocation. Every test uses `finally` to call `shutdownNow`, waits at most 5 seconds for termination, and fails with unfinished worker/thread/state diagnostics. No backend test may create an unbounded executor or wait indefinitely.

- [ ] **Step 4: Add executable interface defaults and Kotlin facades**

`BinarySerializer` receives `serializeTo(graph, target)` and `deserializeFrom(source)` defaults. `JsonSerializer` receives `serializeTo(graph, target)` and `deserializeFrom(source, clazz)`. Avro reflect/generic/specific/list interfaces receive the exact reviewed `serializeTo`, `deserializeFrom`, `serializeListTo`, and `deserializeListFrom` defaults from design section 4.3. Keep `BinarySerializer.deserialize(ByteBuffer)` at the existing static symbol but delegate to `deserializeFrom`; add Kotlin `ByteBuffer` extensions for JSON and Avro without annotating interface members with `@JvmName`.

Add English public KDoc in the same step for position/limit/mark/order, read-only and overflow precedence, position-only rollback, unspecified failed content, null/empty policy, caller ownership, thread confinement, trusted/caller-bounded input, and the allocating nature of default fallbacks. PR 5 may reconcile capability results, but it must not be the first place the PR 1 public contract is documented.

Compile and run the new Java callers in `check-serializer-buffer-abi.sh`. They must execute Binary/JSON `serializeTo` and `deserializeFrom`, plus Avro reflect/generic/specific/list `serializeTo`, `deserializeFrom`, `serializeListTo`, and `deserializeListFrom`. Java null `target` and `source` calls must throw `NullPointerException` before any side-effecting backend invocation.

`--build-current` removes only the three known current jar outputs, runs all three exact `jar` tasks, rejects zero or multiple candidate jars, and compiles/runs against those resolved files rather than persistent directories. Every invocation records expected/current HEAD and tree plus each jar path/SHA-256 in the ABI report; any mismatch or stale/extra jar fails closed.

- [ ] **Step 5: Run GREEN and full interface-module tests**

```bash
./gradlew :bluetape4k-io:test :bluetape4k-json:test :bluetape4k-avro:test --no-configuration-cache
```

Expected: exit 0; source state is preserved on success and failure, and existing ByteArray tests remain green.

- [ ] **Step 6: Commit the public defaults**

```text
Add buffer ergonomics without breaking existing serializer implementations

Constraint: Preserve Java null-literal calls and the Binary Kotlin extension symbol
Rejected: Same-name Java overloads | They make deserialize(null) ambiguous
Confidence: high
Scope-risk: broad
Directive: Keep default paths explicitly documented as allocating fallbacks
Tested: Binary, JSON, and Avro contract matrices plus existing module tests
```

### Task 4: Prove ABI And Enforce The 1.12.0 Release Hold

**Complexity:** High
**Depends on:** Task 3
**Pattern skills:** `bluetape-maintenance`, `bluetape-kotlin-patterns`

**Files:**
- Modify: `scripts/check-serializer-buffer-abi.sh`
- Create: `.github/release-holds/1.12.0-issue-754.json`
- Create: `scripts/check-release-holds.py`
- Create: `scripts/test_check_release_holds.py`
- Create: `scripts/issue-754-github-settings.py`
- Create: `scripts/test_issue_754_github_settings.py`
- Modify: `.github/workflows/publish-snapshot.yml`
- Modify: `.github/workflows/release.yml`
- Create: `docs/evidence/issue-754/contract/abi-report.json`
- Create: `docs/evidence/issue-754/contract/release-hold-report.json`
- Create: `docs/evidence/issue-754/contract/SHA256SUMS`

- [ ] **Step 1: Run the ABI fixture GREEN**

```bash
./gradlew :bluetape4k-io:jar :bluetape4k-json:jar :bluetape4k-avro:jar --no-configuration-cache
bash scripts/check-serializer-buffer-abi.sh --build-current --expected-head "$(git rev-parse HEAD)"
javap -classpath io/io/build/libs/bluetape4k-io-1.12.0-SNAPSHOT.jar -p io.bluetape4k.io.serializer.BinarySerializer
javap -classpath io/json/build/libs/bluetape4k-json-1.12.0-SNAPSHOT.jar -p io.bluetape4k.json.JsonSerializer
javap -classpath io/avro/build/libs/bluetape4k-avro-1.12.0-SNAPSHOT.jar -p io.bluetape4k.avro.AvroReflectSerializer
javap -classpath io/avro/build/libs/bluetape4k-avro-1.12.0-SNAPSHOT.jar -p io.bluetape4k.avro.AvroGenericRecordSerializer
javap -classpath io/avro/build/libs/bluetape4k-avro-1.12.0-SNAPSHOT.jar -p io.bluetape4k.avro.AvroSpecificRecordSerializer
```

Expected: callers and third-party Java/Kotlin implementations compiled against the old interfaces run against the new jars; each legacy implementation inherits and executes every applicable new default; new Java callers compile and execute every new member; Java null `target`/`source` precedence is proved before backend invocation; new methods are executable JVM defaults; Java input names are `deserializeFrom`/`deserializeListFrom`; the legacy static Binary extension remains.

- [ ] **Step 2: Write release-hold validator RED tests**

Use Python `unittest` fixtures for open issue, unmerged/missing PR, malformed manifest, checksum mismatch, head/tree mismatch, merge/tree mismatch, stale release-candidate SHA, and complete pass. Include static workflow tests asserting the exact job name `release-hold-1.12.0-issue-754` and that every 1.12.0 publish/tag side effect lists it in `needs`. Add a fail-closed workflow audit that rejects any non-`release.yml` reference to environment `release-tag-1.12.0`, generic-token tag creation, or guarded tag/publish path without the hold. The same audit requires `release-generic.yml`, its explicit push exclusion and manual rejection for `1.12.0`, and its exact resolve -> publish -> GitHub Release dependency chain.

```bash
python3 -m unittest scripts/test_check_release_holds.py -v
```

Expected: FAIL before the validator and workflow wiring exist.

- [ ] **Step 3: Implement fail-closed manifest validation**

The JSON authority includes `schemaVersion`, release `1.12.0`, issue `754`, five named stack slices, expected PR/head/merge/tree fields, committed evidence paths, SHA-256 entries, and release-candidate SHA. Missing or unknown fields fail; there is no boolean bypass field.

- [ ] **Step 4: Wire mandatory workflow preconditions**

Add the exact precondition job to both guarded workflows. Every 1.12.0 snapshot publish, release publish, GitHub Release, and tag-creation job must depend on it. Remove the push-tag trigger from `release.yml`; make the checked dispatch path the sole 1.12.0 tag creator. Preserve the old tag/manual release path as `release-generic.yml`, but explicitly exclude and reject `1.12.0` before checkout, credentials, publication, or release creation. Split release dispatch into explicit `phase=prepare` and `phase=publish`: both require exact `candidate_validation_run_id` and `candidate_validation_request_id`; prepare verifies that unique PASS artifact before it may create the exact tag and run named job/check `issue-754-tag-immutability`, but cannot publish; publish additionally requires `prepare_run_id`, retrieves only that run's retained artifact, and reruns the check in verify-only mode. Use request-specific run-names for both phases. A first dependency job `candidate-sha-guard` has no environment/secrets, rejects `github.sha != inputs.candidate_sha`, and checks out/verifies that exact SHA; artifact-verification and every protected/mutating/publishing job need this guard and use the same SHA. Static fixtures cover a moved `develop` ref and reject environment access or mutation without the guard. The publish-phase immutability job has exact permissions `actions: read` and `contents: read`, and its cross-run download step receives only `GH_TOKEN: ${{ github.token }}`; static tests reject missing or excess permissions/authentication. Every Maven/GitHub publication job must `need` both the general hold and `issue-754-tag-immutability`. Bind held snapshot jobs to `snapshot-publish-1.12.0` and release/tag jobs to `release-tag-1.12.0`; the static audit rejects either environment on an unheld job, rejects `release-tag-1.12.0` outside `release.yml`, and rejects any publication path without the immutability dependency.

The plan must not treat workflow YAML alone as a credential boundary. `issue-754-github-settings.py` owns fixture-tested snapshot/apply/verify/probe/rollback and immutable-closeout state-machine commands for rulesets, environment deployment policies, reviewers, App installation bypass, and the five existing Maven/signing secret names (`CENTRAL_USERNAME`, `CENTRAL_PASSWORD`, `SIGNING_KEY_ID`, `SIGNING_KEY`, `SIGNING_PASSWORD`) without ever recording values. The release environment additionally owns two distinct principals: tag App variable/secret `RELEASE_TAG_APP_ID`/`RELEASE_TAG_APP_PRIVATE_KEY` with repository Contents write but no Administration write, and settings App variable/secret `RELEASE_SETTINGS_APP_ID`/`RELEASE_SETTINGS_APP_PRIVATE_KEY` with least-privilege Administration write plus Metadata/Contents read but no Contents write. Neither principal exists at repository scope; their App/installation IDs must differ, only the tag App is a ruleset bypass actor, and neither token may perform the other's operation. The five Maven/signing secrets remain environment-scoped in both the 1.12.0 environments and the existing `maven-central-release` environment so non-1.12.0 releases remain operable; repository-scoped copies are forbidden. Tests include actor/permission confusion, accepted-response-loss, partial update failure, stale ruleset state, wrong production-tag target, missing/wrong/expired/duplicate prepare or candidate-validation artifacts/run IDs, and rollback under recognized versus unknown drift.

- [ ] **Step 5: Run validator GREEN and workflow static checks**

```bash
python3 -m unittest scripts/test_check_release_holds.py -v
python3 -m unittest scripts/test_issue_754_github_settings.py -v
python3 scripts/check-release-holds.py --manifest .github/release-holds/1.12.0-issue-754.json --repository . --release-candidate "$(git rev-parse HEAD)"
actionlint .github/workflows/publish-snapshot.yml .github/workflows/release.yml .github/workflows/release-generic.yml
```

Expected: unit fixtures pass; the live manifest intentionally reports HOLD because PRs 1-5 and final evidence are incomplete. HOLD is the correct PR 1 production state, not a failure of the unit tests.

- [ ] **Step 6: Commit PR 1 release enforcement**

```text
Prevent 1.12.0 publication before the serializer stack is proven

Constraint: The hold must fail closed across snapshot, release, and tag side effects
Rejected: Manual bypass input | It cannot prove the exact release head
Confidence: high
Scope-risk: broad
Directive: Keep the live manifest on HOLD until PR 5 exact-head evidence passes
Tested: Validator fixtures, workflow dependency assertions, ABI report, checksums
Not-tested: Repository tag ruleset and release App require post-merge authority
```

### Task 5: Close PR 1 And Gate External Repository Settings

**Complexity:** High
**Depends on:** Task 4
**Pattern skills:** `verification-before-completion`, `requesting-code-review`

**Files:**
- Create: `.github/release-holds/1.12.0-github-settings.json`
- Modify: `scripts/issue-754-github-settings.py`
- Modify: `scripts/test_issue_754_github_settings.py`
- Modify: `docs/evidence/issue-754/contract/abi-report.json`
- Modify: `docs/evidence/issue-754/contract/release-hold-report.json`
- Create: `docs/evidence/issue-754/contract/github-settings-report.json`
- Create: `docs/evidence/issue-754/contract/github-probe-report.json`
- Modify: `docs/evidence/issue-754/contract/SHA256SUMS`
- Modify: PR 1 body only after authorization and exact-head proof

- [ ] **Step 1: Run PR 1 inherited verification**

```bash
repo-test-summary -- ./gradlew :bluetape4k-io:test :bluetape4k-json:test :bluetape4k-avro:test --no-configuration-cache
./gradlew :bluetape4k-jackson2:compileKotlin :bluetape4k-jackson3:compileKotlin :bluetape4k-fastjson2:compileKotlin --no-configuration-cache
python3 -m unittest scripts/test_check_release_holds.py -v
python3 -m unittest scripts/test_issue_754_github_settings.py -v
bash scripts/check-serializer-buffer-abi.sh --build-current --expected-head "$(git rev-parse HEAD)"
actionlint .github/workflows/publish-snapshot.yml .github/workflows/release.yml .github/workflows/release-generic.yml
git diff --check origin/develop...HEAD
```

Expected: all test/ABI/static checks pass; the live release decision remains HOLD for the documented incomplete-stack reasons only.

- [ ] **Step 2: Complete six-lens pre-PR review and commit the converged head**

Run performance, stability, security, ops, developer/API, and caller lenses against the exact PR 1 diff. Repair all P0/P1 and rerun affected proof.

- [ ] **Step 3: Push/create PR 1 and wait for exact-head CI**

Target `develop`, head `feat/issue-754-buffer-contract`, assign `debop`, mirror issue milestone/labels, and do not use `Closes #754`. Stop at a merge-ready report with exact PR/head and request fresh merge approval.

- [ ] **Step 4: After approved merge, verify tree equality and sync descendants**

```bash
git rev-parse "$PR1_HEAD^{tree}"
git rev-parse "$PR1_MERGE^{tree}"
```

Expected: equal tree IDs. If unequal, invalidate the evidence and rerun PR 1's full gate on the merge SHA before rebasing PR 2.

- [ ] **Step 5: Stop for separate GitHub setting approval**

Before asking, run the read-only snapshot command below and checksum its sanitized output. Report the exact repository; proposed ruleset `release-tags-1.12.0`; protected patterns `1.12.0` and `release-gate-probe/issue-754/*`; distinct tag/settings App installations and exact permissions; environments `snapshot-publish-1.12.0` and `release-tag-1.12.0`; the five secret names to remove from repository scope while retaining them in legacy `maven-central-release`; release-only `RELEASE_TAG_APP_ID`/`RELEASE_TAG_APP_PRIVATE_KEY` and `RELEASE_SETTINGS_APP_ID`/`RELEASE_SETTINGS_APP_PRIVATE_KEY`; deployment-ref restrictions; and tested rollback input. Verify the authorized credential source can provide all required values before mutation, while recording names/presence only. Only after fresh approval may the operator create/update settings, install environment secrets from stdin, delete old-scope copies, and run probes. Never read or record secret values and never exercise production tag `1.12.0` during a probe.

```bash
python3 scripts/issue-754-github-settings.py snapshot --repository "$OWNER/$REPO" --output .codex/issue-754/pre-state.json
```

- [ ] **Step 6: After approval, apply and read back the protected settings**

Use the checked configuration body in `.github/release-holds/1.12.0-github-settings.json`. The script issues bounded `gh api` requests to `repos/$OWNER/$REPO/rulesets`, `repos/$OWNER/$REPO/environments/{environment_name}`, environment deployment policies/secrets, and repository/environment secret-name endpoints. It captures both App/installation identities and installed permissions, rejects equal IDs, repository-scoped copies, tag-App Administration, settings-App Contents write, or settings-App bypass membership, then records sanitized before/after state. It verifies only held jobs reference the 1.12.0 environments, confirms repository copies of all five Maven/signing secrets no longer exist, and confirms `maven-central-release` still owns exactly those five names for generic releases before proceeding. Secret installation uses stdin-backed `gh secret set NAME --env ENV`; values never appear in reports or command arguments. Rollback similarly restores names/scopes from the authorized source rather than from the sanitized pre-state.

```bash
python3 scripts/issue-754-github-settings.py apply --repository "$OWNER/$REPO" --config .github/release-holds/1.12.0-github-settings.json --pre-state .codex/issue-754/pre-state.json
python3 scripts/issue-754-github-settings.py verify --repository "$OWNER/$REPO" --config .github/release-holds/1.12.0-github-settings.json --output .codex/issue-754/post-state.json
```

- [ ] **Step 7: Prove current and historical workflow attempts cannot bypass the hold**

Create a unique temporary probe branch at the pinned pre-hold commit, dispatch the pre-hold snapshot workflow against that branch, then delete the branch after the run. Also request a rerun of a retained pre-hold publication run where available. Both must fail before credential-bearing steps because old YAML has no qualifying environment and old scopes contain no credentials. Record branch create/delete, run IDs, original SHA/ref, job conclusions, environment-deployment evidence, and secret-name presence/absence only. An unavailable retained run is recorded as N/A, but the old-ref dispatch and branch cleanup are mandatory.

```bash
python3 scripts/issue-754-github-settings.py prove-workflow-isolation --repository "$OWNER/$REPO" --pre-hold-ref 90b267871e9154f242e6de7ee9fd0539f83e509e --output .codex/issue-754/workflow-isolation.json
```

- [ ] **Step 8: Prove the probe lifecycle and fail-safe cleanup**

Capture repository rulesets and the `release-tag-1.12.0` environment with exact `gh api` responses. A validator must assert both protected patterns, the dedicated App installation as the sole bypass actor, required environment reviewers, and absence of token/user/team bypasses. Then record denied-actor and allowed-App create/update/delete exit codes for a unique `release-gate-probe/issue-754/$PROBE_ID` tag, and prove the probe ref is absent afterward. Persist sanitized requests, responses, actor IDs, timestamps, commands, exit codes, and assertions in `release-hold-report.json`; any failed assertion or cleanup keeps the hold red and blocks Task 6.

```bash
python3 scripts/issue-754-github-settings.py probe --repository "$OWNER/$REPO" --config .github/release-holds/1.12.0-github-settings.json --output .codex/issue-754/probe.json
python3 scripts/check-release-holds.py --audit-github-settings .codex/issue-754
```

Journal every mutation with endpoint, prior/current ID, normalized prior/intended hash, and secret-name presence transition. On apply/read-back/isolation/probe failure, read current state first and permit the fixture-tested rollback only when every ID/hash/name-presence row matches a recognized journaled intermediate state. Unknown concurrent drift or ambiguous response loss enters blocked recovery without further mutation. For an eligible rollback, restore from the captured pre-state/authorized credential source, verify restoration, and keep the release hold red. Copy sanitized, checksummed settings/probe reports into the contract evidence paths only after all assertions and cleanup pass. Task 6 remains blocked until this gate is green.

```bash
python3 scripts/issue-754-github-settings.py rollback --repository "$OWNER/$REPO" --pre-state .codex/issue-754/pre-state.json
python3 scripts/issue-754-github-settings.py verify-rollback --repository "$OWNER/$REPO" --pre-state .codex/issue-754/pre-state.json
```

## PR 2 - Core Serializers

### Task 6: Add JDK Native Buffer Paths

**Complexity:** High
**Depends on:** merged/verified PR 1, successful Task 5 repository-setting/probe gate, and approved PR 2 branch base
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializer.kt`
- Modify: `io/io/build.gradle.kts`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerBufferTestSupport.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializerByteBufferTest.kt`
- Modify: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/JdkBinarySerializerSecurityTest.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/JdkGlobalFilterBufferForkTest.kt`

- [ ] **Step 1: Write RED tests for direct stream bridging and filter parity**

Cover fixed output, duplicate-backed input, null/default/custom/global `ObjectInputFilter`, malformed payloads, read-only input, overflow, deterministic writer/read/flush/close `Error`, source preservation, and retry. Run the full integration fault matrix at first write, after N bytes, flush, and close, including primary+cleanup, overflow+cleanup, and fatal+cleanup combinations. Assert exact primary/cause/suppressed identities, canaries, position rollback, reference release, and successful retry. Injected fatal rows assert original `Error` identity and no overflow translation. Launch `JdkGlobalFilterBufferForkTest` in an isolated JVM with a pinned `-Djdk.serialFilter=...` and a payload rejected only by that filter; compare null-filter ByteArray and native-ByteBuffer rejection exception/cause families and prove no process-global filter state leaks into other tests. Add a bounded barrier-synchronized test using one shared serializer and one buffer per call; mix malformed/overflow and valid calls, then prove clean sequential success.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :bluetape4k-io:test --tests '*JdkBinarySerializerByteBufferTest' --tests '*JdkBinarySerializerSecurityTest' --tests '*JdkGlobalFilterBufferForkTest' --no-configuration-cache
```

Expected: default fallback passes basic behavior but native-path assertions fail.

- [ ] **Step 3: Override output/input using caller-confined streams**

Capture `start = target.position()`, write through `ObjectOutputStream(ByteBufferOutputStream.fixed(target.duplicate()))`, complete flush/close and failure classification, compute bytes written from the duplicate, and update the original target position only on complete success. Input reads `ObjectInputStream(ByteBufferInputStream(source.duplicate()))` and installs the same configured/global filter before `readObject`. Flush/close failures keep the original position at `start`; preserve the fatal/overflow graph.

- [ ] **Step 4: Run GREEN and full IO tests**

```bash
./gradlew :bluetape4k-io:test --no-configuration-cache
```

- [ ] **Step 5: Commit JDK buffer paths**

```text
Reuse caller buffers without weakening JDK deserialization filters

Constraint: Install the configured or global filter before the first readObject
Confidence: high
Scope-risk: moderate
Directive: Keep untrusted input unsupported without caller resource budgets
Tested: Buffer contract, filter parity, malformed input, overflow, retry
```

### Task 7: Add Kryo Native Buffer Paths

**Complexity:** High
**Depends on:** Task 6
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/KryoBinarySerializer.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/KryoBinarySerializerByteBufferTest.kt`
- Modify: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/SecureKryoBinarySerializerTest.kt`
- Modify: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/KryoFastBinarySerializerTest.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerBufferLifecycleTest.kt`

- [ ] **Step 1: Write RED tests for heap/direct/slice paths and pool lifecycle**

Assert explicit `ByteOrder.BIG_ENDIAN`, exact byte parity with the existing Kryo path, registration-required rejection parity, deterministic writer/read/flush/close `Error`, wrapper release after overflow/malformed/fatal input, clean success after failure, and concurrent instance use with one buffer per call. Run the same first-write/N-byte/flush/close dual-failure matrix as Task 6, asserting primary/cause/suppressed identity, canaries, rollback, and release. Serialize with a permissive instance, then deserialize under registration-required configuration through heap, direct, sliced, and `asReadOnlyBuffer()` inputs; each must reject with the ByteArray exception/cause family, preserve source state, and permit successful subsequent use. Fatal rows assert same-instance propagation, no overflow translation, target-position policy, and deterministic tracked wrapper/Kryo release before retry. Barrier-synchronized mixed valid/invalid calls and explicit borrow/return counters are pass criteria; bounded `ReferenceQueue` collection is diagnostic only and never a GC-timing gate.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :bluetape4k-io:test --tests '*Kryo*ByteBufferTest' --tests '*SecureKryoBinarySerializerTest' --tests '*KryoFastBinarySerializerTest' --no-configuration-cache
```

- [ ] **Step 3: Implement call-scoped Kryo `ByteBufferInput`/`ByteBufferOutput` paths**

Borrow only the configured Kryo instance. For output, capture the original position, bind a new per-call non-growing Kryo `ByteBufferOutput` to `target.slice().order(ByteOrder.BIG_ENDIAN)`, capture its written count before locally closing/clearing the wrapper, release the pooled Kryo, then advance the original target only after every cleanup step succeeds. Never pool or return buffer wrappers. Input uses a new per-call BIG_ENDIAN-preserving `ByteBufferInput` over a duplicate/slice, locally clears it, and releases only Kryo. Translate recognized Kryo overflow through the shared classifier. Retention tests prove wrappers and caller buffers are not retained after success or failure.

- [ ] **Step 4: Run GREEN and wire/security compatibility**

```bash
./gradlew :bluetape4k-io:test --tests '*Kryo*' --no-configuration-cache
```

- [ ] **Step 5: Commit Kryo paths**

```text
Bind Kryo calls to caller buffers while preserving pool and wire contracts

Constraint: Release every call-scoped wrapper before publishing target position
Confidence: high
Scope-risk: moderate
Directive: Treat registration mode and byte order as compatibility authority
Tested: Default, fast, secure, lifecycle, concurrency, and wire parity tests
```

### Task 8: Add Fory Input And Prove Output Fallback

**Complexity:** Medium
**Depends on:** Task 7
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `io/io/src/main/kotlin/io/bluetape4k/io/serializer/ForyBinarySerializer.kt`
- Modify: `io/io/build.gradle.kts`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/ForyBinarySerializerByteBufferTest.kt`
- Modify: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/SecureForyBinarySerializerTest.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerRollingCompatibilityTest.kt`
- Create: `io/io/src/test/kotlin/io/bluetape4k/io/serializer/BinarySerializerCompatibilityForkMain.kt`
- Modify: `io/io/src/test/resources/compat/issue-754/pre-change/manifest.json`
- Modify: `io/io/README.md`
- Modify: `io/io/README.ko.md`
- Create: `docs/evidence/issue-754/core/compatibility-report.json`
- Create: `docs/evidence/issue-754/core/SHA256SUMS`

- [ ] **Step 1: Write RED input/security tests**

Cover compile-time resolution of exact `ThreadSafeFory.deserialize(ByteBuffer)`, heap/direct/read-only input, source state, malformed data, registration-required rejection, deterministic backend `Error` with same-instance propagation and call-local reference release, success after failure, and barrier-synchronized bounded concurrency on one shared serializer with one buffer per call. Mix invalid and valid calls, then prove clean sequential success. Add a control asserting `serializeTo` remains the allocating default fallback.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :bluetape4k-io:test --tests '*Fory*ByteBufferTest' --tests '*SecureForyBinarySerializerTest' --no-configuration-cache
```

- [ ] **Step 3: Implement only the supported Fory input path**

Invoke the official `ThreadSafeFory.deserialize(source.duplicate())` overload directly and release call-local references after the call. Do not introduce a manual `MemoryBuffer` wrapper and do not add a Fory output override because primary-source and benchmark evidence do not justify an allocation claim.

Add backend/source KDoc for JDK, Kryo, and Fory that distinguishes native input/output from allocating fallback; states that `source.remaining()` bounds copied bytes but does not bound decompression, graph depth/references, object count, native memory, or CPU; preserves configured filter/registration behavior; and requires trusted input or caller-enforced resource budgets.

- [ ] **Step 4: Register and prove the rolling compatibility task**

Add `issue754CoreCompatibilityTest` in `io/io/build.gradle.kts` as a forked JavaExec/test gate whose inputs are the pinned manifest plus old jar under `.codex/compat/issue-754/90b267871e9154f242e6de7ee9fd0539f83e509e/`, current classes/jar, and fork-main fixture. Its report output is `build/reports/issue-754/core-compatibility.json`. First assert RED before registration, then verify discovery and GREEN:

```bash
./gradlew :bluetape4k-io:tasks --all --no-configuration-cache
./gradlew :bluetape4k-io:issue754CoreCompatibilityTest --no-configuration-cache -Pissue754PrechangeManifest=io/io/src/test/resources/compat/issue-754/pre-change/manifest.json
```

- [ ] **Step 5: Run full PR 2 inherited gate**

```bash
repo-test-summary -- ./gradlew :bluetape4k-io:test :bluetape4k-json:test --no-configuration-cache
./gradlew :bluetape4k-io:issue754CoreCompatibilityTest --no-configuration-cache -Pissue754PrechangeManifest=io/io/src/test/resources/compat/issue-754/pre-change/manifest.json
bash scripts/check-serializer-buffer-abi.sh --build-current --expected-head "$(git rev-parse HEAD)"
python3 -m unittest scripts/test_check_release_holds.py -v
git diff --check origin/develop...HEAD
```

- [ ] **Step 6: Commit evidence and finish PR 2 delivery**

Commit the core compatibility manifest/checksums, run six-lens review, push/create PR 2, wait for exact-head CI, report merge-ready, and stop for fresh merge approval. After merge, require head/merge tree equality before rebasing PR 3.

## PR 3 - JSON Serializers

### Task 9: Freeze JSON Authority And Add Jackson 2/3 Native Paths

**Complexity:** High
**Depends on:** merged/verified PR 2
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt`
- Modify: `io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JacksonSerializer.kt`
- Create: `io/jackson2/src/test/resources/compat/issue-754/pre-change/jackson2-default.json`
- Create: `io/jackson2/src/test/resources/compat/issue-754/pre-change/jackson2-polymorphic.json`
- Create: `io/jackson3/src/test/resources/compat/issue-754/pre-change/jackson3-default.json`
- Create: `io/jackson3/src/test/resources/compat/issue-754/pre-change/jackson3-polymorphic.json`
- Create: `io/fastjson2/src/test/resources/compat/issue-754/pre-change/fastjson2-default.json`
- Create: `io/fastjson2/src/test/resources/compat/issue-754/pre-change/fastjson2-default.jsonb`
- Create: `io/fastjson2/src/test/resources/compat/issue-754/pre-change/fastjson2-type-metadata.jsonb`
- Create: `io/json/src/test/resources/compat/issue-754/pre-change/json-manifest.json`
- Create: `io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonSerializerByteBufferTest.kt`
- Create: `io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonSerializerByteBufferTest.kt`
- Create: `io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonSerializerRollingCompatibilityTest.kt`
- Create: `io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonSerializerRollingCompatibilityTest.kt`
- Modify: both modules' `AbstractJsonSerializerTest.kt`

- [ ] **Step 1: Build and checksum the pinned JSON/JSONB authority**

In the clean `90b267871e9154f242e6de7ee9fd0539f83e509e` worktree, build Jackson 2, Jackson 3, and Fastjson2 jars and generate configured default plus crafted type-metadata fixtures. Record artifact paths/SHA-256, mapper/features/modules/polymorphism settings, JSON versus JSONB mode, producer commit, and fixture checksums in `json-manifest.json`; keep jars only under ignored `.codex/compat/issue-754/`.

```bash
./gradlew :bluetape4k-jackson2:jar :bluetape4k-jackson3:jar :bluetape4k-fastjson2:jar --no-configuration-cache
```

- [ ] **Step 2: Write symmetric RED contract/security tests**

Run the same heap/direct/slice/read-only/position/limit/mark/order/overflow/retry matrix against Jackson 2 and 3. Add deterministic first-write/N-byte/flush/close fault injection, including primary+cleanup, overflow+cleanup, and fatal+cleanup combinations, with exact primary/cause/suppressed identities, canaries, same-instance fatal propagation, no overflow translation, unchanged original position, reference release, and subsequent success. Crafted type metadata must have the same result as each mapper's ByteArray path; the buffer path must reuse the exact mapper and must not enable default typing. Rolling tests require the pinned old reader to consume new buffer output and the new reader to consume each frozen output, with mapper/polymorphism parity recorded for every direction. Each reusable mapper/serializer also gets a bounded barrier-synchronized mixed valid/invalid test using one buffer per call followed by clean sequential success.

- [ ] **Step 3: Verify RED**

```bash
./gradlew :bluetape4k-jackson2:test :bluetape4k-jackson3:test --tests '*JacksonSerializerByteBufferTest' --no-configuration-cache
```

- [ ] **Step 4: Implement mapper-native stream paths**

Capture `start = target.position()`, use the mapper's stream writer over `ByteBufferOutputStream.fixed(target.duplicate())`, complete flush/close and failure classification, compute the duplicate's written count, and update the original position only on complete success. Reader input uses `ByteBufferInputStream(source.duplicate())`. Flush/close failures keep the original at `start`; preserve `JsonSerializationException`, raw read-only/overflow failures, and common cleanup ordering.

Add backend/source KDoc stating capability, allocation behavior, mapper/polymorphism reuse, trusted-input/resource-budget expectations, and that a source limit bounds consumed encoded bytes but not decompression, nesting, references, object count, native memory, or CPU.

- [ ] **Step 5: Run GREEN and full Jackson module tests**

```bash
./gradlew :bluetape4k-jackson2:test :bluetape4k-jackson3:test --no-configuration-cache
```

Expected: both rolling matrices pass solely against the pinned jars/fixtures and current outputs; write their rows into `docs/evidence/issue-754/json/compatibility-report.json`.

- [ ] **Step 6: Commit Jackson paths**

```text
Route Jackson serializers through caller-owned buffers without mapper drift

Constraint: Reuse the existing ObjectMapper and polymorphic configuration
Confidence: high
Scope-risk: moderate
Directive: Keep Jackson 2 and 3 contract tests behaviorally symmetric
Tested: Buffer matrix, malformed input, polymorphic security, retry, module tests
```

### Task 10: Prove Fastjson2 Fallback And Close PR 3

**Complexity:** Medium
**Depends on:** Task 9
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/FastjsonSerializer.kt`
- Modify: `.github/workflows/ci.yml`
- Create: `io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/FastjsonSerializerByteBufferTest.kt`
- Create: `io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/FastjsonSerializerRollingCompatibilityTest.kt`
- Modify: `io/fastjson2/src/test/kotlin/io/bluetape4k/fastjson2/AbstractJsonSerializerTest.kt`
- Modify: JSON module README pairs for Jackson 2/3/Fastjson2
- Create: `docs/evidence/issue-754/json/compatibility-report.json`
- Create: `docs/evidence/issue-754/json/SHA256SUMS`

- [ ] **Step 1: Add fallback and `SupportAutoType` negative tests**

Prove heap/direct/read-only input preservation and JSONB parity through the interface default. Crafted type metadata must not cause the buffer path to enable `SupportAutoType`; output remains an explicitly allocating fallback control. Require the pinned old reader to consume new fallback output and the new reader to consume all frozen JSON/JSONB/type-metadata fixtures; record the bidirectional rows and `SupportAutoType` result in `json/compatibility-report.json`. Add a bounded barrier-synchronized mixed malformed/valid test on one shared serializer with one buffer per call, followed by clean sequential success.

- [ ] **Step 2: Run the inherited-fallback characterization gate**

```bash
./gradlew :bluetape4k-fastjson2:test --tests '*FastjsonSerializerByteBufferTest' --no-configuration-cache
```

Expected: tests pass using the reviewed PR 1 default after only documentation/capability wiring; no unsupported native output path is introduced. A new PR 3 RED cycle is N/A for Fastjson2 because PR 3 intentionally changes no Fastjson2 runtime behavior; the PR 1 default already had RED/GREEN proof. Include a negative mutation fixture that would fail if `SupportAutoType` were enabled so the characterization is behavior-sensitive.

Add backend/source KDoc with the same trust/resource warnings as Jackson and explicitly label JSONB output as an allocating fallback. Its input fallback allocates exactly `source.remaining()` heap bytes before backend controls execute; the source limit still does not cap parser depth, references, object count, native memory, or CPU.

- [ ] **Step 3: Run the full inherited JSON gate**

Before running it, add `io/fastjson2/**` to the CI path filter and `:bluetape4k-fastjson2:test` to the matching required CI job. Validate the workflow with `actionlint` so Fastjson2 buffer regressions cannot be skipped by path routing.

```bash
repo-test-summary -- ./gradlew :bluetape4k-io:test :bluetape4k-json:test :bluetape4k-jackson2:test :bluetape4k-jackson3:test :bluetape4k-fastjson2:test --no-configuration-cache
actionlint .github/workflows/ci.yml
bash scripts/check-serializer-buffer-abi.sh --build-current --expected-head "$(git rev-parse HEAD)"
python3 -m unittest scripts/test_check_release_holds.py -v
git diff --check origin/develop...HEAD
```

- [ ] **Step 4: Commit evidence and finish PR 3 delivery**

Commit the JSON compatibility manifest/checksums, run six-lens review, push/create PR 3, wait for exact-head CI, report merge-ready, and stop for fresh merge approval. Verify head/merge tree equality before rebasing PR 4.

## PR 4 - Avro Serializers

### Task 11: Freeze Avro OCF Authority And Build The Semantic Oracle

**Complexity:** High
**Depends on:** merged/verified PR 3
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/reflect-default.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/generic-default.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/specific-default.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/specific-list-default.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/specific-deflate.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/specific-zstd-3.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/specific-zstd-3-checksum.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/specific-zstd-3-checksum-bufferpool.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/specific-snappy.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/specific-xz.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/pre-change/specific-bzip.avro`
- Create: `io/avro/src/test/resources/compatibility/issue-754/avro/manifest.json`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/compat/AvroOcfSemanticOracle.kt`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/compat/AvroOcfCompatibilityTest.kt`

- [ ] **Step 1: Generate frozen OCF fixtures from the pinned base**

Build the Avro module at `90b267871e9154f242e6de7ee9fd0539f83e509e`, generate reflect/generic/specific/list fixtures across the existing codec matrix, and record producer commit, schema, codec, reader class, Java/Kotlin/Gradle versions, and SHA-256. Do not regenerate from the current implementation after buffer overrides exist.

- [ ] **Step 2: Write RED semantic-oracle tests**

```bash
./gradlew :bluetape4k-avro:test --tests '*AvroOcfCompatibilityTest' --no-configuration-cache
```

Expected: FAIL because the parsed oracle does not yet exist. Include deliberate corrupt/truncated/trailing fixtures so a test cannot pass by comparing decoded records alone.

- [ ] **Step 3: Implement the parsed OCF semantic oracle**

Validate `Obj\u0001`, embedded schema equality, exact codec, user metadata, record order/value, positive block counts, in-bounds sizes, per-file sync markers, clean EOF, and no trailing corruption. Permit only sync marker, block partition, and codec-byte differences; do not normalize bytes.

- [ ] **Step 4: Run GREEN and commit the frozen authority/oracle**

```bash
./gradlew :bluetape4k-avro:compileKotlin :bluetape4k-avro:compileTestKotlin --no-configuration-cache
./gradlew :bluetape4k-avro:test --tests '*AvroOcfCompatibilityTest' --no-configuration-cache
```

### Task 12: Implement Reflect, Generic, Specific, And List Paths

**Complexity:** High
**Depends on:** Task 11
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroReflectSerializer.kt`
- Modify: `io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroGenericRecordSerializer.kt`
- Modify: `io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroSpecificRecordSerializer.kt`
- Create: `io/avro/src/main/kotlin/io/bluetape4k/avro/impl/AvroBufferSerializationSupport.kt`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/impl/AvroBufferContractSupport.kt`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/impl/DefaultAvroReflectSerializerBufferTest.kt`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/impl/DefaultAvroGenericRecordSerializerBufferTest.kt`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/impl/DefaultAvroSpecificRecordSerializerBufferTest.kt`

- [ ] **Step 1: Write RED implementation tests**

Require `DataFileWriter` over the fixed adapter bound to a duplicate and `DataFileStream` over a duplicate-backed input stream. Cover first-write/N-byte/flush/close overflow and fatal injection, including primary+cleanup, overflow+cleanup, and fatal+cleanup combinations; assert exact primary/cause/suppressed identities, canaries, release, and retry. Also cover codec parity, schema evolution/default/alias behavior, logical types, and success after failure. Fatal rows preserve original `Error` identity, avoid overflow translation, and keep the original target position at its captured start. For every reusable reflect/generic/specific/list serializer, run bounded barrier-synchronized mixed malformed/overflow/valid calls with one buffer per call, then prove clean sequential success.

- [ ] **Step 2: Verify RED**

```bash
./gradlew :bluetape4k-avro:test --tests '*SerializerBufferTest' --no-configuration-cache
```

- [ ] **Step 3: Implement trusted/caller-bounded native stream paths**

For output, capture the original target position, serialize through `DataFileWriter` and `ByteBufferOutputStream.fixed(target.duplicate())`, finish flush/close and failure classification, compute written count from the duplicate, and commit the original position only on complete success. Reflect input uses `ReflectDatumReader(readerSchema, readerSchema)` where `readerSchema = schemaOf(clazz)`; generic uses `GenericDatumReader<GenericData.Record>(suppliedSchema)`; specific/list uses `SpecificDatumReader(clazz)`. Let `DataFileStream` install the embedded writer schema. Apply overflow/fatal classification before the existing Avro null/empty policy.

Add backend/source KDoc that identifies native OCF stream paths, caller ownership, null/empty and failure policy, trusted/caller-bounded input, and the fact that a `ByteBuffer` limit bounds container bytes only, not codec expansion, datum nesting/references, record count, native memory, or CPU.

- [ ] **Step 4: Run GREEN**

```bash
./gradlew :bluetape4k-avro:test --no-configuration-cache
```

### Task 13: Add Bounded Hostile Avro Proof And Close PR 4

**Complexity:** High
**Depends on:** Task 12
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `io/avro/build.gradle.kts`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/impl/AvroByteBufferSecurityParityTest.kt`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/impl/AvroHostileFixtureForkMain.kt`
- Create: `io/avro/src/test/kotlin/io/bluetape4k/avro/impl/AvroHostileFixtureForkTest.kt`
- Create: `io/avro/src/test/resources/issue-754/hostile/incompatible-schema.avro`
- Create: `io/avro/src/test/resources/issue-754/hostile/malformed-metadata.avro`
- Create: `io/avro/src/test/resources/issue-754/hostile/unsupported-codec.avro`
- Create: `io/avro/src/test/resources/issue-754/hostile/truncated-block.avro`
- Create: `io/avro/src/test/resources/issue-754/hostile/oversized-declared-block.avro`
- Create: `io/avro/src/test/resources/issue-754/hostile/decompression-expansion.avro`
- Create: `io/avro/src/test/resources/issue-754/hostile/deep-nesting.avro`
- Create: `io/avro/src/test/resources/issue-754/hostile/excessive-record-count.avro`
- Create: `io/avro/src/test/resources/issue-754/hostile/trailing-bytes.avro`
- Modify: `io/avro/README.md`, `io/avro/README.ko.md`
- Modify: `io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroReflectSerializer.kt`
- Modify: `io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroGenericRecordSerializer.kt`
- Modify: `io/avro/src/main/kotlin/io/bluetape4k/avro/impl/DefaultAvroSpecificRecordSerializer.kt`
- Create: `docs/evidence/issue-754/avro/compatibility-report.json`
- Create: `docs/evidence/issue-754/avro/security-parity-report.json`
- Create: `docs/evidence/issue-754/avro/SHA256SUMS`

- [ ] **Step 1: Add bounded hostile fixtures and RED parity tests**

Cover incompatible schemas, invalid logical conversions, malformed metadata, unsupported codecs, corrupt blocks, oversized declared blocks, bounded decompression expansion, deterministic deep nesting, excessive collection/reference shapes, and bounded excessive record count. Pin fixture files to at most 1 MiB, nesting to 64 levels, and declared record count to 10,000. `AvroHostileFixtureForkTest` launches one fixture per child JVM sequentially with `-Xmx128m`, a 5-second wall-clock timeout, retained stdout/stderr, and forced termination plus join. A timeout, OOM, unexpected/nonzero exit, or surviving process fails the gate. Every expected-rejection child exits zero only after asserting preserved source state and existing null/empty policy.

Task 13 owns only Avro implementation repairs exposed by these tests. If evidence requires a change to a PR 1 interface or PR 2/3 backend, move the exact negative test and behavior to its owning task, restack descendants, and rerun that slice's full gate; do not repair earlier trust boundaries opportunistically in PR 4.

- [ ] **Step 2: Verify and repair security parity**

```bash
./gradlew :bluetape4k-avro:test --tests '*AvroByteBufferSecurityParityTest' --no-configuration-cache
./gradlew :bluetape4k-avro:test --tests '*AvroHostileFixtureForkTest' --no-configuration-cache
```

Expected: the new buffer path matches the existing null/empty/logging family policy while preserving source state.

- [ ] **Step 3: Run PR 4 full inherited gate**

Add `io/avro/**` to the IO path filter and require the sequential hostile fork test, full `:bluetape4k-avro:test`, and `:bluetape4k-avro:koverXmlReport` in the matching required IO job. Validate the exact routing before relying on PR CI; Testcontainers-backed work remains serialized separately.

```bash
repo-test-summary -- ./gradlew :bluetape4k-io:test :bluetape4k-json:test :bluetape4k-jackson2:test :bluetape4k-jackson3:test :bluetape4k-fastjson2:test :bluetape4k-avro:test --no-configuration-cache
actionlint .github/workflows/ci.yml
bash scripts/check-serializer-buffer-abi.sh --build-current --expected-head "$(git rev-parse HEAD)"
python3 -m unittest scripts/test_check_release_holds.py -v
git diff --check origin/develop...HEAD
```

- [ ] **Step 4: Commit evidence and finish PR 4 delivery**

Run six-lens review, push/create PR 4, wait for exact-head CI, report merge-ready, and stop for fresh merge approval. Verify head/merge tree equality before rebasing PR 5.

## PR 5 - Allocation Proof And Documentation

### Task 14: Add The PR 5 Benchmark And Validation Harness

**Complexity:** High
**Depends on:** merged/verified PR 4
**Pattern skills:** `bluetape-kotlin-patterns`, `test-driven-development`

**Files:**
- Modify: `.github/workflows/ci.yml`
- Create: `benchmark/serializer-bytebuffer-benchmark/build.gradle.kts`
- Create: `benchmark/serializer-bytebuffer-benchmark/src/benchmark/kotlin/io/bluetape4k/serializer/benchmark/SerializerByteBufferBenchmark.kt`
- Create: `benchmark/serializer-bytebuffer-benchmark/src/benchmark/avro/Issue754BenchmarkRecord.avsc`
- Create: `benchmark/serializer-bytebuffer-benchmark/src/test/kotlin/io/bluetape4k/serializer/benchmark/SerializerBenchmarkContractTest.kt`
- Create: `benchmark/serializer-bytebuffer-benchmark/src/test/resources/junit-platform.properties`
- Create: `benchmark/serializer-bytebuffer-benchmark/src/test/resources/logback-test.xml`
- Create: `benchmark/serializer-bytebuffer-benchmark/src/test/resources/issue-754-benchmark-cells.json`
- Create: `benchmark/serializer-bytebuffer-benchmark/README.md`
- Create: `benchmark/serializer-bytebuffer-benchmark/README.ko.md`
- Create: `scripts/check-issue-754-allocation.py`
- Create: `scripts/tests/test_check_issue_754_allocation.py`
- Create: `scripts/check-issue-754-docs.py`
- Create: `scripts/tests/test_check_issue_754_docs.py`
- Create: `scripts/check-issue-754-release-candidate.py`
- Create: `scripts/tests/test_check_issue_754_release_candidate.py`
- Create: `.github/workflows/validate-issue-754-release-candidate.yml`
- Create: `docs/evidence/issue-754/pr5/module-applicability.json`

- [ ] **Step 1: Add RED module/validator tests**

Test task discovery, unique run IDs, existing-directory rejection, required metric/cardinality validation, finite values, `B > 0`, 10% median improvement, 5% per-fork ceiling, and disagreement between two runs. Run metadata tests reject producer-commit, `testedCodeTreeSha256`, benchmark-jar SHA-256, JDK vendor/version, normalized JVM flags, heap/GC, OS, or architecture mismatch. Add RED `verify-final-head` fixtures for non-ancestor producer, wrong final commit, code-tree/jar drift, checksum failure, aggregation mismatch, and a commit created after a previously green gate. The checked cell manifest enumerates every supported implementation, direction, payload, and heap/direct candidate; tests reject missing, duplicate, extra, or misclassified cells. Fory output and Fastjson2 JSONB fallback are explicit controls and can never receive an allocation-improving label. The median implementation sorts finite values and selects the middle of the odd-sized 5-iteration and 3-fork sets; it never pools samples across runs. Docs-validator fixtures cover missing/mismatched locale sections, stale public names, absent exact safety claims, argument/example gaps, and malformed manifests. Candidate-validator/workflow fixtures cover malformed checksums/evidence rows, wrong candidate/head/tree/code-tree digest, wrong request/run identity, HOLD/PASS, exact inputs/run-name/check/artifact, read-only permissions, no secrets/environment, and deterministic retrieval. All are RED before implementation so every PR 5 source/build/workflow change is complete before Task 15.

- [ ] **Step 2: Verify module auto-registration and RED**

```bash
./gradlew projects --no-configuration-cache
./gradlew :serializer-bytebuffer-benchmark:tasks --all --no-configuration-cache
python3 -m unittest scripts/tests/test_check_issue_754_allocation.py -v
python3 -m unittest scripts/tests/test_check_issue_754_docs.py -v
python3 -m unittest scripts/tests/test_check_issue_754_release_candidate.py -v
./gradlew :serializer-bytebuffer-benchmark:test --no-configuration-cache
```

Expected: project and standard `benchmark`/`benchmarkBenchmark` tasks exist; validator tests initially fail before implementation.

- [ ] **Step 3: Configure pinned benchmark protocol**

Use `@State(Scope.Thread)`, 3 forks, five 1-second warmups, five 1-second measurements, thread-confined reused buffers, pre-encoded input, and `Blackhole`/return consumption. Compare ByteArray, reused heap, and supported reused direct cells by implementation/direction/payload.

- [ ] **Step 4: Implement `gcProfile`**

The task depends on `benchmarkBenchmarkJar`, executes it with `-prof gc`, is never up-to-date, requires `-PbenchmarkRunId`, rejects an existing output directory, writes JSON under `build/reports/benchmarks/issue-754/$RUN_ID/`, and invokes `scripts/check-issue-754-allocation.py validate-run` so missing GC metrics fail the task. Each run writes producer commit, `testedCodeTreeSha256`, benchmark-jar SHA-256, JDK vendor/version, normalized JVM flags, heap/GC, OS, and architecture. Add `aggregate-runs`, which accepts exactly two distinct run IDs/paths, validates each full 3-fork by 5-measurement cell matrix against the manifest, rejects any matrix or identity mismatch, applies the disagreement rule, and emits one deterministic `allocation-gate.json`. Implement `verify-final-head` in this same Task 14 GREEN step so it proves producer ancestry, final-commit identity, final code-tree/jar hashes, reproduced aggregation, full-head checksums, and post-gate staleness before any measurement begins. Implement the fail-closed docs/candidate validators and validation-only workflow with structured parsing, unknown/missing-field rejection, deterministic normalized output, exact path allowlists, nonzero HOLD/identity exits, exact-SHA checkout, request-specific artifact upload, and no publishing credentials/environment.

- [ ] **Step 5: Run validator GREEN and module hazards**

Add a benchmark path-filter output and required CI job that runs project/task discovery plus `:serializer-bytebuffer-benchmark:test`; include it in aggregate `ci-status`. Add required PR 5 job `issue-754-final-head`, which checks out the exact PR head SHA and runs the same `verify-final-head` command defined in Task 16; `ci-status` must require it whenever issue-754 proof/docs paths change. Record Nightly as N/A for heavyweight JMH execution, Kover as N/A for a benchmark harness with contract tests covered by the required job, and BOM/catalog/publishing as N/A because the root policy excludes benchmark modules from publication. The checked applicability report names each decision and its source evidence.

```bash
python3 -m unittest scripts/tests/test_check_issue_754_allocation.py -v
python3 -m unittest scripts/tests/test_check_issue_754_docs.py -v
python3 -m unittest scripts/tests/test_check_issue_754_release_candidate.py -v
./gradlew :serializer-bytebuffer-benchmark:test :serializer-bytebuffer-benchmark:tasks --all --no-configuration-cache
./gradlew projects --no-configuration-cache
actionlint .github/workflows/ci.yml
actionlint .github/workflows/validate-issue-754-release-candidate.yml
```

Expected: benchmark remains unpublished by root policy; no settings edit is needed because `includeModules("benchmark", false, false)` auto-registers it.

- [ ] **Step 6: Commit the benchmark harness**

```text
Make serializer allocation claims reproducible and fail closed

Constraint: kotlinx-benchmark profiler configuration cannot prove gc metrics here
Rejected: Throughput-only evidence | It does not establish allocation reduction
Confidence: high
Scope-risk: moderate
Directive: Use two fresh run IDs and retain raw per-fork JSON
Tested: Task discovery, metric/cardinality validation, median gate, module registration
```

### Task 15: Produce Two Fresh Allocation Runs

**Complexity:** High and heavyweight
**Depends on:** Task 14 committed with every PR 5 source/build/workflow change complete
**Pattern skills:** `verification-before-completion`

**Files:**
- Create: `docs/evidence/issue-754/pr5/run-1/jmh.json`
- Create: `docs/evidence/issue-754/pr5/run-1/environment.json`
- Create: `docs/evidence/issue-754/pr5/run-2/jmh.json`
- Create: `docs/evidence/issue-754/pr5/run-2/environment.json`
- Create: `docs/evidence/issue-754/pr5/allocation-gate.json`
- Create: `docs/evidence/issue-754/pr5/SHA256SUMS`

- [ ] **Step 1: Run the first fresh GC profile serially**

Freeze `MEASURED_HEAD` and `testedCodeTreeSha256` after confirming no remaining PR 5 source, build, validator, KDoc, or workflow edit. KDoc is already complete in Tasks 2/3/8/9/10/12. Any later covered-file change invalidates both runs and requires two new UUID runs; evidence from an invalidated identity is deleted rather than relabeled.

```bash
MEASURED_HEAD="$(git rev-parse HEAD)"
TESTED_CODE_TREE="$(python3 scripts/check-issue-754-allocation.py print-code-tree --repository .)"
RUN_1="$(uuidgen | tr '[:upper:]' '[:lower:]')"
./gradlew :serializer-bytebuffer-benchmark:gcProfile --no-configuration-cache --rerun-tasks -PbenchmarkRunId="$RUN_1" -Pissue754ProducerCommit="$MEASURED_HEAD" -Pissue754CodeTree="$TESTED_CODE_TREE"
```

Expected: exit 0; raw JMH JSON contains `gc.alloc.rate.norm` and emitted `gc.alloc.rate`, `gc.count`, `gc.time` metrics.

- [ ] **Step 2: Run a second distinct profile**

Generate and retain `RUN_2`, run the exact same command, and assert IDs and directories differ.

```bash
RUN_2="$(uuidgen | tr '[:upper:]' '[:lower:]')"
test "$RUN_1" != "$RUN_2"
./gradlew :serializer-bytebuffer-benchmark:gcProfile --no-configuration-cache --rerun-tasks -PbenchmarkRunId="$RUN_2" -Pissue754ProducerCommit="$MEASURED_HEAD" -Pissue754CodeTree="$TESTED_CODE_TREE"
```

- [ ] **Step 3: Validate each run independently**

```bash
./gradlew :serializer-bytebuffer-benchmark:validateGcProfile --no-configuration-cache -PbenchmarkRunId="$RUN_1"
./gradlew :serializer-bytebuffer-benchmark:validateGcProfile --no-configuration-cache -PbenchmarkRunId="$RUN_2"
```

Expected: each allocation-improving cell passes 10% median reduction and every candidate fork is at most 5% worse than its same-run baseline.

- [ ] **Step 4: Aggregate both immutable runs**

```bash
python3 scripts/check-issue-754-allocation.py aggregate-runs \
  --manifest benchmark/serializer-bytebuffer-benchmark/src/test/resources/issue-754-benchmark-cells.json \
  --run "build/reports/benchmarks/issue-754/$RUN_1/jmh.json" \
  --run "build/reports/benchmarks/issue-754/$RUN_2/jmh.json" \
  --expected-producer "$MEASURED_HEAD" \
  --expected-code-tree "$TESTED_CODE_TREE" \
  --output docs/evidence/issue-754/pr5/allocation-gate.json
```

Expected: exactly two complete, distinct, shape-identical runs from one producer/code tree/benchmark jar and normalized runtime are evaluated together. A claimed cell must pass both runs; disagreement makes it neutral and controls remain non-improving.

- [ ] **Step 5: Copy checked evidence and record checksums**

Record exact commit, JDK/OS/arch, heap/GC/JVM flags, payloads, protocol, commands, raw paths, and SHA-256. Direct-buffer claims are limited to Java-heap per-call allocation under this protocol.

### Task 16: Finish Documentation, Final Hold Proof, And PR 5

**Complexity:** High
**Depends on:** Task 15
**Pattern skills:** `bluetape-writer`, `bluetape-maintenance`, `verification-before-completion`

**Files:**
- Modify: `io/io/README.md`, `io/io/README.ko.md`
- Modify: `io/json/README.md`, `io/json/README.ko.md`
- Modify: `io/jackson2/README.md`, `io/jackson2/README.ko.md`
- Modify: `io/jackson3/README.md`, `io/jackson3/README.ko.md`
- Modify: `io/fastjson2/README.md`, `io/fastjson2/README.ko.md`
- Modify: `io/avro/README.md`, `io/avro/README.ko.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/manual/manifest.yaml`
- Create: `docs/manual/en/modules/serializer-bytebuffer-benchmark.md`
- Create: `docs/manual/ko/modules/serializer-bytebuffer-benchmark.md`
- Modify: `.github/release-holds/1.12.0-issue-754.json`
- Use: `scripts/check-issue-754-docs.py` and its Task 14 fixture tests
- Use: `scripts/check-issue-754-release-candidate.py` and its Task 14 fixture tests
- Use: `.github/workflows/validate-issue-754-release-candidate.yml` created in Task 14
- Create: `docs/evidence/issue-754/final/compatibility-report.json`
- Create: `docs/evidence/issue-754/final/release-hold-decision.json`
- Create: `docs/evidence/issue-754/final/SHA256SUMS`
- Create: `docs/lessons/2026-07-16-issue-754-bytebuffer-serializer-stack.md`

- [ ] **Step 1: Add bilingual capability tables and exact examples**

Every English/Korean README pair must contain Kotlin and Java examples for zero-origin `clear/serializeTo/flip`, non-zero framing/slices, one-byte-short overflow followed by retry into a larger buffer, and inspection rules for a failed target whose position rolls back but content is unusable. Cover every Binary/JSON/Avro argument shape, including Avro list methods. State allocating fallback versus supported native paths, caller ownership, thread confinement, and trusted/caller-bounded input; do not claim zero-copy for paths with chunks/arrays. Capability tables must state that uncapped paths are unsupported for adversarial input without caller framing/resource budgets and backend controls; filters, registration, and polymorphism are type/security controls, not resource controls; fallback input allocates exactly `source.remaining()` heap bytes before backend controls; and a buffer limit does not bound decompression, depth, references, object count, native memory, or CPU.

- [ ] **Step 2: Update release notes and explicit deferrals**

The migration section must state that `serializeAsByteBuffer()` still allocates, the Binary Kotlin `deserialize(ByteBuffer)` extension remains and is not deprecated, and Java input methods are named `deserializeFrom`/`deserializeListFrom`. Release notes must cover sizing/retry, position-only rollback, unspecified partial writes, read-only failure precedence, native/fallback behavior, compatibility evidence, evidence paths, and the release hold. Their security/resource section repeats the exact adversarial-input, control-category, pre-control fallback allocation, and non-bounded-resource statements from Step 1. Explicitly defer compression, Redis, Protobuf, and Kafka integration to #755-#758.

- [ ] **Step 3: Validate README locale parity and source claims**

Use `scripts/check-issue-754-docs.py` to compare capability table keys and Kotlin/Java examples between each English/Korean pair, verify every public name against source, and fail when any required zero-origin/framing/retry/failed-target/argument-shape/migration/release-note section is absent. `test_check_issue_754_docs.py` also fails if any relevant README pair, `CHANGELOG.md`, or public KDoc omits the exact adversarial-input, control-category, pre-control fallback allocation, or non-bounded-resource claim. Register the benchmark manual entry in `docs/manual/manifest.yaml`, regenerate English/Korean manual pages, and run the existing manual validators. Any diagram or chart added must follow `bluetape-diagram`; otherwise record diagram N/A because this change is API/benchmark-table documentation rather than a topology explanation.

- [ ] **Step 4: Run the complete repository proof serially**

```bash
repo-test-summary -- ./gradlew :bluetape4k-io:test :bluetape4k-json:test :bluetape4k-jackson2:test :bluetape4k-jackson3:test :bluetape4k-fastjson2:test :bluetape4k-avro:test :serializer-bytebuffer-benchmark:test --no-configuration-cache
repo-test-summary -- ./gradlew build --no-configuration-cache
./gradlew exportManualModuleInventory --no-configuration-cache
python3 -m unittest scripts/tests/test_check_issue_754_allocation.py scripts/tests/test_check_issue_754_docs.py scripts/tests/test_check_issue_754_release_candidate.py scripts/test_check_release_holds.py scripts/test_issue_754_github_settings.py -v
python3 scripts/check-issue-754-allocation.py aggregate-runs --manifest benchmark/serializer-bytebuffer-benchmark/src/test/resources/issue-754-benchmark-cells.json --run docs/evidence/issue-754/pr5/run-1/jmh.json --run docs/evidence/issue-754/pr5/run-2/jmh.json --expected-code-tree "$(python3 scripts/check-issue-754-allocation.py print-code-tree --repository .)" --expected-benchmark-jar "$(python3 scripts/check-issue-754-allocation.py print-benchmark-jar-sha --repository .)" --output .codex/issue-754/allocation-gate-recheck.json
cmp .codex/issue-754/allocation-gate-recheck.json docs/evidence/issue-754/pr5/allocation-gate.json
shasum -a 256 -c docs/evidence/issue-754/pr5/SHA256SUMS
shasum -a 256 -c docs/evidence/issue-754/final/SHA256SUMS
ruby scripts/manual/generate_manuals_test.rb
ruby scripts/manual/validate_manuals_test.rb
ruby scripts/manual/export_manifest_test.rb
ruby scripts/manual/release_contract_test.rb
bash scripts/check-serializer-buffer-abi.sh --build-current --expected-head "$(git rev-parse HEAD)"
actionlint .github/workflows/validate-issue-754-release-candidate.yml
git diff --check origin/develop...HEAD
```

Expected: all affected tests and full build pass; no compression/integration files changed; evidence checksums validate; both runs share one producer commit that is an ancestor of exact PR 5 HEAD, and the exact current code tree plus benchmark jar match both measured runs. Any covered source/build/workflow drift invalidates both UUIDs and returns to Task 15 for two fresh runs before proceeding.

- [ ] **Step 5: Converge six-lens code review and commit the final PR head**

Run performance, stability, security, ops, developer/API, and caller review against the exact full stack. Repair P0/P1, rerun affected proof, then commit all docs, evidence, hold authority, and the durable lesson. The worktree must be clean and this commit becomes `FINAL_HEAD`; any later commit invalidates the following gate and requires it again.

- [ ] **Step 6: Verify the post-review committed exact head**

```bash
FINAL_HEAD="$(git rev-parse HEAD)"
test -z "$(git status --porcelain)"
python3 scripts/check-issue-754-allocation.py verify-final-head \
  --final-head "$FINAL_HEAD" \
  --manifest benchmark/serializer-bytebuffer-benchmark/src/test/resources/issue-754-benchmark-cells.json \
  --run docs/evidence/issue-754/pr5/run-1/jmh.json \
  --run docs/evidence/issue-754/pr5/run-2/jmh.json \
  --allocation-gate docs/evidence/issue-754/pr5/allocation-gate.json \
  --checksums docs/evidence/issue-754/pr5/SHA256SUMS \
  --checksums docs/evidence/issue-754/final/SHA256SUMS
test "$(git rev-parse HEAD)" = "$FINAL_HEAD"
```

Expected: the measured producer is an ancestor of `FINAL_HEAD`; code-tree and benchmark-jar hashes recomputed from `FINAL_HEAD` match both runs; two-run aggregation reproduces the committed gate; all full-head checksums pass. Any subsequent commit makes this evidence stale until rerun.

- [ ] **Step 7: Push/create PR 5 and prove exact-head CI**

Target `develop`, head `feat/issue-754-allocation-proof`, use `Closes #754`, and require all four predecessors merged. Required CI job `issue-754-final-head` checks out the reported PR head and runs `verify-final-head --final-head "$(git rev-parse HEAD)"`; aggregate `ci-status` cannot pass without it. After CI/review are green on that exact head, record PR/head/tree and report merge-ready; stop for fresh merge approval.

- [ ] **Step 8: After approved merge, bind evidence to the actual release candidate**

Verify PR 5 head/merge tree equality. Dispatch validation-only workflow `validate-issue-754-release-candidate.yml` with exact candidate SHA and a UUID request ID. It has read-only contents/actions permissions, no protected environment, no secrets, run-name `issue-754-candidate-$REQUEST_ID`, check/job name `issue-754-release-candidate`, and retained artifact `issue-754-release-candidate-$REQUEST_ID`. The artifact contains all five PR/head/merge/tree rows, committed evidence checksums, recomputed `testedCodeTreeSha256`, candidate SHA, request/run IDs, and PASS/HOLD decision without amending the tested commit.

```bash
CANDIDATE="$(git rev-parse origin/develop)"
CANDIDATE_REQUEST_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
BEFORE="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
gh workflow run validate-issue-754-release-candidate.yml --ref develop -f candidate_sha="$CANDIDATE" -f request_id="$CANDIDATE_REQUEST_ID"
CANDIDATE_VALIDATION_RUN_ID="$(gh run list --workflow validate-issue-754-release-candidate.yml --event workflow_dispatch --limit 20 --json databaseId,createdAt,displayTitle,headSha --jq '.[] | select(.createdAt >= "'"$BEFORE"'" and .headSha == "'"$CANDIDATE"'" and .displayTitle == "issue-754-candidate-'"$CANDIDATE_REQUEST_ID"'") | .databaseId' | head -1)"
test -n "$CANDIDATE_VALIDATION_RUN_ID"
gh run watch "$CANDIDATE_VALIDATION_RUN_ID" --exit-status
gh run view "$CANDIDATE_VALIDATION_RUN_ID" --json name,event,headSha,jobs
gh run download "$CANDIDATE_VALIDATION_RUN_ID" --name "issue-754-release-candidate-$CANDIDATE_REQUEST_ID" --dir ".codex/issue-754/candidate-$CANDIDATE_REQUEST_ID"
python3 scripts/check-issue-754-release-candidate.py verify-artifact --candidate "$CANDIDATE" --request-id "$CANDIDATE_REQUEST_ID" --run-id "$CANDIDATE_VALIDATION_RUN_ID" --artifact ".codex/issue-754/candidate-$CANDIDATE_REQUEST_ID"
```

Expected: exact request-specific display title, candidate/head/check name, selected run ID versus artifact run ID, and checksums match, and decision is PASS. HOLD or any identity mismatch exits nonzero. If `develop` advances, discard the stale result and repeat the full five-row validation on the new SHA. This named check clears the issue hold only; it does not authorize snapshot, Nightly, tag, or publish side effects.

### Task 17: Close The 1.12.0 Tag Into An Immutable State

**Complexity:** High and external-production
**Depends on:** Task 16 exact-candidate PASS, a fresh `bluetape-publish-jvm` checklist, and separate snapshot/Nightly/publish/tag authorization
**Pattern skills:** `bluetape-publish-jvm`, `verification-before-completion`

**Files:**
- Execute: `.github/workflows/release.yml` implemented and statically proved by Task 4
- Execute: `scripts/issue-754-github-settings.py` immutable-closeout/verify commands
- Modify external GitHub ruleset `release-tags-1.12.0` only under explicit authorization
- Create retained release-closeout artifact `issue-754-tag-immutability-$REQUEST_ID`

- [ ] **Step 1: Refresh the publish checklist and stop for release authorization**

Pin target `1.12.0`, latest external version, exact candidate SHA, target authority, consumer scope, `CANDIDATE_VALIDATION_RUN_ID`/`CANDIDATE_REQUEST_ID` and their artifact, credential-environment state, and dispatch hold. Snapshot, Nightly, Maven Central publish, tag creation, and ruleset transition remain separate irreversible gates.

- [ ] **Step 2: Dispatch prepare to create the tag and execute the immutable-transition state machine**

After separate tag/prepare approval, dispatch only `phase=prepare`. First, environment-free job `candidate-sha-guard` asserts `github.sha == inputs.candidate_sha`, checks out that exact SHA, and exposes the verified SHA to dependent jobs; a moved `develop` ref fails before artifact access or mutation. Before tag creation, the hold job downloads only `CANDIDATE_VALIDATION_RUN_ID`'s named artifact and verifies repository, workflow/ref, exact candidate SHA, candidate request/run identities, check conclusion, checksums, uniqueness, and PASS. Named job/check `issue-754-tag-immutability` then invokes `immutable-closeout`, uploads `issue-754-tag-immutability-$RELEASE_REQUEST_ID`, and exits zero only for exact candidate/tag plus verified no-bypass state and twin-probe PASS. The phase has no Maven/GitHub publication jobs.

```bash
RELEASE_REQUEST_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
BEFORE="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
gh workflow run release.yml --ref develop -f version=1.12.0 -f phase=prepare -f candidate_sha="$CANDIDATE" -f candidate_validation_run_id="$CANDIDATE_VALIDATION_RUN_ID" -f candidate_validation_request_id="$CANDIDATE_REQUEST_ID" -f request_id="$RELEASE_REQUEST_ID"
PREPARE_RUN_ID="$(gh run list --workflow release.yml --event workflow_dispatch --limit 20 --json databaseId,createdAt,displayTitle,headSha --jq '.[] | select(.createdAt >= "'"$BEFORE"'" and .headSha == "'"$CANDIDATE"'" and .displayTitle == "issue-754-release-prepare-'"$RELEASE_REQUEST_ID"'") | .databaseId' | head -1)"
test -n "$PREPARE_RUN_ID"
gh run watch "$PREPARE_RUN_ID" --exit-status
gh run download "$PREPARE_RUN_ID" --name "issue-754-tag-immutability-$RELEASE_REQUEST_ID" --dir ".codex/issue-754/tag-immutability-$RELEASE_REQUEST_ID"
python3 scripts/issue-754-github-settings.py verify-immutable-artifact --repository "$OWNER/$REPO" --workflow release.yml --ref develop --prepare-run-id "$PREPARE_RUN_ID" --request-id "$RELEASE_REQUEST_ID" --candidate-validation-run-id "$CANDIDATE_VALIDATION_RUN_ID" --candidate-validation-request-id "$CANDIDATE_REQUEST_ID" --tag 1.12.0 --candidate "$CANDIDATE" --artifact ".codex/issue-754/tag-immutability-$RELEASE_REQUEST_ID"
```

Inside the prepare job, the sole mutating command is `immutable-closeout`; the operator does not repeat it locally:

```bash
python3 scripts/issue-754-github-settings.py immutable-closeout --repository "$GITHUB_REPOSITORY" --tag 1.12.0 --candidate "$CANDIDATE" --candidate-validation-run-id "$CANDIDATE_VALIDATION_RUN_ID" --candidate-validation-request-id "$CANDIDATE_REQUEST_ID" --ruleset release-tags-1.12.0 --request-id "$RELEASE_REQUEST_ID" --run-id "$GITHUB_RUN_ID" --output "$RUNNER_TEMP/issue-754-tag-immutability-$RELEASE_REQUEST_ID.json"
```

Use the settings App token exclusively to snapshot/update/read back the production ruleset ID/body/hash, and the tag App token exclusively to create/inspect/delete allowed tag refs. Before artifact publication, create a request-bound annotated `1.12.0` tag object through the tag App at the validated candidate, create the production ref at that exact object SHA, and verify both object identity and target; submit and read back bypass removal only through the settings App. The immutable artifact and rollback journal bind both distinct App/installation actor IDs, token permission summaries, and the actor used for each request; actor confusion fails before mutation.

The transition has only these outcomes: `bypass-retained` permits App deletion of the unconsumed correctly targeted tag plus verified pre-state restoration; `no-bypass-applied` is immutable and may proceed only after all checks pass; `ambiguous`, stale-ID/body, wrong-target, or partial state is publication-blocked recovery. In any applied or ambiguous no-bypass state, production deletion/update is forbidden; retain the tag, keep publication blocked, and require a fresh settings-authority recovery plan. Never assume the App remains authorized after an API error, and never weaken a verified production no-bypass rule. A rerun after lost artifact upload may recover only when the no-bypass ruleset, annotated object message, request ID, tag name, and candidate all match exactly; all other pre-existing tags remain blocked.

- [ ] **Step 3: Prove immutable semantics on an isolated twin probe**

Create a temporary probe tag/ruleset with the same no-bypass rule body before switching it immutable. After transition, assert both ordinary and App update/delete attempts are denied, compare the normalized rule body with production, then remove only the temporary ruleset and probe tag under the documented cleanup authority. Record requests, actor IDs, exit codes, policy hashes, production tag target, cleanup, and check conclusion in the retained closeout artifact. No destructive request is sent to production `1.12.0`.

- [ ] **Step 4: Stop for publish approval, then dispatch verify-only publication**

Report prepare run/check/artifact IDs, exact tag target, normalized production ruleset hash, and twin-probe cleanup, then stop for fresh Maven/GitHub publication approval. The artifact schema binds repository, workflow/ref, prepare run ID, release and candidate request/run IDs, candidate SHA, tag, normalized ruleset hash, and PASS decision. On approval, dispatch `phase=publish` with the exact `prepare_run_id`; `issue-754-tag-immutability` independently downloads both the original candidate-validation artifact from `CANDIDATE_VALIDATION_RUN_ID` and the prepare artifact from `PREPARE_RUN_ID`, rejects missing/wrong/expired/duplicate artifacts, verifies each artifact, cross-checks all shared identities, verifies live no-bypass state, and runs before any credential-bearing job. Every publication job has `needs: [release-hold-1.12.0-issue-754, issue-754-tag-immutability]`; any mismatch/HOLD exits nonzero and skips publication.

```bash
gh workflow run release.yml --ref develop -f version=1.12.0 -f phase=publish -f candidate_sha="$CANDIDATE" -f candidate_validation_run_id="$CANDIDATE_VALIDATION_RUN_ID" -f candidate_validation_request_id="$CANDIDATE_REQUEST_ID" -f request_id="$RELEASE_REQUEST_ID" -f prepare_run_id="$PREPARE_RUN_ID"
```

Inside the publish check job, retrieve and verify the cross-run artifact before live verification:

```bash
GH_TOKEN="${GH_TOKEN:?github.token is required}" gh run download "$CANDIDATE_VALIDATION_RUN_ID" --name "issue-754-release-candidate-$CANDIDATE_REQUEST_ID" --dir "$RUNNER_TEMP/issue-754-release-candidate-$CANDIDATE_REQUEST_ID"
python3 scripts/check-issue-754-release-candidate.py verify-artifact --candidate "$CANDIDATE" --request-id "$CANDIDATE_REQUEST_ID" --run-id "$CANDIDATE_VALIDATION_RUN_ID" --artifact "$RUNNER_TEMP/issue-754-release-candidate-$CANDIDATE_REQUEST_ID"
GH_TOKEN="${GH_TOKEN:?github.token is required}" gh run download "$PREPARE_RUN_ID" --name "issue-754-tag-immutability-$RELEASE_REQUEST_ID" --dir "$RUNNER_TEMP/issue-754-tag-immutability-$RELEASE_REQUEST_ID"
python3 scripts/issue-754-github-settings.py verify-immutable-closeout --repository "$GITHUB_REPOSITORY" --workflow release.yml --ref develop --prepare-run-id "$PREPARE_RUN_ID" --candidate-validation-run-id "$CANDIDATE_VALIDATION_RUN_ID" --candidate-validation-request-id "$CANDIDATE_REQUEST_ID" --tag 1.12.0 --candidate "$CANDIDATE" --request-id "$RELEASE_REQUEST_ID" --artifact "$RUNNER_TEMP/issue-754-tag-immutability-$RELEASE_REQUEST_ID"
```

The download step environment is exactly `GH_TOKEN: ${{ github.token }}` and the job-level permissions are exactly `{ actions: read, contents: read }`.

Expected: prepare and publish each expose check `issue-754-tag-immutability`; static workflow tests and `actionlint` reject missing dependencies or a publish job enabled during prepare. Any failure leaves publication blocked and requires a fresh recovery decision; successful closeout does not authorize later tag replacement or deletion.

If Maven Central accepts the deployment but the publish command loses its
response, do not retry the immutable version blindly. Verify the external
deployment first and complete the GitHub Release only as a separately approved
recovery action after authoritative readback.

### Task 18: Recover A Failed Merged Stack Slice

**Complexity:** High and conditional
**Depends on:** a verified post-merge regression and fresh incident/revert authority
**Pattern skills:** `bluetape-bugfix`, `verification-before-completion`

- [ ] **Step 1: Force and prove HOLD before code recovery**

Set the issue decision to HOLD through the existing fail-closed authority, verify snapshot/release/tag jobs cannot pass, and record the failed slice plus all descendant PR/head/merge/tree IDs. Do not mutate an immutable production tag; if release already occurred, use a separate incident/release procedure.

- [ ] **Step 2: Freeze descendants and prepare ordered reverts**

Stop dispatches, mark open descendants blocked, and close them only with the matching explicit authority. Prepare revert PRs newest descendant first through the failed slice so dependency order remains valid; each revert is independently reviewed, CI-proved, and freshly approved for merge.

- [ ] **Step 3: Invalidate and regenerate evidence**

Mark affected ABI/security/allocation/candidate artifacts invalid with exact old heads, regenerate manifests/checksums/release notes on the revert head, and discard benchmark runs whose covered code tree changed. Never reuse a prior PASS under a new head.

- [ ] **Step 4: Require full inherited proof before resumption**

Run every inherited gate through the reverted slice, six-lens review, exact-head CI, and candidate HOLD validation. Resume a repaired descendant only from the verified revert head with a new branch/evidence identity and the normal fresh merge approvals.

## Per-PR Delivery Checklist

Apply this sequence independently to PRs 1-5:

1. Confirm predecessor merge SHA/tree and branch base.
2. Run the slice RED/GREEN evidence and every inherited test row.
3. Run Kotlin diagnostics/import/deprecation checks for touched `.kt` files.
4. Run `git diff --check`, module hazards, docs parity, ABI/security/performance checks triggered by the slice.
5. Run six independent review lenses plus main integration; reach P0=0/P1=0.
6. Commit only intended files with Lore trailers and record exact local head.
7. Push the exact authorized head, create/update the PR, verify assignment/milestone/labels/body/DoD.
8. Wait for required CI and live review on that exact head.
9. Report merge-ready with exact PR/head and request fresh merge approval.
10. After approval, squash merge, verify live state and head-tree/merge-tree equality, sync `develop`, then rebase/retarget and completely revalidate the next descendant.

## Six-Perspective Plan Review Record

Final independent reviews were rerun after integrating every P0/P1/P2 repair. The accepted plan state is:

| Lens | Final result | Principal repairs verified |
|---|---|---|
| Performance | P0=0, P1=0, P2=0 | complete cell manifest, two-run aggregate gate, frozen producer/code-tree/jar/runtime identity, post-review `verify-final-head`, exact-head CI |
| Stability/reliability | P0=0, P1=0, P2=0 | duplicate-backed commit, fatal/dual-failure matrix, bounded concurrency harness, hostile Avro child budgets, fresh ABI jars, drift-safe recovery |
| Security | P0=0, P1=0, P2=0 | filter/registration/polymorphism parity, exact trust/resource warnings, credential isolation, distinct tag/settings Apps, no-bypass transition |
| Ops/release | P0=0, P1=0, P2=0 | protected environment credentials, old-ref/rerun denial, prepare/publish split, exact cross-run artifact binding, immutable job dependencies |
| Developer/API | P0=0, P1=0, P2=0 | legacy implementation ABI fixtures, executable Java defaults, official backend APIs, PR-specific CI routing, validator TDD |
| Caller/user | P0=0, P1=0, P2=0 | null precedence, Kotlin/Java examples, migration names, retry/failure behavior, exact KDoc/resource claims and docs validation |

Main integration also verified slice ownership, stack invalidation/revert order, external side-effect gates, and the no-implementation-before-plan-approval stop condition. No review lane was used to authorize implementation, push, PR creation, GitHub setting mutation, workflow dispatch, tag, publish, merge, or cleanup.

## Plan DoD

- [x] Every design acceptance criterion maps to an ordered task and command.
- [x] Every task names exact files, ownership, pattern skill, TDD behavior, and expected evidence.
- [x] Success, failure, edge, lifecycle, concurrency, security, backend-capability, and allocation cases are assigned.
- [x] Public KDoc, bilingual README, CHANGELOG, evidence, lesson, workflow, and module-registration impacts are assigned.
- [x] Rollback/rerun points cover ABI, stack rebases, backend fallback, benchmark disagreement, release hold, and external GitHub settings.
- [x] Six independent plan-review lenses plus integration report P0=0/P1=0.
- [x] The approved design and reviewed plan are committed before Task 1 implementation begins.
