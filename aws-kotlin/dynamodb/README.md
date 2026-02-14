# Module bluetape4k-aws-kotlin-dynamodb

AWS SDK for Kotlin DynamoDB 사용을 단순화하는 확장 라이브러리입니다.

## 주요 기능

- **DynamoDbClient 확장 함수**: DynamoDB 호출 편의 API
- **요청/모델 빌더 지원**: 주요 DynamoDB 요청 객체 생성 유틸
- **AttributeValue 변환 지원**: 값 매핑 보조
- **Reserved Words 지원**: DynamoDB 예약어 상수 제공

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-dynamodb:${version}")
}
```

## 주요 기능 상세

- `DynamoDbClientExtensions.kt`
- `model/*.kt` (CreateTable, PutItem, Query, TransactWriteItem 등)
- `ReservedWords.kt`
