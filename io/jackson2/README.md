# Module bluetape4k-jackson

English | [한국어](./README.ko.md)

## Overview

`bluetape4k-jackson2` is a module that wraps the [Jackson 2.x](https://github.com/FasterXML/jackson) library with Kotlin DSL and extension functions.

It provides convenient access to the Jackson ecosystem in Kotlin, covering default `JsonMapper` configuration,
`ObjectMapper` extensions, async JSON parsing, UUID Base62 encoding, field-level encryption, and field masking.

## Why Jackson2 in bluetape4k

- Keeps the widely deployed Jackson 2.x API while adding Kotlin-first mapper defaults and extension functions.
- Provides one module for JSON, binary formats, UUID Base62 encoding, field masking, and Tink-backed field encryption.
- Exposes streaming parsers that work with callback-style byte chunks and coroutine `Flow<ByteArray>` pipelines.
- Uses explicit EOF APIs for non-blocking parsing so truncated final JSON fails fast instead of being treated as "more input may arrive later".

## Architecture

### Class Structure

![Class Structure diagram](../../docs/images/readme-diagrams/io-jackson2-diagram-01.png)

### Jackson Serialization Pipeline

![Jackson Serialization Pipeline diagram](../../docs/images/readme-diagrams/io-jackson2-diagram-02.png)

### Field Encryption Flow (@JsonTinkEncrypt)

![Field Encryption Flow (@JsonTinkEncrypt) diagram](../../docs/images/readme-diagrams/io-jackson2-sequence-01.png)

## Recommended Usage Scenarios

- Use `Jackson.defaultJsonMapper` when services need a shared Kotlin-ready `JsonMapper`.
- Use `JacksonSerializer` behind cache, messaging, or storage abstractions that depend on the common `JsonSerializer` contract.
- Use `AsyncJsonParser` for Netty, WebSocket, TCP, and listener callbacks that receive byte chunks one by one.
- Use `SuspendJsonParser.consumeComplete(flow)` for finite `Flow<ByteArray>` streams such as HTTP responses, file streams, and broker payloads.
- Use `@JsonTinkEncrypt` for field-level encryption where ciphertext can stay inside JSON documents.

## Anti-Patterns

- Do not omit `endOfInput()` or `consumeComplete(flow)` for a finite stream. Jackson's non-blocking parser needs an explicit EOF signal to report truncated JSON.
- Do not reuse a parser after `endOfInput()` or `consumeComplete(flow)`. Create a new parser for each logical stream.
- Do not use field encryption as a replacement for transport security, database access control, key rotation, or audit policy.
- Do not add every Jackson dataformat dependency by default. Add only the runtime formats the application actually reads or writes.

## Key Features

### 1. JsonMapper DSL

Build a `JsonMapper` concisely using Kotlin DSL.

### 2. JacksonSerializer

Implements the `JsonSerializer` interface backed by Jackson's `ObjectMapper`.

`JacksonSerializer` failure policy:

- `serialize(null)` returns an empty `ByteArray`.
- `deserialize(null)` / `deserializeFromString(null)` returns `null`.
- All other serialization / deserialization failures throw `JsonSerializationException`.

### 3. ObjectMapper Extension Functions

Extension functions for safe deserialization from various input sources — returns `null` instead of throwing on failure.

### 4. Async JSON Parsing

Streaming JSON parsing powered by Jackson's `NonBlockingJsonParser`.

When to use each parser:

- `AsyncJsonParser`: push-style code that receives
  `ByteArray` chunks via callbacks — Netty, WebSocket, TCP, message listeners, etc.
- `SuspendJsonParser`: `Flow<ByteArray>`-based pipelines where post-processing must be suspendable —
  `WebClient`, file streams, broker streams, etc.
- Both parsers handle multiple consecutive JSON roots and scalar JSON roots (`"text"`, `123`, `true`, `null`).
- Call `endOfInput()` for callback streams or `consumeComplete(flow)` for finite `Flow` streams when no more bytes will arrive.

### 5. UUID Base62 Encoding

Encodes UUIDs as Base62 strings for compact JSON storage.

### 6. Field Encryption (@JsonTinkEncrypt)

Automatically encrypts and decrypts sensitive fields during JSON serialization.

Supported algorithms for `@JsonTinkEncrypt`:

| `TinkEncryptAlgorithm`     | Description                                                            |
|----------------------------|------------------------------------------------------------------------|
| `AES256_GCM`               | AES256-GCM non-deterministic — general purpose, default                |
| `AES128_GCM`               | AES128-GCM non-deterministic — performance-focused                     |
| `CHACHA20_POLY1305`        | ChaCha20-Poly1305 — for environments without hardware AES acceleration |
| `XCHACHA20_POLY1305`       | XChaCha20-Poly1305 — large nonce (192-bit)                             |
| `DETERMINISTIC_AES256_SIV` | AES256-SIV deterministic — process-local equality only                 |

### 7. Field Masking (@JsonMasker)

Masks sensitive values during JSON serialization.

### 8. Binary / Text Format Support

| Format     | Type   | Runtime Dependency              |
|------------|--------|---------------------------------|
| CBOR       | Binary | `jackson-dataformat-cbor`       |
| Ion        | Binary | `jackson-dataformat-ion`        |
| Smile      | Binary | `jackson-dataformat-smile`      |
| Avro       | Binary | `jackson-dataformat-avro`       |
| Protobuf   | Binary | `jackson-dataformat-protobuf`   |
| YAML       | Text   | `jackson-dataformat-yaml`       |
| CSV        | Text   | `jackson-dataformat-csv`        |
| TOML       | Text   | `jackson-dataformat-toml`       |
| Properties | Text   | `jackson-dataformat-properties` |

## Usage Examples

### JsonMapper DSL

```kotlin
import io.bluetape4k.jackson.*

// DSL style
val mapper = jsonMapper {
    findAndAddModules()
    enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}

// Pre-configured JsonMapper (includes the Kotlin module)
val defaultMapper = Jackson.defaultJsonMapper

// Pretty-print output
val prettyJson = Jackson.prettyJsonWriter.writeValueAsString(data)
```

### Safe Polymorphic Mapper

Use `Jackson.createTypedJsonMapper(...)` when JSON must carry Jackson default type information.
The allowlist is enforced against polymorphic subtype class names, and type ids are written as the
`@class` property.

```kotlin
val mapper = Jackson.createTypedJsonMapper("com.example.model.")
val json = mapper.writeValueAsString(value)
val restored = mapper.readValue(json, ModelEnvelope::class.java)
```

Do not use the deprecated `Jackson.typedJsonMapper` with untrusted JSON. It keeps the legacy
`Any`-base default typing behavior for compatibility and is unsafe for external payloads.

### JacksonSerializer

```kotlin
import io.bluetape4k.jackson.JacksonSerializer

val serializer = JacksonSerializer()

// Byte array serialization / deserialization
val bytes = serializer.serialize(user)
val restored = serializer.deserialize<User>(bytes)

// String serialization / deserialization
val jsonText = serializer.serializeAsString(user)
val restored2 = serializer.deserializeFromString<User>(jsonText)

// Throws JsonSerializationException on failure
try {
    serializer.deserialize<User>("{not-json".toByteArray())
} catch (e: JsonSerializationException) {
    // handle
}
```

### ObjectMapper Extension Functions

```kotlin
import io.bluetape4k.jackson.*

val mapper = Jackson.defaultJsonMapper

// Deserialize from various sources (null on failure)
val user = mapper.readValueOrNull<User>(jsonString)
val user2 = mapper.readValueOrNull<User>(inputStream)
val user3 = mapper.readValueOrNull<User>(byteArray)
val user4 = mapper.readValueOrNull<User>(file)

// Object conversion
val dto = mapper.convertValueOrNull<UserDto>(entity)

// Serialization extensions
val json = mapper.writeAsString(user)
val bytes = mapper.writeAsBytes(user)
val prettyJson = mapper.prettyWriteAsString(user)
```

### Async JSON Parsing

```kotlin
import io.bluetape4k.jackson.async.*

// Callback-based async parsing
val parser = AsyncJsonParser { root ->
    println("Completed node: $root")
}
parser.consume(chunk1)
parser.consume(chunk2)
parser.endOfInput()

// Coroutine-based parsing
val suspendParser = SuspendJsonParser { root ->
    processNode(root)  // suspendable
}
suspendParser.consumeComplete(byteArrayFlow)
```

### UUID Base62 Encoding

```kotlin
import io.bluetape4k.jackson.uuid.JsonUuidEncoder
import io.bluetape4k.jackson.uuid.JsonUuidEncoderType

data class User(
    @field:JsonUuidEncoder                              // Base62 (default)
    val userId: UUID,
    @field:JsonUuidEncoder(JsonUuidEncoderType.PLAIN)   // original UUID
    val plainId: UUID,
)

// Serialized output:
// { "userId": "6gVuscij1cec8CelrpHU5h", "plainId": "413684f2-..." }
```

### Field Encryption (@JsonTinkEncrypt) — Recommended

```kotlin
import io.bluetape4k.jackson.crypto.JsonTinkEncrypt
import io.bluetape4k.jackson.crypto.TinkEncryptAlgorithm

data class User(
    val username: String,
    @get:JsonTinkEncrypt                                               // AES256-GCM (default)
    val password: String,
    @get:JsonTinkEncrypt(TinkEncryptAlgorithm.DETERMINISTIC_AES256_SIV) // deterministic within this JVM keyset
    val mobile: String,
)

// Serialized: { "username": "debop", "password": "AXYzK1...", "mobile": "BVp0..." }
// Automatically decrypted on deserialization
```

`@JsonTinkEncrypt` uses `TinkEncryptors` singleton instances, whose keysets are generated in memory for the
current JVM process. Do not use this annotation for durable encrypted database columns or searchable indexes that must
survive restart, rollout, or multi-instance access. For durable searchable storage, use `bluetape4k-tink` versioned
keyset APIs such as `TinkDaeads.versioned(store)` with a protected `VersionedKeysetStore`.

### Field Masking

```kotlin
import io.bluetape4k.jackson.mask.JsonMasker

data class User(
    val name: String,
    @field:JsonMasker("***")    // custom masking string
    val mobile: String,
)

// Serialized: { "name": "debop", "mobile": "***" }
```

### CBOR Serialization

```kotlin
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import com.fasterxml.jackson.databind.ObjectMapper

val cborMapper = ObjectMapper(CBORFactory())
val bytes = cborMapper.writeValueAsBytes(user)      // binary serialization
val restored = cborMapper.readValue<User>(bytes)    // deserialization
```

## Module Structure

```
io.bluetape4k.jackson
├── Jackson.kt                    # Default JsonMapper singleton
├── JacksonSerializer.kt          # JsonSerializer implementation
├── JsonMapperSupport.kt          # ObjectMapper extension functions
├── JsonNodeExtensions.kt         # JsonNode extension functions
├── JsonGeneratorExtensions.kt    # JsonGenerator extension functions
├── async/                        # Async JSON parsing
│   ├── AsyncJsonParser.kt        # Callback-based async parser
│   └── SuspendJsonParser.kt      # Coroutine-based parser
├── crypto/                           # Field encryption
│   ├── TinkEncryptAlgorithm.kt       # Tink algorithm enum
│   ├── JsonTinkEncrypt.kt            # @JsonTinkEncrypt annotation (Google Tink)
│   ├── JsonTinkEncryptSerializer.kt  # Tink encryption serializer
│   └── JsonTinkEncryptDeserializer.kt # Tink decryption deserializer
├── mask/                         # Field masking
│   ├── JsonMasker.kt             # @JsonMasker annotation
│   └── JsonMaskerSerializer.kt   # Masking serializer
└── uuid/                         # UUID encoding
    ├── JsonUuidEncoder.kt        # @JsonUuidEncoder annotation
    ├── JsonUuidEncoderType.kt    # BASE62 / PLAIN enum
    ├── JsonUuidModule.kt         # Jackson Module registration
    ├── JsonUuidBase62Serializer.kt   # UUID → Base62 serializer
    ├── JsonUuidBase62Deserializer.kt # Base62 → UUID deserializer
    └── JsonUuidEncoderAnnotationInterospector.kt
```

## Dependencies

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-jackson2:${bluetape4kVersion}")

    // Binary formats (add only what you need)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")

    // Text formats (add only what you need)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml")

    // Encryption (optional)
    implementation("io.github.bluetape4k:bluetape4k-tink:${bluetape4kVersion}")    // for @JsonTinkEncrypt (Google Tink)
}
```

## References

- [Jackson](https://github.com/FasterXML/jackson)
- [Jackson Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin)
- [Url62 (Base62)](https://github.com/nicksrandall/url62)
