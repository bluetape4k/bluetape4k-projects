# Module bluetape4k-aws-kotlin-sqs

AWS SDK for Kotlin SQS(Simple Queue Service) 사용을 위한 확장 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 합니다.

## 주요 기능

- **SQS Client 확장 함수**: 메시지 송수신/삭제 보조 API
- **요청 모델 빌더 지원**: Send/Receive/Delete 요청 생성 유틸
- **메시지 속성 지원**: MessageAttributeValue 보조
- **가시성 제어 지원**: MessageVisibility 유틸
- **배치 작업 지원**: 배치 송수신/삭제

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-sqs:${version}")
}
```

## 사용 예시

### 클라이언트 생성

```kotlin
import io.bluetape4k.aws.kotlin.sqs.sqsClientOf
import aws.smithy.kotlin.runtime.net.url.Url

val sqsClient = sqsClientOf(
    endpointUrl = Url.parse("http://localhost:4566"),
    region = "ap-northeast-2",
    credentialsProvider = credentialsProvider
)
```

### Queue 관리

```kotlin
import io.bluetape4k.aws.kotlin.sqs.*

// Queue 생성
val response = sqsClient.createQueue("my-queue")
val queueUrl = response.queueUrl

// Queue 존재 확인
val exists = sqsClient.existsQueue("my-queue")

// Queue가 없으면 생성
val ensuredUrl = sqsClient.ensureQueue("my-queue")

// Queue URL 조회
val url = sqsClient.getQueueUrl("my-queue")

// Queue 목록 조회
val queues = sqsClient.listQueues(queueNamePrefix = "my-")

// Queue 삭제
sqsClient.deleteQueue(queueUrl)
```

### 메시지 송수신

```kotlin
// 단일 메시지 전송
val sendResponse = sqsClient.sendMessage(
    queueUrl = queueUrl,
    messageBody = "Hello, SQS!",
    delaySeconds = 10
) {
    messageAttributes = mapOf(
        "eventType" to MessageAttributeValue {
            dataType = "String"
            stringValue = "order.created"
        }
    )
}
println("MessageId: ${sendResponse.messageId}")

// 메시지 수신
val receiveResponse = sqsClient.receiveMessage(
    queueUrl = queueUrl,
    maxNumberOfMessages = 10
)

receiveResponse.messages?.forEach { message ->
    println("Received: ${message.body}")

    // 메시지 처리 후 삭제
    sqsClient.deleteMessage(queueUrl, message.receiptHandle)
}
```

### 배치 작업

```kotlin
import aws.sdk.kotlin.services.sqs.model.*
import io.bluetape4k.aws.kotlin.sqs.model.*

// 배치 메시지 전송
val batchEntries = List(10) { index ->
    SendMessageBatchRequestEntry {
        id = "msg-$index"
        messageBody = "Message $index"
        delaySeconds = 0
    }
}

val batchResponse = sqsClient.sendMessageBatch(queueUrl, batchEntries)
println("Successful: ${batchResponse.successful?.size}")
println("Failed: ${batchResponse.failed?.size}")

// 배치 메시지 삭제
val deleteEntries = messages.map { message ->
    DeleteMessageBatchRequestEntry {
        id = message.messageId
        receiptHandle = message.receiptHandle
    }
}
sqsClient.deleteMessageBatch(queueUrl, deleteEntries)
```

### 가시성 타임아웃 관리

```kotlin
// 단일 메시지 가시성 변경
sqsClient.changeMessageVisibility(
    queueUrl = queueUrl,
    receiptHandle = receiptHandle,
    visibilityTimeout = 60  // 60초 동안 다른 컨슈머에게 보이지 않음
)

// 배치 가시성 변경
val visibilityEntries = messages.map { message ->
    ChangeMessageVisibilityBatchRequestEntry {
        id = message.messageId
        receiptHandle = message.receiptHandle
        visibilityTimeout = 30
    }
}
sqsClient.changeMessageVisibilityBatch(queueUrl, visibilityEntries)
```

### 메시지 속성 사용

```kotlin
import io.bluetape4k.aws.kotlin.sqs.model.*

// 속성과 함께 메시지 전송
sqsClient.sendMessage(
    queueUrl = queueUrl,
    messageBody = """{"orderId": "12345"}"""
) {
    messageAttributes = mapOf(
        "eventType" to messageValueAttributeOf("order.created"),
        "version" to messageNumberAttributeOf("1.0"),
        "binary" to messageBinaryAttributeOf(byteArrayOf(1, 2, 3))
    )
}
```

## 주요 기능 상세

| 파일                               | 설명                   |
|----------------------------------|----------------------|
| `SqsClientExtensions.kt`         | SQS 클라이언트 확장 함수      |
| `model/SendMessage.kt`           | SendMessage 요청 빌더    |
| `model/ReceiveMessage.kt`        | ReceiveMessage 요청 빌더 |
| `model/DeleteMessage.kt`         | DeleteMessage 요청 빌더  |
| `model/MessageAttributeValue.kt` | 메시지 속성 값 빌더          |
| `model/MessageVisibility.kt`     | 가시성 제어 빌더            |
