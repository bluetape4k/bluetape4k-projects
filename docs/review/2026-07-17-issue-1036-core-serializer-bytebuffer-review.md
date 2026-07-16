# Issue 1036 Core Serializer ByteBuffer Review

## Scope

- Route JDK serialization through fixed ByteBuffer-backed object streams.
- Route Kryo serialization through scoped pooled ByteBuffer adapters.
- Use Fory's supported ByteBuffer input while retaining its ByteArray output fallback.
- Preserve ByteArray APIs, wire bytes, security and registration configuration,
  exception policy, and caller-owned buffer state.

## Review Result

- Final integrated review: APPROVE, P0=0, P1=0.
- Performance: the JDK and Kryo paths avoid the compatibility ByteArray staging
  path; measured allocation and pool-contention claims remain assigned to #1039.
- Stability: Kryo adapters detach caller buffers before returning to the pool,
  and mixed concurrent success, overflow, and malformed-input calls remain isolated.
- Security: JDK applies the configured or global `ObjectInputFilter`; Kryo and
  Fory retain configured registration behavior. Failure logging records only
  `graphType`, never the caller graph.
- Ops: no module registration, Gradle configuration, release, or workflow surface changed.
- Developer/API: no new public or protected JVM method was added in this slice;
  scoped internal adapters avoid exposing pool release ordering to callers.
- User/caller: heap, direct, sliced, and read-only inputs preserve caller state.
  Output failure restores position only; overwritten bytes remain unspecified as
  documented by `BinarySerializer`.

## Resolved Findings

- Removed convenience methods from `AbstractBinarySerializer`; adding protected
  final JVM methods could conflict with external subclasses.
- Replaced direct Kryo adapter obtain/release calls with scoped provider methods
  that always detach caller buffers before pool return.
- Changed ByteBuffer failure logs from `graph=$graph` to bounded type metadata and
  added a regression test whose caller `toString()` throws.
- Verified the resolved Fory dependency is `1.3.0` and that its ByteBuffer input
  overload accepts all supported source shapes.

## Dispositions

- Read-only output rejects before null handling and failure rolls back only the
  position. Both behaviors are existing executable `BinarySerializer` contracts.
- JEP 290 quantitative limits would change the existing ByteArray and ByteBuffer
  security acceptance boundary, so that baseline hardening is outside #1036.
- Allocation rate, throughput, and Kryo pool-contention evidence are deferred to #1039.
- Fory output remains on the compatibility ByteArray fallback because its output
  buffer may grow or detach caller storage.

## Verification

- RED: the initial tests did not compile before the backend ByteBuffer paths existed.
- Core serializer ByteBuffer suite: 18 passing.
- `./gradlew :bluetape4k-io:test --no-configuration-cache --rerun-tasks`:
  1055 passing.
- `git diff --check`: passing.
- Production unsafe-pattern scan: no new matches.
- The module has no `detekt` task; root `detekt` is `NO-SOURCE`, so Kotlin
  compilation and the complete module test suite provide the static fallback.

## Documentation

Public method contracts were established in #1031. Backend capability tables,
measured allocation claims, representative README updates, and CHANGELOG updates
remain assigned to #1039.
