package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldStartWith
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.Duration
import kotlin.test.assertFailsWith

/**
 * [MiniStackServer] 기본 동작 테스트.
 */
class MiniStackServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { MiniStackServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { MiniStackServer(tag = " ") }
    }

    @Test
    fun `MiniStack 서버가 시작되고 실행 중이어야 한다`() {
        MiniStackServer().use { server ->
            server.start()
            server.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `MiniStack 서버의 기본 속성이 올바르게 설정되어야 한다`() {
        MiniStackServer().use { server ->
            server.start()

            server.awsEndpoint.shouldNotBeNull()
            server.awsEndpoint.toString() shouldStartWith "http://"
            server.awsAccessKey shouldBeEqualTo MiniStackServer.DEFAULT_ACCESS_KEY
            server.awsSecretKey shouldBeEqualTo MiniStackServer.DEFAULT_SECRET_KEY
            server.regionName shouldBeEqualTo MiniStackServer.DEFAULT_REGION
        }
    }

    @Test
    fun `MiniStack S3 서비스를 사용하여 버킷을 생성하고 오브젝트를 저장 및 조회한다`() {
        MiniStackServer().use { server ->
            server.start()

            val credentialProvider = server.getCredentialProvider()

            val s3Client = S3Client.builder()
                .endpointOverride(server.awsEndpoint)
                .region(Region.of(server.regionName))
                .credentialsProvider(credentialProvider)
                .serviceConfiguration(
                    S3Configuration.builder().pathStyleAccessEnabled(true).build()
                )
                .build()
                .apply { ShutdownQueue.register(this) }

            s3Client.createBucket(CreateBucketRequest.builder().bucket("test-bucket").build())

            val putRequest = PutObjectRequest.builder()
                .bucket("test-bucket")
                .key("test-key")
                .build()
            s3Client.putObject(putRequest, RequestBody.fromString("hello-ministack"))

            val getRequest = GetObjectRequest.builder()
                .bucket("test-bucket")
                .key("test-key")
                .build()

            var content: String? = null
            await atMost Duration.ofSeconds(5) until {
                runCatching {
                    content = s3Client.getObjectAsBytes(getRequest).asUtf8String()
                    content == "hello-ministack"
                }.getOrDefault(false)
            }

            content shouldBeEqualTo "hello-ministack"
        }
    }

    @Test
    fun `withServices는 no-op이어야 한다 (MiniStack은 항상 모든 서비스 활성화)`() {
        MiniStackServer().use { server ->
            val returned = server.withServices("S3", "SQS", "DynamoDB")
            (returned === server).shouldBeTrue()
        }
    }
}
