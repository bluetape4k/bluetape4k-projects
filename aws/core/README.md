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

## 사용 예시

### 인증 정보 구성

```kotlin
import io.bluetape4k.aws.auth.staticCredentialsProviderOf
import io.bluetape4k.aws.auth.LocalAwsCredentialsProvider

// 기본 인증 정보로 CredentialsProvider 생성
val credentialsProvider = staticCredentialsProviderOf("accessKey", "secretKey")

// 로컬 테스트용 기본 Provider 사용
val localProvider = LocalAwsCredentialsProvider
```

### Coroutine 연동

```kotlin
import io.bluetape4k.aws.coroutines.suspendCommand

// 동기 AWS 호출을 suspend 함수로 래핑
suspend fun fetchData(): Result = suspendCommand {
    someAwsClient.someOperation()
}

// 요청과 함께 사용
suspend fun fetchData(request: Request): Result = suspendCommand(request) { req ->
    someAwsClient.someOperation(req)
}
```

## 주요 기능 상세

| 파일                                               | 설명                      |
|--------------------------------------------------|-------------------------|
| `auth/AuthSupport.kt`                            | AWS 인증 정보 생성 유틸리티       |
| `http/SdkHttpClientProvider.kt`                  | 동기 HTTP 클라이언트 Provider  |
| `http/SdkAsyncHttpClientProvider.kt`             | 비동기 HTTP 클라이언트 Provider |
| `http/AwsCrtAsyncHttpClientSupport.kt`           | AWS CRT 기반 비동기 클라이언트    |
| `http/NettyNioAsyncHttpClientSupport.kt`         | Netty NIO 기반 비동기 클라이언트  |
| `client/ClientOverrideConfigurationSupport.kt`   | 클라이언트 Override 설정       |
| `client/ClientAsyncConfigurationSupport.kt`      | 비동기 클라이언트 설정            |
| `coroutines/AwsCoroutineSupport.kt`              | AWS 호출 코루틴 브릿지          |
| `core/SdkBytesSupport.kt`                        | SdkBytes 유틸리티           |
| `core/AwsRequestOverrideConfigurationSupport.kt` | 요청 Override 설정          |
