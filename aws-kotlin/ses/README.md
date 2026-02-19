# Module bluetape4k-aws-kotlin-ses

AWS SDK for Kotlin SES 사용을 위한 확장 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 하며, SES v1 API를 제공합니다.
> SES v2 API를 사용하려면 `bluetape4k-aws-kotlin-sesv2` 모듈을 참조하세요.

## 주요 기능

- **SES Client 확장 함수**: 메일 전송 호출 편의 API
- **요청 모델 빌더 지원**: Destination/Message/SendEmailRequest 유틸
- **Raw Email 지원**: RawMessage 기반 전송 유틸
- **템플릿 이메일 지원**: 템플릿 기반 대량 발송

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-ses:${version}")
}
```

## 사용 예시

### 클라이언트 생성

```kotlin
import io.bluetape4k.aws.kotlin.ses.sesClientOf
import aws.smithy.kotlin.runtime.net.url.Url

val sesClient = sesClientOf(
    endpointUrl = Url.parse("http://localhost:4566"),
    region = "ap-northeast-2",
    credentialsProvider = credentialsProvider
)
```

### 기본 이메일 전송

```kotlin
import io.bluetape4k.aws.kotlin.ses.*
import aws.sdk.kotlin.services.ses.model.*

val request = SendEmailRequest {
    destination {
        toAddresses = listOf("recipient@example.com")
        ccAddresses = listOf("cc@example.com")
    }
    message {
        subject {
            data = "Hello from SES"
        }
        body {
            text {
                data = "This is plain text body"
            }
            html {
                data = "<h1>Hello</h1><p>This is HTML body</p>"
            }
        }
    }
    source = "sender@example.com"
}

val response = sesClient.send(request)
println("MessageId: ${response.messageId}")
```

### Raw 이메일 전송 (첨부파일 포함)

```kotlin
import io.bluetape4k.aws.kotlin.ses.model.*

val rawRequest = SendRawEmailRequest {
    rawMessage {
        data = """
            From: sender@example.com
            To: recipient@example.com
            Subject: Test Email with Attachment
            MIME-Version: 1.0
            Content-Type: multipart/mixed; boundary="boundary"

            --boundary
            Content-Type: text/plain

            This is the email body.
            --boundary
            Content-Type: application/pdf; name="document.pdf"
            Content-Transfer-Encoding: base64
            Content-Disposition: attachment; filename="document.pdf"

            ${Base64.getEncoder().encodeToString(pdfBytes)}
            --boundary--
        """.trimIndent()
    }
}

val response = sesClient.sendRaw(rawRequest)
```

### 템플릿 이메일 전송

```kotlin
// 템플릿 생성
val template = Template {
    templateName = "welcome-email"
    subjectPart = "Welcome, {{name}}!"
    htmlPart = "<h1>Hello {{name}}!</h1><p>Welcome to our service.</p>"
    textPart = "Hello {{name}}! Welcome to our service."
}
sesClient.createTemplate(template)

// 템플릿 조회
val existingTemplate = sesClient.getTemplateOrNull("welcome-email")

// 템플릿으로 이메일 발송
val templatedRequest = SendTemplatedEmailRequest {
    destination {
        toAddresses = listOf("recipient@example.com")
    }
    template {
        templateName = "welcome-email"
        templateData = """{"name": "John Doe"}"""
    }
    source = "sender@example.com"
}
val response = sesClient.sendTemplated(templatedRequest)
```

### 대량 템플릿 이메일 전송

```kotlin
import aws.sdk.kotlin.services.ses.model.*

val bulkRequest = SendBulkTemplatedEmailRequest {
    destinations = listOf(
        BulkEmailDestination {
            destination {
                toAddresses = listOf("user1@example.com")
            }
            replacementTemplateData = """{"name": "User 1"}"""
        },
        BulkEmailDestination {
            destination {
                toAddresses = listOf("user2@example.com")
            }
            replacementTemplateData = """{"name": "User 2"}"""
        }
    )
    defaultTemplate {
        templateName = "welcome-email"
    }
    source = "sender@example.com"
}

val response = sesClient.sendBulkTemplated(bulkRequest)
```

## 주요 기능 상세

| 파일                             | 설명                                               |
|--------------------------------|--------------------------------------------------|
| `SesClientExtensions.kt`       | SES 클라이언트 확장 함수 (send, sendRaw, sendTemplated 등) |
| `model/Destination.kt`         | Destination 빌더                                   |
| `model/Message.kt`             | Message 빌더 (Subject, Body)                       |
| `model/RawMessage.kt`          | RawMessage 빌더                                    |
| `model/SendEmailRequest.kt`    | SendEmailRequest 빌더                              |
| `model/SendRawEmailRequest.kt` | SendRawEmailRequest 빌더                           |
