# Module bluetape4k-aws-kotlin-sns

AWS SDK for Kotlin SNS(Simple Notification Service) 사용을 위한 확장 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 합니다.

## 주요 기능

- **SNS Client 확장 함수**: 토픽 발행/구독 보조 API
- **요청 모델 빌더 지원**: Publish/Subscribe/Topic 관련 유틸
- **메시지 속성 지원**: MessageAttributeValue 보조
- **배치 발행 지원**: PublishBatch API
- **SMS 지원**: SMS 발송 및 Opt-out 확인

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-sns:${version}")
}
```

## 사용 예시

### 클라이언트 생성

```kotlin
import io.bluetape4k.aws.kotlin.sns.snsClientOf
import aws.smithy.kotlin.runtime.net.url.Url

val snsClient = snsClientOf(
    endpointUrl = Url.parse("http://localhost:4566"),
    region = "ap-northeast-2",
    credentialsProvider = credentialsProvider
)
```

### Topic 관리

```kotlin
import io.bluetape4k.aws.kotlin.sns.*

// 일반 Topic 생성
val topic = snsClient.createTopic("my-topic")

// FIFO Topic 생성 (이름이 .fifo로 끝나야 함)
val fifoTopic = snsClient.createFifoTopic("my-fifo-topic.fifo")

// Topic 삭제
snsClient.deleteTopic(topic.topicArn)
```

### 메시지 발행

```kotlin
import io.bluetape4k.aws.kotlin.sns.*

// 기본 발행
val response = snsClient.publish(
    topicArn = topicArn,
    message = "Hello, SNS!",
    subject = "Test Message"
)

// 속성과 함께 발행
snsClient.publish(
    topicArn = topicArn,
    message = """{"event": "order.created", "orderId": "123"}""",
    subject = "Order Event"
) {
    messageAttributes = mapOf(
        "eventType" to MessageAttributeValue {
            dataType = "String"
            stringValue = "order.created"
        }
    )
}

// SMS 발송
snsClient.publish(
    topicArn = null,
    message = "Your verification code is 123456"
) {
    phoneNumber = "+821012345678"
}
```

### 배치 발행

```kotlin
import io.bluetape4k.aws.kotlin.sns.model.*
import aws.sdk.kotlin.services.sns.model.*

val entries = List(10) { index ->
    publishBatchRequestEntryOf(
        id = "msg-$index",
        message = "Message $index"
    )
}

val batchResponse = snsClient.publishBatch(
    topicArn = topicArn,
    entries = entries
)

println("Successful: ${batchResponse.successful?.size}")
println("Failed: ${batchResponse.failed?.size}")
```

### 구독 관리

```kotlin
// 구독 생성
val subscription = snsClient.subscribe(
    topicArn = topicArn,
    endpoint = "https://example.com/webhook",
    protocol = "https"
)

// 전화번호 구독 (SMS)
val smsSubscription = snsClient.subscribe(
    topicArn = topicArn,
    endpoint = "+821012345678",
    protocol = "sms"
)

// 구독 해제
snsClient.unsubscribe(subscriptionArn)
```

### 모바일 푸시 알림

```kotlin
// Platform Endpoint 생성 (FCM/GCM)
val endpoint = snsClient.createPlatformEndpoint(
    token = "device-fcm-token",
    platformApplicationArn = "arn:aws:sns:ap-northeast-2:123456789:app/GCM/MyApp"
)

// Endpoint로 직접 발송
snsClient.publish(
    topicArn = null,
    message = """{"GCM": "{\"data\":{\"message\":\"Hello!\"}}"}"""
) {
    targetArn = endpoint.endpointArn
}
```

### SMS Opt-out 확인

```kotlin
// 전화번호가 Opt-out 되었는지 확인
val response = snsClient.checkIfPhoneNumberIsOptedOut("+821012345678")
if (response.isOptedOut == true) {
    println("This phone number has opted out of SMS")
}
```

## 주요 기능 상세

| 파일                               | 설명                      |
|----------------------------------|-------------------------|
| `SnsClientExtensions.kt`         | SNS 클라이언트 확장 함수         |
| `model/Publish.kt`               | Publish 요청 빌더           |
| `model/Subscribe.kt`             | Subscribe 요청 빌더         |
| `model/Topic.kt`                 | Topic 관련 빌더             |
| `model/ListTopics.kt`            | ListTopics 요청 빌더        |
| `model/ListSubscriptions.kt`     | ListSubscriptions 요청 빌더 |
| `model/GetTopicAttributes.kt`    | Topic 속성 조회 빌더          |
| `model/MessageAttributeValue.kt` | 메시지 속성 값 빌더             |
