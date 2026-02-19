# Module bluetape4k-aws-dynamodb

AWS SDK for Java v2 DynamoDB 사용을 단순화하는 확장 라이브러리입니다.

## 주요 기능

- **Client 생성 보조**: `DynamoDbClient`, `DynamoDbAsyncClient` 팩토리
- **Enhanced Client 확장**: `DynamoDbEnhancedClient`/`Table` 보조 함수
- **모델/요청 빌더 지원**: Key, AttributeValue, Request 생성 유틸
- **Query DSL 지원**: 조건/필터/정렬키 DSL
- **Repository 지원**: 코루틴 기반 DynamoDB Repository 패턴
- **배치 처리 지원**: Batch 실행기 (Resilience4j Retry 연동)

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-dynamodb:${version}")
}
```

## 사용 예시

### Repository 패턴

```kotlin
import io.bluetape4k.aws.dynamodb.repository.DynamoDbCoroutineRepository
import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity

data class User(
    override val partitionKey: String,
    val name: String,
    val email: String,
): DynamoDbEntity

class UserRepository(
    override val client: DynamoDbEnhancedAsyncClient,
    override val table: DynamoDbAsyncTable<User>,
    override val itemClass: Class<User>,
): DynamoDbCoroutineRepository<User> {

    suspend fun findByEmail(email: String): User? {
        return findFirstByPartitionKey(email).firstOrNull()
    }
}

// 사용
suspend fun example(repo: UserRepository) {
    val user = User("user#123", "John", "john@example.com")
    repo.save(user)

    val found = repo.findByKey(keyOf("user#123"))
    repo.delete(user)
}
```

### Query DSL

```kotlin
import io.bluetape4k.aws.dynamodb.query.queryRequest
import io.bluetape4k.aws.dynamodb.query.primaryKey
import io.bluetape4k.aws.dynamodb.query.sortKey
import io.bluetape4k.aws.dynamodb.query.filtering

val request = queryRequest {
    tableName = "Users"
    primaryKey("pk") {
        equals = "USER#123"
    }
    sortKey("sk") {
        comparisonOperator = SortKeyOperator.BEGINS_WITH
        value = "PROFILE#"
    }
    filtering {
        attribute("email").beginsWith("john")
        attribute("status").eq("ACTIVE")
    }
}

val response = dynamoDbClient.query(request)
```

### 배치 처리

```kotlin
import io.bluetape4k.aws.dynamodb.DynamoDbBatchExecutor
import io.github.resilience4j.retry.Retry

val executor = DynamoDbBatchExecutor<User>(
    dynamoDB = dynamoDbClient,
    retry = Retry.ofDefaults("dynamo-batch")
)

// 대량 저장
suspend fun batchSave(users: List<User>) {
    executor.persist("Users", users) { user ->
        mapOf(
            "pk" to AttributeValue.fromS(user.partitionKey),
            "name" to AttributeValue.fromS(user.name),
            "email" to AttributeValue.fromS(user.email)
        )
    }
}
```

## 주요 기능 상세

| 파일                                               | 설명                   |
|--------------------------------------------------|----------------------|
| `DynamoDbClientSupport.kt`                       | 동기 클라이언트 생성          |
| `DynamoDbAsyncClientSupport.kt`                  | 비동기 클라이언트 생성         |
| `DynamoDbStreamClientSupport.kt`                 | Stream 동기 클라이언트      |
| `DynamoDbStreamsAsyncClientSupport.kt`           | Stream 비동기 클라이언트     |
| `enhanced/DynamoDbEnhancedClientSupport.kt`      | Enhanced 클라이언트 생성    |
| `enhanced/DynamoDbEnhancedAsyncClientSupport.kt` | Enhanced 비동기 클라이언트   |
| `enhanced/*Extensions.kt`                        | Enhanced 클라이언트 확장 함수 |
| `model/AttributeValueSupport.kt`                 | AttributeValue 빌더    |
| `model/DynamoDbKeySupport.kt`                    | Key 빌더               |
| `model/RequestSupport.kt`                        | 요청 빌더                |
| `query/QueryDslSupport.kt`                       | Query DSL            |
| `query/ConditionDslSupport.kt`                   | 조건 DSL               |
| `query/FilterQueryDslSupport.kt`                 | 필터 DSL               |
| `repository/DynamoDbCoroutineRepository.kt`      | 코루틴 Repository       |
| `repository/PublisherSupport.kt`                 | Reactor Publisher 지원 |
| `schema/TableSchemaSupport.kt`                   | 테이블 스키마              |
| `schema/DynamoDbAsyncTableSupport.kt`            | 비동기 테이블              |
| `DynamoDbBatchExecutor.kt`                       | 배치 처리 실행기            |
| `DynamoItemMapper.kt`                            | 아이템 매퍼               |
| `ReservedWords.kt`                               | DynamoDB 예약어         |
