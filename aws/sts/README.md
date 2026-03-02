# Module bluetape4k-aws-sts

AWS SDK for Java v2 STS(Security Token Service) 사용을 위한 확장 라이브러리입니다.

## 개요

AWS STS는 AWS 계정에 대한 임시 자격 증명을 발급하는 관리형 서비스입니다.
이 모듈은 AWS SDK for Java v2의 `StsClient`(동기)와 `StsAsyncClient`(비동기)를 보다 편리하게
생성하고 사용할 수 있도록 Kotlin 스타일의 DSL 팩토리 함수와 Request 빌더 확장을 제공합니다.

## 주요 기능

- **호출자 신원 조회** (GetCallerIdentity): 계정 ID, 사용자 ID, ARN 반환
- **IAM 역할 임시 맡기** (AssumeRole): 임시 자격 증명(AccessKey, SecretKey, SessionToken) 발급
- **임시 세션 자격 증명 발급** (GetSessionToken): MFA 인증 기반 임시 자격 증명
- **3단계 API 지원**: 동기 / 비동기 (CompletableFuture) / Coroutines
- **LocalStack 기반 통합 테스트 지원**: `AbstractStsTest`
- **자동 클라이언트 종료**: `ShutdownQueue` 등록으로 JVM 종료 시 자동 정리

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-aws-sts:${version}")
}
```

## 사용 방법

### StsClient 생성 (동기)

```kotlin
import io.bluetape4k.aws.sts.StsClientFactory
import software.amazon.awssdk.regions.Region
import java.net.URI

// DSL 빌더 방식
val client = StsClientFactory.Sync.create {
    region(Region.AP_NORTHEAST_2)
}

// 파라미터 직접 지정 방식 (LocalStack 등 로컬 환경)
val localClient = StsClientFactory.Sync.create(
    endpointOverride = URI.create("http://localhost:4566"),
    region = Region.US_EAST_1,
    credentialsProvider = myCredentialsProvider
)
```

### StsAsyncClient 생성 (비동기)

```kotlin
import io.bluetape4k.aws.sts.StsClientFactory

val asyncClient = StsClientFactory.Async.create(
    endpointOverride = URI.create("http://localhost:4566"),
    region = Region.US_EAST_1,
    credentialsProvider = myCredentialsProvider
)
```

### 호출자 신원 조회

```kotlin
import io.bluetape4k.aws.sts.*

// 동기
val response = client.getCallerIdentity()
println("account=${response.account()}, userId=${response.userId()}, arn=${response.arn()}")

// 비동기 (CompletableFuture)
val futureResponse = asyncClient.getCallerIdentityAsync().join()
println("account=${futureResponse.account()}")

// 코루틴
suspend fun getIdentity() {
    val response = asyncClient.getCallerIdentity()
    println("account=${response.account()}")
}
```

### IAM 역할 임시 맡기

```kotlin
// 동기
val response = client.assumeRole(
    roleArn = "arn:aws:iam::123456789012:role/MyRole",
    sessionName = "my-session",
    durationSeconds = 3600
)
val credentials = response.credentials()
println("accessKeyId=${credentials.accessKeyId()}")
println("secretAccessKey=${credentials.secretAccessKey()}")
println("sessionToken=${credentials.sessionToken()}")

// 비동기 (CompletableFuture)
val futureResponse = asyncClient.assumeRoleAsync(
    roleArn = "arn:aws:iam::123456789012:role/MyRole",
    sessionName = "my-session",
    durationSeconds = 3600
).join()
println("accessKeyId=${futureResponse.credentials().accessKeyId()}")

// 코루틴
suspend fun assumeRole() {
    val response = asyncClient.assumeRole(
        roleArn = "arn:aws:iam::123456789012:role/MyRole",
        sessionName = "coroutine-session",
        durationSeconds = 3600
    )
    println("accessKeyId=${response.credentials().accessKeyId()}")
}
```

### 임시 세션 자격 증명 발급

```kotlin
// 동기
val response = client.getSessionToken(durationSeconds = 900)
println("sessionToken=${response.credentials().sessionToken()}")

// 비동기 (CompletableFuture)
val futureResponse = asyncClient.getSessionTokenAsync(durationSeconds = 900).join()
println("sessionToken=${futureResponse.credentials().sessionToken()}")

// 코루틴
suspend fun getSessionToken() {
    val response = asyncClient.getSessionToken(durationSeconds = 900)
    println("sessionToken=${response.credentials().sessionToken()}")
}
```

### Request 빌더 (고급 사용)

```kotlin
import io.bluetape4k.aws.sts.model.*

// GetCallerIdentityRequest 빌드
val identityReq = getCallerIdentityRequest {}

// AssumeRoleRequest 빌드
val assumeRoleReq = assumeRoleRequestOf(
    roleArn = "arn:aws:iam::123456789012:role/MyRole",
    sessionName = "my-session"
) {
    durationSeconds(3600)
}

// GetSessionTokenRequest 빌드
val sessionReq = getSessionTokenRequestOf(durationSeconds = 900) {
    // 추가 설정 가능
}

// 또는 DSL 방식
val sessionReq2 = getSessionTokenRequest {
    durationSeconds(900)
}
```

## 주요 파일 구조

| 파일                                          | 설명                   |
|----------------------------------------------|----------------------|
| `StsClientFactory.kt`                        | STS 클라이언트 팩토리        |
| `StsClientSupport.kt`                        | 동기 클라이언트 생성          |
| `StsAsyncClientSupport.kt`                   | 비동기 클라이언트 생성         |
| `StsClientExtensions.kt`                     | 동기 클라이언트 확장 함수       |
| `StsAsyncClientExtensions.kt`                | 비동기 클라이언트 확장 함수      |
| `StsAsyncClientCoroutinesExtensions.kt`      | 코루틴 확장 함수            |
| `model/GetCallerIdentity.kt`                 | GetCallerIdentityRequest 빌더 |
| `model/AssumeRole.kt`                        | AssumeRoleRequest 빌더 |
| `model/GetSessionToken.kt`                   | GetSessionTokenRequest 빌더 |

## 테스트

이 모듈은 [LocalStack](https://localstack.cloud/)을 사용한 통합 테스트를 제공합니다.
테스트 실행 시 Docker가 설치되어 있어야 합니다.

```bash
# 특정 모듈 테스트 실행
./gradlew :bluetape4k-aws-sts:test

# 동기 클라이언트 테스트
./gradlew :bluetape4k-aws-sts:test --tests "io.bluetape4k.aws.sts.StsClientTest"

# 비동기 클라이언트 테스트
./gradlew :bluetape4k-aws-sts:test --tests "io.bluetape4k.aws.sts.StsAsyncClientCoroutinesExtensionsTest"
```

### 테스트 기반 클래스

```kotlin
import io.bluetape4k.aws.sts.AbstractStsTest

class MyTest: AbstractStsTest() {
    private val client: StsClient by lazy { /* 기존 client 사용 */ }
    private val asyncClient: StsAsyncClient by lazy { /* 기존 asyncClient 사용 */ }

    // 테스트 작성...
}
```

## 관련 모듈

| 모듈 | 설명 |
|------|------|
| `bluetape4k-aws-core` | AWS Java SDK v2 공통 유틸리티 |
| `bluetape4k-aws-kotlin-sts` | AWS Kotlin SDK 기반 STS 확장 (native suspend 지원) |

## Java SDK vs Kotlin SDK 비교

| 기능 | `bluetape4k-aws-sts` (Java SDK) | `bluetape4k-aws-kotlin-sts` (Kotlin SDK) |
|------|------|------|
| 동기 클라이언트 | `StsClient` | - |
| 비동기 클라이언트 | `StsAsyncClient` + `CompletableFuture` | `StsClient` (suspend) |
| Coroutines 지원 | `.await()` 변환 필요 | 기본 제공 |
| Request 빌더 | `model/` 패키지 DSL 제공 | AWS SDK 내장 DSL 사용 |

## 주의사항

- 생성된 클라이언트는 자동으로 `ShutdownQueue`에 등록되어 JVM 종료 시 정리됩니다.
- LocalStack 테스트 환경에서는 임의의 역할 ARN으로도 임시 자격 증명을 발급할 수 있습니다.
- 코루틴 환경에서 `getCallerIdentity()` suspend 함수를 사용할 때는 `StsAsyncClient`를 사용해야 합니다.
