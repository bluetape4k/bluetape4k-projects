# Module bluetape4k-aws-kotlin-cloudwatch

AWS SDK for Kotlin CloudWatch(Amazon CloudWatch) 및 CloudWatch Logs 사용을 위한 확장 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 합니다.

## 주요 기능

- **CloudWatch Client 확장 함수**: 메트릭 발행 및 조회 보조 API
- **CloudWatch Logs Client 확장 함수**: 로그 그룹/스트림 생성 및 로그 이벤트 발행
- **메트릭 빌더 지원**: MetricDatum DSL 빌더
- **로그 이벤트 빌더 지원**: InputLogEvent DSL 빌더
- **배치 메트릭 발행**: 단일 또는 여러 메트릭 동시 발행
- **네이티브 Suspend 함수**: 모든 API가 Kotlin Coroutines 기반

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-aws-kotlin-cloudwatch:${version}")
}
```

## 사용 예시

### CloudWatch 클라이언트 생성

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.cloudWatchClientOf
import io.bluetape4k.aws.kotlin.cloudwatch.cloudWatchLogsClientOf
import aws.smithy.kotlin.runtime.net.url.Url

// CloudWatch 메트릭 클라이언트 생성
val cloudWatchClient = cloudWatchClientOf(
    endpointUrl = Url.parse("http://localhost:4566"),
    region = "ap-northeast-2",
    credentialsProvider = credentialsProvider
)

// CloudWatch Logs 클라이언트 생성
val cloudWatchLogsClient = cloudWatchLogsClientOf(
    endpointUrl = Url.parse("http://localhost:4566"),
    region = "ap-northeast-2",
    credentialsProvider = credentialsProvider
)
```

### 메트릭 발행

#### 단일 메트릭 발행

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.*
import io.bluetape4k.aws.kotlin.cloudwatch.model.*
import aws.sdk.kotlin.services.cloudwatch.model.StandardUnit

// metricDatumOf를 사용한 단일 메트릭 발행
val response = cloudWatchClient.putMetricData(
    namespace = "MyApp/Performance",
    metricDatum = metricDatumOf(
        metricName = "Latency",
        value = 100.0,
        unit = StandardUnit.Milliseconds
    )
)
```

#### DSL 빌더로 메트릭 생성

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.model.*

// DSL 블록을 사용한 메트릭 생성
val latencyMetric = metricDatum {
    metricName = "Latency"
    value = 100.0
    unit = StandardUnit.Milliseconds
    dimensions = listOf(
        aws.sdk.kotlin.services.cloudwatch.model.Dimension {
            name = "Service"
            value = "API"
        }
    )
    timestamp = System.currentTimeMillis()
}

val response = cloudWatchClient.putMetricData(
    namespace = "MyApp/Performance",
    metricDatum = latencyMetric
)
```

#### 배치 메트릭 발행

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.*
import io.bluetape4k.aws.kotlin.cloudwatch.model.*
import aws.sdk.kotlin.services.cloudwatch.model.StandardUnit

// 여러 메트릭을 배치로 발행
val metrics = listOf(
    metricDatumOf(
        metricName = "Latency",
        value = 100.0,
        unit = StandardUnit.Milliseconds
    ),
    metricDatumOf(
        metricName = "RequestCount",
        value = 1.0,
        unit = StandardUnit.Count
    ),
    metricDatumOf(
        metricName = "ErrorCount",
        value = 0.0,
        unit = StandardUnit.Count
    )
)

val response = cloudWatchClient.putMetricData(
    namespace = "MyApp/Performance",
    metricData = metrics
)
```

### 메트릭 조회

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.*

// 네임스페이스 내 모든 메트릭 조회
val allMetrics = cloudWatchClient.listMetrics(
    namespace = "MyApp/Performance"
)
allMetrics.metrics?.forEach { metric ->
    println("Metric: ${metric.metricName}")
}

// 특정 메트릭명으로 조회
val latencyMetrics = cloudWatchClient.listMetrics(
    namespace = "MyApp/Performance",
    metricName = "Latency"
)

// Dimensions 필터를 사용한 조회
val serviceMetrics = cloudWatchClient.listMetrics(
    namespace = "MyApp/Performance",
    dimensions = listOf(
        aws.sdk.kotlin.services.cloudwatch.model.DimensionFilter {
            name = "Service"
            value = "API"
        }
    )
)
```

### CloudWatch Logs 관리

#### 로그 그룹 생성

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.*

// 로그 그룹 생성
val createGroupResponse = cloudWatchLogsClient.createLogGroup(
    logGroupName = "/aws/lambda/my-function"
)
```

#### 로그 스트림 생성

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.*

// 로그 스트림 생성
val createStreamResponse = cloudWatchLogsClient.createLogStream(
    logGroupName = "/aws/lambda/my-function",
    logStreamName = "2024/01/01/[$LATEST]abc123"
)
```

#### 로그 이벤트 발행

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.*
import io.bluetape4k.aws.kotlin.cloudwatch.model.cloudwatchlogs.*

// inputLogEventOf를 사용한 로그 이벤트 생성
val logEvent = inputLogEventOf(
    timestamp = System.currentTimeMillis(),
    message = "Hello, CloudWatch Logs!"
)

// 로그 이벤트 발행
val putResponse = cloudWatchLogsClient.putLogEvents(
    logGroupName = "/aws/lambda/my-function",
    logStreamName = "2024/01/01/[$LATEST]abc123",
    logEvents = listOf(logEvent)
)

// 여러 로그 이벤트 발행
val events = listOf(
    inputLogEventOf(System.currentTimeMillis(), "Event 1"),
    inputLogEventOf(System.currentTimeMillis() + 1, "Event 2"),
    inputLogEventOf(System.currentTimeMillis() + 2, "Event 3")
)

cloudWatchLogsClient.putLogEvents(
    logGroupName = "/aws/lambda/my-function",
    logStreamName = "2024/01/01/[$LATEST]abc123",
    logEvents = events
)
```

#### DSL 빌더로 로그 이벤트 생성

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.model.cloudwatchlogs.*

// DSL 블록을 사용한 로그 이벤트 생성
val event = inputLogEvent {
    timestamp = System.currentTimeMillis()
    message = "Structured log message"
}

cloudWatchLogsClient.putLogEvents(
    logGroupName = "/aws/lambda/my-function",
    logStreamName = "2024/01/01/[$LATEST]abc123",
    logEvents = listOf(event)
)
```

### 로그 그룹/스트림 조회

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.*

// 로그 그룹 목록 조회
val logGroups = cloudWatchLogsClient.describeLogGroups(
    logGroupNamePrefix = "/aws/lambda"
)
logGroups.logGroups?.forEach { group ->
    println("Log Group: ${group.logGroupName}")
}

// 특정 로그 그룹의 스트림 목록 조회
val logStreams = cloudWatchLogsClient.describeLogStreams(
    logGroupName = "/aws/lambda/my-function"
)
logStreams.logStreams?.forEach { stream ->
    println("Log Stream: ${stream.logStreamName}")
}

// 스트림 이름 접두어로 필터링
val filteredStreams = cloudWatchLogsClient.describeLogStreams(
    logGroupName = "/aws/lambda/my-function",
    logStreamNamePrefix = "2024/01/01"
)
```

## 주요 기능 상세

| 파일                                    | 설명                           |
|---------------------------------------|------------------------------|
| `CloudWatchClientExtensions.kt`       | CloudWatch 메트릭 API 확장 함수   |
| `CloudWatchLogsClientExtensions.kt`   | CloudWatch Logs API 확장 함수    |
| `model/Metric.kt`                     | MetricDatum 빌더 함수           |
| `model/cloudwatchlogs/LogEvent.kt`    | InputLogEvent 빌더 함수         |

## CloudWatch Client 확장 함수

### putMetricData

메트릭 데이터를 CloudWatch에 발행합니다.

```kotlin
// 단일 메트릭 발행
suspend fun CloudWatchClient.putMetricData(
    namespace: String,
    metricDatum: MetricDatum,
    builder: suspend PutMetricDataRequest.Builder.() -> Unit = {}
): PutMetricDataResponse

// 배치 메트릭 발행
suspend fun CloudWatchClient.putMetricData(
    namespace: String,
    metricData: List<MetricDatum>,
    builder: suspend PutMetricDataRequest.Builder.() -> Unit = {}
): PutMetricDataResponse
```

### listMetrics

CloudWatch 메트릭 목록을 조회합니다.

```kotlin
suspend fun CloudWatchClient.listMetrics(
    namespace: String? = null,
    metricName: String? = null,
    dimensions: List<DimensionFilter>? = null,
    builder: suspend ListMetricsRequest.Builder.() -> Unit = {}
): ListMetricsResponse
```

## CloudWatch Logs Client 확장 함수

### createLogGroup

로그 그룹을 생성합니다.

```kotlin
suspend fun CloudWatchLogsClient.createLogGroup(
    logGroupName: String,
    builder: suspend CreateLogGroupRequest.Builder.() -> Unit = {}
): CreateLogGroupResponse
```

### createLogStream

로그 스트림을 생성합니다.

```kotlin
suspend fun CloudWatchLogsClient.createLogStream(
    logGroupName: String,
    logStreamName: String,
    builder: suspend CreateLogStreamRequest.Builder.() -> Unit = {}
): CreateLogStreamResponse
```

### putLogEvents

로그 이벤트를 발행합니다.

```kotlin
suspend fun CloudWatchLogsClient.putLogEvents(
    logGroupName: String,
    logStreamName: String,
    logEvents: List<InputLogEvent>,
    builder: suspend PutLogEventsRequest.Builder.() -> Unit = {}
): PutLogEventsResponse
```

### describeLogGroups

로그 그룹 목록을 조회합니다.

```kotlin
suspend fun CloudWatchLogsClient.describeLogGroups(
    logGroupNamePrefix: String? = null,
    builder: suspend DescribeLogGroupsRequest.Builder.() -> Unit = {}
): DescribeLogGroupsResponse
```

### describeLogStreams

로그 스트림 목록을 조회합니다.

```kotlin
suspend fun CloudWatchLogsClient.describeLogStreams(
    logGroupName: String,
    logStreamNamePrefix: String? = null,
    builder: suspend DescribeLogStreamsRequest.Builder.() -> Unit = {}
): DescribeLogStreamsResponse
```

## 빌더 함수

### metricDatum

DSL 블록으로 MetricDatum을 생성합니다.

```kotlin
inline fun metricDatum(
    @BuilderInference builder: MetricDatum.Builder.() -> Unit
): MetricDatum
```

### metricDatumOf

메트릭 이름, 값, 단위로 MetricDatum을 생성합니다.

```kotlin
inline fun metricDatumOf(
    metricName: String,
    value: Double,
    unit: StandardUnit = StandardUnit.None,
    builder: MetricDatum.Builder.() -> Unit = {}
): MetricDatum
```

### inputLogEvent

DSL 블록으로 InputLogEvent를 생성합니다.

```kotlin
inline fun inputLogEvent(
    @BuilderInference builder: InputLogEvent.Builder.() -> Unit
): InputLogEvent
```

### inputLogEventOf

타임스탬프와 메시지로 InputLogEvent를 생성합니다.

```kotlin
inline fun inputLogEventOf(
    timestamp: Long,
    message: String,
    builder: InputLogEvent.Builder.() -> Unit = {}
): InputLogEvent
```

## 테스트

이 모듈은 [LocalStack](https://localstack.cloud/)을 사용한 통합 테스트를 제공합니다.
테스트 실행 시 Docker가 설치되어 있어야 합니다.

```bash
# 특정 모듈 테스트 실행
./gradlew :bluetape4k-aws-kotlin-cloudwatch:test

# 특정 테스트 클래스 실행
./gradlew :bluetape4k-aws-kotlin-cloudwatch:test --tests "io.bluetape4k.aws.kotlin.cloudwatch.CloudWatchClientExtensionsTest"

# 특정 테스트 메소드 실행
./gradlew :bluetape4k-aws-kotlin-cloudwatch:test --tests "io.bluetape4k.aws.kotlin.cloudwatch.CloudWatchClientExtensionsTest.putMetricData"
```

## 관련 모듈

| 모듈 | 설명 |
|------|------|
| `bluetape4k-aws-kotlin-core` | AWS Kotlin SDK 공통 유틸리티 |
| `bluetape4k-aws-kotlin-tests` | AWS Kotlin SDK 테스트 지원 (LocalStack) |
| `bluetape4k-aws-cloudwatch` | AWS Java SDK v2 기반 CloudWatch 확장 |

## AWS SDK for Kotlin vs AWS SDK for Java v2 비교

| 기능 | `bluetape4k-aws-cloudwatch` (Java SDK) | `bluetape4k-aws-kotlin-cloudwatch` (Kotlin SDK) |
|------|--------|--------|
| 클라이언트 | `CloudWatchClient`, `CloudWatchAsyncClient` | `CloudWatchClient` (단일 suspend 기반) |
| 비동기 방식 | `CompletableFuture` + `.await()` | Native suspend function |
| 코루틴 통합 | 별도 `kotlinx-coroutines-jdk8` 필요 | 기본 제공 |
| 메트릭 발행 | 별도 빌더 필요 | DSL 빌더 포함 |
| 로그 관리 | 기본 API만 제공 | 로그 그룹/스트림 관리 확장 함수 포함 |
