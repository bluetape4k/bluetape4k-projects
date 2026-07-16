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
| 1 | `feat/issue-754-buffer-contract` | API defaults, fixed-buffer contract, ABI fixtures | Merged; preserve through corrective PR |
| corrective | `fix/issue-754-remove-release-scope` | Remove unauthorized release/settings coupling | Current |
| 2 | `feat/issue-754-core-serializers` | JDK, Kryo, Fory lower-copy paths | Pending |
| 3 | `feat/issue-754-json-serializers` | Jackson 2/3 and Fastjson2 | Pending |
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
actionlint .github/workflows/release.yml .github/workflows/publish-snapshot.yml
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

### Task 1: Jackson 2 and Jackson 3

1. Add RED tests for fixed output, direct/read-only input, overflow, rollback,
   mapper configuration, polymorphic typing, and wire compatibility.
2. Connect mapper stream APIs to the fixed ByteBuffer adapters.
3. Preserve exception types and caller state on all paths.
4. Run each module's targeted tests and inherited contract proof.

### Task 2: Fastjson2

1. Add JSONB compatibility, direct-input, and fallback-output tests.
2. Use a lower-copy path only if the resolved API demonstrably avoids a full
   internal array.
3. Otherwise retain the default and document it as ergonomic only.

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
