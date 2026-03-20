# Module Examples - Cassandra & Spring Data Cassandra (Spring Boot 4)

Apache Cassandra와 Spring Data Cassandra를 활용하는 종합 예제입니다 (Spring Boot 4.x).

> Spring Boot 3 예제(`spring-boot3/cassandra-demo`)와 동일한 예제를 Spring Boot 4.x API로 제공합니다.

## 예제 목록

### 기본 (basic/)

| 예제 파일                                 | 설명                         |
|---------------------------------------|----------------------------|
| `BasicUserRepositoryTest.kt`          | 기본 Repository 사용법          |
| `CassandraOperationsTest.kt`          | CassandraOperations로 쿼리 실행 |
| `CoroutineCassandraOperationsTest.kt` | Coroutines 기반 비동기 쿼리       |

### Kotlin DSL (kotlin/)

| 예제 파일                     | 설명                        |
|---------------------------|---------------------------|
| `PersonRepositoryTest.kt` | Kotlin DSL로 Repository 정의 |
| `TemplateTest.kt`         | CassandraTemplate 사용법     |

### Reactive (reactive/)

| 예제 파일                              | 설명                    |
|------------------------------------|-----------------------|
| `ReactivePersonRepositoryTest.kt`  | Reactive Repository   |
| `CoroutinePersonRepositoryTest.kt` | Coroutines Repository |

### 감사 (auditing/)

| 예제 파일             | 설명                              |
|-------------------|---------------------------------|
| `AuditingTest.kt` | `@CreatedBy`, `@LastModifiedBy` |

## Entity 정의

```kotlin
@Table
data class User(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val email: String,
)
```

## Repository

```kotlin
interface UserRepository : CassandraRepository<User, UUID> {
    fun findByEmail(email: String): User?
}
```

## Coroutines 지원

```kotlin
interface CoroutinePersonRepository : CoroutineCrudRepository<Person, UUID> {
    suspend fun findByLastName(lastName: String): Flow<Person>
}
```

## 실행 방법

```bash
# Cassandra Docker 실행
docker run -d --name cassandra -p 9042:9042 cassandra:4

# 모든 예제 실행
./gradlew :bluetape4k-spring-boot4-cassandra-demo:test
```

## 참고

- [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra)
- [Apache Cassandra](https://cassandra.apache.org/)
