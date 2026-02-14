# Module bluetape4k-aws-kotlin-sesv2

AWS SDK for Kotlin SESv2 사용을 위한 확장 라이브러리입니다.

## 주요 기능

- **SESv2 Client 확장 함수**: 이메일 전송 API 보조
- **요청 모델 빌더 지원**: Destination/Message/SendEmailRequest 유틸
- **Raw Message 지원**: RawMessage 기반 전송 유틸

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-sesv2:${version}")
}
```

## 주요 기능 상세

- `SesV2ClientExtensions.kt`
- `model/Destination.kt`, `model/Message.kt`
- `model/SendEmailRequest.kt`, `model/RawMessage.kt`
