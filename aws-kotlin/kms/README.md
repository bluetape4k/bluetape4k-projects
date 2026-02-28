# Module bluetape4k-aws-kotlin-kms

AWS SDK for Kotlin KMS(Key Management Service) 사용을 위한 확장 라이브러리입니다.

## 개요

AWS KMS는 암호화 키를 생성하고 관리하는 AWS 관리형 서비스입니다.
이 모듈은 AWS SDK for Kotlin의 `KmsClient`를 보다 편리하게 생성하고 사용할 수 있도록
Kotlin 스타일의 DSL 팩토리 함수를 제공합니다.

## 주요 기능

- `kmsClientOf()` - Kotlin DSL 스타일의 `KmsClient` 팩토리 함수
- LocalStack 기반 통합 테스트 지원 (`AbstractKmsTest`)

## 의존성

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kotlin-kms:$version")
}
```

## 사용 방법

### KmsClient 생성

```kotlin
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.kms.kmsClientOf

// 기본 사용 (환경 변수 또는 IAM Role로 자동 인증)
val client = kmsClientOf(
    region = "ap-northeast-2"
)

// 엔드포인트 직접 지정 (LocalStack 등 로컬 환경)
val localClient = kmsClientOf(
    endpointUrl = Url.parse("http://localhost:4566"),
    region = "us-east-1",
    credentialsProvider = myCredentialsProvider
)

// 추가 설정이 필요한 경우 builder 람다 활용
val customClient = kmsClientOf(
    region = "ap-northeast-2"
) {
    retryStrategy = myRetryStrategy
}
```

### 대칭 키 생성 및 암호화/복호화

```kotlin
import aws.sdk.kotlin.services.kms.createKey
import aws.sdk.kotlin.services.kms.encrypt
import aws.sdk.kotlin.services.kms.decrypt
import aws.sdk.kotlin.services.kms.model.KeySpec
import aws.sdk.kotlin.services.kms.model.KeyUsageType
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String

// 대칭 키 생성
val createResp = client.createKey {
    keySpec = KeySpec.SymmetricDefault
    keyUsage = KeyUsageType.EncryptDecrypt
    description = "애플리케이션 데이터 암호화 키"
}
val keyId = createResp.keyMetadata?.keyId!!

// 데이터 암호화
val plaintext = "민감한 데이터"
val encryptResp = client.encrypt {
    this.keyId = keyId
    this.plaintext = plaintext.toUtf8Bytes()
}
val ciphertext = encryptResp.ciphertextBlob!!

// 데이터 복호화
val decryptResp = client.decrypt {
    this.keyId = keyId
    this.ciphertextBlob = ciphertext
}
val decrypted = decryptResp.plaintext!!.toUtf8String()
```

### Alias(별칭) 관리

```kotlin
import aws.sdk.kotlin.services.kms.createAlias
import aws.sdk.kotlin.services.kms.listAliases
import aws.sdk.kotlin.services.kms.deleteAlias

// Alias 생성 (반드시 "alias/" 접두사 필요)
client.createAlias {
    aliasName = "alias/my-app-key"
    targetKeyId = keyId
}

// Alias 목록 조회
val aliases = client.listAliases { limit = 100 }.aliases

// Alias 삭제
client.deleteAlias { aliasName = "alias/my-app-key" }
```

### Grant(권한 위임) 관리

```kotlin
import aws.sdk.kotlin.services.kms.createGrant
import aws.sdk.kotlin.services.kms.listGrants
import aws.sdk.kotlin.services.kms.revokeGrant
import aws.sdk.kotlin.services.kms.model.GrantOperation

// Grant 생성
val grantResp = client.createGrant {
    this.keyId = keyId
    granteePrincipal = "arn:aws:iam::123456789012:role/MyAppRole"
    operations = listOf(GrantOperation.Encrypt, GrantOperation.Decrypt)
}
val grantId = grantResp.grantId!!

// Grant 목록 조회
val grants = client.listGrants {
    this.keyId = keyId
    limit = 100
}.grants

// Grant 취소
client.revokeGrant {
    this.keyId = keyId
    this.grantId = grantId
}
```

### 키 정책(Key Policy) 설정

```kotlin
import aws.sdk.kotlin.services.kms.putKeyPolicy

val policy = """
    {
        "Version": "2012-10-17",
        "Statement": [
            {
                "Effect": "Allow",
                "Principal": {"AWS": "arn:aws:iam::123456789012:root"},
                "Action": "kms:*",
                "Resource": "*"
            }
        ]
    }
""".trimIndent()

client.putKeyPolicy {
    this.keyId = keyId
    policyName = "default"
    this.policy = policy
}
```

## 테스트

이 모듈은 [LocalStack](https://localstack.cloud/)을 사용한 통합 테스트를 제공합니다.
테스트 실행 시 Docker가 설치되어 있어야 합니다.

```bash
# 특정 모듈 테스트 실행
./gradlew :bluetape4k-aws-kotlin-kms:test

# 특정 테스트 클래스 실행
./gradlew :bluetape4k-aws-kotlin-kms:test --tests "io.bluetape4k.aws.kotlin.kms.KmsClientTest"
```

## 관련 모듈

| 모듈 | 설명 |
|------|------|
| `bluetape4k-aws-kotlin-core` | AWS Kotlin SDK 공통 유틸리티 |
| `bluetape4k-aws-kotlin-tests` | AWS Kotlin SDK 테스트 지원 (LocalStack) |
| `bluetape4k-aws-kms` | AWS Java SDK v2 기반 KMS 확장 (동기/비동기 클라이언트) |

## Java SDK vs Kotlin SDK 비교

| 기능 | `bluetape4k-aws-kms` (Java SDK) | `bluetape4k-aws-kotlin-kms` (Kotlin SDK) |
|------|------|------|
| 클라이언트 | `KmsClient`, `KmsAsyncClient` | `KmsClient` (단일 suspend 기반) |
| 비동기 방식 | `CompletableFuture` + `.await()` | Native suspend function |
| 코루틴 통합 | 별도 `kotlinx-coroutines-jdk8` 필요 | 기본 제공 |
