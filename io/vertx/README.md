# Module bluetape4k-vertx

English | [한국어](./README.ko.md)

A unified module for async and Coroutines-based development with Vert.x.

> The former `vertx/core`, `vertx/sqlclient`, and `vertx/resilience4j` modules have been merged into this single module.

## What's Included

### Vert.x Core (formerly `vertx/core`)

- Vert.x Kotlin Coroutines extensions
- Verticle deployment and management utilities
- EventBus coroutine adapters
- Suspend support based on `vertx_lang_kotlin_coroutines`

### Vert.x SQL Client (formerly `vertx/sqlclient`)

- `vertx-sql-client` + `vertx-sql-client-templates` integration
- MySQL / PostgreSQL drivers included
- MyBatis Dynamic SQL integration
- JDBC client support (optional)
- Coroutines-based query execution

### Resilience4j Integration (formerly `vertx/resilience4j`)

- Vert.x + Resilience4j Circuit Breaker integration
- Resilience4j Micrometer metrics integration (optional)

## Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-vertx:${bluetape4kVersion}")
}
```

Optional runtime dependencies per service:

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-vertx:${bluetape4kVersion}")

    // For MySQL
    runtimeOnly(Libs.vertx_mysql_client)

    // For PostgreSQL
    runtimeOnly(Libs.vertx_pg_client)
}
```

## Dependency Structure

| Category                       | Scope            | Description              |
|--------------------------------|------------------|--------------------------|
| `vertx-core`                   | `api`            | Vert.x core              |
| `vertx-lang-kotlin`            | `api`            | Kotlin language support  |
| `vertx-lang-kotlin-coroutines` | `api`            | Coroutines support       |
| `vertx-sql-client`             | `api`            | SQL client abstraction   |
| `bluetape4k-resilience4j`      | `api`            | Resilience4j integration |
| `vertx-mysql-client`           | `implementation` | MySQL driver             |
| `vertx-pg-client`              | `implementation` | PostgreSQL driver        |
| `vertx-web`                    | `compileOnly`    | Optional web support     |
| `vertx-jdbc-client`            | `compileOnly`    | Optional JDBC            |

## Architecture Diagrams

### Module Dependency Structure

```mermaid
flowchart TD
    subgraph bluetape4k-vertx
        CORE[Vert.x Core<br/>vertx-core]
        KOTLIN[Vert.x Kotlin<br/>vertx-lang-kotlin]
        COROUTINES[Vert.x Coroutines<br/>vertx-lang-kotlin-coroutines]
        SQL[Vert.x SQL Client<br/>vertx-sql-client]
        R4J[bluetape4k-resilience4j]
    end

    subgraph OptionalRuntime["Optional Runtime"]
        MYSQL[vertx-mysql-client]
        PG[vertx-pg-client]
        WEB[vertx-web]
        JDBC[vertx-jdbc-client]
    end

    CORE --> KOTLIN --> COROUTINES
    CORE --> SQL
    COROUTINES --> SQL
    bluetape4k-vertx --> MYSQL
    bluetape4k-vertx --> PG
    bluetape4k-vertx -.->|compileOnly| WEB
    bluetape4k-vertx -.->|compileOnly| JDBC

    classDef coreStyle fill:#1B5E20,stroke:#1B5E20,color:#FFFFFF,font-weight:bold
    classDef asyncStyle fill:#6A1B9A,stroke:#6A1B9A,color:#FFFFFF
    classDef serviceStyle fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    classDef extStyle fill:#37474F,stroke:#37474F,color:#FFFFFF

    class CORE,KOTLIN coreStyle
    class COROUTINES asyncStyle
    class SQL,R4J serviceStyle
    class MYSQL,PG,WEB,JDBC extStyle
```

### Vert.x Event Loop + Coroutines Processing Flow

```mermaid
flowchart LR
    subgraph EventLoop["Vert.x Event Loop"]
        EL[Event Loop Thread]
        EB[EventBus]
        VER[Verticle<br/>CoroutineVerticle]
    end

    subgraph Coroutines["Kotlin Coroutines"]
        SC[suspend fun start]
        COR[CoroutineScope<br/>vertxDispatcher]
    end

    subgraph SQL_Client["SQL Client"]
        POOL[Connection Pool]
        QUERY[preparedQuery.execute]
        RS["RowSet&lt;Row&gt;"]
    end

    EL --> VER
    VER --> SC
    SC --> COR
    COR -->|await| EB
    COR -->|await| POOL
    POOL --> QUERY --> RS
    RS -->|await| COR

    classDef coreStyle fill:#1B5E20,stroke:#1B5E20,color:#FFFFFF,font-weight:bold
    classDef asyncStyle fill:#6A1B9A,stroke:#6A1B9A,color:#FFFFFF
    classDef serviceStyle fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    classDef dataStyle fill:#F57F17,stroke:#E65100,color:#000000

    class EL,EB coreStyle
    class VER,SC,COR asyncStyle
    class POOL,QUERY serviceStyle
    class RS dataStyle
```

### Circuit Breaker + Resilience4j Integration Flow

```mermaid
sequenceDiagram
    box rgb(237, 231, 246) Application
        participant App as Verticle (Coroutines)
    end
    box rgb(227, 242, 253) Resilience4j
        participant CB as CircuitBreaker
    end
    box rgb(232, 245, 233) Backend
        participant SVC as Remote Service
    end

    App->>CB: cb.executeSuspend { remoteCall() }
    CB->>CB: Check state (CLOSED/OPEN/HALF_OPEN)

    alt CLOSED (normal)
        CB->>SVC: Remote call
        SVC-->>CB: Response
        CB-->>App: Successful result
    else OPEN (blocked)
        CB-->>App: CallNotPermittedException
    else HALF_OPEN (probing)
        CB->>SVC: Test call
        SVC-->>CB: Success / failure
        CB->>CB: Transition state (CLOSED/OPEN)
        CB-->>App: Return result
    end
```

### Vert.x Core Component Class Structure

```mermaid
classDiagram
    class CoroutineVerticle {
        <<VertxKotlin>>
        +vertx: Vertx
        +context: Context
        +start()
        +stop()
    }

    class EventBus {
        +send(address, message)
        +publish(address, message)
        +consumer(address) MessageConsumer
    }

    class SqlClient {
        <<interface>>
        +preparedQuery(sql) PreparedQuery
        +query(sql) Query
        +close() Future
    }

    class Pool {
        +withConnection(handler) Future
        +withTransaction(handler) Future
    }

    class CircuitBreaker {
        <<Resilience4j>>
        +executeSuspend(block) T
        +getState() State
    }

    CoroutineVerticle --> EventBus : publish / consume events
    CoroutineVerticle --> Pool : SQL queries
    Pool --|> SqlClient
    CoroutineVerticle --> CircuitBreaker : fault isolation

    style CoroutineVerticle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style EventBus fill:#1B5E20,stroke:#1B5E20,color:#FFFFFF
    style SqlClient fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style Pool fill:#00897B,stroke:#00695C,color:#FFFFFF
    style CircuitBreaker fill:#37474F,stroke:#263238,color:#FFFFFF
```

## Usage Examples

### Verticle (Coroutines)

```kotlin
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class MainVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val server = vertx.createHttpServer()
        server.requestHandler { req ->
            req.response().end("Hello Vert.x!")
        }
        server.listen(8080).await()
    }
}
```

### SQL Client (Coroutines)

```kotlin
import io.vertx.sqlclient.Pool
import io.vertx.kotlin.coroutines.await

suspend fun findUser(pool: Pool, id: Long): RowSet<Row> {
    return pool.preparedQuery("SELECT * FROM users WHERE id = $1")
        .execute(Tuple.of(id))
        .await()
}
```

### Circuit Breaker + Resilience4j

```kotlin
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.bluetape4k.resilience4j.circuitbreaker.executeSuspend

val cb = CircuitBreaker.ofDefaults("vertx-service")

suspend fun callRemoteService(): String =
    cb.executeSuspend { remoteCall() }
```
