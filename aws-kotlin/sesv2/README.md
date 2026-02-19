# Module bluetape4k-aws-kotlin-sesv2

AWS SDK for Kotlin SESv2 사용을 위한 확장 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 하며, SES v2 API를 제공합니다.
> LocalStack은 현재 SESv2를 지원하지 않으므로, 실제 AWS 환경에서 테스트해야 합니다.

## 주요 기능

- **SESv2 Client 확장 함수**: 이메일 전송 API 보조
- **요청 모델 빌더 지원**: Destination/Message/SendEmailRequest 유틸
- **Raw Message 지원**: RawMessage 기반 전송 유틸
- **대량 이메일 지원**: Bulk Email 발송

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-sesv2:${version}")
}
```

## 사용 예시

### 클라이언트 생성

```kotlin
import io.bluetape4k.aws.kotlin.sesv2.sesV2ClientOf

val sesV2Client = sesV2ClientOf(
    region = "ap-northeast-2",
    credentialsProvider = credentialsProvider
)
```

### 기본 이메일 전송

```kotlin
import io.bluetape4k.aws.kotlin.sesv2.*
import aws.sdk.kotlin.services.sesv2.model.*

val request = SendEmailRequest {
    destination {
        toAddresses = listOf("recipient@example.com")
        ccAddresses = listOf("cc@example.com")
    }
    content {
        simple {
            subject {
                data = "Hello from SESv2"
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
    }
    fromEmailAddress = "sender@example.com"
}

val response = sesV2Client.send(request)
println("MessageId: ${response.messageId}")
```

### 템플릿 이메일 전송

```kotlin
val templatedRequest = SendEmailRequest {
    destination {
        toAddresses = listOf("recipient@example.com")
    }
    content {
        template {
            templateName = "welcome-email"
            templateData = """{"name": "John Doe"}"""
        }
    }
    fromEmailAddress = "sender@example.com"
}

val response = sesV2Client.send(templatedRequest)
```

### 대량 이메일 전송

```kotlin
import aws.sdk.kotlin.services.sesv2.model.*

val bulkRequest = SendBulkEmailRequest {
    bulkEmailEntries = listOf(
        BulkEmailEntry {
            destination {
                toAddresses = listOf("user1@example.com")
            }
            replacementTags = listOf(MessageTag { name = "name"; value = "User 1" })
        },
        BulkEmailEntry {
            destination {
                toAddresses = listOf("user2@example.com")
            }
            replacementTags = listOf(MessageTag { name = "name"; value = "User 2" })
        }
    )
    defaultContent {
        template {
            templateName = "welcome-email"
        }
    }
    fromEmailAddress = "sender@example.com"
}

val response = sesV2Client.sendBulk(bulkRequest)
```

### 템플릿 관리

```kotlin
// 템플릿 조회
val template = sesV2Client.getTemplateOrNull("welcome-email")

// 템플릿이 존재하는지 확인
if (template != null) {
    println("Template found: ${template.templateName}")
}
```

## 주요 기능 상세

| 파일                          | 설명                                                    |
|-----------------------------|-------------------------------------------------------|
| `SesV2ClientExtensions.kt`  | SESv2 클라이언트 확장 함수 (send, sendBulk, getTemplateOrNull) |
| `model/Destination.kt`      | Destination 빌더                                        |
| `model/Message.kt`          | Message 빌더                                            |
| `model/RawMessage.kt`       | RawMessage 빌더                                         |
| `model/SendEmailRequest.kt` | SendEmailRequest 빌더                                   |

## SES v1 vs SES v2 비교

| 특징            | SES v1                                  | SES v2               |
|---------------|-----------------------------------------|----------------------|
| API 구조        | 별도의 메서드 (sendEmail, sendTemplatedEmail) | 통합된 SendEmailRequest |
| 대량 발송         | SendBulkTemplatedEmail                  | SendBulkEmail        |
| LocalStack 지원 | 지원                                      | 미지원                  |
| 추천 용도         | LocalStack 테스트 필요 시                     | 최신 AWS 기능 사용 시       |
