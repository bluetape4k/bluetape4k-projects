# Module bluetape4k-exposed-r2dbc-tests

## 개요

[Exposed R2DBC](https://github.com/JetBrains/Exposed) 기반 모듈 테스트를 위한 공통 테스트 인프라 모듈입니다. 반응형(R2DBC) 데이터베이스 테스트를 쉽게 작성할 수 있도록 도와줍니다.

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.bluetape4k:bluetape4k-exposed-r2dbc-tests:${version}")
}
```

## 주요 기능

- **공통 테스트 베이스**: `AbstractExposedR2dbcTest`로 R2DBC 테스트 기본 구조 제공
- **다중 DB 지원**: H2, MySQL, MariaDB, PostgreSQL R2DBC 테스트 지원
- **Testcontainers 통합**: Docker 기반 실제 DB 테스트 지원
- **Coroutine 네이티브**: 모든 테스트가 suspend 함수 기반
- **테이블/스키마 유틸**: 테스트용 엔티티/테이블 재사용

## 지원 데이터베이스

| 데이터베이스           | TestDB       | R2DBC Driver       |
|------------------|--------------|--------------------|
| H2               | `H2`         | `r2dbc-h2`         |
| H2 MySQL 모드      | `H2_MYSQL`   | `r2dbc-h2`         |
| H2 MariaDB 모드    | `H2_MARIADB` | `r2dbc-h2`         |
| H2 PostgreSQL 모드 | `H2_PSQL`    | `r2dbc-h2`         |
| MariaDB          | `MARIADB`    | `r2dbc-mariadb`    |
| MySQL 8.0        | `MYSQL_V8`   | `r2dbc-mysql`      |
| PostgreSQL       | `POSTGRESQL` | `r2dbc-postgresql` |

## 사용 예시

### 기본 테스트 작성

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

### withDb - 테이블 없이 DB 연결만 필요한 경우

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withDb

@ParameterizedTest
@MethodSource(ENABLE_DIALECTS_METHOD)
fun `should connect to database`(testDB: TestDB) = runBlocking {
    withDb(testDB) {
        // suspend 트랜잭션 내에서 실행
        val isConnected = true // 연결 확인 로직
        assertTrue(isConnected)
    }
}
```

### withTables - 테이블 자동 생성/삭제

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables

@ParameterizedTest
@MethodSource(ENABLE_DIALECTS_METHOD)
fun `should create and drop tables`(testDB: TestDB) = runBlocking {
    withTables(testDB, Users, Orders) {
        // 테스트 시작 전 테이블 자동 생성
        // 테스트 종료 후 테이블 자동 삭제

        Users.insert { /* ... */ }
        Orders.insert { /* ... */ }

        // 테스트 로직
    }
}
```

### 특정 DB만 테스트

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.TestDB

class PostgresOnlyTest: AbstractExposedR2dbcTest() {

    // PostgreSQL만 테스트
    companion object {
        @JvmStatic
        fun databases() = TestDB.ALL_POSTGRES
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun `postgres specific test`(testDB: TestDB) = runBlocking {
        withTables(testDB, Users) {
            // PostgreSQL 전용 테스트
        }
    }
}
```

### DB 그룹별 테스트

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.TestDB

class MySQLLikeTest: AbstractExposedR2dbcTest() {

    companion object {
        // MySQL + MariaDB + H2 MySQL 모드
        @JvmStatic
        fun databases() = TestDB.ALL_MYSQL_LIKE

        // PostgreSQL + H2 PostgreSQL 모드
        @JvmStatic
        fun postgresDatabases() = TestDB.ALL_POSTGRES_LIKE
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun `mysql compatible test`(testDB: TestDB) = runBlocking {
        withTables(testDB, Users) {
            // MySQL 호환 DB 테스트
        }
    }
}
```

### Flow 기반 스트리밍 쿼리

```kotlin
import kotlinx.coroutines.flow.toList

@ParameterizedTest
@MethodSource(ENABLE_DIALECTS_METHOD)
fun `should stream query results`(testDB: TestDB) = runBlocking {
    withTables(testDB, Users) {
        // 여러 레코드 삽입
        repeat(100) { i ->
            Users.insert {
                it[name] = "User$i"
                it[email] = "user$i@example.com"
            }
        }

        // Flow로 스트리밍 조회
        val users = Users.selectAll().toList()
        assertEquals(100, users.size)
    }
}
```

## TestDB 설정

```kotlin
// Testcontainers 사용 여부
const val USE_TESTCONTAINERS = true

// 빠른 테스트를 위해 H2만 사용
const val USE_FAST_DB = false
```

## 테스트용 스키마/데이터

### 공유 테이블 스키마

| 파일                               | 설명               |
|----------------------------------|------------------|
| `shared/entities/BoardSchema.kt` | Board 테이블        |
| `shared/mapping/PersonSchema.kt` | Person 매핑 테이블    |
| `shared/mapping/OrderSchema.kt`  | Order 매핑 테이블     |
| `shared/samples/BankSchema.kt`   | Bank 계좌 테이블      |
| `shared/samples/UserCities.kt`   | User-City 관계 테이블 |
| `shared/dml/DMLTestData.kt`      | DML 테스트 데이터      |

## Testcontainers 구성

```kotlin
import io.bluetape4k.exposed.r2dbc.tests.Containers

// MariaDB 컨테이너
Containers.MariaDB

// MySQL 8.0 컨테이너
Containers.MySQL8

// PostgreSQL 컨테이블
Containers.Postgres
```

## JDBC vs R2DBC 테스트 비교

| 특징         | exposed-tests     | exposed-r2dbc-tests      |
|------------|-------------------|--------------------------|
| API        | JDBC              | R2DBC                    |
| 실행 모델      | 동기/비동기            | Coroutine 네이티브           |
| withDb     | `withDb`          | `suspend fun withDb`     |
| withTables | `withTables`      | `suspend fun withTables` |
| 트랜잭션       | `JdbcTransaction` | `R2dbcTransaction`       |

## 주요 기능 상세

| 파일                            | 설명                     |
|-------------------------------|------------------------|
| `AbstractExposedR2dbcTest.kt` | R2DBC 테스트 기본 클래스       |
| `TestDB.kt`                   | R2DBC 지원 DB 정의         |
| `Containers.kt`               | Testcontainers 컨테이너 관리 |
| `withDb.kt`                   | R2DBC DB 연결 유틸         |
| `withTables.kt`               | R2DBC 테이블 유틸           |
| `withAutoCommit.kt`           | AutoCommit 모드 유틸       |
| `withSchemas.kt`              | Schema 유틸              |
| `Assertions.kt`               | 테스트 어설션 유틸             |
| `TestSupports.kt`             | 테스트 보조 유틸              |

## R2DBC 연결 문자열 예시

```kotlin
// H2
"r2dbc:h2:mem:///regular;DB_CLOSE_DELAY=-1;"

// H2 MySQL 모드
"r2dbc:h2:mem:///mysql;DB_CLOSE_DELAY=-1;MODE=MySQL;"

// MariaDB
"r2dbc:mariadb://user:pass@host:3306/database"

// MySQL
"r2dbc:mysql://user:pass@host:3306/database"

// PostgreSQL
"r2dbc:postgresql://user:pass@host:5432/database"
```

## 참고 사항

- R2DBC 테스트는 모두 `suspend` 함수 기반입니다
- MySQL 5.7은 R2DBC 드라이버 호환성 문제로 제외됩니다
- Testcontainers 사용 시 Docker가 필요합니다
- Flow 기반 스트리밍 쿼리가 가능합니다
