# Module bluetape4k-aws-kotlin-sts

AWS SDK for Kotlin STS(Security Token Service) 사용을 위한 확장 라이브러리입니다.

> **참고**: 이 모듈은 AWS SDK for Kotlin (네이티브 Kotlin SDK)을 기반으로 합니다.

## 개요

AWS Security Token Service(STS)는 AWS 리소스에 대한 액세스를 제어할 수 있는
임시 제한 권한 자격 증명을 요청할 수 있는 웹 서비스입니다.

이 모듈은 AWS SDK for Kotlin의 `StsClient`를 보다 편리하게 생성하고 사용할 수 있도록
Kotlin 스타일의 DSL 팩토리 함수와 유틸리티를 제공합니다.

## 주요 기능

- **STS Client 팩토리**: `stsClientOf()` - Kotlin DSL 스타일의 `StsClient` 생성
- **호출자 신원 조회**: `getCallerIdentity()` - 계정 ID, 사용자 ID, ARN 조회
- **IAM 역할 임시 맡기**: `assumeRole()` - 임시 자격 증명 발급
- **세션 토큰 발급**: `getSessionToken()` - MFA 기반 임시 자격 증명 발급
- **요청 모델 빌더**: `assumeRoleRequestOf()` DSL - 역할 임시 맡기 요청 구성

## 의존성 추가

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-aws-kotlin-sts:${version}")
}
```

## 사용 예시

### StsClient 생성

```kotlin
import io.bluetape4k.aws.kotlin.sts.stsClientOf
import aws.smithy.kotlin.runtime.net.url.Url

// 기본 사용 (환경 변수 또는 IAM Role로 자동 인증)
val client = stsClientOf(
    region = "ap-northeast-2"
)

// 엔드포인트 직접 지정 (LocalStack 등 로컬 환경)
val localClient = stsClientOf(
    endpointUrl = Url.parse("http://localhost:4566"),
    region = "us-east-1",
    credentialsProvider = myCredentialsProvider
)

// 추가 설정이 필요한 경우 builder 람다 활용
val customClient = stsClientOf(
    region = "ap-northeast-2"
) {
    retryStrategy = myRetryStrategy
}
```

### 호출자 신원 조회

현재 AWS 자격 증명의 호출자 신원 정보를 조회합니다.

```kotlin
import io.bluetape4k.aws.kotlin.sts.getCallerIdentity

val response = client.getCallerIdentity()
println("account=${response.account}")
println("userId=${response.userId}")
println("arn=${response.arn}")
```

### IAM 역할 임시 맡기

IAM 역할을 임시로 맡아(Assume) 임시 자격 증명을 발급합니다.

```kotlin
import io.bluetape4k.aws.kotlin.sts.assumeRole

// 기본 사용 (기본 유효 시간: 3600초)
val response = client.assumeRole(
    roleArn = "arn:aws:iam::123456789012:role/MyRole",
    sessionName = "my-session"
)

// 유효 시간 커스터마이징 (900초 = 15분)
val response = client.assumeRole(
    roleArn = "arn:aws:iam::123456789012:role/MyRole",
    sessionName = "my-session",
    durationSeconds = 900
)

// 임시 자격 증명 사용
val credentials = response.credentials
println("accessKeyId=${credentials?.accessKeyId}")
println("secretAccessKey=${credentials?.secretAccessKey}")
println("sessionToken=${credentials?.sessionToken}")
println("expiration=${credentials?.expiration}")
```

### AssumeRoleRequest 상세 설정

DSL 빌더를 사용하여 추가 옵션을 설정할 수 있습니다.

```kotlin
import io.bluetape4k.aws.kotlin.sts.model.assumeRoleRequestOf
import aws.sdk.kotlin.services.sts.assumeRole

val request = assumeRoleRequestOf(
    roleArn = "arn:aws:iam::123456789012:role/MyRole",
    sessionName = "my-session"
) {
    durationSeconds = 3600
    externalId = "optional-external-id"
    serialNumber = "arn:aws:iam::123456789012:mfa/user" // MFA 기기 일련번호
    tokenCode = "123456" // MFA 토큰
}

val response = client.assumeRole(request)
```

### 세션 토큰 발급

MFA 인증 기반의 임시 세션 자격 증명을 발급합니다.

```kotlin
import io.bluetape4k.aws.kotlin.sts.getSessionToken

// 기본 사용 (기본 유효 시간: 3600초)
val response = client.getSessionToken()

// 유효 시간 커스터마이징 (900초 = 15분)
val response = client.getSessionToken(durationSeconds = 900)

val credentials = response.credentials
println("accessKeyId=${credentials?.accessKeyId}")
println("secretAccessKey=${credentials?.secretAccessKey}")
println("sessionToken=${credentials?.sessionToken}")
println("expiration=${credentials?.expiration}")
```

## 주요 파일

| 파일 | 설명 |
|------|------|
| `StsClientSupport.kt` | STS 클라이언트 팩토리 및 suspend 함수 확장 |
| `model/AssumeRole.kt` | AssumeRoleRequest 빌더 DSL |

## 실전 예시

### 크로스 어카운트 접근 (Cross-Account Access)

다른 AWS 계정의 역할에 접근하는 예시입니다.

```kotlin
import io.bluetape4k.aws.kotlin.sts.stsClientOf
import io.bluetape4k.aws.kotlin.sts.assumeRole
import aws.sdk.kotlin.services.s3.S3Client

// 1. 현재 계정의 STS 클라이언트로 다른 계정의 역할 임시 맡기
val stsClient = stsClientOf(region = "ap-northeast-2")

val assumeRoleResponse = stsClient.assumeRole(
    roleArn = "arn:aws:iam::999999999999:role/CrossAccountRole",
    sessionName = "cross-account-session",
    durationSeconds = 3600
)

// 2. 임시 자격 증명으로 S3 클라이언트 생성
val credentials = assumeRoleResponse.credentials!!
val s3Client = S3Client {
    region = "ap-northeast-2"
    credentialsProvider = StaticCredentialsProvider(
        AwsCredentials(
            accessKeyId = credentials.accessKeyId!!,
            secretAccessKey = credentials.secretAccessKey!!,
            sessionToken = credentials.sessionToken
        )
    )
}

// 3. 다른 계정의 S3 버킷에 접근
val bucketObjects = s3Client.listObjects {
    bucket = "cross-account-bucket"
}
```

### 외부 ID를 사용한 안전한 역할 임시 맡기

외부 ID를 사용하여 역할 임시 맡기의 보안성을 강화합니다.

```kotlin
import io.bluetape4k.aws.kotlin.sts.model.assumeRoleRequestOf

val request = assumeRoleRequestOf(
    roleArn = "arn:aws:iam::999999999999:role/CrossAccountRole",
    sessionName = "secure-session"
) {
    durationSeconds = 1800 // 30분
    externalId = "d3d5a9c8-2b4e-4c3f-9a1b-8e5d7c6f4a2b" // 역할 신뢰 정책에서 요구하는 외부 ID
}

val response = stsClient.assumeRole(request)
```

## 테스트

이 모듈은 [LocalStack](https://localstack.cloud/)을 사용한 통합 테스트를 제공합니다.
테스트 실행 시 Docker가 설치되어 있어야 합니다.

```bash
# 특정 모듈 테스트 실행
./gradlew :bluetape4k-aws-kotlin-sts:test

# 특정 테스트 클래스 실행
./gradlew :bluetape4k-aws-kotlin-sts:test --tests "io.bluetape4k.aws.kotlin.sts.StsClientTest"
```

## 관련 모듈

| 모듈 | 설명 |
|------|------|
| `bluetape4k-aws-kotlin-core` | AWS Kotlin SDK 공통 유틸리티 |
| `bluetape4k-aws-kotlin-tests` | AWS Kotlin SDK 테스트 지원 (LocalStack) |
| `bluetape4k-aws-sts` | AWS Java SDK v2 기반 STS 확장 (동기/비동기 클라이언트) |

## Java SDK vs Kotlin SDK 비교

| 기능 | `bluetape4k-aws-sts` (Java SDK) | `bluetape4k-aws-kotlin-sts` (Kotlin SDK) |
|------|------|------|
| 클라이언트 | `StsClient`, `StsAsyncClient` | `StsClient` (단일 suspend 기반) |
| 비동기 방식 | `CompletableFuture` + `.await()` | Native suspend function |
| 코루틴 통합 | 별도 `kotlinx-coroutines-jdk8` 필요 | 기본 제공 |
