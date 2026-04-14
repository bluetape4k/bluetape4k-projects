# Module bluetape4k-exposed

English | [한국어](./README.ko.md)

A **backward-compatible umbrella module** that bundles `bluetape4k-exposed-core`, `bluetape4k-exposed-dao`, and
`bluetape4k-exposed-jdbc` together.

## Overview

Existing code that depends on the single `bluetape4k-exposed` module continues to work **without any changes
**. For new projects, we recommend referencing only the specific sub-modules you actually need.

```text
bluetape4k-exposed  (umbrella)
├── bluetape4k-exposed-core   ← Core column types and extension functions (no JDBC dependency)
├── bluetape4k-exposed-dao    ← DAO entities and ID table strategies
└── bluetape4k-exposed-jdbc   ← JDBC Repository, transactions, and query extensions
```

## Adding Dependencies

### Existing Code (no changes required)

```kotlin
dependencies {
    // Works exactly as before
    implementation("io.github.bluetape4k:bluetape4k-exposed:${version}")
}
```

### New Code (prefer minimal dependencies)

- R2DBC, Jackson, encrypted/compressed column types, etc. → `bluetape4k-exposed-core`
- DAO entities, custom IdTable (KSUID, etc.) → `bluetape4k-exposed-dao`
- JDBC Repository, queries, transactions → `bluetape4k-exposed-jdbc`
- Backward compatibility with existing code → `bluetape4k-exposed` (this module)

```kotlin
// Example: use only core in an R2DBC module
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-core:${version}")
}
```

```kotlin
// Example: when a JDBC Repository is needed
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-exposed-jdbc:${version}")
    // exposed-jdbc transitively includes core + dao
}
```

## Sub-Module Details

### bluetape4k-exposed-core

- Foundation module usable without a JDBC dependency
- Compressed (LZ4/Snappy/Zstd), encrypted, and serialized (Kryo/Fory) column types
- Client-side ID generation extensions (`timebasedGenerated`, `snowflakeGenerated`, `ksuidGenerated`)
- Common interfaces: `HasIdentifier<ID>`, `ExposedPage<T>`
- `BatchInsertOnConflictDoNothing`

See the `bluetape4k-exposed-core` module README for details.

### bluetape4k-exposed-dao

- DAO Entity helpers: `idEquals`, `idHashCode`, `entityToStringBuilder`
- `StringEntity` / `StringEntityClass` (String primary key support)
- Custom IdTables: `KsuidTable`, `KsuidMillisTable`, `SnowflakeIdTable`, `TimebasedUUIDTable`,
  `TimebasedUUIDBase62Table`, `SoftDeletedIdTable`

See the `bluetape4k-exposed-dao` module README for details.

### bluetape4k-exposed-jdbc

- `ExposedRepository<T, ID>` — CRUD, pagination, batch insert/upsert
- `SoftDeletedRepository<T, ID>` — Soft delete support
- `suspendedQuery { }` — Coroutines-based JDBC queries
- `virtualThreadTransaction { }` — JDK 21+ Virtual Thread transactions
- `SchemaUtilsExtensions`, `TableExtensions`, `ImplicitSelectAll`

See the `bluetape4k-exposed-jdbc` module README for details.

## Testing

```bash
# Test individual modules
./gradlew :bluetape4k-exposed-core:test
./gradlew :bluetape4k-exposed-dao:test
./gradlew :bluetape4k-exposed-jdbc:test
```

## Module Dependency Graph

```mermaid
flowchart TD
    E[exposed\numbrella] --> EC[exposed-core\nColumn Types + Auditable]
    E --> ED[exposed-dao\nDAO Entity + IdTable]
    E --> EJ[exposed-jdbc\nJDBC Repository\n+ VirtualThread]
    E --> ER[exposed-r2dbc\nR2DBC Repository\n+ Flow/suspend]

    EC --> EJK[exposed-jackson3\nJSONB Columns]
    EC --> EP[exposed-postgresql\nPostGIS + pgvector]
    EC --> EM[exposed-mysql8\nGIS Types]
    EJ --> EJL[exposed-jdbc-lettuce\nJDBC + Lettuce Cache]
    EJ --> EJR[exposed-jdbc-redisson\nJDBC + Redisson Cache]
    ER --> ERL[exposed-r2dbc-lettuce\nR2DBC + Lettuce Cache]
    ER --> ERR[exposed-r2dbc-redisson\nR2DBC + Redisson Cache]

    classDef umbrellaStyle fill:#37474F,stroke:#263238,color:#FFFFFF,font-weight:bold
    classDef coreStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef daoStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF
    classDef jdbcStyle fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    classDef extStyle fill:#2E7D32,stroke:#1B5E20,color:#FFFFFF
    classDef cacheStyle fill:#AD1457,stroke:#880E4F,color:#FFFFFF
    class E umbrellaStyle
    class EC coreStyle
    class ED daoStyle
    class EJ jdbcStyle
    class ER jdbcStyle
    class EJK extStyle
    class EP extStyle
    class EM extStyle
    class EJL cacheStyle
    class EJR cacheStyle
    class ERL cacheStyle
    class ERR cacheStyle
```

## Layered Execution Flow

```mermaid
sequenceDiagram
    box rgb(232,245,233) Application Layer
        participant App as Kotlin Application
    end
    box rgb(227,242,253) DSL Layer
        participant DSL as Exposed DSL
        participant Core as exposed-core
        participant DAO as exposed-dao
    end
    box rgb(243,229,245) Execution Layer
        participant JDBC as exposed-jdbc
        participant R2DBC as exposed-r2dbc
    end
    box rgb(255,243,224) Database Layer
        participant DB as Database
    end

    App->>DSL: Build query / define entity
    DSL->>Core: Column types, ID generation
    DSL->>DAO: DAO entity, IdTable strategy
    DSL->>JDBC: transaction { } / suspendedQuery
    DSL->>R2DBC: suspendTransaction / queryFlow
    JDBC->>DB: JDBC SQL execution
    R2DBC->>DB: Reactive SQL execution
    DB-->>JDBC: ResultSet
    DB-->>R2DBC: Publisher<Row>
    JDBC-->>App: Entity / ResultRow
    R2DBC-->>App: Flow<ResultRow>
```

## References

- [JetBrains Exposed](https://github.com/JetBrains/Exposed)
- bluetape4k-exposed-core
- bluetape4k-exposed-dao
- bluetape4k-exposed-jdbc
