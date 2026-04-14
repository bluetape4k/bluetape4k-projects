# Module bluetape4k-exposed-jackson

English | [한국어](./README.ko.md)

A module for serializing and deserializing Exposed JSON/JSONB columns using Jackson 2.

## Overview

`bluetape4k-exposed-jackson` provides serialization and deserialization of JetBrains Exposed JSON/JSONB column types using [Jackson 2.x](https://github.com/FasterXML/jackson). It supports JSON column types in PostgreSQL, H2, and other databases.

### Key Features

- **Jackson column types**: JSON/JSONB column mapping
- **Serializer support**: Common Jackson serializer configuration
- **JSON functions/conditions**: Helpers for building JSON query expressions
- **ResultRow extensions**: Utilities for reading JSON column values

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jackson:${version}")
    implementation("io.github.bluetape4k:bluetape4k-jackson2:${version}")
}
```

## Basic Usage

### 1. Defining a JSON Column

```kotlin
import io.bluetape4k.exposed.core.jackson.jackson
import org.jetbrains.exposed.v1.core.dao.id.IdTable

// Data class
data class UserSettings(
    val theme: String = "light",
    val notifications: Boolean = true,
    val language: String = "ko"
)

// Table definition
object Users: IdTable<Long>("users") {
    val name = varchar("name", 100)

    // JSON column
    val settings = jackson<UserSettings>("settings")
}
```

### 2. Defining a JSONB Column

```kotlin
import io.bluetape4k.exposed.core.jackson.jacksonb

object Products: IdTable<Long>("products") {
    val name = varchar("name", 255)

    // JSONB column (binary format)
    val metadata = jacksonb<ProductMetadata>("metadata")
}
```

### 3. Using a Custom Serializer

```kotlin
import io.bluetape4k.exposed.core.jackson.jackson
import io.bluetape4k.jackson.JacksonSerializer

// Define a custom serializer
val customSerializer = JacksonSerializer(
    jsonMapper {
        configure(SerializationFeature.INDENT_OUTPUT, false)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }
)

object Events: IdTable<Long>("events") {
    val payload = jackson<EventPayload>("payload", customSerializer)
}
```

### 4. Using JSON Functions

```kotlin
import io.bluetape4k.exposed.core.jackson.*

// JSON path query
val theme = Users.settings.jsonPath<String>("$.theme")

// JSON condition expression
val query = Users
    .selectAll()
    .where { Users.settings.jsonContains("theme", "dark") }
```

### 5. ResultRow Extensions

```kotlin
import io.bluetape4k.exposed.core.jackson.*

val settings: UserSettings = resultRow.getJackson(Users.settings)
val metadata: ProductMetadata? = resultRow.getJacksonOrNull(Products.metadata)
```

## Architecture Diagram

### Column Type Structure (Summary)

```mermaid
classDiagram
    direction LR
    class JacksonColumnType~T~ {
        <<ColumnType>>
        -objectMapper: ObjectMapper
        +valueFromDB(value): T
        +valueToDB(value): Any
    }
    class JacksonBColumnType~T~ {
        <<ColumnTypeJSONB>>
        -objectMapper: ObjectMapper
        +valueFromDB(value): T
        +valueToDB(value): PGobject
    }
    class TableExtensions {
        <<extensionFunctions>>
        +Table.jackson~T~(name): Column~T~
        +Table.jacksonb~T~(name): Column~T~
    }
    JacksonColumnType <|-- JacksonBColumnType
    TableExtensions --> JacksonColumnType : creates
    TableExtensions --> JacksonBColumnType : creates

    style JacksonColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style JacksonBColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style TableExtensions fill:#FFF3E0,stroke:#FFCC80,color:#E65100
```

## Class Diagram

```mermaid
classDiagram
    class ColumnType~T~ {
        <<Exposed>>
        +sqlType(): String
        +valueFromDB(value: Any): T?
        +notNullValueToDB(value: T): Any
    }
    class JsonColumnMarker {
        <<interfaceExposed>>
        +usesBinaryFormat: Boolean
        +needsBinaryFormatCast: Boolean
    }

    class JacksonColumnType~T~ {
        +serilaize: (T) -> String
        +deserialize: (String) -> T
        +sqlType(): String
        +valueFromDB(value: Any): T?
        +notNullValueToDB(value: T): Any
    }
    class JacksonBColumnType~T~ {
        +usesBinaryFormat: Boolean = true
        +sqlType(): String
        +parameterMarker(value: T?): String
    }

    class JacksonSerializer {
        <<bluetape4k_jackson>>
        +serializeAsString(value: T): String
        +deserializeFromString(json: String): T?
    }

    ColumnType <|-- JacksonColumnType
    JsonColumnMarker <|.. JacksonColumnType
    JacksonColumnType <|-- JacksonBColumnType
    JacksonColumnType ..> JacksonSerializer : uses

    style ColumnType fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style JsonColumnMarker fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    style JacksonColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style JacksonBColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style JacksonSerializer fill:#FFF3E0,stroke:#FFCC80,color:#E65100
```

## Serialization / Deserialization Sequence Diagrams

### Object → JSON → DB

```mermaid
sequenceDiagram
        participant App as Application
        participant Col as JacksonColumnType~T~
        participant Ser as JacksonSerializer
        participant DB as Database

    App->>Col: insert { it[settings] = UserSettings(theme="dark") }
    Col->>Ser: serializeAsString(UserSettings(...))
    Note over Ser: ObjectMapper.writeValueAsString()
    Ser-->>Col: '{"theme":"dark","notifications":true}'
    Col->>DB: INSERT ... VALUES ('{"theme":"dark",...}')
    Note over DB: Stored in JSON / JSONB column
```

### DB → JSON → Object

```mermaid
sequenceDiagram
        participant App as Application
        participant Col as JacksonColumnType~T~
        participant Ser as JacksonSerializer
        participant DB as Database

    App->>DB: SELECT settings FROM users WHERE id = 1
    DB-->>Col: '{"theme":"dark","notifications":true}'
    Col->>Ser: deserializeFromString~UserSettings~(json)
    Note over Ser: ObjectMapper.readValue()
    Ser-->>Col: UserSettings(theme="dark", notifications=true)
    Col-->>App: row[Users.settings]
```

## Key Files / Classes

| File                     | Description                          |
|--------------------------|--------------------------------------|
| `JacksonColumnType.kt`   | JSON column type (string-based)      |
| `JacksonBColumnType.kt`  | JSONB column type (binary format)    |
| `JacksonSerializer.kt`   | Jackson serializer configuration     |
| `JsonFunctions.kt`       | JSON function extensions             |
| `JsonConditions.kt`      | JSON condition expression extensions |
| `ResultRowExtensions.kt` | ResultRow JSON read extensions       |
| `ReadableExtensions.kt`  | Readable JSON read extensions        |

## Testing

```bash
./gradlew :bluetape4k-exposed-jackson:test
```

## References

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [Jackson 2.x](https://github.com/FasterXML/jackson)
- [PostgreSQL JSON Types](https://www.postgresql.org/docs/current/datatype-json.html)
