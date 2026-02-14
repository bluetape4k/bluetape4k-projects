# Module bluetape4k-aws-kotlin-ses

AWS SDK for Kotlin SES 사용을 위한 확장 라이브러리입니다.

## 주요 기능

- **SES Client 확장 함수**: 메일 전송 호출 편의 API
- **요청 모델 빌더 지원**: Destination/Message/SendEmailRequest 유틸
- **Raw Email 지원**: RawMessage 기반 전송 유틸

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-ses:${version}")
}
```

## 주요 기능 상세

- `SesClientExtensions.kt`
- `model/Destination.kt`, `model/Message.kt`
- `model/SendEmailRequest.kt`, `model/SendRawEmailRequest.kt`
