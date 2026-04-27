package io.bluetape4k.testcontainers.aws.floci.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.FlociServer
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * [FlociServer]를 사용한 S3 서비스 통합 테스트.
 *
 * LocalStack 기반 [io.bluetape4k.testcontainers.aws.services.S3Test]에 대응합니다.
 */
@Suppress("DEPRECATION")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FlociS3Test: AbstractContainerTest() {

    companion object: KLogging()

    private val floci: FlociServer
        get() = FlociServer.Launcher.floci

    private val s3Client: S3Client by lazy {
        S3Client.builder()
            .endpointOverride(floci.awsEndpoint)
            .region(Region.of(floci.regionName))
            .credentialsProvider(floci.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    private val bucketName = "foo"
    private val keyName = "bar"
    private val content = "baz"

    @BeforeAll
    fun setup() {
        floci.isRunning.shouldBeTrue()
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

        val createBucketRequest = CreateBucketRequest.builder().bucket(bucketName).build()
        s3Client.createBucket(createBucketRequest)

        val bucketRequestWait = HeadBucketRequest.builder().bucket(bucketName).build()
        val waiterResponse = waiter.waitUntilBucketExists(bucketRequestWait)
        waiterResponse.matched().response().ifPresent {
            log.debug { "S3 HTTP response: ${it.sdkHttpResponse()}" }
            it.sdkHttpResponse().isSuccessful.shouldBeTrue()
        }
    }

    @Test
    @Order(3)
    fun `put object`() {
        val metadata = mapOf("x-amz-meta-myVal" to "test")
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(keyName)
            .metadata(metadata)
            .build()

        val response = s3Client.putObject(request, RequestBody.fromBytes(content.toUtf8Bytes()))

        log.debug { "eTag=${response.eTag()}" }
        response.eTag().shouldNotBeEmpty()
    }

    @Test
    @Order(4)
    fun `get object`() {
        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(keyName)
            .build()

        val response = s3Client.getObjectAsBytes(request)!!
        val bytes = response.asByteArray()!!
        bytes.toUtf8String() shouldBeEqualTo content
    }
}
