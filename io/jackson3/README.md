# Module bluetape4k-jackson3

English | [한국어](./README.ko.md)

## Overview

`bluetape4k-jackson3` is a module that wraps the [Jackson 3.x](https://github.com/FasterXML/jackson) library with Kotlin DSL and extension functions.

It provides the same feature set as Jackson 2.x (
`bluetape4k-jackson2`), while following the new Jackson 3.x API and package structure (`tools.jackson.*`).

## Jackson 2.x vs 3.x

| Item             | Jackson 2.x                             | Jackson 3.x                               |
|------------------|-----------------------------------------|-------------------------------------------|
| Package          | `com.fasterxml.jackson.*`               | `tools.jackson.*`                         |
| Module           | bluetape4k-jackson2                     | bluetape4k-jackson3                       |
| Module SPI       | `com.fasterxml.jackson.databind.Module` | `tools.jackson.databind.JacksonModule`    |
| Type info        | `activateDefaultTyping()` supported     | Removed                                   |
| JsonMapper build | `JsonMapper.builder()`                  | `jsonMapper { }` (kotlin module built-in) |

## Key Features

### 1. JsonMapper DSL

```kotlin
import io.bluetape4k.jackson3.*

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

### 2. JacksonSerializer

```kotlin
import io.bluetape4k.jackson3.JacksonSerializer

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

`JacksonSerializer` failure policy:

- `serialize(null)` returns an empty `ByteArray`.
- `deserialize(null)` / `deserializeFromString(null)` returns `null`.
- All other serialization / deserialization failures throw `JsonSerializationException`.

### 3. ObjectMapper Extension Functions

```kotlin
import io.bluetape4k.jackson3.*

val mapper = Jackson.defaultJsonMapper

// Deserialize from various sources (null on failure)
val user = mapper.readValueOrNull<User>(jsonString)
val user2 = mapper.readValueOrNull<User>(inputStream)
val user3 = mapper.readValueOrNull<User>(byteArray)
val user4 = mapper.readValueOrNull<User>(file)
val user5 = mapper.readValueOrNull<User>(path)  // Path support

// Serialization extensions
val json = mapper.writeAsString(user)
val bytes = mapper.writeAsBytes(user)
val prettyJson = mapper.prettyWriteAsString(user)

// Retrieve registered module names
val moduleNames = mapper.registeredModuleNames()
```

### 4. Async JSON Parsing

```kotlin
import io.bluetape4k.jackson3.async.*

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

When to use each parser:

- `AsyncJsonParser`: push-style code that receives
  `ByteArray` chunks via callbacks — Netty, WebSocket, TCP, message listeners, etc.
- `SuspendJsonParser`: `Flow<ByteArray>`-based pipelines where post-processing must be suspendable —
  `WebClient`, file streams, broker streams, etc.
- Both parsers handle multiple consecutive JSON roots and scalar JSON roots (`"text"`, `123`, `true`, `null`).

### 4-1. WebClient Streaming Example

Consuming a `/stream/3` response from `HttpbinHttp2Server` via
`WebClient` and processing three root JSON objects sequentially.

```kotlin
import io.bluetape4k.jackson3.async.SuspendJsonParser
import io.bluetape4k.testcontainers.http.HttpbinHttp2Server
import kotlinx.coroutines.reactive.asFlow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.web.reactive.function.client.WebClient

val httpbin = HttpbinHttp2Server.Launcher.httpbinHttp2
val webClient = WebClient.builder()
    .baseUrl(httpbin.url)
    .build()

val parser = SuspendJsonParser { root ->
    println(root["url"].asText())   // process each JSON object from /stream/3
}

val chunkFlow = webClient.get()
    .uri("/stream/3")
    .retrieve()
    .bodyToFlux(DataBuffer::class.java)
    .map { buffer ->
        try {
            ByteArray(buffer.readableByteCount()).also { buffer.read(it) }
        } finally {
            DataBufferUtils.release(buffer)
        }
    }
    .asFlow()

parser.consume(chunkFlow)
```

If you are already receiving chunks via callbacks, `AsyncJsonParser` is more natural for the same scenario.

### 5. UUID Base62 Encoding

```kotlin
import io.bluetape4k.jackson3.uuid.JsonUuidEncoder
import io.bluetape4k.jackson3.uuid.JsonUuidEncoderType

data class User(
    @field:JsonUuidEncoder                              // Base62 (default)
    val userId: UUID,
    @field:JsonUuidEncoder(JsonUuidEncoderType.PLAIN)   // original UUID
    val plainId: UUID,
)
```

### 6. Field Encryption (@JsonEncrypt / @JsonTinkEncrypt)

#### Jasypt-based (`@JsonEncrypt`) — Deprecated

```kotlin
import io.bluetape4k.jackson3.crypto.JsonEncrypt
import io.bluetape4k.jackson3.crypto.JsonEncryptModule

data class User(
    val username: String,
    @get:JsonEncrypt          // AES encryption via Jasypt
    val password: String,
)

// JsonEncryptModule must be registered
val mapper = Jackson.createDefaultJsonMapper().rebuild()
    .addModule(JsonEncryptModule())
    .build()
```

#### Google Tink-based (`@JsonTinkEncrypt`) — Recommended

Requires the `bluetape4k-tink` dependency and explicit registration of `JsonTinkEncryptModule`.

```kotlin
import io.bluetape4k.jackson3.crypto.JsonTinkEncrypt
import io.bluetape4k.jackson3.crypto.JsonTinkEncryptModule
import io.bluetape4k.jackson3.crypto.TinkEncryptAlgorithm

data class User(
    val username: String,
    @get:JsonTinkEncrypt                                               // AES256-GCM (default)
    val password: String,
    @get:JsonTinkEncrypt(TinkEncryptAlgorithm.DETERMINISTIC_AES256_SIV) // deterministic encryption for DB search
    val mobile: String,
)

// JsonTinkEncryptModule must be registered
val mapper = Jackson.createDefaultJsonMapper().rebuild()
    .addModule(JsonTinkEncryptModule())
    .build()

// Serialized: { "username": "debop", "password": "AXYzK1...", "mobile": "BVp0..." }
// Automatically decrypted on deserialization
```

Supported algorithms:

| `TinkEncryptAlgorithm`     | Description                                                            |
|----------------------------|------------------------------------------------------------------------|
| `AES256_GCM`               | AES256-GCM non-deterministic — general purpose, default                |
| `AES128_GCM`               | AES128-GCM non-deterministic — performance-focused                     |
| `CHACHA20_POLY1305`        | ChaCha20-Poly1305 — for environments without hardware AES acceleration |
| `XCHACHA20_POLY1305`       | XChaCha20-Poly1305 — large nonce (192-bit)                             |
| `DETERMINISTIC_AES256_SIV` | AES256-SIV deterministic — searchable in DB                            |

### 7. Field Masking (@JsonMasker)

```kotlin
import io.bluetape4k.jackson3.mask.JsonMasker

data class User(
    val name: String,
    @field:JsonMasker("***")    // custom masking string
    val mobile: String,
)
```

In Jackson 3.x, `JsonMaskerAnnotationInterospector` is automatically registered via `JsonMaskerModule`.

### 8. JsonNode Extension Functions

```kotlin
import io.bluetape4k.jackson3.*

val objectNode = Jackson.defaultJsonMapper.createObjectNode()
objectNode.addString("name", "name")
objectNode.addInt(42, "age")
objectNode.addBoolean(true, "active")
objectNode.addNull("description")
```

## Binary / Text Format Support

> The former `bluetape4k-jackson3-binary` and `bluetape4k-jackson3-text` modules have been merged into this module.

Binary and text formats are declared as
`compileOnly` dependencies, so you must add the desired format's dependency at runtime.

| Format     | Type   | Runtime Dependency               |
|------------|--------|----------------------------------|
| CBOR       | Binary | `jackson3-dataformat-cbor`       |
| Ion        | Binary | `jackson3-dataformat-ion`        |
| Smile      | Binary | `jackson3-dataformat-smile`      |
| Avro       | Binary | `jackson3-dataformat-avro`       |
| Protobuf   | Binary | `jackson3-dataformat-protobuf`   |
| YAML       | Text   | `jackson3-dataformat-yaml`       |
| CSV        | Text   | `jackson3-dataformat-csv`        |
| TOML       | Text   | `jackson3-dataformat-toml`       |
| Properties | Text   | `jackson3-dataformat-properties` |

### CBOR Serialization Example

```kotlin
import tools.jackson.dataformat.cbor.CBORFactory
import tools.jackson.databind.ObjectMapper

val cborMapper = ObjectMapper(CBORFactory())
val bytes = cborMapper.writeValueAsBytes(user)      // binary serialization
val restored = cborMapper.readValue<User>(bytes)    // deserialization
```

### YAML Serialization Example

```kotlin
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.databind.ObjectMapper

val yamlMapper = ObjectMapper(YAMLFactory())
val yaml = yamlMapper.writeValueAsString(user)      // YAML serialization
val restored = yamlMapper.readValue<User>(yaml)     // deserialization
```

## Architecture Diagrams

### Jackson 2.x vs 3.x Module Comparison

```mermaid
flowchart LR
    subgraph JK2["bluetape4k-jackson2 (Jackson 2.x)"]
        M2[com.fasterxml.jackson.*]
        MOD2[Module SPI:<br/>com.fasterxml.jackson.databind.Module]
        TK2["@JsonTinkEncrypt<br/>→ auto-registered"]
    end

    subgraph JK3["bluetape4k-jackson3 (Jackson 3.x)"]
        M3[tools.jackson.*]
        MOD3[Module SPI:<br/>tools.jackson.databind.JacksonModule]
        TK3["@JsonTinkEncrypt<br/>→ manual JsonTinkEncryptModule registration"]
    end

    JK2 -->|same features, different package| JK3
```

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
        +createDefaultJsonMapper() JsonMapper
    }

    class JsonEncryptModule {
        +setupModule(context)
    }

    class JsonTinkEncryptModule {
        +setupModule(context)
    }

    class JsonMaskerModule {
        +setupModule(context)
    }

    class JsonUuidModule {
        +setupModule(context)
    }

    JsonSerializer <|.. JacksonSerializer
    JacksonSerializer --> Jackson : uses
    Jackson --> JsonEncryptModule : registers
    Jackson --> JsonTinkEncryptModule : registers
    Jackson --> JsonMaskerModule : registers
    Jackson --> JsonUuidModule : registers

```

### Jackson 3.x Module Registration Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant J as Jackson
    participant M as JsonMapper.Builder
    participant MOD as JacksonModules

    App->>J: Jackson.createDefaultJsonMapper()
    J->>M: jsonMapper { findAndAddModules() }
    M->>MOD: Register JsonTinkEncryptModule
    M->>MOD: Register JsonMaskerModule
    M->>MOD: Register JsonUuidModule
    MOD-->>M: Wire Introspector / Serializer / Deserializer
    M-->>J: Build JsonMapper
    J-->>App: Configured ObjectMapper

    App->>J: mapper.writeValueAsString(obj)
    J->>MOD: Detect @JsonTinkEncrypt fields
    MOD-->>J: Encrypted value
    J-->>App: JSON string
```

## Dependencies

```kotlin
dependencies {
    implementation(project(":bluetape4k-jackson3"))

    // Binary formats (add only what you need)
    implementation("tools.jackson.dataformat:jackson-dataformat-cbor3")
    implementation("tools.jackson.dataformat:jackson-dataformat-smile3")

    // Text formats (add only what you need)
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml3")
    implementation("tools.jackson.dataformat:jackson-dataformat-csv3")
    implementation("tools.jackson.dataformat:jackson-dataformat-toml3")

    // Encryption (optional)
    implementation(project(":bluetape4k-crypto"))  // for @JsonEncrypt (Jasypt)
    implementation(project(":bluetape4k-tink"))    // for @JsonTinkEncrypt (Google Tink)
}
```

## Module Structure

```
io.bluetape4k.jackson3
├── Jackson.kt                    # Default JsonMapper singleton
├── JacksonSerializer.kt          # JsonSerializer implementation
├── JsonMapperSupport.kt          # ObjectMapper extension functions
├── JsonNodeExtensions.kt         # JsonNode extension functions
├── JsonGeneratorExtensions.kt    # JsonGenerator extension functions
├── async/                        # Async JSON parsing
│   ├── AsyncJsonParser.kt        # Callback-based async parser
│   └── SuspendJsonParser.kt      # Coroutine-based parser
├── crypto/                                       # Field encryption
│   ├── JsonEncrypt.kt                            # @JsonEncrypt annotation (Jasypt, Deprecated)
│   ├── JsonEncryptModule.kt                      # Jasypt Module registration
│   ├── JsonEncryptAnnotationInterospector.kt     # Jasypt Introspector
│   ├── JsonEncryptSerializer.kt                  # Jasypt encryption serializer
│   ├── JsonEncryptDeserializer.kt                # Jasypt decryption deserializer
│   ├── JsonEncryptors.kt                         # Encryptor cache management
│   ├── TinkEncryptAlgorithm.kt                   # Tink algorithm enum
│   ├── JsonTinkEncrypt.kt                        # @JsonTinkEncrypt annotation (Google Tink)
│   ├── JsonTinkEncryptModule.kt                  # Tink Module registration
│   ├── JsonTinkEncryptAnnotationIntrospector.kt  # Tink Introspector
│   ├── JsonTinkEncryptSerializer.kt              # Tink encryption serializer
│   └── JsonTinkEncryptDeserializer.kt            # Tink decryption deserializer
├── mask/                         # Field masking
│   ├── JsonMasker.kt             # @JsonMasker annotation
│   ├── JsonMaskerModule.kt       # Jackson 3.x Module registration
│   ├── JsonMaskerAnnotationInterospector.kt
│   └── JsonMaskerSerializer.kt   # Masking serializer
└── uuid/                         # UUID encoding
    ├── JsonUuidEncoder.kt        # @JsonUuidEncoder annotation
    ├── JsonUuidEncoderType.kt    # BASE62 / PLAIN enum
    ├── JsonUuidModule.kt         # Jackson 3.x Module registration
    ├── JsonUuidBase62Serializer.kt   # UUID → Base62 serializer
    ├── JsonUuidBase62Deserializer.kt # Base62 → UUID deserializer
    └── JsonUuidEncoderAnnotationInterospector.kt
```

## Testing

```bash
./gradlew :bluetape4k-jackson3:test
```

## References

- [Jackson 3.x](https://github.com/FasterXML/jackson)
- [Jackson 3.x Release Notes](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [Jackson Kotlin Module](https://github.com/FasterXML/jackson-module-kotlin)
