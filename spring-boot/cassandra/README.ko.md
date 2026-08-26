# Module bluetape4k-spring-boot-cassandra

[English](./README.md) | 한국어

Spring Data Cassandra 기반 개발에서 자주 쓰는 코루틴 확장, 편의 DSL, 스키마 유틸을 제공합니다 (Spring Boot 4.x).

> Spring Boot 4 기반 versionless 표준 구현입니다.

## 주요 기능

- `ReactiveSession`/`ReactiveCassandraOperations`/`AsyncCassandraOperations` 코루틴 확장
- CQL 옵션(`QueryOptions`, `WriteOptions` 등) DSL 헬퍼
- 스키마 생성/트렁케이트 유틸 (`SchemaGenerator`)

## 아키텍처 다이어그램

### 핵심 확장 함수와 클래스 구조

![Spring Boot Cassandra 핵심 확장 함수와 클래스 구조 다이어그램](../../docs/images/readme-diagrams/spring-boot-cassandra-diagram-01.png)

### Cassandra 데이터 접근 계층

![Spring Boot Cassandra 데이터 접근 계층 다이어그램](../../docs/images/readme-diagrams/spring-boot-cassandra-diagram-02.png)

### 코루틴 변환 흐름

![Spring Boot Cassandra 코루틴 변환 시퀀스 다이어그램](../../docs/images/readme-diagrams/spring-boot-cassandra-sequence-01.png)

## 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-spring-boot-cassandra:${bluetape4kVersion}")
}
```

## 사용 예시

### 코루틴 확장

```kotlin
val result = reactiveSession.executeSuspending("SELECT * FROM users WHERE id = ?", id)
```

### WriteOptions DSL

```kotlin
import java.util.concurrent.TimeUnit

val options = writeOptions {
    ttl(Duration.ofSeconds(30))
    timestamp(TimeUnit.MILLISECONDS.toMicros(System.currentTimeMillis()))
}
```

`WriteOptions`는 Cassandra 문장별 다음 계약을 따릅니다.

| 입력 | 결과 |
| --- | --- |
| `ttl == null` | TTL 절을 렌더링하지 않습니다. |
| `ttl == Duration.ZERO` 또는 `1ms`/`500ms` 같은 subsecond duration | whole-second 값으로 절삭하며 TTL 0을 렌더링합니다(`INSERT`는 `USING TTL 0`, `UPDATE`는 `AND TTL 0`). |
| 음수 TTL | Spring Data builder가 `IllegalArgumentException("TTL must be greater than equal to zero")`로 실패합니다. |
| `Int` 범위를 벗어난 TTL 초 | 문장을 실행하기 전에 `addWriteOptions`가 `ArithmeticException`으로 실패합니다. |
| `timestamp` | Cassandra microseconds 단위로 적용합니다. `Delete`는 timestamp를 보존하지만 TTL은 적용하지 않습니다. |

기존 `isPositiveTtl` 확장은 이름과 달리 0을 포함한 모든 non-negative TTL에서 `true`, TTL이 없을 때 `false`를 반환합니다.

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
./gradlew :bluetape4k-spring-boot-cassandra:test
```

## 참고

- [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra)
- [Apache Cassandra](https://cassandra.apache.org/)
