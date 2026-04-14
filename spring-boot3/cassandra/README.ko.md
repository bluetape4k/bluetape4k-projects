# Module bluetape4k-spring-cassandra

[English](./README.md) | 한국어

`bluetape4k-spring-cassandra`는 Spring Data Cassandra 기반 개발에서 자주 쓰는 코루틴 확장과 편의 DSL, 스키마 유틸을 제공합니다.

## 주요 기능

- `ReactiveSession`/`ReactiveCassandraOperations`/`AsyncCassandraOperations` 코루틴 확장
- CQL 옵션(`QueryOptions`, `WriteOptions` 등) DSL 헬퍼
- 스키마 생성/트렁케이트 유틸 (`SchemaGenerator`)
- Calendar/Period 기반 테스트 유틸 및 예제

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-cassandra")
}
```

## 코루틴 확장 예시

```kotlin
val result = reactiveSession.executeSuspending("SELECT * FROM users WHERE id = ?", id)
```

## 옵션 DSL 예시

```kotlin
val options = writeOptions {
    ttl(Duration.ofSeconds(30))
    timestamp(System.currentTimeMillis())
}
```

## 아키텍처 다이어그램

### 핵심 클래스 다이어그램

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

    style ReactiveCassandraOperationsExt fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style ReactiveSessionExt fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    style WriteOptionsDsl fill:#F57F17,stroke:#E65100,color:#FFFFFF
    style SchemaGenerator fill:#E65100,stroke:#BF360C,color:#FFFFFF
    style AbstractReactiveCassandraCoroutineTest fill:#AD1457,stroke:#880E4F,color:#FFFFFF
```

### Cassandra 데이터 접근 계층

```mermaid
flowchart TD
    App["애플리케이션 코드"] --> Ext["코루틴 확장 함수<br/>(bluetape4k-spring-cassandra)"]
    Ext --> ROps["ReactiveCassandraOperations<br/>코루틴 확장"]
    Ext --> RSession["ReactiveSession<br/>코루틴 확장"]
    Ext --> AOps["AsyncCassandraOperations<br/>코루틴 확장"]
    DSL["WriteOptions / QueryOptions DSL<br/>writeOptions { ttl / timestamp }"] --> ROps
    ROps --> Driver["Cassandra Reactive Driver"]
    RSession --> Driver
    AOps --> Driver
    Driver --> Cassandra[("Apache Cassandra")]
    SchemaGen["SchemaGenerator<br/>스키마 생성 / 트렁케이트"] --> ROps

    classDef serviceStyle fill:#1565C0,stroke:#1565C0,color:#FFFFFF
    classDef asyncStyle fill:#6A1B9A,stroke:#4A148C,color:#FFFFFF
    classDef dataStyle fill:#F57F17,stroke:#E65100,color:#000000
    classDef extStyle fill:#37474F,stroke:#263238,color:#FFFFFF
    classDef utilStyle fill:#E65100,stroke:#BF360C,color:#FFFFFF

    class App serviceStyle
    class Ext asyncStyle
    class ROps asyncStyle
    class RSession asyncStyle
    class AOps asyncStyle
    class DSL dataStyle
    class Driver extStyle
    class Cassandra dataStyle
    class SchemaGen utilStyle
```

### 코루틴 변환 흐름

```mermaid
sequenceDiagram
    box rgb(224,224,224) 애플리케이션 계층
        participant App as 애플리케이션
    end
    box rgb(225,190,231) 코루틴 확장
        participant Ext as 코루틴 확장
        participant Ops as ReactiveCassandraOperations
    end
    box rgb(224,224,224) 데이터 계층
        participant DB as Apache Cassandra
    end

    App->>Ext: executeSuspending(cql, args)
    Ext->>Ops: execute(statement) → Mono/Flux
    Ops->>DB: CQL 쿼리 전송
    DB-->>Ops: ReactiveResultSet
    Ops-->>Ext: Mono<ReactiveResultSet>
    Ext-->>App: suspend 결과 반환 (코루틴)
```

## 테스트

```bash
./gradlew :bluetape4k-spring-cassandra:test
```
