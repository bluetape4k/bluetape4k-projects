package io.bluetape4k.testcontainers.aws.ministack.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.MiniStackServer
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient

/**
 * MiniStack STS 서비스 통합 테스트.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MiniStackSTSTest: AbstractContainerTest() {

    companion object: KLogging()

    private val miniStack: MiniStackServer by lazy { MiniStackServer.Launcher.miniStack }

    private val stsClient: StsClient by lazy {
        StsClient.builder()
            .endpointOverride(miniStack.awsEndpoint)
            .region(Region.of(miniStack.regionName))
            .credentialsProvider(miniStack.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    private lateinit var accountId: String

    @BeforeAll
    fun setup() {
        miniStack.start()
    }

    @Test
    @Order(1)
    fun `get caller identity`() {
        val identity = stsClient.getCallerIdentity { }
        log.debug { "Account: ${identity.account()}, UserId: ${identity.userId()}, ARN: ${identity.arn()}" }

        accountId = identity.account()
        identity.account().shouldNotBeBlank()
        identity.userId().shouldNotBeBlank()
        identity.arn().shouldNotBeBlank()
    }

    @Test
    @Order(2)
    fun `assume role`() {
        val credentials = stsClient.assumeRole {
            it.roleArn("arn:aws:iam::$accountId:role/test-execution-role")
                .roleSessionName("bluetape4k-ministack-test-session")
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
        val credentials = stsClient.getSessionToken { it.durationSeconds(3600) }.credentials()

        log.debug { "SessionToken AccessKeyId: ${credentials?.accessKeyId()}" }
        credentials.shouldNotBeNull()
        credentials.accessKeyId().shouldNotBeBlank()
        credentials.secretAccessKey().shouldNotBeBlank()
        credentials.sessionToken().shouldNotBeBlank()
    }
}
