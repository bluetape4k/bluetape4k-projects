package io.bluetape4k.testcontainers.aws.ministack.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.MiniStackServer
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.CreateKeyRequest
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.DisableKeyRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest
import software.amazon.awssdk.services.kms.model.GrantOperation
import software.amazon.awssdk.services.kms.model.KeySpec
import software.amazon.awssdk.services.kms.model.KeyUsageType

/**
 * MiniStack KMS 서비스 통합 테스트.
 *
 * **알려진 제한사항 (MiniStack v1.3.14)**:
 * - `CreateGrant`, `ListGrants`, `RevokeGrant` 미지원 (Unknown action 400 오류 반환)
 * - 해당 테스트는 `@Disabled`로 표시되어 있으며, 향후 MiniStack 업그레이드 시 재활성화 가능
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MiniStackKMSTest: AbstractContainerTest() {

    companion object: KLogging()

    private val miniStack: MiniStackServer by lazy { MiniStackServer.Launcher.miniStack }

    private val kmsClient: KmsClient by lazy {
        KmsClient.builder()
            .endpointOverride(miniStack.awsEndpoint)
            .region(Region.of(miniStack.regionName))
            .credentialsProvider(miniStack.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    private val keyDesc = "Created by the MiniStack KMS API"
    private lateinit var keyId: String
    private val data = "동해물과 백두산이"
    private lateinit var encryptedData: SdkBytes
    private val granteePrincipal = ""
    private lateinit var grantId: String
    private val aliasName = "alias/MiniStackExampleName"

    @BeforeAll
    fun setup() {
        miniStack.start()
    }

    @Test
    @Order(1)
    fun `container loading`() {
        kmsClient.shouldNotBeNull()
    }

    @Test
    @Order(2)
    fun `create custom key`() {
        val response = kmsClient.createKey(
            CreateKeyRequest.builder()
                .description(keyDesc)
                .keySpec(KeySpec.SYMMETRIC_DEFAULT)
                .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                .build()
        )
        log.debug { "Created key with arn=${response.keyMetadata().arn()}" }
        keyId = response.keyMetadata().keyId()
        log.info { "custom keyId=$keyId" }
        keyId.shouldNotBeEmpty()
    }

    @Test
    @Order(3)
    fun `encrypt data`() {
        val response = kmsClient.encrypt(
            EncryptRequest.builder()
                .keyId(keyId)
                .plaintext(SdkBytes.fromUtf8String(data))
                .build()
        )
        log.debug { "Encryption algorithm: ${response.encryptionAlgorithmAsString()}" }
        encryptedData = response.ciphertextBlob()
    }

    @Test
    @Order(4)
    fun `decrypt data`() {
        val response = kmsClient.decrypt(
            DecryptRequest.builder()
                .keyId(keyId)
                .ciphertextBlob(encryptedData)
                .build()
        )
        response.plaintext().asUtf8String() shouldBeEqualTo data
    }

    @Test
    @Order(5)
    fun `disable customer key`() {
        val response = kmsClient.disableKey(DisableKeyRequest.builder().keyId(keyId).build())
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(6)
    fun `enable customer key`() {
        val response = kmsClient.enableKey { it.keyId(keyId) }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(7)
    @Disabled("MiniStack v1.3.14 미지원: CreateGrant (Unknown action 400) — 향후 업그레이드 시 재활성화")
    fun `create grant`() {
        val response = kmsClient.createGrant {
            it.keyId(keyId)
                .granteePrincipal(granteePrincipal)
                .operations(GrantOperation.CREATE_GRANT, GrantOperation.ENCRYPT, GrantOperation.DECRYPT)
        }
        log.debug { "Grant id=${response.grantId()}, token=${response.grantToken()}" }
        grantId = response.grantId()
    }

    @Test
    @Order(8)
    @Disabled("MiniStack v1.3.14 미지원: ListGrants (Unknown action 400) — 향후 업그레이드 시 재활성화")
    fun `list grants`() {
        val grants = kmsClient.listGrants { it.keyId(keyId).limit(15) }.grants()
        grants.forEach { log.debug { "Grant id=${it.grantId()}" } }
        grants.map { it.grantId() } shouldContain grantId
    }

    @Test
    @Order(9)
    @Disabled("MiniStack v1.3.14 미지원: RevokeGrant — create grant가 비활성화되어 grantId 없음")
    fun `revoke grant`() {
        val response = kmsClient.revokeGrant { it.keyId(keyId).grantId(grantId) }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(10)
    fun `describe key`() {
        val keyMetadata = kmsClient.describeKey { it.keyId(keyId) }.keyMetadata()
        log.debug { "key description=${keyMetadata.description()}, arn=${keyMetadata.arn()}" }
        keyMetadata.shouldNotBeNull()
    }

    @Test
    @Order(11)
    fun `create custom alias`() {
        val response = kmsClient.createAlias { it.aliasName(aliasName).targetKeyId(keyId) }
        log.debug { "CreateAlias metadata=${response.responseMetadata()}" }
    }

    @Test
    @Order(12)
    fun `list aliases`() {
        val response = kmsClient.listAliases { it.limit(10) }
        response.aliases().forEach { log.debug { "Alias name=$it" } }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(13)
    fun `delete alias`() {
        val response = kmsClient.deleteAlias { it.aliasName(aliasName) }
        log.debug { "DeleteAlias metadata=${response.responseMetadata()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(14)
    fun `list keys`() {
        val response = kmsClient.listKeys { it.limit(15) }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
        response.keys().forEach { log.debug { "Key arn=${it.keyArn()}, id=${it.keyId()}" } }
    }

    @Test
    @Order(15)
    fun `put key policy`() {
        val policy = """
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Effect": "Allow",
                        "Principal": {"AWS": "arn:aws:iam::814548047983:root"},
                        "Action": "kms:*",
                        "Resource": "*"
                    }
                ]
            }""".trimIndent()
        val response = kmsClient.putKeyPolicy {
            it.keyId(keyId).policyName("default").policy(policy)
        }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }
}
