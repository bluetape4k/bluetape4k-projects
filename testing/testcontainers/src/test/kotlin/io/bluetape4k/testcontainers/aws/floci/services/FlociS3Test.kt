package io.bluetape4k.testcontainers.aws.floci.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.testcontainers.aws.floci.AbstractFlociServiceTest
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
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
 * [io.bluetape4k.testcontainers.aws.FlociServer]를 사용한 S3 서비스 통합 테스트.
 *
 * LocalStack 기반 [io.bluetape4k.testcontainers.aws.localstack.services.S3Test]에 대응합니다.
 * Floci는 virtual-hosted-style URL을 지원하지 않으므로 path-style access를 사용합니다.
 */
@Suppress("DEPRECATION")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FlociS3Test : AbstractFlociServiceTest() {

    companion object : KLogging() {
        private val BUCKET_NAME = "foo-${System.currentTimeMillis()}"
        private const val KEY_NAME = "bar"
        private const val CONTENT = "baz"
    }

    private val s3Client: S3Client by lazy {
        S3Client.builder()
            .endpointOverride(floci.awsEndpoint)
            .region(Region.of(floci.regionName))
            .credentialsProvider(floci.getCredentialProvider())
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    @Test
    @Order(1)
    fun `run s3 server by FlociServer`() {
        floci.isRunning.shouldBeTrue()
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
        log.debug { "S3 HTTP response: ${response.sdkHttpResponse()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(3)
    fun `put object`() {
        val metadata = mapOf("x-amz-meta-myVal" to "test")
        val request = PutObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(KEY_NAME)
            .metadata(metadata)
            .build()

        val response = s3Client.putObject(request, RequestBody.fromBytes(CONTENT.toUtf8Bytes()))

        log.debug { "eTag=${response.eTag()}" }
        response.eTag().shouldNotBeEmpty()
    }

    @Test
    @Order(4)
    fun `get object`() {
        val request = GetObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(KEY_NAME)
            .build()

        val bytes = s3Client.getObjectAsBytes(request).asByteArray()
        bytes.toUtf8String() shouldBeEqualTo CONTENT
    }
}
