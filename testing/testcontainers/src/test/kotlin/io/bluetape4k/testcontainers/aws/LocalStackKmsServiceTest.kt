package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.CreateAliasRequest
import software.amazon.awssdk.services.kms.model.CreateKeyRequest
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest
import software.amazon.awssdk.services.kms.model.DataKeySpec
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest
import software.amazon.awssdk.services.kms.model.KeyUsageType
import software.amazon.awssdk.services.kms.model.ListKeysRequest

/**
 * LocalStack을 사용한 AWS KMS 서비스 예제 테스트.
 *
 * 각 테스트는 독립적인 [LocalStackServer]를 사용하여 격리된 환경에서 실행됩니다.
 */
class LocalStackKmsServiceTest: AbstractContainerTest() {

    companion object: KLogging()

    private fun buildKmsClient(server: LocalStackServer): KmsClient =
        KmsClient.builder()
            .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.KMS))
            .region(Region.of(server.region))
            .credentialsProvider(server.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }

    @Test
    fun `KMS 대칭 키 생성 후 데이터 암호화 및 복호화`() {
        LocalStackServer().withServices(LocalStackContainer.Service.KMS).use { server ->
            server.start()
            val kmsClient = buildKmsClient(server)

            // 대칭 암호화 키 생성
            val keyId = kmsClient.createKey(
                CreateKeyRequest.builder()
                    .description("테스트용 대칭 암호화 키 (ENCRYPT_DECRYPT)")
                    .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                    .build()
            ).keyMetadata().keyId()
            keyId.shouldNotBeNull()

            // 키 목록 조회
            val keys = kmsClient.listKeys(ListKeysRequest.builder().build()).keys()
            keys.shouldNotBeEmpty()

            // 데이터 암호화
            val plainText = "민감한 개인정보 - KMS 암호화 테스트 🔐"
            val cipherBlob = kmsClient.encrypt(
                EncryptRequest.builder()
                    .keyId(keyId)
                    .plaintext(SdkBytes.fromUtf8String(plainText))
                    .build()
            ).ciphertextBlob()
            cipherBlob.shouldNotBeNull()

            // 데이터 복호화
            val decrypted = kmsClient.decrypt(
                DecryptRequest.builder()
                    .ciphertextBlob(cipherBlob)
                    .build()
            ).plaintext().asUtf8String()

            decrypted shouldBeEqualTo plainText
        }
    }

    @Test
    fun `KMS 키 별칭 생성 및 별칭으로 키 조회`() {
        LocalStackServer().withServices(LocalStackContainer.Service.KMS).use { server ->
            server.start()
            val kmsClient = buildKmsClient(server)

            // 키 생성
            val keyId = kmsClient.createKey(
                CreateKeyRequest.builder().description("별칭 테스트용 키").build()
            ).keyMetadata().keyId()

            // 별칭 등록
            val aliasName = "alias/bluetape4k-test-key"
            kmsClient.createAlias(
                CreateAliasRequest.builder()
                    .aliasName(aliasName)
                    .targetKeyId(keyId)
                    .build()
            )

            // 별칭으로 키 설명 조회
            val described = kmsClient.describeKey(
                DescribeKeyRequest.builder().keyId(aliasName).build()
            ).keyMetadata()
            described.keyId() shouldBeEqualTo keyId
        }
    }

    @Test
    fun `KMS GenerateDataKey로 Envelope Encryption 데이터 키 생성`() {
        LocalStackServer().withServices(LocalStackContainer.Service.KMS).use { server ->
            server.start()
            val kmsClient = buildKmsClient(server)

            // CMK(Customer Master Key) 생성
            val cmkId = kmsClient.createKey(
                CreateKeyRequest.builder().description("Envelope Encryption CMK").build()
            ).keyMetadata().keyId()

            // 데이터 키 생성 (평문 + 암호화된 데이터 키 반환)
            val dataKeyResponse = kmsClient.generateDataKey(
                GenerateDataKeyRequest.builder()
                    .keyId(cmkId)
                    .keySpec(DataKeySpec.AES_256)
                    .build()
            )
            // 평문 데이터 키 (로컬에서 직접 암호화에 사용 후 즉시 삭제) — 명시적 타입으로 플랫폼 타입 모호성 해소
            val plaintextKey: SdkBytes = checkNotNull(dataKeyResponse.plaintext()) { "plaintext is null" }
            // CMK로 암호화된 데이터 키 (안전하게 저장 가능)
            val encryptedKey: SdkBytes = checkNotNull(dataKeyResponse.ciphertextBlob()) { "ciphertextBlob is null" }

            // CMK로 암호화된 데이터 키를 복호화하면 원본 평문 데이터 키가 복원됨
            val decryptedKey: SdkBytes = checkNotNull(
                kmsClient.decrypt(
                    DecryptRequest.builder()
                        .ciphertextBlob(encryptedKey)
                        .build()
                ).plaintext()
            ) { "decrypted plaintext is null" }

            // ByteArray는 contentEquals로 내용 비교
            decryptedKey.asByteArray().contentEquals(plaintextKey.asByteArray()).shouldBeTrue()
        }
    }
}
