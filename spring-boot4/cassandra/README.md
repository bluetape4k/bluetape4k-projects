# Module bluetape4k-spring-boot4-cassandra

Spring Data Cassandra 기반 개발에서 자주 쓰는 코루틴 확장, 편의 DSL, 스키마 유틸을 제공합니다 (Spring Boot 4.x).

> Spring Boot 3 모듈(`bluetape4k-spring-cassandra`)과 동일한 기능을 Spring Boot 4.x API로 제공합니다.

## 주요 기능

- `ReactiveSession`/`ReactiveCassandraOperations`/`AsyncCassandraOperations` 코루틴 확장
- CQL 옵션(`QueryOptions`, `WriteOptions` 등) DSL 헬퍼
- 스키마 생성/트렁케이트 유틸 (`SchemaGenerator`)

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot4-cassandra:${bluetape4kVersion}")
}
```

## 사용 예시

### 코루틴 확장

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

### Entity 정의

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

## 빌드 및 테스트

```bash
./gradlew :bluetape4k-spring-boot4-cassandra:test
```

## 아키텍처 다이어그램

### 핵심 클래스 구조

```mermaid
classDiagram
    class ReactiveCassandraOperationsExt:::serviceStyle {
        <<extension>>
        +findOneOrNullSuspending(query): T?
        +findAllAsFlow(): Flow~T~
        +insertSuspending(entity): T
        +countSuspending(query): Long
        +existsSuspending(query): Boolean
        +updateMultiSuspending(query, update): UpdateResult
        +aggregateAsFlow(aggregation): Flow~O~
    }
    class ReactiveSessionExt:::serviceStyle {
        <<extension>>
        +executeSuspending(cql, args): ReactiveResultSet
    }
    class WriteOptionsDsl:::configStyle {
        <<DSL>>
        +ttl(duration)
        +timestamp(millis)
    }
    class SchemaGenerator:::configStyle {
        +createTables(operations, types)
        +truncateTables(operations, types)
    }
    class UserRepository:::repoStyle {
        <<interface>>
        +findByEmail(email): User?
    }
    class CoroutineUserRepository:::repoStyle {
        <<interface>>
        +findByEmail(email): User?
    }
    classDef controllerStyle fill:#2196F3,stroke:#1565C0
    classDef serviceStyle fill:#4CAF50,stroke:#388E3C
    classDef repoStyle fill:#9C27B0,stroke:#6A1B9A
    classDef entityStyle fill:#FF9800,stroke:#E65100
    classDef configStyle fill:#607D8B,stroke:#37474F
    classDef cacheStyle fill:#F44336,stroke:#B71C1C

    ReactiveCassandraOperationsExt --> WriteOptionsDsl : uses
    SchemaGenerator --> ReactiveCassandraOperationsExt : uses
    ReactiveSessionExt --> ReactiveCassandraOperationsExt : complements
    CoroutineUserRepository --> ReactiveCassandraOperationsExt : delegates
```

### Cassandra 데이터 접근 계층

```mermaid
flowchart TD
    App["애플리케이션 코드"] --> Ext["코루틴 확장 함수<br/>(bluetape4k-spring-boot4-cassandra)"]
    Ext --> ROps["ReactiveCassandraOperations<br/>코루틴 확장"]
    Ext --> RSession["ReactiveSession<br/>코루틴 확장"]
    Ext --> AOps["AsyncCassandraOperations<br/>코루틴 확장"]
    DSL["WriteOptions / QueryOptions DSL<br/>writeOptions { ttl / timestamp }"] --> ROps
    ROps --> Driver["Cassandra Reactive Driver"]
    RSession --> Driver
    AOps --> Driver
    Driver --> Cassandra[("Apache Cassandra")]
    SchemaGen["SchemaGenerator<br/>스키마 생성 / 트렁케이트"] --> ROps
```

### 코루틴 변환 흐름

```mermaid
sequenceDiagram
    participant App as 애플리케이션
    participant Ext as 코루틴 확장
    participant Ops as ReactiveCassandraOperations
    participant DB as Apache Cassandra

    App->>Ext: executeSuspending(cql, args)
    Ext->>Ops: execute(statement) → Mono/Flux
    Ops->>DB: CQL 쿼리 전송
    DB-->>Ops: ReactiveResultSet
    Ops-->>Ext: Mono<ReactiveResultSet>
    Ext-->>App: suspend 결과 반환 (코루틴)
```

## 참고

- [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra)
- [Apache Cassandra](https://cassandra.apache.org/)
