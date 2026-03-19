package io.bluetape4k.aws.kms

import io.bluetape4k.aws.core.toUtf8SdkBytes
import io.bluetape4k.aws.kms.model.createAliasRequestOf
import io.bluetape4k.aws.kms.model.createGrantRequestOf
import io.bluetape4k.aws.kms.model.createKeyRequestOf
import io.bluetape4k.aws.kms.model.decryptRequestOf
import io.bluetape4k.aws.kms.model.describeKeyOf
import io.bluetape4k.aws.kms.model.disableKeyRequestOf
import io.bluetape4k.aws.kms.model.enableKeyRequestOf
import io.bluetape4k.aws.kms.model.encryptRequestOf
import io.bluetape4k.aws.kms.model.listAliasesRequestOf
import io.bluetape4k.aws.kms.model.putKeyPolicyRequestOf
import io.bluetape4k.aws.kms.model.revokeGrantRequestOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kms.model.GrantOperation
import software.amazon.awssdk.services.kms.model.KeySpec
import software.amazon.awssdk.services.kms.model.KeyUsageType
import software.amazon.awssdk.services.kms.model.KmsResponseMetadata

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class KmsAsyncClientTest: AbstractKmsTest() {

    companion object: KLogging()

    private val keyDescription = "예제용 KMS 키에 대한 설명입니다 - By KmsAsyncClient"
    private lateinit var keyId: String

    private val data = randomString()
    private lateinit var encryptedData: SdkBytes

    private val granteePrincipal = ""
    private lateinit var grantId: String

    // alias 는 prefix로 "alias/" 를 써야합니다.
    private val aliasName = "alias/CustomAliasNameByAsyncClient"

    @Test
    @Order(1)
    fun `KmsAsyncClient 인스턴스 생성`() {
        asyncClient.shouldNotBeNull()
    }

    @Test
    @Order(2)
    fun `대칭 키 생성`() = runTest {
        val request = createKeyRequestOf(
            description = keyDescription,
            keySpec = KeySpec.SYMMETRIC_DEFAULT,
            keyUsage = KeyUsageType.ENCRYPT_DECRYPT
        )

        val response = asyncClient.createKey(request).await()
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
        log.debug { "Create a custom key at arn=${response.keyMetadata().arn()}" }

        keyId = response.keyMetadata().keyId()
        log.info { "custom keyId=$keyId" }
        keyId.shouldNotBeEmpty()
    }

    @Test
    @Order(3)
    fun `데이터 암호화`() = runTest {
        val request = encryptRequestOf(
            keyId = keyId,
            plainText = data.toUtf8SdkBytes()
        )
        val response = asyncClient.encrypt(request).await()
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()

        val algorithm = response.encryptionAlgorithmAsString()
        log.debug { "Encryption algorithm: $algorithm" }

        // Get the encrypted data
        encryptedData = response.ciphertextBlob()
    }

    @Test
    @Order(4)
    fun `데이터 복호화`() = runTest {
        val request = decryptRequestOf(
            keyId = keyId,
            ciphertextBlob = encryptedData
        )
        val response = asyncClient.decrypt(request).await()
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()

        val plainBytes = response.plaintext()
        plainBytes.asUtf8String() shouldBeEqualTo data
    }

    @Test
    @Order(5)
    fun `키 비활성화`() = runTest {
        val request = disableKeyRequestOf(keyId)

        val response = asyncClient.disableKey(request).await()
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(6)
    fun `키 활성화`() = runTest {
        val request = enableKeyRequestOf(keyId)
        val response = asyncClient.enableKey(request).await()
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(7)
    fun `Grant 생성`() = runTest {
        val request = createGrantRequestOf(
            keyId = keyId,
            granteePrincipal = granteePrincipal,
            GrantOperation.CREATE_GRANT, GrantOperation.ENCRYPT, GrantOperation.DECRYPT
        )
        val response = asyncClient.createGrant(request).await()
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()

        log.debug { "Grant id=${response.grantId()}, token=${response.grantToken()}" }
        grantId = response.grantId()
    }

    @Test
    @Order(8)
    fun `Grant 목록 조회`() = runTest {
        val listResp = asyncClient.listGrants {
            it.keyId(keyId)
            it.limit(15)
        }.await()

        listResp.sdkHttpResponse().isSuccessful.shouldBeTrue()

        val grants = listResp.grants()
        grants.forEach { grant ->
            log.debug { "Grantt id=${grant.grantId()}" }
        }
        grants.shouldNotBeEmpty()
        grants.map { it.grantId() } shouldContain grantId
    }

    @Test
    @Order(9)
    fun `Grant 취소`() = runTest {
        val request = revokeGrantRequestOf(keyId, grantId)
        val response = asyncClient.revokeGrant(request).await()

        log.debug { "Response=$response" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(10)
    fun `키 메타데이터 조회`() = runTest {
        val request = describeKeyOf(keyId)
        val response = asyncClient.describeKey(request).await()

        response.sdkHttpResponse().isSuccessful.shouldBeTrue()

        val keyMetadata = response.keyMetadata()
        log.debug { "key description=${keyMetadata.description()}" }
        log.debug { "key id=${keyMetadata.keyId()}, arn=${keyMetadata.arn()}" }
        keyMetadata.description() shouldBeEqualTo keyDescription
    }

    @Test
    @Order(11)
    fun `커스텀 Alias 생성`() = runTest {
        log.debug { "Create custom alias. alias name=${aliasName}, keyId=$keyId" }

        val request = createAliasRequestOf(aliasName, keyId)
        val response = asyncClient.createAlias(request).await()

        response.sdkHttpResponse().isSuccessful.shouldBeTrue()

        val metadata: KmsResponseMetadata = response.responseMetadata()
        log.debug { "metadata=$metadata" }
    }

    @Test
    @Order(12)
    fun `Alias 목록 조회`() = runTest {
        val request = listAliasesRequestOf(limit = 10)
        val response = asyncClient.listAliases(request).await()

        response.sdkHttpResponse().isSuccessful.shouldBeTrue()

        val aliases = response.aliases()
        aliases.forEach { alias ->
            log.debug { "alias=$alias" }
        }
        aliases.shouldNotBeEmpty()
        aliases.map { it.aliasName() } shouldContain aliasName
    }

    @Test
    @Order(13)
    fun `Alias 삭제`() = runTest {
        val response = asyncClient.deleteAlias { it.aliasName(aliasName) }.await()
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(14)
    fun `키 목록 조회`() = runTest {
        val response = asyncClient.listKeys { it.limit(15) }.await()
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()

        val keys = response.keys()
        keys.forEach { key ->
            log.debug { "key=$key" }
        }
        keys.map { it.keyId() } shouldContain keyId
    }


    @Test
    @Order(15)
    fun `키 정책 설정`() = runTest {
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

        val request = putKeyPolicyRequestOf(keyId, policyName, policy)
        val response = asyncClient.putKeyPolicy(request).await()
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

}
