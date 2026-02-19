# Module bluetape4k-aws-kotlin-core

AWS SDK for Kotlin 사용 시 공통으로 필요한 보조 기능을 제공하는 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 합니다.
> AWS SDK for Java v2 기반 모듈은 `bluetape4k-aws-core`를 참조하세요.

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

## 사용 예시

### 인증 정보 구성

```kotlin
import io.bluetape4k.aws.kotlin.auth.staticCredentialsProviderOf
import io.bluetape4k.aws.kotlin.auth.LocalCredentialsProvider

// 커스텀 인증 정보로 CredentialsProvider 생성
val credentialsProvider = staticCredentialsProviderOf("accessKey", "secretKey")

// 로컬 테스트용 기본 Provider 사용 (accesskey/secretkey)
val localProvider = LocalCredentialsProvider
```

### HTTP Engine 구성

```kotlin
import io.bluetape4k.aws.kotlin.http.crtHttpEngineOf
import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine

// CRT 기반 HTTP 엔진 생성 (권장)
val httpEngine = crtHttpEngineOf()

// 클라이언트 생성 시 사용
val s3Client = S3Client {
    httpClient = httpEngine
    region = "ap-northeast-2"
    credentialsProvider = credentialsProvider
}
```

## 주요 기능 상세

| 파일                             | 설명                                                             |
|--------------------------------|----------------------------------------------------------------|
| `auth/AuthSupport.kt`          | AWS 인증 정보 생성 유틸리티 (`StaticCredentialsProvider`, `Credentials`) |
| `http/CrtHttpEngineSupport.kt` | AWS CRT 기반 HTTP 엔진 (권장, OkHttp 버전 충돌 방지)                       |
| `http/OkHttpEngineSupport.kt`  | OkHttp 기반 HTTP 엔진                                              |

## AWS SDK for Kotlin vs AWS SDK for Java v2

| 특징      | AWS SDK for Kotlin    | AWS SDK for Java v2           |
|---------|-----------------------|-------------------------------|
| API 스타일 | suspend 함수 (네이티브 코루틴) | CompletableFuture → 코루틴 변환 필요 |
| HTTP 엔진 | CRT (Smithy Kotlin)   | Netty, Apache, CRT            |
| 의존성 크기  | 경량                    | 상대적으로 무거움                     |
| 추천 용도   | 순수 Kotlin 프로젝트        | 기존 Java 프로젝트와 통합              |
