# Module bluetape4k-spring-boot-mongodb

[English](./README.md) | 한국어

[Spring Data MongoDB Reactive](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)를 Kotlin Coroutines 기반으로 편리하게 사용할 수 있도록 하는 확장 라이브러리입니다 (Spring Boot 4.1+).

`ReactiveMongoOperations`의 `Flux`/`Mono` 반환 타입을 `Flow`/`suspend`로 변환하는 확장 함수와,
`Criteria`·`Query`·`Update` 구성을 위한 Kotlin infix DSL을 제공합니다.

> Spring Boot 4.1+ 기반 versionless 표준 구현입니다.

## Spring Boot 4.1 설정 경계

Spring Boot 4.1은 Mongo 연결 설정을 `spring.mongodb.*` namespace로
바인딩합니다. 현재 URI 설정은 다음과 같이 작성하세요.

```yaml
spring:
    mongodb:
        uri: mongodb://127.0.0.1:27018/synthetic
```

`ReactiveMongoAutoConfiguration`은 Spring Boot의
`DataMongoReactiveAutoConfiguration` 이후에 실행됩니다. 애플리케이션이나 Spring
Boot가 이미 제공한 `ReactiveMongoOperations` Bean이 항상 우선하며, operations Bean이
없고 `ReactiveMongoDatabaseFactory`와 `MongoConverter`가 모두 있을 때만 fallback
`ReactiveMongoTemplate`을 생성합니다. operations Bean이 이미 있으면 legacy property
검사까지 포함한 library auto-configuration 전체가 backoff합니다.

`ReactiveMongoAutoConfiguration`은 Spring framework가 관리하는 구현 클래스이며,
애플리케이션이 직접 생성하는 public API가 아닙니다. framework 및 binary compatibility를
위해 public no-arg 생성자를 유지하고, Spring lifecycle callback으로 `Environment`를
주입합니다. URI 검사 상수는 내부 구현에 속하며 public field로 노출하지 않습니다.

### `spring.data.mongodb.uri`에서 마이그레이션

| 이전 설정 | 현재 설정 |
|----------|----------|
| `spring.data.mongodb.uri` | `spring.mongodb.uri` |

library fallback이 참여하는 경로에서 legacy key만 남아 있으면 기본 localhost DB로
조용히 연결하지 않고 다음 예외로 즉시 실패합니다.

```text
IllegalStateException: Unsupported legacy MongoDB property 'spring.data.mongodb.uri'; use 'spring.mongodb.uri' on Spring Boot 4.1+
```

단계적 전환 중 두 key가 함께 있으면 `spring.mongodb.uri`가 우선합니다.
테스트에는 synthetic URI를 사용하고 credential을 로그나 진단 artifact에
남기지 마세요. 애플리케이션이나 Spring Boot가 `ReactiveMongoOperations`를 제공하면
활성 연결 경로도 해당 Bean이 소유하므로 이 library는 backoff 경로의 legacy key를
검사하지 않습니다.

즉시 마이그레이션할 수 없다면 legacy namespace를 지원하는 마지막 stable artifact와
BOM을 고정한 뒤 전환을 완료하고 Boot 4.1+ artifact로 돌아오세요.

```kotlin
dependencies {
    implementation(platform("io.github.bluetape4k:bluetape4k-bom:1.12.1"))
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-mongodb:1.12.1")
}
```

## 특징

- **ReactiveMongoOperations 코루틴 확장**: `Flux` → `Flow`, `Mono` → `suspend` 변환
- **Criteria infix DSL**: `"age".criteria() gt 28`, `"name".criteria() eq "Alice"` 등
- **Query 빌더 확장**: `queryOf()`, `sortAscBy()`, `paginate()` 등
- **Update DSL**: `"field" setTo value`, `"field".incBy()` 등

## 다이어그램

### 핵심 클래스 구조

![Spring Boot MongoDB 코루틴 확장 구조 다이어그램](../../docs/images/readme-diagrams/spring-boot-mongodb-diagram-01.png)

### ReactiveMongoOperations 코루틴 확장 흐름

![ReactiveMongoOperations 코루틴 변환 흐름 다이어그램](../../docs/images/readme-diagrams/spring-boot-mongodb-diagram-02.png)

### Criteria / Query / Update DSL 흐름

![Criteria Query Update DSL 흐름 다이어그램](../../docs/images/readme-diagrams/spring-boot-mongodb-diagram-03.png)

### 코루틴 변환 시퀀스

![MongoDB 코루틴 변환 시퀀스 다이어그램](../../docs/images/readme-diagrams/spring-boot-mongodb-sequence-01.png)

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-mongodb:${bluetape4kVersion}")
}
```

## 사용 예시

### ReactiveMongoOperations 코루틴 확장

```kotlin
import io.bluetape4k.spring.mongodb.coroutines.*

// 조회
val user: User? = mongoOperations.findOneOrNullSuspending(
    Query(Criteria.where("name").`is`("Alice"))
)

// Flow로 전체 조회
val users: List<User> = mongoOperations.findAllAsFlow<User>().toList()

// 삽입
val saved: User = mongoOperations.insertSuspending(User(name = "Bob", age = 25))

// 카운트
val count: Long = mongoOperations.countSuspending<User>()

// 업데이트
mongoOperations.updateMultiSuspending<User>(
    Query(Criteria.where("city").`is`("Seoul")),
    Update().set("city", "Suwon")
)
```

### Criteria infix DSL

```kotlin
import io.bluetape4k.spring.mongodb.query.*

val c1 = "age".criteria() gt 20
val c2 = "name".criteria() eq "Alice"
val c3 = "city".criteria() inValues listOf("Seoul", "Busan")
val c4 = "deletedAt".criteria().isNull()
val c5 = "age".criteria().gt(20) andWith "city".criteria().`is`("Seoul")
```

### Query 빌더 확장

```kotlin
val query = queryOf("age".criteria() gt 20, "city".criteria() eq "Seoul")
    .sortAscBy("name")
    .paginate(page = 0, size = 10)
```

### Update DSL

```kotlin
val update = ("name" setTo "Alice")
    .andSet("age", 30)
    .andSet("city", "Seoul")
```

## 제공 확장 함수 목록

| 함수                                        | 반환 타입          | 설명               |
|-------------------------------------------|----------------|------------------|
| `findAsFlow<T>(query)`                    | `Flow<T>`      | 조건에 맞는 문서 스트림    |
| `findAllAsFlow<T>()`                      | `Flow<T>`      | 전체 문서 스트림        |
| `findOneOrNullSuspending<T>(query)`       | `T?`           | 단건 조회 (없으면 null) |
| `countSuspending<T>(query?)`              | `Long`         | 문서 수 조회          |
| `existsSuspending<T>(query)`              | `Boolean`      | 존재 여부 확인         |
| `insertSuspending(entity)`                | `T`            | 단건 삽입            |
| `insertAllAsFlow(entities)`               | `Flow<T>`      | 다건 삽입            |
| `saveSuspending(entity)`                  | `T`            | 저장 (삽입 또는 업데이트)  |
| `updateMultiSuspending<T>(query, update)` | `UpdateResult` | 다건 업데이트          |
| `removeSuspending<T>(query)`              | `DeleteResult` | 조건 삭제            |
| `aggregateAsFlow<I, O>(aggregation)`      | `Flow<O>`      | Aggregation 실행   |
| `dropCollectionSuspending<T>()`           | `Unit`         | 컬렉션 삭제           |

## 빌드 및 테스트

```bash
./gradlew :bluetape4k-spring-boot-mongodb:test
```

`ReactiveMongoAutoConfigurationTest` context suite는 namespace binding,
legacy fail-fast, dual-key 우선순위, fallback 조건, Boot 순서, 단일 인스턴스
생성, context close를 MongoDB 네트워크 I/O 없이 검증합니다. 실제 데이터베이스
검증이 필요한 코루틴 통합 테스트는 공유 Testcontainers MongoDB 서버를
사용하므로 별도로 실행하세요.

## 참고 자료

- [Spring Data MongoDB 공식 문서](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [bluetape4k-mongodb](../../data/mongodb/README.ko.md) — 네이티브 MongoDB Kotlin 드라이버 확장
