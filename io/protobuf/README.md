# Module bluetape4k-protobuf

English | [한국어](./README.ko.md)

A Kotlin extension library for working with Google Protocol Buffers messages.

## Overview

`bluetape4k-protobuf` provides pure Protobuf utilities for message conversion, serialization, and type aliasing. Because it has no dependency on gRPC, it can be used as a lightweight addition to any module that only needs Protobuf message handling.

## Architecture

### Type Conversion Class Structure

![Type Conversion Class Structure 1](../../docs/images/readme-diagrams/io-protobuf-diagram-01.svg)

### Protobuf Type Conversion Flow

![Protobuf Type Conversion Flow 2](../../docs/images/readme-diagrams/io-protobuf-diagram-02.svg)

### Serialization Sequence

![Serialization Sequence 3](../../docs/images/readme-diagrams/io-protobuf-diagram-03.svg)

## Key Features

- **Type aliases**: `ProtoMessage`, `ProtoAny`, `ProtoTimestamp`, `ProtoDuration`, `ProtoMoney`, etc.
- **Timestamp conversion**: `Instant` ↔ `Timestamp`, RFC3339 parsing
- **Duration conversion**: Java `Duration` ↔ Protobuf `Duration`, comparison and arithmetic operators
- **DateTime conversion**: `LocalDate`/`LocalTime`/`LocalDateTime` ↔ Protobuf `Date`/`TimeOfDay`/`DateTime`
- **Money conversion**: JavaMoney ↔ Protobuf `Money`
- **Message utilities**: pack/unpack based on `Any`
- **Protobuf serializer**: `BinarySerializer` implementation (`ProtobufSerializer`) with allowlist-based security

### Security: ProtobufSerializer Allowlist

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

The fallback serializer for non-Protobuf objects was also changed from `Jdk` to `Kryo` to avoid JDK deserialization RCE risks.

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

val bytes = packMessage(myMessage)
val restored: MyMessage? = unpackMessage(bytes)
```

### 6. ProtobufSerializer (BinarySerializer Implementation)

```kotlin
import io.bluetape4k.protobuf.serializers.ProtobufSerializer

val serializer = ProtobufSerializer()
val bytes = serializer.serialize(protoMessage)
val message = serializer.deserialize<MyMessage>(bytes)
```

Recommended usage patterns:

- If all values are Protobuf messages, using `packMessage` / `unpackMessage` or each message's own
  `parseFrom` directly is the simplest approach.
- For stores that mix Protobuf messages with general JVM objects (e.g., caches, sessions, queues),
  `ProtobufSerializer` paired with a fallback serializer is more practical.
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
