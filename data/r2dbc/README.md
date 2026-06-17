# Module bluetape4k-r2dbc

English | [한국어](./README.ko.md)

A library that supports reactive data access using Coroutines and Flow in an R2DBC (Reactive Relational Database Connectivity) environment.

## Features

- **Kotlin Coroutines/Flow Support**: Converts R2DBC Reactive streams to Kotlin Flow
- **DatabaseClient Extensions**: Parameter binding and SQL execution helpers
- **Query Builder**: Convenient builder for composing dynamic queries
- **Transaction Support**: R2DBC transaction management
- **Spring Boot Auto Configuration**: Automatic configuration in a Spring environment

## Architecture Diagrams

### Extension Function API Overview

![Extension Function API Overview diagram](../../docs/images/readme-diagrams/data-r2dbc-diagram-01.png)

### Core API Class Structure

![Core API Class Structure diagram](../../docs/images/readme-diagrams/data-r2dbc-diagram-02.png)

### R2DBC Query Execution Flow

![R2DBC Query Execution Flow diagram](../../docs/images/readme-diagrams/data-r2dbc-sequence-01.png)

### JDBC vs R2DBC Comparison

![JDBC vs R2DBC Comparison diagram](../../docs/images/readme-diagrams/data-r2dbc-diagram-03.png)

## Dependency

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-r2dbc:${version}")
}
```

## Core Features

### 1. R2DBC Connection Pool Tuning

`R2dbcPoolConfig` provides a high-throughput preset and first-class access to the r2dbc-pool options that usually matter under load: pool size, warmup size, pending acquire queue, acquisition timeout, creation timeout, validation timeout, validation depth, optional validation query, pool name, and JMX registration.

```kotlin
import io.bluetape4k.r2dbc.pool.R2dbcPoolConfig
import io.bluetape4k.r2dbc.pool.r2dbcConnectionPool
import java.time.Duration

val pool = r2dbcConnectionPool("r2dbc:postgresql://user:secret@localhost:5432/app") {
    val preset = R2dbcPoolConfig.highThroughput(
        maxSize = 128,
        poolName = "app-r2dbc",
    )

    maxSize = preset.maxSize
    initialSize = preset.initialSize
    minIdle = preset.minIdle
    acquireRetry = preset.acquireRetry
    maxPendingAcquire = preset.maxPendingAcquire
    maxAcquireTime = Duration.ofSeconds(3)
    maxCreateConnectionTime = preset.maxCreateConnectionTime
    maxValidationTime = preset.maxValidationTime
    validationDepth = preset.validationDepth
    validationQuery = null // avoid an extra SQL round trip on every acquire
    poolName = preset.poolName
}
```

Use `maxSize` with the database server's connection limit and total application
instance count in mind. For latency-sensitive services, prefer a bounded
`maxAcquireTime` and `maxPendingAcquire` so overload fails quickly instead of
building an unbounded queue.

Run the pool benchmarks with:

```bash
./gradlew :bluetape4k-r2dbc:benchmarkPoolConfig
./gradlew :bluetape4k-r2dbc:benchmarkH2PoolAcquire
./gradlew :bluetape4k-r2dbc:benchmarkPostgresPoolAcquire
./gradlew :bluetape4k-r2dbc:benchmarkMysql8PoolAcquire
./gradlew :bluetape4k-r2dbc:benchmarkH2PoolContention
```

Run PostgreSQL and MySQL benchmark tasks sequentially because they use
Testcontainers-backed databases.

Recent local acquire benchmark (`8` JMH threads, `3` measurement iterations,
`validationQuery = "SELECT 1"`) showed that pure acquire/close throughput varies by driver, while realistic connection hold time makes default and high-throughput profiles converge:

| Database                    | Hold time | Default       | High-throughput |
|-----------------------------|-----------|---------------|-----------------|
| H2                          | 0 ms      | 100,200 ops/s | 95,423 ops/s    |
| H2                          | 1 ms      | 6,921 ops/s   | 6,906 ops/s     |
| H2                          | 5 ms      | 1,430 ops/s   | 1,439 ops/s     |
| PostgreSQL 18 Testcontainer | 0 ms      | 16,571 ops/s  | 16,960 ops/s    |
| PostgreSQL 18 Testcontainer | 1 ms      | 4,271 ops/s   | 4,695 ops/s     |
| PostgreSQL 18 Testcontainer | 5 ms      | 1,050 ops/s   | 1,066 ops/s     |
| MySQL 8.4 Testcontainer     | 0 ms      | 9,007 ops/s   | 8,251 ops/s     |
| MySQL 8.4 Testcontainer     | 1 ms      | 4,266 ops/s   | 4,279 ops/s     |
| MySQL 8.4 Testcontainer     | 5 ms      | 918 ops/s     | 960 ops/s       |

![R2DBC Pool Acquire Throughput chart](../../docs/images/readme-charts/data-r2dbc-pool-acquire-throughput-chart-01.png)

The contention benchmark uses `64` JMH threads with `maxSize` below concurrency.
Default uses an unbounded pending queue in this benchmark; high-throughput uses
bounded pending acquire plus a `250 ms` acquire timeout so overload is visible as
fast rejection. The JMH score is operations per second, so read it together with
the acquired/failed trial counts.

| Hold time | maxSize | Default ops/s | Default acquired/failed | High-throughput ops/s | High-throughput acquired/failed |
|-----------|--------:|--------------:|-------------------------:|----------------------:|--------------------------------:|
| 10 ms     | 4       | 360 ops/s     | 1,885 / 0                | 38,342 ops/s          | 1,508 / 150,669                |
| 10 ms     | 8       | 733 ops/s     | 3,321 / 0                | 21,530 ops/s          | 3,043 / 82,978                 |
| 10 ms     | 16      | 1,476 ops/s   | 6,173 / 0                | 1,477 ops/s           | 6,195 / 0                      |
| 50 ms     | 4       | 76 ops/s      | 796 / 0                  | 37,763 ops/s          | 386 / 150,891                  |
| 50 ms     | 8       | 155 ops/s     | 1,092 / 0                | 20,810 ops/s          | 775 / 82,893                   |
| 50 ms     | 16      | 313 ops/s     | 1,676 / 0                | 310 ops/s             | 1,676 / 0                      |

![R2DBC Pool Contention Throughput chart](../../docs/images/readme-charts/data-r2dbc-pool-contention-throughput-chart-01.png)

#### Tuning guide from the measurement

- For pure acquire/close paths (`0 ms` hold), compare with your actual driver. H2/PostgreSQL slightly favored the high-throughput preset in this run, while MySQL 8 favored the default profile. This path is mostly a driver/pool overhead microbenchmark and should not drive server defaults by itself.
- Use `R2dbcPoolConfig.highThroughput()` for server workloads where each request holds a connection while running SQL or a transaction. At `1 ms` and `5 ms` hold time, throughput was dominated by the hold time and the two profiles were effectively equivalent across H2, PostgreSQL, and MySQL 8, so the high-throughput preset's bounded queue and warmup behavior become the more important operational property.
- Increase `maxSize` only when concurrent requests exceed the pool size and the database can handle the additional sessions. Under `64` contending threads, throughput scaled almost linearly with `maxSize` on the default unbounded queue path because the benchmark was connection-slot limited.
- Treat high-throughput contention rows with large failure counts as overload evidence, not as successful SQL throughput. A bounded pending queue exposes backpressure quickly; raise `maxSize`, reduce hold time, shed traffic, or increase the pending/acquire timeout budget only when the database can absorb the extra work.
- Long-running queries and transactions reduce the useful throughput ceiling to roughly
  `maxSize / connection hold time`. In the contention benchmark, moving from `10 ms` to
  `50 ms` hold time reduced throughput by about `5x` for the same `maxSize`.
- Size `maxSize` from the database side first:
  `floor((db max_connections - reserved admin/replication connections) / application instance count)`, then lower it if p95/p99 database latency rises under load.
- Keep `initialSize` and `minIdle` below or equal to `maxSize`. The high-throughput preset warms up
  `min(maxSize, max(availableProcessors * 2, 16))` connections so cold starts do not pay the full allocation cost on the first traffic spike.
- Keep `maxPendingAcquire` bounded for user-facing services. The preset uses
  `maxSize * 4`, which keeps short bursts queued without allowing an unbounded backlog that hides overload and increases tail latency.
- If `maxPendingAcquire` is too low, r2dbc-pool rejects extra acquire attempts once the pool and pending queue are full. This is useful for fail-fast overload control, but it should be paired with application metrics for acquire failures/timeouts.
- Keep `maxAcquireTime` finite.
  `2-3s` is a reasonable starting point for API services; batch jobs can use a longer timeout if waiting is preferable to failing.
- Prefer `ValidationDepth.LOCAL` and no
  `validationQuery` in production drivers that support local validation. The benchmark used
  `SELECT 1` to keep H2/PostgreSQL/MySQL validation behavior consistent; a SQL validation query adds one database round trip to every connection acquisition.
- Treat benchmark numbers as local baselines, not universal limits. Re-run the DB-specific pool acquire benchmark against your driver/database shape when query latency, transaction duration, instance count, or DB connection limits change.

### 2. Executing SQL with DatabaseClient

```kotlin
import io.bluetape4k.r2dbc.support.*
import kotlinx.coroutines.flow.toList

// Execute a SELECT query
val users = databaseClient
    .sql("SELECT * FROM users WHERE active = :active")
    .bind("active", true)
    .fetch()
    .flow { row, _ ->
        User(
            id = row.get("id") as Int,
            name = row.get("name") as String,
            email = row.get("email") as String
        )
    }
    .toList()

// Fetch a single result
val user = databaseClient
    .sql("SELECT * FROM users WHERE id = :id")
    .bind("id", 1)
    .fetch()
    .awaitSingle { row, _ ->
        User(
            id = row.get("id") as Int,
            name = row.get("name") as String
        )
    }

// Fetch result as a Map
val userMap = databaseClient
    .sql("SELECT * FROM users WHERE id = :id")
    .bind("id", 1)
    .fetch()
    .awaitSingleAsMap()
```

### 3. Parameter Binding

```kotlin
// Bind parameters from a Map
val parameters = mapOf(
    "username" to "john",
    "active" to true
)

val users = databaseClient
    .sql("SELECT * FROM users WHERE username = :username AND active = :active")
    .bindMap(parameters)
    .fetch()
    .flow { row, _ -> /* mapping */ }

// Index-based parameter binding
val indexedParams = mapOf(
    1 to "john",
    2 to true
)

val users = databaseClient
    .sql("SELECT * FROM users WHERE username = ? AND active = ?")
    .bindIndexedMap(indexedParams)
    .fetch()
    .flow { row, _ -> /* mapping */ }
```

### 4. CRUD Operations

```kotlin
// INSERT and return generated key
val generatedId = databaseClient
    .sqlInsert("INSERT INTO users (name, email) VALUES (:name, :email)")
    .bind("name", "John Doe")
    .bind("email", "john@example.com")
    .fetch()
    .awaitGeneratedKey()

// UPDATE
val affectedRows = databaseClient
    .sqlUpdate("UPDATE users SET name = :name WHERE id = :id")
    .bind("name", "Jane Doe")
    .bind("id", 1)
    .fetch()
    .awaitRowsUpdated()

// DELETE
val deletedRows = databaseClient
    .sqlDelete("DELETE FROM users WHERE id = :id")
    .bind("id", 1)
    .fetch()
    .awaitRowsUpdated()
```

### 5. Flow and Coroutine Support

```kotlin
import kotlinx.coroutines.flow.*

// Collect results as a Flow
val userFlow: Flow<User> = databaseClient
    .sql("SELECT * FROM users")
    .fetch()
    .flow { row, metadata ->
        User(
            id = row.get("id") as Int,
            name = row.get("name") as String
        )
    }

// Transform the Flow
val names = userFlow
    .map { it.name }
    .filter { it.startsWith("A") }
    .toList()

// Collect into a List
val users = databaseClient
    .sql("SELECT * FROM users")
    .fetch()
    .awaitList { row, _ -> /* mapping */ }
```

### 6. Transaction Management

```kotlin
import io.bluetape4k.r2dbc.support.withTransactionSuspend

// Execute within a transaction
databaseClient.withTransactionSuspend { tx ->
    databaseClient
        .sql("INSERT INTO accounts (user_id, balance) VALUES (:userId, :balance)")
        .bind("userId", 1)
        .bind("balance", 1000)
        .fetch()
        .awaitRowsUpdated()

    databaseClient
        .sql("INSERT INTO logs (message) VALUES (:message)")
        .bind("message", "Account created")
        .fetch()
        .awaitRowsUpdated()

    "success"
}
```

### 7. Query Builder

```kotlin
import io.bluetape4k.r2dbc.query.QueryBuilder

// Compose a dynamic query
val query = QueryBuilder().build {
    select("SELECT * FROM users")
    parameter("active", true)
    whereGroup("and") {
        where("username LIKE :pattern")
        where("created_at > :date")
    }
    orderBy("created_at DESC")
    limit(10)
}

// Execute the query
val users = databaseClient
    .sql(query.sql)
    .bindMap(query.parameters)
    .fetch()
    .flow { row, _ -> /* mapping */ }
```

### 8. Using R2dbcClient

```kotlin
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.core.execute

// Execute a query with R2dbcClient
val r2dbcClient: R2dbcClient = TODO() // injected

val users = r2dbcClient
    .execute<User>("SELECT * FROM users WHERE active = :active")
    .bind("active", true)
    .fetch()
    .flow()

// Execute with a Query object
val query = QueryBuilder().build { /* ... */ }
val results = r2dbcClient.execute<User>(query).fetch()
```

### 9. Count and Existence Check

```kotlin
// Count
val count = databaseClient
    .sql("SELECT COUNT(*) FROM users WHERE active = :active")
    .bind("active", true)
    .fetch()
    .awaitCount()

// Check existence
val exists = databaseClient
    .sql("SELECT 1 FROM users WHERE id = :id")
    .bind("id", 1)
    .fetch()
    .awaitExists()
```

### 10. Spring Boot Auto Configuration

```yaml
# application.yml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/mydb
    username: user
    password: pass
```

```kotlin
// R2dbcClient auto-injection
@Service
class UserService(
    private val r2dbcClient: R2dbcClient
) {
    suspend fun findAll(): Flow<User> {
        return r2dbcClient
            .execute<User>("SELECT * FROM users")
            .fetch()
            .flow()
    }
}
```

## Test Support

```kotlin
import io.bluetape4k.r2dbc.AbstractR2dbcTest
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest

@DataR2dbcTest
class UserRepositoryTest: AbstractR2dbcTest() {

    @Test
    fun `user lookup test`() = runSuspendIO {
        val user = client.databaseClient
            .sql("SELECT * FROM users WHERE username = :username")
            .bind("username", "jsmith")
            .fetch()
            .awaitSingle { row, _ ->
                User(
                    id = row.get("user_id") as Int,
                    username = row.get("username") as String
                )
            }

        user.username shouldBeEqualTo "jsmith"
    }
}
```

## References

- [R2DBC Official Documentation](https://r2dbc.io/)
- [Spring Data R2DBC](https://docs.spring.io/spring-data/r2dbc/docs/current/reference/html/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)

## License

MIT License
