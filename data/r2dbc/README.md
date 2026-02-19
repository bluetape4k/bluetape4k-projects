# Module bluetape4k-r2dbc

R2DBC(Reactive Relational Database Connectivity) 환경에서 코루틴과 Flow를 활용한 반응형 데이터 접근을 지원하는 라이브러리입니다.

## 특징

- **Kotlin Coroutines/Flow 지원**: R2DBC의 Reactive 스트림을 Kotlin Flow로 변환
- **DatabaseClient 확장**: 파라미터 바인딩, SQL 실행 보조 함수
- **Query Builder**: 동적 쿼리 구성을 위한 간편한 빌더
- **Transaction 지원**: R2DBC 트랜잭션 관리
- **Spring Boot Auto Configuration**: Spring 환경 자동 구성

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-r2dbc:${version}")
}
```

## 주요 기능

### 1. DatabaseClient SQL 실행

```kotlin
import io.bluetape4k.r2dbc.support.*
import kotlinx.coroutines.flow.toList

// SELECT 쿼리 실행
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

// 단일 결과 조회
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

// 결과를 Map으로 조회
val userMap = databaseClient
    .sql("SELECT * FROM users WHERE id = :id")
    .bind("id", 1)
    .fetch()
    .awaitSingleAsMap()
```

### 2. 파라미터 바인딩

```kotlin
// Map으로 파라미터 바인딩
val parameters = mapOf(
    "username" to "john",
    "active" to true
)

val users = databaseClient
    .sql("SELECT * FROM users WHERE username = :username AND active = :active")
    .bindMap(parameters)
    .fetch()
    .flow { row, _ -> /* mapping */ }

// 인덱스 기반 파라미터 바인딩
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

### 3. CRUD 연산

```kotlin
// INSERT 및 생성된 키 반환
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

### 4. Flow 및 코루틴 지원

```kotlin
import kotlinx.coroutines.flow.*

// Flow로 결과 수집
val userFlow: Flow<User> = databaseClient
    .sql("SELECT * FROM users")
    .fetch()
    .flow { row, metadata ->
        User(
            id = row.get("id") as Int,
            name = row.get("name") as String
        )
    }

// Flow 변환
val names = userFlow
    .map { it.name }
    .filter { it.startsWith("A") }
    .toList()

// 리스트로 수집
val users = databaseClient
    .sql("SELECT * FROM users")
    .fetch()
    .awaitList { row, _ -> /* mapping */ }
```

### 5. 트랜잭션 관리

```kotlin
import io.bluetape4k.r2dbc.support.withTransactionSuspend

// 트랜잭션 내에서 실행
databaseClient.withTransactionSuspend { tx ->
    // 트랜잭션 내에서 여러 작업 수행
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

### 6. Query Builder

```kotlin
import io.bluetape4k.r2dbc.query.QueryBuilder

// 동적 쿼리 구성
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

// 쿼리 실행
val users = databaseClient
    .sql(query.sql)
    .bindMap(query.parameters)
    .fetch()
    .flow { row, _ -> /* mapping */ }
```

### 7. R2dbcClient 사용

```kotlin
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.core.execute

// R2dbcClient로 쿼리 실행
val r2dbcClient: R2dbcClient = /* 주입 */

val users = r2dbcClient
    .execute<User>("SELECT * FROM users WHERE active = :active")
    .bind("active", true)
    .fetch()
    .flow()

// Query 객체로 실행
val query = QueryBuilder().build { /* ... */ }
val results = r2dbcClient.execute<User>(query).fetch()
```

### 8. 카운트 및 존재 여부 확인

```kotlin
// 카운트
val count = databaseClient
    .sql("SELECT COUNT(*) FROM users WHERE active = :active")
    .bind("active", true)
    .fetch()
    .awaitCount()

// 존재 여부
val exists = databaseClient
    .sql("SELECT 1 FROM users WHERE id = :id")
    .bind("id", 1)
    .fetch()
    .awaitExists()
```

### 9. Spring Boot Auto Configuration

```kotlin
// application.yml
spring:
r2dbc:
url: r2dbc:postgresql://localhost:5432/mydb
username: user
password: pass

// R2dbcClient 자동 주입
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

## 테스트 지원

```kotlin
import io.bluetape4k.r2dbc.AbstractR2dbcTest
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest

@DataR2dbcTest
class UserRepositoryTest: AbstractR2dbcTest() {

    @Test
    fun `사용자 조회 테스트`() = runSuspendIO {
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

## 참고 자료

- [R2DBC 공식 문서](https://r2dbc.io/)
- [Spring Data R2DBC](https://docs.spring.io/spring-data/r2dbc/docs/current/reference/html/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)

## 라이선스

Apache License 2.0
