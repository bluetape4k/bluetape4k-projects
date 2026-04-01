# Module bluetape4k-vertx

Vert.x 기반 비동기/Coroutines 개발을 위한 단일 통합 모듈입니다.

> 구 `vertx/core`, `vertx/sqlclient`, `vertx/resilience4j` 모듈이 이 모듈로 통합되었습니다.

## 제공 기능

### Vert.x Core (구 `vertx/core`)
- Vert.x Kotlin Coroutines 확장
- Verticle 배포/관리 유틸리티
- EventBus 코루틴 어댑터
- `vertx_lang_kotlin_coroutines` 기반 suspend 지원

### Vert.x SQL Client (구 `vertx/sqlclient`)
- `vertx-sql-client` + `vertx-sql-client-templates` 통합
- MySQL / PostgreSQL 드라이버 내장
- MyBatis Dynamic SQL 통합
- JDBC 클라이언트 지원 (선택적)
- Coroutines 기반 쿼리 실행

### Resilience4j 통합 (구 `vertx/resilience4j`)
- Vert.x + Resilience4j Circuit Breaker 통합
- Resilience4j Micrometer 메트릭 연동 (선택적)

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-vertx:${bluetape4kVersion}")
}
```

서비스별 선택적 런타임 의존성:

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-vertx:${bluetape4kVersion}")

    // MySQL 사용 시
    runtimeOnly(Libs.vertx_mysql_client)

    // PostgreSQL 사용 시
    runtimeOnly(Libs.vertx_pg_client)
}
```

## 주요 의존성 구조

| 범주 | 의존 방식 | 설명 |
|------|-----------|------|
| `vertx-core` | `api` | Vert.x 핵심 |
| `vertx-lang-kotlin` | `api` | Kotlin 언어 지원 |
| `vertx-lang-kotlin-coroutines` | `api` | Coroutines 지원 |
| `vertx-sql-client` | `api` | SQL 클라이언트 추상화 |
| `bluetape4k-resilience4j` | `api` | Resilience4j 통합 |
| `vertx-mysql-client` | `implementation` | MySQL 드라이버 |
| `vertx-pg-client` | `implementation` | PostgreSQL 드라이버 |
| `vertx-web` | `compileOnly` | 선택적 Web 지원 |
| `vertx-jdbc-client` | `compileOnly` | 선택적 JDBC |

## 아키텍처 다이어그램

### 모듈 의존성 구조

```mermaid
flowchart TD
    subgraph bluetape4k-vertx
        CORE[Vert.x Core<br/>vertx-core]
        KOTLIN[Vert.x Kotlin<br/>vertx-lang-kotlin]
        COROUTINES[Vert.x Coroutines<br/>vertx-lang-kotlin-coroutines]
        SQL[Vert.x SQL Client<br/>vertx-sql-client]
        R4J[bluetape4k-resilience4j]
    end

    subgraph 선택적 런타임
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
```

### Vert.x 이벤트 루프 + Coroutines 처리 흐름

```mermaid
flowchart LR
    subgraph 이벤트 루프["Vert.x 이벤트 루프"]
        EL[Event Loop Thread]
        EB[EventBus]
        VER[Verticle<br/>CoroutineVerticle]
    end

    subgraph Coroutines["Kotlin Coroutines"]
        SC[suspend fun start]
        COR[CoroutineScope<br/>vertxDispatcher]
    end

    subgraph SQL_Client["SQL 클라이언트"]
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
```

### Circuit Breaker + Resilience4j 통합 흐름

```mermaid
sequenceDiagram
    participant 앱 as Verticle (Coroutines)
    participant CB as CircuitBreaker<br/>(Resilience4j)
    participant SVC as 원격 서비스

    앱->>CB: cb.executeSuspend { remoteCall() }
    CB->>CB: 상태 확인 (CLOSED/OPEN/HALF_OPEN)

    alt CLOSED (정상)
        CB->>SVC: 원격 호출
        SVC-->>CB: 응답
        CB-->>앱: 성공 결과
    else OPEN (차단)
        CB-->>앱: CallNotPermittedException
    else HALF_OPEN (테스트)
        CB->>SVC: 테스트 호출
        SVC-->>CB: 성공/실패
        CB->>CB: 상태 전환 (CLOSED/OPEN)
        CB-->>앱: 결과 반환
    end
```

### Vert.x 핵심 컴포넌트 클래스 구조

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

    CoroutineVerticle --> EventBus : 이벤트 수신/발행
    CoroutineVerticle --> Pool : SQL 쿼리
    Pool --|> SqlClient
    CoroutineVerticle --> CircuitBreaker : 장애 격리

```

## 사용 예시

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
