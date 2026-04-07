# Module bluetape4k-jackson

English | [한국어](./README.ko.md)

## Overview

`bluetape4k-jackson2` is a module that wraps the [Jackson 2.x](https://github.com/FasterXML/jackson) library with Kotlin DSL and extension functions.

It provides convenient access to the Jackson ecosystem in Kotlin, covering default `JsonMapper` configuration,
`ObjectMapper` extensions, async JSON parsing, UUID Base62 encoding, field-level encryption, and field masking.

## Architecture

### Class Structure

```mermaid
classDiagram
    class JsonSerializer {
        <<interface>>
        +serialize(graph) ByteArray
        +deserialize(bytes, clazz) T?
        +serializeAsString(graph) String
        +deserializeFromString(text, clazz) T?
    }

    class JacksonSerializer {
        -mapper: ObjectMapper
    }

    class Jackson {
        <<singleton>>
        +defaultJsonMapper: JsonMapper
        +prettyJsonWriter: ObjectWriter
    }

    class AsyncJsonParser {
        -callback: (JsonNode) -> Unit
        +consume(bytes: ByteArray)
    }

    class SuspendJsonParser {
        -callback: suspend (JsonNode) -> Unit
        +consume(flow: Flow~ByteArray~)
    }

    class JsonEncrypt {
        <<annotation>>
    }

    class JsonTinkEncrypt {
        <<annotation>>
        +algorithm: TinkEncryptAlgorithm
    }

    class JsonMasker {
        <<annotation>>
        +value: String
    }

    class JsonUuidEncoder {
        <<annotation>>
        +type: JsonUuidEncoderType
    }

    JsonSerializer <|.. JacksonSerializer
    JacksonSerializer --> Jackson : uses
    AsyncJsonParser --> SuspendJsonParser
```

### Jackson Serialization Pipeline

```mermaid
flowchart LR
    subgraph DataClass["Data Class"]
        OBJ[Kotlin Object]
        ANN["@JsonTinkEncrypt<br/>@JsonMasker<br/>@JsonUuidEncoder"]
    end

    subgraph ObjectMapper["ObjectMapper Processing"]
        SER[Serializer]
        DES[Deserializer]
        MOD[Jackson Module<br/>Registration]
    end

    subgraph OutputFormats["Output Formats"]
        JSON[JSON Text]
        CBOR[CBOR Binary]
        YAML[YAML]
        SMILE[Smile Binary]
        CSV_FMT[CSV]
    end

    OBJ --> ANN
    ANN --> SER
    MOD --> SER
    MOD --> DES
    SER --> JSON
    SER --> CBOR
    SER --> YAML
    SER --> SMILE
    SER --> CSV_FMT
    JSON --> DES --> OBJ
```

### Field Encryption Flow (@JsonTinkEncrypt)

```mermaid
sequenceDiagram
    participant App as Application
    participant M as ObjectMapper
    participant S as JsonTinkEncryptSerializer
    participant T as Google Tink AEAD

    Note over App,T: Serialization (Encryption)
    App->>M: writeValueAsString(user)
    M->>S: serialize(@JsonTinkEncrypt field)
    S->>T: AEAD.encrypt(plaintext)
    T-->>S: Base64 ciphertext
    S-->>M: Encrypted JSON field
    M-->>App: JSON string

    Note over App,T: Deserialization (Decryption)
    App->>M: readValue(json, User::class)
    M->>S: deserialize(@JsonTinkEncrypt field)
    S->>T: AEAD.decrypt(ciphertext)
    T-->>S: plaintext
    S-->>M: Decrypted value
    M-->>App: User object
```

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

### 5. UUID Base62 Encoding

Encodes UUIDs as Base62 strings for compact JSON storage.

### 6. Field Encryption (@JsonEncrypt / @JsonTinkEncrypt)

Automatically encrypts and decrypts sensitive fields during JSON serialization.

Supported algorithms for `@JsonTinkEncrypt`:

| `TinkEncryptAlgorithm`     | Description                                                            |
|----------------------------|------------------------------------------------------------------------|
| `AES256_GCM`               | AES256-GCM non-deterministic — general purpose, default                |
| `AES128_GCM`               | AES128-GCM non-deterministic — performance-focused                     |
| `CHACHA20_POLY1305`        | ChaCha20-Poly1305 — for environments without hardware AES acceleration |
| `XCHACHA20_POLY1305`       | XChaCha20-Poly1305 — large nonce (192-bit)                             |
| `DETERMINISTIC_AES256_SIV` | AES256-SIV deterministic — searchable in DB                            |

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

// Coroutine-based parsing
val suspendParser = SuspendJsonParser { root ->
    processNode(root)  // suspendable
}
suspendParser.consume(byteArrayFlow)
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
    @get:JsonTinkEncrypt(TinkEncryptAlgorithm.DETERMINISTIC_AES256_SIV) // deterministic encryption for DB search
    val mobile: String,
)

// Serialized: { "username": "debop", "password": "AXYzK1...", "mobile": "BVp0..." }
// Automatically decrypted on deserialization
```

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
│   ├── JsonEncrypt.kt                # @JsonEncrypt annotation (Jasypt, Deprecated)
│   ├── JsonEncryptSerializer.kt      # Encryption serializer
│   ├── JsonEncryptDeserializer.kt    # Decryption deserializer
│   ├── JsonEncryptors.kt             # Encryptor cache management
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
    implementation(project(":bluetape4k-jackson2"))

    // Binary formats (add only what you need)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")

    // Text formats (add only what you need)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml")

    // Encryption (optional)
    implementation(project(":bluetape4k-crypto"))  // for @JsonEncrypt (Jasypt)
    implementation(project(":bluetape4k-tink"))    // for @JsonTinkEncrypt (Google Tink)
}
```

## References

- [Jackson](https://github.com/FasterXML/jackson)
- [Jackson Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin)
- [Url62 (Base62)](https://github.com/nicksrandall/url62)
