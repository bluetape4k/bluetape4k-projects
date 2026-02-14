# Module bluetape4k-aws-dynamodb

AWS SDK for Java v2 DynamoDB 사용을 단순화하는 확장 라이브러리입니다.

## 주요 기능

- **Client 생성 보조**: `DynamoDbClient`, `DynamoDbAsyncClient` 팩토리
- **Enhanced Client 확장**: `DynamoDbEnhancedClient`/`Table` 보조 함수
- **모델/요청 빌더 지원**: Key, AttributeValue, Request 생성 유틸
- **Query DSL 지원**: 조건/필터/정렬키 DSL
- **Repository 지원**: 코루틴 기반 DynamoDB Repository 패턴
- **배치 처리 지원**: Batch 실행기

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-dynamodb:${version}")
}
```

## 주요 기능 상세

- `DynamoDbClientSupport.kt`, `DynamoDbAsyncClientSupport.kt`
- `enhanced/*Extensions.kt`, `enhanced/*Support.kt`
- `model/*Support.kt`
- `query/*DslSupport.kt`
- `repository/DynamoDbCoroutineRepository.kt`
- `DynamoDbBatchExecutor.kt`
