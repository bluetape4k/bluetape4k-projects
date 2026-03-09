# Module bluetape4k-aws-sqs

AWS SDK for Java v2 SQS(Simple Queue Service) 사용을 위한 Kotlin 확장 라이브러리입니다.

## 주요 기능

- 동기/비동기 SQS 클라이언트 생성 헬퍼
- Send/Receive/Delete/Visibility API 확장 함수
- Coroutines 브릿지(`suspend`) 지원
- 배치 처리 유틸 (`sendBatch`, `deleteMessageBatch`, `changeMessageVisibilityBatch`)
- 요청 빌더 유틸 (`model/*`)

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-aws-sqs:${version}")
}
```

## 사용 예시

### 동기 클라이언트

```kotlin
import io.bluetape4k.aws.sqs.*
import io.bluetape4k.aws.sqs.model.sendMessageBatchRequestEntry

val sqsClient: SqsClient = // ...

val queueUrl = sqsClient.createQueue("my-queue")

val sendResponse = sqsClient.send(queueUrl, "Hello, SQS!")
println(sendResponse.messageId())

val receiveResponse = sqsClient.receiveMessages(queueUrl, maxResults = 3)
receiveResponse.messages().forEach { message ->
    sqsClient.deleteMessage(queueUrl, message.receiptHandle())
}

val batchEntries = List(3) { index ->
    sendMessageBatchRequestEntry {
        id("id-$index")
        messageBody("Hello, batch-$index")
    }
}
sqsClient.sendBatch(queueUrl, batchEntries)
```

### 비동기/코루틴

```kotlin
import io.bluetape4k.aws.sqs.*

suspend fun process(asyncClient: SqsAsyncClient, queueUrl: String) {
    asyncClient.send(queueUrl, "hello")

    val response = asyncClient.receiveMessages(queueUrl, maxResults = 3)
    response.messages().forEach { message ->
        asyncClient.deleteMessage(queueUrl, message.receiptHandle())
    }
}
```

### 요청 빌더

```kotlin
import io.bluetape4k.aws.sqs.model.*

val receiveRequest = receiveMessageRequestOf(
    queueUrl = "https://sqs.ap-northeast-2.amazonaws.com/123456789012/my-queue",
    maxNumber = 3,
    waitTimeSeconds = 20,
)
```

## 입력 검증(안정성)

- `receiveMessages*`의 `maxResults`는 `1..10` 범위를 강제합니다.
- `receiveMessageRequestOf`의 `maxNumber`는 `1..10`, `waitTimeSeconds`는 `0..20` 범위를 강제합니다.
- 배치 API(`sendBatch*`, `changeMessageVisibilityBatch*`, `deleteMessageBatch*`)는 빈 엔트리를 허용하지 않습니다.

## 주요 소스 파일

| 파일 | 설명 |
|---|---|
| `SqsClientExtensions.kt` | 동기 `SqsClient` 확장 함수 |
| `SqsAsyncClientExtensions.kt` | 비동기 `SqsAsyncClient` 확장 함수 |
| `SqsAsyncClientCoroutinesExtensions.kt` | 코루틴 확장 함수 |
| `SqsClientFactory.kt` | SQS 클라이언트 팩토리 |
| `model/SendMessage.kt` | 메시지 전송 요청 유틸 |
| `model/ReceiveMessage.kt` | 메시지 수신 요청 유틸 |
| `model/DeleteMessage.kt` | 메시지 삭제 요청 유틸 |
