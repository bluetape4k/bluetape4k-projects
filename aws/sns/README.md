# Module bluetape4k-aws-sns

AWS SDK for Java v2 SNS(Simple Notification Service) 사용을 위한 확장 라이브러리입니다.

## 주요 기능

- **SNS Client 생성 지원**: 동기/비동기 클라이언트 보조
- **Publish/Subscribe 지원**: 토픽 발행/구독 요청 빌더
- **Topic/Subscription 관리**: 생성/조회/속성 설정 유틸
- **Coroutine 브릿지**: Async API의 suspend 사용 지원

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-sns:${version}")
}
```

## 주요 기능 상세

- `SnsFactory.kt`, `SnsClientSupport.kt`, `SnsAsyncClientSupport.kt`
- `SnsClientExtensions.kt`, `SnsAsyncClientExtensions.kt`
- `SnsAsyncClientCoroutinesExtensions.kt`
- `model/Publish.kt`, `model/Subscribe.kt`, `model/CreateTopic.kt`
