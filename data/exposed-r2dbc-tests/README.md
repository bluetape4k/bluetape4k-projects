# Module bluetape4k-exposed-r2dbc-tests

English | [한국어](./README.ko.md)

## Overview

A shared test-infrastructure module for testing modules built on [Exposed R2DBC](https://github.com/JetBrains/Exposed). It helps you write reactive database tests more easily.

## Adding the Dependency

```kotlin
dependencies {
    testImplementation("io.github.bluetape4k:bluetape4k-exposed-r2dbc-tests:${version}")
}
```

## Key Features

- **Common test base**: `AbstractExposedR2dbcTest` provides the base structure for R2DBC tests
- **Multiple database support**: supports H2, MySQL, MariaDB, and PostgreSQL R2DBC tests
- **Testcontainers integration**: supports real database tests through Docker-based containers
- **Coroutine-native**: all tests are based on suspend functions
- **Table and schema utilities**: reusable entities and tables for tests

## Supported Databases

| Database           | TestDB       | R2DBC Driver       |
|--------------------|--------------|--------------------|
| H2                 | `H2`         | `r2dbc-h2`         |
| H2 MySQL mode      | `H2_MYSQL`   | `r2dbc-h2`         |
| H2 MariaDB mode    | `H2_MARIADB` | `r2dbc-h2`         |
| H2 PostgreSQL mode | `H2_PSQL`    | `r2dbc-h2`         |
| MariaDB            | `MARIADB`    | `r2dbc-mariadb`    |
| MySQL 8.0          | `MYSQL_V8`   | `r2dbc-mysql`      |
| PostgreSQL         | `POSTGRESQL` | `r2dbc-postgresql` |

## Usage Examples

### Write a Basic Test

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

object Users: LongIdTable("users") {
    val name = varchar("name", 50)
    val email = varchar("email", 100)
}

class UserRepositoryTest: AbstractExposedR2dbcTest() {

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `should insert and find user`(testDB: TestDB) = runBlocking {
        withTables(testDB, Users) {
            // Insert
            Users.insert {
                it[name] = "John"
                it[email] = "john@example.com"
            }

            // Query
            val user = Users.selectAll().single()

            assertEquals("John", user[Users.name])
            assertEquals("john@example.com", user[Users.email])
        }
    }
}
```

### `withDb` - When You Only Need a DB Connection

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withDb

@ParameterizedTest
@MethodSource(ENABLE_DIALECTS_METHOD)
fun `should connect to database`(testDB: TestDB) = runBlocking {
    withDb(testDB) {
        // runs inside a suspend transaction
        val isConnected = true // connection check logic
        assertTrue(isConnected)
    }
}
```

### `withTables` - Automatic Table Create/Drop

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables

@ParameterizedTest
@MethodSource(ENABLE_DIALECTS_METHOD)
fun `should create and drop tables`(testDB: TestDB) = runBlocking {
    withTables(testDB, Users, Orders) {
        // tables are created automatically before the test
        // tables are dropped automatically after the test

        Users.insert { /* ... */ }
        Orders.insert { /* ... */ }

        // test logic
    }
}
```

### Test Only a Specific Database

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.TestDB

class PostgresOnlyTest: AbstractExposedR2dbcTest() {

    // PostgreSQL only
    companion object {
        @JvmStatic
        fun databases() = TestDB.ALL_POSTGRES
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun `postgres specific test`(testDB: TestDB) = runBlocking {
        withTables(testDB, Users) {
            // PostgreSQL-specific test
        }
    }
}
```

### Test by Database Group

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.TestDB

class MySQLLikeTest: AbstractExposedR2dbcTest() {

    companion object {
        // MySQL + MariaDB + H2 MySQL mode
        @JvmStatic
        fun databases() = TestDB.ALL_MYSQL_LIKE

        // PostgreSQL + H2 PostgreSQL mode
        @JvmStatic
        fun postgresDatabases() = TestDB.ALL_POSTGRES_LIKE
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun `mysql compatible test`(testDB: TestDB) = runBlocking {
        withTables(testDB, Users) {
            // test on MySQL-compatible databases
        }
    }
}
```

### Flow-Based Streaming Query

```kotlin
import kotlinx.coroutines.flow.toList

@ParameterizedTest
@MethodSource(ENABLE_DIALECTS_METHOD)
fun `should stream query results`(testDB: TestDB) = runBlocking {
    withTables(testDB, Users) {
        // insert multiple records
        repeat(100) { i ->
            Users.insert {
                it[name] = "User$i"
                it[email] = "user$i@example.com"
            }
        }

        // stream results with Flow
        val users = Users.selectAll().toList()
        assertEquals(100, users.size)
    }
}
```

## `TestDB` Configuration

```kotlin
object TestDBConfig {
    // true: use Testcontainers (default)
    // false: use locally installed DB servers directly
    var useTestcontainers = true

    // true: use only in-memory H2 for fast local tests (default)
    // false: use H2 + PostgreSQL + MySQL V8 (requires Testcontainers)
    var useFastDB = true
}
```

If `useFastDB = true` (default), `enabledDialects()` returns only H2. Set
`useFastDB = false` when full database coverage is required. Docker is needed in that case.

## Test Schema and Data

### Shared Table Schemas

| File                             | Description              |
|----------------------------------|--------------------------|
| `shared/entities/BoardSchema.kt` | `Board` table            |
| `shared/mapping/PersonSchema.kt` | `Person` mapping table   |
| `shared/mapping/OrderSchema.kt`  | `Order` mapping table    |
| `shared/samples/BankSchema.kt`   | bank account table       |
| `shared/samples/UserCities.kt`   | user-city relation table |
| `shared/dml/DMLTestData.kt`      | DML test data            |

## Testcontainers Configuration

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.Containers

// MariaDB container
Containers.MariaDB

// MySQL 8.0 container
Containers.MySQL8

// PostgreSQL container
Containers.Postgres
```

## JDBC vs R2DBC Test Comparison

| Feature         | exposed-tests              | exposed-r2dbc-tests      |
|-----------------|----------------------------|--------------------------|
| API             | JDBC                       | R2DBC                    |
| Execution model | synchronous / asynchronous | coroutine-native         |
| `withDb`        | `withDb`                   | `suspend fun withDb`     |
| `withTables`    | `withTables`               | `suspend fun withTables` |
| Transaction     | `JdbcTransaction`          | `R2dbcTransaction`       |

## Feature Details

| File                          | Description                                                                                                                                                           |
|-------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AbstractExposedR2dbcTest.kt` | base class for R2DBC tests                                                                                                                                            |
| `TestDB.kt`                   | definitions of supported R2DBC databases                                                                                                                              |
| `TestDBConfig.kt`             | test-environment settings (`useTestcontainers`, `useFastDB`)                                                                                                          |
| `Containers.kt`               | Testcontainers management                                                                                                                                             |
| `withDb.kt`                   | R2DBC DB connection utility                                                                                                                                           |
| `withTables.kt`               | R2DBC table utility                                                                                                                                                   |
| `withAutoCommit.kt`           | AutoCommit mode utility                                                                                                                                               |
| `withSchemas.kt`              | schema utility                                                                                                                                                        |
| `Assertions.kt`               | assertion helpers for tests (`assertTrue`, `assertFalse`, `assertEquals`, `assertNotEquals`, `assertFailAndRollback`, `expectException`, `expectExceptionSuspending`) |
| `TestSupports.kt`             | test helper utilities (`inProperCase`, `currentDialectTest`, `insertAndSuspending`, and more)                                                                         |

## Example R2DBC Connection Strings

```kotlin
// H2
"r2dbc:h2:mem:///regular;DB_CLOSE_DELAY=-1;"

// H2 MySQL mode
"r2dbc:h2:mem:///mysql;DB_CLOSE_DELAY=-1;MODE=MySQL;"

// MariaDB
"r2dbc:mariadb://user:pass@host:3306/database"

// MySQL
"r2dbc:mysql://user:pass@host:3306/database"

// PostgreSQL
"r2dbc:postgresql://user:pass@host:5432/database"
```

## Test Infrastructure Structure

```mermaid
flowchart TD
    subgraph TestClasses
        A[AbstractExposedR2dbcTest] --> B[ParameterizedTest]
        B --> C[ENABLE_DIALECTS_METHOD]
    end

    subgraph SupportedDBs_R2DBC
        D[H2 / H2_MYSQL / H2_MARIADB / H2_PSQL<br/>r2dbc-h2]
        E[MariaDB - r2dbc-mariadb]
        F[MySQL 8.0 - r2dbc-mysql]
        G[PostgreSQL - r2dbc-postgresql]
    end

    subgraph CoroutineUtilities
        H[suspend withDb]
        I[suspend withTables]
        J[suspend withAutoCommit]
        K[suspend withSchemas]
    end

    C --> D
    C --> E
    C --> F
    C --> G

    A --> H
    A --> I
    A --> J

    classDef testStyle fill:#37474F,stroke:#263238,color:#FFFFFF
    classDef dbStyle fill:#F57F17,stroke:#E65100,color:#FFFFFF
    classDef coroutineStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF

    class TestClasses testStyle
    class SupportedDBs_R2DBC dbStyle
    class CoroutineUtilities coroutineStyle
```

### JDBC vs R2DBC Test Comparison

```mermaid
flowchart LR
    subgraph JDBC_Tests["exposed-jdbc-tests"]
        J1[withDb] -->|synchronous| J2[JdbcTransaction]
        J3[withTables] -->|sync/async| J4[withTablesSuspending]
    end
    subgraph R2DBC_Tests["exposed-r2dbc-tests"]
        R1[suspend withDb] -->|coroutine-native| R2[R2dbcTransaction]
        R3[suspend withTables] -->|Flow streaming| R4[Flow queries]
    end

    style JDBC_Tests fill:#1565C0,stroke:#0D47A1,color:#FFFFFF
    style R2DBC_Tests fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
```

## Notes

- All R2DBC tests are based on `suspend` functions
- MySQL 5.7 is excluded due to R2DBC driver compatibility issues
- Docker is required when using Testcontainers
- Flow-based streaming queries are supported
