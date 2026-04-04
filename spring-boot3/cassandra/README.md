# Module bluetape4k-spring-cassandra

English | [한국어](./README.ko.md)

`bluetape4k-spring-cassandra` provides Kotlin coroutine extensions, convenience DSLs, and schema utilities commonly needed for Spring Data Cassandra development.

## Key Features

- Coroutine extensions for `ReactiveSession`, `ReactiveCassandraOperations`, and `AsyncCassandraOperations`
- DSL helpers for CQL options (`QueryOptions`, `WriteOptions`, etc.)
- Schema creation and truncation utilities (`SchemaGenerator`)
- Test utilities and examples based on Calendar/Period

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-cassandra")
}
```

## Coroutine Extension Example

```kotlin
val result = reactiveSession.executeSuspending("SELECT * FROM users WHERE id = ?", id)
```

## Options DSL Example

```kotlin
val options = writeOptions {
    ttl(Duration.ofSeconds(30))
    timestamp(System.currentTimeMillis())
}
```

## Architecture Diagrams

### Core Class Diagram

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
    class AbstractReactiveCassandraCoroutineTest {
        +mongoOperations: ReactiveMongoOperations
        +runTest(block)
    }

    ReactiveCassandraOperationsExt --> WriteOptionsDsl : uses
    SchemaGenerator --> ReactiveCassandraOperationsExt : uses
    AbstractReactiveCassandraCoroutineTest --> ReactiveCassandraOperationsExt : tests
    ReactiveSessionExt --> ReactiveCassandraOperationsExt : complements
```

### Cassandra Data Access Layer

```mermaid
flowchart TD
    App["Application Code"] --> Ext["Coroutine Extension Functions<br/>(bluetape4k-spring-cassandra)"]
    Ext --> ROps["ReactiveCassandraOperations<br/>Coroutine Extensions"]
    Ext --> RSession["ReactiveSession<br/>Coroutine Extensions"]
    Ext --> AOps["AsyncCassandraOperations<br/>Coroutine Extensions"]
    DSL["WriteOptions / QueryOptions DSL<br/>writeOptions { ttl / timestamp }"] --> ROps
    ROps --> Driver["Cassandra Reactive Driver"]
    RSession --> Driver
    AOps --> Driver
    Driver --> Cassandra[("Apache Cassandra")]
    SchemaGen["SchemaGenerator<br/>Schema Creation / Truncation"] --> ROps
```

### Coroutine Conversion Flow

```mermaid
sequenceDiagram
    participant App as Application
    participant Ext as Coroutine Extension
    participant Ops as ReactiveCassandraOperations
    participant DB as Apache Cassandra

    App->>Ext: executeSuspending(cql, args)
    Ext->>Ops: execute(statement) → Mono/Flux
    Ops->>DB: Send CQL query
    DB-->>Ops: ReactiveResultSet
    Ops-->>Ext: Mono<ReactiveResultSet>
    Ext-->>App: Return suspend result (coroutine)
```

## Testing

```bash
./gradlew :bluetape4k-spring-cassandra:test
```
