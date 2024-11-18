package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.Network
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class LocalStackServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `run S3 Service`() {
        LocalStackServer().withServices(LocalStackContainer.Service.S3).use { server ->
            server.start()

            // AWS SDK V2 사용
            val credentialProvider = server.getCredentialProvider()

            val s3Client = S3Client.builder()
                .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(server.region))
                .credentialsProvider(credentialProvider)
                .build()
                .apply {
                    ShutdownQueue.register(this)
                }

            s3Client.createBucket(CreateBucketRequest.builder().bucket("foo").build())
            Thread.sleep(100)

            val putRequest = PutObjectRequest.builder()
                .bucket("foo")
                .key("bar")
                .build()
            s3Client.putObject(putRequest, RequestBody.fromString("baz"))
            Thread.sleep(100)

            val getRequest = GetObjectRequest.builder()
                .bucket("foo")
                .key("bar")
                .build()
            val content = s3Client.getObjectAsBytes(getRequest).asUtf8String()
            content shouldBeEqualTo "baz"
        }
    }

    // @Disabled("custom network 을 쓰게 되면 다른 테스트에서 예외가 발생한다")
    @Test
    fun `run multiple services with custom network`() {
        Network.newNetwork().use { network ->
            LocalStackServer()
                .withNetwork(network)
                .withNetworkAliases("notthis", "localstack")
                .withServices(LocalStackContainer.Service.CLOUDWATCH, LocalStackContainer.Service.CLOUDWATCHLOGS)
                .use { server ->
                    server.start()
                    server.isRunning.shouldBeTrue()
                }
        }
    }
}
