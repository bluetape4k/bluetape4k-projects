# Module bluetape4k-exposed-fastjson2

English | [한국어](./README.ko.md)

A module for serializing and deserializing Exposed JSON/JSONB columns using Fastjson2.

## Overview

`bluetape4k-exposed-fastjson2` provides serialization and deserialization of JetBrains Exposed JSON/JSONB column types using [Alibaba Fastjson2](https://github.com/alibaba/fastjson2). It is well-suited for environments that require high-performance JSON processing.

### Key Features

- **Fastjson column types**: JSON/JSONB column mapping
- **ResultRow extensions**: Utilities for reading JSON column values
- **JSON functions/conditions**: Helpers for building database-specific JSON query conditions

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-fastjson2:${version}")
    implementation("io.github.bluetape4k:bluetape4k-fastjson2:${version}")
}
```

## Basic Usage

### 1. Defining JSON Columns

```kotlin
import io.bluetape4k.exposed.core.fastjson2.fastjson
import io.bluetape4k.exposed.core.fastjson2.fastjsonb
import org.jetbrains.exposed.v1.core.dao.id.IdTable

// Data class
data class ProductMetadata(
    val brand: String = "",
    val tags: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap()
)

// Table definition
object Products: IdTable<Long>("products") {
    val name = varchar("name", 255)

    // JSON column (string-based)
    val metadata = fastjson<ProductMetadata>("metadata")

    // JSONB column (binary format, PostgreSQL)
    val extraData = fastjsonb<Map<String, Any>>("extra_data")
}
```

### 2. Using JSON Columns

```kotlin
// Insert
Products.insert {
    it[name] = "Product A"
    it[metadata] = ProductMetadata(
        brand = "BrandX",
        tags = listOf("electronics", "sale"),
        attributes = mapOf("color" to "red")
    )
}

// Query
val product = Products.selectAll().where { Products.id eq 1L }.single()
val metadata: ProductMetadata = product[Products.metadata]
val tags = metadata.tags  // ["electronics", "sale"]
```

### 3. JSON Condition Expressions

```kotlin
import io.bluetape4k.exposed.core.fastjson2.*

// Search by JSON path
val query = Products.selectAll()
    .where { Products.metadata.jsonPath<String>("$.brand") eq "BrandX" }

// Search by JSON containment
val query2 = Products.selectAll()
    .where { Products.metadata.jsonContains("tags", "sale") }
```

### 4. ResultRow Extensions

```kotlin
import io.bluetape4k.exposed.core.fastjson2.*

val metadata: ProductMetadata = resultRow.getFastjson(Products.metadata)
val extraData: Map<String, Any>? = resultRow.getFastjsonOrNull(Products.extraData)
```

## Key Files / Classes

| File                     | Description                          |
|--------------------------|--------------------------------------|
| `FastjsonColumnType.kt`  | JSON column type (string-based)      |
| `FastjsonBColumnType.kt` | JSONB column type (binary format)    |
| `JsonFunctions.kt`       | JSON function extensions             |
| `JsonConditions.kt`      | JSON condition expression extensions |
| `ResultRowExtensions.kt` | ResultRow JSON read extensions       |

## Jackson vs Fastjson2 Selection Guide

| Feature         | Jackson     | Fastjson2                  |
|-----------------|-------------|----------------------------|
| Performance     | Good        | Very fast                  |
| Stability       | High        | Moderate                   |
| Features        | Rich        | Basic                      |
| Recommended for | General use | High-performance scenarios |

## Testing

```bash
./gradlew :bluetape4k-exposed-fastjson2:test
```

## Architecture Diagram

### Column Type Structure (Summary)

```mermaid
classDiagram
    direction LR
    class Fastjson2ColumnType~T~ {
        <<ColumnType>>
        +valueFromDB(value): T
        +valueToDB(value): Any
    }
    class Fastjson2BColumnType~T~ {
        <<ColumnTypeJSONB>>
        +valueToDB(value): PGobject
    }
    class TableExtensions {
        <<extensionFunctions>>
        +Table.fastjson2~T~(name): Column~T~
        +Table.fastjson2b~T~(name): Column~T~
    }
    Fastjson2ColumnType <|-- Fastjson2BColumnType

    style Fastjson2ColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style Fastjson2BColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style TableExtensions fill:#FFF3E0,stroke:#FFCC80,color:#E65100
```

### JSON Column Type Class Structure

```mermaid
classDiagram
    class ColumnType {
        <<Exposed base abstract>>
        +sqlType(): String
        +valueFromDB(value): T
        +notNullValueToDB(value): Any
    }
    class FastjsonColumnType~T~ {
        -objectMapper: ObjectMapper
        +sqlType(): String
        +valueFromDB(value): T
        +notNullValueToDB(value): String
    }
    class FastjsonBColumnType~T~ {
        -objectMapper: ObjectMapper
        +sqlType(): String
        +valueFromDB(value): T
        +notNullValueToDB(value): ByteArray
    }
    class JsonFunctions {
        +Column.jsonPath(path): Expression
        +Column.jsonContains(field, value): Op
    }
    class ResultRowExtensions {
        +ResultRow.getFastjson(col): T
        +ResultRow.getFastjsonOrNull(col): T?
    }

    ColumnType <|-- FastjsonColumnType
    ColumnType <|-- FastjsonBColumnType
    FastjsonColumnType --> JsonFunctions : integrates
    FastjsonBColumnType --> JsonFunctions : integrates
    ResultRowExtensions --> FastjsonColumnType : uses
    ResultRowExtensions --> FastjsonBColumnType : uses

    style ColumnType fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    style FastjsonColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style FastjsonBColumnType fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style JsonFunctions fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style ResultRowExtensions fill:#FFFDE7,stroke:#FFF176,color:#F57F17
```

### JSON Column Data Flow

```mermaid
flowchart LR
    A[Kotlin Object] -->|Fastjson2 serialize| B[JSON String / ByteArray]
    B -->|Store to DB| C[(Database)]
    C -->|Read from DB| D[JSON String / ByteArray]
    D -->|Fastjson2 deserialize| E[Kotlin Object]

    subgraph JSON Column Types
        F["fastjson~T~ → TEXT"]
        G["fastjsonb~T~ → JSONB/BLOB"]
    end

    classDef objectStyle fill:#ECEFF1,stroke:#B0BEC5,color:#37474F
    classDef processStyle fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    classDef dbStyle fill:#FFFDE7,stroke:#FFF176,color:#F57F17

    class A objectStyle
    class E objectStyle
    class B processStyle
    class D processStyle
    class C dbStyle
```

## References

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- [Fastjson2](https://github.com/alibaba/fastjson2)
- bluetape4k-fastjson2
