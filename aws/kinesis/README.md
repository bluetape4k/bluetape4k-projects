# Module bluetape4k-aws-kinesis

AWS SDK for Java v2 Kinesis(Amazon Kinesis) 사용을 위한 확장 라이브러리입니다.

## 개요

Amazon Kinesis는 실시간 데이터 스트리밍을 처리하는 AWS 관리형 서비스입니다.
이 모듈은 AWS SDK for Java v2의 `KinesisClient`(동기)와 `KinesisAsyncClient`(비동기)를 보다 편리하게
생성하고 사용할 수 있도록 Kotlin 스타일의 DSL 팩토리 함수와 Request 빌더 확장을 제공합니다.

## 주요 기능

- `kinesisClient()` / `kinesisClientOf()` - Kotlin DSL 스타일의 동기식 `KinesisClient` 팩토리 함수
- `kinesisAsyncClient()` / `kinesisAsyncClientOf()` - Kotlin DSL 스타일의 비동기식 `KinesisAsyncClient` 팩토리 함수
- **Stream 관리**: 스트림 생성/삭제/조회 지원
- **레코드 전송**: 단일 레코드 전송 및 배치 레코드 전송(최대 500건)
- **레코드 조회**: 샤드 이터레이터 기반 폴링 지원
- **Coroutine 브릿지**: Async API의 suspend 사용 지원
- `model/` 패키지 - 모든 Kinesis Request 타입에 대한 Kotlin DSL 빌더 함수 제공
- JVM 종료 시 클라이언트 자동 종료 (`ShutdownQueue` 등록)
- LocalStack 기반 통합 테스트 지원 (`AbstractKinesisTest`)

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-aws-kinesis:$version")
}
```

## 사용 방법

### KinesisClient 생성 (동기)

```kotlin
import io.bluetape4k.aws.kinesis.kinesisClient
import io.bluetape4k.aws.kinesis.kinesisClientOf
import software.amazon.awssdk.regions.Region
import java.net.URI

// DSL 빌더 방식
val client = kinesisClient {
    region(Region.AP_NORTHEAST_2)
}

// 파라미터 직접 지정 방식 (LocalStack 등 로컬 환경)
val localClient = kinesisClientOf(
    endpointOverride = URI.create("http://localhost:4566"),
    region = Region.US_EAST_1,
    credentialsProvider = myCredentialsProvider
)
```

### KinesisAsyncClient 생성 (비동기)

```kotlin
import io.bluetape4k.aws.kinesis.kinesisAsyncClient
import io.bluetape4k.aws.kinesis.kinesisAsyncClientOf
import kotlinx.coroutines.future.await

val asyncClient = kinesisAsyncClientOf(
    endpointOverride = URI.create("http://localhost:4566"),
    region = Region.US_EAST_1,
    credentialsProvider = myCredentialsProvider
)

// CompletableFuture → Coroutines 변환
val response = asyncClient.putRecord("my-stream", "pk", data).await()
```

### Stream 생성 및 삭제

```kotlin
import io.bluetape4k.aws.kinesis.*

val client: KinesisClient = // ...

// 스트림 생성 (샤드 1개)
val createResp = client.createStream("my-stream", shardCount = 1)

// 스트림 상세 정보 조회
val describeResp = client.describeStream("my-stream")
val shards = describeResp.streamDescription().shards()

// 스트림 삭제
client.deleteStream("my-stream")
```

### 단일 레코드 전송

```kotlin
import io.bluetape4k.aws.kinesis.*
import software.amazon.awssdk.core.SdkBytes

val client: KinesisClient = // ...

// 단일 레코드 전송
val data = SdkBytes.fromUtf8String("hello kinesis")
val response = client.putRecord(
    streamName = "my-stream",
    partitionKey = "partition-1",
    data = data
)

println("sequenceNumber=${response.sequenceNumber()}")
println("shardId=${response.shardId()}")
```

### 배치 레코드 전송

```kotlin
import io.bluetape4k.aws.kinesis.*
import io.bluetape4k.aws.kinesis.model.putRecordsRequestEntryOf
import software.amazon.awssdk.core.SdkBytes

val client: KinesisClient = // ...

// 배치 레코드 생성 (최대 500건)
val entries = (1..10).map { i ->
    putRecordsRequestEntryOf(
        partitionKey = "partition-$i",
        data = SdkBytes.fromUtf8String("message-$i")
    )
}

// 배치 전송
val response = client.putRecords("my-stream", entries)

// 실패한 레코드 확인
response.records().forEachIndexed { index, record ->
    if (record.errorCode() != null) {
        println("Record $index failed: ${record.errorMessage()}")
    }
}
```

### 샤드 이터레이터 조회 및 레코드 폴링

```kotlin
import io.bluetape4k.aws.kinesis.*
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType

val client: KinesisClient = // ...

// 스트림에서 샤드 ID 조회
val describeResp = client.describeStream("my-stream")
val shardId = describeResp.streamDescription().shards()[0].shardId()

// 샤드 이터레이터 조회 (TRIM_HORIZON: 처음부터 읽기)
val iterResp = client.getShardIterator(
    streamName = "my-stream",
    shardId = shardId,
    type = ShardIteratorType.TRIM_HORIZON
)

// 레코드 폴링
var shardIterator = iterResp.shardIterator()
while (shardIterator != null) {
    val records = client.getRecords(shardIterator, limit = 100)

    records.records().forEach { record ->
        println("data=${record.data().asUtf8String()}")
    }

    shardIterator = records.nextShardIterator()
}
```

### Coroutine 기반 사용

```kotlin
import io.bluetape4k.aws.kinesis.*
import io.bluetape4k.aws.kinesis.model.putRecordsRequestEntryOf
import software.amazon.awssdk.core.SdkBytes

val asyncClient: KinesisAsyncClient = // ...

suspend fun createStreamAndPutRecord() {
    // 스트림 생성
    asyncClient.createStream("my-stream", shardCount = 1)

    // 단일 레코드 전송
    val response = asyncClient.putRecord(
        streamName = "my-stream",
        partitionKey = "pk-1",
        data = SdkBytes.fromUtf8String("hello")
    )
    println("sequenceNumber=${response.sequenceNumber()}")

    // 배치 레코드 전송
    val entries = (1..5).map { i ->
        putRecordsRequestEntryOf(
            partitionKey = "pk-$i",
            data = SdkBytes.fromUtf8String("msg-$i")
        )
    }
    asyncClient.putRecords("my-stream", entries)

    // 스트림 삭제
    asyncClient.deleteStream("my-stream")
}
```

## Request 빌더 함수 목록

| 함수 | 설명 |
|------|------|
| `createStreamRequest` / `createStreamRequestOf` | Stream 생성 요청 |
| `deleteStreamRequest` / `deleteStreamRequestOf` | Stream 삭제 요청 |
| `describeStreamRequest` / `describeStreamRequestOf` | Stream 상세 조회 요청 |
| `putRecordRequest` / `putRecordRequestOf` | 단일 레코드 전송 요청 |
| `putRecordsRequest` / `putRecordsRequestOf` | 배치 레코드 전송 요청 |
| `putRecordsRequestEntryOf` | 배치 엔트리 생성 |
| `getRecordsRequest` / `getRecordsRequestOf` | 레코드 조회 요청 |
| `getShardIteratorRequest` / `getShardIteratorRequestOf` | 샤드 이터레이터 조회 요청 |

## 주요 기능 상세

| 파일                                      | 설명                   |
|------------------------------------------|----------------------|
| `KinesisClientFactory.kt`                | Kinesis 클라이언트 팩토리        |
| `KinesisClientSupport.kt`                | 동기 클라이언트 생성          |
| `KinesisAsyncClientSupport.kt`           | 비동기 클라이언트 생성         |
| `KinesisClientExtensions.kt`             | 동기 클라이언트 확장 함수       |
| `KinesisAsyncClientExtensions.kt`        | 비동기 클라이언트 확장 함수      |
| `KinesisAsyncClientCoroutinesExtensions.kt` | 코루틴 확장 함수            |
| `model/CreateStream.kt`                  | Stream 생성 빌더        |
| `model/DeleteStream.kt`                  | Stream 삭제 빌더        |
| `model/DescribeStream.kt`                | Stream 상세 조회 빌더   |
| `model/PutRecord.kt`                     | 단일 레코드 전송 빌더        |
| `model/PutRecords.kt`                    | 배치 레코드 전송 빌더        |
| `model/GetRecords.kt`                    | 레코드 조회 빌더           |
| `model/GetShardIterator.kt`              | 샤드 이터레이터 조회 빌더 |

## 샤드 이터레이터 타입

Kinesis는 다음과 같은 샤드 이터레이터 타입을 지원합니다:

| 타입 | 설명 |
|------|------|
| `TRIM_HORIZON` | 가장 오래된 레코드부터 읽기 |
| `LATEST` | 가장 최신 레코드부터 읽기 |
| `AT_TIMESTAMP` | 특정 타임스탐프 이후 레코드부터 읽기 |
| `AFTER_SEQUENCE_NUMBER` | 특정 시퀀스 번호 이후 레코드부터 읽기 |
| `AT_SEQUENCE_NUMBER` | 특정 시퀀스 번호의 레코드부터 읽기 |

## 테스트

이 모듈은 [LocalStack](https://localstack.cloud/)을 사용한 통합 테스트를 제공합니다.
테스트 실행 시 Docker가 설치되어 있어야 합니다.

```bash
# 특정 모듈 테스트 실행
./gradlew :bluetape4k-aws-kinesis:test

# 동기 클라이언트 테스트
./gradlew :bluetape4k-aws-kinesis:test --tests "*KinesisClientTest"

# 비동기/코루틴 클라이언트 테스트
./gradlew :bluetape4k-aws-kinesis:test --tests "*KinesisAsyncClientTest"
```

## 관련 모듈

| 모듈 | 설명 |
|------|------|
| `bluetape4k-aws-core` | AWS Java SDK v2 공통 유틸리티 |
| `bluetape4k-aws-kotlin-kinesis` | AWS Kotlin SDK 기반 Kinesis 확장 (native suspend 지원) |

## Java SDK vs Kotlin SDK 비교

| 기능 | `bluetape4k-aws-kinesis` (Java SDK) | `bluetape4k-aws-kotlin-kinesis` (Kotlin SDK) |
|------|------|------|
| 동기 클라이언트 | `KinesisClient` | - |
| 비동기 클라이언트 | `KinesisAsyncClient` + `CompletableFuture` | `KinesisClient` (suspend) |
| Coroutines 지원 | `.await()` 변환 필요 | 기본 제공 |
| Request 빌더 | `model/` 패키지 DSL 제공 | AWS SDK 내장 DSL 사용 |
