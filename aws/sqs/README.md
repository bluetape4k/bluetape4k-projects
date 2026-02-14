# Module bluetape4k-aws-sqs

AWS SDK for Java v2 SQS 사용을 위한 확장 라이브러리입니다.

## 주요 기능

- **SQS Client 생성 지원**: 동기/비동기 클라이언트 보조
- **메시지 송수신 유틸**: Send/Receive/Delete 요청 빌더
- **Client 확장 함수**: SQS API 호출 편의 함수
- **Coroutine 브릿지**: Async API의 suspend 사용 지원

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-sqs:${version}")
}
```

## 주요 기능 상세

- `SqsFactory.kt`
- `SqsClientExtensions.kt`, `SqsAsyncClientExtensions.kt`
- `SqsAsyncClientCoroutinesExtensions.kt`
- `model/SendMessage.kt`, `model/ReceiveMessage.kt`, `model/DeleteMessage.kt`
