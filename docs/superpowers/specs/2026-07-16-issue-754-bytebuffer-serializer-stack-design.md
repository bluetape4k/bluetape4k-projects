# Issue #754 ByteBuffer Serializer Stack Design

- Issue: [#754 Add ByteBuffer-oriented serializer APIs to reduce allocation pressure](https://github.com/bluetape4k/bluetape4k-projects/issues/754)
- Milestone: `1.12.0`
- Base: `origin/develop@90b267871e9154f242e6de7ee9fd0539f83e509e`
- Delivery: five sequential stacked pull requests
- Performance claim: allocation rate and GC pressure only; no throughput promise

## 1. Outcome

Serializer callers that already own a `ByteBuffer` can write into that buffer or
deserialize its remaining range without forcing a public `ByteArray` round trip.
Existing `ByteArray` APIs remain available and keep their current wire format,
null handling, exception policy, security configuration, and implementation
registration behavior.

The public contract is deliberately uniform even when an underlying library
cannot provide a true lower-copy path. Such implementations use a compatible
fallback and are excluded from allocation-improvement claims.

## 2. Scope

### Included

- `BinarySerializer` and its JDK, Kryo, and Fory implementations
- `JsonSerializer` and the Jackson 2, Jackson 3, and Fastjson2 implementations
- Avro reflect, generic-record, and specific-record serializers, including the
  existing specific-record list APIs
- Heap, direct, sliced, read-only input, non-zero position, and restricted limit
  behavior
- Allocation-focused benchmark evidence and English/Korean API documentation

### Excluded

- `CompressableBinarySerializer` optimization, which belongs to #755
- Redis and cache integrations, which belong to #756
- Protobuf integrations, which belong to #757
- Kafka serializers, which belong to #758
- Netty `ByteBuf` and Okio `Buffer` public APIs unless a later issue proves that
  an adapter avoids an intermediate `ByteArray`
- Library-owned `ByteBuffer.allocateDirect()` on each call
- Any claim that the change improves throughput
- Avro raw binary encoding; the existing Object Container File/DataFile wire
  format remains authoritative

## 3. Current Evidence

### 3.1 Repository behavior

- `BinarySerializer` exposes only `serialize(graph): ByteArray` and
  `deserialize(bytes: ByteArray?)`.
- `BinarySerializerSupport.serializeAsByteBuffer()` wraps a newly allocated
  result array. Its `deserialize(ByteBuffer)` path uses `getBytes()`, whose
  direct-buffer path copies and advances the source while some heap paths do
  not. The current position behavior is therefore inconsistent.
- `ByteBufferInputStream` can consume an existing buffer. Passing a duplicate
  gives the stream an independent position.
- `ByteBufferOutputStream` can wrap an existing buffer, but currently replaces
  its internal buffer when capacity is insufficient. That behavior cannot
  implement a fixed caller-owned target contract.
- `AbstractBinarySerializer` preserves empty/null behavior and wraps failures in
  `BinarySerializationException`.
- JDK serialization retains its `ObjectInputFilter`; Kryo and Fory retain their
  current registration and pooling configuration.
- Fastjson2 currently uses JSONB, not UTF-8 JSON text.
- The Avro implementations currently emit Object Container Files through
  `DataFileWriter`, not raw Avro binary records.

Baseline proof on the isolated branch:

```text
repo-test-summary -- ./gradlew :bluetape4k-io:test --no-configuration-cache
BUILD SUCCESSFUL in 29s
1005 passing (8.5s)
```

### 3.2 Resolved dependency authority

Gradle `dependencyInsight` against the pinned `bt4k` catalog and local catalog
resolved these exact implementation lines:

| Library | Resolved version |
|---|---:|
| JDK | 21 toolchain |
| Kryo | 5.6.2 |
| Apache Fory | 1.3.0 |
| Jackson 2 databind | 2.22.1 |
| Jackson 3 databind | 3.2.0 |
| Fastjson2 | 2.0.62 |
| Apache Avro | 1.12.1 |
| kotlinx-benchmark | 0.4.17 |

### 3.3 Primary-source capability evidence

- JDK `ByteBuffer.duplicate()` and `slice()` share content while keeping
  position, limit, and mark independent. Direct buffers should be caller-owned
  and used only after measurement: [Java 21 ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html).
- `ObjectOutputStream(OutputStream)` and `ObjectInputStream(InputStream)` allow
  fixed ByteBuffer-backed adapters without a final `ByteArrayOutputStream`
  copy: [ObjectOutputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectOutputStream.html),
  [ObjectInputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/ObjectInputStream.html).
- Kryo 5.6.2 provides `ByteBufferOutput(ByteBuffer)` and
  `ByteBufferInput(ByteBuffer)`. A sliced output view is required so a caller's
  current limit, rather than the backing capacity, is the hard maximum:
  [output source](https://github.com/EsotericSoftware/kryo/blob/kryo-parent-5.6.2/src/com/esotericsoftware/kryo/io/ByteBufferOutput.java#L64-L201),
  [input source](https://github.com/EsotericSoftware/kryo/blob/kryo-parent-5.6.2/src/com/esotericsoftware/kryo/io/ByteBufferInput.java#L71-L126).
- Fory 1.3.0 accepts `ByteBuffer` directly for deserialization and preserves the
  caller position and limit. Its caller-supplied `MemoryBuffer` output grows to
  new heap storage when capacity is insufficient, so it cannot satisfy the
  strict fixed-target overflow contract:
  [BaseFory](https://github.com/apache/fory/blob/v1.3.0/java/fory-core/src/main/java/org/apache/fory/BaseFory.java#L203-L227),
  [growth behavior](https://github.com/apache/fory/blob/v1.3.0/java/fory-core/src/main/java/org/apache/fory/memory/MemoryBuffer.java#L4087-L4108).
- Jackson 2 and 3 support `writeValue(OutputStream, value)` and
  `readValue(InputStream, type)`, which remove the final full-payload array while
  retaining internal generator/parser chunk buffers:
  [Jackson 2](https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.22.1/src/main/java/com/fasterxml/jackson/databind/ObjectMapper.java#L3987-L4106),
  [Jackson 3](https://github.com/FasterXML/jackson-databind/blob/jackson-databind-3.2.0/src/main/java/tools/jackson/databind/ObjectMapper.java#L1630-L1817).
- Fastjson2 JSONB exposes stream-shaped APIs, but version 2.0.62 still builds or
  eagerly reads a full internal byte array. Its ByteBuffer overload is therefore
  ergonomic only and not allocation proof:
  [JSONB output](https://github.com/alibaba/fastjson2/blob/2.0.62/core/src/main/java/com/alibaba/fastjson2/JSONB.java#L1909-L1936),
  [JSONB input](https://github.com/alibaba/fastjson2/blob/2.0.62/core/src/main/java/com/alibaba/fastjson2/JSONReaderJSONB.java#L106-L138).
- Avro 1.12.1 `DataFileWriter.create(schema, OutputStream)` and
  `DataFileStream(InputStream, DatumReader)` retain the current DataFile wire
  format while removing the final `ByteArrayOutputStream.toByteArray()` copy:
  [DataFileWriter](https://github.com/apache/avro/blob/release-1.12.1/lang/java/avro/src/main/java/org/apache/avro/file/DataFileWriter.java#L155-L161),
  [DataFileStream](https://github.com/apache/avro/blob/release-1.12.1/lang/java/avro/src/main/java/org/apache/avro/file/DataFileStream.java#L82-L92).

## 4. Public Buffer Contract

### 4.1 BinarySerializer

```kotlin
interface BinarySerializer {
    fun serialize(graph: Any?): ByteArray
    fun <T: Any> deserialize(bytes: ByteArray?): T?

    fun serializeTo(graph: Any?, target: ByteBuffer): Int =
        BufferSerializationDefaults.serializeTo(target) { serialize(graph) }

    fun <T: Any> deserializeFrom(source: ByteBuffer): T? =
        deserialize(BufferSerializationDefaults.copyRemaining(source))
}

fun <T: Any> BinarySerializer.deserialize(source: ByteBuffer): T? =
    deserializeFrom(source)
```

Kotlin keeps the requested `deserialize(source)` spelling through the existing
extension, which delegates polymorphically to the overridable interface default.
Java sees `deserializeFrom(ByteBuffer)`, so an existing Java call such as
`serializer.deserialize(null)` remains unambiguous and continues to select the
`byte[]` method.

### 4.2 JsonSerializer

```kotlin
interface JsonSerializer {
    fun serialize(graph: Any?): ByteArray
    fun <T: Any> deserialize(bytes: ByteArray?, clazz: Class<T>): T?

    fun serializeTo(graph: Any?, target: ByteBuffer): Int =
        BufferSerializationDefaults.serializeTo(target) { serialize(graph) }

    fun <T: Any> deserializeFrom(source: ByteBuffer, clazz: Class<T>): T? =
        deserialize(BufferSerializationDefaults.copyRemaining(source), clazz)
}

fun <T: Any> JsonSerializer.deserialize(source: ByteBuffer, clazz: Class<T>): T? =
    deserializeFrom(source, clazz)

inline fun <reified T: Any> JsonSerializer.deserialize(source: ByteBuffer): T? =
    deserializeFrom(source, T::class.java)
```

The reified Kotlin extension calls the buffer member. Java callers use
`deserializeFrom(source, clazz)` and retain the existing unambiguous
`deserialize(null, clazz)` source form.

### 4.3 Avro serializers

Every current ByteArray operation gains exactly one corresponding target/source
operation. Kotlin input methods retain the current verb and use a distinct JVM
name to preserve Java null-literal source compatibility:

```kotlin
interface AvroReflectSerializer {
    fun <T> serializeTo(graph: T?, target: ByteBuffer): Int =
        BufferSerializationDefaults.serializeNullableTo(target) { serialize(graph) }

    fun <T> deserializeFrom(source: ByteBuffer, clazz: Class<T>): T? =
        deserialize(BufferSerializationDefaults.copyRemaining(source), clazz)
}

interface AvroGenericRecordSerializer {
    fun serializeTo(schema: Schema, graph: GenericRecord?, target: ByteBuffer): Int =
        BufferSerializationDefaults.serializeNullableTo(target) { serialize(schema, graph) }

    fun deserializeFrom(schema: Schema, source: ByteBuffer): GenericData.Record? =
        deserialize(schema, BufferSerializationDefaults.copyRemaining(source))
}

interface AvroSpecificRecordSerializer {
    fun <T: SpecificRecord> serializeTo(graph: T?, target: ByteBuffer): Int =
        BufferSerializationDefaults.serializeNullableTo(target) { serialize(graph) }

    fun <T: SpecificRecord> deserializeFrom(source: ByteBuffer, clazz: Class<T>): T? =
        deserialize(BufferSerializationDefaults.copyRemaining(source), clazz)

    fun <T: SpecificRecord> serializeListTo(collection: List<T>?, target: ByteBuffer): Int =
        BufferSerializationDefaults.serializeNullableTo(target) { serializeList(collection) }

    fun <T: SpecificRecord> deserializeListFrom(source: ByteBuffer, clazz: Class<T>): List<T> =
        deserializeList(BufferSerializationDefaults.copyRemaining(source), clazz)
}

fun <T> AvroReflectSerializer.deserialize(source: ByteBuffer, clazz: Class<T>): T? =
    deserializeFrom(source, clazz)
fun AvroGenericRecordSerializer.deserialize(schema: Schema, source: ByteBuffer): GenericData.Record? =
    deserializeFrom(schema, source)
fun <T: SpecificRecord> AvroSpecificRecordSerializer.deserialize(source: ByteBuffer, clazz: Class<T>): T? =
    deserializeFrom(source, clazz)
fun <T: SpecificRecord> AvroSpecificRecordSerializer.deserializeList(source: ByteBuffer, clazz: Class<T>): List<T> =
    deserializeListFrom(source, clazz)

inline fun <reified T: Any> AvroReflectSerializer.deserialize(source: ByteBuffer): T? =
    deserializeFrom(source, T::class.java)
inline fun <reified T: SpecificRecord> AvroSpecificRecordSerializer.deserialize(source: ByteBuffer): T? =
    deserializeFrom(source, T::class.java)
inline fun <reified T: SpecificRecord> AvroSpecificRecordSerializer.deserializeList(source: ByteBuffer): List<T> =
    deserializeListFrom(source, T::class.java)
```

The exact JVM-visible interface input names are `deserializeFrom` and
`deserializeListFrom`; Kotlin extensions preserve the idiomatic existing verbs.
Generic bounds and schema/class argument order remain identical to the ByteArray
APIs. Interface input members keep their declared JVM names without annotations.

### 4.4 Concrete default behavior

Each interface-owning module (`:bluetape4k-io`, `:bluetape4k-json`, and
`:bluetape4k-avro`) owns a non-public `BufferSerializationDefaults` helper with
identical contract tests. The implementation modules already depend on their
interface module and `:bluetape4k-io`, so this adds no dependency edge, cycle,
or published dependency. Its normative behavior is:

```kotlin
internal inline fun serializeTo(
    target: ByteBuffer,
    produce: () -> ByteArray,
): Int = serializeNullableTo(target, produce)

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
        // Transaction cleanup only; Error is rethrown unchanged.
        target.position(start)
        throw failure
    }
}

internal fun copyRemaining(source: ByteBuffer): ByteArray =
    ByteArray(source.remaining()).also { source.duplicate().get(it) }
```

The implementation may avoid non-local returns, but observable behavior must be
identical. New methods must compile to executable JVM interface defaults under
the repository's existing Kotlin compiler mode; this issue must not change the
global JVM-default compiler setting.

### 4.5 Position, limit, validation, and failure rules

Validation precedence is normative:

1. Kotlin non-null parameter checks reject a Java null `target` or `source` with
   `NullPointerException` before serializer or adapter logic.
2. Reject a read-only output target with raw `ReadOnlyBufferException`, before
   invoking serializer code, including when the graph is null or an empty list.
3. On a writable target, apply the existing ByteArray operation's null/empty
   graph policy. A null result writes nothing and returns `0`.
4. A non-null result with more bytes than `target.remaining()` fails with raw
   `BufferOverflowException`; a zero-remaining target therefore accepts only a
   zero-byte or null result.
5. Otherwise write from `target.position()` through at most `target.limit()`.

On success, `target.position()` advances by exactly the returned byte count.
Capacity, limit, and byte order remain unchanged. The output mark remains usable
when it is not invalidated by the position movement under normal JDK rules. On
overflow or any other failure, the original position is restored. Bytes already
overwritten in the attempted writable range are unspecified; content rollback
is not promised. Bytes before the original position and at or after the original
limit must remain unchanged.

Overflow classification covers operation and cleanup failure graphs separately.
Traversal follows cause and suppressed edges with an identity set and terminates
on cycles. Fatal precedence is applied first, so an `Error` is never reclassified
because overflow appears elsewhere in its graph. For non-fatal failures:

- an operation root that is already a public `BufferOverflowException` is
  rethrown as the same instance;
- if an operation root contains a nested public or recognized native overflow,
  including a suppressed overflow, create one new public
  `BufferOverflowException` whose cause is the complete operation root;
- if the operation is successful and cleanup root is a direct public overflow,
  rethrow that same cleanup instance; if cleanup contains nested/native overflow,
  create a new public overflow whose cause is the complete cleanup root;
- if an ordinary operation failure is followed by cleanup overflow, create a new
  public overflow, set the ordinary operation root as cause, and attach the
  complete cleanup root as suppressed.

Recognized native overflow includes Kryo's overflow type and fixed-adapter
overflow discovered during Avro writer creation, append, flush, or close. Avro
applies this classification before its ordinary log-and-null/log-and-empty
policy, so retry remains possible without losing the original throwable graph.

All `Error` subclasses are fatal for the new buffer transaction. They may be
caught only by the outer transaction to attempt position/reference cleanup and
are never wrapped or translated. An operation-origin `Error` is rethrown as the
same primary instance; a later cleanup `Error` may be attached to it as
suppressed. If the operation had no fatal primary, a cleanup-origin `Error`
becomes and is rethrown as the same primary instance. Position restoration is
guaranteed for normal JDK buffer operations and ordinary exceptions. Under a
fatal VM failure it is best-effort because the runtime itself may no longer be
able to execute cleanup. This intentionally tightens new buffer-path behavior
without changing legacy ByteArray methods that currently catch `Throwable`.

All other failures preserve the current family policy after position restoration:
Binary uses
`BinarySerializationException`, JSON uses `JsonSerializationException`, Avro
single-record operations return `null`, and Avro list deserialization returns an
empty list.

Deserialization reads only `[source.position(), source.limit())` through a
duplicate/slice and preserves the original source position, limit, mark, and
byte order on success and failure. Read-only input is required for every family;
an implementation that cannot use its native direct path must use the preserving
default fallback. Buffers are caller-owned; callers must not mutate or share a
buffer concurrently with an active serializer call.

### 4.6 Null, empty, and malformed-input matrix

| Family/operation | Null graph/list policy | Empty source | Malformed source |
|---|---|---|---|
| Binary serialize/deserialize | null graph -> `0` | `null` | current `BinarySerializationException` policy |
| Jackson 2/3 serialize/deserialize | null graph -> `0` | current `JsonSerializationException` | current `JsonSerializationException` |
| Fastjson2 JSONB serialize/deserialize | null graph -> `0` | `null` | current `JsonSerializationException` |
| Avro reflect serialize/deserialize | null graph -> `0` | `null` | log and return `null` |
| Avro generic serialize/deserialize | null graph -> `0` | `null` | log and return `null` |
| Avro specific serialize/deserialize | null graph -> `0` | `null` | log and return `null` |
| Avro specific list serialize | null or empty list -> `0` | n/a | log and return `0` because the ByteArray sibling returns `null` |
| Avro specific list deserialize | n/a | empty list | log and return empty list |

The matrix is tested against heap, direct, sliced, and read-only buffers. A
side-effecting test serializer proves the read-only preflight happens before the
null/empty operation can invoke backend code. Empty strings, maps, collections,
and other non-null logical values use normal Binary/JSON serialization and must
round-trip; they do not map to zero output. Only Avro specific-list null/empty
input has the explicit zero-output list rule.

### 4.7 Caller retry and publication

A failed target is not publishable, even though its position is restored. The
caller may catch only `BufferOverflowException`, allocate or select a larger
buffer, reset it to the same logical start, retry, and publish a bounded view
only after success. A successful retry must replace the whole attempted logical
range before those bytes are consumed.

For a zero-origin buffer, `clear -> serializeTo -> flip` is valid. For a non-zero
framed start, unconditional `flip()` is incorrect because it exposes preceding
bytes. The caller saves `start`, uses the returned count, and creates a bounded
duplicate with `position(start)` and `limit(start + count)`, then slices that
view for the consumer.

## 5. Compatibility Strategy

The new methods are concrete interface methods that delegate to the existing
ByteArray APIs. Existing third-party implementations therefore remain binary
compatible and automatically receive the ergonomic buffer API. Source
compatibility is proved for both Kotlin and Java, including existing Java
`deserialize(null)` and `deserialize(null, clazz)` calls. The distinct JVM input
names prevent the new non-null `ByteBuffer` members from making those calls
ambiguous.

The existing top-level `BinarySerializer.deserialize(ByteBuffer)` extension is
kept at the same static JVM symbol and retains its existing `getBytes()` plus
ByteArray delegation, including the historical direct/read-only-buffer position
consumption. The new overridable `deserializeFrom(ByteBuffer)` interface default
is the preserving API. This avoids changing framed legacy loops while giving new
callers one consistent source-state contract. The extension is not deprecated or
removed in `1.12.0`.

Default output implementations serialize to `ByteArray`, validate remaining
capacity, and then perform one relative `put`. This is compatibility-only and
must not be described as lower allocation. Optimized implementations override
the default only when primary-source evidence and benchmarks justify it.

Compatibility proof has two levels:

1. Deterministic formats compare each buffer result byte-for-byte with the
   existing ByteArray sibling and perform cross-path round trips.
2. Avro OCF does not use universal byte identity as an oracle because fresh
   `DataFileWriter` instances may emit permitted variable container fields.
   The semantic oracle parses both files and requires: valid `Obj\u0001` magic;
   `Schema.equals` equality for the embedded writer schema; exact `avro.codec`
   name; byte equality for every non-reserved user metadata entry; equal decoded
   record count, order, and value under the same expected reader schema/class;
   positive block counts, non-negative in-bounds block sizes, every block trailer
   matching that file header's 16-byte sync marker, clean EOF, and no trailing
   corruption. Permitted differences are the 16-byte sync marker, block
   partitioning/count/size encoding, and codec-produced block bytes. No byte
   rewriting normalization is allowed. Byte identity is required only for a
   fixture explicitly proven deterministic; otherwise this parsed semantic tuple
   is the sole pass/fail oracle.

The pre-change authority is a clean artifact built from
`origin/develop@90b267871e9154f242e6de7ee9fd0539f83e509e`, not an inferred Maven
version. Its jar and frozen fixture SHA-256 checksums, Java/Kotlin/Gradle versions,
serializer configuration, schema/codec, and producer commit are recorded. At
each stack head, pre-change readers consume new
output, new readers consume frozen pre-change output, and mixed old/new producer
and consumer combinations retain object filters, mapper/polymorphic features,
Kryo/Fory registrations, Avro reader schema, and codec behavior.

ABI proof compiles minimal third-party Kotlin and Java implementations and
callers against the pre-change interfaces, then runs those classes against the
new artifact. The Java fixture includes null-literal calls. `javap` verifies
that all new methods are non-abstract executable JVM defaults, Java-visible
input methods use the `deserializeFrom`/`deserializeListFrom` names, and the
legacy static extension symbol remains present.

| Caller surface | Before 1.12.0 | 1.12.0 guidance |
|---|---|---|
| `serializeAsByteBuffer(graph)` | Allocates a ByteArray and returns a ready-to-read wrapped buffer | Remains allocating and behavior-compatible; use `serializeTo` for caller storage |
| Kotlin `deserialize(buffer)` extension | Heap/direct paths may differ; a direct source can advance | Same static symbol and legacy ByteArray delegation preserve the existing behavior; use `deserializeFrom` for source-state preservation |
| Kotlin new output | n/a | `serializeTo` leaves target positioned after output; use returned count for framed writes |
| Java new input | n/a | Call `deserializeFrom(buffer, ...)`; existing `deserialize(null, ...)` remains unchanged |

## 6. Fixed Buffer Adapters

`ByteBufferOutputStream(buffer)` retains its current growable behavior. A new
`ByteBufferOutputStream.fixed(buffer)` factory creates a non-growing view that
throws `BufferOverflowException` when the current limit is exhausted.

The exact added public/JVM surface is:

```kotlin
companion object {
    @JvmStatic
    fun fixed(buffer: ByteBuffer): ByteBufferOutputStream
}
```

Java calls `ByteBufferOutputStream.fixed(buffer)`. Kotlin's non-null parameter
check rejects a Java null argument with `NullPointerException` before factory
logic. The factory rejects a read-only buffer immediately with
`ReadOnlyBufferException`, records the factory-time position as `start`, and
uses the exact supplied buffer view: writes alias its storage, advance that
view's position, honor its current limit, and never access capacity beyond the
limit or replace/grow storage. Serializer transactions pass a duplicate/slice,
not the caller's original position-bearing object.

For a fixed stream, inherited `toByteArray()` returns a new array containing
only `[start, currentPosition)`; bytes preceding the factory-time position are
excluded. For all existing growable factories it retains the current
`[0, currentPosition)` behavior. Optimized serializer paths do not call
`toByteArray()`.

Optimized stream implementations use this transaction pattern:

1. reject a read-only target;
2. save the original target position;
3. create `target.duplicate()` and wrap the duplicate in the fixed stream;
4. serialize and flush/close the library writer against the no-op-close adapter;
5. compute bytes written from the duplicate position;
6. commit the original target position only after success;
7. leave the original position unchanged on every exception.

InputStream-based implementations wrap `source.duplicate()` so parser read-ahead
cannot alter the caller's source position. Closing the adapter is a no-op with
respect to the caller-owned buffer.

The adapter is not a transactional content store. Partial writes remain visible
after a failed operation, matching the public failure contract.

The fixed adapter is owned by `:bluetape4k-io` alongside the existing growable
adapter. `close()` is idempotent, does not close or invalidate the caller buffer,
and retains the existing no-op-close convention: subsequent adapter writes are
allowed. Writer create/append/flush/close failures restore only the original
target position. Deterministic fault-injection tests fail on the first write,
after N bytes, on flush, and on close; they assert canaries outside the attempted
range, make no assertion inside it, then prove a retry succeeds.

Dual-failure precedence is normative:

| Primary operation | Cleanup/close/reset/release | Public result |
|---|---|---|
| fatal `Error` | any failure | attempt cleanup, attach non-identical cleanup failure as suppressed when possible, rethrow the same `Error` instance |
| success or non-fatal primary | fatal cleanup `Error` | cleanup `Error` becomes the fatal primary and is rethrown unchanged; attach the preceding non-identical non-fatal primary as suppressed when possible |
| direct public operation overflow | any non-fatal cleanup | rethrow the same public overflow instance; attach cleanup failure as suppressed |
| operation root containing nested/native overflow | any non-fatal cleanup | new public overflow; complete operation root is cause and cleanup failure is suppressed |
| ordinary operation failure | cleanup root containing public/native overflow | new public overflow; operation root is cause and cleanup root is suppressed |
| ordinary backend failure | ordinary cleanup failure | preserve the family primary policy; cleanup failure is suppressed and never replaces it |
| success | direct public cleanup overflow | rethrow the same cleanup overflow instance |
| success | cleanup root containing nested/native overflow | new public overflow with complete cleanup root as cause |
| success | ordinary cleanup-only failure | apply the family's ordinary failure policy |

Suppression never adds a throwable to itself or walks an identity twice. A fatal
operation primary remains primary; otherwise a cleanup-originated fatal `Error`
supersedes a preceding success/non-fatal result exactly as the table states.
Tests inject write+close, append+close, overflow+reset, malformed+release,
fatal-operation+cleanup, success+fatal-cleanup, overflow+fatal-cleanup, and
ordinary-failure+fatal-cleanup dual faults.

Kryo uses a specific commit algorithm: create `target.slice()` with the caller's
remaining range and explicitly set the slice to `ByteOrder.BIG_ENDIAN`, matching
the existing Kryo `Output` wire fixture regardless of the caller buffer's order.
The preserving input duplicate/slice uses that same explicit wire order. Pass it
to a call-scoped
`ByteBufferOutput`, prohibit any replacement/growth, and capture the count from
the Kryo output/slice position rather than the parent duplicate before wrapper
reset. Wrapper close/reset and pooled-Kryo release all complete before commit.
Only then does the implementation set the original target position to
`start + count`; any pre-commit cleanup/release failure leaves it at `start`.
The caller buffer's own byte order remains unchanged.

## 7. Implementation Capability Matrix

| Implementation | Output strategy | Input strategy | Claim |
|---|---|---|---|
| JDK | `ObjectOutputStream` over fixed duplicate | `ObjectInputStream` over duplicate | Copy-topology improvement; removes final payload array |
| Kryo 5.6.2 | pooled Kryo + `ByteBufferOutput(target.slice())` | pooled Kryo + `ByteBufferInput(source.duplicate())` | Direct caller-buffer path |
| Fory 1.3.0 | interface ByteArray fallback | official `deserialize(source.duplicate())` | Input copy-topology improvement; output ergonomic only |
| Compressable | unchanged until #755 | unchanged until #755 | Excluded |
| Jackson 2 | `ObjectMapper.writeValue(fixedStream, graph)` | `readValue(duplicateStream, clazz)` | Copy-topology improvement; internal chunks remain |
| Jackson 3 | same as Jackson 2 | same as Jackson 2 | Copy-topology improvement; internal chunks remain |
| Fastjson2 JSONB | interface ByteArray fallback | interface ByteArray fallback | Ergonomic only; excluded from claim |
| Avro reflect | `DataFileWriter` to fixed stream | `DataFileStream` from duplicate stream | Copy-topology improvement; block buffers remain |
| Avro generic | same as reflect | same as reflect | Same claim |
| Avro specific/list | same as reflect | same as reflect | Same claim |

`Copy-topology improvement` is a source-backed statement naming an eliminated
full-payload array or copy. `Allocation-improving` is reserved for a benchmark
cell that passes Section 10. Internal chunks, wrappers, and backend buffers are
named as residual allocation; neither term means zero-copy.

Only the configured Kryo instance remains pooled for these paths. New
`ByteBufferInput` and `ByteBufferOutput` wrappers are call-scoped, closed/cleared
on every path, and never returned to a pool, so no pooled object retains a heap
or direct caller buffer. Fory continues using the repository's `ThreadSafeFory`;
registration is completed before concurrent use. Jackson configuration is
completed before first use. Fastjson writers and Avro encoders/readers are
call-scoped; only documented thread-safe factories/configuration may be shared.
Failure-then-success and confined concurrent calls prove serializer instances
are not poisoned after overflow or malformed input.

## 8. Stacked Pull Request Delivery

### PR 1: Buffer contract

- Branch: `feat/issue-754-buffer-contract`
- Base: `develop`
- Adds concrete interface overloads, compatibility shims, fixed buffer adapter,
  contract tests, KDoc, and `.github/release-holds/1.12.0-issue-754.json`
- Adds `scripts/check-release-holds.py` and wires the mandatory
  `release-hold-1.12.0-issue-754` precondition job into both
  `.github/workflows/publish-snapshot.yml` and `.github/workflows/release.yml`;
  every publish/tag job declares `needs` on it with no continue-on-error or
  manual override input
- Changes `release.yml` to workflow-dispatch-only for `1.12.0`: after the hold
  job it creates the exact approved tag and then publishes. The existing
  push-tag trigger is removed, and an installed GitHub tag ruleset
  `release-tags-1.12.0` rejects direct creation/update/deletion of `1.12.0`
  outside that workflow's release-exclusive GitHub App installation
- Preserves releases for every version other than `1.12.0` in
  `release-generic.yml`. Its push trigger explicitly excludes `1.12.0`, its
  manual resolver rejects `1.12.0`, and its publication jobs retain the
  existing `maven-central-release` environment
- Does not close #754

### PR 2: Core serializer implementations

- Branch: `feat/issue-754-core-serializers`
- Initial base: PR 1 branch
- Adds optimized JDK/Kryo output, JDK/Kryo/Fory input, security/registration
  regression tests, and capability documentation
- Leaves Fory output on the compatibility fallback
- Does not close #754

### PR 3: JSON serializer implementations

- Branch: `feat/issue-754-json-serializers`
- Initial base: PR 2 branch
- Adds Jackson 2/3 optimized paths, JSON contract tests, and Fastjson2 fallback
  documentation/tests
- Does not close #754

### PR 4: Avro serializer implementations

- Branch: `feat/issue-754-avro-serializers`
- Initial base: PR 3 branch
- Adds reflect/generic/specific/list buffer paths while preserving DataFile wire
  compatibility and codec behavior
- Includes schema evolution/mismatch, logical type, malformed metadata,
  unsupported codec, declared-block/decompression, and record-count bounded
  security parity evidence
- Does not close #754

### PR 5: Allocation proof and documentation

- Branch: `feat/issue-754-allocation-proof`
- Initial base: PR 4 branch
- Adds `benchmark/serializer-bytebuffer-benchmark`, allocation/GC evidence,
  README/KDoc limits, and the final DoD update
- Uses `Closes #754`; all four predecessors must already be merged before PR 5
  can merge

Each PR is reviewed and squash-merged independently. After its predecessor merges, the
next branch is rebased onto the new `origin/develop`, its PR base is retargeted
to `develop`, the exact remote head is verified, and required CI/review gates are
rerun. Every merge requires fresh user approval tied to that PR's current head.
Downstream stack branches are never merged with stale predecessor commits.
If a merged slice must be rolled back, revert exactly from the newest merged
dependent slice through the failed slice, inclusive. Earlier independent slices
remain unless their own contract is invalid; the contract PR is not reverted
while merged implementations still compile against it.

| PR | Required local/CI proof | Retained evidence |
|---|---|---|
| 1 contract | `:bluetape4k-io:test`, Kotlin/Java ABI fixtures, `javap`, contract tests, release-hold validator unit fixtures, and both workflow dependency assertions | ABI reports, pre-change fixture metadata, and release-hold enforcement proof |
| 2 core | `:bluetape4k-io:test`, JDK/Kryo/Fory targeted tests, full rolling-compatibility matrix | wire fixture report and security/registration results |
| 3 JSON | Jackson 2/3 and Fastjson2 targeted tests plus the full inherited stack gate | JSON/JSONB compatibility and polymorphic-security report |
| 4 Avro | `:bluetape4k-avro:test`, bounded hostile schema/metadata/codec/block/decompression/record-count cases, plus the full inherited stack gate | OCF schema/codec/record compatibility and security-parity report with frozen fixtures |
| 5 proof/docs | benchmark task discovery, `gcProfile`, documentation checks, all affected module tests, `./gradlew build` | checksummed raw JMH JSON, environment manifest, compatibility report, release-hold decision |

After every predecessor rebase, the descendant reruns its own row and all rows
it inherits; the PR description records exact head SHA, commands, exit codes,
and artifact links. A green check from a pre-rebase SHA is not merge evidence.

Evidence that must outlive CI is committed under
`docs/evidence/issue-754/<slice>/` with stable names, a schema-versioned JSON
manifest, raw machine-readable files, environment metadata, and `SHA256SUMS`.
Frozen compatibility fixtures live under the relevant test resources and record
their producer commit. CI additionally uploads
`issue-754-pr<slice>-<head-sha>-evidence`; expiration of that convenience artifact
does not remove the committed evidence. Recovery validates `SHA256SUMS` and
reruns the row when either committed evidence or its checksum is missing.

For each squash merge, the evidence manifest/check records PR number, PR head
SHA/tree SHA, merge SHA/tree SHA, commands, and check-run URLs. Immediately after
merge, `git rev-parse <pr-head>^{tree}` must equal
`git rev-parse <merge-sha>^{tree}`. A mismatch invalidates pre-merge evidence and
requires the full inherited gate on the merge SHA. PR 5's merge SHA is the
initial release-candidate head; if `develop` advances before release, the full
five-row gate is rerun and attached to the actual release-candidate SHA.

The `1.12.0` release train is held while any of PRs 1-5 is missing from the exact
release head or its final ABI, rolling-compatibility, documentation, or allocation
evidence is absent. PR 4's Avro security-parity report is part of that required
evidence. No `1.12.0` tag, release branch, snapshot/release publication,
or release workflow dispatch may cross that hold.

`.github/release-holds/1.12.0-issue-754.json` is the machine-readable authority:
it names issue #754, all five PRs once known, committed evidence paths, and the
required exact-head check. Snapshot/release/tag workflows fail their precondition
while #754 is open, a listed PR is unmerged, a checksum fails, PR 5 head/merge
trees differ without a merge-head rerun, or the actual release-candidate SHA
lacks the full gate. The maintainer/user clears the hold only by obtaining that
green exact-head check; no manual boolean bypass or auto-merge is allowed.
PR 1 must prove the validator's hold/pass/malformed/checksum/tree-mismatch cases
and statically assert that both workflow files expose the exact mandatory job
name and make every side-effecting publish/tag job depend on it.

The committed authority records `evidenceProducerSha`, not a self-referential
final candidate SHA. The exact candidate is an external workflow/CLI input and
is emitted in the validation decision only after the commit exists. Validation
recomputes `testedCodeTreeSha256` from both the producer and exact candidate;
the covered paths include the Jackson 2, Jackson 3, and Fastjson2 implementations.
ABI generation rejects dirty covered paths. The `proof-allocation` report is a
distinct strict schema: `gc.alloc.rate.norm` in `B/op`, the pinned 3-fork/5-warmup/
5-measurement protocol, two distinct runs, full JDK/JVM/heap/GC/OS/architecture
metadata, checksummed raw artifacts, and threshold-checked per-cell results are
required before a `PASS` can clear the hold.

Because the current `release.yml` tag-push trigger runs after tag creation, PR 1
removes that trigger and makes the checked workflow the only supported 1.12.0
tag creator. Installing/updating the non-bypassable GitHub tag ruleset is a
separate repository-setting side effect requiring fresh user approval after PR 1
merges. PR 1 is not operationally complete, and the release hold cannot clear,
until `gh api` evidence records the ruleset ID, include patterns `1.12.0` and
`release-gate-probe/issue-754/*`, allowed actor, and denied probe operations.
The ruleset contains exact creation, update, and deletion rules. A twin ruleset
must deny ordinary creation before the tag App creates its probe; after bypass
removal, both ordinary and tag-App update/delete attempts must remain denied.

The sole bypass actor is the dedicated GitHub App installation
`bluetape4k-release-tag-bot`, not the generic GitHub Actions app, repository
admins, users, teams, deploy keys, or `GITHUB_TOKEN`. Its installation ID and
private-key secret exist only in the protected `release-tag-1.12.0` environment,
which requires a designated human reviewer; only `release.yml` may reference
that environment. Static workflow audit proves no other workflow can request the
credential. Pre-release live probes never create the production tag: denied
actors attempt a fresh UUID under `release-gate-probe/issue-754/*` and must fail
create/update/delete, while the release App creates and deletes that probe tag
through the protected environment. The exact `1.12.0` allow path is exercised
only once by the approved release job after the hold clears; immutable-tag
verification then rejects replacement or deletion.

The production ref points to an annotated tag object whose exact message binds
the release request ID and whose target binds the candidate commit. Response-loss
reconciliation treats only that object SHA as owned by the current request. If
bypass removal succeeds but artifact upload is interrupted, a rerun with the
same request ID may reconstruct the closeout from the no-bypass ruleset and the
same annotated tag; a different request, direct commit tag, or mismatched target
remains blocked. Maven/signing secrets stay in `maven-central-release` for
generic versions while repository-scoped copies remain forbidden.

Recovery freezes all open descendants and reverts from the newest merged slice
through the failed slice, inclusive,
invalidates old descendant CI, and rebases or closes affected PRs. The revert
head reruns the ABI and rolling-compatibility matrix and updates release notes
and evidence manifests. Resume only when the new stack head passes its complete
inherited gate; abandon the stack when the contract itself is reverted or a
backend cannot preserve wire/security behavior. If publication somehow occurred,
normal immutable-artifact policy applies: do not overwrite it; publish explicit
corrective guidance and move recovery to a successor version.
If Maven Central accepts publication but the workflow loses the response, do
not retry blindly. Verify the external deployment first, keep GitHub Release
creation blocked until that readback is authoritative, then complete the
GitHub Release as a separately approved recovery action.

## 9. Test Design

Every serializer family receives parameterized contract coverage for:

- heap and direct buffers;
- non-zero target/source position;
- a limit lower than capacity;
- sliced buffers and non-zero array offsets;
- exact returned byte count and successful target position advance;
- target capacity, limit, byte order, and mark/reset behavior after success,
  overflow, backend failure, flush failure, and close failure across heap,
  direct, and sliced targets;
- source position/limit/mark/byte-order preservation on success and malformed
  input, including successful `reset()` on heap, direct, and sliced sources;
- read-only input for every family, using the preserving fallback when needed;
- read-only output rejection;
- exact-capacity success and one-byte-short overflow;
- original target position restoration after overflow and serializer failure;
- deterministic first-write, N-byte, flush, and close failures with canaries
  outside the attempted range and no assertion inside that range;
- failure followed by successful retry on the same serializer instance;
- null and empty behavior;
- ordinary empty strings, collections, and maps still serialize and round-trip
  for Binary/JSON; only null graphs and Avro specific null/empty lists map to
  zero output;
- Java compilation/runtime coverage for every new interface method, including
  null `target`/`source` arguments and preserved legacy null-literal overloads;
- deterministic byte-for-byte parity and Avro OCF semantic parity as defined in
  Section 5;
- concurrent serializer-instance use where the current implementation promises
  it, while keeping each buffer confined to one call.

Pool lifecycle checks prove overflow/malformed calls release call-scoped
wrappers and caller references, a following success is clean, and heap/direct
buffers can be garbage-collected after the call. Correctness concurrency uses
one confined buffer per invocation; pool contention and scalability are not
performance claims in this issue.

### 9.1 Security and resource-exhaustion regressions

The overloads do not make untrusted deserialization safe and add no generic
size, depth, collection-count, decompression, or reference limit. Every uncapped
path, including Avro, is unsupported for adversarial untrusted input unless the
caller first enforces application-specific framing/resource budgets and enables
the backend's applicable controls. Registration, object filters, and
polymorphic validators are type/graph controls, not complete CPU, memory,
decompression, or object-count controls.

Compatibility fallbacks allocate exactly `source.remaining()` heap bytes before
backend controls execute. They are therefore trusted/caller-bounded paths; this
issue deliberately adds no pre-copy size limit because doing so would change the
existing serializer contract. A ByteBuffer limit bounds encoded input bytes
only. It does not bound decompressed bytes, object count, depth, references,
native memory, or CPU. KDoc, capability tables, and release notes state this.
Hostile tests use small deterministic fixtures and test-process budgets; no test
attempts unbounded allocation merely to prove failure behavior.

- JDK installs the configured serializer `ObjectInputFilter` before the first
  `readObject`. If that filter is null, the JVM global serial filter remains the
  authority. Default, custom, and null-with-global cases must reject the same
  payload with the same exception/cause family as the ByteArray path. The global
  filter case runs in an isolated forked JVM with pinned filter configuration
  and payload so process-global state cannot make tests order-dependent.
- Default Kryo and Fory configurations permit broad class handling and are not
  safe for untrusted data. Their registration-enforcing modes must reject a
  payload produced by a permissive instance when read through heap, direct, and
  read-only ByteBuffer paths.
- Jackson 2/3 reuse the same mapper instance and do not enable or broaden default
  typing or polymorphic validators. Fastjson2's JSONB fallback must not enable
  `SupportAutoType`. Crafted type-metadata fixtures prove buffer paths behave
  exactly like ByteArray paths.
- Avro treats embedded writer schema and codec metadata as untrusted. For
  reflect input, construct `ReflectDatumReader(readerSchema, readerSchema)` where
  `readerSchema = schemaOf(clazz)`; for generic input use
  `GenericDatumReader<GenericData.Record>(suppliedSchema)`; for specific and list
  input use `SpecificDatumReader(clazz)`. `DataFileStream` then installs the OCF
  embedded schema as the actual writer schema while preserving the constructor's
  supplied schema/class as the expected reader constraint. Standard Avro
  resolving rules remain authoritative for aliases, defaults/evolution, logical
  type conversions, and specific-class materialization. Incompatible schemas,
  invalid logical conversions, malformed metadata, unsupported codecs, and
  corrupt blocks follow the existing Avro null/empty family policy after logging.
  Bounded parity tests cover those cases plus oversized declared blocks,
  decompression expansion, excessive collection/reference shapes, and excessive
  list record counts. The API adds no general record-count cap, so Avro input is
  trusted/caller-bounded only and documentation states that limit.
- Bounded payloads exercise deep nesting, declared lengths, collections, object
  references, Avro blocks, and decompression without turning the test itself into
  an OOM/denial-of-service event.

## 10. Allocation Proof

PR 5 adds an unpublished, auto-registered
`benchmark/serializer-bytebuffer-benchmark` module using the existing
`kotlinx-benchmark` and JMH conventions. Gradle task discovery must show the
standard `benchmark` and `benchmarkBenchmark` tasks before results are recorded.
The module-addition gate also verifies `settings.gradle.kts` auto-registration,
project/catalog checks, benchmark publication exclusion, and the repository's
generated catalog/check scripts required for a new module.

Because kotlinx-benchmark 0.4.x silently ignores the repository's previously
attempted `advanced()` profiler configuration, the module also owns a separate
`:serializer-bytebuffer-benchmark:gcProfile` Gradle task. It depends on the
generated runnable JMH artifact, executes that artifact with `-prof gc`, writes
JMH JSON, and fails if the result lacks GC-profiler metrics. Each fresh evidence
command uses a new UUID and is exactly:

```text
./gradlew :serializer-bytebuffer-benchmark:gcProfile --no-configuration-cache \
  --rerun-tasks -PbenchmarkRunId=<new-uuid>
```

`gcProfile` declares itself non-up-to-date, rejects an existing run ID/directory,
and writes only to
`build/reports/benchmarks/issue-754/<run-id>/`. The validator proves the two run
IDs differ, prevents output overwrite/reuse, and copies checked raw JSON and
environment manifests into the durable Section 8 evidence path.

Each supported optimized implementation compares:

1. the existing `serialize(graph): ByteArray` baseline;
2. `serializeTo(graph, reusedHeapBuffer)`;
3. `serializeTo(graph, reusedDirectBuffer)` where supported;
4. ByteArray deserialization;
5. heap/direct ByteBuffer deserialization.

Payloads cover small and representative medium objects. Benchmarks use
`@State(Scope.Thread)` and thread-confined buffers. Setup allocates, pre-touches
direct buffers, and pre-encodes deserialization input outside measurement; the
measured operation resets only position/limit and consumes its return/value via
the benchmark return or `Blackhole`. Baseline and candidate use identical
logical ranges and payloads. No measured operation may allocate another direct
buffer or retain the caller buffer after return.

The pinned protocol is 3 forks, 5 one-second warmup iterations, and 5 one-second
measurement iterations with the same JDK, heap size, GC, and JVM flags across
baseline and candidate. Results require `gc.alloc.rate.norm` as the primary
bytes-per-operation metric and retain `gc.alloc.rate`, `gc.count`, and `gc.time`
when emitted. Throughput is context only and cannot satisfy the issue.

Claims are gated independently by implementation, direction, payload, and
heap/direct cell; aggregate averages cannot hide a regressing cell. A cell is
`allocation-improving` only when two complete protocol runs both show a median
`gc.alloc.rate.norm` at least 10% below its ByteArray baseline and no fork is
more than 5% worse than baseline. If the two runs disagree, a cell misses the
gate rather than selecting the better run. Neutral/regressing cells remain
ergonomic or copy-topology improvements and are documented that way. Fory output
and Fastjson2 JSONB are fallback controls, not success targets.

The gate algorithm is deterministic and applied separately to each complete
run. For each method and each of its 3 forks, take the median of that fork's 5
finite `gc.alloc.rate.norm` measurement-iteration values as the fork score. Take
the median of the three ByteArray baseline fork scores as `B` and the median of
the three candidate fork scores as `C`; require `B > 0` and
`(B - C) / B >= 0.10`. Independently require every candidate fork score to be
`<= B * 1.05`. Baseline and candidate are from the same run/environment; forks
are not paired by index and samples are never pooled across runs. A missing,
NaN, infinite, or wrong-cardinality value fails that run and therefore the claim.

The evidence artifact records exact commit, JDK vendor/version, OS/architecture,
heap/GC/JVM flags, payload shape, warmup/measurement/fork counts, exact command,
raw per-fork JMH JSON paths, and SHA-256 checksums. Both complete fresh runs and
an environment manifest are retained. The validation task fails when required
metrics or raw files are absent. Direct-buffer results prove only Java-heap
per-call allocation under this protocol; they do not prove native-memory cost,
lifecycle, throughput, contention, or scalability improvements.

## 11. Documentation

- Public KDoc states position, limit, overflow, failure-content, null, ownership,
  and thread-safety rules.
- English and Korean module READMEs use identical capability tables and avoid
  `zero-copy` wording where internal chunks or arrays remain.
- Kotlin and Java examples show zero-origin `clear/serializeTo/flip`, non-zero
  framed output using `[start, start + count)`, and overflow retry into a larger
  buffer. They explicitly mark failed-target content unusable until overwritten
  by a complete successful retry.
- Migration guidance distinguishes the preserving buffer member from the legacy
  static extension, documents `serializeAsByteBuffer()` as allocating, gives the
  exact Java `deserializeFrom` names, and states that the existing Binary Kotlin
  extension is not deprecated in `1.12.0`.
- Release notes cover sizing/retry, position-only rollback, partial writes,
  read-only failures, native versus fallback implementations, old/new
  interoperability evidence, trust boundaries, diagnostics/evidence paths, and
  the release hold. They explicitly defer compression, Redis, Protobuf, and
  Kafka integration to #755-#758.

The non-zero output pattern is normative:

```kotlin
val start = target.position()
val count = serializer.serializeTo(value, target)
val payload = target.duplicate()
    .position(start)
    .limit(start + count)
    .slice()
```

```java
int start = target.position();
int count = serializer.serializeTo(value, target);
ByteBuffer payload = target.duplicate()
    .position(start)
    .limit(start + count)
    .slice();
Object value = serializer.deserializeFrom(payload);
MyType jsonValue = jsonSerializer.deserializeFrom(payload, MyType.class);
MyPojo reflected = avroReflect.deserializeFrom(payload, MyPojo.class);
GenericData.Record generic = avroGeneric.deserializeFrom(schema, payload);
MyRecord specific = avroSpecific.deserializeFrom(payload, MyRecord.class);
List<MyRecord> records = avroSpecific.deserializeListFrom(payload, MyRecord.class);
```

Java output examples preserve each argument shape:

```java
int jsonBytes = jsonSerializer.serializeTo(value, target);
int reflectBytes = avroReflect.serializeTo(value, target);
int genericBytes = avroGeneric.serializeTo(schema, record, target);
int specificBytes = avroSpecific.serializeTo(record, target);
int listBytes = avroSpecific.serializeListTo(records, target);
```

## 12. Failure Modes and Mitigations

| Failure mode | Consequence | Mitigation and proof |
|---|---|---|
| Adapter silently grows away from caller target | Returned count/position no longer represents caller bytes | Separate fixed factory; exact-capacity and one-byte-short tests |
| Parser consumes caller source or reads beyond limit | Retry and framing logic breaks | Always duplicate/slice; assert source state and trailing sentinel |
| Overflow is wrapped or swallowed | Caller cannot resize/retry reliably | Re-throw `BufferOverflowException`; module-specific failure tests |
| Interface change breaks third-party implementations or Java null calls | Binary linkage or source compilation failure | Concrete JVM defaults, distinct Java input names, Kotlin/Java fixtures, retained extension symbol |
| Wire format changes during optimization | Existing persisted/cache payloads become unreadable | Deterministic byte parity plus Avro OCF semantic parity and two-version cross-reading |
| Security/registration setup is bypassed | Deserialization accepts unsafe/unregistered types | Reuse current configured instances and explicit regression tests |
| Library closes caller-owned resource | Buffer adapter becomes unusable after one call | Adapter close is no-op; close-behavior tests |
| Pool retains caller buffer | Heap/direct storage lifetime leaks across calls | Call-scoped wrappers, cleanup on every path, retention/lifecycle tests |
| Hostile input expands without a bound | Memory/CPU exhaustion | Preserve backend security controls; document caller framing and run bounded hostile fixtures |
| Benchmark counts setup allocation | False allocation improvement or regression | Reuse buffers/payloads outside measured method and use GC profiler |
| Stacked PR base becomes stale | CI proves a different composition than the merged result | Rebase/retarget after every predecessor merge and revalidate exact head |
| Partial stack is released as 1.12.0 | Public API ships without proof or backend coverage | Exact-head release hold cleared only by PR 5 evidence manifest |

## 13. Alternatives Rejected

### One monolithic PR

Rejected because the API/ABI contract, core backends, JSON, Avro, and benchmark
proof have different failure modes and review surfaces. A five-PR stack keeps
each slice independently testable while preserving dependency order.

### A separate `BufferBinarySerializer` capability interface

Rejected because callers would need runtime type checks and fallback branching,
and every integration would duplicate the same negotiation logic. Concrete
default methods preserve existing implementations while optimized overrides can
be introduced incrementally.

### Extension-only ByteBuffer APIs

Rejected because extensions cannot be overridden polymorphically by serializers
with native buffer support. They also caused the current inconsistent direct/heap
position behavior.

### Library-owned direct buffer allocation

Rejected because direct allocation is expensive, ownership/lifetime is unclear,
and it defeats caller-controlled reuse. The caller chooses heap or direct storage.

### Preflight serialization to guarantee content rollback

Rejected because staging or size computation reintroduces allocation/work and
would negate the target use case. The contract guarantees position rollback,
not byte-content rollback.

### Fory `MemoryBuffer` output as a strict implementation

Rejected for the optimized path because Fory grows insufficient caller-backed
buffers into new heap storage instead of throwing at the target limit. The
default ByteArray fallback remains correct and is documented as ergonomic only.

### Fastjson2 JSON text stream APIs

Rejected because the current serializer's wire format is JSONB. Switching to
text JSON would be a compatibility break, and JSONB 2.0.62 stream APIs still
buffer the full payload internally.

### Avro raw `directBinaryEncoder`

Rejected because current callers consume Object Container Files. The optimized
path must retain `DataFileWriter`/`DataFileStream` and codec metadata.

## 14. Acceptance Criteria Traceability

| Issue criterion | Design proof |
|---|---|
| Inventory public ByteArray APIs | Sections 3 and 4 cover Binary, JSON, and all Avro families |
| Add compatible buffer APIs | Sections 4 and 5 define concrete defaults and retained shim |
| Implement true lower-copy where supported | Section 7 capability matrix |
| Heap/direct, position, limit, preservation tests | Section 9 contract matrix |
| Allocation/GC evidence, not throughput only | Section 10 benchmark gate |
| README/API scope and limits | Section 11 bilingual documentation |
| Keep compression/integrations separate | Section 2 exclusions and Section 8 stack |

## 15. Six-Perspective Review Record

Independent review was rerun after every blocking repair. The final current-file
gates are:

| Perspective | Final P0 | Final P1 | Result |
|---|---:|---:|---|
| Developer/API | 0 | 0 | PASS |
| Operations/release | 0 | 0 | PASS |
| Caller ergonomics | 0 | 0 | PASS |
| Performance/allocation | 0 | 0 | PASS |
| Stability/compatibility | 0 | 0 | PASS |
| Security/resource boundaries | 0 | 0 | PASS |

All actionable P2 findings were integrated into this revision; none is silently
deferred. The review repaired Java null-literal ABI shape, fatal/overflow cleanup
precedence, Kryo wire order and release boundary, Avro semantic compatibility,
trusted-input limits, deterministic JMH statistics, exact-head squash evidence,
durable artifacts, and enforceable release/tag hold choreography.

## 16. Design DoD

- The five-PR stack and merge/rebase choreography are explicit.
- Existing ByteArray APIs and wire formats remain authoritative.
- Buffer state, overflow, failure, and ownership contracts are unambiguous.
- Native and fallback implementations are separated without false zero-copy
  claims.
- Security, registration, threading, tests, benchmark evidence, and docs are
  assigned to concrete stack slices.
- The six-perspective review passed with final `P0=0` and `P1=0`.
- Implementation planning remains blocked until this written design receives
  explicit user approval.
