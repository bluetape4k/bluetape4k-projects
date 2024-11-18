package io.bluetape4k.testcontainers.aws.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import java.net.URI

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CloudWatchTest: AbstractContainerTest() {

    companion object: KLogging()

    private val cloudWatch: LocalStackServer by lazy {
        LocalStackServer.Launcher.localStack
            .withServices(
                LocalStackContainer.Service.CLOUDWATCH,
                LocalStackContainer.Service.CLOUDWATCHLOGS
            )
    }

    private val cloudWatchEndpoint: URI
        get() = cloudWatch.getEndpointOverride(LocalStackContainer.Service.CLOUDWATCH)

    private val cloudWatchLogsEndpoint: URI
        get() = cloudWatch.getEndpointOverride(LocalStackContainer.Service.CLOUDWATCHLOGS)

    private val cloudWatchClient by lazy {
        CloudWatchClient.builder()
            .endpointOverride(cloudWatchEndpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(cloudWatch.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    private val cloudWatchLogsClient by lazy {
        CloudWatchLogsClient.builder()
            .endpointOverride(cloudWatchEndpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(cloudWatch.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }


    @BeforeAll
    fun setup() {
        cloudWatch.start()
    }

    @Test
    @Order(1)
    fun `create client`() {
        cloudWatchClient.shouldNotBeNull()
        cloudWatchLogsClient.shouldNotBeNull()
    }
}
