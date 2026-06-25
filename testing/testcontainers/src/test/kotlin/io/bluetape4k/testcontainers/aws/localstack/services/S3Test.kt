@file:Suppress("DEPRECATION")

package io.bluetape4k.testcontainers.aws.localstack.services

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.testcontainers.aws.localstack.AbstractLocalStackServiceTest
import io.bluetape4k.utils.ShutdownQueue
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
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
import java.net.URI

@TestMethodOrder(OrderAnnotation::class)
class S3Test: AbstractLocalStackServiceTest() {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    private val s3Server: LocalStackServer by lazy {
        localStack.withServices("s3")
    }
    private val endpoint: URI get() = s3Server.endpoint

    private val s3Client by lazy {
        S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.of(s3Server.region))
            .credentialsProvider(s3Server.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    private val bucketName = "foo"
    private val keyName = "bar"
    private val content = "baz"

    @BeforeAll
    fun setup() {
        s3Server.start()
    }

    @Test
    @Order(1)
    fun `run s3 server by LocalStackServer`() {
        s3Server.isRunning.shouldBeTrue()
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
            println(it.sdkHttpResponse())
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
