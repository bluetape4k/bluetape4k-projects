# Module bluetape4k-aws-kotlin-sqs

AWS SDK for Kotlin SQS 사용을 위한 확장 라이브러리입니다.

## 주요 기능

- **SQS Client 확장 함수**: 메시지 송수신/삭제 보조 API
- **요청 모델 빌더 지원**: Send/Receive/Delete 요청 생성 유틸
- **메시지 속성 지원**: MessageAttributeValue 보조
- **가시성 제어 지원**: MessageVisibility 유틸

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-sqs:${version}")
}
```

## 주요 기능 상세

- `SqsClientExtensions.kt`
- `model/SendMessage.kt`, `model/ReceiveMessage.kt`, `model/DeleteMessage.kt`
- `model/MessageAttributeValue.kt`, `model/MessageVisibility.kt`
