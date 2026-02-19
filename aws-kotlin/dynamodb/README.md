# Module bluetape4k-aws-kotlin-dynamodb

AWS SDK for Kotlin DynamoDB 사용을 단순화하는 확장 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 합니다.

## 주요 기능

- **DynamoDbClient 확장 함수**: DynamoDB 호출 편의 API
- **요청/모델 빌더 지원**: 주요 DynamoDB 요청 객체 생성 유틸
- **AttributeValue 변환 지원**: 값 매핑 보조
- **Reserved Words 지원**: DynamoDB 예약어 상수 제공
- **배치 처리 지원**: Batch Write Executor (Resilience4j Retry 연동)

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-dynamodb:${version}")
}
```

## 사용 예시

### 클라이언트 생성

```kotlin
import io.bluetape4k.aws.kotlin.dynamodb.dynamoDbClientOf
import aws.smithy.kotlin.runtime.net.url.Url

val client = dynamoDbClientOf(
    endpointUrl = Url.parse("http://localhost:4566"),
    region = "ap-northeast-2",
    credentialsProvider = credentialsProvider
)
```

### 테이블 관리

```kotlin
import io.bluetape4k.aws.kotlin.dynamodb.*

// 테이블 존재 확인
val exists = client.existsTable("Users")

// 테이블 생성
val response = client.createTable(
    tableName = "Users",
    keySchema = listOf(
        KeySchemaElement { attributeName = "pk"; keyType = KeyType.Hash },
        KeySchemaElement { attributeName = "sk"; keyType = KeyType.Range }
    ),
    attributeDefinitions = listOf(
        AttributeDefinition { attributeName = "pk"; attributeType = ScalarAttributeType.S },
        AttributeDefinition { attributeName = "sk"; attributeType = ScalarAttributeType.S }
    ),
    readCapacityUnits = 5,
    writeCapacityUnits = 5
)

// 테이블 준비 대기
client.waitForTableReady("Users", timeout = 60.seconds)

// 테이블 삭제
client.deleteTableIfExists("Users")
```

### 아이템 조작

```kotlin
import io.bluetape4k.aws.kotlin.dynamodb.model.toAttributeValueMap

// 아이템 저장
client.putItem(
    tableName = "Users",
    item = mapOf(
        "pk" to "USER#123",
        "sk" to "PROFILE",
        "name" to "John Doe",
        "email" to "john@example.com"
    )
)

// Scan 페이지네이션
val scanFlow = client.scanPaginated(
    tableName = "Users",
    exclusiveStartKey = emptyMap(),
    limit = 100
)
scanFlow.collect { response ->
    response.items?.forEach { item ->
        println(item)
    }
}
```

### 배치 처리

```kotlin
import io.bluetape4k.aws.kotlin.dynamodb.DynamoDbBatchExecutor
import io.github.resilience4j.retry.Retry

val executor = DynamoDbBatchExecutor<User>(
    client = client,
    retry = Retry.ofDefaults("dynamo-batch"),
    maxUnprocessedRetry = 10
)

// 대량 저장
suspend fun batchSave(users: List<User>) {
    executor.putAll("Users", users) { user ->
        mapOf(
            "pk" to AttributeValue.S(user.id),
            "sk" to AttributeValue.S("PROFILE"),
            "name" to AttributeValue.S(user.name),
            "email" to AttributeValue.S(user.email)
        )
    }
}

// 대량 삭제
suspend fun batchDelete(users: List<User>) {
    executor.deleteAll("Users", users) { user ->
        mapOf("pk" to AttributeValue.S(user.id), "sk" to AttributeValue.S("PROFILE"))
    }
}
```

## 주요 기능 상세

| 파일                               | 설명                                |
|----------------------------------|-----------------------------------|
| `DynamoDbClientExtensions.kt`    | 클라이언트 확장 함수 (테이블 관리, 아이템 조작)      |
| `DynamoDbBatchExecutor.kt`       | 배치 처리 실행기 (Resilience4j Retry 연동) |
| `DynamoItemMapper.kt`            | 아이템 매퍼 인터페이스                      |
| `Defaults.kt`                    | 기본값 상수                            |
| `ReservedWords.kt`               | DynamoDB 예약어 목록                   |
| `model/AttributeValue.kt`        | AttributeValue 변환 유틸              |
| `model/JacksonAttributeValue.kt` | Jackson 기반 AttributeValue 변환      |
| `model/CreateTable.kt`           | CreateTable 요청 빌더                 |
| `model/PutItem.kt`               | PutItem 요청 빌더                     |
| `model/Query.kt`                 | Query 요청 빌더                       |
| `model/Scan.kt`                  | Scan 요청 빌더                        |
| `model/BatchWriteItem.kt`        | BatchWriteItem 요청 빌더              |
| `model/TransactWriteItem.kt`     | 트랜잭션 쓰기 빌더                        |
| `model/GetItemRequest.kt`        | GetItem 요청 빌더                     |
