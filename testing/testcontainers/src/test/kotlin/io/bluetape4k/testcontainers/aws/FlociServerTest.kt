package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
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
import io.bluetape4k.assertions.assertFailsWith

/**
 * [FlociServer] 테스트
 */
@Suppress("DEPRECATION")
class FlociServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { FlociServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { FlociServer(tag = " ") }
    }

    @Test
    fun `Floci 서버가 시작되고 실행 중이어야 한다`() {
        FlociServer().use { server ->
            server.start()
            server.isRunning.shouldBeTrue()
        }
    }

    @Test
    fun `Floci S3 서비스를 사용하여 버킷을 생성하고 오브젝트를 저장 및 조회한다`() {
        FlociServer().use { server ->
            server.start()

            val credentialProvider = server.getCredentialProvider()

            val s3Client = S3Client.builder()
                .endpointOverride(server.awsEndpoint)
                .region(Region.of(server.regionName))
                .credentialsProvider(credentialProvider)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()
                .apply { ShutdownQueue.register(this) }

            s3Client.createBucket(CreateBucketRequest.builder().bucket("test-bucket").build())

            val putRequest = PutObjectRequest.builder()
                .bucket("test-bucket")
                .key("test-key")
                .build()
            s3Client.putObject(putRequest, RequestBody.fromString("hello-floci"))

            val getRequest = GetObjectRequest.builder()
                .bucket("test-bucket")
                .key("test-key")
                .build()

            var content: String? = null
            await atMost Duration.ofSeconds(5) until {
                runCatching {
                    content = s3Client.getObjectAsBytes(getRequest).asUtf8String()
                    content == "hello-floci"
                }.getOrDefault(false)
            }

            content shouldBeEqualTo "hello-floci"
        }
    }

    @Test
    fun `withServices는 no-op이어야 한다 (Floci는 항상 모든 서비스 활성화)`() {
        FlociServer().use { server ->
            // withServices 호출해도 예외 없이 this를 반환해야 함
            val returned = server.withServices("S3", "SQS", "DynamoDB")
            (returned === server).shouldBeTrue()
        }
    }
}
