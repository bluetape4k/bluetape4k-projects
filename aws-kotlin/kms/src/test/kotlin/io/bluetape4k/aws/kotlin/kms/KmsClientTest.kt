package io.bluetape4k.aws.kotlin.kms

import aws.sdk.kotlin.services.kms.createAlias
import aws.sdk.kotlin.services.kms.createGrant
import aws.sdk.kotlin.services.kms.createKey
import aws.sdk.kotlin.services.kms.decrypt
import aws.sdk.kotlin.services.kms.deleteAlias
import aws.sdk.kotlin.services.kms.describeKey
import aws.sdk.kotlin.services.kms.disableKey
import aws.sdk.kotlin.services.kms.enableKey
import aws.sdk.kotlin.services.kms.encrypt
import aws.sdk.kotlin.services.kms.listAliases
import aws.sdk.kotlin.services.kms.listGrants
import aws.sdk.kotlin.services.kms.listKeys
import aws.sdk.kotlin.services.kms.model.GrantOperation
import aws.sdk.kotlin.services.kms.model.KeySpec
import aws.sdk.kotlin.services.kms.model.KeyUsageType
import aws.sdk.kotlin.services.kms.putKeyPolicy
import aws.sdk.kotlin.services.kms.revokeGrant
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class KmsClientTest: AbstractKmsTest() {

    companion object: KLogging()

    private val testKeyDescription = "예제용 KMS 키에 대한 설명입니다 - By KmsClient"
    private lateinit var testKeyId: String

    private val data = randomString()
    private lateinit var encryptedBlob: ByteArray

    // LocalStack 테스트 환경에서는 빈 문자열로도 Grant 생성이 가능합니다.
    private val testGranteePrincipal = ""
    private lateinit var testGrantId: String

    // alias 는 prefix로 "alias/" 를 써야합니다.
    private val testAliasName = "alias/CustomAliasNameByKmsClient"

    @Test
    @Order(1)
    fun `KmsClient 인스턴스 생성`() {
        client.shouldNotBeNull()
    }

    @Test
    @Order(2)
    fun `대칭 키 생성`() = runTest {
        val response = client.createKey {
            keySpec = KeySpec.SymmetricDefault
            keyUsage = KeyUsageType.EncryptDecrypt
            description = testKeyDescription
        }
        log.debug { "Create a custom key at arn=${response.keyMetadata?.arn}" }

        testKeyId = response.keyMetadata?.keyId ?: ""
        log.info { "custom keyId=$testKeyId" }
        testKeyId.shouldNotBeEmpty()
    }

    @Test
    @Order(3)
    fun `데이터 암호화`() = runTest {
        val response = client.encrypt {
            keyId = testKeyId
            plaintext = data.toUtf8Bytes()
        }

        val algorithm = response.encryptionAlgorithm.toString()
        log.debug { "Encryption algorithm: $algorithm" }
        algorithm.shouldNotBeEmpty()

        encryptedBlob = response.ciphertextBlob.shouldNotBeNull()
    }

    @Test
    @Order(4)
    fun `데이터 복호화`() = runTest {
        val response = client.decrypt {
            keyId = testKeyId
            ciphertextBlob = encryptedBlob
        }

        val plainBytes = response.plaintext.shouldNotBeNull()
        plainBytes.toUtf8String() shouldBeEqualTo data
    }

    @Test
    @Order(5)
    fun `키 비활성화`() = runTest {
        val response = client.disableKey {
            keyId = testKeyId
        }
        log.debug { "disableKey response=$response" }
    }

    @Test
    @Order(6)
    fun `키 활성화`() = runTest {
        val response = client.enableKey {
            keyId = testKeyId
        }
        log.debug { "enableKey response=$response" }
    }

    @Test
    @Order(7)
    fun `Grant 생성`() = runTest {
        val response = client.createGrant {
            keyId = testKeyId
            granteePrincipal = testGranteePrincipal
            operations = listOf(GrantOperation.CreateGrant, GrantOperation.Encrypt, GrantOperation.Decrypt)
        }

        log.debug { "Grant id=${response.grantId}, token=${response.grantToken}" }
        testGrantId = response.grantId.shouldNotBeNull()
    }

    @Test
    @Order(8)
    fun `Grant 목록 조회`() = runTest {
        val listResp = client.listGrants {
            keyId = testKeyId
            limit = 15
        }

        val grants = listResp.grants
        grants?.forEach { grant ->
            log.debug { "Grant id=${grant.grantId}" }
        }
        grants.shouldNotBeNull().shouldNotBeEmpty()
        grants.map { it.grantId } shouldContain testGrantId
    }

    @Test
    @Order(9)
    fun `Grant 취소`() = runTest {
        val response = client.revokeGrant {
            keyId = testKeyId
            grantId = testGrantId
        }
        log.debug { "revokeGrant response=$response" }
    }

    @Test
    @Order(10)
    fun `키 메타데이터 조회`() = runTest {
        val response = client.describeKey {
            keyId = testKeyId
        }

        val keyMetadata = response.keyMetadata.shouldNotBeNull()
        log.debug { "key metadata=$keyMetadata" }
        log.debug { "key description=${keyMetadata.description}" }
        log.debug { "key id=${keyMetadata.keyId}, arn=${keyMetadata.arn}" }
        keyMetadata.description shouldBeEqualTo testKeyDescription
    }

    @Test
    @Order(11)
    fun `커스텀 Alias 생성`() = runTest {
        log.debug { "Create custom alias. alias name=${testAliasName}, keyId=$testKeyId" }

        val response = client.createAlias {
            aliasName = testAliasName
            targetKeyId = testKeyId
        }

        log.debug { "createAlias response=$response" }
    }

    @Test
    @Order(12)
    fun `Alias 목록 조회`() = runTest {
        val response = client.listAliases { limit = 15 }

        val aliases = response.aliases.shouldNotBeNull()
        aliases.forEach { alias ->
            log.debug { "alias=$alias" }
        }
        aliases.shouldNotBeEmpty()
        aliases.map { it.aliasName } shouldContain testAliasName
    }

    @Test
    @Order(13)
    fun `Alias 삭제`() = runTest {
        val response = client.deleteAlias { aliasName = testAliasName }
        log.debug { "deleteAlias response=$response" }
    }

    @Test
    @Order(14)
    fun `키 목록 조회`() = runTest {
        val response = client.listKeys { limit = 15 }

        val keys = response.keys.shouldNotBeNull()
        keys.forEach { key ->
            log.debug { "key=$key" }
        }
        keys.map { it.keyId } shouldContain testKeyId
    }

    @Test
    @Order(15)
    fun `키 정책 설정`() = runTest {
        val testPolicyName = "default"
        val testPolicy = """
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

        val response = client.putKeyPolicy {
            keyId = testKeyId
            policyName = testPolicyName
            policy = testPolicy
        }
        log.debug { "putKeyPolicy response=$response" }
    }
}
