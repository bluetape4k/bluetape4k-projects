package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
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
        MiniStackServer(reuse = false).use { server ->
            server.start()
            server.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `MiniStack 서버의 기본 속성이 올바르게 설정되어야 한다`() {
        MiniStackServer(reuse = false).use { server ->
            server.start()

            server.awsEndpoint.shouldNotBeNull()
            server.awsEndpoint.toString() shouldStartWith "http://"
            server.url shouldStartWith "http://"
            server.awsAccessKey shouldBeEqualTo MiniStackServer.DEFAULT_ACCESS_KEY
            server.awsSecretKey shouldBeEqualTo MiniStackServer.DEFAULT_SECRET_KEY
            server.regionName shouldBeEqualTo MiniStackServer.DEFAULT_REGION
        }
    }

    @Test
    fun `propertyKeys는 start 전에도 호출 가능하고 7개의 키를 반환해야 한다`() {
        val server = MiniStackServer(reuse = false)
        server.propertyKeys() shouldContainAll setOf(
            "host", "port", "url", "aws-endpoint", "aws-access-key", "aws-secret-key", "region"
        )
    }

    @Test
    fun `propertyNamespace는 ministack이어야 한다`() {
        MiniStackServer(reuse = false).propertyNamespace shouldBeEqualTo MiniStackServer.NAME
    }

    @Test
    fun `start 후 시스템 프로퍼티가 올바르게 등록되어야 한다`() {
        MiniStackServer(reuse = false).use { server ->
            server.start()
            System.getProperty("testcontainers.ministack.host") shouldBeEqualTo server.host
            System.getProperty("testcontainers.ministack.port") shouldBeEqualTo server.port.toString()
            System.getProperty("testcontainers.ministack.url") shouldBeEqualTo server.url
            System.getProperty("testcontainers.ministack.aws-endpoint") shouldBeEqualTo server.awsEndpoint.toString()
            System.getProperty("testcontainers.ministack.region") shouldBeEqualTo MiniStackServer.DEFAULT_REGION
        }
    }

    @Test
    fun `MiniStack S3 서비스를 사용하여 버킷을 생성하고 오브젝트를 저장 및 조회한다`() {
        MiniStackServer(reuse = false).use { server ->
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

            val content = s3Client.getObjectAsBytes(getRequest).asUtf8String()
            content shouldBeEqualTo "hello-ministack"
        }
    }

    @Test
    fun `withServices는 no-op이어야 한다 (MiniStack은 항상 모든 서비스 활성화)`() {
        MiniStackServer(reuse = false).use { server ->
            val returned = server.withServices("S3", "SQS", "DynamoDB")
            (returned === server).shouldBeTrue()
        }
    }
}
