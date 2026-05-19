# Module bluetape4k-r2dbc

[English](./README.md) | 한국어

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
    implementation("io.github.bluetape4k:bluetape4k-r2dbc:${version}")
}
```

## 주요 기능

### 1. R2DBC 커넥션 풀 튜닝

`R2dbcPoolConfig`는 고처리량 프리셋과 부하 환경에서 중요한 r2dbc-pool 옵션을 제공합니다. 풀 크기, 워밍업 커넥션 수, 커넥션 획득 대기 큐, 획득 타임아웃, 생성 타임아웃, 검증 타임아웃, 검증 깊이, 선택적 검증 쿼리, 풀 이름, JMX 등록을 직접 설정할 수 있습니다.

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
    validationQuery = null // 커넥션 획득마다 추가 SQL 왕복을 만들지 않음
    poolName = preset.poolName
}
```

`maxSize`는 DB 서버의 전체 커넥션 한도와 애플리케이션 인스턴스 수를 함께 고려해 정하세요. 지연 시간에 민감한 서비스에서는 `maxAcquireTime`과
`maxPendingAcquire`를 제한해 과부하 시 무제한 대기열이 쌓이지 않게 하는 것이 좋습니다.

풀 benchmark는 다음 task로 실행합니다.

```bash
./gradlew :bluetape4k-r2dbc:benchmarkPoolConfig
./gradlew :bluetape4k-r2dbc:benchmarkH2PoolAcquire
./gradlew :bluetape4k-r2dbc:benchmarkPostgresPoolAcquire
./gradlew :bluetape4k-r2dbc:benchmarkMysql8PoolAcquire
./gradlew :bluetape4k-r2dbc:benchmarkH2PoolContention
```

최근 로컬 acquire benchmark(`8` JMH threads, `3` measurement iterations,
`validationQuery = "SELECT 1"`)에서는 순수 acquire/close 처리량은 드라이버마다 달랐고, 실제 커넥션 점유 시간이 들어가면 기본/고처리량 profile이 수렴했습니다.

| DB                          | 점유 시간 | 기본 설정         | 고처리량 프리셋      |
|-----------------------------|-------|---------------|---------------|
| H2                          | 0 ms  | 161,741 ops/s | 173,026 ops/s |
| H2                          | 1 ms  | 6,788 ops/s   | 6,803 ops/s   |
| H2                          | 5 ms  | 1,423 ops/s   | 1,423 ops/s   |
| PostgreSQL 18 Testcontainer | 0 ms  | 18,008 ops/s  | 18,724 ops/s  |
| PostgreSQL 18 Testcontainer | 1 ms  | 4,775 ops/s   | 4,637 ops/s   |
| PostgreSQL 18 Testcontainer | 5 ms  | 1,289 ops/s   | 1,282 ops/s   |
| MySQL 8.4 Testcontainer     | 0 ms  | 8,570 ops/s   | 8,147 ops/s   |
| MySQL 8.4 Testcontainer     | 1 ms  | 4,305 ops/s   | 4,339 ops/s   |
| MySQL 8.4 Testcontainer     | 5 ms  | 1,183 ops/s   | 1,177 ops/s   |

contention benchmark는 `64` JMH threads에서 동시성보다 작은 `maxSize`를 사용합니다. 이 경우에는 풀 크기의 영향이 뚜렷하게 나타납니다.

| 점유 시간 | maxSize=4 | maxSize=8 | maxSize=16  |
|-------|-----------|-----------|-------------|
| 10 ms | 365 ops/s | 733 ops/s | 1,470 ops/s |
| 50 ms | 78 ops/s  | 156 ops/s | 311 ops/s   |

#### 실측 기반 튜닝 가이드

- 순수 acquire/close 경로(
  `0 ms` 점유)는 실제 사용하는 드라이버 기준으로 비교하세요. 이번 실행에서는 H2/PostgreSQL은 고처리량 프리셋이 약간 높았고, MySQL 8은 기본 profile이 높았습니다. 이 경로는 대부분 드라이버/풀 오버헤드 microbenchmark이므로 서버 기본값을 이것만으로 결정하지 마세요.
- 요청이 SQL 또는 트랜잭션 실행 동안 커넥션을 점유하는 서버 워크로드라면 `R2dbcPoolConfig.highThroughput()`을 사용하세요. `1 ms`,
  `5 ms` 점유 시간에서는 H2, PostgreSQL, MySQL 8 모두 점유 시간이 처리량을 지배해 두 profile이 사실상 수렴했고, 이때는 high-throughput 프리셋의 bounded queue와 warmup 동작이 더 중요한 운영 속성이 됩니다.
- 동시 요청 수가 풀 크기를 넘고 DB가 추가 세션을 감당할 수 있을 때만 `maxSize`를 늘리세요. `64`개 경쟁 thread에서는 커넥션 슬롯이 병목이라 처리량이
  `maxSize`에 거의 선형으로 반응했습니다.
- 긴 쿼리와 트랜잭션은 유효 처리량 상한을 대략 `maxSize / connection hold time`으로 낮춥니다. contention benchmark에서 같은 `maxSize`일 때 점유 시간이
  `10 ms`에서 `50 ms`로 늘면 처리량도 약 `5x` 낮아졌습니다.
- `maxSize`는 DB 기준으로 먼저 산정하세요:
  `floor((DB max_connections - 운영/복제 예약 커넥션 수) / 애플리케이션 인스턴스 수)`. 이후 부하 테스트에서 DB p95/p99 latency가 상승하면 낮춥니다.
- `initialSize`와 `minIdle`은 항상 `maxSize` 이하로 유지하세요. high-throughput 프리셋은
  `min(maxSize, max(availableProcessors * 2, 16))` 커넥션을 워밍업해 첫 트래픽 스파이크에서 전체 할당 비용을 피합니다.
- 사용자-facing 서비스에서는 `maxPendingAcquire`를 제한하세요. 프리셋은
  `maxSize * 4`를 사용해 짧은 burst는 흡수하되, 과부하를 숨기고 tail latency를 키우는 무제한 backlog를 피합니다.
-
`maxPendingAcquire`가 너무 작으면 풀과 pending queue가 찼을 때 r2dbc-pool이 추가 획득을 거부합니다. 이는 fail-fast 과부하 제어에 유용하지만, 애플리케이션 acquire 실패/timeout 지표와 함께 운영해야 합니다.
- `maxAcquireTime`은 유한한 값으로 두세요. API 서비스는 `2-3s`를 시작점으로 삼고, 실패보다 대기가 나은 배치 작업은 더 길게 둘 수 있습니다.
- 운영 드라이버가 로컬 검증을 지원한다면 `ValidationDepth.LOCAL`과 `validationQuery = null`을 우선하세요. benchmark에서
  `SELECT 1`을 사용한 것은 H2/PostgreSQL/MySQL 검증 경로를 일관되게 만들기 위한 것이며, SQL 검증 쿼리는 커넥션 획득마다 DB 왕복을 추가합니다.
- 위 수치는 로컬 기준선이지 보편적 한계값이 아닙니다. 쿼리 latency, 트랜잭션 시간, 인스턴스 수, DB 커넥션 한도가 바뀌면 DB별 pool acquire benchmark를 다시 실행하세요.

### 2. DatabaseClient SQL 실행

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

### 3. 파라미터 바인딩

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

### 4. CRUD 연산

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

### 5. Flow 및 코루틴 지원

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

### 6. 트랜잭션 관리

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

### 7. Query Builder

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

### 8. R2dbcClient 사용

```kotlin
import io.bluetape4k.r2dbc.R2dbcClient
import io.bluetape4k.r2dbc.core.execute

// R2dbcClient로 쿼리 실행
val r2dbcClient: R2dbcClient = TODO() // 주입

val users = r2dbcClient
    .execute<User>("SELECT * FROM users WHERE active = :active")
    .bind("active", true)
    .fetch()
    .flow()

// Query 객체로 실행
val query = QueryBuilder().build { /* ... */ }
val results = r2dbcClient.execute<User>(query).fetch()
```

### 9. 카운트 및 존재 여부 확인

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

## 아키텍처 다이어그램

### 확장 함수 API 개요

![Component Function API Component 1](../../docs/images/readme-diagrams/data-r2dbc-ko-diagram-01.svg)

### 주요 API 구조

![Component API Component 2](../../docs/images/readme-diagrams/data-r2dbc-ko-diagram-02.svg)

### R2DBC 쿼리 실행 흐름

![R2DBC Query Component Component 3](../../docs/images/readme-diagrams/data-r2dbc-ko-diagram-03.svg)

### JDBC vs R2DBC 비교

![JDBC vs R2DBC Component 4](../../docs/images/readme-diagrams/data-r2dbc-ko-diagram-04.svg)

## 참고 자료

- [R2DBC 공식 문서](https://r2dbc.io/)
- [Spring Data R2DBC](https://docs.spring.io/spring-data/r2dbc/docs/current/reference/html/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)

## 라이선스

MIT License
