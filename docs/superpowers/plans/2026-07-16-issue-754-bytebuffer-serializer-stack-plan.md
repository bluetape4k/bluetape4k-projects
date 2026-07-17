# Issue #754 ByteBuffer Serializer Stack Implementation Plan

**Goal:** Preserve existing serializer contracts while adding caller-owned
`ByteBuffer` APIs, backend-specific lower-copy paths, and allocation evidence.

**Authority:** The live issue [#754](https://github.com/bluetape4k/bluetape4k-projects/issues/754)
defines scope. This plan does not authorize release, tag, publish, credential,
GitHub App, ruleset, environment, or GitHub Release changes.

**Delivery:** Five sequential PRs. PR creation is authorized after each slice
passes its pre-PR gates. Every merge waits for fresh approval tied to the exact
PR head after CI and review pass.

## Stop Conditions

- Do not change existing `ByteArray` signatures or wire/security behavior.
- Do not claim lower allocation for a compatibility fallback.
- If a predecessor changes, rebase descendants and rerun inherited proof.
- Keep #755-#758 integration work outside this stack.
- Stop any external release or repository-setting side effect; it requires a
  separate workflow and explicit authority.

## Stack

| Slice | Branch | Scope | Status |
|---|---|---|---|
| 1 | `feat/issue-754-buffer-contract` | API defaults, fixed-buffer contract, ABI fixtures | Merged via PR #1031 |
| corrective | `fix/issue-754-remove-release-scope` | Remove unauthorized release/settings coupling | Merged via PR #1034 |
| 2 | `feat/issue-754-core-serializers` | JDK, Kryo, Fory lower-copy paths | Merged via PR #1040 |
| 3 | `feat/issue-754-json-serializers` | Jackson 2/3 and Fastjson2 | Current; base `7459b84cb976a349e1bbc03fefb36d4ca50d02ee` |
| 4 | `feat/issue-754-avro-serializers` | Reflect, generic, specific/list | Pending |
| 5 | `feat/issue-754-allocation-proof` | Benchmarks, allocation evidence, docs | Pending |

## Slice 1: Contract And Compatibility

### Completed outcomes to preserve

- `BinarySerializer`, `JsonSerializer`, and Avro interfaces expose executable
  JVM default buffer methods while retaining old abstract methods.
- Java input names avoid null-literal ambiguity; Kotlin extensions remain.
- Fixed target behavior covers heap/direct/sliced/read-only buffers, non-zero
  position, reduced limit, overflow, and rollback.
- Pinned pre-change jars, legacy callers, `javap`, and frozen wire fixtures prove
  compatibility.
- The ABI script and contract evidence are serializer-owned and independent of
  release policy.

### Corrective verification

```bash
python3 -m unittest scripts/test_release_workflow_policy.py -v
actionlint .github/workflows/ci.yml .github/workflows/release.yml \
  .github/workflows/publish-snapshot.yml
bash scripts/check-serializer-buffer-abi.sh \
  --build-current --expected-head "$(git rev-parse HEAD)"
```

Expected: policy tests and actionlint pass; ABI check reports all legacy/new
caller and default-dispatch checks PASS. Corrective changes must not modify
serializer Kotlin/Java source or fixtures.

## Slice 2: Core Binary Serializers

### Task 1: JDK optimized path

1. Add RED tests for direct/heap/sliced input and fixed output.
2. Route object streams through caller-owned ByteBuffer stream adapters.
3. Preserve `ObjectInputFilter`, exception wrapping, position, and cleanup.
4. Run targeted JDK serializer tests and inherited contract/ABI proof.

### Task 2: Kryo optimized path

1. Add RED tests for exact-capacity output, overflow, direct input, pool release,
   registration, and reference configuration.
2. Use Kryo ByteBuffer input/output adapters only within the current target
   range.
3. Prove failed calls release pooled state and do not poison the next call.
4. Run Kryo tests plus inherited contract/ABI proof.

### Task 3: Fory capability decision

1. Add direct-input tests and fallback-output tests.
2. Optimize input only where the resolved Fory API preserves caller state and
   configured registration/security behavior.
3. Keep output on the compatibility default if the backend can grow or detach
   storage.
4. Document optimized and fallback cells without allocation claims yet.

### Slice 2 gate

```bash
./gradlew :bluetape4k-io:test --no-configuration-cache
bash scripts/check-serializer-buffer-abi.sh \
  --build-current --expected-head "$(git rev-parse HEAD)"
```

Review correctness, API compatibility, security, resource lifecycle, and
performance claim boundaries. Repair P0/P1 before PR creation.

## Slice 3: JSON Serializers

### Task 1: Jackson 2 RED contract

1. Add `JacksonSerializerByteBufferTest` under
   `io/jackson2/src/test/kotlin/io/bluetape4k/jackson/`.
2. Cover heap/direct/sliced/read-only input, non-zero position, reduced limit,
   order/mark preservation, empty and malformed input, exact-capacity output,
   read-only target prevalidation, raw overflow, rollback, fatal-error identity,
   failed-call reuse, mapper configuration, old/new cross-reading, and
   `serializeTo(null)` returning `0` without changing target position or invoking
   the mapper.
3. Prove `serializeTo` and `deserializeFrom` bypass the allocating `ByteArray`
   methods with throwing/counting overrides.
4. Prove concrete reified `List<User>` and `Map<String, User>` input retain
   Jackson `TypeReference` semantics. Also prove a receiver statically typed as
   `JsonSerializer` retains the existing raw class-based collection behavior and
   that an invalid target class keeps the established failure wrapper.
5. Exercise allowed and denied root/nested class identifiers through a typed
   mapper and preserve the existing allowlist rejection wrapper/cause.
6. Run the focused test and observe the expected RED failures before production
   edits.

   ```bash
   ./gradlew :bluetape4k-jackson2:test \
     --tests "io.bluetape4k.jackson.JacksonSerializerByteBufferTest" \
     --no-configuration-cache
   ```

### Task 2: Jackson 2 implementation and inherited formats

1. Override buffer methods in
   `io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt` using
   `ByteBufferOutputStream.fixed(target.duplicate())` and
   `ByteBufferInputStream(source.duplicate())` with mapper stream APIs.
2. Add the concrete reified ByteBuffer API as a top-level concrete-receiver extension using
   `jacksonTypeRef<T>()`; do not add a final JVM method to the open serializer class or a new public `Type`
   overload to `JsonSerializer`.
3. Return `0` before mapper work when the graph is null. Classify output
   failures as raw read-only/overflow, identical fatal `Error`,
   or the established `JsonSerializationException`; commit target position only
   after successful completion.
4. Add old/new cross-reading buffer coverage for YAML, Properties, CSV, TOML,
   CBOR, Ion, and Smile in
   `io/jackson2/src/test/kotlin/io/bluetape4k/jackson/JacksonFormatSerializerByteBufferTest.kt`
   because they inherit the base override.
5. Run focused ByteBuffer and inherited-format tests, then the full module test.

   ```bash
   ./gradlew :bluetape4k-jackson2:test \
     --tests "io.bluetape4k.jackson.JacksonSerializerByteBufferTest" \
     --tests "io.bluetape4k.jackson.JacksonFormatSerializerByteBufferTest" \
     --no-configuration-cache
   ./gradlew :bluetape4k-jackson2:test --no-configuration-cache
   ```

### Task 3: Jackson 3 RED and implementation

1. Mirror Tasks 1-2 in `io/jackson3`, using Jackson 3 mapper/type-reference APIs.
2. Add a negative ByteBuffer test proving unsolicited class-id properties do not
   introduce default typing or instantiate arbitrary subtypes.
3. Preserve annotation-driven polymorphism, mapper factories, failure policy,
   caller state, reuse, and all inherited format wire semantics.
4. Put inherited-format coverage in
   `io/jackson3/src/test/kotlin/io/bluetape4k/jackson3/JacksonFormatSerializerByteBufferTest.kt`.
5. Run focused ByteBuffer and inherited-format tests, then the full module test.

   ```bash
   ./gradlew :bluetape4k-jackson3:test \
     --tests "io.bluetape4k.jackson3.JacksonSerializerByteBufferTest" \
     --tests "io.bluetape4k.jackson3.JacksonFormatSerializerByteBufferTest" \
     --no-configuration-cache
   ./gradlew :bluetape4k-jackson3:test --no-configuration-cache
   ```

### Task 4: Fastjson2 capability decision and bounded optimization

1. Extract the resolved Fastjson2 `2.0.62` sources into the required inspection
   directory and inspect `JSONB.toBytes`, the `JSONReaderJSONB(InputStream)`
   constructor, and the `byte[]` offset/length `parseObject` overloads:

   ```bash
   set -euo pipefail
   sources_jar="$(find "$HOME/.gradle/caches/modules-2/files-2.1/com.alibaba.fastjson2/fastjson2/2.0.62" \
     -name 'fastjson2-2.0.62-sources.jar' -print -quit)"
   test -n "$sources_jar"
   rm -rf .codex/lib-sources/fastjson2-2.0.62
   mkdir -p .codex/lib-sources/fastjson2-2.0.62
   unzip -q -o "$sources_jar" -d .codex/lib-sources/fastjson2-2.0.62
   shasum -a 256 "$sources_jar"
   rg -n "toBytes\(Object|JSONReaderJSONB\(Context ctx, InputStream|parseObject\(" \
     .codex/lib-sources/fastjson2-2.0.62/com/alibaba/fastjson2/JSONB.java \
     .codex/lib-sources/fastjson2-2.0.62/com/alibaba/fastjson2/JSONReaderJSONB.java
   ```

   Record that JSONB output and stream input allocate or grow internal arrays,
   while the `byte[]` offset/length parser aliases an existing heap array. Store
   the resolved sources-JAR SHA-256, exact matched source locations, conclusions,
   and optimized/fallback matrix in
   `docs/evidence/issue-754/json/fastjson2-2.0.62-capability.md`. Remove the
   extracted inspection directory before preparing the PR; the evidence document
   is the durable review artifact.
2. Add `FastjsonSerializerByteBufferTest` covering JSONB old/new cross-reading,
   heap/direct/sliced/read-only input, caller state, concrete reified
   `List<User>` and `Map<String, User>`, interface-typed raw collections, invalid
   target classes, malformed/fatal failures, retry, output overflow/rollback,
   `serializeTo(null)` returning `0` without changing target position, and a
   negative type-metadata/AutoType case.
3. Override class-token `deserializeFrom` and add the concrete reified
   `deserialize(ByteBuffer)` overload. For writable array-backed input, pass the
   backing array, `arrayOffset() + position()`, remaining length, and respectively
   `clazz` or `reference<T>().type` to JSONB. For direct/read-only input, copy the
   duplicate's remaining range and call the same class/type-token parser.
4. Override `serializeTo` as an explicitly allocating compatibility path using
   `JSONB.toBytes`, capacity validation, a duplicate target, and commit-on-success;
   return `0` before JSONB work for null input and do not describe it as
   lower-copy.
5. In class-token, reified, and output buffer paths, catch in this order: rethrow
   the identical fatal `Error`; expose raw `ReadOnlyBufferException` and
   `BufferOverflowException`; wrap other failures in the existing
   `JsonSerializationException` message/cause contract; restore output position
   on every failure. Preserve the feature-free reader context and do not enable
   AutoType.
6. Run the focused test, then the full Fastjson2 module test.

   ```bash
   ./gradlew :bluetape4k-fastjson2:test \
     --tests "io.bluetape4k.fastjson2.FastjsonSerializerByteBufferTest" \
     --no-configuration-cache
   ./gradlew :bluetape4k-fastjson2:test --no-configuration-cache
   ```

### Task 5: Slice documentation and inherited proof

1. Update the class and buffer-overload KDoc in
   `io/jackson2/src/main/kotlin/io/bluetape4k/jackson/JacksonSerializer.kt`,
   `io/jackson3/src/main/kotlin/io/bluetape4k/jackson3/JacksonSerializer.kt`, and
   `io/fastjson2/src/main/kotlin/io/bluetape4k/fastjson2/FastjsonSerializer.kt`.
2. Update `io/jackson2/README.md`, `io/jackson2/README.ko.md`,
   `io/jackson3/README.md`, `io/jackson3/README.ko.md`,
   `io/fastjson2/README.md`, and `io/fastjson2/README.ko.md` with buffer state
   rules, optimized/fallback cells, the allocation-claim exclusion, a Kotlin
   concrete `deserialize<List<User>>(buffer)` example, a Java
   `deserializeFrom(buffer, User.class)` example, and the interface receiver's
   raw-generic limitation.
3. Run `git diff --check` and verify every language variant contains the
   required contract and examples with:

   ```bash
   set -euo pipefail
   for readme in \
     io/jackson2/README.md io/jackson2/README.ko.md \
     io/jackson3/README.md io/jackson3/README.ko.md \
     io/fastjson2/README.md io/fastjson2/README.ko.md
   do
     rg -q "ByteBuffer" "$readme"
     rg -q "optimized|optimization|최적화" "$readme"
     rg -q "fallback|compatibility|호환" "$readme"
     rg -q "allocation|할당" "$readme"
     rg -q "JsonSerializer" "$readme"
     rg -q 'deserialize<List<User>>\(buffer\)' "$readme"
     rg -q 'deserialize<Map<String, User>>\(buffer\)' "$readme"
     rg -q 'deserializeFrom\(buffer, User\.class\)' "$readme"
   done
   ```

4. Run all three module tests and the inherited ABI script.

### Slice 3 gate

```bash
./gradlew \
  :bluetape4k-jackson2:test \
  :bluetape4k-jackson3:test \
  :bluetape4k-fastjson2:test \
  --no-configuration-cache
bash scripts/check-serializer-buffer-abi.sh \
  --build-current --expected-head "$(git rev-parse HEAD)"
```

Verify mapper/security configuration and old/new cross-reading before PR.

## Slice 4: Avro Serializers

### Task 1: Reflect and generic-record paths

1. Add RED tests for direct/heap input, fixed output, schema handling, codec,
   overflow, close failure, and caller-state preservation.
2. Connect DataFile readers/writers to ByteBuffer streams.
3. Use semantic OCF comparison where container metadata is nondeterministic.

### Task 2: Specific-record and list paths

1. Cover nullable records, empty/non-empty lists, direct input, exact capacity,
   overflow, and schema mismatch.
2. Preserve existing list framing and exception behavior.
3. Rerun legacy caller/default-dispatch ABI proof.

### Slice 4 gate

```bash
./gradlew :bluetape4k-avro:test --no-configuration-cache
bash scripts/check-serializer-buffer-abi.sh \
  --build-current --expected-head "$(git rev-parse HEAD)"
```

Verify schema/codec/wire semantics and cleanup before PR creation.

## Slice 5: Allocation Proof And Documentation

### Task 1: Benchmark protocol

1. Add or extend the serializer ByteBuffer benchmark using the repository's
   `kotlinx-benchmark` pattern.
2. Compare existing ByteArray APIs, default buffer fallbacks, and optimized
   backend paths with equivalent payloads and serializer configuration.
3. Record bytes/op, allocations/op where available, GC counts, environment,
   raw result paths, and two fresh run identifiers.
4. Treat throughput as diagnostic only.

### Task 2: Acceptance

- Repeated runs must agree on direction for any allocation claim.
- Functional, ABI, wire, security, and resource tests must remain green.
- Fallback cells are labeled ergonomic and excluded from improvement claims.
- No direct-buffer recommendation is made without measured benefit and caller
  lifecycle guidance.

### Task 3: Documentation

Update public KDoc, representative README pairs, and `CHANGELOG.md` with:

- output position/limit/overflow/rollback behavior;
- input position preservation and remaining-range behavior;
- Java/Kotlin method names and migration examples;
- optimized versus fallback backend table;
- allocation evidence and limitations;
- explicit deferral of #755-#758.

### Slice 5 gate

Run benchmark validation, all affected module tests, ABI proof, documentation
parity checks, detekt, and the proportional build. Review exact evidence and
claims before PR creation.

## Per-PR Delivery Checklist

- [ ] Branch is based on the merged predecessor.
- [ ] Diff contains only the slice's owned files.
- [ ] RED failure was observed for each new behavior.
- [ ] Targeted tests and inherited ABI/contract proof pass.
- [ ] Security, wire, position, overflow, and resource contracts pass where
  applicable.
- [ ] Documentation and evidence match actual optimized/fallback behavior.
- [ ] P0=0 and P1=0 on the exact head.
- [ ] PR metadata mirrors issue #754 and ends with `## DoD Status`.
- [ ] CI and current review threads are green on the exact head.
- [ ] Merge waits for fresh explicit approval.

## Plan DoD

- [x] Scope matches the live issue and milestone.
- [x] Existing contract implementation and ABI evidence are preserved.
- [x] Core, JSON, Avro, and allocation work is split into reviewable slices.
- [x] Compatibility, security, wire, position, overflow, cleanup, and
  allocation proof are explicit.
- [x] #755-#758 and throughput claims are excluded.
- [x] Release, tag, publish, credentials, GitHub Apps, rulesets, environments,
  and GitHub Release creation are excluded and separately gated.
