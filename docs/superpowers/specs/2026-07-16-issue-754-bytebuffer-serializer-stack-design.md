# Issue #754 ByteBuffer Serializer Stack Design

- Issue: [#754 Add ByteBuffer-oriented serializer APIs to reduce allocation pressure](https://github.com/bluetape4k/bluetape4k-projects/issues/754)
- Milestone: `1.12.0`
- Baseline authority: `origin/develop@90b267871e9154f242e6de7ee9fd0539f83e509e`
- Delivery: five sequential serializer-focused pull requests
- Performance claim: allocation rate and GC pressure only; no throughput promise

## 1. Outcome

Serializer callers that already own a `ByteBuffer` can write into that buffer or
deserialize its remaining range without requiring a public `ByteArray` round
trip. Existing `ByteArray` APIs remain source- and binary-compatible and retain
their current wire format, null handling, exception policy, security
configuration, and implementation registration behavior.

The public contract is uniform even when an underlying library cannot provide a
true lower-copy path. Such implementations use the compatibility fallback and
are excluded from allocation-improvement claims.

## 2. Authority And Scope

The live issue is the scope authority. This design specifies how to implement
that issue; it does not authorize release, publication, repository-setting, or
credential changes.

### Included

- `BinarySerializer` and its JDK, Kryo, and Fory implementations
- `JsonSerializer` and Jackson 2, Jackson 3, and Fastjson2 implementations
- Avro reflect, generic-record, and specific-record serializers, including list
  APIs
- Heap, direct, sliced, read-only input, non-zero position, and restricted limit
  behavior
- Source/binary compatibility and frozen wire-fixture proof
- Allocation-focused benchmark evidence and English/Korean API documentation

### Excluded

- `CompressableBinarySerializer` optimization, tracked by #755
- Redis/cache, Protobuf, and Kafka integrations, tracked by #756-#758
- Netty `ByteBuf` and Okio `Buffer` APIs without lower-copy evidence
- Per-call library-owned direct-buffer allocation
- Throughput improvement claims
- GitHub Release creation, release holds, GitHub Apps, repository rulesets,
  protected release environments, tag mutation, and publication dispatch

## 3. Public Contract

### 3.1 Output

```kotlin
fun serializeTo(graph: Any?, target: ByteBuffer): Int
```

- Writes from the target's initial position up to its current limit.
- Returns the number of committed bytes.
- On success, advances only `position` by the returned count.
- Preserves `limit`, byte order, and caller ownership.
- Never replaces or grows the caller's buffer.
- A read-only target fails before serializer work starts.
- Insufficient capacity reports `BufferOverflowException`.
- A failed call restores the target position to its initial value; bytes already
  touched are unspecified and callers may retry only after resetting content as
  required by their protocol.

### 3.2 Input

```kotlin
fun <T : Any> deserializeFrom(source: ByteBuffer): T?
```

- Reads exactly the source's initial remaining range.
- Preserves caller `position`, `limit`, byte order, and mark state by operating
  on a duplicate or slice.
- Supports heap, direct, sliced, and read-only sources.
- Preserves the serializer's existing null, empty-input, exception, security,
  and registration behavior.

### 3.3 Compatibility

Existing `ByteArray` methods remain unchanged. New interface members are JVM
default methods so pre-change Java and Kotlin implementations continue to load.
Java input methods use `deserializeFrom` to avoid making existing null-literal
calls ambiguous. Existing Kotlin `deserialize(ByteBuffer)` extensions delegate
to the new polymorphic member.

Avro serializers add corresponding methods for reflect, generic-record,
specific-record, and specific-record list operations. They retain the existing
Object Container File/DataFile wire format.

## 4. Default And Optimized Paths

### 4.1 Compatibility default

Interface defaults delegate to the existing `ByteArray` method. Output checks
capacity before committing bytes. Input copies only the source's remaining
range without changing caller state. These defaults provide API compatibility,
not an allocation claim.

### 4.2 Core binary serializers

- JDK serialization can use fixed ByteBuffer-backed input/output streams while
  retaining the configured `ObjectInputFilter`.
- Kryo can use ByteBuffer input/output adapters and must preserve pool release
  and registration behavior on success and failure.
- Fory may optimize direct input where supported. Output remains the fallback if
  the resolved API can grow or detach caller-provided storage.

### 4.3 JSON serializers

- Jackson 2 and Jackson 3 can use stream-shaped mapper APIs backed by the fixed
  ByteBuffer adapters.
- Fastjson2 JSONB remains on the compatibility fallback unless measurement shows
  that the resolved implementation avoids a full internal array.
- Mapper configuration, polymorphic typing, filters, and wire bytes remain
  authoritative.

### 4.4 Avro serializers

Avro implementations can connect `DataFileWriter` and `DataFileStream` to fixed
ByteBuffer streams while preserving schema, codec, sync marker, close/flush
behavior, and the current DataFile format. Allocation claims require semantic
and allocation proof; byte-for-byte equality is not required when container
metadata is nondeterministic.

## 5. Failure And Resource Policy

- Validate target mutability and capacity constraints before invoking a backend
  when possible.
- Preserve fatal JVM errors and established serializer exception wrappers.
- If backend and cleanup failures both occur, retain the primary failure and
  attach cleanup failures as suppressed without cycles.
- Release pooled serializers, streams, writers, readers, and temporary state in
  `finally` blocks.
- A failed call must not poison the next call or retain caller buffers.
- No optimized path may weaken JDK filtering, Kryo/Fory registration, Jackson
  typing, Fastjson2 JSONB semantics, or Avro schema/codec handling.

## 6. Evidence

### Contract slice

- pinned pre-change jars and fixture manifest
- Java/Kotlin legacy caller compilation
- legacy implementation loading and new-default dispatch
- `javap` verification of executable JVM defaults
- frozen binary fixture reproduction
- heap/direct/sliced/read-only and position/limit tests

### Backend slices

Each backend PR reruns inherited contract tests and adds wire/security/resource
tests for its own optimized path. A backend that cannot safely optimize keeps
the default and records that limitation.

### Allocation slice

Use a repeatable JMH or equivalent allocation protocol with fresh runs. Record
bytes per operation, allocations per operation where available, GC counts,
environment, raw artifacts, and variance. Throughput is diagnostic only.

An allocation claim passes only when repeated runs agree and functional,
compatibility, and security checks remain green.

## 7. Delivery Boundaries

1. Contract/API/ABI defaults
2. Core binary serializers
3. JSON serializers
4. Avro serializers
5. Allocation proof and documentation

Each PR is independently reviewable and mergeable. Descendants are rebased on
the merged predecessor and rerun inherited proof. PR creation is allowed by the
approved delivery plan, but every merge requires fresh exact-head approval after
CI and review are green.

Release, tag, snapshot, Maven publication, and GitHub repository settings remain
outside this design and follow their own workflows and authority gates.

## 8. Acceptance Criteria

- Existing `ByteArray` APIs and compiled callers remain compatible.
- New APIs support heap/direct/sliced/read-only input and bounded output.
- Position, limit, order, mark, overflow, and failure contracts are tested.
- Lower-copy claims are made only for measured backend overrides.
- Wire, security, registration, and resource-lifecycle behavior is preserved.
- Allocation evidence reports allocation/GC metrics rather than throughput alone.
- English/Korean docs explain optimized versus fallback behavior.
- No issue-specific release, credential, settings, tag, or publication machinery
  is introduced by #754.
