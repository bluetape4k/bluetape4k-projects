package io.bluetape4k.testcontainers.aws.floci.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.aws.floci.AbstractFlociServiceTest
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient

/**
 * [io.bluetape4k.testcontainers.aws.FlociServer]를 사용한 STS 서비스 통합 테스트.
 *
 * LocalStack 기반 [io.bluetape4k.testcontainers.aws.localstack.services.STSTest]에 대응합니다.
 */
@Suppress("DEPRECATION")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FlociSTSTest : AbstractFlociServiceTest() {

    companion object : KLogging()

    private val stsClient: StsClient by lazy {
        StsClient.builder()
            .endpointOverride(floci.awsEndpoint)
            .region(Region.of(floci.regionName))
            .credentialsProvider(floci.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    private lateinit var accountId: String

    @Test
    @Order(1)
    fun `get caller identity`() {
        val identity = stsClient.getCallerIdentity { }
        log.debug { "Account: ${identity.account()}, UserId: ${identity.userId()}, ARN: ${identity.arn()}" }

        accountId = requireNotNull(identity.account()) {
            "getCallerIdentity response returned a null account — Floci STS failed"
        }
        accountId.shouldNotBeBlank()
        identity.userId().shouldNotBeBlank()
        identity.arn().shouldNotBeBlank()
    }

    @Test
    @Order(2)
    fun `assume role`() {
        val credentials = stsClient.assumeRole {
            it.roleArn("arn:aws:iam::$accountId:role/test-execution-role")
                .roleSessionName("bluetape4k-test-session")
                .durationSeconds(3600)
        }.credentials()

        log.debug { "AssumeRole AccessKeyId: ${credentials.accessKeyId()}" }
        credentials.shouldNotBeNull()
        credentials.accessKeyId().shouldNotBeBlank()
        credentials.secretAccessKey().shouldNotBeBlank()
        credentials.sessionToken().shouldNotBeBlank()
        credentials.expiration().shouldNotBeNull()
    }

    @Test
    @Order(3)
    fun `get session token`() {
        val credentials = stsClient.getSessionToken {
            it.durationSeconds(3600)
        }.credentials()

        log.debug { "SessionToken AccessKeyId: ${credentials?.accessKeyId()}" }
        credentials.shouldNotBeNull()
        credentials.accessKeyId().shouldNotBeBlank()
        credentials.secretAccessKey().shouldNotBeBlank()
        credentials.sessionToken().shouldNotBeBlank()
    }
}
