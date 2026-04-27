package io.bluetape4k.testcontainers.aws.floci.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.FlociServer
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
import software.amazon.awssdk.services.kms.model.DecryptResponse
import software.amazon.awssdk.services.kms.model.DisableKeyRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest
import software.amazon.awssdk.services.kms.model.GrantOperation
import software.amazon.awssdk.services.kms.model.KeySpec
import software.amazon.awssdk.services.kms.model.KeyUsageType

/**
 * [FlociServer]를 사용한 KMS 서비스 통합 테스트.
 *
 * LocalStack 기반 [io.bluetape4k.testcontainers.aws.services.KMSTest]에 대응합니다.
 *
 * > **알려진 제한사항 1**: Floci #586 — 비대칭 키(asymmetric key)의 `GetKeyRotationStatus` 동작 불가.
 * > 비대칭 키 관련 테스트는 포함하지 않습니다.
 * > **알려진 제한사항 2**: `DisableKey`, `EnableKey`, `CreateGrant`, `ListGrants`, `RevokeGrant` 미지원 (Status 400).
 * > 해당 테스트는 `@Disabled`로 표시합니다.
 */
@Suppress("DEPRECATION")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FlociKMSTest: AbstractContainerTest() {

    companion object: KLogging()

    private val floci: FlociServer
        get() = FlociServer.Launcher.floci

    private val kmsClient: KmsClient by lazy {
        KmsClient.builder()
            .endpointOverride(floci.awsEndpoint)
            .region(Region.of(floci.regionName))
            .credentialsProvider(floci.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    private val keyDesc = "Create by the AWS KMS API"
    private lateinit var keyId: String

    private val data = "동해물과 백두산이"
    private lateinit var encryptedData: SdkBytes

    private val granteePrincipal = ""
    private lateinit var grantId: String

    private val aliasName = "alias/ExampleName"

    @BeforeAll
    fun setup() {
        floci.isRunning.shouldBeTrue()
    }

    @Test
    @Order(1)
    fun `container loading`() {
        kmsClient.shouldNotBeNull()
    }

    @Test
    @Order(2)
    fun `create custom key`() {
        val keyRequest = CreateKeyRequest.builder()
            .description(keyDesc)
            .keySpec(KeySpec.SYMMETRIC_DEFAULT)
            .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
            .build()

        val response = kmsClient.createKey(keyRequest)
        log.debug { "Created a custom key with id=${response.keyMetadata().arn()}" }

        keyId = response.keyMetadata().keyId()
        log.info { "custom keyId=$keyId" }
        keyId.shouldNotBeEmpty()
    }

    @Test
    @Order(3)
    fun `encrypt data`() {
        val encryptRequest = EncryptRequest.builder()
            .keyId(keyId)
            .plaintext(SdkBytes.fromUtf8String(data))
            .build()

        val response = kmsClient.encrypt(encryptRequest)
        val algorithm = response.encryptionAlgorithmAsString()
        log.debug { "Encryption algorithm is $algorithm" }

        encryptedData = response.ciphertextBlob()
    }

    @Test
    @Order(4)
    fun `decrypt data`() {
        val decryptRequest = DecryptRequest.builder()
            .keyId(keyId)
            .ciphertextBlob(encryptedData)
            .build()

        val response: DecryptResponse = kmsClient.decrypt(decryptRequest)
        val plainBytes = response.plaintext()

        plainBytes.asUtf8String() shouldBeEqualTo data
    }

    @Test
    @Order(5)
    @Disabled("Floci KMS: DisableKey 미지원 (Operation not supported, Status 400)")
    fun `disable customer key`() {
        val disableKeyRequest = DisableKeyRequest.builder().keyId(keyId).build()

        val response = kmsClient.disableKey(disableKeyRequest)
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(6)
    @Disabled("Floci KMS: EnableKey 미지원 (Operation not supported, Status 400)")
    fun `enable customer key`() {
        val response = kmsClient.enableKey { it.keyId(keyId) }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(7)
    @Disabled("Floci KMS: CreateGrant 미지원 (Operation not supported, Status 400)")
    fun `create grant`() {
        val response = kmsClient.createGrant {
            it.keyId(keyId)
            it.granteePrincipal(granteePrincipal)
            it.operations(GrantOperation.CREATE_GRANT, GrantOperation.ENCRYPT, GrantOperation.DECRYPT)
        }
        log.debug { "Grant id=${response.grantId()}, token=${response.grantToken()}" }
        grantId = response.grantId()
    }

    @Test
    @Order(8)
    @Disabled("Floci KMS: ListGrants 미지원 (Operation not supported, Status 400)")
    fun `list grants`() {
        val response = kmsClient.listGrants {
            it.keyId(keyId)
            it.limit(15)
        }
        val grants = response.grants()
        grants.forEach { grant ->
            log.debug { "Grant id=${grant.grantId()}" }
        }
        grants.map { it.grantId() } shouldContain this.grantId
    }

    @Test
    @Order(9)
    @Disabled("Floci KMS: RevokeGrant 미지원 (CreateGrant 선행 필요, 동일 제약)")
    fun `revoke grant`() {
        val response = kmsClient.revokeGrant { it.keyId(keyId).grantId(grantId) }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(10)
    fun `describe key`() {
        val response = kmsClient.describeKey { it.keyId(keyId) }

        val keyMetadata = response.keyMetadata()
        log.debug { "key description=${keyMetadata.description()}" }
        log.debug { "key arn=${keyMetadata.arn()}" }
    }

    @Test
    @Order(11)
    fun `create custom alias`() {
        val response = kmsClient.createAlias {
            it.aliasName(aliasName).targetKeyId(keyId)
        }

        val metadata = response.responseMetadata()
        log.debug { "metadata=$metadata" }
    }

    @Test
    @Order(12)
    fun `list aliases`() {
        val response = kmsClient.listAliases { it.limit(10) }
        response.aliases().forEach { alias ->
            log.debug { "Alias name=$alias" }
        }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(13)
    fun `delete alias`() {
        val response = kmsClient.deleteAlias { it.aliasName(aliasName) }
        val metadata = response.responseMetadata()
        log.debug { "metadata=$metadata" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(14)
    fun `list keys`() {
        val response = kmsClient.listKeys { it.limit(15) }

        response.sdkHttpResponse().isSuccessful.shouldBeTrue()

        val keys = response.keys()
        keys.forEach { entry ->
            log.debug { "Key arn=${entry.keyArn()}, id=${entry.keyId()}" }
        }
    }

    @Test
    @Order(15)
    fun `put key policy`() {
        val policyName = "default"
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
            it.keyId(keyId).policyName(policyName).policy(policy)
        }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }
}
