# Module bluetape4k-aws-kms

AWS SDK for Java v2 KMS(Key Management Service) 사용을 위한 확장 라이브러리입니다.

## 개요

AWS KMS는 암호화 키를 생성하고 관리하는 AWS 관리형 서비스입니다.
이 모듈은 AWS SDK for Java v2의 `KmsClient`(동기)와 `KmsAsyncClient`(비동기)를 보다 편리하게
생성하고 사용할 수 있도록 Kotlin 스타일의 DSL 팩토리 함수와 Request 빌더 확장을 제공합니다.

## 주요 기능

- `kmsClient()` / `kmsClientOf()` - Kotlin DSL 스타일의 동기식 `KmsClient` 팩토리 함수
- `kmsAsyncClient()` / `kmsAsyncClientOf()` - Kotlin DSL 스타일의 비동기식 `KmsAsyncClient` 팩토리 함수
- `model/` 패키지 - 모든 KMS Request 타입에 대한 Kotlin DSL 빌더 함수 제공
- JVM 종료 시 클라이언트 자동 종료 (`ShutdownQueue` 등록)
- LocalStack 기반 통합 테스트 지원 (`AbstractKmsTest`)

## 의존성

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-aws-kms:$version")
}
```

## 사용 방법

### KmsClient 생성 (동기)

```kotlin
import io.bluetape4k.aws.kms.kmsClient
import io.bluetape4k.aws.kms.kmsClientOf
import software.amazon.awssdk.regions.Region
import java.net.URI

// DSL 빌더 방식
val client = kmsClient {
    region(Region.AP_NORTHEAST_2)
}

// 파라미터 직접 지정 방식 (LocalStack 등 로컬 환경)
val localClient = kmsClientOf(
    endpointOverride = URI.create("http://localhost:4566"),
    region = Region.US_EAST_1,
    credentialsProvider = myCredentialsProvider
)
```

### KmsAsyncClient 생성 (비동기)

```kotlin
import io.bluetape4k.aws.kms.kmsAsyncClient
import io.bluetape4k.aws.kms.kmsAsyncClientOf
import kotlinx.coroutines.future.await

val asyncClient = kmsAsyncClientOf(
    endpointOverride = URI.create("http://localhost:4566"),
    region = Region.US_EAST_1,
    credentialsProvider = myCredentialsProvider
) {}

// CompletableFuture → Coroutines 변환
val response = asyncClient.createKey { ... }.await()
```

### 대칭 키 생성 및 암호화/복호화

```kotlin
import io.bluetape4k.aws.kms.model.createKeyRequestOf
import io.bluetape4k.aws.kms.model.encryptRequestOf
import io.bluetape4k.aws.kms.model.decryptRequestOf
import io.bluetape4k.aws.core.toUtf8SdkBytes
import software.amazon.awssdk.services.kms.model.KeySpec
import software.amazon.awssdk.services.kms.model.KeyUsageType

// 대칭 키 생성
val createResp = client.createKey(
    createKeyRequestOf(
        description = "애플리케이션 데이터 암호화 키",
        keySpec = KeySpec.SYMMETRIC_DEFAULT,
        keyUsage = KeyUsageType.ENCRYPT_DECRYPT
    )
)
val keyId = createResp.keyMetadata().keyId()

// 데이터 암호화
val encryptResp = client.encrypt(
    encryptRequestOf(keyId = keyId, plainText = "민감한 데이터".toUtf8SdkBytes())
)
val ciphertext = encryptResp.ciphertextBlob()

// 데이터 복호화
val decryptResp = client.decrypt(decryptRequestOf(keyId = keyId, ciphertextBlob = ciphertext))
val decrypted = decryptResp.plaintext().asUtf8String()
```

### Alias(별칭) 관리

```kotlin
import io.bluetape4k.aws.kms.model.createAliasRequestOf
import io.bluetape4k.aws.kms.model.listAliasesRequestOf
import io.bluetape4k.aws.kms.model.deleteAliasOf

// Alias 생성 (반드시 "alias/" 접두사 필요)
client.createAlias(createAliasRequestOf("alias/my-app-key", keyId))

// Alias 목록 조회
val aliases = client.listAliases(listAliasesRequestOf(limit = 100)).aliases()

// Alias 삭제
client.deleteAlias(deleteAliasOf("alias/my-app-key"))
```

### Grant(권한 위임) 관리

```kotlin
import io.bluetape4k.aws.kms.model.createGrantRequestOf
import io.bluetape4k.aws.kms.model.revokeGrantRequestOf
import software.amazon.awssdk.services.kms.model.GrantOperation

// Grant 생성
val grantResp = client.createGrant(
    createGrantRequestOf(
        keyId = keyId,
        granteePrincipal = "arn:aws:iam::123456789012:role/MyAppRole",
        GrantOperation.ENCRYPT, GrantOperation.DECRYPT
    )
)
val grantId = grantResp.grantId()

// Grant 취소
client.revokeGrant(revokeGrantRequestOf(keyId, grantId))
```

### 키 정책(Key Policy) 설정

```kotlin
import io.bluetape4k.aws.kms.model.putKeyPolicyRequestOf

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

client.putKeyPolicy(putKeyPolicyRequestOf(keyId, "default", policy))
```

### 키 비활성화/활성화

```kotlin
import io.bluetape4k.aws.kms.model.disableKeyRequestOf
import io.bluetape4k.aws.kms.model.enableKeyRequestOf

client.disableKey(disableKeyRequestOf(keyId))   // 비활성화
client.enableKey(enableKeyRequestOf(keyId))     // 다시 활성화
```

## Request 빌더 함수 목록

| 함수 | 설명 |
|------|------|
| `createKeyRequest` / `createKeyRequestOf` | KMS 키 생성 요청 |
| `encryptRequest` / `encryptRequestOf` | 데이터 암호화 요청 |
| `decryptRequest` / `decryptRequestOf` | 데이터 복호화 요청 |
| `createAliasRequest` / `createAliasRequestOf` | Alias 생성 요청 |
| `deleteAlias` / `deleteAliasOf` | Alias 삭제 요청 |
| `listAliasesRequest` / `listAliasesRequestOf` | Alias 목록 조회 요청 |
| `createGrantRequest` / `createGrantRequestOf` | Grant 생성 요청 |
| `listGrantsRequest` / `listGrantsRequestOf` | Grant 목록 조회 요청 |
| `revokeGrantRequest` / `revokeGrantRequestOf` | Grant 취소 요청 |
| `describeKey` / `describeKeyOf` | 키 메타데이터 조회 요청 |
| `disableKeyRequest` / `disableKeyRequestOf` | 키 비활성화 요청 |
| `enableKeyRequest` / `enableKeyRequestOf` | 키 활성화 요청 |
| `putKeyPolicyRequest` / `putKeyPolicyRequestOf` | 키 정책 설정 요청 |
| `listKeysRequest` / `listKeysRequestOf` | 키 목록 조회 요청 |

## 테스트

이 모듈은 [LocalStack](https://localstack.cloud/)을 사용한 통합 테스트를 제공합니다.
테스트 실행 시 Docker가 설치되어 있어야 합니다.

```bash
# 특정 모듈 테스트 실행
./gradlew :bluetape4k-aws-kms:test

# 동기 클라이언트 테스트
./gradlew :bluetape4k-aws-kms:test --tests "io.bluetape4k.aws.kms.KsmClientTest"

# 비동기 클라이언트 테스트
./gradlew :bluetape4k-aws-kms:test --tests "io.bluetape4k.aws.kms.KmsAsyncClientTest"
```

## 관련 모듈

| 모듈 | 설명 |
|------|------|
| `bluetape4k-aws-core` | AWS Java SDK v2 공통 유틸리티 |
| `bluetape4k-aws-kotlin-kms` | AWS Kotlin SDK 기반 KMS 확장 (native suspend 지원) |

## Java SDK vs Kotlin SDK 비교

| 기능 | `bluetape4k-aws-kms` (Java SDK) | `bluetape4k-aws-kotlin-kms` (Kotlin SDK) |
|------|------|------|
| 동기 클라이언트 | `KmsClient` | - |
| 비동기 클라이언트 | `KmsAsyncClient` + `CompletableFuture` | `KmsClient` (suspend) |
| Coroutines 지원 | `.await()` 변환 필요 | 기본 제공 |
| Request 빌더 | `model/` 패키지 DSL 제공 | AWS SDK 내장 DSL 사용 |
