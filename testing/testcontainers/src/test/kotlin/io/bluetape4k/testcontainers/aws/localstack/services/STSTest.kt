@file:Suppress("DEPRECATION")

package io.bluetape4k.testcontainers.aws.localstack.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.testcontainers.aws.localstack.AbstractLocalStackServiceTest
import io.bluetape4k.utils.ShutdownQueue
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient
import java.net.URI

@TestMethodOrder(OrderAnnotation::class)
class STSTest: AbstractLocalStackServiceTest() {

    companion object: KLogging()

    private val stsServer: LocalStackServer by lazy {
        localStack.withServices("sts")
    }
    private val endpoint: URI get() = stsServer.endpoint

    private val stsClient: StsClient by lazy {
        StsClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(stsServer.region))
            .credentialsProvider(stsServer.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    private lateinit var accountId: String

    @BeforeAll
    fun setup() {
        stsServer.start()
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
