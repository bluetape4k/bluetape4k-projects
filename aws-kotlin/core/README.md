# Module bluetape4k-aws-kotlin-core

AWS SDK for Kotlin 사용 시 공통으로 필요한 보조 기능을 제공하는 라이브러리입니다.

## 주요 기능

- **Auth 지원**: 인증 관련 설정 보조
- **HTTP Engine 지원**: CRT/OkHttp 엔진 구성 유틸
- **코루틴 친화 구성**: AWS Kotlin SDK 기본 실행 모델에 맞춘 보조 API

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-core:${version}")
}
```

## 주요 기능 상세

- `auth/AuthSupport.kt`
- `http/CrtHttpEngineSupport.kt`
- `http/OkHttpEngineSupport.kt`
