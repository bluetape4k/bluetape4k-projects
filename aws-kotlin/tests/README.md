# Module bluetape4k-aws-kotlin-tests

AWS Kotlin SDK 모듈 테스트를 위한 공통 Test Support 라이브러리입니다.

## 주요 기능

- **LocalStack 연동 유틸**: 테스트 환경에서 AWS 서비스 에뮬레이션 지원
- **컨테이너 확장 함수**: LocalStack 설정/초기화 보조
- **통합 테스트 보조**: 서비스별 테스트 코드 중복 제거

## 의존성 추가

```kotlin
dependencies {
    testImplementation("io.bluetape4k:bluetape4k-aws-kotlin-tests:${version}")
}
```

## 사용 예시

### LocalStack 컨테이너 실행

```kotlin
import io.bluetape4k.aws.kotlin.tests.*
import org.testcontainers.containers.localstack.LocalStackContainer

// LocalStack 서버 실행
val localStack = getLocalStackServer(
    LocalStackContainer.Service.S3,
    LocalStackContainer.Service.SQS,
    LocalStackContainer.Service.SNS,
    LocalStackContainer.Service.DYNAMODB
)

// 엔드포인트 URL 가져오기
val endpointUrl = localStack.endpointUrl

// 인증 정보 제공자 가져오기
val credentialsProvider = localStack.getCredentialsProvider()

// 특정 서비스의 엔드포인트 가져오기
val s3Endpoint = localStack.getServiceEndpoint(LocalStackContainer.Service.S3)
```

### S3 클라이언트 테스트

```kotlin
import io.bluetape4k.aws.kotlin.tests.*
import io.bluetape4k.aws.kotlin.s3.s3ClientOf

class S3ClientTest {

    private val localStack = getLocalStackServer(LocalStackContainer.Service.S3)
    private val s3Client = s3ClientOf(
        endpointUrl = localStack.endpointUrl,
        region = "us-east-1",
        credentialsProvider = localStack.getCredentialsProvider()
    )

    @Test
    fun `should upload and download file`() = runTest {
        // 테스트 코드
        s3Client.createBucket("test-bucket")
        s3Client.putFromString("test-bucket", "test.txt", "Hello, World!")

        val content = s3Client.getAsString("test-bucket", "test.txt")
        assertEquals("Hello, World!", content)
    }
}
```

### DynamoDB 클라이언트 테스트

```kotlin
import io.bluetape4k.aws.kotlin.tests.*
import io.bluetape4k.aws.kotlin.dynamodb.dynamoDbClientOf

class DynamoDbClientTest {

    private val localStack = getLocalStackServer(LocalStackContainer.Service.DYNAMODB)
    private val dynamoDbClient = dynamoDbClientOf(
        endpointUrl = localStack.endpointUrl,
        region = "us-east-1",
        credentialsProvider = localStack.getCredentialsProvider()
    )

    @Test
    fun `should create and query table`() = runTest {
        // 테이블 생성
        dynamoDbClient.createTable(
            tableName = "TestTable",
            keySchema = listOf(
                KeySchemaElement { attributeName = "pk"; keyType = KeyType.Hash }
            ),
            attributeDefinitions = listOf(
                AttributeDefinition { attributeName = "pk"; attributeType = ScalarAttributeType.S }
            )
        )

        // 테이블 준비 대기
        dynamoDbClient.waitForTableReady("TestTable")

        // 아이템 저장
        dynamoDbClient.putItem("TestTable", mapOf("pk" to "test-key"))
    }
}
```

### SQS 클라이언트 테스트

```kotlin
import io.bluetape4k.aws.kotlin.tests.*
import io.bluetape4k.aws.kotlin.sqs.sqsClientOf

class SqsClientTest {

    private val localStack = getLocalStackServer(LocalStackContainer.Service.SQS)
    private val sqsClient = sqsClientOf(
        endpointUrl = localStack.endpointUrl,
        region = "us-east-1",
        credentialsProvider = localStack.getCredentialsProvider()
    )

    @Test
    fun `should send and receive message`() = runTest {
        val queueUrl = sqsClient.createQueue("test-queue").queueUrl

        sqsClient.sendMessage(queueUrl, "Hello, SQS!")

        val response = sqsClient.receiveMessage(queueUrl, maxNumberOfMessages = 1)
        assertEquals(1, response.messages?.size)
        assertEquals("Hello, SQS!", response.messages?.first()?.body)
    }
}
```

### SNS 클라이언트 테스트

```kotlin
import io.bluetape4k.aws.kotlin.tests.*
import io.bluetape4k.aws.kotlin.sns.snsClientOf

class SnsClientTest {

    private val localStack = getLocalStackServer(LocalStackContainer.Service.SNS)
    private val snsClient = snsClientOf(
        endpointUrl = localStack.endpointUrl,
        region = "us-east-1",
        credentialsProvider = localStack.getCredentialsProvider()
    )

    @Test
    fun `should publish message`() = runTest {
        val topic = snsClient.createTopic("test-topic")

        val response = snsClient.publish(
            topicArn = topic.topicArn,
            message = "Hello, SNS!"
        )

        assertNotNull(response.messageId)
    }
}
```

## 주요 기능 상세

| 파일                                 | 설명                    |
|------------------------------------|-----------------------|
| `LocalStackContainerExtensions.kt` | LocalStack 컨테이너 확장 함수 |

### 확장 함수 목록

| 함수                                                | 설명                        |
|---------------------------------------------------|---------------------------|
| `getLocalStackServer(vararg services)`            | LocalStack 서버 실행          |
| `LocalStackContainer.endpointUrl`                 | AWS Kotlin SDK용 엔드포인트 URL |
| `LocalStackContainer.getCredentialsProvider()`    | 테스트용 인증 제공자               |
| `LocalStackContainer.getServiceEndpoint(service)` | 특정 서비스의 엔드포인트             |

## 주의사항

- LocalStack은 모든 AWS 서비스를 완벽하게 에뮬레이션하지 않습니다.
- SESv2는 LocalStack에서 지원하지 않습니다.
- 일부 고급 기능은 실제 AWS 환경에서 테스트해야 합니다.
