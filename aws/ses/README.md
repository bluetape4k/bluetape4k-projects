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

## 주요 기능 상세

- `SesFactory.kt`, `SesClientSupport.kt`, `SesAsyncClientSupport.kt`
- `SesClientExtensions.kt`, `SesAsyncClientExtensions.kt`
- `SesAsyncClientCoroutinesExtensions.kt`
- `model/*Support.kt`
- `waiters/*Support.kt`
