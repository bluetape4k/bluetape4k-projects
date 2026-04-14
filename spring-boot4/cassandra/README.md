# Module bluetape4k-spring-boot4-cassandra

English | [한국어](./README.ko.md)

Provides coroutine extensions, convenience DSLs, and schema utilities for Spring Data Cassandra development (Spring Boot 4.x).

> Provides the same functionality as the Spring Boot 3 module (
`bluetape4k-spring-cassandra`), adapted to the Spring Boot 4.x API.

## Key Features

- Coroutine extensions for `ReactiveSession`, `ReactiveCassandraOperations`, and `AsyncCassandraOperations`
- DSL helpers for CQL options (`QueryOptions`, `WriteOptions`, etc.)
- Schema creation and truncation utilities (`SchemaGenerator`)

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot4-cassandra:${bluetape4kVersion}")
}
```

## Usage Examples

### Coroutine Extensions

```kotlin
val result = reactiveSession.executeSuspending("SELECT * FROM users WHERE id = ?", id)
```

### WriteOptions DSL

```kotlin
val options = writeOptions {
    ttl(Duration.ofSeconds(30))
    timestamp(System.currentTimeMillis())
}
```

### Entity Definition

```kotlin
@Table
data class User(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val email: String,
)
```

### Repository

```kotlin
interface UserRepository : CassandraRepository<User, UUID> {
    fun findByEmail(email: String): User?
}

// Coroutines Repository
interface CoroutineUserRepository : CoroutineCrudRepository<User, UUID> {
    suspend fun findByEmail(email: String): User?
}
```

## Build and Test

```bash
./gradlew :bluetape4k-spring-boot4-cassandra:test
```

## Architecture Diagrams

### Core Class Structure

```mermaid
classDiagram
    class ReactiveCassandraOperationsExt {
        <<extension>>
        +findOneOrNullSuspending(query): T?
        +findAllAsFlow(): Flow~T~
        +insertSuspending(entity): T
        +countSuspending(query): Long
        +existsSuspending(query): Boolean
        +updateMultiSuspending(query, update): UpdateResult
        +aggregateAsFlow(aggregation): Flow~O~
    }
    class ReactiveSessionExt {
        <<extension>>
        +executeSuspending(cql, args): ReactiveResultSet
    }
    class WriteOptionsDsl {
        <<DSL>>
        +ttl(duration)
        +timestamp(millis)
    }
    class SchemaGenerator {
        +createTables(operations, types)
        +truncateTables(operations, types)
    }
    class UserRepository {
        <<interface>>
        +findByEmail(email): User?
    }
    class CoroutineUserRepository {
        <<interface>>
        +findByEmail(email): User?
    }

    ReactiveCassandraOperationsExt --> WriteOptionsDsl : uses
    SchemaGenerator --> ReactiveCassandraOperationsExt : uses
    ReactiveSessionExt --> ReactiveCassandraOperationsExt : complements
    CoroutineUserRepository --> ReactiveCassandraOperationsExt : delegates

    style ReactiveCassandraOperationsExt fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style ReactiveSessionExt fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style WriteOptionsDsl fill:#F57F17,stroke:#E65100,color:#000000
    style SchemaGenerator fill:#E65100,stroke:#BF360C,color:#FFFFFF
    style UserRepository fill:#AD1457,stroke:#880E4F,color:#FFFFFF
    style CoroutineUserRepository fill:#AD1457,stroke:#880E4F,color:#FFFFFF
```

### Cassandra Data Access Layer

```mermaid
flowchart TD
    App["Application Code"] --> Ext["Coroutine Extension Functions<br/>(bluetape4k-spring-boot4-cassandra)"]
    Ext --> ROps["ReactiveCassandraOperations<br/>Coroutine Extensions"]
    Ext --> RSession["ReactiveSession<br/>Coroutine Extensions"]
    Ext --> AOps["AsyncCassandraOperations<br/>Coroutine Extensions"]
    DSL["WriteOptions / QueryOptions DSL<br/>writeOptions { ttl / timestamp }"] --> ROps
    ROps --> Driver["Cassandra Reactive Driver"]
    RSession --> Driver
    AOps --> Driver
    Driver --> Cassandra[("Apache Cassandra")]
    SchemaGen["SchemaGenerator<br/>Schema Creation / Truncation"] --> ROps

    classDef appStyle fill:#37474F,stroke:#263238,color:#FFFFFF
    classDef extStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef dslStyle fill:#F57F17,stroke:#E65100,color:#000000
    classDef driverStyle fill:#00897B,stroke:#00695C,color:#FFFFFF
    classDef dbStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF
    classDef utilStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF

    class App appStyle
    class Ext,ROps,RSession,AOps extStyle
    class DSL dslStyle
    class Driver driverStyle
    class Cassandra dbStyle
    class SchemaGen utilStyle
```

### Coroutine Conversion Sequence

```mermaid
sequenceDiagram
    box rgb(224,224,224) Application Layer
        participant App as Application
    end
    box rgb(225,190,231) Coroutine Layer
        participant Ext as Coroutine Extension
        participant Ops as ReactiveCassandraOperations
    end
    box rgb(224,224,224) Data Layer
        participant DB as Apache Cassandra
    end

    App->>Ext: executeSuspending(cql, args)
    Ext->>Ops: execute(statement) → Mono/Flux
    Ops->>DB: Send CQL query
    DB-->>Ops: ReactiveResultSet
    Ops-->>Ext: Mono<ReactiveResultSet>
    Ext-->>App: Return suspend result (coroutine)
```

## References

- [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra)
- [Apache Cassandra](https://cassandra.apache.org/)
