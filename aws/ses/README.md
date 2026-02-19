# Module bluetape4k-aws-ses

AWS SDK for Java v2 SES(Simple Email Service) 사용을 위한 확장 라이브러리입니다.

## 주요 기능

- **SES Client 생성 지원**: 동기/비동기 클라이언트 보조
- **Email 요청 빌더 지원**: `Destination`, `Message`, `SendEmailRequest` 유틸
- **Client 확장 함수**: 전송 API 호출 편의 함수
- **Coroutine 브릿지**: Async API의 suspend 사용 지원
- **Waiter 지원**: SES 리소스 상태 확인 보조

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-ses:${version}")
}
```

## 사용 예시

### 동기 클라이언트

```kotlin
import io.bluetape4k.aws.ses.*
import io.bluetape4k.aws.ses.model.*

val sesClient: SesClient = // ...

// 기본 이메일 전송
val request = sendEmailRequest {
    source("sender@example.com")
    destination {
        toAddresses("recipient@example.com")
        ccAddresses("cc@example.com")
    }
    message {
        subject("Hello from SES")
        body {
            text("This is plain text body")
            html("<h1>Hello</h1><p>This is HTML body</p>")
        }
    }
}

val response = sesClient.send(request)
println("MessageId: ${response.messageId()}")

// Raw 이메일 전송 (첨부파일 포함)
val rawResponse = sesClient.sendRaw(rawEmailRequest)

// 템플릿 이메일 전송
val templatedResponse = sesClient.sendTemplated(templatedEmailRequest)

// 대량 템플릿 이메일 전송
val bulkResponse = sesClient.sendBulkTemplated(bulkTemplatedEmailRequest)
```

### 비동기 클라이언트 (Coroutine)

```kotlin
import io.bluetape4k.aws.ses.*
import kotlinx.coroutines.future.await

val sesAsyncClient: SesAsyncClient = // ...

    suspend
fun sendEmail(): String {
    val response = sesAsyncClient.sendEmail(request).await()
    return response.messageId()
}
```

### 메시지 빌더

```kotlin
import io.bluetape4k.aws.ses.model.*

// Destination 빌더
val destination = destination {
    toAddresses("user1@example.com", "user2@example.com")
    ccAddresses("cc@example.com")
    bccAddresses("bcc@example.com")
}

// Message 빌더
val message = message {
    subject("Important Notification")
    body {
        text("Plain text content")
        html("<html><body><h1>HTML content</h1></body></html>")
    }
}

// SendEmailRequest 빌더
val request = sendEmailRequest {
    source("noreply@example.com")
    destination(destination)
    message(message)
    replyToAddresses("support@example.com")
}
```

### Waiter 사용

```kotlin
import io.bluetape4k.aws.ses.waiters.*

// 이메일 전송 완료 대기
sesAsyncClient.waitUntilEmailExists { request ->
    // waiter 조건 설정
}
```

## 주요 기능 상세

| 파일                                      | 설명                  |
|-----------------------------------------|---------------------|
| `SesFactory.kt`                         | SES 클라이언트 팩토리       |
| `SesClientSupport.kt`                   | 동기 클라이언트 생성         |
| `SesAsyncClientSupport.kt`              | 비동기 클라이언트 생성        |
| `SesClientExtensions.kt`                | 동기 클라이언트 확장 함수      |
| `SesAsyncClientExtensions.kt`           | 비동기 클라이언트 확장 함수     |
| `SesAsyncClientCoroutinesExtensions.kt` | 코루틴 확장 함수           |
| `model/DestinationSupport.kt`           | Destination 빌더      |
| `model/MessageSupport.kt`               | Message 빌더          |
| `model/SendEmailRequestSupport.kt`      | SendEmailRequest 빌더 |
| `waiters/SesWaiterSupport.kt`           | 동기 Waiter           |
| `waiters/SesAsyncWaiterSupport.kt`      | 비동기 Waiter          |
