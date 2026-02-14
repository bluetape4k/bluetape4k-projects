# Module bluetape4k-aws-kotlin-sns

AWS SDK for Kotlin SNS 사용을 위한 확장 라이브러리입니다.

## 주요 기능

- **SNS Client 확장 함수**: 토픽 발행/구독 보조 API
- **요청 모델 빌더 지원**: Publish/Subscribe/Topic 관련 유틸
- **메시지 속성 지원**: MessageAttributeValue 보조

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-sns:${version}")
}
```

## 주요 기능 상세

- `SnsClientExtensions.kt`
- `model/Publish.kt`, `model/Subscribe.kt`
- `model/ListTopics.kt`, `model/ListSubscriptions.kt`, `model/Topic.kt`
