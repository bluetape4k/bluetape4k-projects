package io.bluetape4k.testcontainers.aws.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsAsyncClient
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.model.CreateKeyRequest
import software.amazon.awssdk.services.kms.model.DecryptRequest
import software.amazon.awssdk.services.kms.model.DecryptResponse
import software.amazon.awssdk.services.kms.model.DisableKeyRequest
import software.amazon.awssdk.services.kms.model.EncryptRequest
import software.amazon.awssdk.services.kms.model.GrantOperation
import software.amazon.awssdk.services.kms.model.KeySpec
import software.amazon.awssdk.services.kms.model.KeyUsageType
import java.net.URI


@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class KMSTest: AbstractContainerTest() {

    companion object: KLogging()

    private val kmsServer: LocalStackServer by lazy {
        LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.KMS)
    }
    private val endpoint: URI get() = kmsServer.getEndpointOverride(LocalStackContainer.Service.KMS)

    private val kmsClient: KmsClient by lazy {
        KmsClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(kmsServer.region))
            .credentialsProvider(kmsServer.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }
    private val kmsAsyncClient: KmsAsyncClient by lazy {
        KmsAsyncClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(kmsServer.region))
            .credentialsProvider(kmsServer.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    private val keyDesc = "Create by the AWS KMS API"
    private lateinit var keyId: String

    private val data = "동해물과 백두산이"
    private lateinit var encryptedData: SdkBytes

    private val granteePrincipal = ""
    private val operation = "Decrypt, Encrypt"
    private lateinit var grantId: String

    // alias 는 prefix로 "alias/" 를 써야합니다.
    private val aliasName = "alias/ExampleName"

    @BeforeAll
    fun setup() {
        kmsServer.start()
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

        // Get the encrypted data
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
    fun `disable customer key`() {
        val disableKeyRequest = DisableKeyRequest.builder().keyId(keyId).build()

        val response = kmsClient.disableKey(disableKeyRequest)
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(6)
    fun `enable customer key`() {
        // val enableKeyRequest = EnableKeyRequest.builder().keyId(keyId).build()

        val response = kmsClient.enableKey { it.keyId(keyId) }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(7)
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
            // alias 는 prefix로 "alias/" 를 써야합니다.
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
