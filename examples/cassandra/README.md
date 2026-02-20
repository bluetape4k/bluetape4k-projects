# Examples - Cassandra & Spring Data Cassandra

Apache Cassandra와 Spring Data Cassandra를 활용하는 종합 예제입니다.

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

### 멀티테넌시 (multitenancy/)

| 예제 파일                       | 설명                |
|-----------------------------|-------------------|
| `keyspace/`                 | Keyspace 기반 멀티테넌시 |
| `row/RowMultitenantTest.kt` | Row-Level 멀티테넌시   |

### 감사 (auditing/)

| 예제 파일                | 설명                          |
|----------------------|-----------------------------|
| `AuditingTest.kt`    | @CreatedBy, @LastModifiedBy |
| `reactive/auditing/` | Reactive 환경 감사              |

### 도메인 모델 (domain/model/)

| 모델                    | 설명              |
|-----------------------|-----------------|
| `User.kt`             | 기본 사용자 엔티티      |
| `Person.kt`           | Embedded 타입 예제  |
| `AllPossibleTypes.kt` | Cassandra 타입 매핑 |
| `VersionedEntity.kt`  | 낙관적 잠금          |

### 기타 기능

| 예제                   | 설명              |
|----------------------|-----------------|
| `udt/`               | 사용자 정의 타입 (UDT) |
| `optimisticlocking/` | 낙관적 잠금 패턴       |
| `projection/`        | 프로젝션 쿼리         |
| `convert/`           | 커스텀 컨버터         |
| `event/`             | 도메인 이벤트         |
| `streamnullable/`    | Nullable 스트림 처리 |

## 주요 학습 포인트

### Entity 정의

```kotlin
@Table
data class User(
    @Id val id: UUID?,
    val name: String,
    val email: String
)
```

### Repository

```kotlin
interface UserRepository : CassandraRepository<User, UUID> {
    fun findByEmail(email: String): User?
}
```

### Coroutines 지원

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
./gradlew :examples:cassandra:test
```

## 참고

- [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra)
- [Apache Cassandra](https://cassandra.apache.org/)
