# Module bluetape4k-aws-cloudwatch

AWS SDK for Java v2 CloudWatch(Amazon CloudWatch) 및 CloudWatch Logs 사용을 위한 확장 라이브러리입니다.

## 개요

AWS CloudWatch는 AWS 리소스 및 애플리케이션 메트릭을 수집, 모니터링, 분석하는 AWS 관리형 서비스입니다.
이 모듈은 AWS SDK for Java v2의 `CloudWatchClient`(동기), `CloudWatchAsyncClient`(비동기),
`CloudWatchLogsClient`(동기), `CloudWatchLogsAsyncClient`(비동기)를 보다 편리하게 생성하고
사용할 수 있도록 Kotlin 스타일의 DSL 팩토리 함수와 Request 빌더 확장을 제공합니다.

## 주요 기능

### CloudWatch 메트릭 (CloudWatch)
- `CloudWatchClient` / `CloudWatchAsyncClient` 팩토리 함수
- 메트릭 데이터 발행: `putMetricData()` (단일/배치)
- 메트릭 조회: `listMetrics()` (namespace/metricName/dimensions 필터)
- Coroutine 브릿지: Async API의 suspend 사용 지원

### CloudWatch Logs
- `CloudWatchLogsClient` / `CloudWatchLogsAsyncClient` 팩토리 함수
- 로그 그룹 생성: `createLogGroup()`
- 로그 스트림 생성: `createLogStream()`
- 로그 이벤트 발행: `putLogEvents()`
- 로그 그룹/스트림 조회: `describeLogGroups()`, `describeLogStreams()`
- Coroutine 브릿지: Async API의 suspend 사용 지원

### 공통 기능
- JVM 종료 시 클라이언트 자동 종료 (`ShutdownQueue` 등록)
- LocalStack 기반 통합 테스트 지원

## 의존성

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-aws-cloudwatch:$version")
}
```

## 사용 방법

### CloudWatchClient 생성 (동기)

```kotlin
import io.bluetape4k.aws.cloudwatch.CloudWatchClientFactory
import software.amazon.awssdk.regions.Region
import java.net.URI

// DSL 빌더 방식
val client = CloudWatchClientFactory.Sync.create {
    region(Region.AP_NORTHEAST_2)
}

// 파라미터 직접 지정 방식 (LocalStack 등 로컬 환경)
val localClient = CloudWatchClientFactory.Sync.create(
    endpointOverride = URI.create("http://localhost:4566"),
    region = Region.US_EAST_1,
    credentialsProvider = myCredentialsProvider
)
```

### CloudWatchAsyncClient 생성 (비동기)

```kotlin
import io.bluetape4k.aws.cloudwatch.CloudWatchClientFactory
import kotlinx.coroutines.future.await

val asyncClient = CloudWatchClientFactory.Async.create(
    endpointOverride = URI.create("http://localhost:4566"),
    region = Region.US_EAST_1
)

// CompletableFuture → Coroutines 변환
val response = asyncClient.putMetricData(namespace, datum).await()
```

### 메트릭 데이터 발행 (동기)

```kotlin
import io.bluetape4k.aws.cloudwatch.CloudWatchClientFactory
import io.bluetape4k.aws.cloudwatch.putMetricData
import io.bluetape4k.aws.cloudwatch.model.metricDatumOf
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit

val client = CloudWatchClientFactory.Sync.create {
    region(Region.US_EAST_1)
}

// 단일 메트릭 발행
val datum = metricDatumOf(
    metricName = "Latency",
    value = 100.0,
    unit = StandardUnit.MILLISECONDS
)
val response = client.putMetricData("MyApp/Performance", datum)

// 배치 메트릭 발행
val data = listOf(
    metricDatumOf("Latency", 100.0, StandardUnit.MILLISECONDS),
    metricDatumOf("RequestCount", 50.0, StandardUnit.COUNT),
    metricDatumOf("ErrorCount", 5.0, StandardUnit.COUNT),
)
val batchResponse = client.putMetricData("MyApp/Performance", data)
```

### 메트릭 조회 (동기)

```kotlin
import io.bluetape4k.aws.cloudwatch.listMetrics

val response = client.listMetrics(namespace = "MyApp/Performance")
response.metrics().forEach { metric ->
    println("Metric: ${metric.metricName()}")
}

// 메트릭 이름으로 필터링
val filtered = client.listMetrics(
    namespace = "MyApp/Performance",
    metricName = "Latency"
)
```

### 메트릭 코루틴 발행 (비동기)

```kotlin
import io.bluetape4k.aws.cloudwatch.CloudWatchClientFactory
import io.bluetape4k.aws.cloudwatch.putMetricData
import io.bluetape4k.aws.cloudwatch.model.metricDatumOf
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit

val asyncClient = CloudWatchClientFactory.Async.create(
    region = Region.US_EAST_1
)

suspend fun publishMetric() {
    val datum = metricDatumOf(
        metricName = "Latency",
        value = 100.0,
        unit = StandardUnit.MILLISECONDS
    )
    val response = asyncClient.putMetricData("MyApp/Performance", datum)
}

suspend fun publishBatchMetrics() {
    val data = listOf(
        metricDatumOf("Latency", 100.0, StandardUnit.MILLISECONDS),
        metricDatumOf("RequestCount", 50.0, StandardUnit.COUNT),
    )
    val response = asyncClient.putMetricData("MyApp/Performance", data)
}

suspend fun listMetricsAsync() {
    val response = asyncClient.listMetrics(namespace = "MyApp/Performance")
    response.metrics().forEach { metric ->
        println("Metric: ${metric.metricName()}")
    }
}
```

### CloudWatchLogsClient 생성

```kotlin
import io.bluetape4k.aws.cloudwatch.CloudWatchLogsClientFactory
import software.amazon.awssdk.regions.Region

// 동기 클라이언트
val logsClient = CloudWatchLogsClientFactory.Sync.create(
    region = Region.US_EAST_1
)

// 비동기 클라이언트
val asyncLogsClient = CloudWatchLogsClientFactory.Async.create(
    region = Region.US_EAST_1
)
```

### 로그 그룹 및 스트림 관리 (동기)

```kotlin
import io.bluetape4k.aws.cloudwatch.createLogGroup
import io.bluetape4k.aws.cloudwatch.createLogStream
import io.bluetape4k.aws.cloudwatch.describeLogGroups
import io.bluetape4k.aws.cloudwatch.describeLogStreams

// 로그 그룹 생성
val groupResponse = logsClient.createLogGroup("/my-app/logs")

// 로그 스트림 생성
val streamResponse = logsClient.createLogStream(
    logGroupName = "/my-app/logs",
    logStreamName = "my-stream"
)

// 로그 그룹 목록 조회
val groups = logsClient.describeLogGroups(logGroupNamePrefix = "/my-app")
groups.logGroups().forEach { group ->
    println("LogGroup: ${group.logGroupName()}")
}

// 로그 스트림 목록 조회
val streams = logsClient.describeLogStreams(
    logGroupName = "/my-app/logs",
    logStreamNamePrefix = "my-"
)
streams.logStreams().forEach { stream ->
    println("LogStream: ${stream.logStreamName()}")
}
```

### 로그 이벤트 발행 (동기)

```kotlin
import io.bluetape4k.aws.cloudwatch.putLogEvents
import io.bluetape4k.aws.cloudwatch.model.cloudwatchlogs.inputLogEventOf

// 로그 이벤트 생성
val events = listOf(
    inputLogEventOf(System.currentTimeMillis(), "Application started"),
    inputLogEventOf(System.currentTimeMillis() + 1, "Processing request..."),
    inputLogEventOf(System.currentTimeMillis() + 2, "Request completed"),
)

// 로그 발행
val response = logsClient.putLogEvents(
    logGroupName = "/my-app/logs",
    logStreamName = "my-stream",
    logEvents = events
)
```

### 로그 코루틴 발행 (비동기)

```kotlin
import io.bluetape4k.aws.cloudwatch.CloudWatchLogsClientFactory
import io.bluetape4k.aws.cloudwatch.putLogEvents
import io.bluetape4k.aws.cloudwatch.createLogGroup
import io.bluetape4k.aws.cloudwatch.createLogStream
import io.bluetape4k.aws.cloudwatch.model.cloudwatchlogs.inputLogEventOf
import software.amazon.awssdk.regions.Region

val asyncLogsClient = CloudWatchLogsClientFactory.Async.create(
    region = Region.US_EAST_1
)

suspend fun setupLogsAndPublish() {
    // 로그 그룹 생성
    asyncLogsClient.createLogGroup("/my-app/logs")

    // 로그 스트림 생성
    asyncLogsClient.createLogStream(
        logGroupName = "/my-app/logs",
        logStreamName = "my-stream"
    )

    // 로그 이벤트 발행
    val events = listOf(
        inputLogEventOf(System.currentTimeMillis(), "Message 1"),
        inputLogEventOf(System.currentTimeMillis() + 1, "Message 2"),
    )
    asyncLogsClient.putLogEvents(
        logGroupName = "/my-app/logs",
        logStreamName = "my-stream",
        logEvents = events
    )
}
```

## Request 빌더 함수 목록

### CloudWatch 메트릭

| 함수 | 설명 |
|------|------|
| `putMetricDataRequest` / `putMetricDataRequestOf` | PutMetricData 요청 빌더 |
| `metricDatum` / `metricDatumOf` | MetricDatum 객체 빌더 |
| `listMetricsRequest` / `listMetricsRequestOf` | ListMetrics 요청 빌더 |

### CloudWatch Logs

| 함수 | 설명 |
|------|------|
| `putLogEventsRequest` / `putLogEventsRequestOf` | PutLogEvents 요청 빌더 |
| `inputLogEvent` / `inputLogEventOf` | InputLogEvent 객체 빌더 |
| `createLogGroupRequest` / `createLogGroupRequestOf` | CreateLogGroup 요청 빌더 |
| `createLogStreamRequest` / `createLogStreamRequestOf` | CreateLogStream 요청 빌더 |

## 주요 기능 상세

| 파일 | 설명 |
|------|------|
| `CloudWatchClientFactory.kt` | CloudWatch/CloudWatchLogs 클라이언트 팩토리 |
| `CloudWatchClientSupport.kt` | 동기 CloudWatchClient 생성 보조 |
| `CloudWatchAsyncClientSupport.kt` | 비동기 CloudWatchAsyncClient 생성 보조 |
| `CloudWatchClientExtensions.kt` | 동기 메트릭 확장 함수 |
| `CloudWatchAsyncClientExtensions.kt` | 비동기 메트릭 확장 함수 |
| `CloudWatchAsyncClientCoroutinesExtensions.kt` | 코루틴 메트릭 확장 함수 |
| `CloudWatchLogsClientSupport.kt` | 동기 CloudWatchLogsClient 생성 보조 |
| `CloudWatchLogsAsyncClientSupport.kt` | 비동기 CloudWatchLogsAsyncClient 생성 보조 |
| `CloudWatchLogsClientExtensions.kt` | 동기 로그 확장 함수 |
| `CloudWatchLogsAsyncClientCoroutinesExtensions.kt` | 코루틴 로그 확장 함수 |
| `model/PutMetricData.kt` | 메트릭 데이터 요청/객체 빌더 |
| `model/ListMetrics.kt` | 메트릭 조회 요청 빌더 |
| `model/cloudwatchlogs/PutLogEvents.kt` | 로그 이벤트 요청/객체 빌더 |
| `model/cloudwatchlogs/CreateLogGroup.kt` | 로그 그룹 생성 요청 빌더 |
| `model/cloudwatchlogs/CreateLogStream.kt` | 로그 스트림 생성 요청 빌더 |

## 테스트

이 모듈은 [LocalStack](https://localstack.cloud/)을 사용한 통합 테스트를 제공합니다.
테스트 실행 시 Docker가 설치되어 있어야 합니다.

```bash
# 특정 모듈 테스트 실행
./gradlew :bluetape4k-aws-cloudwatch:test

# CloudWatch 메트릭 테스트
./gradlew :bluetape4k-aws-cloudwatch:test --tests "io.bluetape4k.aws.cloudwatch.CloudWatchClientTest"

# CloudWatch Logs 테스트
./gradlew :bluetape4k-aws-cloudwatch:test --tests "io.bluetape4k.aws.cloudwatch.CloudWatchLogsClientTest"

# 비동기 클라이언트 코루틴 테스트
./gradlew :bluetape4k-aws-cloudwatch:test --tests "io.bluetape4k.aws.cloudwatch.CloudWatchAsyncClientCoroutinesExtensionsTest"
```

## 관련 모듈

| 모듈 | 설명 |
|------|------|
| `bluetape4k-aws-core` | AWS Java SDK v2 공통 유틸리티 |
| `bluetape4k-aws-kotlin-cloudwatch` | AWS Kotlin SDK 기반 CloudWatch 확장 (native suspend 지원) |

## Java SDK vs Kotlin SDK 비교

| 기능 | `bluetape4k-aws-cloudwatch` (Java SDK) | `bluetape4k-aws-kotlin-cloudwatch` (Kotlin SDK) |
|------|------|------|
| 동기 클라이언트 | `CloudWatchClient`, `CloudWatchLogsClient` | - |
| 비동기 클라이언트 | `CloudWatchAsyncClient` + `CompletableFuture` | `CloudWatchClient` (suspend) |
| Coroutines 지원 | `.await()` 변환 필요 | 기본 제공 |
| Request 빌더 | `model/` 패키지 DSL 제공 | AWS SDK 내장 DSL 사용 |
