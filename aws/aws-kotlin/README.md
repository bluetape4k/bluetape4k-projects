# Module bluetape4k-aws-kotlin

AWS Kotlin SDK 기반 단일 통합 모듈입니다. native `suspend` 함수를 기본 제공하여 `.await()` 변환 없이 Coroutines 환경에서 바로 사용할 수 있습니다.

> AWS Java SDK v2 기반 모듈은 `bluetape4k-aws`를 사용하세요.

## 제공 서비스

| 서비스 | 주요 기능 |
|--------|----------|
| **DynamoDB** | 테이블 CRUD, 스캔/쿼리, DSL 빌더 |
| **S3** | 객체 업로드/다운로드, 멀티파트, 버킷 관리 |
| **SES / SESv2** | 이메일 발송, 템플릿 메일 |
| **SNS** | 토픽 발행, SMS, 구독 관리 |
| **SQS** | 메시지 발송/수신/삭제, FIFO 큐 |
| **KMS** | 암호화 키 관리, 데이터 키 생성 |
| **CloudWatch** | 메트릭 발행/조회, DSL(`metricDatum {}`) |
| **CloudWatch Logs** | 로그 이벤트 전송, DSL(`inputLogEvent {}`) |
| **Kinesis** | 스트림 레코드 전송, DSL(`putRecordRequestOf {}`) |
| **STS** | AssumeRole, CallerIdentity, DSL(`stsClientOf {}`) |

## Java SDK v2 vs Kotlin SDK 비교

| 항목 | `bluetape4k-aws` (Java SDK) | `bluetape4k-aws-kotlin` (Kotlin SDK) |
|------|----------------------------|--------------------------------------|
| Coroutines | `.await()` 변환 필요 | native `suspend` 기본 제공 |
| DSL 지원 | 제한적 | 풍부한 DSL 빌더 |
| 성능 | CRT/Netty NIO 선택 | CRT / OkHttp 선택 |

## 설치

AWS Kotlin SDK 서비스는 `compileOnly`로 선언되어 있으므로, 사용할 서비스 SDK를 런타임 의존성으로 추가해야 합니다.

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-aws-kotlin:${bluetape4kVersion}")

    // 사용할 서비스만 선택적으로 추가
    implementation("aws.sdk.kotlin:dynamodb:${awsKotlinSdkVersion}")
    implementation("aws.sdk.kotlin:s3:${awsKotlinSdkVersion}")
    implementation("aws.sdk.kotlin:sqs:${awsKotlinSdkVersion}")
    // ... 필요한 서비스 추가
}
```

## 사용 예시

### DynamoDB (native suspend)

```kotlin
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.*

val client = DynamoDbClient { region = "ap-northeast-2" }

// native suspend - .await() 불필요
suspend fun getItem(tableName: String, key: Map<String, AttributeValue>) =
    client.getItem {
        this.tableName = tableName
        this.key = key
    }
```

### CloudWatch 메트릭 (DSL)

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatch.*
import aws.sdk.kotlin.services.cloudwatch.CloudWatchClient

val cw = CloudWatchClient { region = "ap-northeast-2" }

suspend fun publishMetric(namespace: String, value: Double) {
    cw.putMetricData {
        this.namespace = namespace
        metricData = listOf(
            metricDatum {           // bluetape4k DSL
                metricName = "RequestCount"
                this.value = value
                unit = StandardUnit.Count
            }
        )
    }
}
```

### CloudWatch Logs (DSL)

```kotlin
import io.bluetape4k.aws.kotlin.cloudwatchlogs.*

suspend fun sendLog(client: CloudWatchLogsClient, logGroup: String, logStream: String, message: String) {
    client.putLogEvents {
        logGroupName = logGroup
        logStreamName = logStream
        logEvents = listOf(
            inputLogEvent {         // bluetape4k DSL
                timestamp = System.currentTimeMillis()
                this.message = message
            }
        )
    }
}
```

### STS (DSL)

```kotlin
import io.bluetape4k.aws.kotlin.sts.*

// bluetape4k DSL로 StsClient 생성
val stsClient = stsClientOf(region = "ap-northeast-2")

suspend fun getCallerIdentity() = stsClient.getCallerIdentity {}
```

### Kinesis (DSL)

```kotlin
import io.bluetape4k.aws.kotlin.kinesis.*

suspend fun putRecord(client: KinesisClient, streamName: String, data: ByteArray) {
    client.putRecord(
        putRecordRequestOf(streamName, data, partitionKey = "default")
    )
}
```

## 테스트 환경

LocalStack을 사용한 통합 테스트를 지원합니다:

```kotlin
@Testcontainers
class SqsTest {
    companion object {
        @Container
        val localstack = LocalStackContainer(DockerImageName.parse("localstack/localstack"))
            .withServices(LocalStackContainer.Service.SQS)
    }
}
```
