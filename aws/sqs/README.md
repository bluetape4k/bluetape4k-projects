# Module bluetape4k-aws-sqs

AWS SDK for Java v2 SQS(Simple Queue Service) 사용을 위한 확장 라이브러리입니다.

## 주요 기능

- **SQS Client 생성 지원**: 동기/비동기 클라이언트 보조
- **메시지 송수신 유틸**: Send/Receive/Delete 요청 빌더
- **Client 확장 함수**: SQS API 호출 편의 함수
- **Coroutine 브릿지**: Async API의 suspend 사용 지원
- **배치 작업 지원**: 메시지 일괄 처리

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-sqs:${version}")
}
```

## 사용 예시

### 클라이언트 생성

```kotlin
import io.bluetape4k.aws.sqs.*

// 기본 클라이언트 생성
val sqsClient = sqsClient {
    region(Region.AP_NORTHEAST_2)
}

// 로컬/커스텀 엔드포인트로 클라이언트 생성
val localSqsClient = sqsClientOf(
    endpoint = URI.create("http://localhost:4566"),
    region = Region.AP_NORTHEAST_2,
    credentialsProvider = LocalAwsCredentialsProvider
)
```

### 동기 클라이언트

```kotlin
import io.bluetape4k.aws.sqs.*

val sqsClient: SqsClient = // ...

// Queue 생성
val queueUrl = sqsClient.createQueue("my-queue")

// Queue 목록 조회
val queues = sqsClient.listQueues(prefix = "my-")

// Queue URL 조회
val queueUrlResponse = sqsClient.getQueueUrl("my-queue")

// 단일 메시지 전송
val sendResponse = sqsClient.send(queueUrl, "Hello, SQS!")
println("MessageId: ${sendResponse.messageId()}")

// 메시지 수신
val receiveResponse = sqsClient.receiveMessages(queueUrl, maxResults = 10)
receiveResponse.messages().forEach { message ->
    println("Received: ${message.body()}")

    // 메시지 처리 후 삭제
    sqsClient.deleteMessage(queueUrl, message.receiptHandle())
}

// 메시지 가시성 변경
sqsClient.changeMessageVisibility(queueUrl, receiptHandle, visibilityTimeout = 30)

// Queue 삭제
sqsClient.deleteQueue(queueUrl)
```

### 배치 작업

```kotlin
import io.bluetape4k.aws.sqs.model.*

// 배치 메시지 전송
val batchEntries = listOf(
    sendMessageBatchEntryOf(1, "Message 1"),
    sendMessageBatchEntryOf(2, "Message 2"),
    sendMessageBatchEntryOf(3, "Message 3")
)

val batchResponse = sqsClient.sendBatch(queueUrl, batchEntries)
println("Successful: ${batchResponse.successful().size}")
println("Failed: ${batchResponse.failed().size}")

// 배치 메시지 삭제
val deleteEntries = messages.map { message ->
    DeleteMessageBatchRequestEntry.builder()
        .id(message.messageId())
        .receiptHandle(message.receiptHandle())
        .build()
}
sqsClient.deleteMessageBatch(queueUrl, deleteEntries)

// 배치 가시성 변경
val visibilityEntries = messages.map { message ->
    ChangeMessageVisibilityBatchRequestEntry.builder()
        .id(message.messageId())
        .receiptHandle(message.receiptHandle())
        .visibilityTimeout(60)
        .build()
}
sqsClient.changeMessageVisibilityBatch(queueUrl, visibilityEntries)
```

### 비동기 클라이언트 (Coroutine)

```kotlin
import io.bluetape4k.aws.sqs.*
import kotlinx.coroutines.future.await

val sqsAsyncClient: SqsAsyncClient = // ...

    suspend
fun sendMessage(queueUrl: String, body: String): String {
    val response = sqsAsyncClient.send(queueUrl, body)
    return response.messageId()
}

suspend fun receiveAndProcess(queueUrl: String) {
    val response = sqsAsyncClient.receiveMessages(queueUrl, maxResults = 10).await()

    response.messages().forEach { message ->
        // 메시지 처리
        processMessage(message.body())

        // 삭제
        sqsAsyncClient.deleteMessage(queueUrl, message.receiptHandle()).await()
    }
}
```

### 메시지 요청 빌더

```kotlin
import io.bluetape4k.aws.sqs.model.*

// 메시지 전송 요청
val sendRequest = sendMessageRequestOf(queueUrl, "message body") {
    delaySeconds(10)
    messageAttributes(
        mapOf(
            "eventType" to MessageAttributeValue.builder()
                .dataType("String")
                .stringValue("order.created")
                .build()
        )
    )
}

// 메시지 수신 요청
val receiveRequest = receiveMessageRequestOf(queueUrl) {
    maxNumberOfMessages(10)
    waitTimeSeconds(20)  // Long Polling
    visibilityTimeout(30)
    attributeNames(QueueAttributeName.ALL)
}
```

## 주요 기능 상세

| 파일                                      | 설명              |
|-----------------------------------------|-----------------|
| `SqsFactory.kt`                         | SQS 클라이언트 팩토리   |
| `SqsClientExtensions.kt`                | 동기 클라이언트 확장 함수  |
| `SqsAsyncClientExtensions.kt`           | 비동기 클라이언트 확장 함수 |
| `SqsAsyncClientCoroutinesExtensions.kt` | 코루틴 확장 함수       |
| `model/SendMessage.kt`                  | 메시지 전송 빌더       |
| `model/ReceiveMessage.kt`               | 메시지 수신 빌더       |
| `model/DeleteMessage.kt`                | 메시지 삭제 빌더       |
