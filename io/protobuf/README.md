# Module bluetape4k-protobuf

English | [한국어](./README.ko.md)

A Kotlin extension library for working with Google Protocol Buffers messages.

## Overview

`bluetape4k-protobuf` provides pure Protobuf utilities for message conversion, serialization, and type aliasing. Because it has no dependency on gRPC, it can be used as a lightweight addition to any module that only needs Protobuf message handling.

## Architecture

### Protobuf Class Structure

![Protobuf Class Structure diagram](../../docs/images/readme-diagrams/io-protobuf-diagram-01.png)

### Protobuf Type Conversion Flow

![Protobuf Type Conversion Flow diagram](../../docs/images/readme-diagrams/io-protobuf-diagram-02.png)

### ProtobufSerializer Allowlist Sequence

![ProtobufSerializer Allowlist Sequence diagram](../../docs/images/readme-diagrams/io-protobuf-sequence-01.png)

## Key Features

- **Type aliases**: `ProtoMessage`, `ProtoAny`, `ProtoTimestamp`, `ProtoDuration`, `ProtoMoney`, etc.
- **Timestamp conversion**: `Instant` ↔ `Timestamp`, RFC3339 parsing
- **Duration conversion**: Java `Duration` ↔ Protobuf `Duration`, comparison and arithmetic operators
- **DateTime conversion**: `LocalDate`/`LocalTime`/`LocalDateTime` ↔ Protobuf `Date`/`TimeOfDay`/`DateTime`
- **Money conversion**: JavaMoney ↔ Protobuf `Money`
- **Message utilities**: pack/unpack based on `Any`
- **Protobuf serializer**: `BinarySerializer` implementation (`ProtobufSerializer`) with allowlist-based security

### Security: ProtobufSerializer Allowlist

Trust profile: `AllowListedTypes`.

`ProtobufSerializer` checks the `typeUrl` of each `Any` message against an allowlist before deserializing.
Classes whose prefix is not in the allowlist throw `SecurityException` (wrapped as `BinarySerializationException`).

**Default allowed prefixes** (`DEFAULT_ALLOWED_PREFIXES`):

| Prefix | Description |
|---|---|
| `io.bluetape4k.` | All bluetape4k domain messages |
| `com.google.protobuf.` | Standard Protobuf well-known types |

**Custom allowlist example:**

```kotlin
// Narrow: only allow specific packages
val narrowSerializer = ProtobufSerializer(
    allowedClassPrefixes = setOf("io.bluetape4k.", "com.mycompany.proto.")
)

// Expand: add extra packages alongside defaults
val expandedSerializer = ProtobufSerializer(
    allowedClassPrefixes = ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES + setOf("com.example.")
)
```

`ProtobufSerializer()` is strict by default: it accepts Protobuf `Message` values and rejects non-Protobuf values or
bytes. `ProtobufSerializer(fallback = nonNullSerializer)` and
`ProtobufSerializer.trustedInternalProtobuf(...)` enable a compatibility fallback only for stores where every producer
and stored payload is trusted. Do not enable either profile for untrusted payloads. A fallback never bypasses a
terminal allowlist violation or a resolved non-`Message` type.

`RedissonProtobufCodec()` and `RedissonProtobufCodec(allowedClassPrefixes)` are also strict. They do not use Kryo5 or
another fallback by default. `RedissonProtobufCodec(fallbackCodec)` and
`RedissonProtobufCodec.trustedInternal(...)` are explicit trusted-store-only fallback profiles. For a fully trusted
legacy deployment that temporarily needs allow-all Protobuf class loading, configure the migration escape hatch
explicitly:

```kotlin
val codec = RedissonProtobufCodec(
    allowedClassPrefixes = RedissonProtobufCodec.ALLOW_ALL_CLASSES_UNSAFE,
)
```

A trusted Redisson fallback decoder must consume its temporary input synchronously and return only an independent
object. Retaining the input, transferring it to another thread, or returning a derived `ByteBuf` view is unsupported.
If the decoded result must preserve those bytes, return an independently owned `ByteBuf.copy()` instead.

On decode, only a contiguous input that exposes exactly one NIO buffer (`nioBufferCount() == 1`) uses the lower-copy
path. Composite input remains on the copied compatibility path, and trusted fallback decoding remains isolated through
its own copied input. This is not a zero-copy guarantee.

`ALLOW_ALL_CLASSES_UNSAFE` changes only the Protobuf class allowlist. It does not activate a fallback codec, so
non-Protobuf values remain rejected by this constructor.

Prefer a narrow custom allowlist for production:

```kotlin
val codec = RedissonProtobufCodec(
    allowedClassPrefixes = ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES + setOf("com.example.proto.")
)
```

### Lettuce caller-owned ByteBuf encoding

`LettuceProtobufCodecs.protobuf()` keeps the strict default allowlist and writes an uncompressed Protobuf message
directly into Lettuce's caller-owned `ByteBuf`. `trustedInternalProtobuf()` uses the same target path but retains the
trusted Kryo fallback and must not be used across a shared or untrusted boundary.

```kotlin
val strictCodec = LettuceProtobufCodecs.protobuf<MyBluetapeMessage>()
val trustedLegacyCodec = LettuceProtobufCodecs.trustedInternalProtobuf<MyBluetapeMessage>()
```

For a package outside the default prefixes, construct the generic codec with an explicit serializer; this remains the
copied compatibility path:

```kotlin
val customCodec = LettuceBinaryCodec<MyMessage>(
    ProtobufSerializer(allowedClassPrefixes = setOf("com.mycompany.proto.")),
)
```

Compressed factories and the single-argument `ByteBuffer` encode/decode API are unchanged. A target write commits
`writerIndex` only after success. Failure can still leave capacity growth or attempted bytes behind, so the caller must
clear/reinitialize that range or discard the buffer. The measured heap/direct allocation reduction is recorded in the
[issue #757 report](../../docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md); it is not a zero-copy or throughput
guarantee. Java callers use `LettuceProtobufCodecs.INSTANCE.protobuf()`.

## Usage Examples

### 1. Type Aliases

```kotlin
import io.bluetape4k.protobuf.*

val message: ProtoMessage = myProtoMessage
val any: ProtoAny = ProtoAny.pack(message)
val empty: ProtoEmpty = PROTO_EMPTY
```

### 2. Timestamp Conversion

```kotlin
import io.bluetape4k.protobuf.*

val timestamp = Instant.now().toTimestamp()
val instant = timestamp.toInstant()
val fromRfc3339 = "2024-01-01T00:00:00Z".toTimestamp()
```

### 3. Duration Conversion

```kotlin
import io.bluetape4k.protobuf.*

val protoDuration = java.time.Duration.ofMinutes(5).toProtoDuration()
val javaDuration = protoDuration.toJavaDuration()

// Comparison and arithmetic
val sum = duration1 + duration2
val diff = duration1 - duration2
```

### 4. Money Conversion

```kotlin
import io.bluetape4k.protobuf.*
import org.javamoney.moneta.Money

val javaMoney = Money.of(10000, "KRW")
val protoMoney = javaMoney.toProtoMoney()
val backToJava = protoMoney.toJavaMoney()
```

### 5. Message Pack/Unpack

```kotlin
import io.bluetape4k.protobuf.*
import java.nio.ByteBuffer

val bytes = packMessage(myMessage)
val restored: MyMessage? = unpackMessage(bytes)

val target = ByteBuffer.allocateDirect(4096).apply { position(8) }
val written = packMessageTo(myMessage, target)
val source = target.duplicate().apply {
    position(8)
    limit(8 + written)
}
val decoded = unpackMessage<MyMessage>(source)
```

The caller-owned path is intended for an oversized buffer that is reused across calls. For tests or internal sizing
checks, the exact capacity can be calculated without adding a separate public size API:

```kotlin
val packed = com.google.protobuf.Any.pack(myMessage)
val exactTarget = ByteBuffer.allocate(packed.serializedSize)
packMessageTo(myMessage, exactTarget)
```

Production callers should normally keep the deliberately larger reusable buffer. Exact sizing constructs the packed
`Any` before the real write and gives up part of the intended allocation benefit. Existing `ByteArray` callers do not
need to migrate.

### 6. ProtobufSerializer (BinarySerializer Implementation)

```kotlin
import io.bluetape4k.protobuf.serializers.ProtobufSerializer
import java.nio.ByteBuffer

val serializer = ProtobufSerializer()
val bytes = serializer.serialize(protoMessage)
val message = serializer.deserialize<MyMessage>(bytes)

val target = ByteBuffer.allocateDirect(4096).apply { position(8) }
val written = serializer.serializeTo(protoMessage, target)
val source = target.duplicate().apply {
    position(8)
    limit(8 + written)
}
val decoded = serializer.deserializeFrom<MyMessage>(source)
```

`serializeTo` keeps the caller-owned encode optimization. `deserializeFrom` deliberately uses the inherited
`BinarySerializer` compatibility path: it copies only the bounded remaining bytes before decoding and preserves the
source position, limit, mark, and byte order for heap, direct, sliced, and read-only buffers.

The target remains caller-owned. A preflight `BufferOverflowException` does not change it, but a failure after writing
has started restores only `position`; bytes may already have been overwritten. Clear and reinitialize every
caller-owned prefix byte, or discard the buffer before reuse (`HEADER_SIZE` is the caller's prefix boundary):

```kotlin
try {
    serializer.serializeTo(message, target)
} catch (failure: Throwable) {
    target.clear()
    target.position(HEADER_SIZE)
    // Rewrite every caller-owned prefix byte before reuse, or discard target.
    throw failure
}
```

Recommended usage patterns:

- If all values are Protobuf messages, using `packMessage` / `unpackMessage` or each message's own
  `parseFrom` directly is the simplest approach.
- For trusted stores that mix Protobuf messages with historical JVM objects (e.g., internal caches or sessions), use
  an explicit trusted fallback profile only after confirming every producer and stored payload is trusted.
- Leave the service-to-service wire protocol to gRPC/Protobuf conventions, and use
  `ProtobufSerializer` at internal binary storage and delivery boundaries within the application.

## Key Files / Classes

| File                                | Description                                                                    |
|-------------------------------------|--------------------------------------------------------------------------------|
| `TypeAlias.kt`                      | Protobuf message type aliases (`ProtoMessage`, `ProtoAny`, `ProtoMoney`, etc.) |
| `TimestampSupport.kt`               | `Instant`/`Date` ↔ `Timestamp` conversion, RFC3339 parsing                     |
| `DurationSupport.kt`                | Java `Duration` ↔ Protobuf `Duration` conversion and operators                 |
| `DateTimeSupport.kt`                | `LocalDate`/`LocalTime`/`LocalDateTime` ↔ Protobuf date/time conversion        |
| `MoneySupport.kt`                   | JavaMoney ↔ Protobuf `Money` conversion                                        |
| `MessageSupport.kt`                 | `Any`-based message pack/unpack utilities                                      |
| `serializers/ProtobufSerializer.kt` | `BinarySerializer` implementation (Protobuf + fallback serialization)          |

## Dependencies

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-protobuf:${version}")
}
```

## Testing

```bash
./gradlew :bluetape4k-protobuf:test
```

## References

- [Protocol Buffers](https://protobuf.dev/)
- [Protobuf Kotlin](https://protobuf.dev/getting-started/kotlintutorial/)
