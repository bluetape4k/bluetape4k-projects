# Module bluetape4k-aws-kotlin-kinesis

AWS SDK for Kotlin Kinesis(Amazon Kinesis Data Streams) 사용을 위한 확장 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 합니다.

## 주요 기능

- **Kinesis Client 확장 함수**: 스트림 관리/레코드 송수신 보조 API
- **요청 모델 빌더 지원**: PutRecord/PutRecords/GetShardIterator 요청 생성 유틸
- **스트림 관리**: 스트림 생성/삭제/상태 조회
- **단일 레코드 전송**: putRecord API로 파티션 키와 데이터 전송
- **배치 레코드 전송**: putRecords API로 복수 레코드 배치 전송
- **샤드 조회**: getShardIterator로 샤드 이터레이터 조회
- **레코드 폴링**: getRecords로 샤드에서 레코드 조회

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-aws-kotlin-kinesis:${version}")
}
```

## 사용 예시

### 클라이언트 생성

```kotlin
import io.bluetape4k.aws.kotlin.kinesis.kinesisClientOf
import aws.smithy.kotlin.runtime.net.url.Url

val kinesisClient = kinesisClientOf(
    endpointUrl = Url.parse("http://localhost:4566"),
    region = "ap-northeast-2",
    credentialsProvider = credentialsProvider
)
```

### 스트림 관리

```kotlin
import io.bluetape4k.aws.kotlin.kinesis.*

// 스트림 생성
val createResponse = kinesisClient.createStream("my-stream", shardCount = 1)

// 스트림 상세 정보 조회
val describeResponse = kinesisClient.describeStream("my-stream")
println("StreamStatus: ${describeResponse.streamDescription?.streamStatus}")
println("ShardCount: ${describeResponse.streamDescription?.shards?.size}")

// 스트림 삭제
kinesisClient.deleteStream("my-stream")
```

### 단일 레코드 전송

```kotlin
import io.bluetape4k.aws.kotlin.kinesis.*

val response = kinesisClient.putRecord(
    streamName = "my-stream",
    partitionKey = "user-123",
    data = "Hello Kinesis!".toByteArray()
)

println("SequenceNumber: ${response.sequenceNumber}")
println("ShardId: ${response.shardId}")
```

### 배치 레코드 전송

```kotlin
import io.bluetape4k.aws.kotlin.kinesis.model.*

val entries = (1..5).map { index ->
    putRecordsRequestEntryOf(
        partitionKey = "user-$index",
        data = "message-$index".toByteArray()
    )
}

val batchResponse = kinesisClient.putRecords("my-stream", entries)

println("FailedRecordCount: ${batchResponse.failedRecordCount}")
batchResponse.records?.forEach { record ->
    println("SequenceNumber: ${record.sequenceNumber}, ShardId: ${record.shardId}")
}
```

### 레코드 조회

```kotlin
import io.bluetape4k.aws.kotlin.kinesis.*
import aws.sdk.kotlin.services.kinesis.model.ShardIteratorType

// 샤드 이터레이터 조회
val iteratorResponse = kinesisClient.getShardIterator(
    streamName = "my-stream",
    shardId = "shardId-000000000000",
    type = ShardIteratorType.TrimHorizon  // 스트림 처음부터 읽기
)

val shardIterator = iteratorResponse.shardIterator

// 레코드 조회
val recordsResponse = kinesisClient.getRecords(
    shardIterator = shardIterator,
    limit = 100
)

recordsResponse.records?.forEach { record ->
    println("PartitionKey: ${record.partitionKey}")
    println("Data: ${record.data.decodeToString()}")
    println("SequenceNumber: ${record.sequenceNumber}")
}

// 다음 이터레이터 (레코드를 계속 폴링할 경우)
val nextIterator = recordsResponse.nextShardIterator
```

### ShardIteratorType 지정

```kotlin
import aws.sdk.kotlin.services.kinesis.model.ShardIteratorType
import io.bluetape4k.aws.kotlin.kinesis.*

// TrimHorizon: 스트림 가장 처음부터 읽기
val resp1 = kinesisClient.getShardIterator(
    streamName = "my-stream",
    shardId = "shardId-000000000000",
    type = ShardIteratorType.TrimHorizon
)

// Latest: 스트림 가장 최근부터 읽기
val resp2 = kinesisClient.getShardIterator(
    streamName = "my-stream",
    shardId = "shardId-000000000000",
    type = ShardIteratorType.Latest
)

// AtTimestamp: 특정 시간 이후부터 읽기
val resp3 = kinesisClient.getShardIterator(
    streamName = "my-stream",
    shardId = "shardId-000000000000",
    type = ShardIteratorType.AtTimestamp
) {
    timestamp = System.currentTimeMillis() / 1000  // UNIX timestamp (초)
}

// AtSequenceNumber: 특정 시퀀스 번호 이후부터 읽기
val resp4 = kinesisClient.getShardIterator(
    streamName = "my-stream",
    shardId = "shardId-000000000000",
    type = ShardIteratorType.AtSequenceNumber
) {
    startingSequenceNumber = "49590338271490256608559692538361571095921575989136588898"
}

// AfterSequenceNumber: 특정 시퀀스 번호 다음부터 읽기
val resp5 = kinesisClient.getShardIterator(
    streamName = "my-stream",
    shardId = "shardId-000000000000",
    type = ShardIteratorType.AfterSequenceNumber
) {
    startingSequenceNumber = "49590338271490256608559692538361571095921575989136588898"
}
```

### 요청 빌더 사용

```kotlin
import io.bluetape4k.aws.kotlin.kinesis.model.*

// putRecordRequestOf 빌더
val putReq = putRecordRequestOf(
    streamName = "my-stream",
    partitionKey = "user-123",
    data = "Hello".toByteArray()
) {
    // 추가 옵션 설정 (필요시)
    explicitHashKey = "your-hash-key"
}

// putRecordsRequestEntryOf 빌더
val entry = putRecordsRequestEntryOf(
    partitionKey = "user-456",
    data = "World".toByteArray()
) {
    // 추가 옵션 설정 (필요시)
    explicitHashKey = "your-hash-key"
}

// getShardIteratorRequestOf 빌더
val iterReq = getShardIteratorRequestOf(
    streamName = "my-stream",
    shardId = "shardId-000000000000",
    type = ShardIteratorType.Latest
) {
    // 추가 옵션 설정
}
```

## 주요 기능 상세

| 파일                          | 설명                              |
|-------------------------------|----------------------------------|
| `KinesisClientExtensions.kt`  | Kinesis 클라이언트 확장 함수       |
| `model/PutRecord.kt`          | PutRecord 요청 빌더               |
| `model/PutRecords.kt`         | PutRecords 요청 항목 빌더          |
| `model/GetShardIterator.kt`   | GetShardIterator 요청 빌더        |

## 관련 모듈

- `bluetape4k-aws-kotlin-core`: AWS Kotlin SDK 공통 유틸리티
- `bluetape4k-aws-kinesis`: AWS Java SDK v2 기반 Kinesis 확장
