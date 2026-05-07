package io.bluetape4k.testcontainers.aws.ministack.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.testcontainers.aws.ministack.AbstractMiniStackServiceTest
import io.bluetape4k.utils.ShutdownQueue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * MiniStack S3 서비스 통합 테스트.
 *
 * MiniStack은 virtual-hosted URL을 지원하지 않을 수 있으므로 path-style access를 사용합니다.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class MiniStackS3Test: AbstractMiniStackServiceTest() {

    companion object: KLogging() {
        private val BUCKET_NAME = "ministack-test-bucket-${System.currentTimeMillis()}"
        private const val KEY_NAME = "test-object"
        private const val CONTENT = "hello-ministack-s3"
    }

    private val s3Client by lazy {
        S3Client.builder()
            .endpointOverride(miniStack.awsEndpoint)
            .region(Region.of(miniStack.regionName))
            .credentialsProvider(miniStack.getCredentialProvider())
            .serviceConfiguration(
                S3Configuration.builder().pathStyleAccessEnabled(true).build()
            )
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    @Test
    @Order(1)
    fun `MiniStack S3 서버가 실행 중이어야 한다`() {
        miniStack.isRunning.shouldBeTrue()
    }

    @Test
    @Order(2)
    fun `create bucket`() {
        val waiter = s3Client.waiter()

        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build())

        val waiterResponse = waiter.waitUntilBucketExists(
            HeadBucketRequest.builder().bucket(BUCKET_NAME).build()
        )
        waiterResponse.matched().exception().ifPresent { ex ->
            throw AssertionError("Bucket creation waiter failed via exception branch", ex)
        }
        val response = waiterResponse.matched().response()
            .orElseThrow { AssertionError("Waiter returned neither response nor exception") }
        log.debug { "Bucket created: ${response.sdkHttpResponse()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(3)
    fun `put object`() {
        val response = s3Client.putObject(
            PutObjectRequest.builder().bucket(BUCKET_NAME).key(KEY_NAME).build(),
            RequestBody.fromBytes(CONTENT.toUtf8Bytes())
        )
        log.debug { "eTag=${response.eTag()}" }
        response.eTag().shouldNotBeEmpty()
    }

    @Test
    @Order(4)
    fun `get object`() {
        val bytes = s3Client.getObjectAsBytes(
            GetObjectRequest.builder().bucket(BUCKET_NAME).key(KEY_NAME).build()
        ).asByteArray()
        bytes.toUtf8String() shouldBeEqualTo CONTENT
    }
}
