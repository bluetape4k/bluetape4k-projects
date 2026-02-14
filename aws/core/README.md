# Module bluetape4k-aws-core

AWS SDK for Java v2 사용 시 공통으로 필요한 보조 기능을 제공하는 라이브러리입니다.

## 주요 기능

- **Auth 지원**: Credentials/인증 구성 보조
- **HTTP Client 지원**: Apache/Netty/AWS CRT 클라이언트 선택 보조
- **Client Configuration 확장**: Override/Async 설정 유틸
- **Coroutines 연동**: AWS Async 호출의 코루틴 브릿지
- **SDK Core 확장**: `SdkBytes`, request override 설정 보조

## 의존성 추가

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-core:${version}")
}
```

## 주요 기능 상세

- `auth/AuthSupport.kt`
- `http/SdkHttpClientProvider.kt`, `http/SdkAsyncHttpClientProvider.kt`
- `http/AwsCrtAsyncHttpClientSupport.kt`, `http/NettyNioAsyncHttpClientSupport.kt`
- `client/ClientOverrideConfigurationSupport.kt`, `client/ClientAsyncConfigurationSupport.kt`
- `coroutines/AwsCoroutineSupport.kt`
