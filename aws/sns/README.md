# Module bluetape4k-aws-sns

AWS SDK for Java v2 SNS(Simple Notification Service) 사용을 위한 확장 라이브러리입니다.

## 주요 기능

- **SNS Client 생성 지원**: 동기/비동기 클라이언트 보조
- **Publish/Subscribe 지원**: 토픽 발행/구독 요청 빌더
- **Topic/Subscription 관리**: 생성/조회/속성 설정 유틸
- **Coroutine 브릿지**: Async API의 suspend 사용 지원
- **SMS 지원**: SMS 발송 및 설정

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-sns:${version}")
}
```

## 사용 예시

### 동기 클라이언트

```kotlin
import io.bluetape4k.aws.sns.*
import io.bluetape4k.aws.sns.model.*

val snsClient: SnsClient = // ...

// Topic 생성
val topic = snsClient.createTopic("my-topic")

// FIFO Topic 생성 (중복 제거, 순서 보장)
val fifoTopic = snsClient.createFIFOTopic("my-fifo-topic.fifo")

// 메시지 발행
val publishResponse = snsClient.publish {
    topicArn(topic.topicArn())
    message("Hello, SNS!")
    subject("Test Message")
}

// 메시지 속성과 함께 발행
snsClient.publish {
    topicArn(topic.topicArn())
    message("{\"event\": \"order.created\", \"orderId\": \"123\"}")
    messageAttributes(
        mapOf(
            "eventType" to MessageAttributeValue.builder()
                .dataType("String")
                .stringValue("order.created")
                .build()
        )
    )
}

// SMS 발송
snsClient.publish {
    phoneNumber("+821012345678")
    message("Your verification code is 123456")
}

// Platform Endpoint 생성 (모바일 푸시)
val endpoint = snsClient.createPlatformEndpoint(
    token = "device-token",
    platformApplicationArn = "arn:aws:sns:ap-northeast-2:123456789:app/GCM/MyApp"
)

// 구독 생성
val subscription = snsClient.subscribe {
    topicArn(topic.topicArn())
    protocol("https")
    endpoint("https://example.com/webhook")
}

// 구독 확인
snsClient.confirmSubscription {
    topicArn(topic.topicArn())
    token("confirmation-token")
}

// 구독 해제
snsClient.unsubscribe {
    subscriptionArn(subscription.subscriptionArn())
}

// Topic 삭제
snsClient.deleteTopic {
    topicArn(topic.topicArn())
}
```

### 비동기 클라이언트 (Coroutine)

```kotlin
import io.bluetape4k.aws.sns.*
import kotlinx.coroutines.future.await

val snsAsyncClient: SnsAsyncClient = // ...

    suspend
fun publishMessage(topicArn: String, message: String): String {
    val response = snsAsyncClient.publish {
        it.topicArn(topicArn)
        it.message(message)
    }.await()
    return response.messageId()
}

suspend fun createTopic(name: String): String {
    val response = snsAsyncClient.createTopic {
        it.name(name)
    }.await()
    return response.topicArn()
}
```

### Topic 속성 조회

```kotlin
import io.bluetape4k.aws.sns.model.*

val attributes = snsClient.getTopicAttributes {
    topicArn(topicArn)
}

println("DisplayName: ${attributes.attributes()["DisplayName"]}")
println("SubscriptionsConfirmed: ${attributes.attributes()["SubscriptionsConfirmed"]}")
```

## 주요 기능 상세

| 파일                                      | 설명                   |
|-----------------------------------------|----------------------|
| `SnsFactory.kt`                         | SNS 클라이언트 팩토리        |
| `SnsClientSupport.kt`                   | 동기 클라이언트 생성          |
| `SnsAsyncClientSupport.kt`              | 비동기 클라이언트 생성         |
| `SnsClientExtensions.kt`                | 동기 클라이언트 확장 함수       |
| `SnsAsyncClientExtensions.kt`           | 비동기 클라이언트 확장 함수      |
| `SnsAsyncClientCoroutinesExtensions.kt` | 코루틴 확장 함수            |
| `model/Publish.kt`                      | Publish 요청 빌더        |
| `model/Subscribe.kt`                    | Subscribe 요청 빌더      |
| `model/CreateTopic.kt`                  | Topic 생성 빌더          |
| `model/DeleteTopic.kt`                  | Topic 삭제 빌더          |
| `model/Unsubscribe.kt`                  | 구독 해제 빌더             |
| `model/ConfirmSubscription.kt`          | 구독 확인 빌더             |
| `model/GetTopicAttributes.kt`           | Topic 속성 조회          |
| `model/GetSubscriptionAttributes.kt`    | 구독 속성 조회             |
| `model/SetSmsAttributes.kt`             | SMS 속성 설정            |
| `model/SetSubscriptionAttributes.kt`    | 구독 속성 설정             |
| `model/CreatePlatformEndpoint.kt`       | Platform Endpoint 생성 |
| `model/MessageAttributeValue.kt`        | 메시지 속성 값             |
| `model/ListTopics.kt`                   | Topic 목록 조회          |
| `model/ListSubscriptions.kt`            | 구독 목록 조회             |
| `model/ListPhoneNumbersOptedOut.kt`     | 수신 거부 번호 목록          |
| `model/CheckIfPhoneNumberIsOptedOut.kt` | 수신 거부 확인             |
| `model/TagResource.kt`                  | 리소스 태그               |
